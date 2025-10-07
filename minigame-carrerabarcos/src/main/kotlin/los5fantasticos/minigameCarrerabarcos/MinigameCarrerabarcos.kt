package los5fantasticos.minigameCarrerabarcos

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * Manager del minijuego Carrera de Barcos.
 * 
 * Juego de carreras donde los jugadores compiten en barcos para llegar primero a la meta.
 */
class MinigameCarrerabarcos(private val torneoPlugin: TorneoPlugin) : MinigameModule {
    
    private lateinit var plugin: Plugin
    
    override val gameName = "Carrera de Barcos"
    override val version = "1.0"
    override val description = "Minijuego de carreras acuáticas - ¡el más rápido gana!"
    
    private val activePlayers = mutableSetOf<Player>()
    private var gameRunning = false
    
    override fun onEnable(plugin: Plugin) {
        this.plugin = plugin
        
        // TODO: Inicializar lógica del juego Carrera de Barcos
        // - Crear pistas de carreras
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
     * Inicia una nueva partida de Carrera de Barcos.
     */
    @Suppress("unused")
    fun startGame(players: List<Player>) {
        if (gameRunning) {
            plugin.logger.warning("Ya hay una partida de Carrera de Barcos en curso")
            return
        }
        
        gameRunning = true
        activePlayers.addAll(players)
        
        // TODO: Implementar lógica de inicio del juego
        // - Preparar pista de carreras
        // - Dar barcos a los jugadores
        // - Iniciar countdown
        
        players.forEach { player ->
            player.sendMessage("§6[Carrera de Barcos] §e¡La carrera ha comenzado! §7¡El más rápido gana!")
            recordGamePlayed(player)
        }
        
        plugin.logger.info("Partida de Carrera de Barcos iniciada con ${players.size} jugadores")
    }
    
    /**
     * Termina la partida actual.
     */
    fun endGame(winner: Player? = null) {
        if (!gameRunning) return
        
        gameRunning = false
        
        if (winner != null) {
            // El ganador recibe puntos basados en la posición
            val points = calculateWinnerPoints()
            awardPoints(winner, points, "Victoria en Carrera de Barcos")
            recordVictory(winner)
            
            // Notificar a todos los jugadores
            activePlayers.forEach { player ->
                if (player == winner) {
                    player.sendMessage("§6[Carrera de Barcos] §a¡Felicidades! Has ganado la carrera.")
                } else {
                    player.sendMessage("§6[Carrera de Barcos] §c${winner.name} ha ganado la carrera.")
                }
            }
        }
        
        // Limpiar jugadores activos
        activePlayers.clear()
        
        plugin.logger.info("Partida de Carrera de Barcos terminada${if (winner != null) " - Ganador: ${winner.name}" else ""}")
    }
    
    /**
     * Calcula los puntos para el ganador de la carrera.
     */
    private fun calculateWinnerPoints(): Int {
        return 100  // 1er lugar
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
