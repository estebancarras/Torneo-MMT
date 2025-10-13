package los5fantasticos.memorias.commands

import los5fantasticos.memorias.GameManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
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
                sender.sendMessage(Component.text("Subcomando desconocido. Usa /memorias para ver los comandos disponibles.", NamedTextColor.RED))
                return true
            }
        }
    }
    
    private fun showHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("╔══════════════════════════════════╗", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("║  ", NamedTextColor.GOLD)
            .append(Component.text("Comandos de Memorias          ", NamedTextColor.YELLOW))
            .append(Component.text("║", NamedTextColor.GOLD)))
        sender.sendMessage(Component.text("╚══════════════════════════════════╝", NamedTextColor.GOLD))
        sender.sendMessage(Component.empty())
        sender.sendMessage(Component.text("/memorias join ", NamedTextColor.YELLOW)
            .append(Component.text("- Unirse al juego", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/memorias leave ", NamedTextColor.YELLOW)
            .append(Component.text("- Salir del juego", NamedTextColor.GRAY)))
        
        if (sender.hasPermission("memorias.admin") || sender.isOp) {
            sender.sendMessage(Component.empty())
            sender.sendMessage(Component.text("═══ Comandos de Admin ═══", NamedTextColor.GOLD))
            sender.sendMessage(Component.text("Paso 1: Configurar ubicaciones", NamedTextColor.AQUA))
            sender.sendMessage(Component.text("/memorias setlobby ", NamedTextColor.YELLOW)
                .append(Component.text("- Establecer lobby de regreso", NamedTextColor.GRAY)))
            sender.sendMessage(Component.text("/memorias setspawn ", NamedTextColor.YELLOW)
                .append(Component.text("- Establecer spawn de jugadores", NamedTextColor.GRAY)))
            sender.sendMessage(Component.text("/memorias settablero ", NamedTextColor.YELLOW)
                .append(Component.text("- Establecer centro del tablero", NamedTextColor.GRAY)))
            sender.sendMessage(Component.text("/memorias size <3-15> ", NamedTextColor.YELLOW)
                .append(Component.text("- Cambiar tamaño del tablero", NamedTextColor.GRAY)))
            sender.sendMessage(Component.empty())
            sender.sendMessage(Component.text("Paso 2: Crear el arena", NamedTextColor.AQUA))
            sender.sendMessage(Component.text("/memorias setarena ", NamedTextColor.YELLOW)
                .append(Component.text("- Crear arena con ubicaciones", NamedTextColor.GRAY)))
        }
        sender.sendMessage(Component.empty())
    }
    
    private fun handleJoin(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores", NamedTextColor.RED))
            return true
        }
        
        gameManager.joinPlayer(sender)
        return true
    }
    
    private fun handleLeave(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores", NamedTextColor.RED))
            return true
        }
        
        gameManager.removePlayerAndEndGame(sender)
        return true
    }
    
    private fun handleSetArena(sender: CommandSender): Boolean {
        if (!sender.hasPermission("memorias.admin") && !sender.isOp) {
            sender.sendMessage(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED))
            return true
        }
        
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores", NamedTextColor.RED))
            return true
        }
        
        gameManager.createArenaFromCurrentLocation(sender)
        return true
    }
    
    private fun handleSetLobby(sender: CommandSender): Boolean {
        if (!sender.hasPermission("memorias.admin") && !sender.isOp) {
            sender.sendMessage(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED))
            return true
        }
        
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores", NamedTextColor.RED))
            return true
        }
        
        gameManager.setLobbyLocation(sender.location)
        sender.sendMessage(Component.text("✓ Lobby establecido en tu ubicación actual", NamedTextColor.GREEN))
        return true
    }
    
    private fun handleSetSpawn(sender: CommandSender): Boolean {
        if (!sender.hasPermission("memorias.admin") && !sender.isOp) {
            sender.sendMessage(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED))
            return true
        }
        
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores", NamedTextColor.RED))
            return true
        }
        
        gameManager.setSpawnLocation(sender.location)
        sender.sendMessage(Component.text("✓ Spawn de jugadores establecido en tu ubicación actual", NamedTextColor.GREEN))
        return true
    }
    
    private fun handleSetTablero(sender: CommandSender): Boolean {
        if (!sender.hasPermission("memorias.admin") && !sender.isOp) {
            sender.sendMessage(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED))
            return true
        }
        
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores", NamedTextColor.RED))
            return true
        }
        
        gameManager.setTableroCenter(sender.location)
        sender.sendMessage(Component.text("✓ Centro del tablero establecido en tu ubicación actual", NamedTextColor.GREEN))
        sender.sendMessage(Component.text("  El tablero se generará centrado aquí con el tamaño configurado", NamedTextColor.YELLOW))
        return true
    }
    
    private fun handleSetSize(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("memorias.admin") && !sender.isOp) {
            sender.sendMessage(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED))
            return true
        }
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /memorias size <3-15>", NamedTextColor.RED))
            sender.sendMessage(Component.text("Ejemplo: /memorias size 5", NamedTextColor.YELLOW))
            return true
        }
        
        try {
            val size = args[1].toInt()
            gameManager.setGridSize(size)
            sender.sendMessage(Component.text("✓ Tamaño del tablero configurado a ${size}x${size}", NamedTextColor.GREEN))
        } catch (_: NumberFormatException) {
            sender.sendMessage(Component.text("El tamaño debe ser un número válido entre 3 y 15.", NamedTextColor.RED))
        }
        
        return true
    }
}

