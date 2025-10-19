package los5fantasticos.minigameLaberinto

import los5fantasticos.minigameLaberinto.commands.LaberintoCommand
import los5fantasticos.minigameLaberinto.listeners.GameListener
import los5fantasticos.minigameLaberinto.listeners.PlayerQuitListener
import los5fantasticos.minigameLaberinto.services.ArenaManager
import los5fantasticos.minigameLaberinto.services.GameManager
import los5fantasticos.minigameLaberinto.services.ScoreService
import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
import org.bukkit.command.CommandExecutor
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * Manager del minijuego Laberinto.
 * 
 * Minijuego de carrera contrarreloj a través de laberintos configurables,
 * integrando mecanicas de sustos (jumpscares) y un sistema de puntuación
 * basado en la finalización.
 */
class MinigameLaberinto(val torneoPlugin: TorneoPlugin) : MinigameModule {
    
    lateinit var plugin: Plugin
        private set
    
    override val gameName = "Laberinto"
    override val version = "1.0"
    override val description = "Minijuego de carrera contrarreloj a través de laberintos con jumpscares"
    
    /**
     * Gestor de partidas activas.
     */
    lateinit var gameManager: GameManager
        private set
    
    /**
     * Gestor de arenas.
     */
    lateinit var arenaManager: ArenaManager
        private set
    
    /**
     * Servicio de puntuación.
     */
    lateinit var scoreService: ScoreService
        private set
    
    override fun onEnable(plugin: Plugin) {
        this.plugin = plugin
        
        // Cargar configuración del minijuego
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        
        // Inicializar servicios
        arenaManager = ArenaManager()
        arenaManager.initialize(plugin.dataFolder)
        
        gameManager = GameManager(this)
        
        scoreService = ScoreService(this, torneoPlugin.torneoManager)
        
        // Registrar listeners
        plugin.server.pluginManager.registerEvents(GameListener(this), plugin)
        plugin.server.pluginManager.registerEvents(PlayerQuitListener(this), plugin)
        
        plugin.logger.info("✓ $gameName v$version habilitado")
        plugin.logger.info("  - GameManager inicializado")
        plugin.logger.info("  - ArenaManager inicializado")
        plugin.logger.info("  - ScoreService inicializado")
        plugin.logger.info("  - GameListener registrado")
        plugin.logger.info("  - PlayerQuitListener registrado")
    }
    
    /**
     * Proporciona los ejecutores de comandos para registro centralizado.
     * Llamado por TorneoPlugin durante el registro del módulo.
     */
    override fun getCommandExecutors(): Map<String, CommandExecutor> {
        return mapOf(
            "laberinto" to LaberintoCommand(this)
        )
    }
    
    override fun onDisable() {
        // Guardar arenas antes de limpiar
        if (::arenaManager.isInitialized) {
            arenaManager.saveArenas()
        }
        
        // Limpiar todos los managers
        if (::gameManager.isInitialized) {
            gameManager.clearAll()
        }
        if (::arenaManager.isInitialized) {
            arenaManager.clearAll()
        }
        if (::scoreService.isInitialized) {
            scoreService.clearAll()
        }
        
        plugin.logger.info("✓ $gameName deshabilitado")
    }
    
    override fun isGameRunning(): Boolean {
        return gameManager.getActiveGames().any { it.state == los5fantasticos.minigameLaberinto.game.GameState.IN_GAME }
    }
    
    override fun getActivePlayers(): List<Player> {
        return gameManager.getActiveGames()
            .flatMap { game -> game.players }
    }
    
    /**
     * Inicia el minijuego en modo torneo centralizado.
     */
    override fun onTournamentStart(players: List<Player>) {
        plugin.logger.info("[$gameName] ═══ INICIO DE TORNEO ═══")
        plugin.logger.info("[$gameName] Teletransportando ${players.size} jugadores al lobby")
        
        // Obtener el lobby del laberinto
        val lobbyLocation = arenaManager.getLobbyLocation()
        if (lobbyLocation == null) {
            plugin.logger.severe("[$gameName] No hay lobby configurado")
            return
        }
        
        // Teletransportar todos los jugadores al lobby
        players.forEach { player ->
            try {
                player.teleport(lobbyLocation)
                plugin.logger.info("[$gameName] Jugador ${player.name} teletransportado al lobby")
            } catch (e: Exception) {
                plugin.logger.severe("[$gameName] Error teletransportando ${player.name}: ${e.message}")
                e.printStackTrace()
            }
        }
        
        plugin.logger.info("[$gameName] ✓ Torneo iniciado con ${players.size} jugadores")
    }
    
}