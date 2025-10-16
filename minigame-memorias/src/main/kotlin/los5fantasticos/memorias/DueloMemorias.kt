package los5fantasticos.memorias

import los5fantasticos.memorias.*
import los5fantasticos.torneo.core.TorneoManager
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
 * @property torneoManager Manager del torneo para registrar puntuaciones
 */
class DueloMemorias(
    val player1: Player,
    val player2: Player,
    val parcela: Parcela,
    private val plugin: Plugin,
    private val torneoManager: TorneoManager?
) {
    // ===== CONFIGURACIÓN DEL JUEGO =====
    private val memorizationTimeSeconds: Int
    private val playerTimeSeconds: Int
    private val turnChangeOnFail: Boolean
    private val gridSize: Int
    private val revealTimeSeconds: Int
    private val puntosVictoria: Int
    private val puntosParticipacion: Int
    
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
    
    // ===== SISTEMA DE BLOQUEO ANTI-EXPLOIT =====
    private var aceptandoInput: Boolean = true
    
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
        gridSize = config.getInt("game-settings.grid-size", 10)
        revealTimeSeconds = config.getInt("game-settings.reveal-time-seconds", 2)
        puntosVictoria = config.getInt("puntuacion.por-victoria", 3)
        puntosParticipacion = config.getInt("puntuacion.por-participacion", 1)
        
        // Inicializar temporizadores (en ticks, cada jugador tiene su tiempo)
        tiempoRestante[player1.uniqueId] = playerTimeSeconds * 20
        tiempoRestante[player2.uniqueId] = playerTimeSeconds * 20
        
        // Inicializar tiempo de memorización
        ticksParaMemorizar = memorizationTimeSeconds * 20
        
        // FASE 2: Setup automatizado del duelo
        setupDuelo()
    }
    
    /**
     * FASE 2 [CRÍTICO]: Configuración automatizada del duelo.
     * 
     * CONVENCIÓN SOBRE CONFIGURACIÓN:
     * 1. Calcula el centro geométrico (X, Z) de la parcela
     * 2. Usa la Y mínima como suelo
     * 3. Genera el tablero centrado en ese punto
     * 4. Calcula spawns de jugadores relativos al tablero
     * 5. Teletransporta jugadores
     */
    private fun setupDuelo() {
        // 1. Obtener centro y suelo de la parcela
        val (centroX, centroZ) = parcela.getCentroXZ()
        val ySuelo = parcela.getYSuelo()
        val world = parcela.region.world
        
        // 2. Generar tablero centrado
        val totalCasillas = gridSize * gridSize
        val paresNecesarios = totalCasillas / 2
        
        // Obtener bloques únicos del mazo
        val materialesUnicos = BlockDeckManager.getShuffledDeck(paresNecesarios)
        
        // Crear pares duplicados
        val pares = mutableListOf<Material>()
        materialesUnicos.forEach { material ->
            pares.add(material)
            pares.add(material)
        }
        pares.shuffle()
        
        // Generar tablero en cuadrícula centrada
        val offset = (gridSize - 1) / 2.0
        var indice = 0
        
        for (x in 0 until gridSize) {
            for (z in 0 until gridSize) {
                if (indice >= pares.size) break
                
                val ubicacion = Location(
                    world,
                    centroX + (x - offset),
                    ySuelo.toDouble(),
                    centroZ + (z - offset)
                )
                
                val casilla = CasillaMemorias(
                    ubicacion = ubicacion,
                    colorReal = pares[indice],
                    revelada = false,
                    idPar = indice / 2  // ID del par
                )
                
                tablero.add(casilla)
                indice++
            }
        }
        
        // 3. Calcular spawns de jugadores (2 bloques detrás de bordes opuestos)
        val spawn1 = Location(
            world,
            centroX - offset - 2.5,  // Oeste del tablero
            ySuelo + 1.0,
            centroZ,
            90f,  // Mirando hacia el este (hacia el tablero)
            0f
        )
        
        val spawn2 = Location(
            world,
            centroX + offset + 2.5,  // Este del tablero
            ySuelo + 1.0,
            centroZ,
            -90f,  // Mirando hacia el oeste (hacia el tablero)
            0f
        )
        
        // 4. Teletransportar jugadores
        player1.teleport(spawn1)
        player2.teleport(spawn2)
        
        // 5. Revelar tablero para fase de memorización
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
     * Maneja el clic de un jugador en un bloque.
     * Esta lógica migra desde PlayerListener.
     * 
     * @return true si el clic fue válido y procesado
     */
    fun handlePlayerClick(jugador: Player, ubicacionClic: Location): Boolean {
        // GUARDA DE SEGURIDAD PRINCIPAL: Anti-exploit de múltiples clics
        if (!aceptandoInput || estado != DueloEstado.JUGANDO || jugador.uniqueId != turnoActual) {
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
            
            // BLOQUEO INMEDIATO: Prevenir más clics hasta que se resuelva este turno
            aceptandoInput = false
            
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
        
        // CORRECCIÓN CRÍTICA: Comparar materiales directamente, no idPar
        if (primera.colorReal == segunda.colorReal) {
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
            
            // DESBLOQUEO: Permitir siguiente acción del jugador
            aceptandoInput = true
            
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
        
        // DESBLOQUEO: Permitir input del nuevo jugador (importante para cambios de turno por fallo)
        aceptandoInput = true
        
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
        
        // FASE 3: Registrar puntuación en el torneo
        torneoManager?.let { tm ->
            tm.addScore(ganador.uniqueId, "Memorias", puntosVictoria, "Victoria por timeout")
            tm.addScore(perdedor.uniqueId, "Memorias", puntosParticipacion, "Participación")
            
            plugin.logger.info("[Memorias] Puntuación registrada: ${ganador.name} +$puntosVictoria, ${perdedor.name} +$puntosParticipacion")
        }
        
        // Mostrar resultados
        val mensajeResultado = Component.text("¡Duelo finalizado por tiempo!", NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text("\n", NamedTextColor.WHITE))
            .append(Component.text("¡${ganador.name} gana por tiempo!", NamedTextColor.GREEN))
            .append(Component.text("\n", NamedTextColor.WHITE))
            .append(Component.text("» Puntos: ", NamedTextColor.GRAY))
            .append(Component.text("+$puntosVictoria", NamedTextColor.GREEN))
            .append(Component.text(" (ganador), ", NamedTextColor.GRAY))
            .append(Component.text("+$puntosParticipacion", NamedTextColor.YELLOW))
            .append(Component.text(" (participación)", NamedTextColor.GRAY))
        
        jugadores.forEach { it.sendMessage(mensajeResultado) }
        
        // Títulos individuales
        val tituloGanador = Component.text("¡VICTORIA!", NamedTextColor.GOLD, TextDecoration.BOLD)
        val subtituloGanador = Component.text("Tu oponente se quedó sin tiempo (+$puntosVictoria pts)", NamedTextColor.YELLOW)
        val tiempos = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
        ganador.showTitle(Title.title(tituloGanador, subtituloGanador, tiempos))
        ganador.playSound(ganador.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
        
        val tituloPerdedor = Component.text("¡DERROTA!", NamedTextColor.RED, TextDecoration.BOLD)
        val subtituloPerdedor = Component.text("¡Te quedaste sin tiempo! (+$puntosParticipacion pts)", NamedTextColor.GRAY)
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
        
        // FASE 3: Registrar puntuación en el torneo
        if (puntos1 == puntos2) {
            // Empate - ambos reciben puntos de participación
            torneoManager?.let { tm ->
                tm.addScore(player1.uniqueId, "Memorias", puntosParticipacion, "Empate")
                tm.addScore(player2.uniqueId, "Memorias", puntosParticipacion, "Empate")
                plugin.logger.info("[Memorias] Empate: ${player1.name} +$puntosParticipacion, ${player2.name} +$puntosParticipacion")
            }
        } else if (ganador != null) {
            val perdedor = jugadores.find { it != ganador }
            torneoManager?.let { tm ->
                tm.addScore(ganador.uniqueId, "Memorias", puntosVictoria, "Victoria")
                perdedor?.let { p -> tm.addScore(p.uniqueId, "Memorias", puntosParticipacion, "Participación") }
                plugin.logger.info("[Memorias] Victoria: ${ganador.name} +$puntosVictoria, ${perdedor?.name} +$puntosParticipacion")
            }
        }
        
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
