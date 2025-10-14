package los5fantasticos.memorias

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.time.Duration

/**
 * Representa un duelo individual de Memorias entre dos jugadores.
 * Esta clase NO crea BukkitTasks - toda su lógica es impulsada por el Game Loop.
 * 
 * @property player1 Primer jugador del duelo
 * @property player2 Segundo jugador del duelo
 * @property parcela Parcela donde se desarrolla el duelo
 * @property gridSize Tamaño del tablero (default 5x5)
 */
class DueloMemorias(
    val player1: Player,
    val player2: Player,
    val parcela: Parcela,
    private val gridSize: Int = 5
) {
    // Estado del duelo
    private var estado: EstadoDuelo = EstadoDuelo.PREPARANDO
    private var ticksTranscurridos = 0
    
    // Tablero de juego
    private val tablero = mutableListOf<CasillaMemorias>()
    private val materialOculto = Material.GRAY_WOOL
    
    // Sistema de turnos
    private var jugadorActualIndex = 0
    private val jugadores = listOf(player1, player2)
    
    // Puntuaciones y estadísticas
    private val puntuaciones = mutableMapOf(player1 to 0, player2 to 0)
    private val intentosUsados = mutableMapOf(player1 to 0, player2 to 0)
    private val maxIntentos = 4
    
    // Estado de selección (para el par)
    private var esperandoSegundoClic = false
    private var primerBloque: CasillaMemorias? = null
    private var segundoBloque: CasillaMemorias? = null
    
    // Control de tiempo para ocultar bloques (en ticks)
    private var ticksParaOcultar = 0
    
    // Temporizador de turno
    private var ticksTurnoRestantes = 30 * 20 // 30 segundos en ticks
    
    // Colores disponibles
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
    
    /**
     * Estados posibles del duelo.
     */
    enum class EstadoDuelo {
        PREPARANDO,     // Cuenta regresiva inicial
        JUGANDO,        // Juego activo
        FINALIZADO      // Duelo terminado
    }
    
    init {
        // Teleportar jugadores a sus spawns
        player1.teleport(parcela.spawn1)
        player2.teleport(parcela.spawn2)
        
        // Generar tablero
        generarTablero()
        
        // Inicializar intentos
        intentosUsados[player1] = 0
        intentosUsados[player2] = 0
    }
    
    /**
     * Método principal llamado por el Game Loop cada 2 ticks.
     * PROHIBIDO crear BukkitTasks aquí.
     */
    fun actualizar() {
        when (estado) {
            EstadoDuelo.PREPARANDO -> actualizarPreparacion()
            EstadoDuelo.JUGANDO -> actualizarJuego()
            EstadoDuelo.FINALIZADO -> {}  // No hace nada, espera a ser removido
        }
        
        ticksTranscurridos++
    }
    
    /**
     * Actualiza la fase de preparación (cuenta regresiva).
     */
    private fun actualizarPreparacion() {
        // Cuenta regresiva: 3 segundos = 60 ticks
        val segundosRestantes = 3 - (ticksTranscurridos / 20)
        
        when {
            ticksTranscurridos % 20 == 0 && segundosRestantes > 0 -> {
                // Cada segundo, mostrar número
                jugadores.forEach { jugador ->
                    val titulo = Component.text("$segundosRestantes", NamedTextColor.GOLD, TextDecoration.BOLD)
                    val subtitulo = Component.text("Preparándose...", NamedTextColor.YELLOW)
                    val tiempos = Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(750), Duration.ofMillis(250))
                    jugador.showTitle(Title.title(titulo, subtitulo, tiempos))
                    jugador.playSound(jugador.location, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f)
                }
            }
            ticksTranscurridos >= 60 -> {
                // Iniciar juego
                estado = EstadoDuelo.JUGANDO
                ticksTranscurridos = 0
                
                jugadores.forEach { jugador ->
                    val titulo = Component.text("¡COMENZÓ!", NamedTextColor.GREEN, TextDecoration.BOLD)
                    val subtitulo = Component.text("¡Encuentra los pares! (4 intentos)", NamedTextColor.YELLOW)
                    val tiempos = Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(1), Duration.ofMillis(250))
                    jugador.showTitle(Title.title(titulo, subtitulo, tiempos))
                    jugador.playSound(jugador.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
                }
                
                anunciarTurno()
            }
        }
    }
    
    /**
     * Actualiza la fase de juego activo.
     */
    private fun actualizarJuego() {
        // Manejar ocultación de bloques temporales
        if (ticksParaOcultar > 0) {
            ticksParaOcultar--
            if (ticksParaOcultar == 0) {
                ocultarBloquesTemporales()
            }
        }
        
        // Actualizar temporizador de turno
        if (ticksTurnoRestantes > 0) {
            ticksTurnoRestantes--
            
            // Actualizar hotbar cada 10 ticks
            if (ticksTurnoRestantes % 10 == 0) {
                actualizarHotbar()
            }
            
            // Timeout de turno
            if (ticksTurnoRestantes == 0) {
                manejarTimeoutTurno()
            }
        }
        
        // Verificar si el duelo terminó
        if (todosJugadoresTerminaron()) {
            finalizarDuelo()
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
        if (estado != EstadoDuelo.JUGANDO) {
            jugador.sendMessage(Component.text("El juego aún no ha comenzado.", NamedTextColor.RED))
            return false
        }
        
        // Verificar turno
        val jugadorActual = jugadores[jugadorActualIndex]
        if (jugadorActual != jugador) {
            jugador.sendMessage(Component.text("¡No es tu turno! Espera a que ${jugadorActual.name} termine.", NamedTextColor.RED))
            return false
        }
        
        // Verificar intentos
        val intentos = intentosUsados[jugador] ?: 0
        if (intentos >= maxIntentos) {
            jugador.sendMessage(Component.text("¡Ya usaste tus $maxIntentos intentos!", NamedTextColor.RED))
            siguienteTurno()
            return false
        }
        
        // Buscar casilla
        val casilla = buscarCasillaPorUbicacion(ubicacionClic) ?: return false
        
        // Verificar si ya está revelada
        if (casilla.revelada) {
            jugador.sendMessage(Component.text("Este par ya fue encontrado.", NamedTextColor.RED))
            return false
        }
        
        // Primer o segundo clic
        if (!esperandoSegundoClic) {
            // Primer clic
            revelarCasillaTemporalmente(casilla)
            primerBloque = casilla
            esperandoSegundoClic = true
            jugador.sendMessage(Component.text("Selecciona un segundo bloque.", NamedTextColor.YELLOW))
            return true
        } else {
            // Segundo clic
            if (casilla.ubicacion == primerBloque?.ubicacion) {
                jugador.sendMessage(Component.text("No puedes seleccionar el mismo bloque dos veces.", NamedTextColor.RED))
                return false
            }
            
            revelarCasillaTemporalmente(casilla)
            segundoBloque = casilla
            
            // Incrementar intentos
            intentosUsados[jugador] = intentos + 1
            
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
            
            val mensaje = Component.text("✓ ${jugador.name} encontró un par! ($puntuacion pares)", NamedTextColor.GREEN, TextDecoration.BOLD)
            jugadores.forEach { it.sendMessage(mensaje) }
            
            jugador.playSound(jugador.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f)
            jugador.playSound(jugador.location, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f)
            
            resetearSeleccion()
            jugador.sendMessage(Component.text("¡Encontraste un par! Puedes seguir jugando.", NamedTextColor.GREEN))
            
        } else {
            // NO ES PAR - programar ocultación en 2 segundos
            val intentosRestantes = maxIntentos - intentosUsados[jugador]!!
            val mensaje = Component.text("✗ ${jugador.name} no encontró un par. ($intentosRestantes intentos restantes)", NamedTextColor.RED)
            jugadores.forEach { it.sendMessage(mensaje) }
            
            jugador.playSound(jugador.location, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f)
            
            // Configurar timer para ocultar en 40 ticks (2 segundos)
            ticksParaOcultar = 40
        }
    }
    
    /**
     * Oculta los bloques revelados temporalmente después del timeout.
     */
    private fun ocultarBloquesTemporales() {
        segundoBloque?.let { ocultarCasilla(it) }
        primerBloque?.let { ocultarCasilla(it) }
        resetearSeleccion()
        siguienteTurno()
        anunciarTurno()
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
     * Avanza al siguiente turno.
     */
    private fun siguienteTurno() {
        jugadorActualIndex = (jugadorActualIndex + 1) % jugadores.size
        ticksTurnoRestantes = 30 * 20 // Reset timer
        resetearSeleccion()
    }
    
    /**
     * Anuncia el turno actual.
     */
    private fun anunciarTurno() {
        val jugadorActual = jugadores[jugadorActualIndex]
        val mensaje = Component.text("Turno de: ${jugadorActual.name}", NamedTextColor.YELLOW, TextDecoration.BOLD)
        jugadores.forEach { it.sendMessage(mensaje) }
    }
    
    /**
     * Maneja el timeout de un turno.
     */
    private fun manejarTimeoutTurno() {
        val jugadorActual = jugadores[jugadorActualIndex]
        val mensaje = Component.text("¡Se agotó el tiempo del turno de ${jugadorActual.name}!", NamedTextColor.RED)
        jugadores.forEach { it.sendMessage(mensaje) }
        
        resetearSeleccion()
        siguienteTurno()
        anunciarTurno()
    }
    
    /**
     * Actualiza el hotbar de todos los jugadores.
     */
    private fun actualizarHotbar() {
        val jugadorActual = jugadores[jugadorActualIndex]
        val segundosRestantes = ticksTurnoRestantes / 20
        
        jugadores.forEach { jugador ->
            val intentos = intentosUsados[jugador] ?: 0
            val puntuacion = puntuaciones[jugador] ?: 0
            val intentosRestantes = maxIntentos - intentos
            
            val colorIntentos = when {
                intentosRestantes > 2 -> NamedTextColor.GREEN
                intentosRestantes > 0 -> NamedTextColor.YELLOW
                else -> NamedTextColor.RED
            }
            
            val indicadorTurno = if (jugador == jugadorActual) {
                Component.text("TU TURNO", NamedTextColor.GOLD, TextDecoration.BOLD)
            } else {
                Component.text("Esperando...", NamedTextColor.GRAY)
            }
            
            val colorTiempo = when {
                segundosRestantes > 20 -> NamedTextColor.GREEN
                segundosRestantes > 10 -> NamedTextColor.YELLOW
                else -> NamedTextColor.RED
            }
            
            val mensaje = indicadorTurno
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.text("Tiempo: ", colorTiempo, TextDecoration.BOLD))
                .append(Component.text("${segundosRestantes}s", NamedTextColor.WHITE))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.text("Pares: ", NamedTextColor.AQUA))
                .append(Component.text("$puntuacion", NamedTextColor.WHITE))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.text("Intentos: ", colorIntentos, TextDecoration.BOLD))
                .append(Component.text("$intentosRestantes/$maxIntentos", colorIntentos))
            
            jugador.sendActionBar(mensaje)
        }
    }
    
    /**
     * Verifica si todos los jugadores terminaron sus intentos.
     */
    private fun todosJugadoresTerminaron(): Boolean {
        return jugadores.all { (intentosUsados[it] ?: 0) >= maxIntentos }
    }
    
    /**
     * Finaliza el duelo y determina el ganador.
     */
    private fun finalizarDuelo() {
        estado = EstadoDuelo.FINALIZADO
        
        val ganador = puntuaciones.maxByOrNull { it.value }?.key
        
        if (ganador != null) {
            jugadores.forEach { jugador ->
                if (jugador == ganador) {
                    val titulo = Component.text("¡VICTORIA!", NamedTextColor.GOLD, TextDecoration.BOLD)
                    val subtitulo = Component.text("¡Ganaste el duelo!", NamedTextColor.YELLOW)
                    val tiempos = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                    jugador.showTitle(Title.title(titulo, subtitulo, tiempos))
                    jugador.playSound(jugador.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
                } else {
                    val titulo = Component.text("¡PERDISTE!", NamedTextColor.RED, TextDecoration.BOLD)
                    val subtitulo = Component.text("${ganador.name} ganó el duelo", NamedTextColor.GRAY)
                    val tiempos = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                    jugador.showTitle(Title.title(titulo, subtitulo, tiempos))
                    jugador.playSound(jugador.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f)
                }
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
    fun getEstado(): EstadoDuelo = estado
    
    /**
     * Obtiene el ganador del duelo (solo válido si está finalizado).
     */
    fun getGanador(): Player? {
        if (estado != EstadoDuelo.FINALIZADO) return null
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
