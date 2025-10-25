package los5fantasticos.minigameColiseo.game

import los5fantasticos.torneo.util.selection.Cuboid
import org.bukkit.Location

/**
 * Representa una arena del Coliseo.
 * 
 * @property name Nombre único de la arena
 * @property eliteSpawns Puntos de aparición para el equipo Élite
 * @property hordeSpawns Puntos de aparición para el equipo Horda
 * @property playRegion Región de juego (límites de la arena)
 */
data class Arena(
    val name: String,
    val eliteSpawns: MutableList<Location> = mutableListOf(),
    val hordeSpawns: MutableList<Location> = mutableListOf(),
    var spectatorSpawn: Location? = null,
    var playRegion: Cuboid? = null
) {
    /**
     * Obtiene un spawn aleatorio para el equipo Élite.
     */
    fun getRandomEliteSpawn(): Location? {
        return eliteSpawns.randomOrNull()?.clone()
    }
    
    /**
     * Obtiene un spawn aleatorio para el equipo Horda.
     */
    fun getRandomHordeSpawn(): Location? {
        return hordeSpawns.randomOrNull()?.clone()
    }
    
    /**
     * Obtiene el spawn para espectadores.
     * Si no está configurado, usa el centro de la arena o un spawn aleatorio.
     */
    fun getSpectatorSpawnLocation(): Location? {
        return spectatorSpawn?.clone() 
            ?: playRegion?.getCenter() 
            ?: eliteSpawns.firstOrNull()?.clone()
    }
    
    /**
     * Verifica si la arena está completamente configurada.
     */
    fun isValid(): Boolean {
        return eliteSpawns.isNotEmpty() && 
               hordeSpawns.isNotEmpty() && 
               playRegion != null
    }
}
