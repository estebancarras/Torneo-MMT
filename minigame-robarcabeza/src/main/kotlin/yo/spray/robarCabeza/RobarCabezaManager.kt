package yo.spray.robarCabeza

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import yo.spray.robarCabeza.listeners.GameListener
import yo.spray.robarCabeza.services.GameManager
import yo.spray.robarCabeza.services.HeadVisualService
import yo.spray.robarCabeza.services.RobarCabezaScoreConfig
import yo.spray.robarCabeza.services.ScoreService
import java.io.File

/**
 * Módulo principal de Robar Cabeza.
 * 
 * Actúa como punto de entrada del minijuego y orquestador de servicios.
 * Toda la lógica de juego ha sido delegada a servicios especializados.
 * 
 * Arquitectura refactorizada:
 * - GameManager: Lógica central del juego
 * - ScoreService: Gestión de puntuación
 * - GameListener: Manejo de eventos
 * - RobarCabezaGame: Modelo de estado de partida
 */
@Suppress("unused")
class RobarCabezaManager(val torneoPlugin: TorneoPlugin) : MinigameModule {

    override val gameName = "RobarCabeza"
    override val version = "4.0"
    override val description = "Minijuego de robar la cabeza del creador"

    private lateinit var plugin: Plugin
    
    /**
     * Configuración del minijuego.
     */
    private lateinit var config: FileConfiguration
    
    /**
     * Servicio de visualización de cabezas.
     */
    private lateinit var headVisualService: HeadVisualService
    
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
        
        // Cargar configuración del minijuego
        loadConfig()
        
        // Inicializar servicios
        headVisualService = HeadVisualService(plugin)
        configureHeadVisualService()
        
        scoreService = ScoreService(torneoPlugin, gameName)
        configureScoreService()
        
        gameManager = GameManager(plugin, torneoPlugin, scoreService, headVisualService)
        gameListener = GameListener(gameManager)
        
        // Cargar configuración de spawns
        loadSpawnFromConfig()
        
        // Registrar listener
        plugin.server.pluginManager.registerEvents(gameListener, plugin)
        
        // Registrar comandos
        val commandExecutor = yo.spray.robarCabeza.commands.RobarCabezaCommands(this)
        torneoPlugin.getCommand("robarcabeza")?.setExecutor(commandExecutor)
        
        plugin.logger.info("✓ $gameName v$version habilitado")
        plugin.logger.info("  - Configuración cargada")
        plugin.logger.info("  - HeadVisualService inicializado")
        plugin.logger.info("  - ScoreService inicializado")
        plugin.logger.info("  - GameManager inicializado")
        plugin.logger.info("  - GameListener registrado")
    }

    override fun onDisable() {
        // Limpiar recursos del GameManager
        if (::gameManager.isInitialized) {
            gameManager.clearAll()
        }
        
        // Limpiar recursos del HeadVisualService
        if (::headVisualService.isInitialized) {
            headVisualService.cleanup()
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
     * Añade un jugador al juego (comando /robarcabeza join).
     */
    fun joinGame(player: Player) {
        gameManager.addPlayer(player)
    }

    /**
     * Remueve un jugador del juego (comando /robarcabeza leave).
     */
    fun removePlayerFromGame(player: Player) {
        gameManager.removePlayer(player)
    }

    /**
     * Da la cabeza a un jugador específico (comando de admin).
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
        torneoPlugin.config.set("robarcabeza.gameSpawn.x", location.x)
        torneoPlugin.config.set("robarcabeza.gameSpawn.y", location.y)
        torneoPlugin.config.set("robarcabeza.gameSpawn.z", location.z)
        torneoPlugin.saveConfig()
    }

    /**
     * Establece el spawn del lobby.
     */
    fun setLobbySpawn(location: Location) {
        gameManager.lobbySpawn = location
        torneoPlugin.config.set("robarcabeza.lobbySpawn.x", location.x)
        torneoPlugin.config.set("robarcabeza.lobbySpawn.y", location.y)
        torneoPlugin.config.set("robarcabeza.lobbySpawn.z", location.z)
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

        gameManager.gameSpawn = if (cfg.contains("robarcabeza.gameSpawn.x")) {
            Location(
                world,
                cfg.getDouble("robarcabeza.gameSpawn.x"),
                cfg.getDouble("robarcabeza.gameSpawn.y"),
                cfg.getDouble("robarcabeza.gameSpawn.z")
            )
        } else {
            world.spawnLocation
        }

        gameManager.lobbySpawn = if (cfg.contains("robarcabeza.lobbySpawn.x")) {
            Location(
                world,
                cfg.getDouble("robarcabeza.lobbySpawn.x"),
                cfg.getDouble("robarcabeza.lobbySpawn.y"),
                cfg.getDouble("robarcabeza.lobbySpawn.z")
            )
        } else {
            world.spawnLocation
        }
    }
    
    /**
     * Carga el archivo de configuración robarcabeza.yml.
     */
    private fun loadConfig() {
        val configFile = File(plugin.dataFolder, "robarcabeza.yml")
        
        // Si no existe, copiar desde resources
        if (!configFile.exists()) {
            plugin.dataFolder.mkdirs()
            plugin.saveResource("robarcabeza.yml", false)
        }
        
        config = YamlConfiguration.loadConfiguration(configFile)
        plugin.logger.info("[$gameName] Configuración cargada desde robarcabeza.yml")
    }
    
    /**
     * Configura el HeadVisualService con los valores del archivo de configuración.
     */
    private fun configureHeadVisualService() {
        val scale = config.getDouble("visuals.scale", 1.5).toFloat()
        val yOffset = config.getDouble("visuals.y-offset", -0.25)
        val creatorHeads = config.getStringList("visuals.creator-heads").ifEmpty {
            listOf("Notch")
        }
        
        headVisualService.configure(scale, yOffset, creatorHeads)
        
        plugin.logger.info("[$gameName] Visual configurado: scale=$scale, offset=$yOffset, heads=${creatorHeads.size}")
    }
    
    /**
     * Configura el ScoreService con los valores del archivo de configuración.
     */
    private fun configureScoreService() {
        val pointsPerSecond = config.getInt("scoring.points-per-second", 1)
        RobarCabezaScoreConfig.POINTS_PER_SECOND = pointsPerSecond
        
        plugin.logger.info("[$gameName] Puntuación configurada: $pointsPerSecond puntos/segundo")
    }
}
