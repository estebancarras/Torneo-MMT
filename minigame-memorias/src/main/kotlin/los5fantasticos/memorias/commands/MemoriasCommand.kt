package los5fantasticos.memorias.commands

import los5fantasticos.memorias.*
import los5fantasticos.torneo.util.selection.SelectionManager
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
 * FILOSOFÍA: Convención sobre Configuración - Los spawns se calculan automáticamente.
 */
class MemoriasCommand(
    private val gameManager: GameManager,
    private val memoriasManager: MemoriasManager
) : CommandExecutor, TabCompleter {
    
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
            "varita", "wand" -> return handleVarita(sender)
            "arena" -> return handleArena(sender, args)
            "parcela" -> return handleParcela(sender, args)
            "admin" -> return handleAdmin(sender, args)
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
            sender.sendMessage(Component.text("/memorias admin startround", NamedTextColor.YELLOW)
                .append(Component.text(" - Iniciar ronda de torneo", NamedTextColor.GRAY)))
            sender.sendMessage(Component.text("/memorias varita", NamedTextColor.YELLOW)
                .append(Component.text(" - Activar/desactivar varita de selección", NamedTextColor.GRAY)))
            sender.sendMessage(Component.empty())
            sender.sendMessage(Component.text("/memorias arena create <nombre>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/memorias arena list", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/memorias arena setlobby <nombre>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/memorias arena select <nombre>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.empty())
            sender.sendMessage(Component.text("/memorias parcela add <arena>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("  (Usa la varita para seleccionar región)", NamedTextColor.GRAY))
            sender.sendMessage(Component.text("/memorias parcela list <arena>", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/memorias parcela remove <arena> <id>", NamedTextColor.YELLOW))
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
    
    private fun handleVarita(sender: CommandSender): Boolean {
        if (!checkAdmin(sender)) return true
        
        if (sender !is Player) {
            sender.sendMessage(Component.text("Solo jugadores pueden usar este comando", NamedTextColor.RED))
            return true
        }
        
        SelectionManager.toggleSelectionMode(sender)
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
            else -> {
                sender.sendMessage(Component.text("Subcomando de parcela desconocido", NamedTextColor.RED))
                return true
            }
        }
    }
    
    /**
     * Añade una nueva parcela a la arena.
     * CONVENCIÓN SOBRE CONFIGURACIÓN: Solo requiere la región, los spawns se calculan automáticamente.
     */
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
        
        // Obtener selección de la varita
        val cuboid = SelectionManager.getSelection(sender)
        if (cuboid == null) {
            sender.sendMessage(Component.text("✗ No tienes una selección válida", NamedTextColor.RED))
            sender.sendMessage(Component.text("1. Usa /memorias varita para activar el modo selección", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("2. Clic Izquierdo: Posición 1 | Clic Derecho: Posición 2", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("3. /memorias parcela add <arena>", NamedTextColor.YELLOW))
            return true
        }
        
        // Crear parcela solo con la región
        val parcela = Parcela(cuboid)
        
        arena.addParcela(parcela)
        memoriasManager.guardarArenas()
        SelectionManager.clearSelection(sender)
        
        sender.sendMessage(Component.text("✓ Parcela añadida a arena '$nombreArena'", NamedTextColor.GREEN))
        sender.sendMessage(Component.text("  Región: ${formatLocation(cuboid.getCenter())}", NamedTextColor.GRAY))
        sender.sendMessage(Component.text("  Total de parcelas: ${arena.getTotalParcelas()}", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("» Los spawns se calcularán automáticamente al iniciar un duelo", NamedTextColor.AQUA))
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
            val centro = parcela.region.getCenter()
            sender.sendMessage(
                Component.text("[$index] Centro: ", NamedTextColor.YELLOW)
                    .append(Component.text(formatLocation(centro), NamedTextColor.GRAY))
            )
            sender.sendMessage(
                Component.text("     Tamaño: ", NamedTextColor.YELLOW)
                    .append(Component.text("${parcela.region.maxX - parcela.region.minX + 1}x${parcela.region.maxZ - parcela.region.minZ + 1}", NamedTextColor.GRAY))
            )
        }
        sender.sendMessage(Component.text("» Los spawns se generan automáticamente", NamedTextColor.AQUA))
        
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
    
    /**
     * Maneja los subcomandos de administración.
     */
    private fun handleAdmin(sender: CommandSender, args: Array<out String>): Boolean {
        if (!checkAdmin(sender)) return true
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso: /memorias admin <subcomando>", NamedTextColor.RED))
            sender.sendMessage(Component.text("Subcomandos disponibles:", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("  - startround: Iniciar ronda de torneo", NamedTextColor.GRAY))
            return true
        }
        
        when (args[1].lowercase()) {
            "startround" -> return handleAdminStartRound(sender)
            else -> {
                sender.sendMessage(Component.text("Subcomando de admin desconocido", NamedTextColor.RED))
                return true
            }
        }
    }
    
    /**
     * Inicia una ronda de torneo con todos los jugadores en el lobby.
     * 
     * VALIDACIONES AUTOMÁTICAS:
     * - Número par de jugadores
     * - Suficientes parcelas disponibles
     */
    private fun handleAdminStartRound(sender: CommandSender): Boolean {
        sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("  Iniciando Ronda de Torneo", NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.GOLD))
        
        // Intentar iniciar la ronda
        val error = gameManager.startRound()
        
        if (error != null) {
            // Hubo un error - mostrar al administrador
            sender.sendMessage(Component.text("✗ $error", NamedTextColor.RED))
            
            // Si es error de número impar, dar sugerencia
            if (error.contains("impar")) {
                sender.sendMessage(Component.empty())
                sender.sendMessage(Component.text("💡 Sugerencia:", NamedTextColor.YELLOW))
                sender.sendMessage(Component.text("  Pide a un jugador comodín que use /memorias join", NamedTextColor.GRAY))
            }
        } else {
            // Éxito - obtener estadísticas
            val stats = gameManager.getStats()
            
            sender.sendMessage(Component.text("✓ Ronda iniciada exitosamente", NamedTextColor.GREEN))
            sender.sendMessage(Component.empty())
            sender.sendMessage(Component.text(stats, NamedTextColor.YELLOW))
        }
        
        sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.GOLD))
        return true
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (args.size == 1) {
            return listOf("join", "leave", "stats", "varita", "arena", "parcela", "admin").filter { it.startsWith(args[0].lowercase()) }
        }
        
        if (args.size == 2 && args[0].equals("admin", true)) {
            return listOf("startround").filter { it.startsWith(args[1].lowercase()) }
        }
        
        if (args.size == 2 && args[0].equals("arena", true)) {
            return listOf("create", "list", "setlobby", "select").filter { it.startsWith(args[1].lowercase()) }
        }
        
        if (args.size == 2 && args[0].equals("parcela", true)) {
            return listOf("add", "list", "remove").filter { it.startsWith(args[1].lowercase()) }
        }
        
        if (args.size == 3 && (args[1].equals("add", true) || args[1].equals("list", true) || args[1].equals("remove", true))) {
            return memoriasManager.obtenerArenas().map { it.nombre }.filter { it.startsWith(args[2]) }
        }
        
        return null
    }
}
