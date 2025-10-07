package los5fantasticos.memorias

import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.random.Random

class GameManager(val plugin: Plugin, private val memoriasManager: MemoriasManager) {

    private val activeGames = mutableMapOf<UUID, Game>()
    private val playerToGameMap = mutableMapOf<UUID, Game>()

    private var lobbyLocation: Location? = null
    private var spawnLocation: Location? = null
    private var tableroLocation: Location? = null
    private var guessArea: Location? = null

    private val arenas = mutableListOf<Arena>()
    private var defaultGridSize = 5 // Tamaño por defecto del grid

    init {
        loadConfig()
    }

    private fun loadConfig() {
        // Cargar tamaño del grid
        defaultGridSize = plugin.config.getInt("memorias.gridSize", 5)
        if (defaultGridSize !in 3..15) {
            defaultGridSize = 5
            plugin.logger.warning("Tamaño de grid inválido en configuración. Usando 5x5 por defecto.")
        }
        
        // Cargar ubicaciones guardadas
        if (plugin.config.contains("memorias.lobby")) {
            lobbyLocation = plugin.config.getLocation("memorias.lobby")
        }
        
        if (plugin.config.contains("memorias.spawn")) {
            spawnLocation = plugin.config.getLocation("memorias.spawn")
        }
        
        if (plugin.config.contains("memorias.tablero")) {
            tableroLocation = plugin.config.getLocation("memorias.tablero")
        }
        
        if (plugin.config.contains("memorias.guessArea")) {
            guessArea = plugin.config.getLocation("memorias.guessArea")
        }
        
        // Cargar arenas guardadas
        if (plugin.config.contains("memorias.arenas")) {
            val arenasList = plugin.config.getList("memorias.arenas") as? List<Map<String, Any>>
            if (arenasList != null) {
                arenas.clear()
                arenasList.forEach { arenaData ->
                    val spawn = plugin.config.getLocation("memorias.arenas.${arenasList.indexOf(arenaData)}.spawn")
                    val tablero = plugin.config.getLocation("memorias.arenas.${arenasList.indexOf(arenaData)}.tablero")
                    val guess = plugin.config.getLocation("memorias.arenas.${arenasList.indexOf(arenaData)}.guess")
                    
                    if (spawn != null && tablero != null && guess != null) {
                        arenas.add(Arena(spawn, tablero, guess))
                    }
                }
            }
        }
        
        plugin.logger.info("Configuración de Memorias cargada: ${arenas.size} arenas, lobby: ${lobbyLocation != null}, spawn: ${spawnLocation != null}, tablero: ${tableroLocation != null}")
    }

    // Métodos para que los administradores configuren el juego
    fun setLobbyLocation(location: Location) {
        this.lobbyLocation = location
        plugin.config.set("memorias.lobby", location)
        plugin.saveConfig()
    }

    fun setSpawnLocation(location: Location) {
        this.spawnLocation = location
        plugin.config.set("memorias.spawn", location)
        plugin.saveConfig()
    }

    fun setTableroLocation(location: Location) {
        this.tableroLocation = location
        plugin.config.set("memorias.tablero", location)
        plugin.saveConfig()
    }
    
    /**
     * Establece el centro del tablero. El tablero se generará centrado en esta ubicación.
     */
    fun setTableroCenter(location: Location) {
        this.tableroLocation = location
        // También establecer guessArea en la misma ubicación (se usa el mismo tablero)
        this.guessArea = location
        plugin.config.set("memorias.tablero", location)
        plugin.config.set("memorias.guessArea", location)
        plugin.saveConfig()
    }

    fun setGuessArea(location: Location) {
        this.guessArea = location
        plugin.config.set("memorias.guessArea", location)
        plugin.saveConfig()
    }

    fun setGridSize(size: Int) {
        if (size !in 3..15) {
            plugin.logger.warning("El tamaño del grid debe estar entre 3 y 15. Usando tamaño por defecto: 5")
            return
        }
        this.defaultGridSize = size
        plugin.config.set("memorias.gridSize", size)
        plugin.saveConfig()
        plugin.logger.info("Tamaño del grid establecido a ${size}x${size}")
    }

