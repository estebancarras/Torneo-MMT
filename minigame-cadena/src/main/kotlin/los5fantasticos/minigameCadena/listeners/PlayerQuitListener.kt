package los5fantasticos.minigameCadena.listeners

import los5fantasticos.minigameCadena.MinigameCadena
import los5fantasticos.minigameCadena.game.GameState
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Maneja la desconexión de jugadores durante partidas de Cadena.
 * 
 * Responsabilidades:
 * - Remover jugadores de partidas activas
 * - Notificar a otros jugadores
 * - Cancelar partidas si no hay suficientes jugadores
 */
class PlayerQuitListener(private val minigame: MinigameCadena) : Listener {
    
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        // Verificar si el jugador está en una partida
        if (!minigame.gameManager.isPlayerInGame(player)) {
            return
        }
        
        // Obtener la partida y el equipo
        val game = minigame.gameManager.getPlayerGame(player) ?: return
        val team = minigame.gameManager.getPlayerTeam(player) ?: return
        
        // Remover jugador
        minigame.gameManager.removePlayer(player)
        
        // Notificar a otros jugadores
        game.teams.forEach { t ->
            t.getOnlinePlayers().forEach { p ->
                p.sendMessage("${ChatColor.GOLD}[Cadena] ${ChatColor.RED}${player.name} ${ChatColor.GRAY}se ha desconectado")
            }
        }
        
        // Verificar si la partida debe cancelarse o continuar
        when (game.state) {
            GameState.LOBBY, GameState.COUNTDOWN -> {
                // Si no hay suficientes jugadores, cancelar cuenta atrás
                if (!game.hasMinimumPlayers()) {
                    game.state = GameState.LOBBY
                    game.teams.forEach { t ->
                        t.getOnlinePlayers().forEach { p ->
                            p.sendMessage("${ChatColor.GOLD}[Cadena] ${ChatColor.RED}No hay suficientes jugadores. Partida cancelada.")
                        }
                    }
                }
            }
            
            GameState.IN_GAME -> {
                // Si el equipo se quedó sin jugadores, eliminar equipo
                if (team.players.isEmpty()) {
                    game.teams.remove(team)
                }
                
                // Si no quedan equipos, finalizar partida
                if (game.teams.isEmpty()) {
                    minigame.gameManager.endGame(game)
                }
                
                // TODO PR3: Si el equipo aún tiene jugadores, continuar con encadenamiento
                // TODO PR4: Verificar si el equipo debe ser penalizado
            }
            
            GameState.FINISHED -> {
                // La partida ya terminó, no hacer nada
            }
        }
    }
}
