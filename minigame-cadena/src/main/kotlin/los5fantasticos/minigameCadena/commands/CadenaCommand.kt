package los5fantasticos.minigameCadena.commands

import los5fantasticos.minigameCadena.MinigameCadena
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Comando principal del minijuego Cadena.
 * 
 * Maneja los subcomandos:
 * - `/cadena join` - Unirse a una partida
 * - `/cadena admin` - Comandos de administración (futuro)
 */
class CadenaCommand(private val minigame: MinigameCadena) : CommandExecutor, TabCompleter {
    
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser ejecutado por jugadores.", NamedTextColor.RED))
            return true
        }
        
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "join" -> handleJoin(sender)
            "admin" -> handleAdmin(sender, args.drop(1).toTypedArray())
            else -> sendHelp(sender)
        }
        
        return true
    }
    
    /**
     * Maneja el subcomando `/cadena join`.
     */
    private fun handleJoin(player: Player) {
        // Verificar si el jugador ya está en una partida
        if (minigame.gameManager.isPlayerInGame(player)) {
            player.sendMessage(Component.text("Ya estás en una partida de Cadena.", NamedTextColor.RED))
            return
        }
        
        // Añadir jugador a una partida
        val success = minigame.gameManager.addPlayer(player)
        
        if (!success) {
            player.sendMessage(Component.text("No se pudo unir a la partida. Intenta de nuevo.", NamedTextColor.RED))
            return
        }
        
        // Obtener información de la partida
        val game = minigame.gameManager.getPlayerGame(player)
        
        if (game == null) {
            player.sendMessage(Component.text("Error al obtener información de la partida.", NamedTextColor.RED))
            return
        }
        
        // Notificar al jugador
        player.sendMessage(Component.empty())
        player.sendMessage(Component.text("━━━━━━━━━━ CADENA ━━━━━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        player.sendMessage(Component.text("¡Te has unido al lobby!", NamedTextColor.GREEN))
        player.sendMessage(Component.empty())
        player.sendMessage(Component.text("Jugadores en lobby: ", NamedTextColor.YELLOW)
            .append(Component.text("${game.getTotalPlayers()}", NamedTextColor.WHITE)))
        player.sendMessage(Component.text("Selecciona tu equipo: ", NamedTextColor.YELLOW)
            .append(Component.text("Haz clic en una lana de color", NamedTextColor.WHITE)))
        player.sendMessage(Component.empty())
        player.sendMessage(Component.text("Esperando más jugadores...", NamedTextColor.GRAY))
        player.sendMessage(Component.text("Mínimo: 1 equipo con 2 jugadores para comenzar", NamedTextColor.GRAY))
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        player.sendMessage(Component.empty())
        
        // Notificar a otros jugadores en el lobby
        game.teams.forEach { t ->
            t.getOnlinePlayers().forEach { p ->
                if (p != player) {
                    val msg = Component.text("[Cadena] ", NamedTextColor.GOLD)
                        .append(Component.text("${player.name} ", NamedTextColor.YELLOW))
                        .append(Component.text("se ha unido al lobby", NamedTextColor.GRAY))
                    p.sendMessage(msg)
                }
            }
        }
        
        // Verificar si hay suficientes jugadores para iniciar cuenta atrás
        minigame.checkStartCountdown(game)
    }
    
    /**
     * Maneja el subcomando `/cadena admin`.
     */
    private fun handleAdmin(player: Player, args: Array<String>) {
        // Verificar permisos
        if (!player.hasPermission("cadena.admin")) {
            player.sendMessage(Component.text("No tienes permiso para usar comandos de administrador.", NamedTextColor.RED))
            return
        }
        
        // PR4: Sistema completo de gestión de arenas
        if (args.isEmpty()) {
            sendAdminHelp(player)
            return
        }
        
        when (args[0].lowercase()) {
            "create" -> handleCreateArena(player, args)
            "setspawn" -> handleSetSpawn(player)
            "addcheckpoint" -> handleAddCheckpoint(player)
            "setfinish" -> handleSetFinish(player)
            "setminheight" -> handleSetMinHeight(player, args)
            "setlobby" -> handleSetLobby(player)
            "save" -> handleSaveArena(player)
            "cancel" -> handleCancelArena(player)
            "list" -> handleListArenas(player)
            "delete" -> handleDeleteArena(player, args)
            "info" -> handleArenaInfo(player, args)
            else -> sendAdminHelp(player)
        }
    }
    
    /**
     * Crea una nueva arena.
     */
    private fun handleCreateArena(player: Player, args: Array<String>) {
        if (args.size < 2) {
            player.sendMessage(Component.text("Uso: /cadena admin create <nombre>", NamedTextColor.RED))
            return
        }
        
        val arenaName = args[1]
        
        // Verificar si ya existe
        if (minigame.arenaManager.arenaExists(arenaName)) {
            player.sendMessage(Component.text("Ya existe una arena con ese nombre.", NamedTextColor.RED))
            return
        }
        
        // Verificar si ya está editando otra arena
        if (minigame.arenaManager.isEditing(player.name)) {
            player.sendMessage(Component.text("Ya estás editando una arena. Usa /cadena admin save o /cadena admin cancel primero.", NamedTextColor.RED))
            return
        }
        
        // Crear arena en modo edición
        val arena = minigame.arenaManager.createArena(arenaName, player.location)
        minigame.arenaManager.startEditing(player.name, arena)
        
        player.sendMessage(Component.text("✓ Arena '$arenaName' creada en modo edición", NamedTextColor.GREEN))
        player.sendMessage(Component.text("Spawn establecido en tu ubicación actual", NamedTextColor.YELLOW))
        player.sendMessage(Component.text("Usa los siguientes comandos para configurarla:", NamedTextColor.GRAY))
        player.sendMessage(Component.text("  /cadena admin addcheckpoint - Añadir checkpoints", NamedTextColor.GRAY))
        player.sendMessage(Component.text("  /cadena admin setfinish - Establecer meta", NamedTextColor.GRAY))
        player.sendMessage(Component.text("  /cadena admin setminheight <Y> - Altura mínima", NamedTextColor.GRAY))
        player.sendMessage(Component.text("  /cadena admin save - Guardar arena", NamedTextColor.GRAY))
    }
    
    /**
     * Establece el spawn de la arena en edición.
     */
    private fun handleSetSpawn(player: Player) {
        val arena = minigame.arenaManager.getEditingArena(player.name)
        
        if (arena == null) {
            player.sendMessage(Component.text("No estás editando ninguna arena.", NamedTextColor.RED))
            player.sendMessage(Component.text("Usa /cadena admin create <nombre> primero.", NamedTextColor.YELLOW))
            return
        }
        
        // Crear nueva arena con spawn actualizado
        val newArena = arena.copy(spawnLocation = player.location.clone())
        minigame.arenaManager.startEditing(player.name, newArena)
        
        player.sendMessage(Component.text("✓ Spawn actualizado", NamedTextColor.GREEN))
        player.sendMessage(Component.text("Ubicación: ${player.location.blockX}, ${player.location.blockY}, ${player.location.blockZ}", NamedTextColor.GRAY))
    }
    
    /**
     * Añade un checkpoint a la arena en edición.
     */
    private fun handleAddCheckpoint(player: Player) {
        val arena = minigame.arenaManager.getEditingArena(player.name)
        
        if (arena == null) {
            player.sendMessage(Component.text("No estás editando ninguna arena.", NamedTextColor.RED))
            player.sendMessage(Component.text("Usa /cadena admin create <nombre> primero.", NamedTextColor.YELLOW))
            return
        }
        
        arena.addCheckpoint(player.location.clone())
        player.sendMessage(Component.text("✓ Checkpoint ${arena.getCheckpointCount()} añadido", NamedTextColor.GREEN))
        player.sendMessage(Component.text("Ubicación: ${player.location.blockX}, ${player.location.blockY}, ${player.location.blockZ}", NamedTextColor.GRAY))
    }
    
    /**
     * Establece la línea de meta.
     */
    private fun handleSetFinish(player: Player) {
        val arena = minigame.arenaManager.getEditingArena(player.name)
        
        if (arena == null) {
            player.sendMessage(Component.text("No estás editando ninguna arena.", NamedTextColor.RED))
            player.sendMessage(Component.text("Usa /cadena admin create <nombre> primero.", NamedTextColor.YELLOW))
            return
        }
        
        // Crear nueva arena con la meta actualizada
        val newArena = arena.copy(finishLocation = player.location.clone())
        minigame.arenaManager.startEditing(player.name, newArena)
        
        player.sendMessage(Component.text("✓ Meta establecida", NamedTextColor.GREEN))
        player.sendMessage(Component.text("Ubicación: ${player.location.blockX}, ${player.location.blockY}, ${player.location.blockZ}", NamedTextColor.GRAY))
    }
    
    /**
     * Establece la altura mínima.
     */
    private fun handleSetMinHeight(player: Player, args: Array<String>) {
        if (args.size < 2) {
            player.sendMessage(Component.text("Uso: /cadena admin setminheight <altura>", NamedTextColor.RED))
            return
        }
        
        val height = args[1].toDoubleOrNull()
        if (height == null) {
            player.sendMessage(Component.text("Altura inválida. Debe ser un número.", NamedTextColor.RED))
            return
        }
        
        val arena = minigame.arenaManager.getEditingArena(player.name)
        
        if (arena == null) {
            player.sendMessage(Component.text("No estás editando ninguna arena.", NamedTextColor.RED))
            player.sendMessage(Component.text("Usa /cadena admin create <nombre> primero.", NamedTextColor.YELLOW))
            return
        }
        
        // Crear nueva arena con la altura actualizada
        val newArena = arena.copy(minHeight = height)
        minigame.arenaManager.startEditing(player.name, newArena)
        
        player.sendMessage(Component.text("✓ Altura mínima establecida: $height", NamedTextColor.GREEN))
    }
    
    /**
     * Establece la ubicación del lobby.
     */
    private fun handleSetLobby(player: Player) {
        minigame.arenaManager.setLobbyLocation(player.location)
        minigame.arenaManager.saveArenas()
        
        player.sendMessage(Component.text("✓ Ubicación del lobby establecida!", NamedTextColor.GREEN, TextDecoration.BOLD))
        player.sendMessage(Component.text("Los jugadores aparecerán aquí cuando se unan.", NamedTextColor.GRAY))
    }
    
    /**
     * Guarda la arena en edición.
     */
    private fun handleSaveArena(player: Player) {
        val arena = minigame.arenaManager.getEditingArena(player.name)
        
        if (arena == null) {
            player.sendMessage(Component.text("No estás editando ninguna arena.", NamedTextColor.RED))
            return
        }
        
        // Validar que tenga al menos la meta configurada
        if (arena.checkpoints.isEmpty()) {
            player.sendMessage(Component.text("⚠ Advertencia: La arena no tiene checkpoints.", NamedTextColor.YELLOW))
        }
        
        // Guardar arena
        minigame.arenaManager.saveArena(arena)
        minigame.arenaManager.stopEditing(player.name)
        
        // Persistir en disco
        minigame.arenaManager.saveArenas()
        
        player.sendMessage(Component.text("✓ Arena '${arena.name}' guardada exitosamente!", NamedTextColor.GREEN, TextDecoration.BOLD))
        player.sendMessage(Component.text("Checkpoints: ${arena.getCheckpointCount()}", NamedTextColor.GRAY))
        player.sendMessage(Component.text("Altura mínima: ${arena.minHeight}", NamedTextColor.GRAY))
    }
    
    /**
     * Cancela la edición de una arena.
     */
    private fun handleCancelArena(player: Player) {
        val arena = minigame.arenaManager.getEditingArena(player.name)
        
        if (arena == null) {
            player.sendMessage(Component.text("No estás editando ninguna arena.", NamedTextColor.RED))
            return
        }
        
        minigame.arenaManager.stopEditing(player.name)
        player.sendMessage(Component.text("Edición de arena '${arena.name}' cancelada.", NamedTextColor.YELLOW))
    }
    
    /**
     * Lista todas las arenas.
     */
    private fun handleListArenas(player: Player) {
        val arenas = minigame.arenaManager.getAllArenas()
        
        if (arenas.isEmpty()) {
            player.sendMessage(Component.text("No hay arenas configuradas.", NamedTextColor.YELLOW))
            player.sendMessage(Component.text("Usa /cadena admin create <nombre> para crear una.", NamedTextColor.GRAY))
            return
        }
        
        player.sendMessage(Component.empty())
        player.sendMessage(Component.text("━━━━━━ Arenas (${arenas.size}) ━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        arenas.forEach { arena ->
            player.sendMessage(Component.text("• ${arena.name} ", NamedTextColor.YELLOW)
                .append(Component.text("(${arena.getCheckpointCount()} checkpoints)", NamedTextColor.GRAY)))
        }
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        player.sendMessage(Component.empty())
    }
    
    /**
     * Elimina una arena.
     */
    private fun handleDeleteArena(player: Player, args: Array<String>) {
        if (args.size < 2) {
            player.sendMessage(Component.text("Uso: /cadena admin delete <nombre>", NamedTextColor.RED))
            return
        }
        
        val arenaName = args[1]
        
        if (!minigame.arenaManager.arenaExists(arenaName)) {
            player.sendMessage(Component.text("No existe una arena con ese nombre.", NamedTextColor.RED))
            return
        }
        
        minigame.arenaManager.deleteArena(arenaName)
        player.sendMessage(Component.text("✓ Arena '$arenaName' eliminada.", NamedTextColor.GREEN))
    }
    
    /**
     * Muestra información de una arena.
     */
    private fun handleArenaInfo(player: Player, args: Array<String>) {
        // Si está editando una arena, mostrar info de esa
        val editingArena = minigame.arenaManager.getEditingArena(player.name)
        
        val arena = if (args.size >= 2) {
            // Buscar por nombre
            val arenaName = args[1]
            minigame.arenaManager.getArena(arenaName) ?: run {
                player.sendMessage(Component.text("No existe una arena con ese nombre.", NamedTextColor.RED))
                return
            }
        } else if (editingArena != null) {
            // Mostrar arena en edición
            editingArena
        } else {
            player.sendMessage(Component.text("Especifica el nombre de la arena: /cadena admin info <nombre>", NamedTextColor.RED))
            player.sendMessage(Component.text("O usa /cadena admin list para ver todas las arenas.", NamedTextColor.GRAY))
            return
        }
        
        player.sendMessage(Component.empty())
        player.sendMessage(Component.text("━━━━━━ Arena: ${arena.name} ━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        player.sendMessage(Component.text("Spawn: ", NamedTextColor.YELLOW)
            .append(Component.text("${arena.spawnLocation.blockX}, ${arena.spawnLocation.blockY}, ${arena.spawnLocation.blockZ}", NamedTextColor.WHITE)))
        player.sendMessage(Component.text("Checkpoints: ", NamedTextColor.YELLOW)
            .append(Component.text("${arena.getCheckpointCount()}", NamedTextColor.WHITE)))
        arena.checkpoints.forEachIndexed { index, checkpoint ->
            player.sendMessage(Component.text("  ${index + 1}. ${checkpoint.blockX}, ${checkpoint.blockY}, ${checkpoint.blockZ}", NamedTextColor.GRAY))
        }
        player.sendMessage(Component.text("Meta: ", NamedTextColor.YELLOW)
            .append(Component.text("${arena.finishLocation.blockX}, ${arena.finishLocation.blockY}, ${arena.finishLocation.blockZ}", NamedTextColor.WHITE)))
        player.sendMessage(Component.text("Altura mínima: ", NamedTextColor.YELLOW)
            .append(Component.text("${arena.minHeight}", NamedTextColor.WHITE)))
        if (editingArena != null) {
            player.sendMessage(Component.text("Estado: ", NamedTextColor.AQUA)
                .append(Component.text("En edición", NamedTextColor.WHITE)))
        }
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        player.sendMessage(Component.empty())
    }
    
    /**
     * Muestra la ayuda de comandos de administrador.
     */
    private fun sendAdminHelp(player: Player) {
        player.sendMessage(Component.empty())
        player.sendMessage(Component.text("━━━━━━ Comandos Admin ━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        player.sendMessage(Component.text("Gestión de Arenas:", NamedTextColor.AQUA))
        player.sendMessage(Component.text("/cadena admin create <nombre> ", NamedTextColor.YELLOW)
            .append(Component.text("- Crear arena", NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/cadena admin list ", NamedTextColor.YELLOW)
            .append(Component.text("- Listar arenas", NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/cadena admin delete <nombre> ", NamedTextColor.YELLOW)
            .append(Component.text("- Eliminar arena", NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/cadena admin info [nombre] ", NamedTextColor.YELLOW)
            .append(Component.text("- Ver info de arena", NamedTextColor.GRAY)))
        player.sendMessage(Component.empty())
        player.sendMessage(Component.text("Edición de Arena:", NamedTextColor.AQUA))
        player.sendMessage(Component.text("/cadena admin setspawn ", NamedTextColor.YELLOW)
            .append(Component.text("- Establecer spawn", NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/cadena admin addcheckpoint ", NamedTextColor.YELLOW)
            .append(Component.text("- Añadir checkpoint", NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/cadena admin setfinish ", NamedTextColor.YELLOW)
            .append(Component.text("- Establecer meta", NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/cadena admin setminheight <Y> ", NamedTextColor.YELLOW)
            .append(Component.text("- Altura mínima", NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/cadena admin setlobby ", NamedTextColor.YELLOW)
            .append(Component.text("- Establecer lobby", NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/cadena admin save ", NamedTextColor.YELLOW)
            .append(Component.text("- Guardar arena", NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/cadena admin cancel ", NamedTextColor.YELLOW)
            .append(Component.text("- Cancelar edición", NamedTextColor.GRAY)))
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        player.sendMessage(Component.empty())
    }
    
    /**
     * Muestra la ayuda del comando.
     */
    private fun sendHelp(player: Player) {
        player.sendMessage(Component.empty())
        player.sendMessage(Component.text("━━━━━━━━━━ Cadena ━━━━━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        player.sendMessage(Component.text("/cadena join ", NamedTextColor.YELLOW)
            .append(Component.text("- Unirse a una partida", NamedTextColor.GRAY)))
        player.sendMessage(Component.text("/cadena admin ", NamedTextColor.YELLOW)
            .append(Component.text("- Comandos de administración", NamedTextColor.GRAY)))
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD))
        player.sendMessage(Component.empty())
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            val subcommands = mutableListOf("join")
            if (sender.hasPermission("cadena.admin")) {
                subcommands.add("admin")
            }
            return subcommands.filter { it.startsWith(args[0].lowercase()) }
        }
        
        if (args.size == 2 && args[0].equals("admin", ignoreCase = true)) {
            val adminCommands = listOf("create", "list", "delete", "info", "setspawn", "addcheckpoint", "setfinish", "setminheight", "save", "cancel")
            return adminCommands.filter { it.startsWith(args[1].lowercase()) }
        }
        
        if (args.size == 3 && args[0].equals("admin", ignoreCase = true)) {
            // Tab completion para comandos que requieren nombre de arena
            if (args[1].equals("delete", ignoreCase = true) || args[1].equals("info", ignoreCase = true)) {
                val arenas = minigame.arenaManager.getAllArenas()
                return arenas.map { it.name }.filter { it.startsWith(args[2], ignoreCase = true) }
            }
        }
        
        return emptyList()
    }
}
