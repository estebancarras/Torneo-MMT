package los5fantasticos.torneo.listeners

import los5fantasticos.torneo.util.selection.SelectionManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

/**
 * Listener para manejar las interacciones con la varita de selección.
 * Permite a los administradores seleccionar regiones para el lobby global.
 */
class SelectionListener : Listener {
    
    /**
     * Maneja los clics con la varita de selección.
     * - Clic Izquierdo: Establece Posición 1
     * - Clic Derecho: Establece Posición 2
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return
        
        // Verificar si es la varita de selección
        if (!SelectionManager.isSelectionWand(item)) return
        
        // Verificar permisos
        if (!player.hasPermission("torneo.admin") && !player.isOp) {
            player.sendMessage(Component.text("✗ No tienes permiso para usar la varita", NamedTextColor.RED))
            event.isCancelled = true
            return
        }
        
        val clickedBlock = event.clickedBlock
        if (clickedBlock == null) {
            player.sendMessage(Component.text("✗ Debes hacer clic en un bloque", NamedTextColor.RED))
            event.isCancelled = true
            return
        }
        
        // Cancelar el evento para evitar romper bloques o interacciones
        event.isCancelled = true
        
        when (event.action) {
            Action.LEFT_CLICK_BLOCK -> {
                // Establecer Posición 1
                SelectionManager.setPos1(player, clickedBlock.location)
            }
            Action.RIGHT_CLICK_BLOCK -> {
                // Establecer Posición 2
                SelectionManager.setPos2(player, clickedBlock.location)
            }
            else -> return
        }
    }
}
