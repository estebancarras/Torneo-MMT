package los5fantasticos.memorias

import org.bukkit.Location
import org.bukkit.World

/**
 * Representa una región cúbica definida por dos esquinas opuestas.
 * Utilizada para definir áreas del tablero de juego.
 */
data class Cuboid(
    val world: World,
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int
) {
    /**
     * Verifica si una ubicación está dentro de este cuboid.
     */
    fun contains(location: Location): Boolean {
        if (location.world != world) return false
        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ
        return x in minX..maxX && y in minY..maxY && z in minZ..maxZ
    }
    
    /**
     * Obtiene el centro del cuboid.
     */
    fun getCenter(): Location {
        return Location(
            world,
            (minX + maxX) / 2.0,
            (minY + maxY) / 2.0,
            (minZ + maxZ) / 2.0
        )
    }
    
    companion object {
        /**
         * Crea un Cuboid desde dos locations.
         */
        fun fromLocations(loc1: Location, loc2: Location): Cuboid {
            require(loc1.world == loc2.world) { "Las ubicaciones deben estar en el mismo mundo" }
            
            return Cuboid(
                world = loc1.world!!,
                minX = minOf(loc1.blockX, loc2.blockX),
                minY = minOf(loc1.blockY, loc2.blockY),
                minZ = minOf(loc1.blockZ, loc2.blockZ),
                maxX = maxOf(loc1.blockX, loc2.blockX),
                maxY = maxOf(loc1.blockY, loc2.blockY),
                maxZ = maxOf(loc1.blockZ, loc2.blockZ)
            )
        }
    }
}
