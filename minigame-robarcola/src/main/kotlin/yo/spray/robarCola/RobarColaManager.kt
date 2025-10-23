package yo.spray.robarCola

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import yo.spray.robarCola.listeners.GameListener
import yo.spray.robarCola.services.GameManager
import yo.spray.robarCola.services.ScoreService

/**
 * Módulo principal de RobarCola.
 * 
 * Actúa como punto de entrada del minijuego y orquestador de servicios.
 * Toda la lógica de juego ha sido delegada a servicios especializados.
 * 
 * Arquitectura refactorizada:
 * - GameManager: Lógica central del juego
 * - ScoreService: Gestión de puntuación
 * - GameListener: Manejo de eventos
 * - RobarColaGame: Modelo de estado de partida
 */
@Suppress("unused")
class RobarColaManager(val torneoPlugin: TorneoPlugin) : MinigameModule {

    override val gameName = "RobarCola"
    override val version = "3.0"
    override val description = "Minijuego dinámico de robar colas entre jugadores"

    private lateinit var plugin: Plugin
    
    /**
     * Servicio de puntuación.
     */
    private lateinit var scoreService: ScoreService
    
    /**
     * Gestor central del juego.
     */
    lateinit var gameManager: GameManager
        private set
    
    /**
     * Listener de eventos del juego.
     */
    private lateinit var gameListener: GameListener

    // ===== Ciclo de vida del módulo =====

    override fun onEnable(plugin: Plugin) {
        this.plugin = plugin
        
        // Inicializar servicios
        scoreService = ScoreService(torneoPlugin, gameName)
        gameManager = GameManager(plugin, torneoPlugin, scoreService)
        gameListener = GameListener(gameManager)
        
        // Cargar configuración
        loadSpawnFromConfig()
        
        // Registrar listener
        plugin.server.pluginManager.registerEvents(gameListener, plugin)
        
        // Registrar comandos
        val commandExecutor = yo.spray.robarCola.commands.RobarColaCommands(this)
        torneoPlugin.getCommand("robarcola")?.setExecutor(commandExecutor)
        
        plugin.logger.info("✓ $gameName v$version habilitado")
        plugin.logger.info("  - ScoreService inicializado")
        plugin.logger.info("  - GameManager inicializado")
        plugin.logger.info("  - GameListener registrado")
    }

    override fun onDisable() {
        // Limpiar recursos del GameManager
        if (::gameManager.isInitialized) {
            gameManager.clearAll()
        }
        
        plugin.logger.info("✓ $gameName deshabilitado")
    }

    override fun getActivePlayers(): List<Player> {
        return if (::gameManager.isInitialized) {
            gameManager.getActivePlayers()
        } else {
            emptyList()
        }
    }

    override fun isGameRunning(): Boolean {
        return if (::gameManager.isInitialized) {
            gameManager.isGameRunning()
        } else {
            false
        }
    }
    
    /**
     * Inicia el minijuego en modo torneo centralizado.
     * Todos los jugadores son añadidos al juego automáticamente.
     */
    override fun onTournamentStart(players: List<Player>) {
        plugin.logger.info("[$gameName] ═══ INICIO DE TORNEO ═══")
        plugin.logger.info("[$gameName] Añadiendo ${players.size} jugadores al juego")
        
        // Añadir todos los jugadores al juego a través del GameManager
        players.forEach { player ->
            try {
                gameManager.addPlayer(player)
                plugin.logger.info("[$gameName] Jugador ${player.name} añadido al juego")
            } catch (e: Exception) {
                plugin.logger.severe("[$gameName] Error añadiendo ${player.name}: ${e.message}")
                e.printStackTrace()
            }
        }
        
        plugin.logger.info("[$gameName] ✓ Torneo iniciado con ${players.size} jugadores")
    }
    
    /**
     * Finaliza todas las partidas activas sin deshabilitar el módulo.
     */
    override fun endAllGames() {
        plugin.logger.info("[$gameName] Finalizando todas las partidas activas...")
        
        if (::gameManager.isInitialized && gameManager.isGameRunning()) {
            gameManager.endGame()
            plugin.logger.info("[$gameName] ✓ Partida finalizada correctamente")
        }
    }

    // ===== Métodos públicos para comandos =====

    /**
     * Añade un jugador al juego (comando /robarcola join).
     */
    fun joinGame(player: Player) {
        gameManager.addPlayer(player)
    }

    /**
     * Remueve un jugador del juego (comando /robarcola leave).
     */
    fun removePlayerFromGame(player: Player) {
        gameManager.removePlayer(player)
    }

    /**
     * Da la cola a un jugador específico (comando de admin).
     */
    fun giveTailToPlayer(player: Player) {
        val game = gameManager.getActiveGame()
        if (game != null && game.players.contains(player.uniqueId)) {
            gameManager.giveTail(player)
        } else {
            player.sendMessage("§c¡No estás en el juego!")
        }
    }

    /**
     * Establece el spawn del juego.
     */
    fun setGameSpawn(location: Location) {
        gameManager.gameSpawn = location
        torneoPlugin.config.set("robarcola.gameSpawn.x", location.x)
        torneoPlugin.config.set("robarcola.gameSpawn.y", location.y)
        torneoPlugin.config.set("robarcola.gameSpawn.z", location.z)
        torneoPlugin.saveConfig()
    }

    /**
     * Establece el spawn del lobby.
     */
    fun setLobbySpawn(location: Location) {
        gameManager.lobbySpawn = location
        torneoPlugin.config.set("robarcola.lobbySpawn.x", location.x)
        torneoPlugin.config.set("robarcola.lobbySpawn.y", location.y)
        torneoPlugin.config.set("robarcola.lobbySpawn.z", location.z)
        torneoPlugin.saveConfig()
    }

    /**
     * Inicia el juego manualmente (comando de admin).
     */
    fun startGameExternal() {
        if (!gameManager.isGameRunning()) {
            val game = gameManager.getActiveGame()
            if (game != null && game.players.size >= 2) {
                gameManager.startGame()
            }
        }
    }

    /**
     * Finaliza el juego manualmente (comando de admin).
     */
    fun endGameExternal() {
        if (gameManager.isGameRunning()) {
            gameManager.endGame()
        }
    }
    
    /**
     * Carga la configuración de spawns desde el archivo de configuración.
     */
    private fun loadSpawnFromConfig() {
        val cfg = torneoPlugin.config
        val world = Bukkit.getWorlds().first()

        gameManager.gameSpawn = if (cfg.contains("robarcola.gameSpawn.x")) {
            Location(
                world,
                cfg.getDouble("robarcola.gameSpawn.x"),
                cfg.getDouble("robarcola.gameSpawn.y"),
                cfg.getDouble("robarcola.gameSpawn.z")
            )
        } else {
            world.spawnLocation
        }

        gameManager.lobbySpawn = if (cfg.contains("robarcola.lobbySpawn.x")) {
            Location(
                world,
                cfg.getDouble("robarcola.lobbySpawn.x"),
                cfg.getDouble("robarcola.lobbySpawn.y"),
                cfg.getDouble("robarcola.lobbySpawn.z")
            )
        } else {
            world.spawnLocation
        }
    }
}