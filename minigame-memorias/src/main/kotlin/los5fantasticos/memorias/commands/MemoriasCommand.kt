package los5fantasticos.memorias.commands

import los5fantasticos.memorias.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Comandos actualizados para el sistema refactorizado.
 * Soporta gestión de múltiples arenas y parcelas.
 */
class MemoriasCommand(
    private val gameManager: GameManager,
    private val memoriasManager: MemoriasManager
) : CommandExecutor, TabCompleter {
    
    // Almacenamiento temporal para configuración de parcelas
    private val parcelaConfig = mutableMapOf<Player, ParcelaBuilder>()
    
    data class ParcelaBuilder(
        var corner1: Location? = null,
        var corner2: Location? = null,
        var spawn1: Location? = null,
        var spawn2: Location? = null
    )
    
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
            "stats", "estadisticas" -> return handleStats(sender)
            "arena" -> return handleArena(sender, args)
            "parcela" -> return handleParcela(sender, args)
            else -> {
                sender.sendMessage(Component.text("Subcomando desconocido. Usa /memorias para ver los comandos.", NamedTextColor.RED))
                return true
            }
        }
    }
    
    private fun showHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("╔══════════════════════════════════╗", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("║  Comandos de Memorias (v2.0)  ║", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("╚══════════════════════════════════╝", NamedTextColor.GOLD))
        sender.sendMessage(Component.empty())
        sender.sendMessage(Component.text("/memorias join ", NamedTextColor.YELLOW)
            .append(Component.text("- Unirse a la cola de duelos", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/memorias leave ", NamedTextColor.YELLOW)
            .append(Component.text("- Salir del duelo actual", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/memorias stats ", NamedTextColor.YELLOW)
            .append(Component.text("- Ver estadísticas del servidor", NamedTextColor.GRAY)))
        
        if (sender.hasPermission("memorias.admin") || sender.isOp) {
            sender.sendMessage(Component.empty())
            sender.sendMessage(Component.text("═══ Comandos de Admin ═══", NamedTextColor.GOLD))
            sender.sendMessage(Component.text("/memorias arena create <nombre>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/memorias arena list", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/memorias arena setlobby <nombre>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/memorias arena select <nombre>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.empty())
            sender.sendMessage(Component.text("/memorias parcela add <arena>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/memorias parcela list <arena>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/memorias parcela remove <arena> <id>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/memorias parcela set <corner1|corner2|spawn1|spawn2>", NamedTextColor.YELLOW))
        }
        sender.sendMessage(Component.empty())
    }
    
    private fun handleJoin(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Solo jugadores pueden usar este comando", NamedTextColor.RED))
            return true
        }
        
        gameManager.joinPlayer(sender)
        return true
    }
    
    private fun handleLeave(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Solo jugadores pueden usar este comando", NamedTextColor.RED))
            return true
        }
        
        gameManager.removePlayer(sender)
        sender.sendMessage(Component.text("Has salido del duelo.", NamedTextColor.YELLOW))
        return true
    }
    
    private fun handleStats(sender: CommandSender): Boolean {
        sender.sendMessage(Component.text("═══ Estadísticas del Servidor ═══", NamedTextColor.GOLD))
        sender.sendMessage(Component.text(gameManager.getStats(), NamedTextColor.YELLOW))
        return true
    }
    
    private fun handleArena(sender: CommandSender, args: Array<out String>): Boolean {
        if (!checkAdmin(sender)) return true
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /memorias arena <create|list|setlobby|select>", NamedTextColor.RED))
            return true
        }
        
        when (args[1].lowercase()) {
            "create" -> return handleArenaCreate(sender, args)
            "list" -> return handleArenaList(sender)
            "setlobby" -> return handleArenaSetLobby(sender, args)
            "select" -> return handleArenaSelect(sender, args)
            else -> {
                sender.sendMessage(Component.text("Subcomando de arena desconocido", NamedTextColor.RED))
                return true
            }
        }
    }
    
    private fun handleArenaCreate(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 3) {
            sender.sendMessage(Component.text("Uso: /memorias arena create <nombre>", NamedTextColor.RED))
            return true
        }
        
        val nombre = args[2]
        val arena = Arena(nombre)
        memoriasManager.agregarArena(arena)
        
        sender.sendMessage(Component.text("✓ Arena '$nombre' creada exitosamente", NamedTextColor.GREEN))
        sender.sendMessage(Component.text("Ahora puedes agregar parcelas con /memorias parcela add $nombre", NamedTextColor.YELLOW))
        return true
    }
    
    private fun handleArenaList(sender: CommandSender): Boolean {
        val arenas = memoriasManager.obtenerArenas()
        
        if (arenas.isEmpty()) {
            sender.sendMessage(Component.text("No hay arenas configuradas", NamedTextColor.YELLOW))
            return true
        }
        
        sender.sendMessage(Component.text("═══ Arenas Disponibles ═══", NamedTextColor.GOLD))
        arenas.forEach { arena ->
            sender.sendMessage(Component.text("• ${arena.nombre}: ${arena.getTotalParcelas()} parcelas", NamedTextColor.YELLOW))
        }
        
        val arenaActual = gameManager.getArena()
        if (arenaActual != null) {
            sender.sendMessage(Component.text("Arena activa: ${arenaActual.nombre}", NamedTextColor.GREEN))
        }
        
        return true
    }
    
    private fun handleArenaSetLobby(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Solo jugadores pueden usar este comando", NamedTextColor.RED))
            return true
        }
        
        if (args.size < 3) {
            sender.sendMessage(Component.text("Uso: /memorias arena setlobby <nombre>", NamedTextColor.RED))
            return true
        }
        
        val nombreArena = args[2]
        val arena = memoriasManager.obtenerArena(nombreArena)
        
        if (arena == null) {
            sender.sendMessage(Component.text("Arena '$nombreArena' no encontrada", NamedTextColor.RED))
            return true
        }
        
        arena.lobbySpawn = sender.location
        memoriasManager.guardarArenas()
        
        sender.sendMessage(Component.text("✓ Lobby de arena '$nombreArena' establecido", NamedTextColor.GREEN))
        return true
    }
    
    private fun handleArenaSelect(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 3) {
            sender.sendMessage(Component.text("Uso: /memorias arena select <nombre>", NamedTextColor.RED))
            return true
        }
        
        val nombreArena = args[2]
        val arena = memoriasManager.obtenerArena(nombreArena)
        
        if (arena == null) {
            sender.sendMessage(Component.text("Arena '$nombreArena' no encontrada", NamedTextColor.RED))
            return true
        }
        
        gameManager.setArena(arena)
        sender.sendMessage(Component.text("✓ Arena '$nombreArena' seleccionada como activa", NamedTextColor.GREEN))
        return true
    }
    
    private fun handleParcela(sender: CommandSender, args: Array<out String>): Boolean {
        if (!checkAdmin(sender)) return true
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /memorias parcela <add|list|remove|set>", NamedTextColor.RED))
            return true
        }
        
        when (args[1].lowercase()) {
            "add" -> return handleParcelaAdd(sender, args)
            "list" -> return handleParcelaList(sender, args)
            "remove" -> return handleParcelaRemove(sender, args)
            "set" -> return handleParcelaSet(sender, args)
            else -> {
                sender.sendMessage(Component.text("Subcomando de parcela desconocido", NamedTextColor.RED))
                return true
            }
        }
    }
    
    private fun handleParcelaAdd(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Solo jugadores pueden usar este comando", NamedTextColor.RED))
            return true
        }
        
        if (args.size < 3) {
            sender.sendMessage(Component.text("Uso: /memorias parcela add <arena>", NamedTextColor.RED))
            return true
        }
        
        val nombreArena = args[2]
        val arena = memoriasManager.obtenerArena(nombreArena)
        
        if (arena == null) {
            sender.sendMessage(Component.text("Arena '$nombreArena' no encontrada", NamedTextColor.RED))
            return true
        }
        
        val builder = parcelaConfig[sender] ?: ParcelaBuilder()
        
        if (builder.corner1 == null || builder.corner2 == null || builder.spawn1 == null || builder.spawn2 == null) {
            sender.sendMessage(Component.text("Debes configurar todos los puntos de la parcela primero:", NamedTextColor.RED))
            sender.sendMessage(Component.text("1. /memorias parcela set corner1", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("2. /memorias parcela set corner2", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("3. /memorias parcela set spawn1", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("4. /memorias parcela set spawn2", NamedTextColor.YELLOW))
            return true
        }
        
        val cuboid = Cuboid.fromLocations(builder.corner1!!, builder.corner2!!)
        val parcela = Parcela(cuboid, builder.spawn1!!, builder.spawn2!!)
        
        arena.addParcela(parcela)
        memoriasManager.guardarArenas()
        parcelaConfig.remove(sender)
        
        sender.sendMessage(Component.text("✓ Parcela añadida a arena '$nombreArena'", NamedTextColor.GREEN))
        sender.sendMessage(Component.text("Total de parcelas: ${arena.getTotalParcelas()}", NamedTextColor.YELLOW))
        return true
    }
    
    private fun handleParcelaList(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 3) {
            sender.sendMessage(Component.text("Uso: /memorias parcela list <arena>", NamedTextColor.RED))
            return true
        }
        
        val nombreArena = args[2]
        val arena = memoriasManager.obtenerArena(nombreArena)
        
        if (arena == null) {
            sender.sendMessage(Component.text("Arena '$nombreArena' no encontrada", NamedTextColor.RED))
            return true
        }
        
        if (arena.parcelas.isEmpty()) {
            sender.sendMessage(Component.text("Arena '$nombreArena' no tiene parcelas", NamedTextColor.YELLOW))
            return true
        }
        
        sender.sendMessage(Component.text("═══ Parcelas de '$nombreArena' ═══", NamedTextColor.GOLD))
        arena.parcelas.forEachIndexed { index, parcela ->
            sender.sendMessage(Component.text("[$index] Spawn1: ${formatLocation(parcela.spawn1)}", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("     Spawn2: ${formatLocation(parcela.spawn2)}", NamedTextColor.YELLOW))
        }
        
        return true
    }
    
    private fun handleParcelaRemove(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 4) {
            sender.sendMessage(Component.text("Uso: /memorias parcela remove <arena> <id>", NamedTextColor.RED))
            return true
        }
        
        val nombreArena = args[2]
        val arena = memoriasManager.obtenerArena(nombreArena)
        
        if (arena == null) {
            sender.sendMessage(Component.text("Arena '$nombreArena' no encontrada", NamedTextColor.RED))
            return true
        }
        
        try {
            val id = args[3].toInt()
            if (arena.removeParcela(id)) {
                memoriasManager.guardarArenas()
                sender.sendMessage(Component.text("✓ Parcela $id eliminada de '$nombreArena'", NamedTextColor.GREEN))
            } else {
                sender.sendMessage(Component.text("ID de parcela inválido", NamedTextColor.RED))
            }
        } catch (e: NumberFormatException) {
            sender.sendMessage(Component.text("El ID debe ser un número", NamedTextColor.RED))
        }
        
        return true
    }
    
    private fun handleParcelaSet(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Solo jugadores pueden usar este comando", NamedTextColor.RED))
            return true
        }
        
        if (args.size < 3) {
            sender.sendMessage(Component.text("Uso: /memorias parcela set <corner1|corner2|spawn1|spawn2>", NamedTextColor.RED))
            return true
        }
        
        val builder = parcelaConfig.getOrPut(sender) { ParcelaBuilder() }
        val ubicacion = sender.location
        
        when (args[2].lowercase()) {
            "corner1" -> {
                builder.corner1 = ubicacion
                sender.sendMessage(Component.text("✓ Corner 1 establecido", NamedTextColor.GREEN))
            }
            "corner2" -> {
                builder.corner2 = ubicacion
                sender.sendMessage(Component.text("✓ Corner 2 establecido", NamedTextColor.GREEN))
            }
            "spawn1" -> {
                builder.spawn1 = ubicacion
                sender.sendMessage(Component.text("✓ Spawn 1 establecido", NamedTextColor.GREEN))
            }
            "spawn2" -> {
                builder.spawn2 = ubicacion
                sender.sendMessage(Component.text("✓ Spawn 2 establecido", NamedTextColor.GREEN))
            }
            else -> {
                sender.sendMessage(Component.text("Opción inválida. Usa: corner1, corner2, spawn1 o spawn2", NamedTextColor.RED))
                return true
            }
        }
        
        // Mostrar progreso
        val completado = listOf(
            builder.corner1 != null,
            builder.corner2 != null,
            builder.spawn1 != null,
            builder.spawn2 != null
        ).count { it }
        
        sender.sendMessage(Component.text("Progreso: $completado/4 puntos configurados", NamedTextColor.YELLOW))
        
        if (completado == 4) {
            sender.sendMessage(Component.text("¡Todos los puntos configurados! Usa /memorias parcela add <arena>", NamedTextColor.GREEN))
        }
        
        return true
    }
    
    private fun checkAdmin(sender: CommandSender): Boolean {
        if (!sender.hasPermission("memorias.admin") && !sender.isOp) {
            sender.sendMessage(Component.text("No tienes permiso para usar este comando", NamedTextColor.RED))
            return false
        }
        return true
    }
    
    private fun formatLocation(loc: Location): String {
        return "(${loc.blockX}, ${loc.blockY}, ${loc.blockZ})"
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (args.size == 1) {
            return listOf("join", "leave", "stats", "arena", "parcela").filter { it.startsWith(args[0].lowercase()) }
        }
        
        if (args.size == 2 && args[0].equals("arena", true)) {
            return listOf("create", "list", "setlobby", "select").filter { it.startsWith(args[1].lowercase()) }
        }
        
        if (args.size == 2 && args[0].equals("parcela", true)) {
            return listOf("add", "list", "remove", "set").filter { it.startsWith(args[1].lowercase()) }
        }
        
        if (args.size == 3 && args[1].equals("set", true)) {
            return listOf("corner1", "corner2", "spawn1", "spawn2").filter { it.startsWith(args[2].lowercase()) }
        }
        
        return null
    }
}
