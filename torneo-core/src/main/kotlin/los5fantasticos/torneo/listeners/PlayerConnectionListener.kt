package los5fantasticos.torneo.listeners

import los5fantasticos.torneo.services.GlobalScoreboardService
import los5fantasticos.torneo.services.TournamentFlowManager
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin

/**
 * Listener para eventos de conexión de jugadores.
 * 
 * Responsabilidades:
 * - Asignar el scoreboard global a jugadores cuando se conectan
 * - Teletransportar jugadores al lobby global del DUOC (incluso primera vez)
 */
class PlayerConnectionListener(
    private val scoreboardService: GlobalScoreboardService
) : Listener {
    
    private lateinit var plugin: Plugin
    
    /**
     * Inicializa el listener con la instancia del plugin.
     * Necesario para programar tareas asíncronas.
     */
    fun initialize(plugin: Plugin) {
        this.plugin = plugin
    }
    
    /**
     * Maneja el evento de conexión de un jugador.
     * Asigna el scoreboard global y teletransporta al lobby DUOC.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // Asignar el scoreboard global al jugador
        scoreboardService.showToPlayer(player)
        
        // Teletransportar al lobby global después de un pequeño delay
        // (para asegurar que los chunks se carguen correctamente)
        if (::plugin.isInitialized) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                // Verificar que el jugador sigue online
                if (player.isOnline) {
                    // Verificar que hay spawns configurados
                    val lobbySpawns = TournamentFlowManager.getLobbySpawns()
                    if (lobbySpawns.isNotEmpty()) {
                        // Teletransportar al lobby DUOC
                        TournamentFlowManager.returnToLobby(player)
                        
                        Bukkit.getLogger().info("[TournamentFlow] Jugador ${player.name} teletransportado al lobby DUOC")
                    } else {
                        Bukkit.getLogger().warning("[TournamentFlow] No hay spawns configurados para el lobby DUOC. Jugador ${player.name} permanece en spawn por defecto.")
                    }
                }
            }, 20L) // 1 segundo de delay
        }
    }
}
