package los5fantasticos.memorias

import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Listener actualizado para el sistema refactorizado.
 * Delega toda la lógica de juego a DueloMemorias a través del GameManager.
 */
class PlayerListener(private val gameManager: GameManager) : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        gameManager.removePlayer(event.player)
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        val player = event.player
        
        // Verificar si es un cartel de unirse al juego
        if (block.state is Sign) {
            val sign = block.state as Sign

            @Suppress("DEPRECATION")
            val line0 = sign.getLine(0)

            if (line0.equals("[Memorias]", ignoreCase = true)) {
                gameManager.joinPlayer(player)
                event.isCancelled = true
                return
            }
        }
        
        // Verificar si el jugador está en un duelo activo
        val duelo = gameManager.getDueloByPlayer(player) ?: return
        
        // Solo procesar clics derechos en bloques
        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }
        
        // Verificar si el bloque es parte del tablero (lana gris o de colores)
        val blockType = block.type
        if (!isGameBlock(blockType)) {
            return
        }
        
        // Delegar el manejo del clic al duelo
        val success = duelo.handlePlayerClick(player, block.location)
        if (success) {
            event.isCancelled = true
        }
    }
    
    /**
     * Verifica si un material es parte del juego de memorias.
     */
    private fun isGameBlock(material: Material): Boolean {
        return material == Material.GRAY_WOOL ||
               material == Material.RED_WOOL ||
               material == Material.BLUE_WOOL ||
               material == Material.GREEN_WOOL ||
               material == Material.YELLOW_WOOL ||
               material == Material.LIME_WOOL ||
               material == Material.ORANGE_WOOL ||
               material == Material.PINK_WOOL ||
               material == Material.PURPLE_WOOL
    }
}

