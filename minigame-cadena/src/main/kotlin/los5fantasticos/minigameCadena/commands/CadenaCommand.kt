package los5fantasticos.minigameCadena.commands

import los5fantasticos.minigameCadena.MinigameCadena
import org.bukkit.ChatColor
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
            sender.sendMessage("${ChatColor.RED}Este comando solo puede ser ejecutado por jugadores.")
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
            player.sendMessage("${ChatColor.RED}Ya estás en una partida de Cadena.")
            return
        }
        
        // Añadir jugador a una partida
        val success = minigame.gameManager.addPlayer(player)
        
        if (!success) {
            player.sendMessage("${ChatColor.RED}No se pudo unir a la partida. Intenta de nuevo.")
            return
        }
        
        // Obtener información de la partida
        val game = minigame.gameManager.getPlayerGame(player)
        
        if (game == null) {
            player.sendMessage("${ChatColor.RED}Error al obtener información de la partida.")
            return
        }
        
        // Notificar al jugador
        player.sendMessage("")
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}━━━━━━━━━━ CADENA ━━━━━━━━━━")
        player.sendMessage("${ChatColor.GREEN}¡Te has unido al lobby!")
        player.sendMessage("")
        player.sendMessage("${ChatColor.YELLOW}Jugadores en lobby: ${ChatColor.WHITE}${game.getTotalPlayers()}")
        player.sendMessage("${ChatColor.YELLOW}Selecciona tu equipo: ${ChatColor.WHITE}Haz clic en una lana de color")
        player.sendMessage("")
        player.sendMessage("${ChatColor.GRAY}Esperando más jugadores...")
        player.sendMessage("${ChatColor.GRAY}Mínimo: 1 equipo con 2 jugadores para comenzar")
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        player.sendMessage("")
        
        // Notificar a otros jugadores en el lobby
        game.teams.forEach { t ->
            t.getOnlinePlayers().forEach { p ->
                if (p != player) {
                    p.sendMessage("${ChatColor.GOLD}[Cadena] ${ChatColor.YELLOW}${player.name} ${ChatColor.GRAY}se ha unido al lobby")
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
            player.sendMessage("${ChatColor.RED}No tienes permiso para usar comandos de administrador.")
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
            player.sendMessage("${ChatColor.RED}Uso: /cadena admin create <nombre>")
            return
        }
        
        val arenaName = args[1]
        
        // Verificar si ya existe
        if (minigame.arenaManager.arenaExists(arenaName)) {
            player.sendMessage("${ChatColor.RED}Ya existe una arena con ese nombre.")
            return
        }
        
        // Verificar si ya está editando otra arena
        if (minigame.arenaManager.isEditing(player.name)) {
            player.sendMessage("${ChatColor.RED}Ya estás editando una arena. Usa /cadena admin save o /cadena admin cancel primero.")
            return
        }
        
        // Crear arena en modo edición
        val arena = minigame.arenaManager.createArena(arenaName, player.location)
        minigame.arenaManager.startEditing(player.name, arena)
        
        player.sendMessage("${ChatColor.GREEN}✓ Arena '$arenaName' creada en modo edición")
        player.sendMessage("${ChatColor.YELLOW}Spawn establecido en tu ubicación actual")
        player.sendMessage("${ChatColor.GRAY}Usa los siguientes comandos para configurarla:")
        player.sendMessage("${ChatColor.GRAY}  /cadena admin addcheckpoint - Añadir checkpoints")
        player.sendMessage("${ChatColor.GRAY}  /cadena admin setfinish - Establecer meta")
        player.sendMessage("${ChatColor.GRAY}  /cadena admin setminheight <Y> - Altura mínima")
        player.sendMessage("${ChatColor.GRAY}  /cadena admin save - Guardar arena")
    }
    
    /**
     * Establece el spawn de la arena en edición.
     */
    private fun handleSetSpawn(player: Player) {
        val arena = minigame.arenaManager.getEditingArena(player.name)
        
        if (arena == null) {
            player.sendMessage("${ChatColor.RED}No estás editando ninguna arena.")
            player.sendMessage("${ChatColor.YELLOW}Usa /cadena admin create <nombre> primero.")
            return
        }
        
        // Crear nueva arena con spawn actualizado
        val newArena = arena.copy(spawnLocation = player.location.clone())
        minigame.arenaManager.startEditing(player.name, newArena)
        
        player.sendMessage("${ChatColor.GREEN}✓ Spawn actualizado")
        player.sendMessage("${ChatColor.GRAY}Ubicación: ${player.location.blockX}, ${player.location.blockY}, ${player.location.blockZ}")
    }
    
    /**
     * Añade un checkpoint a la arena en edición.
     */
    private fun handleAddCheckpoint(player: Player) {
        val arena = minigame.arenaManager.getEditingArena(player.name)
        
        if (arena == null) {
            player.sendMessage("${ChatColor.RED}No estás editando ninguna arena.")
            player.sendMessage("${ChatColor.YELLOW}Usa /cadena admin create <nombre> primero.")
            return
        }
        
        arena.addCheckpoint(player.location.clone())
        player.sendMessage("${ChatColor.GREEN}✓ Checkpoint ${arena.getCheckpointCount()} añadido")
        player.sendMessage("${ChatColor.GRAY}Ubicación: ${player.location.blockX}, ${player.location.blockY}, ${player.location.blockZ}")
    }
    
    /**
     * Establece la línea de meta.
     */
    private fun handleSetFinish(player: Player) {
        val arena = minigame.arenaManager.getEditingArena(player.name)
        
        if (arena == null) {
            player.sendMessage("${ChatColor.RED}No estás editando ninguna arena.")
            player.sendMessage("${ChatColor.YELLOW}Usa /cadena admin create <nombre> primero.")
            return
        }
        
        // Crear nueva arena con la meta actualizada
        val newArena = arena.copy(finishLocation = player.location.clone())
        minigame.arenaManager.startEditing(player.name, newArena)
        
        player.sendMessage("${ChatColor.GREEN}✓ Meta establecida")
        player.sendMessage("${ChatColor.GRAY}Ubicación: ${player.location.blockX}, ${player.location.blockY}, ${player.location.blockZ}")
    }
    
    /**
     * Establece la altura mínima.
     */
    private fun handleSetMinHeight(player: Player, args: Array<String>) {
        if (args.size < 2) {
            player.sendMessage("${ChatColor.RED}Uso: /cadena admin setminheight <altura>")
            return
        }
        
        val height = args[1].toDoubleOrNull()
        if (height == null) {
            player.sendMessage("${ChatColor.RED}Altura inválida. Debe ser un número.")
            return
        }
        
        val arena = minigame.arenaManager.getEditingArena(player.name)
        
        if (arena == null) {
            player.sendMessage("${ChatColor.RED}No estás editando ninguna arena.")
            player.sendMessage("${ChatColor.YELLOW}Usa /cadena admin create <nombre> primero.")
            return
        }
        
        // Crear nueva arena con la altura actualizada
        val newArena = arena.copy(minHeight = height)
        minigame.arenaManager.startEditing(player.name, newArena)
        
        player.sendMessage("${ChatColor.GREEN}✓ Altura mínima establecida: $height")
    }
    
    /**
     * Establece la ubicación del lobby.
     */
    private fun handleSetLobby(player: Player) {
        minigame.arenaManager.setLobbyLocation(player.location)
        minigame.arenaManager.saveArenas()
        
        player.sendMessage("${ChatColor.GREEN}${ChatColor.BOLD}✓ Ubicación del lobby establecida!")
        player.sendMessage("${ChatColor.GRAY}Los jugadores aparecerán aquí cuando se unan.")
    }
    
    /**
     * Guarda la arena en edición.
     */
    private fun handleSaveArena(player: Player) {
        val arena = minigame.arenaManager.getEditingArena(player.name)
        
        if (arena == null) {
            player.sendMessage("${ChatColor.RED}No estás editando ninguna arena.")
            return
        }
        
        // Validar que tenga al menos la meta configurada
        if (arena.checkpoints.isEmpty()) {
            player.sendMessage("${ChatColor.YELLOW}⚠ Advertencia: La arena no tiene checkpoints.")
        }
        
        // Guardar arena
        minigame.arenaManager.saveArena(arena)
        minigame.arenaManager.stopEditing(player.name)
        
        // Persistir en disco
        minigame.arenaManager.saveArenas()
        
        player.sendMessage("${ChatColor.GREEN}${ChatColor.BOLD}✓ Arena '${arena.name}' guardada exitosamente!")
        player.sendMessage("${ChatColor.GRAY}Checkpoints: ${arena.getCheckpointCount()}")
        player.sendMessage("${ChatColor.GRAY}Altura mínima: ${arena.minHeight}")
    }
    
    /**
     * Cancela la edición de una arena.
     */
    private fun handleCancelArena(player: Player) {
        val arena = minigame.arenaManager.getEditingArena(player.name)
        
        if (arena == null) {
            player.sendMessage("${ChatColor.RED}No estás editando ninguna arena.")
            return
        }
        
        minigame.arenaManager.stopEditing(player.name)
        player.sendMessage("${ChatColor.YELLOW}Edición de arena '${arena.name}' cancelada.")
    }
    
    /**
     * Lista todas las arenas.
     */
    private fun handleListArenas(player: Player) {
        val arenas = minigame.arenaManager.getAllArenas()
        
        if (arenas.isEmpty()) {
            player.sendMessage("${ChatColor.YELLOW}No hay arenas configuradas.")
            player.sendMessage("${ChatColor.GRAY}Usa /cadena admin create <nombre> para crear una.")
            return
        }
        
        player.sendMessage("")
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}━━━━━━ Arenas (${arenas.size}) ━━━━━━")
        arenas.forEach { arena ->
            player.sendMessage("${ChatColor.YELLOW}• ${arena.name} ${ChatColor.GRAY}(${arena.getCheckpointCount()} checkpoints)")
        }
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        player.sendMessage("")
    }
    
    /**
     * Elimina una arena.
     */
    private fun handleDeleteArena(player: Player, args: Array<String>) {
        if (args.size < 2) {
            player.sendMessage("${ChatColor.RED}Uso: /cadena admin delete <nombre>")
            return
        }
        
        val arenaName = args[1]
        
        if (!minigame.arenaManager.arenaExists(arenaName)) {
            player.sendMessage("${ChatColor.RED}No existe una arena con ese nombre.")
            return
        }
        
        minigame.arenaManager.deleteArena(arenaName)
        player.sendMessage("${ChatColor.GREEN}✓ Arena '$arenaName' eliminada.")
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
                player.sendMessage("${ChatColor.RED}No existe una arena con ese nombre.")
                return
            }
        } else if (editingArena != null) {
            // Mostrar arena en edición
            editingArena
        } else {
            player.sendMessage("${ChatColor.RED}Especifica el nombre de la arena: /cadena admin info <nombre>")
            player.sendMessage("${ChatColor.GRAY}O usa /cadena admin list para ver todas las arenas.")
            return
        }
        
        player.sendMessage("")
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}━━━━━━ Arena: ${arena.name} ━━━━━━")
        player.sendMessage("${ChatColor.YELLOW}Spawn: ${ChatColor.WHITE}${arena.spawnLocation.blockX}, ${arena.spawnLocation.blockY}, ${arena.spawnLocation.blockZ}")
        player.sendMessage("${ChatColor.YELLOW}Checkpoints: ${ChatColor.WHITE}${arena.getCheckpointCount()}")
        arena.checkpoints.forEachIndexed { index, checkpoint ->
            player.sendMessage("${ChatColor.GRAY}  ${index + 1}. ${checkpoint.blockX}, ${checkpoint.blockY}, ${checkpoint.blockZ}")
        }
        player.sendMessage("${ChatColor.YELLOW}Meta: ${ChatColor.WHITE}${arena.finishLocation.blockX}, ${arena.finishLocation.blockY}, ${arena.finishLocation.blockZ}")
        player.sendMessage("${ChatColor.YELLOW}Altura mínima: ${ChatColor.WHITE}${arena.minHeight}")
        if (editingArena != null) {
            player.sendMessage("${ChatColor.AQUA}Estado: ${ChatColor.WHITE}En edición")
        }
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        player.sendMessage("")
    }
    
    /**
     * Muestra la ayuda de comandos de administrador.
     */
    private fun sendAdminHelp(player: Player) {
        player.sendMessage("")
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}━━━━━━ Comandos Admin ━━━━━━")
        player.sendMessage("${ChatColor.AQUA}Gestión de Arenas:")
        player.sendMessage("${ChatColor.YELLOW}/cadena admin create <nombre> ${ChatColor.GRAY}- Crear arena")
        player.sendMessage("${ChatColor.YELLOW}/cadena admin list ${ChatColor.GRAY}- Listar arenas")
        player.sendMessage("${ChatColor.YELLOW}/cadena admin delete <nombre> ${ChatColor.GRAY}- Eliminar arena")
        player.sendMessage("${ChatColor.YELLOW}/cadena admin info [nombre] ${ChatColor.GRAY}- Ver info de arena")
        player.sendMessage("")
        player.sendMessage("${ChatColor.AQUA}Edición de Arena:")
        player.sendMessage("${ChatColor.YELLOW}/cadena admin setspawn ${ChatColor.GRAY}- Establecer spawn")
        player.sendMessage("${ChatColor.YELLOW}/cadena admin addcheckpoint ${ChatColor.GRAY}- Añadir checkpoint")
        player.sendMessage("${ChatColor.YELLOW}/cadena admin setfinish ${ChatColor.GRAY}- Establecer meta")
        player.sendMessage("${ChatColor.YELLOW}/cadena admin setminheight <Y> ${ChatColor.GRAY}- Altura mínima")
        player.sendMessage("${ChatColor.YELLOW}/cadena admin setlobby ${ChatColor.GRAY}- Establecer lobby")
        player.sendMessage("${ChatColor.YELLOW}/cadena admin save ${ChatColor.GRAY}- Guardar arena")
        player.sendMessage("${ChatColor.YELLOW}/cadena admin cancel ${ChatColor.GRAY}- Cancelar edición")
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        player.sendMessage("")
    }
    
    /**
     * Muestra la ayuda del comando.
     */
    private fun sendHelp(player: Player) {
        player.sendMessage("")
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}━━━━━━━━━━ Cadena ━━━━━━━━━━")
        player.sendMessage("${ChatColor.YELLOW}/cadena join ${ChatColor.GRAY}- Unirse a una partida")
        player.sendMessage("${ChatColor.YELLOW}/cadena admin ${ChatColor.GRAY}- Comandos de administración")
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        player.sendMessage("")
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
