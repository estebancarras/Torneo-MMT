package los5fantasticos.memorias

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File
import java.time.Duration
import java.util.UUID

/**
 * Representa un duelo individual de Memorias entre dos jugadores.
 * Esta clase NO crea BukkitTasks - toda su lógica es impulsada por el Game Loop.
 * 
 * @property player1 Primer jugador del duelo
 * @property player2 Segundo jugador del duelo
 * @property parcela Parcela donde se desarrolla el duelo
 * @property plugin Plugin para cargar configuración
 */
class DueloMemorias(
    val player1: Player,
    val player2: Player,
    val parcela: Parcela,
    private val plugin: Plugin
) {
    // ===== CONFIGURACIÓN DEL JUEGO =====
    private val memorizationTimeSeconds: Int
    private val playerTimeSeconds: Int
    private val turnChangeOnFail: Boolean
    private val gridSize: Int
    private val revealTimeSeconds: Int
    
    // ===== ESTADO DEL DUELO =====
    private var estado: DueloEstado = DueloEstado.MEMORIZANDO
    private var ticksTranscurridos = 0
    
    // ===== FASE DE MEMORIZACIÓN =====
    private var ticksParaMemorizar: Int
    
    // ===== SISTEMA DE TURNOS =====
    private var turnoActual: UUID? = null
    private val jugadores = listOf(player1, player2)
    
    // ===== TEMPORIZADORES INDIVIDUALES =====
    private val tiempoRestante = mutableMapOf(
        player1.uniqueId to 0,
        player2.uniqueId to 0
    )
    
    // ===== TABLERO DE JUEGO =====
    private val tablero = mutableListOf<CasillaMemorias>()
    private val materialOculto = Material.GRAY_WOOL
    
    // ===== PUNTUACIONES =====
    private val puntuaciones = mutableMapOf(player1 to 0, player2 to 0)
    
    // ===== ESTADO DE SELECCIÓN =====
    private var esperandoSegundoClic = false
    private var primerBloque: CasillaMemorias? = null
    private var segundoBloque: CasillaMemorias? = null
    
    // ===== CONTROL DE TIEMPO PARA OCULTAR BLOQUES =====
    private var ticksParaOcultar = 0
    
    // ===== COLORES DISPONIBLES =====
    private val coloresDisponibles = listOf(
        Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL,
        Material.YELLOW_WOOL, Material.LIME_WOOL, Material.ORANGE_WOOL,
        Material.PINK_WOOL, Material.PURPLE_WOOL
    )
    
    /**
     * Representa una casilla del tablero.
     */
    data class CasillaMemorias(
        val ubicacion: Location,
        val colorReal: Material,
        var revelada: Boolean = false,
        val idPar: Int
    )
    
    // El enum EstadoDuelo ahora es DueloEstado y se encuentra en su propio archivo
    
    init {
        // Cargar configuración desde memorias.yml
        val configFile = File(plugin.dataFolder, "memorias.yml")
        val config = if (configFile.exists()) {
            YamlConfiguration.loadConfiguration(configFile)
        } else {
            // Valores por defecto si el archivo no existe
            YamlConfiguration()
        }
        
        memorizationTimeSeconds = config.getInt("game-settings.memorization-time-seconds", 10)
        playerTimeSeconds = config.getInt("game-settings.player-time-seconds", 120)
        turnChangeOnFail = config.getBoolean("game-settings.turn-change-on-fail", true)
        gridSize = config.getInt("game-settings.grid-size", 5)
        revealTimeSeconds = config.getInt("game-settings.reveal-time-seconds", 2)
        
        // Inicializar temporizadores (en ticks, cada jugador tiene su tiempo)
        tiempoRestante[player1.uniqueId] = playerTimeSeconds * 20
        tiempoRestante[player2.uniqueId] = playerTimeSeconds * 20
        
        // Inicializar tiempo de memorización
        ticksParaMemorizar = memorizationTimeSeconds * 20
        
        // Teleportar jugadores a sus spawns
        player1.teleport(parcela.spawn1)
        player2.teleport(parcela.spawn2)
        
        // Generar tablero y revelar todos los bloques para la fase de memorización
        generarTablero()
        revelarTodoElTablero()
    }
    
    /**
     * Método principal llamado por el Game Loop cada 2 ticks.
     * PROHIBIDO crear BukkitTasks aquí.
     */
    fun actualizar() {
        when (estado) {
            DueloEstado.MEMORIZANDO -> actualizarMemorizacion()
            DueloEstado.JUGANDO -> actualizarJuego()
            DueloEstado.FINALIZADO -> {}  // No hace nada, espera a ser removido
        }
        
        ticksTranscurridos++
    }
    
    /**
     * Actualiza la fase de memorización.
     * Todos los bloques están visibles y los jugadores tienen tiempo para memorizarlos.
     */
    private fun actualizarMemorizacion() {
        ticksParaMemorizar--
        
        // Actualizar action bar cada 10 ticks (0.5 segundos)
        if (ticksParaMemorizar % 10 == 0) {
            val segundosRestantes = (ticksParaMemorizar / 20) + 1
            val mensaje = Component.text("¡Memoriza las posiciones! ", NamedTextColor.YELLOW, TextDecoration.BOLD)
                .append(Component.text("Tiempo: ", NamedTextColor.GOLD))
                .append(Component.text("${segundosRestantes}s", NamedTextColor.WHITE))
            
            jugadores.forEach { it.sendActionBar(mensaje) }
        }
        
        // Cuando el tiempo se agota, cambiar a fase de juego
        if (ticksParaMemorizar <= 0) {
            // Ocultar todos los bloques
            ocultarTodoElTablero()
            
            // Cambiar a estado JUGANDO
            estado = DueloEstado.JUGANDO
            
            // Iniciar turno del primer jugador
            iniciarTurno(player1)
            
            // Notificar a los jugadores
            jugadores.forEach { jugador ->
                val titulo = Component.text("¡A JUGAR!", NamedTextColor.GREEN, TextDecoration.BOLD)
                val subtitulo = Component.text("Encuentra los pares de colores", NamedTextColor.YELLOW)
                val tiempos = Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(2), Duration.ofMillis(500))
                jugador.showTitle(Title.title(titulo, subtitulo, tiempos))
                jugador.playSound(jugador.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f)
            }
        }
    }
    
    /**
     * Actualiza la fase de juego activo.
     * Gestiona temporizadores individuales y lógica de turnos.
     */
    private fun actualizarJuego() {
        // Manejar ocultación de bloques temporales
        if (ticksParaOcultar > 0) {
            ticksParaOcultar--
            if (ticksParaOcultar == 0) {
                ocultarBloquesTemporales()
            }
        }
        
        // Actualizar temporizador del jugador actual
        turnoActual?.let { uuidActual ->
            val tiempoActual = tiempoRestante[uuidActual] ?: 0
            
            if (tiempoActual > 0) {
                tiempoRestante[uuidActual] = tiempoActual - 1
                
                // Actualizar action bar cada 10 ticks
                if (tiempoRestante[uuidActual]!! % 10 == 0) {
                    actualizarActionBars()
                }
                
                // Timeout: el jugador se quedó sin tiempo
                if (tiempoRestante[uuidActual]!! <= 0) {
                    manejarTimeoutJugador(uuidActual)
                }
            }
        }
    }
    
    /**
     * Genera el tablero de juego con pares de colores.
     */
    private fun generarTablero() {
        tablero.clear()
        
        val totalCasillas = gridSize * gridSize
        val paresNecesarios = totalCasillas / 2
        
        // Crear lista de pares
        val pares = mutableListOf<Int>()
        for (i in 0 until paresNecesarios) {
            pares.add(i)
            pares.add(i)
        }
        
        // Si hay número impar, agregar una más
        if (totalCasillas % 2 != 0) {
            pares.add(paresNecesarios)
        }
        
        pares.shuffle()
        
        // Generar tablero centrado
        val centro = parcela.getCentroTablero()
        val offset = (gridSize - 1) / 2.0
        var indice = 0
        
        for (x in 0 until gridSize) {
            for (z in 0 until gridSize) {
                if (indice >= pares.size) break
                
                val ubicacion = centro.clone().add(
                    (x - offset), 0.0, (z - offset)
                )
                
                val colorMaterial = coloresDisponibles[pares[indice] % coloresDisponibles.size]
                
                val casilla = CasillaMemorias(
                    ubicacion = ubicacion,
                    colorReal = colorMaterial,
                    revelada = false,
                    idPar = pares[indice]
                )
                
                tablero.add(casilla)
                ubicacion.block.type = materialOculto
                
                indice++
            }
        }
    }
    
    /**
     * Maneja el clic de un jugador en un bloque.
     * Esta lógica migra desde PlayerListener.
     * 
     * @return true si el clic fue válido y procesado
     */
    fun handlePlayerClick(jugador: Player, ubicacionClic: Location): Boolean {
        // Validación: Solo permitir clics durante la fase de juego
        if (estado != DueloEstado.JUGANDO) {
            jugador.sendMessage(Component.text("El juego aún no ha comenzado.", NamedTextColor.RED))
            return false
        }
        
        // Validación: Verificar que sea el turno del jugador
        if (jugador.uniqueId != turnoActual) {
            jugador.sendMessage(Component.text("¡No es tu turno!", NamedTextColor.RED))
            return false
        }
        
        // Buscar casilla
        val casilla = buscarCasillaPorUbicacion(ubicacionClic) ?: return false
        
        // Verificar si ya está revelada permanentemente
        if (casilla.revelada) {
            jugador.sendMessage(Component.text("Este par ya fue encontrado.", NamedTextColor.GRAY))
            return false
        }
        
        // Primer o segundo clic
        if (!esperandoSegundoClic) {
            // Primer clic
            revelarCasillaTemporalmente(casilla)
            primerBloque = casilla
            esperandoSegundoClic = true
            jugador.sendMessage(Component.text("Selecciona un segundo bloque.", NamedTextColor.YELLOW))
            jugador.playSound(jugador.location, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.5f, 1.0f)
            return true
        } else {
            // Segundo clic
            if (casilla.ubicacion == primerBloque?.ubicacion) {
                jugador.sendMessage(Component.text("No puedes seleccionar el mismo bloque dos veces.", NamedTextColor.RED))
                return false
            }
            
            revelarCasillaTemporalmente(casilla)
            segundoBloque = casilla
            
            // Verificar par
            verificarPar(jugador)
            return true
        }
    }
    
    /**
     * Verifica si las dos casillas seleccionadas forman un par.
     */
    private fun verificarPar(jugador: Player) {
        val primera = primerBloque ?: return
        val segunda = segundoBloque ?: return
        
        if (primera.idPar == segunda.idPar) {
            // ¡PAR ENCONTRADO!
            primera.revelada = true
            segunda.revelada = true
            
            val puntuacion = (puntuaciones[jugador] ?: 0) + 1
            puntuaciones[jugador] = puntuacion
            
            val mensaje = Component.text("✓ ${jugador.name} encontró un par!", NamedTextColor.GREEN, TextDecoration.BOLD)
                .append(Component.text(" ($puntuacion pares)", NamedTextColor.AQUA))
            jugadores.forEach { it.sendMessage(mensaje) }
            
            jugador.playSound(jugador.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f)
            jugador.playSound(jugador.location, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f)
            
            resetearSeleccion()
            
            // Verificar si se completaron todos los pares
            if (todosLosParesEncontrados()) {
                finalizarDueloPorCompletado()
            }
            
        } else {
            // NO ES PAR
            val mensaje = Component.text("✗ ${jugador.name} no encontró un par", NamedTextColor.RED)
            jugadores.forEach { it.sendMessage(mensaje) }
            
            jugador.playSound(jugador.location, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f)
            
            // Configurar timer para ocultar bloques (usar tiempo de configuración)
            ticksParaOcultar = revealTimeSeconds * 20
            
            // Si la configuración dice que se cambia de turno al fallar, hacerlo
            if (turnChangeOnFail) {
                // El cambio de turno se ejecutará después de ocultar los bloques
                // en ocultarBloquesTemporales()
            }
        }
    }
    
    /**
     * Oculta los bloques revelados temporalmente después del timeout.
     */
    private fun ocultarBloquesTemporales() {
        segundoBloque?.let { ocultarCasilla(it) }
        primerBloque?.let { ocultarCasilla(it) }
        resetearSeleccion()
        
        // Cambiar de turno si la configuración lo indica
        if (turnChangeOnFail) {
            cambiarTurno()
        }
    }
    
    /**
     * Busca una casilla por ubicación.
     */
    private fun buscarCasillaPorUbicacion(ubicacion: Location): CasillaMemorias? {
        return tablero.firstOrNull { casilla ->
            val loc = casilla.ubicacion
            ubicacion.blockX == loc.blockX &&
            ubicacion.blockY == loc.blockY &&
            ubicacion.blockZ == loc.blockZ
        }
    }
    
    /**
     * Revela una casilla temporalmente (muestra su color).
     */
    private fun revelarCasillaTemporalmente(casilla: CasillaMemorias) {
        casilla.ubicacion.block.type = casilla.colorReal
    }
    
    /**
     * Oculta una casilla (vuelve a gris).
     */
    private fun ocultarCasilla(casilla: CasillaMemorias) {
        if (!casilla.revelada) {
            casilla.ubicacion.block.type = materialOculto
        }
    }
    
    /**
     * Resetea la selección actual.
     */
    private fun resetearSeleccion() {
        esperandoSegundoClic = false
        primerBloque = null
        segundoBloque = null
    }
    
    /**
     * Inicia el turno de un jugador específico.
     * Establece turnoActual y notifica a los jugadores.
     */
    private fun iniciarTurno(jugador: Player) {
        turnoActual = jugador.uniqueId
        
        val mensaje = Component.text("¡Turno de ", NamedTextColor.YELLOW, TextDecoration.BOLD)
            .append(Component.text(jugador.name, NamedTextColor.GOLD))
            .append(Component.text("!", NamedTextColor.YELLOW))
        
        jugadores.forEach { it.sendMessage(mensaje) }
        jugador.playSound(jugador.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f)
    }
    
    /**
     * Cambia al siguiente jugador.
     */
    private fun cambiarTurno() {
        val jugadorActualObj = jugadores.find { it.uniqueId == turnoActual }
        val oponente = jugadores.find { it != jugadorActualObj }
        
        if (oponente != null) {
            iniciarTurno(oponente)
        }
    }
    
    /**
     * Maneja el timeout cuando un jugador se queda sin tiempo.
     * El jugador que se quedó sin tiempo pierde automáticamente.
     */
    private fun manejarTimeoutJugador(uuidJugador: UUID) {
        val jugadorSinTiempo = jugadores.find { it.uniqueId == uuidJugador }
        val ganador = jugadores.find { it.uniqueId != uuidJugador }
        
        if (jugadorSinTiempo != null && ganador != null) {
            val mensaje = Component.text("¡${jugadorSinTiempo.name} se quedó sin tiempo!", NamedTextColor.RED, TextDecoration.BOLD)
            jugadores.forEach { it.sendMessage(mensaje) }
            
            finalizarDueloPorTimeout(ganador, jugadorSinTiempo)
        }
    }
    
    /**
     * Actualiza las action bars de todos los jugadores con información del duelo.
     */
    private fun actualizarActionBars() {
        jugadores.forEach { jugador ->
            val puntuacion = puntuaciones[jugador] ?: 0
            val tiempoEnTicks = tiempoRestante[jugador.uniqueId] ?: 0
            val segundosRestantes = tiempoEnTicks / 20
            val minutos = segundosRestantes / 60
            val segundos = segundosRestantes % 60
            val tiempoFormateado = String.format("%d:%02d", minutos, segundos)
            
            val esSuTurno = jugador.uniqueId == turnoActual
            
            val indicadorTurno = if (esSuTurno) {
                Component.text("➤ TU TURNO", NamedTextColor.GOLD, TextDecoration.BOLD)
            } else {
                Component.text("Esperando...", NamedTextColor.GRAY)
            }
            
            val colorTiempo = when {
                segundosRestantes > 60 -> NamedTextColor.GREEN
                segundosRestantes > 30 -> NamedTextColor.YELLOW
                else -> NamedTextColor.RED
            }
            
            val mensaje = indicadorTurno
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("⏱ ", colorTiempo))
                .append(Component.text(tiempoFormateado, colorTiempo, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("⭐ Pares: ", NamedTextColor.AQUA))
                .append(Component.text("$puntuacion", NamedTextColor.WHITE, TextDecoration.BOLD))
            
            jugador.sendActionBar(mensaje)
        }
    }
    
    /**
     * Verifica si todos los pares del tablero fueron encontrados.
     */
    private fun todosLosParesEncontrados(): Boolean {
        return tablero.all { it.revelada }
    }
    
    /**
     * Finaliza el duelo cuando un jugador se queda sin tiempo.
     */
    private fun finalizarDueloPorTimeout(ganador: Player, perdedor: Player) {
        estado = DueloEstado.FINALIZADO
        
        // Mostrar resultados
        val mensajeResultado = Component.text("¡Duelo finalizado por tiempo!", NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text("\n", NamedTextColor.WHITE))
            .append(Component.text("¡${ganador.name} gana por tiempo!", NamedTextColor.GREEN))
        
        jugadores.forEach { it.sendMessage(mensajeResultado) }
        
        // Títulos individuales
        val tituloGanador = Component.text("¡VICTORIA!", NamedTextColor.GOLD, TextDecoration.BOLD)
        val subtituloGanador = Component.text("Tu oponente se quedó sin tiempo", NamedTextColor.YELLOW)
        val tiempos = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
        ganador.showTitle(Title.title(tituloGanador, subtituloGanador, tiempos))
        ganador.playSound(ganador.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
        
        val tituloPerdedor = Component.text("¡DERROTA!", NamedTextColor.RED, TextDecoration.BOLD)
        val subtituloPerdedor = Component.text("¡Te quedaste sin tiempo!", NamedTextColor.GRAY)
        perdedor.showTitle(Title.title(tituloPerdedor, subtituloPerdedor, tiempos))
        perdedor.playSound(perdedor.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f)
    }
    
    /**
     * Finaliza el duelo cuando todos los pares fueron encontrados.
     */
    private fun finalizarDueloPorCompletado() {
        estado = DueloEstado.FINALIZADO
        
        val ganador = puntuaciones.maxByOrNull { it.value }?.key
        val puntos1 = puntuaciones[player1] ?: 0
        val puntos2 = puntuaciones[player2] ?: 0
        
        // Mensaje de resultados
        val mensajeResultado = Component.text("¡Duelo completado!", NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text("\n", NamedTextColor.WHITE))
            .append(Component.text("${player1.name}: ", NamedTextColor.AQUA))
            .append(Component.text("$puntos1 pares\n", NamedTextColor.WHITE))
            .append(Component.text("${player2.name}: ", NamedTextColor.AQUA))
            .append(Component.text("$puntos2 pares", NamedTextColor.WHITE))
        
        jugadores.forEach { it.sendMessage(mensajeResultado) }
        
        if (ganador != null) {
            val perdedor = jugadores.find { it != ganador }
            
            // Títulos individuales
            val tiempos = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
            
            if (puntos1 == puntos2) {
                // Empate
                val tituloEmpate = Component.text("¡EMPATE!", NamedTextColor.YELLOW, TextDecoration.BOLD)
                val subtituloEmpate = Component.text("Ambos encontraron $puntos1 pares", NamedTextColor.GOLD)
                jugadores.forEach { jugador ->
                    jugador.showTitle(Title.title(tituloEmpate, subtituloEmpate, tiempos))
                    jugador.playSound(jugador.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
                }
            } else {
                // Victoria clara
                val tituloGanador = Component.text("¡VICTORIA!", NamedTextColor.GOLD, TextDecoration.BOLD)
                val subtituloGanador = Component.text("¡Encontraste más pares!", NamedTextColor.YELLOW)
                ganador.showTitle(Title.title(tituloGanador, subtituloGanador, tiempos))
                ganador.playSound(ganador.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
                
                if (perdedor != null) {
                    val tituloPerdedor = Component.text("¡DERROTA!", NamedTextColor.RED, TextDecoration.BOLD)
                    val subtituloPerdedor = Component.text("${ganador.name} encontró más pares", NamedTextColor.GRAY)
                    perdedor.showTitle(Title.title(tituloPerdedor, subtituloPerdedor, tiempos))
                    perdedor.playSound(perdedor.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f)
                }
            }
        }
    }
    
    /**
     * Revela todo el tablero (para la fase de memorización).
     */
    private fun revelarTodoElTablero() {
        tablero.forEach { casilla ->
            casilla.ubicacion.block.type = casilla.colorReal
        }
    }
    
    /**
     * Oculta todo el tablero (al iniciar la fase de juego).
     */
    private fun ocultarTodoElTablero() {
        tablero.forEach { casilla ->
            if (!casilla.revelada) {
                casilla.ubicacion.block.type = materialOculto
            }
        }
    }
    
    /**
     * Limpia todos los bloques del tablero.
     */
    fun limpiarTablero() {
        tablero.forEach { casilla ->
            casilla.ubicacion.block.type = Material.AIR
        }
        tablero.clear()
    }
    
    /**
     * Obtiene el estado actual del duelo.
     */
    fun getEstado(): DueloEstado = estado
    
    /**
     * Obtiene el ganador del duelo (solo válido si está finalizado).
     */
    fun getGanador(): Player? {
        if (estado != DueloEstado.FINALIZADO) return null
        return puntuaciones.maxByOrNull { it.value }?.key
    }
    
    /**
     * Obtiene la puntuación de un jugador.
     */
    fun getPuntuacion(jugador: Player): Int = puntuaciones[jugador] ?: 0
    
    /**
     * Verifica si un jugador pertenece a este duelo.
     */
    fun contieneJugador(jugador: Player): Boolean {
        return jugador == player1 || jugador == player2
    }
}
