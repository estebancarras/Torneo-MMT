package los5fantasticos.minigameCarrerabarcos.game

import org.bukkit.entity.Boat
import org.bukkit.entity.Player

/**
 * Representa una carrera activa en curso (Race).
 * 
 * RESPONSABILIDADES:
 * - Gestionar la lista de participantes (RacePlayer)
 * - Mantener el estado de la carrera (iniciada, en progreso, finalizada)
 * - Coordinar el progreso de todos los jugadores
 * 
 * ARQUITECTURA:
 * Esta clase es principalmente un contenedor de estado.
 * La lógica de negocio (iniciar, finalizar, otorgar puntos) está en GameManager.
 * Usa RacePlayer para encapsular el estado individual de cada corredor.
 * 
 * @property arena Arena/circuito en el que se está corriendo
 * 
 * @author Los 5 Fantásticos
 * @since 2.0
 */
class Carrera(
    /**
     * Arena/circuito en el que se está corriendo.
     */
    val arena: ArenaCarrera
) {
    
    /**
     * Estado de la carrera.
     */
    enum class EstadoCarrera {
        ESPERANDO,      // En lobby, esperando jugadores
        INICIANDO,      // Countdown antes de empezar
        EN_CURSO,       // Carrera activa
        FINALIZADA      // Carrera terminada
    }
    
    /**
     * Estado actual de la carrera.
     */
    var estado: EstadoCarrera = EstadoCarrera.ESPERANDO
        private set
    
    /**
     * Lista de participantes en la carrera.
     * Cada RacePlayer contiene el estado completo de un corredor.
     */
    private val racePlayers = mutableMapOf<Player, RacePlayer>()
    
    /**
     * Timestamp de inicio de la carrera.
     */
    var tiempoInicio: Long = 0
        private set
    
    /**
     * Añade un jugador a la carrera.
     * 
     * @param player Jugador a añadir
     * @return El RacePlayer creado
     */
    fun addJugador(player: Player): RacePlayer {
        val racePlayer = RacePlayer(player = player)
        racePlayers[player] = racePlayer
        return racePlayer
    }
    
    /**
     * Remueve un jugador de la carrera.
     * 
     * @param player Jugador a remover
     */
    fun removeJugador(player: Player) {
        val racePlayer = racePlayers.remove(player)
        
        // Remover y eliminar su barco si existe
        racePlayer?.boat?.remove()
    }
    
    /**
     * Obtiene todos los jugadores en la carrera.
     */
    fun getJugadores(): Set<Player> {
        return racePlayers.keys.toSet()
    }
    
    /**
     * Obtiene el RacePlayer de un jugador.
     * 
     * @param player Jugador
     * @return RacePlayer o null si no está en la carrera
     */
    fun getRacePlayer(player: Player): RacePlayer? {
        return racePlayers[player]
    }
    
    /**
     * Verifica si un jugador está en esta carrera.
     */
    fun hasJugador(player: Player): Boolean {
        return racePlayers.containsKey(player)
    }
    
    /**
     * Obtiene el progreso de un jugador (índice del próximo checkpoint).
     * 
     * @param player Jugador
     * @return Índice del próximo checkpoint (0-based)
     */
    fun getProgreso(player: Player): Int {
        return racePlayers[player]?.nextCheckpointIndex ?: 0
    }
    
    /**
     * Avanza el progreso de un jugador al siguiente checkpoint.
     * 
     * @param player Jugador
     * @return true si el jugador avanzó, false si ya había pasado este checkpoint
     */
    fun avanzarProgreso(player: Player): Boolean {
        val racePlayer = racePlayers[player] ?: return false
        return racePlayer.advanceCheckpoint(arena.checkpoints.size)
    }
    
    /**
     * Verifica si un jugador ha pasado todos los checkpoints y puede cruzar la meta.
     * 
     * @param player Jugador
     * @return true si puede finalizar
     */
    fun puedeFinalizarCarrera(player: Player): Boolean {
        val racePlayer = racePlayers[player] ?: return false
        return racePlayer.hasCompletedAllCheckpoints(arena.checkpoints.size)
    }
    
    /**
     * Marca a un jugador como finalizado.
     * 
     * @param player Jugador que finalizó
     * @return La posición del jugador (1 = primero, 2 = segundo, etc.)
     */
    fun finalizarJugador(player: Player): Int {
        val racePlayer = racePlayers[player] ?: return -1
        
        // Calcular posición (contar cuántos ya terminaron + 1)
        val posicion = racePlayers.values.count { it.hasFinished() } + 1
        
        // Marcar como finalizado
        racePlayer.markAsFinished(posicion)
        
        return posicion
    }
    
    /**
     * Obtiene la lista de jugadores finalizados en orden.
     */
    fun getJugadoresFinalizados(): List<Player> {
        return racePlayers.values
            .filter { it.hasFinished() }
            .sortedBy { it.finalPosition }
            .map { it.player }
    }
    
    /**
     * Verifica si todos los jugadores han finalizado.
     */
    fun todosFinalizaron(): Boolean {
        if (racePlayers.isEmpty()) return false
        return racePlayers.values.all { it.hasFinished() }
    }
    
    /**
     * Asigna un barco a un jugador.
     * 
     * @param player Jugador
     * @param boat Barco a asignar
     */
    fun setBarco(player: Player, boat: Boat) {
        racePlayers[player]?.boat = boat
    }
    
    /**
     * Obtiene el barco de un jugador.
     * 
     * @param player Jugador
     * @return Barco o null si no tiene
     */
    fun getBarco(player: Player): Boat? {
        return racePlayers[player]?.boat
    }
    
    /**
     * Cambia el estado de la carrera.
     * 
     * @param nuevoEstado Nuevo estado
     */
    fun setEstado(nuevoEstado: EstadoCarrera) {
        estado = nuevoEstado
        
        if (nuevoEstado == EstadoCarrera.EN_CURSO) {
            tiempoInicio = System.currentTimeMillis()
        }
    }
    
    /**
     * Obtiene el número de jugadores en la carrera.
     */
    fun getCantidadJugadores(): Int {
        return racePlayers.size
    }
    
    /**
     * Limpia todos los barcos de la carrera.
     */
    fun limpiarBarcos() {
        racePlayers.values.forEach { racePlayer ->
            racePlayer.boat?.remove()
            racePlayer.boat = null
        }
    }
    
    /**
     * Obtiene información de progreso de todos los jugadores.
     */
    fun getEstadisticas(): String {
        return buildString {
            appendLine("Carrera en ${arena.nombre}")
            appendLine("Estado: $estado")
            appendLine("Jugadores: ${racePlayers.size}")
            appendLine("Finalizados: ${getJugadoresFinalizados().size}")
            appendLine("Progreso:")
            
            for (racePlayer in racePlayers.values) {
                val progreso = racePlayer.nextCheckpointIndex
                val totalCheckpoints = arena.checkpoints.size
                val status = if (racePlayer.hasFinished()) {
                    "Finalizado (#${racePlayer.finalPosition})"
                } else {
                    "$progreso/$totalCheckpoints checkpoints"
                }
                appendLine("  ${racePlayer.player.name}: $status")
            }
        }
    }
    
    /**
     * Obtiene todos los RacePlayers.
     * Útil para operaciones que necesitan acceso directo al estado.
     */
    fun getAllRacePlayers(): Collection<RacePlayer> {
        return racePlayers.values
    }
}
