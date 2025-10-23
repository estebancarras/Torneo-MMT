package yo.spray.robarCabeza.game

import los5fantasticos.torneo.util.selection.Cuboid
import org.bukkit.Location

/**
 * Representa una arena de juego para RobarCabeza.
 * 
 * @property name Nombre único de la arena
 * @property spawns Lista de ubicaciones de spawn para los jugadores
 * @property playRegion Región cúbica que delimita el área de juego
 */
data class Arena(
    val name: String,
    val spawns: MutableList<Location> = mutableListOf(),
    var playRegion: Cuboid? = null
)
