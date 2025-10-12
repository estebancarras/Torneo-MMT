package los5fantasticos.torneo.listeners

import los5fantasticos.torneo.services.GlobalScoreboardService
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

/**
 * Listener para eventos de conexión de jugadores.
 * 
 * Responsabilidades:
 * - Asignar el scoreboard global a jugadores cuando se conectan
 */
class PlayerConnectionListener(
    private val scoreboardService: GlobalScoreboardService
) : Listener {
    
    /**
     * Maneja el evento de conexión de un jugador.
     * Asigna el scoreboard global al jugador.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // Asignar el scoreboard global al jugador
        scoreboardService.showToPlayer(player)
    }
}
