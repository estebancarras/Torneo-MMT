package los5fantasticos.minigameLaberinto

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * Manager del minijuego Laberinto.
 * 
 * Juego de navegación donde los jugadores deben encontrar la salida del laberinto.
 */
class MinigameLaberinto(private val torneoPlugin: TorneoPlugin) : MinigameModule {
    
    private lateinit var plugin: Plugin
    
    override val gameName = "Laberinto"
    override val version = "1.0"
    override val description = "Minijuego de navegación - ¡encuentra la salida del laberinto!"
    
    private val activePlayers = mutableSetOf<Player>()
    private var gameRunning = false
    
    override fun onEnable(plugin: Plugin) {
        this.plugin = plugin
        
        // TODO: Inicializar lógica del juego Laberinto
        // - Crear laberintos
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
     * Inicia una nueva partida de Laberinto.
     */
    @Suppress("unused")
    fun startGame(players: List<Player>) {
        if (gameRunning) {
            plugin.logger.warning("Ya hay una partida de Laberinto en curso")
            return
        }
        
        gameRunning = true
        activePlayers.addAll(players)
        
        // TODO: Implementar lógica de inicio del juego
        // - Generar laberinto
        // - Teleportar jugadores al inicio
        // - Iniciar temporizador
        
        players.forEach { player ->
            player.sendMessage("§6[Laberinto] §e¡La partida ha comenzado! §7Encuentra la salida lo más rápido posible.")
            recordGamePlayed(player)
        }
        
        plugin.logger.info("Partida de Laberinto iniciada con ${players.size} jugadores")
    }
    
    /**
     * Termina la partida actual.
     */
    fun endGame(winner: Player? = null) {
        if (!gameRunning) return
        
        gameRunning = false
        
        if (winner != null) {
            // El ganador recibe puntos basados en el tiempo
            val points = calculatePoints()
            awardPoints(winner, points, "Completado el laberinto")
            recordVictory(winner)
            
            // Notificar a todos los jugadores
            activePlayers.forEach { player ->
                if (player == winner) {
                    player.sendMessage("§6[Laberinto] §a¡Felicidades! Has completado el laberinto.")
                } else {
                    player.sendMessage("§6[Laberinto] §c${winner.name} ha completado el laberinto.")
                }
            }
        }
        
        // Limpiar jugadores activos
        activePlayers.clear()
        
        plugin.logger.info("Partida de Laberinto terminada${if (winner != null) " - Ganador: ${winner.name}" else ""}")
    }
    
    /**
     * Calcula los puntos basados en el tiempo de completado.
     */
    private fun calculatePoints(): Int {
        // TODO: Implementar cálculo de puntos basado en tiempo
        // Por ahora, puntos fijos
        return 75
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
