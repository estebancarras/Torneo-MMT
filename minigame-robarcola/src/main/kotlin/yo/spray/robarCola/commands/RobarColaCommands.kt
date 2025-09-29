package yo.spray.robarcola.commands

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import yo.spray.robarcola.RobarColaManager

/**
 * Ejecutor de comandos para el minijuego RobarCola.
 */
class RobarColaCommands(private val manager: RobarColaManager) : CommandExecutor {
    
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        when (command.name.lowercase()) {
            "givetail" -> return handleGiveTail(sender, args)
            "setgamespawn" -> return handleSetGameSpawn(sender)
            "setlobby" -> return handleSetLobby(sender)
            "startgame" -> return handleStartGame(sender)
            "stopgame" -> return handleStopGame(sender)
        }
        return false
    }
    
    private fun handleGiveTail(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}Uso: /giveTail <jugador>")
            return true
        }
        
        val target = Bukkit.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage("${ChatColor.RED}Jugador no encontrado")
            return true
        }
        
        manager.giveTailExternal(target)
        sender.sendMessage("${ChatColor.GREEN}Le diste una cola a ${target.name}")
        return true
    }
    
    private fun handleSetGameSpawn(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Este comando solo puede ser usado por jugadores")
            return true
        }
        
        manager.setGameSpawnExternal(sender.location)
        sender.sendMessage("${ChatColor.GREEN}Spawn del minijuego establecido!")
        return true
    }
    
    private fun handleSetLobby(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Este comando solo puede ser usado por jugadores")
            return true
        }
        
        manager.setLobbySpawnExternal(sender.location)
        sender.sendMessage("${ChatColor.GREEN}Spawn del lobby establecido!")
        return true
    }
    
    private fun handleStartGame(sender: CommandSender): Boolean {
        if (manager.isGameRunning()) {
            sender.sendMessage("${ChatColor.RED}¡El juego ya está en marcha!")
        } else {
            manager.startGameExternal()
            sender.sendMessage("${ChatColor.GREEN}¡Minijuego iniciado!")
        }
        return true
    }
    
    private fun handleStopGame(sender: CommandSender): Boolean {
        if (!manager.isGameRunning()) {
            sender.sendMessage("${ChatColor.RED}¡No hay ningún juego en marcha!")
        } else {
            manager.endGameExternal()
            sender.sendMessage("${ChatColor.RED}¡Minijuego detenido!")
        }
        return true
    }
}
