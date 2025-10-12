package los5fantasticos.torneo.commands

import los5fantasticos.torneo.core.TorneoManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
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
            sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores.", NamedTextColor.RED))
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
                    sender.sendMessage(Component.text("Número inválido. Usa: /ranking top <número>", NamedTextColor.RED))
                    return true
                }
                torneoManager.showGlobalRanking(sender, limit)
            }
            
            else -> {
                // Mostrar ranking de un minijuego específico
                val minigameName = args[0]
                val minigame = torneoManager.getMinigame(minigameName)
                
                if (minigame == null) {
                    sender.sendMessage(Component.text("Minijuego no encontrado: $minigameName", NamedTextColor.RED))
                    sender.sendMessage(Component.text("Minijuegos disponibles:", NamedTextColor.GRAY))
                    torneoManager.getAllMinigames().forEach {
                        val msg = Component.text(" - ", NamedTextColor.GRAY)
                            .append(Component.text(it.gameName, NamedTextColor.WHITE))
                        sender.sendMessage(msg)
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
        
        player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
        player.sendMessage(Component.text("    RANKING - $minigameName", NamedTextColor.YELLOW, TextDecoration.BOLD))
        player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
        
        if (ranking.isEmpty()) {
            player.sendMessage(Component.text("No hay datos para este minijuego aún.", NamedTextColor.GRAY))
        } else {
            ranking.forEachIndexed { index, score ->
                val medalText = when (index + 1) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> "#${index + 1}"
                }
                val medalColor = when (index + 1) {
                    1, 3 -> NamedTextColor.GOLD
                    2 -> NamedTextColor.GRAY
                    else -> NamedTextColor.WHITE
                }
                
                val points = score.getPointsForMinigame(minigameName)
                val isCurrentPlayer = score.playerUUID == player.uniqueId
                val nameColor = if (isCurrentPlayer) NamedTextColor.GREEN else NamedTextColor.WHITE
                
                val line = Component.text(medalText, medalColor)
                    .append(Component.space())
                    .append(Component.text(score.playerName, nameColor))
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(Component.text("$points pts", NamedTextColor.YELLOW))
                
                if (isCurrentPlayer) {
                    line.append(Component.text(" ◄", NamedTextColor.YELLOW))
                }
                
                player.sendMessage(line)
            }
        }
        
        player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
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
