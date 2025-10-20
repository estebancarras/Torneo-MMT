package los5fantasticos.minigameCarrerabarcos.game

import org.bukkit.entity.Boat
import org.bukkit.entity.Player

/**
 * Representa el estado de un jugador individual en una carrera.
 * 
 * Esta clase encapsula toda la información necesaria para rastrear
 * el progreso y estado de un corredor durante la carrera.
 * 
 * ARQUITECTURA:
 * Esta es una data class pura que solo contiene estado.
 * La lógica de actualización está en GameManager y Carrera.
 * 
 * @property player El jugador de Bukkit
 * @property nextCheckpointIndex Índice del próximo checkpoint que debe atravesar (0-based)
 * @property finishTime Timestamp de cuando cruzó la meta (null si no ha terminado)
 * @property finalPosition Posición final en la carrera (1 = primero, null si no ha terminado)
 * @property boat Barco asignado al jugador (null si no tiene barco)
 * 
 * @author Los 5 Fantásticos
 * @since 2.0
 */
data class RacePlayer(
    /**
     * El jugador de Minecraft.
     */
    val player: Player,
    
    /**
     * Índice del próximo checkpoint que debe atravesar.
     * 
     * - 0 = Debe pasar el primer checkpoint
     * - 1 = Debe pasar el segundo checkpoint
     * - N = Debe pasar el checkpoint N+1
     * 
     * Cuando nextCheckpointIndex == totalCheckpoints, el jugador
     * ha completado todos los checkpoints y puede cruzar la meta.
     */
    var nextCheckpointIndex: Int = 0,
    
    /**
     * Timestamp (System.currentTimeMillis()) de cuando el jugador cruzó la meta.
     * 
     * null = El jugador aún no ha terminado la carrera.
     */
    var finishTime: Long? = null,
    
    /**
     * Posición final del jugador en la carrera.
     * 
     * - 1 = Primer lugar
     * - 2 = Segundo lugar
     * - 3 = Tercer lugar
     * - null = Aún no ha terminado
     */
    var finalPosition: Int? = null,
    
    /**
     * Barco asignado al jugador.
     * 
     * null = El jugador no tiene barco asignado (aún no ha iniciado).
     */
    var boat: Boat? = null
) {
    
    /**
     * Verifica si el jugador ha terminado la carrera.
     */
    fun hasFinished(): Boolean {
        return finalPosition != null
    }
    
    /**
     * Verifica si el jugador ha completado todos los checkpoints.
     * 
     * @param totalCheckpoints Número total de checkpoints en la pista
     */
    fun hasCompletedAllCheckpoints(totalCheckpoints: Int): Boolean {
        return nextCheckpointIndex >= totalCheckpoints
    }
    
    /**
     * Avanza al siguiente checkpoint.
     * 
     * @return true si avanzó, false si ya había completado todos
     */
    fun advanceCheckpoint(totalCheckpoints: Int): Boolean {
        if (nextCheckpointIndex >= totalCheckpoints) {
            return false
        }
        nextCheckpointIndex++
        return true
    }
    
    /**
     * Marca al jugador como finalizado.
     * 
     * @param position Posición final (1 = primero)
     * @param timestamp Timestamp de finalización
     */
    fun markAsFinished(position: Int, timestamp: Long = System.currentTimeMillis()) {
        finalPosition = position
        finishTime = timestamp
    }
    
    /**
     * Obtiene el progreso del jugador como porcentaje (0.0 a 1.0).
     * 
     * @param totalCheckpoints Número total de checkpoints
     */
    fun getProgressPercentage(totalCheckpoints: Int): Double {
        if (totalCheckpoints == 0) return 0.0
        return (nextCheckpointIndex.toDouble() / totalCheckpoints.toDouble()).coerceIn(0.0, 1.0)
    }
    
    /**
     * Obtiene una representación legible del estado del jugador.
     */
    override fun toString(): String {
        return "RacePlayer(${player.name}, checkpoint=$nextCheckpointIndex, finished=${hasFinished()}, position=$finalPosition)"
    }
}
