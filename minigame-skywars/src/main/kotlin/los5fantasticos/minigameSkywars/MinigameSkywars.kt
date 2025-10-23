package los5fantasticos.minigameSkywars

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.event.Listener
import org.bukkit.event.EventHandler
import com.walrusone.skywarsreloaded.events.SkyWarsWinEvent
import com.walrusone.skywarsreloaded.events.SkyWarsKillEvent
import los5fantasticos.minigameSkywars.commands.SkywarsCommand
import org.bukkit.command.CommandExecutor
import los5fantasticos.torneo.services.TournamentFlowManager
import com.walrusone.skywarsreloaded.events.SkyWarsLeaveEvent

/**
 * Manager del minijuego SkyWars.
 *
 * Juego de supervivencia en el aire donde los jugadores luchan hasta que solo queda uno.
 */
class MinigameSkywars(private val torneoPlugin: TorneoPlugin) : MinigameModule, Listener {
    
    private lateinit var plugin: Plugin
    
    override val gameName = "SkyWars"
    override val version = "1.0"
    override val description = "Minijuego de supervivencia en el aire - ¡el último en pie gana!"
    
    private val activePlayers = mutableSetOf<Player>()
    private var gameRunning = false
    // Set de guardia para evitar reentradas/recursión al devolver jugadores al lobby
    private val returningPlayers = mutableSetOf<Player>()
    
    override fun onEnable(plugin: Plugin) {
        this.plugin = plugin

        // Registrar el listener para eventos de SkyWarsReloaded
        plugin.server.pluginManager.registerEvents(this, plugin)

        // TODO: Inicializar lógica del juego SkyWars
        // - Crear arenas
        // - Configurar eventos
        // - Registrar comandos

        plugin.logger.info("✓ $gameName v$version habilitado")
    }

    override fun getCommandExecutors(): Map<String, CommandExecutor> {
        val executor = SkywarsCommand(this, torneoPlugin)
        return mapOf("skywars" to executor)
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
     * Delega al comando de SkyWars para manejar la lógica específica.
     */
    override fun onTournamentStart(players: List<Player>) {
        plugin.logger.info("[$gameName] ═══ INICIO DE TORNEO ═══")
        plugin.logger.info("[$gameName] ${players.size} jugadores disponibles")
        plugin.logger.info("[$gameName] Los jugadores deben unirse a las arenas de SkyWars")
        
        // Marcar jugadores como activos
        players.forEach { player ->
            activePlayers.add(player)
        }
        
        gameRunning = true
        plugin.logger.info("[$gameName] ✓ Torneo iniciado - Usa /skywars para gestionar partidas")
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
            torneoPlugin.torneoManager.addScore(winner.uniqueId, gameName, 100, "Victoria en SkyWars")
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

    /**
     * Listener para el evento de victoria en SkyWarsReloaded.
     * Asigna puntos automáticamente al ganador.
     * Se ejecuta varias veces cuando hay varios ganadores (en caso de equipos).
     */
    @EventHandler
    fun onSkyWarsWin(event: SkyWarsWinEvent) {
        val winner = event.player // Implicitamente getPlayer()
        if (winner != null) {
            // Asignar 100 puntos por victoria
            torneoPlugin.torneoManager.addScore(winner.uniqueId, gameName, 100, "Victoria en SkyWars")
            // Registrar la victoria en estadísticas
            torneoPlugin.torneoManager.recordGameWon(winner, gameName)

            plugin.logger.info("Puntos asignados automáticamente a ${winner.name} por victoria en SkyWars")
        }
    }

    /**
     * Listener para el evento de asesinato en SkyWarsReloaded.
     * Asigna 10 puntos al asesino.
     */
    @EventHandler
    fun onSkyWarsKill(event: SkyWarsKillEvent) {
        val killer = event.killer // Implicitamente getKiller()
        val killed = event.killed // Implicitamente getKilled()
        if (killer != null && killed != null) {
            // Asignar 10 puntos por asesinato
            torneoPlugin.torneoManager.addScore(killer.uniqueId, gameName, 10, "Asesinato en SkyWars")

            plugin.logger.info("10 puntos asignados a ${killer.name} por asesinato de ${killed.name} en SkyWars")
        }
    }

    /**
     * Listener para cuando un jugador sale de una partida de SkyWarsReloaded.
     * Enviar al jugador de vuelta al lobby global del torneo usando TournamentFlowManager.
     */
    @EventHandler
    fun onSkyWarsLeave(event: SkyWarsLeaveEvent) {
        val player = event.player

        // Evitar procesar si ya estamos devolviendo a este jugador (protección contra reentradas)
        if (returningPlayers.contains(player)) {
            plugin.logger.fine("Ignorando SkyWarsLeaveEvent de ${player.name} porque ya se está devolviendo al lobby")
            return
        }

        // Eliminar de la lista de jugadores activos del minijuego si estaba presente
        if (activePlayers.remove(player)) {
            plugin.logger.info("Jugador ${player.name} eliminado de activePlayers de SkyWars")
        }

        // Marcar y programar la devolución al lobby en la siguiente tick para romper la cadena de eventos
        returningPlayers.add(player)
        try {
            // Ejecutar en la siguiente tick para evitar event reentrancy/teleport recursivo
            plugin.server.scheduler.runTask(plugin, Runnable {
                try {
                    TournamentFlowManager.returnToLobby(player)
                    plugin.logger.info("Jugador ${player.name} enviado al lobby por SkyWarsLeaveEvent (scheduled)")
                } catch (e: Exception) {
                    plugin.logger.warning("Fallo al enviar ${player.name} al lobby (scheduled): ${e.message}")
                    e.printStackTrace()
                } finally {
                    // Quitar la marca de guardia
                    returningPlayers.remove(player)
                }
            })
        } catch (e: Exception) {
            // En caso de que el scheduler lance error, limpiar la marca
            returningPlayers.remove(player)
            plugin.logger.warning("No se pudo programar devolución al lobby para ${player.name}: ${e.message}")
            e.printStackTrace()
        }
    }
}
