package los5fantasticos.minigameCarrerabarcos.commands

import los5fantasticos.minigameCarrerabarcos.services.ArenaManager
import los5fantasticos.minigameCarrerabarcos.services.GameManager
import los5fantasticos.torneo.util.selection.SelectionManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Sistema de comandos completo para Carrera de Barcos.
 * 
 * COMANDOS DE ADMINISTRACIÓN:
 * - /carrera crear <nombre> - Crea una nueva arena
 * - /carrera setlobby <arena> - Establece el lobby
 * - /carrera addspawn <arena> - Añade un spawn
 * - /carrera addcheckpoint <arena> - Añade un checkpoint
 * - /carrera setmeta <arena> - Establece la meta
 * - /carrera setprotection <arena> - Establece región de protección
 * - /carrera list - Lista todas las arenas
 * - /carrera info <arena> - Información de una arena
 * - /carrera remove <arena> - Elimina una arena
 * - /carrera save - Guarda configuración
 * - /carrera wand - Obtiene varita de selección
 * 
 * COMANDOS DE JUGADOR:
 * - /carrera unirse <arena> - Unirse a una carrera
 * - /carrera salir - Salir de la carrera actual
 * - /carrera stats - Ver estadísticas
 * 
 * ARQUITECTURA:
 * Esta clase SOLO maneja la interfaz de usuario (comandos).
 * Toda la lógica está delegada a los servicios (ArenaManager, GameManager).
 */
