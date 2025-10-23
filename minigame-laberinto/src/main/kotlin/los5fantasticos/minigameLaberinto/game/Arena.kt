package los5fantasticos.minigameLaberinto.game

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.util.BoundingBox

/**
 * Representa una arena de laberinto configurable.
 * 
 * Contiene toda la información necesaria para ejecutar una partida:
 * - Ubicaciones de inicio y meta
 * - Zonas de jumpscares
 * - Configuración de la partida
 */
data class Arena(
    val name: String,
    val world: World,
    val startLocation: Location,
    val finishRegion: BoundingBox,
    val jumpscareLocations: List<Location>,
    val gameDuration: Int = 300, // 5 minutos por defecto
    val minPlayers: Int = 2,
    val maxPlayers: Int = 8,
    val spectatorBounds: BoundingBox? = null
) {
    
    /**
     * Verifica si una ubicación está dentro de la región de meta.
     * 
     * @param location Ubicación a verificar
     * @return true si está en la meta, false en caso contrario
     */
    fun isInFinishRegion(location: Location): Boolean {
        return finishRegion.contains(location.x, location.y, location.z)
    }
    
    /**
     * Encuentra la zona de jumpscare más cercana a una ubicación.
     * 
     * @param location Ubicación de referencia
     * @param maxDistance Distancia máxima para considerar (por defecto 2 bloques)
     * @return La ubicación de jumpscare más cercana, o null si no hay ninguna en rango
     */
    fun getNearestJumpscareLocation(location: Location, maxDistance: Double = 2.0): Location? {
        return jumpscareLocations
            .filter { jumpscareLoc ->
                jumpscareLoc.world == location.world &&
                jumpscareLoc.distance(location) <= maxDistance
            }
            .minByOrNull { it.distance(location) }
    }
    
    
    /**
     * Verifica si la arena está completamente configurada.
     * 
     * @return true si tiene todos los elementos necesarios, false en caso contrario
     */
    fun isComplete(): Boolean {
        return startLocation.world != null &&
        jumpscareLocations.isNotEmpty()
    }
    
    /**
     * Verifica si una ubicación está dentro de los límites de espectador.
     * 
     * @param location Ubicación a verificar
     * @return true si está dentro de los límites o si no hay límites configurados
     */
    fun isWithinSpectatorBounds(location: Location): Boolean {
        return spectatorBounds?.contains(location.x, location.y, location.z) ?: true
    }
    
}
