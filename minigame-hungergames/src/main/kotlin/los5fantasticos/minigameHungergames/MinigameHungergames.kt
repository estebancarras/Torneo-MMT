package los5fantasticos.minigameHungergames

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * Manager del minijuego Hunger Games.
 * 
 * Juego de supervivencia donde los jugadores luchan en un área cerrada hasta que solo queda uno.
 */
class MinigameHungergames(private val torneoPlugin: TorneoPlugin) : MinigameModule {
    
    private lateinit var plugin: Plugin
    
    override val gameName = "Hunger Games"
    override val version = "1.0"
    override val description = "Minijuego de supervivencia - ¡el último en pie sobrevive!"
    
    private val activePlayers = mutableSetOf<Player>()
    private var gameRunning = false
    
    override fun onEnable(plugin: Plugin) {
        this.plugin = plugin
        
        // TODO: Inicializar lógica del juego Hunger Games
        // - Crear arena de combate
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
    
    /**
     * Inicia el minijuego en modo torneo centralizado.
     */
    override fun onTournamentStart(players: List<Player>) {
        plugin.logger.info("[$gameName] ═══ INICIO DE TORNEO ═══")
        plugin.logger.info("[$gameName] ${players.size} jugadores disponibles")
        
        // Añadir jugadores a la lista activa
        players.forEach { player ->
            activePlayers.add(player)
        }
        
        gameRunning = true
        plugin.logger.info("[$gameName] ✓ Torneo iniciado con ${players.size} jugadores")
    }
    
    /**
     * Inicia una nueva partida de Hunger Games.
     */
    @Suppress("unused")
    fun startGame(players: List<Player>) {
        if (gameRunning) {
            plugin.logger.warning("Ya hay una partida de Hunger Games en curso")
            return
        }
        
        gameRunning = true
        activePlayers.addAll(players)
        
        // TODO: Implementar lógica de inicio del juego
        // - Teleportar jugadores a la arena
        // - Dar items iniciales
        // - Iniciar countdown
        
        players.forEach { player ->
            player.sendMessage("§6[Hunger Games] §e¡La partida ha comenzado! §7¡El último en pie sobrevive!")
            recordGamePlayed(player)
        }
        
        plugin.logger.info("Partida de Hunger Games iniciada con ${players.size} jugadores")
    }
    
    /**
     * Termina la partida actual.
     */
    fun endGame(winner: Player? = null) {
        if (!gameRunning) return
        
        gameRunning = false
        
        if (winner != null) {
            // El ganador recibe puntos
            torneoPlugin.torneoManager.addScore(winner.uniqueId, gameName, 150, "Victoria en Hunger Games")
            recordVictory(winner)
            
            // Notificar a todos los jugadores
            activePlayers.forEach { player ->
                if (player == winner) {
                    player.sendMessage("§6[Hunger Games] §a¡Felicidades! Has sobrevivido y ganado.")
                } else {
                    player.sendMessage("§6[Hunger Games] §c${winner.name} ha sobrevivido y ganado.")
                }
            }
        }
        
        // Limpiar jugadores activos
        activePlayers.clear()
        
        plugin.logger.info("Partida de Hunger Games terminada${if (winner != null) " - Ganador: ${winner.name}" else ""}")
    }
    
    /**
     * Termina todos los juegos activos.
     */
    override fun endAllGames() {
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