    /**
     * Crea el arena con las ubicaciones previamente configuradas.
     */
    fun createArenaFromCurrentLocation(sender: Player) {
        // Verificar que todas las ubicaciones estén configuradas
        if (lobbyLocation == null) {
            sender.sendMessage("${org.bukkit.ChatColor.RED}✗ Falta configurar el lobby.")
            sender.sendMessage("${org.bukkit.ChatColor.YELLOW}Usa: /memorias setlobby")
            return
        }
        
        if (spawnLocation == null) {
            sender.sendMessage("${org.bukkit.ChatColor.RED}✗ Falta configurar el spawn de jugadores.")
            sender.sendMessage("${org.bukkit.ChatColor.YELLOW}Usa: /memorias setspawn")
            return
        }
        
        if (tableroLocation == null || guessArea == null) {
            sender.sendMessage("${org.bukkit.ChatColor.RED}✗ Falta configurar el tablero.")
            sender.sendMessage("${org.bukkit.ChatColor.YELLOW}Usa: /memorias settablero")
            return
        }
        
        // Crear el arena con las ubicaciones configuradas
        val newArena = Arena(spawnLocation!!, tableroLocation!!, guessArea!!)
        arenas.add(newArena)
        
        // Informar al jugador
        sender.sendMessage("${org.bukkit.ChatColor.GREEN}✓ Arena creada con éxito!")
        sender.sendMessage("${org.bukkit.ChatColor.YELLOW}Total de arenas: ${arenas.size}")
        sender.sendMessage("${org.bukkit.ChatColor.GREEN}¡Listo para jugar! Los jugadores pueden usar /memorias join")
    }

    fun joinPlayer(player: Player) {
        if (arenas.isEmpty()) {
            player.sendMessage("No hay arenas disponibles. Un administrador debe crearlas.")
            return
        }
        val game = activeGames.values.firstOrNull { it.players.size < 4 } ?: createNewGame()
        game.addPlayer(player)
        playerToGameMap[player.uniqueId] = game
        player.sendMessage("Te has unido a la cola. Jugadores: ${game.players.size}/4")
        
        // Iniciar el juego cuando haya al menos 2 jugadores
        if (game.players.size >= 2) {
            player.sendMessage("¡Hay suficientes jugadores! El juego comenzará en 3 segundos...")
            
            object : org.bukkit.scheduler.BukkitRunnable() {
                override fun run() {
                    if (game.players.size >= 2) {
                        game.startGame()
                    }
                }
            }.runTaskLater(plugin, 60L) // 3 segundos
        }
    }

    private fun createNewGame(): Game {
        val newGame = Game(this, memoriasManager, arenas[Random.nextInt(arenas.size)], defaultGridSize)
        activeGames[UUID.randomUUID()] = newGame
        return newGame
    }

    fun getGameByPlayer(player: Player): Game? {
        return playerToGameMap[player.uniqueId]
    }

    fun removePlayer(player: Player) {
        val game = playerToGameMap.remove(player.uniqueId) ?: return
        game.removePlayer(player)
        if (game.players.isEmpty()) {
            val gameId = activeGames.entries.firstOrNull { it.value == game }?.key ?: return
            activeGames.remove(gameId)
        }
    }
    
    /**
     * Remueve un jugador del juego y finaliza la partida, declarando ganador al jugador restante.
     */
    fun removePlayerAndEndGame(player: Player) {
        val game = getGameByPlayer(player)
        
        if (game == null) {
            // El jugador no está en un juego, solo enviarlo al lobby
            lobbyLocation?.let { player.teleport(it) }
            player.sendMessage("${org.bukkit.ChatColor.YELLOW}Has salido del minijuego Memorias.")
            return
        }
        
        // Si el juego está activo y hay más jugadores, terminar el juego
        if (game.players.size > 1) {
            player.sendMessage("${org.bukkit.ChatColor.YELLOW}Has salido del juego. El juego terminará.")
            
            // Obtener el otro jugador (el ganador)
            val winner = game.players.firstOrNull { it.uniqueId != player.uniqueId }
            
            // Remover al jugador que se va
            game.removePlayer(player)
            playerToGameMap.remove(player.uniqueId)
            lobbyLocation?.let { player.teleport(it) }
            
            // Declarar ganador al jugador restante
            if (winner != null) {
                game.declareWinner(winner)
            }
        } else {
            // Solo queda este jugador, simplemente removerlo
            removePlayer(player)
            lobbyLocation?.let { player.teleport(it) }
            player.sendMessage("${org.bukkit.ChatColor.YELLOW}Has salido del minijuego Memorias.")
        }
    }

    fun getLobbyLocation(): Location? {
        return lobbyLocation
    }
    
    fun hasActiveGames(): Boolean {
        return activeGames.isNotEmpty()
    }
    
    fun getAllActivePlayers(): List<Player> {
        return playerToGameMap.keys.mapNotNull { org.bukkit.Bukkit.getPlayer(it) }
    }
    
    fun endAllGames() {
        activeGames.values.forEach { it.endGame() }
        activeGames.clear()
        playerToGameMap.clear()
    }
}
