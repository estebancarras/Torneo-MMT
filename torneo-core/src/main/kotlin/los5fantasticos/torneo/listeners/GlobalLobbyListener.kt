package los5fantasticos.torneo.listeners

import los5fantasticos.torneo.services.TournamentFlowManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent

/**
 * Listener para proteger el lobby global del torneo.
 * 
 * LÓGICA DE PROTECCIÓN:
 * - Los jugadores que NO están en un minijuego activo no pueden modificar bloques en el lobby
 * - Los administradores con permiso torneo.admin.build pueden ignorar esta restricción
 * - Solo protege la región definida en TournamentFlowManager.globalLobbyRegion
 */
class GlobalLobbyListener : Listener {
    
    /**
     * Protege el lobby contra destrucción de bloques.
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        
        // EXCEPCIÓN: Administradores con permiso especial
        if (player.hasPermission("torneo.admin.build") || player.isOp) {
            return
        }
        
        // Verificar si el jugador está en un minijuego activo
        val activeMinigame = TournamentFlowManager.activeMinigame
        val isInActiveGame = activeMinigame?.getActivePlayers()?.contains(player) ?: false
        
        // Si el jugador NO está en un minijuego, está en el lobby
        if (!isInActiveGame) {
            // Verificar si el bloque está en la región del lobby
            val lobbyRegion = TournamentFlowManager.getLobbyRegion()
            
            if (lobbyRegion != null && lobbyRegion.contains(block.location)) {
                // Bloque dentro del lobby - CANCELAR
                event.isCancelled = true
                player.sendActionBar(
                    Component.text("✗ No puedes romper bloques en el lobby", NamedTextColor.RED)
                )
            }
        }
    }
    
    /**
     * Protege el lobby contra colocación de bloques.
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val block = event.block
        
        // EXCEPCIÓN: Administradores con permiso especial
        if (player.hasPermission("torneo.admin.build") || player.isOp) {
            return
        }
        
        // Verificar si el jugador está en un minijuego activo
        val activeMinigame = TournamentFlowManager.activeMinigame
        val isInActiveGame = activeMinigame?.getActivePlayers()?.contains(player) ?: false
        
        // Si el jugador NO está en un minijuego, está en el lobby
        if (!isInActiveGame) {
            // Verificar si el bloque está en la región del lobby
            val lobbyRegion = TournamentFlowManager.getLobbyRegion()
            
            if (lobbyRegion != null && lobbyRegion.contains(block.location)) {
                // Bloque dentro del lobby - CANCELAR
                event.isCancelled = true
                player.sendActionBar(
                    Component.text("✗ No puedes colocar bloques en el lobby", NamedTextColor.RED)
                )
            }
        }
    }
}
