package los5fantasticos.minigameColiseo

import los5fantasticos.minigameColiseo.commands.ColiseoCommand
import los5fantasticos.minigameColiseo.listeners.GameListener
import los5fantasticos.minigameColiseo.services.*
import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
import org.bukkit.command.CommandExecutor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File

/**
 * Módulo principal del minijuego Coliseo: Élite vs Horda.
 * 
 * Minijuego PvP asimétrico donde un equipo élite de los mejores jugadores
 * se enfrenta a una horda de jugadores más numerosa.
 */
class ColiseoModule(private val torneoPlugin: TorneoPlugin) : MinigameModule {
    
    override val gameName = "Coliseo"
    override val version = "1.0"
    override val description = "PvP asimétrico: La Élite contra la Horda"
    
    private lateinit var plugin: Plugin
    private lateinit var config: YamlConfiguration
    
    // Servicios
    private lateinit var arenaManager: ArenaManager
    private lateinit var teamManager: TeamManager
    private lateinit var kitService: KitService
    private lateinit var scoreService: ScoreService
    private lateinit var coliseoScoreboardService: ColiseoScoreboardService
    private lateinit var gameManager: GameManager
    private lateinit var gameListener: GameListener
    
    override fun onEnable(plugin: Plugin) {
        this.plugin = plugin
        
        // Cargar configuración
        loadConfig()
        
        // Inicializar servicios
        arenaManager = ArenaManager()
        arenaManager.initialize(plugin.dataFolder)
        
        teamManager = TeamManager(plugin)
        kitService = KitService(config)
        scoreService = ScoreService(torneoPlugin, config, gameName)
        
        val gameDuration = config.getInt("game-duration-minutes", 10)
        gameManager = GameManager(
            plugin,
            torneoPlugin,
            teamManager,
            kitService,
            arenaManager,
            scoreService,
            gameDuration
        )
        
        coliseoScoreboardService = ColiseoScoreboardService(
            plugin,
            gameManager,
            teamManager,
            torneoPlugin.scoreboardService
        )
        
        // Inyectar el servicio de scoreboard en el GameManager
        gameManager.setScoreboardService(coliseoScoreboardService)
        
        gameListener = GameListener(
            plugin,
            gameManager,
            teamManager,
            kitService,
            scoreService,
            coliseoScoreboardService
        )
        
        // Registrar listener
        plugin.server.pluginManager.registerEvents(gameListener, plugin)
        
        plugin.logger.info("✓ $gameName v$version habilitado")
        plugin.logger.info("  - ArenaManager inicializado (${arenaManager.getAllArenas().size} arenas)")
        plugin.logger.info("  - TeamManager inicializado")
        plugin.logger.info("  - KitService inicializado")
        plugin.logger.info("  - ScoreService inicializado")
        plugin.logger.info("  - ColiseoScoreboardService inicializado")
        plugin.logger.info("  - GameManager inicializado")
        plugin.logger.info("  - GameListener registrado")
    }
    
    override fun onDisable() {
        // Finalizar partida activa si existe
        gameManager.getActiveGame()?.let { gameManager.endGame(it) }
        
        // Guardar arenas
        if (::arenaManager.isInitialized) {
            arenaManager.saveArenas()
        }
        
        // Limpiar equipos
        if (::teamManager.isInitialized) {
            teamManager.cleanup()
        }
        
        plugin.logger.info("✓ $gameName deshabilitado")
    }
    
    override fun getCommandExecutors(): Map<String, CommandExecutor> {
        return mapOf(
            "coliseo" to ColiseoCommand(arenaManager, torneoPlugin)
        )
    }
    
    override fun isGameRunning(): Boolean {
        return gameManager.isGameRunning()
    }
    
    override fun getActivePlayers(): List<Player> {
        val game = gameManager.getActiveGame() ?: return emptyList()
        return game.getAllPlayers().mapNotNull { org.bukkit.Bukkit.getPlayer(it) }
    }
    
    override fun onTournamentStart(players: List<Player>) {
        plugin.logger.info("[$gameName] ═══ INICIO DE TORNEO ═══")
        plugin.logger.info("[$gameName] Iniciando partida con ${players.size} jugadores")
        
        // Verificar que haya arenas configuradas
        if (arenaManager.getAllArenas().isEmpty()) {
            plugin.logger.severe("[$gameName] No hay arenas configuradas")
            players.forEach { it.sendMessage("§c¡No hay arenas configuradas para el Coliseo!") }
            return
        }
        
        // Iniciar el juego
        gameManager.startGame(players)
        
        plugin.logger.info("[$gameName] ✓ Partida iniciada")
    }
    
    override fun endAllGames() {
        plugin.logger.info("[$gameName] Finalizando todas las partidas activas...")
        
        gameManager.getActiveGame()?.let { game ->
            gameManager.endGame(game)
            plugin.logger.info("[$gameName] ✓ Partida finalizada")
        }
    }
    
    /**
     * Carga el archivo de configuración coliseo.yml.
     */
    private fun loadConfig() {
        val configFile = File(plugin.dataFolder, "coliseo.yml")
        
        // Si no existe, copiar desde resources
        if (!configFile.exists()) {
            plugin.dataFolder.mkdirs()
            plugin.saveResource("coliseo.yml", false)
        }
        
        config = YamlConfiguration.loadConfiguration(configFile)
        plugin.logger.info("[$gameName] Configuración cargada desde coliseo.yml")
    }
}
