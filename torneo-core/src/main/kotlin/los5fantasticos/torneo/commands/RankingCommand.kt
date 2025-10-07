package los5fantasticos.torneo.commands

import los5fantasticos.torneo.core.TorneoManager
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Comando para mostrar el ranking del torneo.
 * 
 * Uso:
 * - /ranking - Muestra el ranking global
 * - /ranking <minijuego> - Muestra el ranking de un minijuego específico
 * - /ranking top <número> - Muestra el top N del ranking global
 */
class RankingCommand(private val torneoManager: TorneoManager) : CommandExecutor, TabCompleter {
    
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Este comando solo puede ser usado por jugadores.")
            return true
        }
        
        when {
            args.isEmpty() -> {
                // Mostrar ranking global
                torneoManager.showGlobalRanking(sender, 10)
            }
            
            args[0].equals("top", ignoreCase = true) && args.size >= 2 -> {
                // Mostrar top N
                val limit = args[1].toIntOrNull()
                if (limit == null || limit <= 0) {
                    sender.sendMessage("${ChatColor.RED}Número inválido. Usa: /ranking top <número>")
                    return true
                }
                torneoManager.showGlobalRanking(sender, limit)
            }
            
            else -> {
                // Mostrar ranking de un minijuego específico
                val minigameName = args[0]
                val minigame = torneoManager.getMinigame(minigameName)
                
                if (minigame == null) {
                    sender.sendMessage("${ChatColor.RED}Minijuego no encontrado: $minigameName")
                    sender.sendMessage("${ChatColor.GRAY}Minijuegos disponibles:")
                    torneoManager.getAllMinigames().forEach {
                        sender.sendMessage("${ChatColor.GRAY} - ${ChatColor.WHITE}${it.gameName}")
                    }
                    return true
                }
                
                showMinigameRanking(sender, minigame.gameName)
            }
        }
        
        return true
    }
    
    /**
     * Muestra el ranking de un minijuego específico.
     */
    private fun showMinigameRanking(player: Player, minigameName: String) {
        val ranking = torneoManager.getMinigameRanking(minigameName, 10)
        
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}═══════════════════════════════")
        player.sendMessage("${ChatColor.YELLOW}${ChatColor.BOLD}    RANKING - $minigameName")
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}═══════════════════════════════")
        
        if (ranking.isEmpty()) {
            player.sendMessage("${ChatColor.GRAY}No hay datos para este minijuego aún.")
        } else {
            ranking.forEachIndexed { index, score ->
                val medal = when (index + 1) {
                    1 -> "${ChatColor.GOLD}🥇"
                    2 -> "${ChatColor.GRAY}🥈"
                    3 -> "${ChatColor.GOLD}🥉"
                    else -> "${ChatColor.WHITE}#${index + 1}"
                }
                
                val points = score.getPointsForMinigame(minigameName)
                val isCurrentPlayer = score.playerUUID == player.uniqueId
                val nameColor = if (isCurrentPlayer) ChatColor.GREEN else ChatColor.WHITE
                val arrow = if (isCurrentPlayer) " ${ChatColor.YELLOW}◄" else ""
                
                player.sendMessage("$medal ${nameColor}${score.playerName}${ChatColor.GRAY}: ${ChatColor.YELLOW}$points pts$arrow")
            }
        }
        
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}═══════════════════════════════")
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            val minigames = torneoManager.getAllMinigames().map { it.gameName }
            val options = mutableListOf("top") + minigames
            return options.filter { it.startsWith(args[0], ignoreCase = true) }
        }
        
        if (args.size == 2 && args[0].equals("top", ignoreCase = true)) {
            return listOf("5", "10", "20", "50")
        }
        
        return emptyList()
    }
}
