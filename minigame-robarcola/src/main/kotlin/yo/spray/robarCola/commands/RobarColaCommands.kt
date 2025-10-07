package yo.spray.robarCola.commands

import org.bukkit.Bukkit
import org.bukkit.ChatColor
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
            sender.sendMessage("${ChatColor.YELLOW}=== Comandos de RobarCola ===")
            sender.sendMessage("${ChatColor.YELLOW}/robarcola join ${ChatColor.GRAY}- Unirse al juego")
            sender.sendMessage("${ChatColor.YELLOW}/robarcola leave ${ChatColor.GRAY}- Salir del juego")
            if (sender.hasPermission("robarcola.admin") || sender.isOp) {
                sender.sendMessage("${ChatColor.GOLD}=== Comandos de Admin ===")
                sender.sendMessage("${ChatColor.YELLOW}/robarcola setspawn ${ChatColor.GRAY}- Establecer spawn del juego")
                sender.sendMessage("${ChatColor.YELLOW}/robarcola setlobby ${ChatColor.GRAY}- Establecer spawn del lobby")
                sender.sendMessage("${ChatColor.YELLOW}/robarcola startgame ${ChatColor.GRAY}- Iniciar juego manualmente")
                sender.sendMessage("${ChatColor.YELLOW}/robarcola stopgame ${ChatColor.GRAY}- Detener juego")
                sender.sendMessage("${ChatColor.YELLOW}/robarcola darcola <jugador> ${ChatColor.GRAY}- Dar cola a un jugador")
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
                sender.sendMessage("${ChatColor.RED}Subcomando desconocido. Usa /robarcola para ver los comandos disponibles.")
                return true
            }
        }
    }
    
    private fun handleJoin(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Este comando solo puede ser usado por jugadores")
            return true
        }
        
        manager.joinGame(sender)
        return true
    }
    
    private fun handleLeave(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Este comando solo puede ser usado por jugadores")
            return true
        }
        
        manager.removePlayerFromGame(sender)
        sender.sendMessage("${ChatColor.YELLOW}Has salido del juego RobarCola.")
        return true
    }
    
    private fun handleGiveTail(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("robarcola.admin") && !sender.isOp) {
            sender.sendMessage("${ChatColor.RED}No tienes permiso para usar este comando.")
            return true
        }
        
        when {
            args.size < 2 -> {
                sender.sendMessage("${ChatColor.RED}Uso: /robarcola darcola <jugador>")
            }
            else -> {
                val target = Bukkit.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage("${ChatColor.RED}Jugador no encontrado")
                } else {
                    manager.giveTailToPlayer(target)
                    sender.sendMessage("${ChatColor.GREEN}Le diste una cola a ${target.name}")
                }
            }
        }
        return true
    }
    
    private fun handleSetGameSpawn(sender: CommandSender): Boolean {
        if (!sender.hasPermission("robarcola.admin") && !sender.isOp) {
            sender.sendMessage("${ChatColor.RED}No tienes permiso para usar este comando.")
            return true
        }
        
        when (sender) {
            is Player -> {
                manager.setGameSpawn(sender.location)
                sender.sendMessage("${ChatColor.GREEN}✓ Spawn del minijuego establecido!")
            }
            else -> {
                sender.sendMessage("${ChatColor.RED}Este comando solo puede ser usado por jugadores")
            }
        }
        return true
    }
    
    private fun handleSetLobby(sender: CommandSender): Boolean {
        if (!sender.hasPermission("robarcola.admin") && !sender.isOp) {
            sender.sendMessage("${ChatColor.RED}No tienes permiso para usar este comando.")
            return true
        }
        
        when (sender) {
            is Player -> {
                manager.setLobbySpawn(sender.location)
                sender.sendMessage("${ChatColor.GREEN}✓ Spawn del lobby establecido!")
            }
            else -> {
                sender.sendMessage("${ChatColor.RED}Este comando solo puede ser usado por jugadores")
            }
        }
        return true
    }
    
    private fun handleStartGame(sender: CommandSender): Boolean {
        if (!sender.hasPermission("robarcola.admin") && !sender.isOp) {
            sender.sendMessage("${ChatColor.RED}No tienes permiso para usar este comando.")
            return true
        }
        
        if (manager.isGameRunning()) {
            sender.sendMessage("${ChatColor.RED}¡El juego ya está en marcha!")
        } else {
            manager.startGameExternal()
            sender.sendMessage("${ChatColor.GREEN}✓ ¡Minijuego iniciado!")
        }
        return true
    }
    
    private fun handleStopGame(sender: CommandSender): Boolean {
        if (!sender.hasPermission("robarcola.admin") && !sender.isOp) {
            sender.sendMessage("${ChatColor.RED}No tienes permiso para usar este comando.")
            return true
        }
        
        if (!manager.isGameRunning()) {
            sender.sendMessage("${ChatColor.RED}¡No hay ningún juego en marcha!")
        } else {
            manager.endGameExternal()
            sender.sendMessage("${ChatColor.GOLD}✓ ¡Minijuego detenido!")
        }
        return true
    }
}
