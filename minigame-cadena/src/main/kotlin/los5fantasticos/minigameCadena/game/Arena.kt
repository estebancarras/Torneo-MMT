package los5fantasticos.minigameCadena.game

import org.bukkit.Location

/**
 * Representa una arena de parkour para el minijuego Cadena.
 * 
 * Contiene todas las ubicaciones necesarias para una partida:
 * - Spawns iniciales (uno por equipo)
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
     * DEPRECATED: Usar spawnLocations en su lugar.
     */
    @Deprecated("Usar spawnLocations para soportar múltiples equipos")
    val spawnLocation: Location,
    
    /**
     * Lista de ubicaciones de spawn para cada equipo.
     * Cada equipo será teletransportado a un spawn diferente.
     * Si está vacía, se usa spawnLocation como fallback.
     */
    val spawnLocations: MutableList<Location> = mutableListOf(),
    
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
    
    /**
     * Añade una ubicación de spawn para equipos.
     */
    fun addSpawnLocation(location: Location) {
        spawnLocations.add(location)
    }
    
    /**
     * Obtiene una ubicación de spawn por índice.
     * Si no hay spawns configurados, devuelve el spawn principal.
     */
    fun getSpawnLocation(index: Int): Location {
        return if (spawnLocations.isEmpty()) {
            spawnLocation
        } else {
            spawnLocations.getOrNull(index % spawnLocations.size) ?: spawnLocation
        }
    }
    
    /**
     * Limpia todas las ubicaciones de spawn.
     */
    fun clearSpawnLocations() {
        spawnLocations.clear()
    }
}
