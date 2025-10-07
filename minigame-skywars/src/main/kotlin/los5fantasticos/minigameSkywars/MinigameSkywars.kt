package los5fantasticos.minigameSkywars

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * Manager del minijuego SkyWars.
 * 
 * Juego de supervivencia en el aire donde los jugadores luchan hasta que solo queda uno.
 */
class MinigameSkywars(private val torneoPlugin: TorneoPlugin) : MinigameModule {
    
    private lateinit var plugin: Plugin
    
    override val gameName = "SkyWars"
    override val version = "1.0"
    override val description = "Minijuego de supervivencia en el aire - ¡el último en pie gana!"
    
    private val activePlayers = mutableSetOf<Player>()
    private var gameRunning = false
    
    override fun onEnable(plugin: Plugin) {
        this.plugin = plugin
        
        // TODO: Inicializar lógica del juego SkyWars
        // - Crear arenas
        // - Configurar eventos
        // - Registrar comandos
        
        plugin.logger.info("✓ $gameName v$version habilitado")
    }
    
    override fun onDisable() {
        // Terminar todos los juegos activos
        endAllGames()
        
        plugin.logger.info("✓ $gameName deshabilitado")
    }
    
    override fun isGameRunning(): Boolean {
        return gameRunning
    }
    
    override fun getActivePlayers(): List<Player> {
        return activePlayers.toList()
    }
    
    override fun awardPoints(player: Player, points: Int, reason: String) {
        torneoPlugin.torneoManager.addPoints(player, gameName, points, reason)
    }
    
    /**
     * Inicia una nueva partida de SkyWars.
     */
    @Suppress("unused")
    fun startGame(players: List<Player>) {
        if (gameRunning) {
            plugin.logger.warning("Ya hay una partida de SkyWars en curso")
            return
        }
        
        gameRunning = true
        activePlayers.addAll(players)
        
        // TODO: Implementar lógica de inicio del juego
        // - Teleportar jugadores a las islas
        // - Dar items iniciales
        // - Iniciar countdown
        
        players.forEach { player ->
            player.sendMessage("§6[SkyWars] §e¡La partida ha comenzado! §7Último en pie gana.")
            recordGamePlayed(player)
        }
        
        plugin.logger.info("Partida de SkyWars iniciada con ${players.size} jugadores")
    }
    
    /**
     * Termina la partida actual.
     */
    fun endGame(winner: Player? = null) {
        if (!gameRunning) return
        
        gameRunning = false
        
        if (winner != null) {
            // El ganador recibe puntos
            awardPoints(winner, 100, "Victoria en SkyWars")
            recordVictory(winner)
            
            // Notificar a todos los jugadores
            activePlayers.forEach { player ->
                if (player == winner) {
                    player.sendMessage("§6[SkyWars] §a¡Felicidades! Has ganado la partida.")
                } else {
                    player.sendMessage("§6[SkyWars] §c${winner.name} ha ganado la partida.")
                }
            }
        }
        
        // Limpiar jugadores activos
        activePlayers.clear()
        
        plugin.logger.info("Partida de SkyWars terminada${if (winner != null) " - Ganador: ${winner.name}" else ""}")
    }
    
    /**
     * Termina todos los juegos activos.
     */
    private fun endAllGames() {
        if (gameRunning) {
            endGame()
        }
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
