package yo.spray.robarCola.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import yo.spray.robarCola.RobarColaManager

/**
 * Ejecutor de comandos para el minijuego RobarCola.
 * Formato de comandos: /robarcola <subcomando>
 */
@Suppress("unused")
class RobarColaCommands(private val manager: RobarColaManager) : CommandExecutor {
    
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("=== Comandos de RobarCola ===", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/robarcola join ", NamedTextColor.YELLOW)
                .append(Component.text("- Unirse al juego", NamedTextColor.GRAY)))
            sender.sendMessage(Component.text("/robarcola leave ", NamedTextColor.YELLOW)
                .append(Component.text("- Salir del juego", NamedTextColor.GRAY)))
            if (sender.hasPermission("robarcola.admin") || sender.isOp) {
                sender.sendMessage(Component.text("=== Comandos de Admin ===", NamedTextColor.GOLD))
                sender.sendMessage(Component.text("/robarcola setspawn ", NamedTextColor.YELLOW)
                    .append(Component.text("- Establecer spawn del juego", NamedTextColor.GRAY)))
                sender.sendMessage(Component.text("/robarcola setlobby ", NamedTextColor.YELLOW)
                    .append(Component.text("- Establecer spawn del lobby", NamedTextColor.GRAY)))
                sender.sendMessage(Component.text("/robarcola startgame ", NamedTextColor.YELLOW)
                    .append(Component.text("- Iniciar juego manualmente", NamedTextColor.GRAY)))
                sender.sendMessage(Component.text("/robarcola stopgame ", NamedTextColor.YELLOW)
                    .append(Component.text("- Detener juego", NamedTextColor.GRAY)))
                sender.sendMessage(Component.text("/robarcola darcola <jugador> ", NamedTextColor.YELLOW)
                    .append(Component.text("- Dar cola a un jugador", NamedTextColor.GRAY)))
            }
            return true
        }

        when (args[0].lowercase()) {
            "join" -> return handleJoin(sender)
            "leave" -> return handleLeave(sender)
            "darcola", "givetail" -> return handleGiveTail(sender, args)
            "setspawn", "setgamespawn" -> return handleSetGameSpawn(sender)
            "setlobby" -> return handleSetLobby(sender)
            "startgame", "start" -> return handleStartGame(sender)
            "stopgame", "stop" -> return handleStopGame(sender)
            else -> {
                sender.sendMessage(Component.text("Subcomando desconocido. Usa /robarcola para ver los comandos disponibles.", NamedTextColor.RED))
                return true
            }
        }
    }
    
    private fun handleJoin(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores", NamedTextColor.RED))
            return true
        }
        
        manager.joinGame(sender)
        return true
    }
    
    private fun handleLeave(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores", NamedTextColor.RED))
            return true
        }
        
        manager.removePlayerFromGame(sender)
        sender.sendMessage(Component.text("Has salido del juego RobarCola.", NamedTextColor.YELLOW))
        return true
    }
    
    private fun handleGiveTail(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("robarcola.admin") && !sender.isOp) {
            sender.sendMessage(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED))
            return true
        }
        
        when {
            args.size < 2 -> {
                sender.sendMessage(Component.text("Uso: /robarcola darcola <jugador>", NamedTextColor.RED))
            }
            else -> {
                val target = Bukkit.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage(Component.text("Jugador no encontrado", NamedTextColor.RED))
                } else {
                    manager.giveTailToPlayer(target)
                    sender.sendMessage(Component.text("Le diste una cola a ${target.name}", NamedTextColor.GREEN))
                }
            }
        }
        return true
    }
    
    private fun handleSetGameSpawn(sender: CommandSender): Boolean {
        if (!sender.hasPermission("robarcola.admin") && !sender.isOp) {
            sender.sendMessage(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED))
            return true
        }
        
        when (sender) {
            is Player -> {
                manager.setGameSpawn(sender.location)
                sender.sendMessage(Component.text("✓ Spawn del minijuego establecido!", NamedTextColor.GREEN))
            }
            else -> {
                sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores", NamedTextColor.RED))
            }
        }
        return true
    }
    
    private fun handleSetLobby(sender: CommandSender): Boolean {
        if (!sender.hasPermission("robarcola.admin") && !sender.isOp) {
            sender.sendMessage(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED))
            return true
        }
        
        when (sender) {
            is Player -> {
                manager.setLobbySpawn(sender.location)
                sender.sendMessage(Component.text("✓ Spawn del lobby establecido!", NamedTextColor.GREEN))
            }
            else -> {
                sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores", NamedTextColor.RED))
            }
        }
        return true
    }
    
    private fun handleStartGame(sender: CommandSender): Boolean {
        if (!sender.hasPermission("robarcola.admin") && !sender.isOp) {
            sender.sendMessage(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED))
            return true
        }
        
        if (manager.isGameRunning()) {
            sender.sendMessage(Component.text("¡El juego ya está en marcha!", NamedTextColor.RED))
        } else {
            manager.startGameExternal()
            sender.sendMessage(Component.text("✓ ¡Minijuego iniciado!", NamedTextColor.GREEN))
        }
        return true
    }
    
    private fun handleStopGame(sender: CommandSender): Boolean {
        if (!sender.hasPermission("robarcola.admin") && !sender.isOp) {
            sender.sendMessage(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED))
            return true
        }
        
        if (!manager.isGameRunning()) {
            sender.sendMessage(Component.text("¡No hay ningún juego en marcha!", NamedTextColor.RED))
        } else {
            manager.endGameExternal()
            sender.sendMessage(Component.text("✓ ¡Minijuego detenido!", NamedTextColor.GOLD))
        }
        return true
    }
}
