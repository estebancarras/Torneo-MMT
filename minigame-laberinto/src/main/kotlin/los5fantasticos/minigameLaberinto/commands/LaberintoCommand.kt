package los5fantasticos.minigameLaberinto.commands

import los5fantasticos.minigameLaberinto.MinigameLaberinto
import los5fantasticos.minigameLaberinto.game.GameState
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Comando principal del minijuego Laberinto.
 * 
 * Proporciona comandos para:
 * - Jugadores: unirse a partidas
 * - Administradores: gestionar arenas y configuraciones
 */
class LaberintoCommand(private val minigame: MinigameLaberinto) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "join" -> handleJoinCommand(sender)
            "leave" -> handleLeaveCommand(sender)
            "list" -> handleListCommand(sender)
            "status" -> handleStatusCommand(sender)
            "admin" -> handleAdminCommand(sender, args)
            else -> showHelp(sender)
        }
        
        return true
    }
    
    /**
     * Maneja el comando de unirse a una partida.
     */
    private fun handleJoinCommand(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores.").color(NamedTextColor.RED))
            return
        }
        
        // Verificar si ya está en una partida
        if (minigame.gameManager.getPlayerGame(sender) != null) {
            sender.sendMessage(Component.text("Ya estás en una partida de Laberinto!").color(NamedTextColor.RED))
            return
        }
        
        // Buscar una partida disponible
        val availableGame = minigame.gameManager.getActiveGames()
            .firstOrNull { it.state == GameState.LOBBY && it.players.size < it.arena.maxPlayers }
        
        if (availableGame != null) {
            if (minigame.gameManager.addPlayerToGame(sender, availableGame.gameId)) {
                sender.sendMessage(Component.text("¡Te has unido a una partida de Laberinto!").color(NamedTextColor.GREEN))
                sender.sendMessage(Component.text("Arena: ${availableGame.arena.name}").color(NamedTextColor.YELLOW))
                sender.sendMessage(Component.text("Jugadores: ${availableGame.players.size}/${availableGame.arena.maxPlayers}").color(NamedTextColor.YELLOW))
                
                if (availableGame.canStart()) {
                    sender.sendMessage(Component.text("¡La partida comenzará pronto!").color(NamedTextColor.GREEN))
                } else {
                    val needed = availableGame.arena.minPlayers - availableGame.players.size
                    sender.sendMessage(Component.text("Se necesitan $needed jugadores más para comenzar").color(NamedTextColor.YELLOW))
                }
            } else {
                sender.sendMessage(Component.text("No se pudo unir a la partida.").color(NamedTextColor.RED))
            }
        } else {
            // Crear una nueva partida automáticamente
            val availableArenas = minigame.arenaManager.getAllArenas().filter { it.isComplete() }
            if (availableArenas.isNotEmpty()) {
                val arena = availableArenas.first()
                val newGame = minigame.gameManager.createNewGame(arena)
                if (minigame.gameManager.addPlayerToGame(sender, newGame.gameId)) {
                    sender.sendMessage(Component.text("¡Te has unido a una nueva partida de Laberinto!").color(NamedTextColor.GREEN))
                    sender.sendMessage(Component.text("Arena: ${arena.name}").color(NamedTextColor.YELLOW))
                    sender.sendMessage(Component.text("Jugadores: 1/${arena.maxPlayers}").color(NamedTextColor.YELLOW))
                    
                    val needed = arena.minPlayers - 1
                    sender.sendMessage(Component.text("Se necesitan $needed jugadores más para comenzar").color(NamedTextColor.YELLOW))
                } else {
                    sender.sendMessage(Component.text("No se pudo crear una nueva partida.").color(NamedTextColor.RED))
                }
            } else {
                sender.sendMessage(Component.text("No hay arenas configuradas.").color(NamedTextColor.RED))
                sender.sendMessage(Component.text("Usa /laberinto admin create para crear una arena.").color(NamedTextColor.YELLOW))
            }
        }
    }
    
    /**
     * Maneja el comando de abandonar una partida.
     */
    private fun handleLeaveCommand(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores.").color(NamedTextColor.RED))
            return
        }
        
        if (minigame.gameManager.removePlayerFromGame(sender)) {
            sender.sendMessage(Component.text("Has abandonado la partida de Laberinto.").color(NamedTextColor.YELLOW))
        } else {
            sender.sendMessage(Component.text("No estás en ninguna partida de Laberinto.").color(NamedTextColor.RED))
        }
    }
    
    /**
     * Maneja el comando de listar partidas activas.
     */
    private fun handleListCommand(sender: CommandSender) {
        val activeGames = minigame.gameManager.getActiveGames()
        
        if (activeGames.isEmpty()) {
            sender.sendMessage(Component.text("No hay partidas activas de Laberinto.").color(NamedTextColor.YELLOW))
            return
        }
        
        sender.sendMessage(Component.text("Partidas activas de Laberinto:").color(NamedTextColor.GOLD))
        activeGames.forEach { game ->
            val stateText = when (game.state) {
                GameState.LOBBY -> Component.text("Lobby").color(NamedTextColor.GREEN)
                GameState.COUNTDOWN -> Component.text("Iniciando").color(NamedTextColor.YELLOW)
                GameState.IN_GAME -> Component.text("En juego").color(NamedTextColor.RED)
                GameState.FINISHED -> Component.text("Finalizando").color(NamedTextColor.GRAY)
            }
            
            sender.sendMessage(Component.text("  - ${game.arena.name}: ").color(NamedTextColor.YELLOW)
                .append(stateText)
                .append(Component.text(" (${game.players.size}/${game.arena.maxPlayers} jugadores)").color(NamedTextColor.YELLOW)))
        }
    }
    
    /**
     * Maneja el comando de estado del jugador.
     */
    private fun handleStatusCommand(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores.").color(NamedTextColor.RED))
            return
        }
        
        val game = minigame.gameManager.getPlayerGame(sender)
        
        if (game == null) {
            sender.sendMessage(Component.text("No estás en ninguna partida de Laberinto.").color(NamedTextColor.YELLOW))
            return
        }
        
        val stateText = when (game.state) {
            GameState.LOBBY -> Component.text("Esperando en el lobby").color(NamedTextColor.GREEN)
            GameState.COUNTDOWN -> Component.text("Cuenta regresiva").color(NamedTextColor.YELLOW)
            GameState.IN_GAME -> if (game.hasPlayerFinished(sender)) {
                Component.text("¡Completado!").color(NamedTextColor.GREEN)
            } else {
                Component.text("Buscando la salida").color(NamedTextColor.RED)
            }
            GameState.FINISHED -> Component.text("Partida finalizada").color(NamedTextColor.GRAY)
        }
        
        sender.sendMessage(Component.text("Estado de tu partida:").color(NamedTextColor.GOLD))
        sender.sendMessage(Component.text("Arena: ${game.arena.name}").color(NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("Estado: ").color(NamedTextColor.YELLOW).append(stateText))
        sender.sendMessage(Component.text("Jugadores: ${game.players.size}/${game.arena.maxPlayers}").color(NamedTextColor.YELLOW))
        
        if (game.state == GameState.IN_GAME && !game.hasPlayerFinished(sender)) {
            val progress = game.getPlayerProgress(sender)
            if (progress != null) {
                sender.sendMessage(Component.text("Sustos activados: ${progress.jumpscaresTriggered}").color(NamedTextColor.YELLOW))
            }
        }
    }
    
    /**
     * Maneja los comandos de administrador.
     */
    private fun handleAdminCommand(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("Los comandos de administrador solo pueden ser usados por jugadores.").color(NamedTextColor.RED))
            return
        }
        
        if (!sender.hasPermission("laberinto.admin")) {
            sender.sendMessage(Component.text("No tienes permisos para usar comandos de administrador.").color(NamedTextColor.RED))
            return
        }
        
        if (args.size < 2) {
            showAdminHelp(sender)
            return
        }
        
        when (args[1].lowercase()) {
            "setlobby" -> handleSetLobbyCommand(sender)
            "create" -> handleCreateArenaCommand(sender, args)
            "setstart" -> handleSetStartCommand(sender, args)
            "setend" -> handleSetEndCommand(sender, args)
            "addjumpscare" -> handleAddJumpscareCommand(sender, args)
            "setspectatorbounds" -> handleSetSpectatorBoundsCommand(sender, args)
            "setspectatorcorners" -> handleSetSpectatorCornersCommand(sender, args)
            "setspectatorcorners2" -> handleSetSpectatorCorners2Command(sender, args)
            "save" -> handleSaveCommand(sender)
            "list" -> handleListArenasCommand(sender)
            "delete" -> handleDeleteArenaCommand(sender, args)
            else -> showAdminHelp(sender)
        }
    }
    
    /**
     * Establece la ubicación del lobby global.
     */
    private fun handleSetLobbyCommand(player: Player) {
        minigame.arenaManager.setLobbyLocation(player.location)
        player.sendMessage(Component.text("Ubicación del lobby establecida.").color(NamedTextColor.GREEN))
    }
    
    /**
     * Crea una nueva arena.
     */
    private fun handleCreateArenaCommand(player: Player, args: Array<out String>) {
        if (args.size < 3) {
            player.sendMessage(Component.text("Uso: /laberinto admin create <nombre>").color(NamedTextColor.RED))
            return
        }
        
        val arenaName = args[2]
        
        if (minigame.arenaManager.arenaExists(arenaName)) {
            player.sendMessage(Component.text("Ya existe una arena con el nombre '$arenaName'.").color(NamedTextColor.RED))
            return
        }
        
        minigame.arenaManager.createArena(arenaName, player.world)
        player.sendMessage(Component.text("Arena '$arenaName' creada.").color(NamedTextColor.GREEN))
        player.sendMessage(Component.text("Usa /laberinto admin setstart $arenaName para configurar el punto de inicio.").color(NamedTextColor.YELLOW))
    }
    
    /**
     * Establece el punto de inicio de una arena.
     */
    private fun handleSetStartCommand(player: Player, args: Array<out String>) {
        if (args.size < 3) {
            player.sendMessage(Component.text("Uso: /laberinto admin setstart <nombre>").color(NamedTextColor.RED))
            return
        }
        
        val arenaName = args[2]
        val arena = minigame.arenaManager.getArena(arenaName)
        
        if (arena == null) {
            player.sendMessage(Component.text("No existe una arena con el nombre '$arenaName'.").color(NamedTextColor.RED))
            return
        }
        
        // Crear una nueva arena con la ubicación actualizada
        val updatedArena = arena.copy(startLocation = player.location.clone())
        minigame.arenaManager.arenas[arenaName] = updatedArena
        
        player.sendMessage(Component.text("Punto de inicio establecido para la arena '$arenaName'.").color(NamedTextColor.GREEN))
    }
    
    /**
     * Establece la región de meta de una arena.
     */
    private fun handleSetEndCommand(player: Player, args: Array<out String>) {
        if (args.size < 3) {
            player.sendMessage(Component.text("Uso: /laberinto admin setend <nombre>").color(NamedTextColor.RED))
            return
        }
        
        val arenaName = args[2]
        val arena = minigame.arenaManager.getArena(arenaName)
        
        if (arena == null) {
            player.sendMessage(Component.text("No existe una arena con el nombre '$arenaName'.").color(NamedTextColor.RED))
            return
        }
        
        // Crear una región de 3x3x3 centrada en la ubicación del jugador
        val center = player.location
        val finishRegion = org.bukkit.util.BoundingBox(
            center.x - 1.5, center.y - 1.5, center.z - 1.5,
            center.x + 1.5, center.y + 1.5, center.z + 1.5
        )
        
        // Crear una nueva arena con la región de meta actualizada
        val updatedArena = arena.copy(finishRegion = finishRegion)
        minigame.arenaManager.arenas[arenaName] = updatedArena
        
        player.sendMessage(Component.text("Región de meta establecida para la arena '$arenaName'.").color(NamedTextColor.GREEN))
    }
    
    /**
     * Añade una zona de jumpscare a una arena.
     */
    private fun handleAddJumpscareCommand(player: Player, args: Array<out String>) {
        if (args.size < 3) {
            player.sendMessage(Component.text("Uso: /laberinto admin addjumpscare <nombre>").color(NamedTextColor.RED))
            return
        }
        
        val arenaName = args[2]
        val arena = minigame.arenaManager.getArena(arenaName)
        
        if (arena == null) {
            player.sendMessage(Component.text("No existe una arena con el nombre '$arenaName'.").color(NamedTextColor.RED))
            return
        }
        
        val newJumpscareLocation = player.location.clone()
        val updatedJumpscares = arena.jumpscareLocations + newJumpscareLocation
        
        // Crear una nueva arena con la ubicación de jumpscare añadida
        val updatedArena = arena.copy(jumpscareLocations = updatedJumpscares)
        minigame.arenaManager.arenas[arenaName] = updatedArena
        
        player.sendMessage(Component.text("Zona de jumpscare añadida a la arena '$arenaName'.").color(NamedTextColor.GREEN))
        player.sendMessage(Component.text("Total de zonas de jumpscare: ${updatedJumpscares.size}").color(NamedTextColor.YELLOW))
    }
    
    /**
     * Establece los límites de espectador para una arena.
     */
    private fun handleSetSpectatorBoundsCommand(player: Player, args: Array<out String>) {
        if (args.size < 3) {
            player.sendMessage(Component.text("Uso: /laberinto admin setspectatorbounds <nombre> [radio]").color(NamedTextColor.RED))
            player.sendMessage(Component.text("Ejemplos:").color(NamedTextColor.YELLOW))
            player.sendMessage(Component.text("  /laberinto admin setspectatorbounds test - Área de 50x50x50 bloques").color(NamedTextColor.GRAY))
            player.sendMessage(Component.text("  /laberinto admin setspectatorbounds test 100 - Área de 200x200x200 bloques").color(NamedTextColor.GRAY))
            player.sendMessage(Component.text("  /laberinto admin setspectatorbounds test 10 - Área de 20x20x20 bloques").color(NamedTextColor.GRAY))
            return
        }
        
        val arenaName = args[2]
        val arena = minigame.arenaManager.getArena(arenaName)
        
        if (arena == null) {
            player.sendMessage(Component.text("No existe una arena con el nombre '$arenaName'.").color(NamedTextColor.RED))
            return
        }
        
        // Obtener el radio (por defecto 25 para un área de 50x50x50)
        val radius = if (args.size > 3) {
            try {
                args[3].toDouble()
            } catch (_: NumberFormatException) {
                player.sendMessage(Component.text("El radio debe ser un número válido.").color(NamedTextColor.RED))
                return
            }
        } else {
            25.0 // Valor por defecto
        }
        
        // Validar que el radio sea razonable
        if (radius < 5.0) {
            player.sendMessage(Component.text("El radio mínimo es 5 bloques.").color(NamedTextColor.RED))
            return
        }
        
        if (radius > 500.0) {
            player.sendMessage(Component.text("El radio máximo es 500 bloques.").color(NamedTextColor.RED))
            return
        }
        
        // Crear una región centrada en la ubicación del jugador
        val center = player.location
        val spectatorBounds = org.bukkit.util.BoundingBox(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius
        )
        
        // Crear una nueva arena con los límites de espectador actualizados
        val updatedArena = arena.copy(spectatorBounds = spectatorBounds)
        minigame.arenaManager.arenas[arenaName] = updatedArena
        
        val diameter = (radius * 2).toInt()
        player.sendMessage(Component.text("Límites de espectador establecidos para la arena '$arenaName'.").color(NamedTextColor.GREEN))
        player.sendMessage(Component.text("Los espectadores no podrán salir de un área de ${diameter}x${diameter}x${diameter} bloques.").color(NamedTextColor.YELLOW))
        player.sendMessage(Component.text("Centro: ${center.x.toInt()}, ${center.y.toInt()}, ${center.z.toInt()}").color(NamedTextColor.AQUA))
    }
    
    /**
     * Establece límites de espectador usando dos esquinas opuestas (más preciso).
     */
    private fun handleSetSpectatorCornersCommand(player: Player, args: Array<out String>) {
        if (args.size < 3) {
            player.sendMessage(Component.text("Uso: /laberinto admin setspectatorcorners <nombre>").color(NamedTextColor.RED))
            player.sendMessage(Component.text("Este comando usa tu ubicación actual como esquina 1.").color(NamedTextColor.YELLOW))
            player.sendMessage(Component.text("Después usa /laberinto admin setspectatorcorners2 <nombre> para la esquina 2.").color(NamedTextColor.YELLOW))
            return
        }
        
        val arenaName = args[2]
        val arena = minigame.arenaManager.getArena(arenaName)
        
        if (arena == null) {
            player.sendMessage(Component.text("No existe una arena con el nombre '$arenaName'.").color(NamedTextColor.RED))
            return
        }
        
        // Guardar la primera esquina en el metadata del jugador
        player.setMetadata("laberinto_corner1_$arenaName", org.bukkit.metadata.FixedMetadataValue(minigame.plugin, player.location))
        
        player.sendMessage(Component.text("Esquina 1 establecida en: ${player.location.x.toInt()}, ${player.location.y.toInt()}, ${player.location.z.toInt()}").color(NamedTextColor.GREEN))
        player.sendMessage(Component.text("Ahora ve a la esquina opuesta y usa: /laberinto admin setspectatorcorners2 $arenaName").color(NamedTextColor.YELLOW))
    }
    
    /**
     * Establece la segunda esquina para los límites de espectador.
     */
    private fun handleSetSpectatorCorners2Command(player: Player, args: Array<out String>) {
        if (args.size < 3) {
            player.sendMessage(Component.text("Uso: /laberinto admin setspectatorcorners2 <nombre>").color(NamedTextColor.RED))
            return
        }
        
        val arenaName = args[2]
        val arena = minigame.arenaManager.getArena(arenaName)
        
        if (arena == null) {
            player.sendMessage(Component.text("No existe una arena con el nombre '$arenaName'.").color(NamedTextColor.RED))
            return
        }
        
        // Obtener la primera esquina
        val corner1Metadata = player.getMetadata("laberinto_corner1_$arenaName")
        if (corner1Metadata.isEmpty()) {
            player.sendMessage(Component.text("Primero debes establecer la esquina 1 con: /laberinto admin setspectatorcorners $arenaName").color(NamedTextColor.RED))
            return
        }
        
        val corner1 = corner1Metadata[0].value() as org.bukkit.Location
        val corner2 = player.location
        
        // Crear el BoundingBox usando las dos esquinas
        val spectatorBounds = org.bukkit.util.BoundingBox(
            minOf(corner1.x, corner2.x), minOf(corner1.y, corner2.y), minOf(corner1.z, corner2.z),
            maxOf(corner1.x, corner2.x), maxOf(corner1.y, corner2.y), maxOf(corner1.z, corner2.z)
        )
        
        // Crear una nueva arena con los límites de espectador actualizados
        val updatedArena = arena.copy(spectatorBounds = spectatorBounds)
        minigame.arenaManager.arenas[arenaName] = updatedArena
        
        // Limpiar metadata
        player.removeMetadata("laberinto_corner1_$arenaName", minigame.plugin)
        
        val width = (spectatorBounds.maxX - spectatorBounds.minX).toInt()
        val height = (spectatorBounds.maxY - spectatorBounds.minY).toInt()
        val length = (spectatorBounds.maxZ - spectatorBounds.minZ).toInt()
        
        player.sendMessage(Component.text("Límites de espectador establecidos para la arena '$arenaName'.").color(NamedTextColor.GREEN))
        player.sendMessage(Component.text("Área personalizada: ${width}x${height}x${length} bloques").color(NamedTextColor.YELLOW))
        player.sendMessage(Component.text("Esquina 1: ${corner1.x.toInt()}, ${corner1.y.toInt()}, ${corner1.z.toInt()}").color(NamedTextColor.AQUA))
        player.sendMessage(Component.text("Esquina 2: ${corner2.x.toInt()}, ${corner2.y.toInt()}, ${corner2.z.toInt()}").color(NamedTextColor.AQUA))
    }
    
    /**
     * Guarda todas las configuraciones de arenas.
     */
    private fun handleSaveCommand(player: Player) {
        minigame.arenaManager.saveArenas()
        player.sendMessage(Component.text("Configuraciones guardadas.").color(NamedTextColor.GREEN))
    }
    
    /**
     * Lista todas las arenas disponibles.
     */
    private fun handleListArenasCommand(player: Player) {
        val arenas = minigame.arenaManager.getAllArenas()
        
        if (arenas.isEmpty()) {
            player.sendMessage(Component.text("No hay arenas configuradas.").color(NamedTextColor.YELLOW))
            return
        }
        
        player.sendMessage(Component.text("Arenas disponibles:").color(NamedTextColor.GOLD))
        arenas.forEach { arena ->
            val status = if (arena.isComplete()) Component.text("Completa").color(NamedTextColor.GREEN) else Component.text("Incompleta").color(NamedTextColor.RED)
            player.sendMessage(Component.text("  - ${arena.name}: ").color(NamedTextColor.YELLOW).append(status))
            player.sendMessage(Component.text("    Jumpscares: ${arena.jumpscareLocations.size}").color(NamedTextColor.GRAY))
        }
    }
    
    /**
     * Elimina una arena.
     */
    private fun handleDeleteArenaCommand(player: Player, args: Array<out String>) {
        if (args.size < 3) {
            player.sendMessage(Component.text("Uso: /laberinto admin delete <nombre>").color(NamedTextColor.RED))
            return
        }
        
        val arenaName = args[2]
        
        if (minigame.arenaManager.removeArena(arenaName)) {
            player.sendMessage(Component.text("Arena '$arenaName' eliminada.").color(NamedTextColor.GREEN))
        } else {
            player.sendMessage(Component.text("No existe una arena con el nombre '$arenaName'.").color(NamedTextColor.RED))
        }
    }
    
    /**
     * Muestra la ayuda general del comando.
     */
    private fun showHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("Comandos de Laberinto:").color(NamedTextColor.GOLD))
        sender.sendMessage(Component.text("/laberinto join - Unirse a una partida").color(NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/laberinto leave - Abandonar la partida actual").color(NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/laberinto list - Listar partidas activas").color(NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/laberinto status - Ver estado de tu partida").color(NamedTextColor.YELLOW))
        
        if (sender.hasPermission("laberinto.admin")) {
            sender.sendMessage(Component.text("/laberinto admin - Comandos de administrador").color(NamedTextColor.RED))
        }
    }
    
    /**
     * Muestra la ayuda de comandos de administrador.
     */
    private fun showAdminHelp(player: Player) {
        player.sendMessage(Component.text("Comandos de Administrador:").color(NamedTextColor.RED))
        player.sendMessage(Component.text("/laberinto admin setlobby - Establecer lobby global").color(NamedTextColor.YELLOW))
        player.sendMessage(Component.text("/laberinto admin create <nombre> - Crear nueva arena").color(NamedTextColor.YELLOW))
        player.sendMessage(Component.text("/laberinto admin setstart <nombre> - Establecer punto de inicio").color(NamedTextColor.YELLOW))
        player.sendMessage(Component.text("/laberinto admin setend <nombre> - Establecer región de meta").color(NamedTextColor.YELLOW))
        player.sendMessage(Component.text("/laberinto admin addjumpscare <nombre> - Añadir zona de jumpscare").color(NamedTextColor.YELLOW))
        player.sendMessage(Component.text("/laberinto admin setspectatorbounds <nombre> [radio] - Límites circulares").color(NamedTextColor.YELLOW))
        player.sendMessage(Component.text("/laberinto admin setspectatorcorners <nombre> - Límites personalizados (2 pasos)").color(NamedTextColor.YELLOW))
        player.sendMessage(Component.text("/laberinto admin save - Guardar configuraciones").color(NamedTextColor.YELLOW))
        player.sendMessage(Component.text("/laberinto admin list - Listar arenas").color(NamedTextColor.YELLOW))
        player.sendMessage(Component.text("/laberinto admin delete <nombre> - Eliminar arena").color(NamedTextColor.YELLOW))
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val completions = mutableListOf("join", "leave", "list", "status")
            if (sender.hasPermission("laberinto.admin")) {
                completions.add("admin")
            }
            return completions.filter { it.startsWith(args[0], ignoreCase = true) }
        }
        
        if (args.size == 2 && args[0].equals("admin", ignoreCase = true) && sender.hasPermission("laberinto.admin")) {
            val adminCompletions = listOf("setlobby", "create", "setstart", "setend", "addjumpscare", "setspectatorbounds", "setspectatorcorners", "setspectatorcorners2", "save", "list", "delete")
            return adminCompletions.filter { it.startsWith(args[1], ignoreCase = true) }
        }
        
        if (args.size == 3 && args[0].equals("admin", ignoreCase = true) && sender.hasPermission("laberinto.admin")) {
            when (args[1].lowercase()) {
                "setstart", "setend", "addjumpscare", "setspectatorbounds", "setspectatorcorners", "setspectatorcorners2", "delete" -> {
                    return minigame.arenaManager.getArenaNames().filter { it.startsWith(args[2], ignoreCase = true) }
                }
            }
        }
        
        return emptyList()
    }
}