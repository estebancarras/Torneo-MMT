package los5fantasticos.minigameCadena

import los5fantasticos.minigameCadena.commands.CadenaCommand
import los5fantasticos.minigameCadena.listeners.ParkourListener
import los5fantasticos.minigameCadena.listeners.PlayerQuitListener
import los5fantasticos.minigameCadena.services.ArenaManager
import los5fantasticos.minigameCadena.services.ChainService
import los5fantasticos.minigameCadena.services.GameManager
import los5fantasticos.minigameCadena.services.LobbyManager
import los5fantasticos.minigameCadena.services.ParkourService
import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
import org.bukkit.command.CommandExecutor
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * Manager del minijuego Cadena.
 * 
 * Minijuego cooperativo competitivo donde equipos de 2-4 jugadores
 * están permanentemente unidos por una cadena invisible y deben
 * completar un recorrido de parkour coordinadamente.
 */
class MinigameCadena(private val torneoPlugin: TorneoPlugin) : MinigameModule {
    
    lateinit var plugin: Plugin
        private set
    
    override val gameName = "Cadena"
    override val version = "1.0"
    override val description = "Minijuego cooperativo de parkour encadenado por equipos"
    
    /**
     * Gestor de partidas activas.
     */
    lateinit var gameManager: GameManager
        private set
    
    /**
     * Gestor de lobby y cuenta atrás.
     */
    private lateinit var lobbyManager: LobbyManager
    
    /**
     * Servicio de encadenamiento entre jugadores.
     */
    lateinit var chainService: ChainService
        private set
    
    /**
     * Servicio de lógica de parkour.
     */
    lateinit var parkourService: ParkourService
        private set
    
    /**
     * Gestor de arenas.
     */
    lateinit var arenaManager: ArenaManager
        private set
    
    override fun onEnable(plugin: Plugin) {
        this.plugin = plugin
        
        // PR2: Inicializar GameManager y LobbyManager
        gameManager = GameManager(this)
        lobbyManager = LobbyManager(this, gameManager)
        
        // PR3: Inicializar ChainService
        chainService = ChainService(this)
        
        // PR4: Inicializar ParkourService y ArenaManager
        parkourService = ParkourService(this)
        arenaManager = ArenaManager()
        
        // PR2 y PR4: Registrar listeners
        plugin.server.pluginManager.registerEvents(PlayerQuitListener(this), plugin)
        plugin.server.pluginManager.registerEvents(ParkourListener(this), plugin)
        
        // PR1: Comandos registrados centralizadamente por TorneoPlugin ✓
        // PR2: GameManager, LobbyManager y Listeners inicializados ✓
        // PR3: ChainService inicializado ✓
        // PR4: ParkourService y ParkourListener inicializados ✓
        
        plugin.logger.info("✓ $gameName v$version habilitado")
        plugin.logger.info("  - GameManager inicializado")
        plugin.logger.info("  - LobbyManager inicializado")
        plugin.logger.info("  - ChainService inicializado")
        plugin.logger.info("  - ParkourService inicializado")
        plugin.logger.info("  - ArenaManager inicializado")
        plugin.logger.info("  - PlayerQuitListener registrado")
        plugin.logger.info("  - ParkourListener registrado")
    }
    
    /**
     * Proporciona los ejecutores de comandos para registro centralizado.
     * Llamado por TorneoPlugin durante el registro del módulo.
     */
    override fun getCommandExecutors(): Map<String, CommandExecutor> {
        return mapOf(
            "cadena" to CadenaCommand(this)
        )
    }
    
    override fun onDisable() {
        // Limpiar todos los managers
        if (::gameManager.isInitialized) {
            gameManager.clearAll()
        }
        if (::lobbyManager.isInitialized) {
            lobbyManager.clearAll()
        }
        if (::chainService.isInitialized) {
            chainService.clearAll()
        }
        if (::arenaManager.isInitialized) {
            arenaManager.clearAll()
        }
        
        plugin.logger.info("✓ $gameName deshabilitado")
    }
    
    override fun isGameRunning(): Boolean {
        return gameManager.getActiveGames().any { it.state == los5fantasticos.minigameCadena.game.GameState.IN_GAME }
    }
    
    override fun getActivePlayers(): List<Player> {
        return gameManager.getActiveGames()
            .flatMap { game -> game.teams }
            .flatMap { team -> team.getOnlinePlayers() }
    }
    
    override fun awardPoints(player: Player, points: Int, reason: String) {
        torneoPlugin.torneoManager.addPoints(player, gameName, points, reason)
    }
    
    /**
     * Verifica y potencialmente inicia la cuenta atrás para una partida.
     * Llamado cuando un jugador se une al lobby.
     */
    fun checkStartCountdown(game: los5fantasticos.minigameCadena.game.CadenaGame) {
        lobbyManager.checkAndStartCountdown(game)
    }
    
    /**
     * Registra una victoria en el torneo.
     */
    private fun recordVictory(player: Player) {
        torneoPlugin.torneoManager.recordGameWon(player, gameName)
    }
    
    /**
     * Registra una partida jugada.
     */
    private fun recordGamePlayed(player: Player) {
        torneoPlugin.torneoManager.recordGamePlayed(player, gameName)
    }
}