class CarreraCommand(
    private val arenaManager: ArenaManager,
    private val gameManager: GameManager
) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            // Comandos de administración
            "crear" -> cmdCrear(sender, args)
            "setlobby" -> cmdSetLobby(sender, args)
            "addspawn" -> cmdAddSpawn(sender, args)
            "addcheckpoint" -> cmdAddCheckpoint(sender, args)
            "setmeta" -> cmdSetMeta(sender, args)
            "setprotection" -> cmdSetProtection(sender, args)
            "list" -> cmdList(sender)
            "info" -> cmdInfo(sender, args)
            "remove" -> cmdRemove(sender, args)
            "save" -> cmdSave(sender)
            "wand" -> cmdWand(sender)
            
            // Comandos de jugador
            "unirse" -> cmdUnirse(sender, args)
            "salir" -> cmdSalir(sender)
            "stats" -> cmdStats(sender)
            
            // Ayuda
            "help" -> showHelp(sender)
            
            else -> {
                sender.sendMessage(
                    Component.text("Comando desconocido. Usa ", NamedTextColor.RED)
                        .append(Component.text("/carrera help", NamedTextColor.YELLOW))
                )
            }
        }
        
        return true
    }
    
    // ========== COMANDOS DE ADMINISTRACIÓN ==========
    
    private fun cmdCrear(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("torneo.admin")) {
            sender.sendMessage(Component.text("✗ No tienes permisos", NamedTextColor.RED))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /carrera crear <nombre>", NamedTextColor.YELLOW))
            return
        }
        
        val nombre = args[1]
        
        if (arenaManager.hasArena(nombre)) {
            sender.sendMessage(Component.text("✗ Ya existe una arena con ese nombre", NamedTextColor.RED))
            return
        }
        
        val arena = arenaManager.createArena(nombre)
        if (arena != null) {
            sender.sendMessage(
                Component.text("✓ Arena '", NamedTextColor.GREEN)
                    .append(Component.text(nombre, NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text("' creada", NamedTextColor.GREEN))
            )
            sender.sendMessage(
                Component.text("Configura la arena con los siguientes comandos:", NamedTextColor.GRAY)
            )
            sender.sendMessage(Component.text("  /carrera setlobby $nombre", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("  /carrera addspawn $nombre", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("  /carrera addcheckpoint $nombre", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("  /carrera setmeta $nombre", NamedTextColor.YELLOW))
        } else {
            sender.sendMessage(Component.text("✗ Error al crear la arena", NamedTextColor.RED))
        }
    }
    
    private fun cmdSetLobby(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("torneo.admin")) {
            sender.sendMessage(Component.text("✗ No tienes permisos", NamedTextColor.RED))
            return
        }
        
        if (sender !is Player) {
            sender.sendMessage(Component.text("✗ Solo jugadores pueden usar este comando", NamedTextColor.RED))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /carrera setlobby <arena>", NamedTextColor.YELLOW))
            return
        }
        
        val nombre = args[1]
        val arena = arenaManager.getArena(nombre)
        
        if (arena == null) {
            sender.sendMessage(Component.text("✗ Arena no encontrada", NamedTextColor.RED))
            return
        }
        
        arena.lobby = sender.location
        sender.sendMessage(
            Component.text("✓ Lobby establecido para '", NamedTextColor.GREEN)
                .append(Component.text(nombre, NamedTextColor.YELLOW))
                .append(Component.text("'", NamedTextColor.GREEN))
        )
    }
    
    private fun cmdAddSpawn(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("torneo.admin")) {
            sender.sendMessage(Component.text("✗ No tienes permisos", NamedTextColor.RED))
            return
        }
        
        if (sender !is Player) {
            sender.sendMessage(Component.text("✗ Solo jugadores pueden usar este comando", NamedTextColor.RED))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /carrera addspawn <arena>", NamedTextColor.YELLOW))
            return
        }
        
        val nombre = args[1]
        val arena = arenaManager.getArena(nombre)
        
        if (arena == null) {
            sender.sendMessage(Component.text("✗ Arena no encontrada", NamedTextColor.RED))
            return
        }
        
        arena.spawns.add(sender.location)
        sender.sendMessage(
            Component.text("✓ Spawn #${arena.spawns.size} añadido a '", NamedTextColor.GREEN)
                .append(Component.text(nombre, NamedTextColor.YELLOW))
                .append(Component.text("'", NamedTextColor.GREEN))
        )
    }
    
    private fun cmdAddCheckpoint(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("torneo.admin")) {
            sender.sendMessage(Component.text("✗ No tienes permisos", NamedTextColor.RED))
            return
        }
        
        if (sender !is Player) {
            sender.sendMessage(Component.text("✗ Solo jugadores pueden usar este comando", NamedTextColor.RED))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /carrera addcheckpoint <arena>", NamedTextColor.YELLOW))
            return
        }
        
        val nombre = args[1]
        val arena = arenaManager.getArena(nombre)
        
        if (arena == null) {
            sender.sendMessage(Component.text("✗ Arena no encontrada", NamedTextColor.RED))
            return
        }
        
        val selection = SelectionManager.getSelection(sender)
        if (selection == null) {
            sender.sendMessage(Component.text("✗ No tienes una selección válida", NamedTextColor.RED))
            sender.sendMessage(
                Component.text("Usa ", NamedTextColor.GRAY)
                    .append(Component.text("/carrera wand", NamedTextColor.YELLOW))
                    .append(Component.text(" para obtener la varita de selección", NamedTextColor.GRAY))
            )
            return
        }
        
        arena.checkpoints.add(selection)
        sender.sendMessage(
            Component.text("✓ Checkpoint #${arena.checkpoints.size} añadido a '", NamedTextColor.GREEN)
                .append(Component.text(nombre, NamedTextColor.YELLOW))
                .append(Component.text("'", NamedTextColor.GREEN))
        )
        sender.sendMessage(
            Component.text("Los checkpoints deben atravesarse en orden: 1 → ${arena.checkpoints.size}", NamedTextColor.GRAY)
        )
    }
    
    private fun cmdSetMeta(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("torneo.admin")) {
            sender.sendMessage(Component.text("✗ No tienes permisos", NamedTextColor.RED))
            return
        }
        
        if (sender !is Player) {
            sender.sendMessage(Component.text("✗ Solo jugadores pueden usar este comando", NamedTextColor.RED))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /carrera setmeta <arena>", NamedTextColor.YELLOW))
            return
        }
        
        val nombre = args[1]
        val arena = arenaManager.getArena(nombre)
        
        if (arena == null) {
            sender.sendMessage(Component.text("✗ Arena no encontrada", NamedTextColor.RED))
            return
        }
        
        val selection = SelectionManager.getSelection(sender)
        if (selection == null) {
            sender.sendMessage(Component.text("✗ No tienes una selección válida", NamedTextColor.RED))
            sender.sendMessage(
                Component.text("Usa ", NamedTextColor.GRAY)
                    .append(Component.text("/carrera wand", NamedTextColor.YELLOW))
                    .append(Component.text(" para obtener la varita de selección", NamedTextColor.GRAY))
            )
            return
        }
        
        arena.meta = selection
        sender.sendMessage(
            Component.text("✓ Meta establecida para '", NamedTextColor.GREEN)
                .append(Component.text(nombre, NamedTextColor.YELLOW))
                .append(Component.text("'", NamedTextColor.GREEN))
        )
    }
    
    private fun cmdSetProtection(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("torneo.admin")) {
            sender.sendMessage(Component.text("✗ No tienes permisos", NamedTextColor.RED))
            return
        }
        
        if (sender !is Player) {
            sender.sendMessage(Component.text("✗ Solo jugadores pueden usar este comando", NamedTextColor.RED))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /carrera setprotection <arena>", NamedTextColor.YELLOW))
            return
        }
        
        val nombre = args[1]
        val arena = arenaManager.getArena(nombre)
        
        if (arena == null) {
            sender.sendMessage(Component.text("✗ Arena no encontrada", NamedTextColor.RED))
            return
        }
        
        val selection = SelectionManager.getSelection(sender)
        if (selection == null) {
            sender.sendMessage(Component.text("✗ No tienes una selección válida", NamedTextColor.RED))
            sender.sendMessage(
                Component.text("Usa ", NamedTextColor.GRAY)
                    .append(Component.text("/carrera wand", NamedTextColor.YELLOW))
                    .append(Component.text(" para obtener la varita de selección", NamedTextColor.GRAY))
            )
            return
        }
        
        arena.protectionRegion = selection
        sender.sendMessage(
            Component.text("✓ Región de protección establecida para '", NamedTextColor.GREEN)
                .append(Component.text(nombre, NamedTextColor.YELLOW))
                .append(Component.text("'", NamedTextColor.GREEN))
        )
    }
    
    private fun cmdList(sender: CommandSender) {
        val arenas = arenaManager.getAllArenas()
        
        if (arenas.isEmpty()) {
            sender.sendMessage(Component.text("No hay arenas configuradas", NamedTextColor.YELLOW))
            return
        }
        
        sender.sendMessage(
            Component.text("═══ Arenas de Carrera ═══", NamedTextColor.GOLD, TextDecoration.BOLD)
        )
        
        for (arena in arenas) {
            val estado = if (arena.isValid()) "✓" else "✗"
            val color = if (arena.isValid()) NamedTextColor.GREEN else NamedTextColor.RED
            
            sender.sendMessage(
                Component.text("$estado ", color)
                    .append(Component.text(arena.nombre, NamedTextColor.YELLOW))
                    .append(Component.text(" - ${arena.spawns.size} spawns, ${arena.checkpoints.size} checkpoints", NamedTextColor.GRAY))
            )
        }
    }
    
    private fun cmdInfo(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /carrera info <arena>", NamedTextColor.YELLOW))
            return
        }
        
        val nombre = args[1]
        val arena = arenaManager.getArena(nombre)
        
        if (arena == null) {
            sender.sendMessage(Component.text("✗ Arena no encontrada", NamedTextColor.RED))
            return
        }
        
        sender.sendMessage(Component.text(arena.getSummary(), NamedTextColor.WHITE))
    }
    
    private fun cmdRemove(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("torneo.admin")) {
            sender.sendMessage(Component.text("✗ No tienes permisos", NamedTextColor.RED))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /carrera remove <arena>", NamedTextColor.YELLOW))
            return
        }
        
        val nombre = args[1]
        
        if (arenaManager.removeArena(nombre)) {
            sender.sendMessage(
                Component.text("✓ Arena '", NamedTextColor.GREEN)
                    .append(Component.text(nombre, NamedTextColor.YELLOW))
                    .append(Component.text("' eliminada", NamedTextColor.GREEN))
            )
        } else {
            sender.sendMessage(Component.text("✗ Arena no encontrada", NamedTextColor.RED))
        }
    }
    
    private fun cmdSave(sender: CommandSender) {
        if (!sender.hasPermission("torneo.admin")) {
            sender.sendMessage(Component.text("✗ No tienes permisos", NamedTextColor.RED))
            return
        }
        
        arenaManager.saveArenas()
        sender.sendMessage(
            Component.text("✓ Configuración de arenas guardada", NamedTextColor.GREEN, TextDecoration.BOLD)
        )
    }
    
    private fun cmdWand(sender: CommandSender) {
        if (!sender.hasPermission("torneo.admin")) {
            sender.sendMessage(Component.text("✗ No tienes permisos", NamedTextColor.RED))
            return
        }
        
        if (sender !is Player) {
            sender.sendMessage(Component.text("✗ Solo jugadores pueden usar este comando", NamedTextColor.RED))
            return
        }
        
        SelectionManager.toggleSelectionMode(sender)
    }
    
    // ========== COMANDOS DE JUGADOR ==========
    
    private fun cmdUnirse(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("✗ Solo jugadores pueden usar este comando", NamedTextColor.RED))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /carrera unirse <arena>", NamedTextColor.YELLOW))
            return
        }
        
        val nombre = args[1]
        val arena = arenaManager.getArena(nombre)
        
        if (arena == null) {
            sender.sendMessage(Component.text("✗ Arena no encontrada", NamedTextColor.RED))
            return
        }
        
        if (!arena.isValid()) {
            sender.sendMessage(Component.text("✗ Esta arena no está completamente configurada", NamedTextColor.RED))
            return
        }
        
        if (gameManager.estaEnCarrera(sender)) {
            sender.sendMessage(Component.text("✗ Ya estás en una carrera", NamedTextColor.RED))
            return
        }
        
        // Iniciar carrera con el jugador
        val carrera = gameManager.iniciarCarrera(arena, listOf(sender))
        
        if (carrera != null) {
            sender.sendMessage(
                Component.text("✓ Te has unido a la carrera en '", NamedTextColor.GREEN)
                    .append(Component.text(nombre, NamedTextColor.YELLOW))
                    .append(Component.text("'", NamedTextColor.GREEN))
            )
        } else {
            sender.sendMessage(Component.text("✗ Error al iniciar la carrera", NamedTextColor.RED))
        }
    }
    
    private fun cmdSalir(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("✗ Solo jugadores pueden usar este comando", NamedTextColor.RED))
            return
        }
        
        if (!gameManager.estaEnCarrera(sender)) {
            sender.sendMessage(Component.text("✗ No estás en ninguna carrera", NamedTextColor.RED))
            return
        }
        
        gameManager.removerJugadorDeCarrera(sender)
    }
    
    private fun cmdStats(sender: CommandSender) {
        if (!sender.hasPermission("torneo.admin")) {
            sender.sendMessage(Component.text("✗ No tienes permisos", NamedTextColor.RED))
            return
        }
        
        sender.sendMessage(Component.text(gameManager.getEstadisticas(), NamedTextColor.WHITE))
    }
    
    // ========== AYUDA ==========
    
    private fun showHelp(sender: CommandSender) {
        sender.sendMessage(
            Component.text("═══ Comandos de Carrera de Barcos ═══", NamedTextColor.GOLD, TextDecoration.BOLD)
        )
        
        if (sender.hasPermission("torneo.admin")) {
            sender.sendMessage(Component.text("Administración:", NamedTextColor.YELLOW, TextDecoration.BOLD))
            sender.sendMessage(Component.text("  /carrera crear <nombre>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("  /carrera setlobby <arena>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("  /carrera addspawn <arena>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("  /carrera addcheckpoint <arena>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("  /carrera setmeta <arena>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("  /carrera setprotection <arena>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("  /carrera list", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("  /carrera info <arena>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("  /carrera remove <arena>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("  /carrera save", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("  /carrera wand", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text(""))
        }
        
        sender.sendMessage(Component.text("Jugadores:", NamedTextColor.YELLOW, TextDecoration.BOLD))
        sender.sendMessage(Component.text("  /carrera unirse <arena>", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("  /carrera salir", NamedTextColor.YELLOW))
    }
    
    // ========== TAB COMPLETION ==========
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val commands = mutableListOf("unirse", "salir", "list", "info", "help")
            
            if (sender.hasPermission("torneo.admin")) {
                commands.addAll(listOf("crear", "setlobby", "addspawn", "addcheckpoint", "setmeta", "setprotection", "remove", "save", "wand", "stats"))
            }
            
            return commands.filter { it.startsWith(args[0].lowercase()) }
        }
        
        if (args.size == 2) {
            val subcommand = args[0].lowercase()
            
            // Comandos que necesitan nombre de arena
            if (subcommand in listOf("setlobby", "addspawn", "addcheckpoint", "setmeta", "setprotection", "info", "remove", "unirse")) {
                return arenaManager.getAllArenas().map { it.nombre }.filter { it.lowercase().startsWith(args[1].lowercase()) }
            }
        }
        
        return emptyList()
    }
}
