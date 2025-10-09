package los5fantasticos.minigameCadena.game

import org.bukkit.Location

/**
 * Representa una arena de parkour para el minijuego Cadena.
 * 
 * Contiene todas las ubicaciones necesarias para una partida:
 * - Spawn inicial
 * - Checkpoints intermedios
 * - Línea de meta
 * - Altura mínima (para detectar caídas)
 */
data class Arena(
    /**
     * Nombre único de la arena.
     */
    val name: String,
    
    /**
     * Ubicación de spawn donde aparecen los equipos al iniciar.
     */
    val spawnLocation: Location,
    
    /**
     * Lista de checkpoints en orden.
     * Los equipos respawnean en el último checkpoint alcanzado.
     */
    val checkpoints: MutableList<Location> = mutableListOf(),
    
    /**
     * Ubicación de la línea de meta.
     * Cuando todo el equipo cruza esta ubicación, completan el parkour.
     */
    val finishLocation: Location,
    
    /**
     * Altura mínima (Y) permitida.
     * Si un jugador cae por debajo de esta altura, se considera una caída.
     */
    val minHeight: Double = 0.0,
    
    /**
     * Radio de detección para checkpoints y meta (en bloques).
     * Un jugador debe estar dentro de este radio para activar un checkpoint.
     */
    val detectionRadius: Double = 2.0
) {
    
    /**
     * Verifica si una ubicación está dentro del radio de un checkpoint.
     * 
     * @param location Ubicación a verificar
     * @param checkpoint Checkpoint a comparar
     * @return true si está dentro del radio
     */
    fun isNearCheckpoint(location: Location, checkpoint: Location): Boolean {
        // Verificar que estén en el mismo mundo
        if (location.world != checkpoint.world) {
            return false
        }
        
        // Calcular distancia horizontal (X, Z) ignorando Y
        val dx = location.x - checkpoint.x
        val dz = location.z - checkpoint.z
        val horizontalDistance = kotlin.math.sqrt(dx * dx + dz * dz)
        
        // Verificar distancia vertical (Y)
        val dy = kotlin.math.abs(location.y - checkpoint.y)
        
        // Debe estar cerca horizontalmente y no muy lejos verticalmente
        return horizontalDistance <= detectionRadius && dy <= 3.0
    }
    
    /**
     * Verifica si una ubicación está por debajo de la altura mínima.
     * 
     * @param location Ubicación a verificar
     * @return true si cayó por debajo del límite
     */
    fun isBelowMinHeight(location: Location): Boolean {
        return location.y < minHeight
    }
    
    /**
     * Obtiene el número total de checkpoints.
     */
    fun getCheckpointCount(): Int = checkpoints.size
    
    /**
     * Añade un checkpoint a la arena.
     */
    fun addCheckpoint(location: Location) {
        checkpoints.add(location)
    }
    
    /**
     * Obtiene un checkpoint por índice.
     */
    fun getCheckpoint(index: Int): Location? {
        return checkpoints.getOrNull(index)
    }
}
