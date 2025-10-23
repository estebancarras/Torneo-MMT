package yo.spray.robarCabeza.commands

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.util.selection.SelectionManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import yo.spray.robarCabeza.RobarCabezaManager
import yo.spray.robarCabeza.services.ArenaManager

/**
 * Ejecutor de comandos para el minijuego Robar Cabeza.
 * 
 * Maneja los subcomandos:
 * - `/robarcabeza join` - Unirse a una partida
 * - `/robarcabeza leave` - Salir de una partida
 * - `/robarcabeza admin` - Comandos de administración
 */
class RobarCabezaCommands(
    private val manager: RobarCabezaManager,
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
            "join" -> handleJoin(sender)
            "leave" -> handleLeave(sender)
            "admin" -> handleAdmin(sender, args.drop(1).toTypedArray())
            else -> sendHelp(sender)
        }
        
        return true
    }
    
    /**
     * Maneja el subcomando `/robarcabeza join`.
     */
    private fun handleJoin(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser ejecutado por jugadores.", NamedTextColor.RED))
            return
        }
        
        manager.joinGame(sender)
    }
    
    /**
     * Maneja el subcomando `/robarcabeza leave`.
     */
    private fun handleLeave(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser ejecutado por jugadores.", NamedTextColor.RED))
            return
        }
        
        manager.removePlayerFromGame(sender)
        sender.sendMessage(Component.text("Has salido del juego.", NamedTextColor.YELLOW))
    }
    
    /**
     * Maneja el subcomando `/robarcabeza admin`.
     */
    private fun handleAdmin(sender: CommandSender, args: Array<String>) {
        // Verificar permisos
        if (!sender.hasPermission("robarcabeza.admin") && !sender.isOp) {
            sender.sendMessage(Component.text("No tienes permiso para usar comandos de administrador.", NamedTextColor.RED))
            return
        }
        
        if (args.isEmpty()) {
            sendAdminHelp(sender)
            return
        }
        
        when (args[0].lowercase()) {
            "create" -> handleCreateArena(sender, args)
            "list" -> handleListArenas(sender)
            "delete" -> handleDeleteArena(sender, args)
            "addspawn" -> handleAddSpawn(sender, args)
            "setregion" -> handleSetRegion(sender, args)
            "info" -> handleArenaInfo(sender, args)
            "startgame" -> handleStartGame(sender, args)
            "stopgame" -> handleStopGame(sender)
            "givehead" -> handleGiveHead(sender, args)
            else -> sendAdminHelp(sender)
        }
    }
    
    /**
     * Crea una nueva arena.
     */
    private fun handleCreateArena(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /robarcabeza admin create <nombre>", NamedTextColor.RED))
            return
        }
        
        val arenaName = args[1]
        
        // Verificar si ya existe
        if (arenaManager.arenaExists(arenaName)) {
            sender.sendMessage(Component.text("Ya existe una arena con ese nombre.", NamedTextColor.RED))
            return
        }
        
        // Crear arena
        arenaManager.createArena(arenaName)
        arenaManager.saveArenas()
        
        sender.sendMessage(Component.text("✓ Arena '$arenaName' creada", NamedTextColor.GREEN, TextDecoration.BOLD))
        sender.sendMessage(Component.text("Usa los siguientes comandos para configurarla:", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("  /robarcabeza admin addspawn <arena> - Añadir spawns", NamedTextColor.GRAY))
        sender.sendMessage(Component.text("  /robarcabeza admin setregion <arena> - Establecer región", NamedTextColor.GRAY))
    }
    
    /**
     * Lista todas las arenas.
     */
    private fun handleListArenas(sender: CommandSender) {
        val arenas = arenaManager.getAllArenas()
        
        if (arenas.isEmpty()) {
            sender.sendMessage(Component.text("No hay arenas configuradas.", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("Usa /robarcabeza admin create <nombre> para crear una.", NamedTextColor.GRAY))
            return
        }
        
        sender.sendMessage(Component.empty())
        sender.sendMessage(Component.text("━━━━━━ Arenas (${arenas.size}) ━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        arenas.forEach { arena ->
            val spawnsInfo = if (arena.spawns.isEmpty()) "§c0 spawns" else "§a${arena.spawns.size} spawns"
            val regionInfo = if (arena.playRegion == null) "§cSin región" else "§aRegión OK"
            sender.sendMessage(Component.text("• ${arena.name} ", NamedTextColor.YELLOW)
                .append(Component.text("($spawnsInfo, $regionInfo)", NamedTextColor.GRAY)))
        }
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        sender.sendMessage(Component.empty())
    }
    
    /**
     * Elimina una arena.
     */
    private fun handleDeleteArena(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /robarcabeza admin delete <nombre>", NamedTextColor.RED))
            return
        }
        
        val arenaName = args[1]
        
        if (!arenaManager.arenaExists(arenaName)) {
            sender.sendMessage(Component.text("No existe una arena con ese nombre.", NamedTextColor.RED))
            return
        }
        
        arenaManager.deleteArena(arenaName)
        arenaManager.saveArenas()
        sender.sendMessage(Component.text("✓ Arena '$arenaName' eliminada.", NamedTextColor.GREEN))
    }
    
    /**
     * Añade un spawn a una arena.
     */
    private fun handleAddSpawn(sender: CommandSender, args: Array<String>) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser ejecutado por jugadores.", NamedTextColor.RED))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /robarcabeza admin addspawn <arena>", NamedTextColor.RED))
            return
        }
        
        val arenaName = args[1]
        val arena = arenaManager.getArena(arenaName)
        
        if (arena == null) {
            sender.sendMessage(Component.text("No existe una arena con ese nombre.", NamedTextColor.RED))
            return
        }
        
        // Añadir spawn en la ubicación actual del jugador
        arena.spawns.add(sender.location.clone())
        arenaManager.saveArenas()
        
        sender.sendMessage(Component.text("✓ Spawn ${arena.spawns.size} añadido a '${arena.name}'", NamedTextColor.GREEN))
        sender.sendMessage(Component.text("Ubicación: ${sender.location.blockX}, ${sender.location.blockY}, ${sender.location.blockZ}", NamedTextColor.GRAY))
    }
    
    /**
     * Establece la región de juego de una arena.
     */
    private fun handleSetRegion(sender: CommandSender, args: Array<String>) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser ejecutado por jugadores.", NamedTextColor.RED))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /robarcabeza admin setregion <arena>", NamedTextColor.RED))
            return
        }
        
        val arenaName = args[1]
        val arena = arenaManager.getArena(arenaName)
        
        if (arena == null) {
            sender.sendMessage(Component.text("No existe una arena con ese nombre.", NamedTextColor.RED))
            return
        }
        
        // Obtener la selección del jugador
        val selection = SelectionManager.getSelection(sender)
        
        if (selection == null) {
            sender.sendMessage(Component.text("No tienes una selección activa.", NamedTextColor.RED))
            sender.sendMessage(Component.text("Usa /torneo wand para obtener la varita de selección.", NamedTextColor.YELLOW))
            return
        }
        
        // Asignar la región a la arena
        arena.playRegion = selection
        arenaManager.saveArenas()
        
        val sizeX = selection.maxX - selection.minX + 1
        val sizeY = selection.maxY - selection.minY + 1
        val sizeZ = selection.maxZ - selection.minZ + 1
        
        sender.sendMessage(Component.text("✓ Región establecida para '${arena.name}'", NamedTextColor.GREEN, TextDecoration.BOLD))
        sender.sendMessage(Component.text("Tamaño: ${sizeX}x${sizeY}x${sizeZ}", NamedTextColor.GRAY))
    }
    
    /**
     * Muestra información de una arena.
     */
    private fun handleArenaInfo(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /robarcabeza admin info <arena>", NamedTextColor.RED))
            return
        }
        
        val arenaName = args[1]
        val arena = arenaManager.getArena(arenaName)
        
        if (arena == null) {
            sender.sendMessage(Component.text("No existe una arena con ese nombre.", NamedTextColor.RED))
            return
        }
        
        sender.sendMessage(Component.empty())
        sender.sendMessage(Component.text("━━━━━━ Arena: ${arena.name} ━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        sender.sendMessage(Component.text("Spawns: ", NamedTextColor.YELLOW)
            .append(Component.text("${arena.spawns.size}", NamedTextColor.WHITE)))
        
        if (arena.spawns.isNotEmpty()) {
            arena.spawns.forEachIndexed { index, spawn ->
                sender.sendMessage(Component.text("  ${index + 1}. ${spawn.blockX}, ${spawn.blockY}, ${spawn.blockZ}", NamedTextColor.GRAY))
            }
        }
        
        if (arena.playRegion != null) {
            val region = arena.playRegion!!
            val sizeX = region.maxX - region.minX + 1
            val sizeY = region.maxY - region.minY + 1
            val sizeZ = region.maxZ - region.minZ + 1
            sender.sendMessage(Component.text("Región: ", NamedTextColor.YELLOW)
                .append(Component.text("${sizeX}x${sizeY}x${sizeZ}", NamedTextColor.WHITE)))
        } else {
            sender.sendMessage(Component.text("Región: ", NamedTextColor.YELLOW)
                .append(Component.text("No configurada", NamedTextColor.RED)))
        }
        
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        sender.sendMessage(Component.empty())
    }
    
    /**
     * Inicia el juego manualmente.
     */
    private fun handleStartGame(sender: CommandSender, args: Array<String>) {
        if (manager.isGameRunning()) {
            sender.sendMessage(Component.text("¡El juego ya está en marcha!", NamedTextColor.RED))
            return
        }
        
        // Si se especifica una arena, usarla
        val arena = if (args.size >= 2) {
            arenaManager.getArena(args[1])
        } else {
            null
        }
        
        manager.startGameExternal(arena)
        sender.sendMessage(Component.text("✓ ¡Minijuego iniciado!", NamedTextColor.GREEN, TextDecoration.BOLD))
    }
    
    /**
     * Detiene el juego.
     */
    private fun handleStopGame(sender: CommandSender) {
        if (!manager.isGameRunning()) {
            sender.sendMessage(Component.text("¡No hay ningún juego en marcha!", NamedTextColor.RED))
            return
        }
        
        manager.endGameExternal()
        sender.sendMessage(Component.text("✓ ¡Minijuego detenido!", NamedTextColor.GOLD))
    }
    
    /**
     * Da una cabeza a un jugador.
     */
    private fun handleGiveHead(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /robarcabeza admin givehead <jugador>", NamedTextColor.RED))
            return
        }
        
        val target = Bukkit.getPlayer(args[1])
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado", NamedTextColor.RED))
            return
        }
        
        manager.giveTailToPlayer(target)
        sender.sendMessage(Component.text("✓ Le diste una cabeza a ${target.name}", NamedTextColor.GREEN))
    }
    
    /**
     * Muestra la ayuda de comandos de administrador.
     */
    private fun sendAdminHelp(sender: CommandSender) {
        sender.sendMessage(Component.empty())
        sender.sendMessage(Component.text("━━━━━━ Comandos Admin ━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        sender.sendMessage(Component.text("Gestión de Arenas:", NamedTextColor.AQUA))
        sender.sendMessage(Component.text("/robarcabeza admin create <nombre> ", NamedTextColor.YELLOW)
            .append(Component.text("- Crear arena", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/robarcabeza admin list ", NamedTextColor.YELLOW)
            .append(Component.text("- Listar arenas", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/robarcabeza admin delete <nombre> ", NamedTextColor.YELLOW)
            .append(Component.text("- Eliminar arena", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/robarcabeza admin addspawn <arena> ", NamedTextColor.YELLOW)
            .append(Component.text("- Añadir spawn", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/robarcabeza admin setregion <arena> ", NamedTextColor.YELLOW)
            .append(Component.text("- Establecer región", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/robarcabeza admin info <arena> ", NamedTextColor.YELLOW)
            .append(Component.text("- Ver info de arena", NamedTextColor.GRAY)))
        sender.sendMessage(Component.empty())
        sender.sendMessage(Component.text("Control de Partida:", NamedTextColor.AQUA))
        sender.sendMessage(Component.text("/robarcabeza admin startgame [arena] ", NamedTextColor.YELLOW)
            .append(Component.text("- Iniciar juego", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/robarcabeza admin stopgame ", NamedTextColor.YELLOW)
            .append(Component.text("- Detener juego", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/robarcabeza admin givehead <jugador> ", NamedTextColor.YELLOW)
            .append(Component.text("- Dar cabeza", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        sender.sendMessage(Component.empty())
    }
    
    /**
     * Muestra la ayuda del comando.
     */
    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage(Component.empty())
        sender.sendMessage(Component.text("━━━━━━━━━━ Robar Cabeza ━━━━━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        sender.sendMessage(Component.text("/robarcabeza join ", NamedTextColor.YELLOW)
            .append(Component.text("- Unirse a una partida", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/robarcabeza leave ", NamedTextColor.YELLOW)
            .append(Component.text("- Salir de una partida", NamedTextColor.GRAY)))
        if (sender.hasPermission("robarcabeza.admin") || sender.isOp) {
            sender.sendMessage(Component.text("/robarcabeza admin ", NamedTextColor.YELLOW)
                .append(Component.text("- Comandos de administración", NamedTextColor.GRAY)))
        }
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        sender.sendMessage(Component.empty())
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            val subcommands = mutableListOf("join", "leave")
            if (sender.hasPermission("robarcabeza.admin") || sender.isOp) {
                subcommands.add("admin")
            }
            return subcommands.filter { it.startsWith(args[0].lowercase()) }
        }
        
        if (args.size == 2 && args[0].equals("admin", ignoreCase = true)) {
            val adminCommands = listOf("create", "list", "delete", "addspawn", "setregion", "info", "startgame", "stopgame", "givehead")
            return adminCommands.filter { it.startsWith(args[1].lowercase()) }
        }
        
        if (args.size == 3 && args[0].equals("admin", ignoreCase = true)) {
            return when (args[1].lowercase()) {
                "delete", "addspawn", "setregion", "info", "startgame" -> {
                    arenaManager.getAllArenas().map { it.name }.filter { it.startsWith(args[2], ignoreCase = true) }
                }
                "givehead" -> {
                    Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[2], ignoreCase = true) }
                }
                else -> emptyList()
            }
        }
        
        return emptyList()
    }
}
