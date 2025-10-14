package los5fantasticos.minigameLaberinto.listeners

import los5fantasticos.minigameLaberinto.MinigameLaberinto
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Listener para manejar cuando los jugadores abandonan el servidor.
 * 
 * Se encarga de remover a los jugadores de sus partidas activas
 * cuando se desconectan del servidor.
 */
class PlayerQuitListener(private val minigame: MinigameLaberinto) : Listener {
    
    /**
     * Maneja cuando un jugador abandona el servidor.
     * 
     * @param event Evento de abandono del jugador
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        // Remover al jugador de su partida activa si la tiene
        val game = minigame.gameManager.getPlayerGame(player)
        if (game != null) {
            minigame.gameManager.removePlayerFromGame(player)
            
            // Notificar a otros jugadores si la partida no está vacía
            if (game.players.isNotEmpty()) {
                game.players.forEach { otherPlayer ->
                    otherPlayer.sendMessage("${player.name} ha abandonado la partida")
                }
            }
            
            minigame.plugin.logger.info("${player.name} removido de la partida ${game.gameId} por desconexión")
        }
    }
}
