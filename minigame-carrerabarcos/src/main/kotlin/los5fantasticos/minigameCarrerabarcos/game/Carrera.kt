package los5fantasticos.minigameCarrerabarcos.game

import org.bukkit.entity.Boat
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Representa una carrera activa en curso.
 * 
 * RESPONSABILIDADES:
 * - Rastrear el progreso de cada jugador (qué checkpoint deben pasar a continuación)
 * - Mantener el estado de la carrera (iniciada, en progreso, finalizada)
 * - Gestionar los barcos de los jugadores
 * 
 * ARQUITECTURA:
 * Esta clase es principalmente datos + estado.
 * La lógica de negocio (iniciar, finalizar, otorgar puntos) está en GameManager.
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
     * Mapa de progreso de jugadores.
     * UUID del jugador -> Índice del próximo checkpoint que debe atravesar.
     * 
     * Cuando un jugador atraviesa un checkpoint, su valor se incrementa.
     * Cuando el valor es igual a arena.checkpoints.size, el jugador ha pasado todos
     * los checkpoints y puede cruzar la meta.
     */
    private val progresoJugadores = mutableMapOf<UUID, Int>()
    
    /**
     * Jugadores participantes en la carrera.
     */
    private val jugadores = mutableSetOf<Player>()
    
    /**
     * Barcos asignados a cada jugador.
     */
    private val barcosJugadores = mutableMapOf<Player, Boat>()
    
    /**
     * Jugadores que han finalizado la carrera, en orden de llegada.
     */
    private val jugadoresFinalizados = mutableListOf<Player>()
    
    /**
     * Timestamp de inicio de la carrera.
     */
    var tiempoInicio: Long = 0
        private set
    
    /**
     * Añade un jugador a la carrera.
     */
    fun addJugador(player: Player) {
        jugadores.add(player)
        progresoJugadores[player.uniqueId] = 0 // Comienza en el checkpoint 0
    }
    
    /**
     * Remueve un jugador de la carrera.
     */
    fun removeJugador(player: Player) {
        jugadores.remove(player)
        progresoJugadores.remove(player.uniqueId)
        
        // Remover y eliminar su barco si existe
        barcosJugadores.remove(player)?.remove()
    }
    
    /**
     * Obtiene todos los jugadores en la carrera.
     */
    fun getJugadores(): Set<Player> {
        return jugadores.toSet()
    }
    
    /**
     * Verifica si un jugador está en esta carrera.
     */
    fun hasJugador(player: Player): Boolean {
        return jugadores.contains(player)
    }
    
    /**
     * Obtiene el progreso de un jugador (índice del próximo checkpoint).
     */
    fun getProgreso(player: Player): Int {
        return progresoJugadores[player.uniqueId] ?: 0
    }
    
    /**
     * Avanza el progreso de un jugador al siguiente checkpoint.
     * 
     * @return true si el jugador avanzó, false si ya había pasado este checkpoint
     */
    fun avanzarProgreso(player: Player): Boolean {
        val progresoActual = progresoJugadores[player.uniqueId] ?: return false
        
        // Verificar que no haya terminado ya
        if (progresoActual >= arena.checkpoints.size) {
            return false
        }
        
        progresoJugadores[player.uniqueId] = progresoActual + 1
        return true
    }
    
    /**
     * Verifica si un jugador ha pasado todos los checkpoints y puede cruzar la meta.
     */
    fun puedeFinalizarCarrera(player: Player): Boolean {
        val progreso = progresoJugadores[player.uniqueId] ?: return false
        return progreso >= arena.checkpoints.size
    }
    
    /**
     * Marca a un jugador como finalizado.
     * 
     * @return La posición del jugador (1 = primero, 2 = segundo, etc.)
     */
    fun finalizarJugador(player: Player): Int {
        if (!jugadoresFinalizados.contains(player)) {
            jugadoresFinalizados.add(player)
        }
        return jugadoresFinalizados.indexOf(player) + 1
    }
    
    /**
     * Obtiene la lista de jugadores finalizados en orden.
     */
    fun getJugadoresFinalizados(): List<Player> {
        return jugadoresFinalizados.toList()
    }
    
    /**
     * Verifica si todos los jugadores han finalizado.
     */
    fun todosFinalizaron(): Boolean {
        return jugadoresFinalizados.size == jugadores.size && jugadores.isNotEmpty()
    }
    
    /**
     * Asigna un barco a un jugador.
     */
    fun setBarco(player: Player, boat: Boat) {
        barcosJugadores[player] = boat
    }
    
    /**
     * Obtiene el barco de un jugador.
     */
    fun getBarco(player: Player): Boat? {
        return barcosJugadores[player]
    }
    
    /**
     * Cambia el estado de la carrera.
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
        return jugadores.size
    }
    
    /**
     * Limpia todos los barcos de la carrera.
     */
    fun limpiarBarcos() {
        barcosJugadores.values.forEach { boat ->
            boat.remove()
        }
        barcosJugadores.clear()
    }
    
    /**
     * Obtiene información de progreso de todos los jugadores.
     */
    fun getEstadisticas(): String {
        return buildString {
            appendLine("Carrera en ${arena.nombre}")
            appendLine("Estado: $estado")
            appendLine("Jugadores: ${jugadores.size}")
            appendLine("Finalizados: ${jugadoresFinalizados.size}")
            appendLine("Progreso:")
            for (player in jugadores) {
                val progreso = progresoJugadores[player.uniqueId] ?: 0
                val totalCheckpoints = arena.checkpoints.size
                appendLine("  ${player.name}: $progreso/$totalCheckpoints checkpoints")
            }
        }
    }
}
