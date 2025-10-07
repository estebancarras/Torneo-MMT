package los5fantasticos.minigameCadena

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * Manager del minijuego Cadena.
 * 
 * Juego de coordinación donde los jugadores deben formar una cadena sin romperla.
 */
class MinigameCadena(private val torneoPlugin: TorneoPlugin) : MinigameModule {
    
    private lateinit var plugin: Plugin
    
    override val gameName = "Cadena"
    override val version = "1.0"
    override val description = "Minijuego de coordinación - ¡mantén la cadena unida!"
    
    private val activePlayers = mutableSetOf<Player>()
    private var gameRunning = false
    
    override fun onEnable(plugin: Plugin) {
        this.plugin = plugin
        
        // TODO: Inicializar lógica del juego Cadena
        // - Configurar mecánicas de cadena
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
     * Inicia una nueva partida de Cadena.
     */
    @Suppress("unused")
    fun startGame(players: List<Player>) {
        if (gameRunning) {
            plugin.logger.warning("Ya hay una partida de Cadena en curso")
            return
        }
        
        gameRunning = true
        activePlayers.addAll(players)
        
        // TODO: Implementar lógica de inicio del juego
        // - Crear cadena entre jugadores
        // - Configurar mecánicas
        // - Iniciar temporizador
        
        players.forEach { player ->
            player.sendMessage("§6[Cadena] §e¡La partida ha comenzado! §7¡Mantén la cadena unida!")
            recordGamePlayed(player)
        }
        
        plugin.logger.info("Partida de Cadena iniciada con ${players.size} jugadores")
    }
    
    /**
     * Termina la partida actual.
     */
    fun endGame(winner: Player? = null) {
        if (!gameRunning) return
        
        gameRunning = false
        
        if (winner != null) {
            // El ganador recibe puntos basados en la duración de la cadena
            val points = calculatePoints()
            awardPoints(winner, points, "Cadena mantenida exitosamente")
            recordVictory(winner)
            
            // Notificar a todos los jugadores
            activePlayers.forEach { player ->
                if (player == winner) {
                    player.sendMessage("§6[Cadena] §a¡Felicidades! Has mantenido la cadena exitosamente.")
                } else {
                    player.sendMessage("§6[Cadena] §c${winner.name} ha mantenido la cadena exitosamente.")
                }
            }
        }
        
        // Limpiar jugadores activos
        activePlayers.clear()
        
        plugin.logger.info("Partida de Cadena terminada${if (winner != null) " - Ganador: ${winner.name}" else ""}")
    }
    
    /**
     * Calcula los puntos basados en la duración de la cadena.
     */
    private fun calculatePoints(): Int {
        // TODO: Implementar cálculo de puntos basado en duración
        // Por ahora, puntos fijos
        return 80
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
