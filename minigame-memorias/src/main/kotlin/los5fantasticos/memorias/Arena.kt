package los5fantasticos.memorias

import org.bukkit.Location

data class Arena(
    val spawnLocation: Location,
    val tableroLocation: Location,
    val guessArea: Location
)