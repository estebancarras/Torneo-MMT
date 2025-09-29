package los5fantasticos.memorias

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.UUID

/**
 * Manager del minijuego Memorias.
 * 
 * Juego de memoria donde los jugadores deben memorizar y reproducir patrones de bloques.
 */
class MemoriasManager(private val torneoPlugin: TorneoPlugin) : MinigameModule {
    
    override val name = "Memorias"
    override val version = "1.0"
    override val description = "Minijuego de memoria donde debes reproducir patrones de bloques"
    
    private lateinit var plugin: Plugin
    private lateinit var gameManager: GameManager
    
    override fun onEnable(plugin: Plugin) {
        this.plugin = plugin
        
        // Inicializar el GameManager
        gameManager = GameManager(plugin, this)
        
        // Registrar eventos
        plugin.server.pluginManager.registerEvents(PlayerListener(gameManager), plugin)
        
        // Registrar comandos (se registran desde TorneoPlugin)
        torneoPlugin.getCommand("pattern")?.setExecutor(PatternCommand(gameManager))
        
        plugin.logger.info("✓ $name v$version habilitado")
    }
    
    override fun onDisable() {
        // Terminar todos los juegos activos
        gameManager.endAllGames()
        
        plugin.logger.info("✓ $name deshabilitado")
    }
    
    override fun isGameRunning(): Boolean {
        return gameManager.hasActiveGames()
    }
    
    override fun getActivePlayers(): List<Player> {
        return gameManager.getAllActivePlayers()
    }
    
    override fun awardPoints(player: Player, points: Int, reason: String) {
        torneoPlugin.torneoManager.addPoints(player, name, points, reason)
    }
    
    /**
     * Otorga puntos a un jugador por completar un patrón correctamente.
     */
    fun awardPatternPoints(player: Player, difficulty: Int) {
        val points = difficulty * 10 // 10 puntos por nivel de dificultad
        awardPoints(player, points, "Patrón completado (nivel $difficulty)")
    }
    
    /**
     * Registra una victoria en el torneo.
     */
    fun recordVictory(player: Player) {
        torneoPlugin.torneoManager.recordGameWon(player, name)
        torneoPlugin.torneoManager.recordGamePlayed(player, name)
    }
    
    /**
     * Registra una partida jugada.
     */
    fun recordGamePlayed(player: Player) {
        torneoPlugin.torneoManager.recordGamePlayed(player, name)
    }
}
