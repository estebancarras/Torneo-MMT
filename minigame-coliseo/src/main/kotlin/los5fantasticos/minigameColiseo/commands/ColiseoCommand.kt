package los5fantasticos.minigameColiseo.commands

import los5fantasticos.minigameColiseo.services.ArenaManager
import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.util.selection.SelectionManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Comandos de administración del Coliseo.
 * 
 * Comandos disponibles:
 * - /coliseo admin create <nombre>
 * - /coliseo admin addelitespawn <arena>
 * - /coliseo admin addhordespawn <arena>
 * - /coliseo admin setregion <arena>
 * - /coliseo admin list
 * - /coliseo admin delete <arena>
 * - /coliseo admin info <arena>
 */
class ColiseoCommand(
    private val arenaManager: ArenaManager,
    private val torneoPlugin: TorneoPlugin
) : CommandExecutor, TabCompleter {
    
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "admin" -> handleAdmin(sender, args.drop(1).toTypedArray())
            else -> sendHelp(sender)
        }
        
        return true
    }
    
    private fun handleAdmin(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("coliseo.admin") && !sender.isOp) {
            sender.sendMessage(Component.text("No tienes permiso para usar comandos de administrador.", NamedTextColor.RED))
            return
        }
        
        if (args.isEmpty()) {
            sendAdminHelp(sender)
            return
        }
        
        when (args[0].lowercase()) {
            "create" -> handleCreate(sender, args)
            "addelitespawn" -> handleAddEliteSpawn(sender, args)
            "addhordespawn" -> handleAddHordeSpawn(sender, args)
            "setspectatorspawn" -> handleSetSpectatorSpawn(sender, args)
            "setregion" -> handleSetRegion(sender, args)
            "list" -> handleList(sender)
            "delete" -> handleDelete(sender, args)
            "info" -> handleInfo(sender, args)
            else -> sendAdminHelp(sender)
        }
    }
    
    private fun handleCreate(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /coliseo admin create <nombre>", NamedTextColor.RED))
            return
        }
        
        val arenaName = args[1]
        
        if (arenaManager.arenaExists(arenaName)) {
            sender.sendMessage(Component.text("Ya existe una arena con ese nombre.", NamedTextColor.RED))
            return
        }
        
        arenaManager.createArena(arenaName)
        arenaManager.saveArenas()
        
        sender.sendMessage(Component.text("✓ Arena '$arenaName' creada", NamedTextColor.GREEN))
        sender.sendMessage(Component.text("Usa los siguientes comandos para configurarla:", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("  /coliseo admin addelitespawn $arenaName", NamedTextColor.GRAY))
        sender.sendMessage(Component.text("  /coliseo admin addhordespawn $arenaName", NamedTextColor.GRAY))
        sender.sendMessage(Component.text("  /coliseo admin setregion $arenaName", NamedTextColor.GRAY))
    }
    
    private fun handleAddEliteSpawn(sender: CommandSender, args: Array<String>) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser ejecutado por jugadores.", NamedTextColor.RED))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /coliseo admin addelitespawn <arena>", NamedTextColor.RED))
            return
        }
        
        val arenaName = args[1]
        val arena = arenaManager.getArena(arenaName)
        
        if (arena == null) {
            sender.sendMessage(Component.text("No existe una arena con ese nombre.", NamedTextColor.RED))
            return
        }
        
        arena.eliteSpawns.add(sender.location.clone())
        arenaManager.saveArenas()
        
        sender.sendMessage(Component.text("✓ Spawn Élite ${arena.eliteSpawns.size} añadido", NamedTextColor.GREEN))
    }
    
    private fun handleAddHordeSpawn(sender: CommandSender, args: Array<String>) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser ejecutado por jugadores.", NamedTextColor.RED))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /coliseo admin addhordespawn <arena>", NamedTextColor.RED))
            return
        }
        
        val arenaName = args[1]
        val arena = arenaManager.getArena(arenaName)
        
        if (arena == null) {
            sender.sendMessage(Component.text("No existe una arena con ese nombre.", NamedTextColor.RED))
            return
        }
        
        arena.hordeSpawns.add(sender.location.clone())
        arenaManager.saveArenas()
        
        sender.sendMessage(Component.text("✓ Spawn Horda ${arena.hordeSpawns.size} añadido", NamedTextColor.GREEN))
    }
    
    private fun handleSetSpectatorSpawn(sender: CommandSender, args: Array<String>) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser ejecutado por jugadores.", NamedTextColor.RED))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /coliseo admin setspectatorspawn <arena>", NamedTextColor.RED))
            return
        }
        
        val arenaName = args[1]
        val arena = arenaManager.getArena(arenaName)
        
        if (arena == null) {
            sender.sendMessage(Component.text("No existe una arena con ese nombre.", NamedTextColor.RED))
            return
        }
        
        arena.spectatorSpawn = sender.location.clone()
        arenaManager.saveArenas()
        
        sender.sendMessage(Component.text("✓ Spawn de espectadores establecido", NamedTextColor.GREEN))
        sender.sendMessage(Component.text("Los jugadores eliminados aparecerán aquí", NamedTextColor.GRAY))
    }
    
    private fun handleSetRegion(sender: CommandSender, args: Array<String>) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser ejecutado por jugadores.", NamedTextColor.RED))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /coliseo admin setregion <arena>", NamedTextColor.RED))
            return
        }
        
        val arenaName = args[1]
        val arena = arenaManager.getArena(arenaName)
        
        if (arena == null) {
            sender.sendMessage(Component.text("No existe una arena con ese nombre.", NamedTextColor.RED))
            return
        }
        
        val selection = SelectionManager.getSelection(sender)
        if (selection == null) {
            sender.sendMessage(Component.text("No tienes una selección activa.", NamedTextColor.RED))
            sender.sendMessage(Component.text("Usa /torneo wand para obtener la varita de selección.", NamedTextColor.YELLOW))
            return
        }
        
        arena.playRegion = selection
        arenaManager.saveArenas()
        
        sender.sendMessage(Component.text("✓ Región establecida para '$arenaName'", NamedTextColor.GREEN))
    }
    
    private fun handleList(sender: CommandSender) {
        val arenas = arenaManager.getAllArenas()
        
        if (arenas.isEmpty()) {
            sender.sendMessage(Component.text("No hay arenas configuradas.", NamedTextColor.YELLOW))
            return
        }
        
        sender.sendMessage(Component.text("═══════ Arenas del Coliseo (${arenas.size}) ═══════", NamedTextColor.GOLD))
        arenas.forEach { arena ->
            val status = if (arena.isValid()) "✓" else "✗"
            sender.sendMessage(Component.text("$status ${arena.name} - Élite: ${arena.eliteSpawns.size}, Horda: ${arena.hordeSpawns.size}", NamedTextColor.YELLOW))
        }
    }
    
    private fun handleDelete(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /coliseo admin delete <arena>", NamedTextColor.RED))
            return
        }
        
        val arenaName = args[1]
        
        if (!arenaManager.arenaExists(arenaName)) {
            sender.sendMessage(Component.text("No existe una arena con ese nombre.", NamedTextColor.RED))
            return
        }
        
        arenaManager.deleteArena(arenaName)
        sender.sendMessage(Component.text("✓ Arena '$arenaName' eliminada.", NamedTextColor.GREEN))
    }
    
    private fun handleInfo(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /coliseo admin info <arena>", NamedTextColor.RED))
            return
        }
        
        val arenaName = args[1]
        val arena = arenaManager.getArena(arenaName)
        
        if (arena == null) {
            sender.sendMessage(Component.text("No existe una arena con ese nombre.", NamedTextColor.RED))
            return
        }
        
        sender.sendMessage(Component.text("═══════ Arena: ${arena.name} ═══════", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("Spawns Élite: ${arena.eliteSpawns.size}", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("Spawns Horda: ${arena.hordeSpawns.size}", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("Región: ${if (arena.playRegion != null) "Configurada" else "No configurada"}", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("Estado: ${if (arena.isValid()) "✓ Válida" else "✗ Incompleta"}", NamedTextColor.YELLOW))
    }
    
    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("═══════ Coliseo: Élite vs Horda ═══════", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("/coliseo admin - Comandos de administración", NamedTextColor.YELLOW))
    }
    
    private fun sendAdminHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("═══════ Comandos Admin - Coliseo ═══════", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("/coliseo admin create <nombre>", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/coliseo admin addelitespawn <arena>", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/coliseo admin addhordespawn <arena>", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/coliseo admin setregion <arena>", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/coliseo admin list", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/coliseo admin delete <arena>", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/coliseo admin info <arena>", NamedTextColor.YELLOW))
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            return listOf("admin").filter { it.startsWith(args[0].lowercase()) }
        }
        
        if (args.size == 2 && args[0].equals("admin", true)) {
            return listOf("create", "addelitespawn", "addhordespawn", "setregion", "list", "delete", "info")
                .filter { it.startsWith(args[1].lowercase()) }
        }
        
        if (args.size == 3 && args[0].equals("admin", true)) {
            return when (args[1].lowercase()) {
                "addelitespawn", "addhordespawn", "setregion", "delete", "info" -> {
                    arenaManager.getAllArenas().map { it.name }
                        .filter { it.startsWith(args[2], ignoreCase = true) }
                }
                else -> emptyList()
            }
        }
        
        return emptyList()
    }
}
