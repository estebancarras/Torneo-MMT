package los5fantasticos.memorias

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.UUID

/**
 * Gestor de selecciones de regiones para administradores.
 * Proporciona un sistema de "varita" para seleccionar áreas cúbicas.
 * 
 * Patrón Singleton para acceso global y gestión centralizada.
 */
object SelectionManager {
    
    /**
     * ItemStack de la varita de selección.
     * Los administradores la usan para marcar corners de parcelas.
     */
    val selectionWand: ItemStack by lazy {
        ItemStack(Material.BLAZE_ROD).apply {
            itemMeta = itemMeta?.apply {
                displayName(
                    Component.text("Varita de Selección", NamedTextColor.GOLD, TextDecoration.BOLD)
                )
                lore(
                    listOf(
                        Component.text("Clic Izquierdo: Posición 1", NamedTextColor.YELLOW),
                        Component.text("Clic Derecho: Posición 2", NamedTextColor.YELLOW),
                        Component.empty(),
                        Component.text("Usa para seleccionar regiones", NamedTextColor.GRAY)
                    )
                )
            }
        }
    }
    
    // Almacena las selecciones de cada jugador: UUID -> (Pos1, Pos2)
    private val selections = mutableMapOf<UUID, Pair<Location?, Location?>>()
    
    // Jugadores actualmente en modo selección
    private val inSelectionMode = mutableSetOf<UUID>()
    
    /**
     * Activa o desactiva el modo de selección para un jugador.
     * 
     * @param player El jugador a togglear
     * @return true si el modo fue activado, false si fue desactivado
     */
    fun toggleSelectionMode(player: Player): Boolean {
        val uuid = player.uniqueId
        
        return if (inSelectionMode.contains(uuid)) {
            // Desactivar modo
            inSelectionMode.remove(uuid)
            selections.remove(uuid)
            
            // Remover varita del inventario
            player.inventory.contents.forEachIndexed { index, item ->
                if (item != null && isSelectionWand(item)) {
                    player.inventory.setItem(index, null)
                }
            }
            
            player.sendMessage(
                Component.text("✓ Modo de selección desactivado", NamedTextColor.YELLOW)
            )
            false
        } else {
            // Activar modo
            inSelectionMode.add(uuid)
            selections[uuid] = Pair(null, null)
            
            // Dar varita al jugador
            player.inventory.addItem(selectionWand)
            
            player.sendMessage(
                Component.text("✓ Modo de selección activado", NamedTextColor.GREEN, TextDecoration.BOLD)
            )
            player.sendMessage(
                Component.text("Clic Izquierdo: Posición 1 | Clic Derecho: Posición 2", NamedTextColor.YELLOW)
            )
            true
        }
    }
    
    /**
     * Verifica si un jugador está en modo de selección.
     */
    fun isInSelectionMode(player: Player): Boolean {
        return inSelectionMode.contains(player.uniqueId)
    }
    
    /**
     * Establece la primera posición de selección.
     */
    fun setPos1(player: Player, location: Location) {
        val uuid = player.uniqueId
        val current = selections[uuid] ?: Pair(null, null)
        selections[uuid] = Pair(location, current.second)
        
        player.sendMessage(
            Component.text("✓ Posición 1 establecida: ", NamedTextColor.GREEN)
                .append(Component.text(formatLocation(location), NamedTextColor.WHITE))
        )
        
        // Mostrar progreso
        if (current.second != null) {
            player.sendMessage(
                Component.text("¡Selección completa! Usa /memorias parcela add <arena>", NamedTextColor.GOLD, TextDecoration.BOLD)
            )
        }
    }
    
    /**
     * Establece la segunda posición de selección.
     */
    fun setPos2(player: Player, location: Location) {
        val uuid = player.uniqueId
        val current = selections[uuid] ?: Pair(null, null)
        selections[uuid] = Pair(current.first, location)
        
        player.sendMessage(
            Component.text("✓ Posición 2 establecida: ", NamedTextColor.GREEN)
                .append(Component.text(formatLocation(location), NamedTextColor.WHITE))
        )
        
        // Mostrar progreso
        if (current.first != null) {
            player.sendMessage(
                Component.text("¡Selección completa! Usa /memorias parcela add <arena>", NamedTextColor.GOLD, TextDecoration.BOLD)
            )
        }
    }
    
    /**
     * Obtiene la selección actual de un jugador como Cuboid.
     * 
     * @return Cuboid si ambas posiciones están establecidas, null en caso contrario
     */
    fun getSelection(player: Player): Cuboid? {
        val selection = selections[player.uniqueId] ?: return null
        val (pos1, pos2) = selection
        
        if (pos1 == null || pos2 == null) {
            return null
        }
        
        return Cuboid.fromLocations(pos1, pos2)
    }
    
    /**
     * Limpia la selección de un jugador.
     */
    fun clearSelection(player: Player) {
        selections[player.uniqueId] = Pair(null, null)
    }
    
    /**
     * Verifica si un ItemStack es la varita de selección.
     */
    fun isSelectionWand(item: ItemStack): Boolean {
        if (item.type != Material.BLAZE_ROD) return false
        
        val meta = item.itemMeta ?: return false
        val displayName = meta.displayName() ?: return false
        
        // Comparar el nombre de la varita
        return displayName == selectionWand.itemMeta?.displayName()
    }
    
    /**
     * Formatea una location para mostrarla al jugador.
     */
    private fun formatLocation(loc: Location): String {
        return "(${loc.blockX}, ${loc.blockY}, ${loc.blockZ})"
    }
    
    /**
     * Limpia todos los datos al desactivar el plugin.
     */
    fun cleanup() {
        selections.clear()
        inSelectionMode.clear()
    }
}
