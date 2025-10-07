package los5fantasticos.memorias.commands

import los5fantasticos.memorias.GameManager
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Ejecutor de comandos para el minijuego Memorias.
 * Formato de comandos: /memorias <subcomando>
 */
class MemoriasCommand(private val gameManager: GameManager) : CommandExecutor {
    
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "join", "unirse" -> return handleJoin(sender)
            "leave", "salir" -> return handleLeave(sender)
            "setarena", "configurar" -> return handleSetArena(sender)
            "setlobby" -> return handleSetLobby(sender)
            "setspawn" -> return handleSetSpawn(sender)
            "settablero", "setpatron" -> return handleSetTablero(sender)
            "size", "tamaño" -> return handleSetSize(sender, args)
            else -> {
                sender.sendMessage("${ChatColor.RED}Subcomando desconocido. Usa /memorias para ver los comandos disponibles.")
                return true
            }
        }
    }
    
    private fun showHelp(sender: CommandSender) {
        sender.sendMessage("${ChatColor.GOLD}╔══════════════════════════════════╗")
        sender.sendMessage("${ChatColor.GOLD}║  ${ChatColor.YELLOW}Comandos de Memorias          ${ChatColor.GOLD}║")
        sender.sendMessage("${ChatColor.GOLD}╚══════════════════════════════════╝")
        sender.sendMessage("")
        sender.sendMessage("${ChatColor.YELLOW}/memorias join ${ChatColor.GRAY}- Unirse al juego")
        sender.sendMessage("${ChatColor.YELLOW}/memorias leave ${ChatColor.GRAY}- Salir del juego")
        
        if (sender.hasPermission("memorias.admin") || sender.isOp) {
            sender.sendMessage("")
            sender.sendMessage("${ChatColor.GOLD}═══ Comandos de Admin ═══")
            sender.sendMessage("${ChatColor.AQUA}Paso 1: Configurar ubicaciones")
            sender.sendMessage("${ChatColor.YELLOW}/memorias setlobby ${ChatColor.GRAY}- Establecer lobby de regreso")
            sender.sendMessage("${ChatColor.YELLOW}/memorias setspawn ${ChatColor.GRAY}- Establecer spawn de jugadores")
            sender.sendMessage("${ChatColor.YELLOW}/memorias settablero ${ChatColor.GRAY}- Establecer centro del tablero")
            sender.sendMessage("${ChatColor.YELLOW}/memorias size <3-15> ${ChatColor.GRAY}- Cambiar tamaño del tablero")
            sender.sendMessage("")
            sender.sendMessage("${ChatColor.AQUA}Paso 2: Crear el arena")
            sender.sendMessage("${ChatColor.YELLOW}/memorias setarena ${ChatColor.GRAY}- Crear arena con ubicaciones")
        }
        sender.sendMessage("")
    }
    
    private fun handleJoin(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Este comando solo puede ser usado por jugadores")
            return true
        }
        
        gameManager.joinPlayer(sender)
        return true
    }
    
    private fun handleLeave(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Este comando solo puede ser usado por jugadores")
            return true
        }
        
        gameManager.removePlayerAndEndGame(sender)
        return true
    }
    
    private fun handleSetArena(sender: CommandSender): Boolean {
        if (!sender.hasPermission("memorias.admin") && !sender.isOp) {
            sender.sendMessage("${ChatColor.RED}No tienes permiso para usar este comando.")
            return true
        }
        
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Este comando solo puede ser usado por jugadores")
            return true
        }
        
        gameManager.createArenaFromCurrentLocation(sender)
        return true
    }
    
    private fun handleSetLobby(sender: CommandSender): Boolean {
        if (!sender.hasPermission("memorias.admin") && !sender.isOp) {
            sender.sendMessage("${ChatColor.RED}No tienes permiso para usar este comando.")
            return true
        }
        
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Este comando solo puede ser usado por jugadores")
            return true
        }
        
        gameManager.setLobbyLocation(sender.location)
        sender.sendMessage("${ChatColor.GREEN}✓ Lobby establecido en tu ubicación actual")
        return true
    }
    
    private fun handleSetSpawn(sender: CommandSender): Boolean {
        if (!sender.hasPermission("memorias.admin") && !sender.isOp) {
            sender.sendMessage("${ChatColor.RED}No tienes permiso para usar este comando.")
            return true
        }
        
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Este comando solo puede ser usado por jugadores")
            return true
        }
        
        gameManager.setSpawnLocation(sender.location)
        sender.sendMessage("${ChatColor.GREEN}✓ Spawn de jugadores establecido en tu ubicación actual")
        return true
    }
    
    private fun handleSetTablero(sender: CommandSender): Boolean {
        if (!sender.hasPermission("memorias.admin") && !sender.isOp) {
            sender.sendMessage("${ChatColor.RED}No tienes permiso para usar este comando.")
            return true
        }
        
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Este comando solo puede ser usado por jugadores")
            return true
        }
        
        gameManager.setTableroCenter(sender.location)
        sender.sendMessage("${ChatColor.GREEN}✓ Centro del tablero establecido en tu ubicación actual")
        sender.sendMessage("${ChatColor.YELLOW}  El tablero se generará centrado aquí con el tamaño configurado")
        return true
    }
    
    private fun handleSetSize(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("memorias.admin") && !sender.isOp) {
            sender.sendMessage("${ChatColor.RED}No tienes permiso para usar este comando.")
            return true
        }
        
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Uso: /memorias size <3-15>")
            sender.sendMessage("${ChatColor.YELLOW}Ejemplo: /memorias size 5")
            return true
        }
        
        try {
            val size = args[1].toInt()
            gameManager.setGridSize(size)
            sender.sendMessage("${ChatColor.GREEN}✓ Tamaño del tablero configurado a ${size}x${size}")
        } catch (_: NumberFormatException) {
            sender.sendMessage("${ChatColor.RED}El tamaño debe ser un número válido entre 3 y 15.")
        }
        
        return true
    }
}

