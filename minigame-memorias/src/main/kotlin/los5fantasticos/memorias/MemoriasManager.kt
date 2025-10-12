package los5fantasticos.memorias

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * Manager del minijuego Memorias.
 * 
 * Juego de memoria clásico donde los jugadores deben encontrar pares de bloques de colores.
 */
class MemoriasManager(internal val torneoPlugin: TorneoPlugin) : MinigameModule {
    
    override val gameName = "Memorias"
    override val version = "2.0"
    override val description = "Minijuego de memoria donde debes encontrar todos los pares de colores"
    
    private lateinit var plugin: Plugin
    private lateinit var gameManager: GameManager
    
    override fun onEnable(plugin: Plugin) {
        this.plugin = plugin
        
        // Inicializar el GameManager
        gameManager = GameManager(plugin, this)
        
        // Registrar eventos
        plugin.server.pluginManager.registerEvents(PlayerListener(gameManager), plugin)
        
        // Registrar comandos
        val commandExecutor = los5fantasticos.memorias.commands.MemoriasCommand(gameManager)
        torneoPlugin.getCommand("memorias")?.setExecutor(commandExecutor)
        
        plugin.logger.info("✓ $gameName v$version habilitado")
    }
    
    override fun onDisable() {
        // Terminar todos los juegos activos
        gameManager.endAllGames()
        
        plugin.logger.info("✓ $gameName deshabilitado")
    }
    
    override fun isGameRunning(): Boolean {
        return gameManager.hasActiveGames()
    }
    
    override fun getActivePlayers(): List<Player> {
        return gameManager.getAllActivePlayers()
    }
    
    
    /**
     * Registra una victoria en el torneo.
     */
    fun recordVictory(player: Player) {
        torneoPlugin.torneoManager.recordGameWon(player, gameName)
        torneoPlugin.torneoManager.recordGamePlayed(player, gameName)
    }
    
    /**
     * Registra una partida jugada.
     */
    fun recordGamePlayed(player: Player) {
        torneoPlugin.torneoManager.recordGamePlayed(player, gameName)
    }
}
