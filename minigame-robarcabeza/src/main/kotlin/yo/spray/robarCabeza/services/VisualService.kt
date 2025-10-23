package yo.spray.robarCabeza.services

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

/**
 * Servicio de visualización simplificado para el casco trofeo.
 * 
 * Responsabilidades:
 * - Equipar cabezas de jugador como cascos
 * - Remover cascos de jugadores
 * - Gestionar las skins de los creadores
 */
class VisualService {
    
    /**
     * Lista de nombres de jugadores cuyas skins se usarán para las cabezas.
     */
    private var creatorHeads: List<String> = listOf("Notch")
    
    /**
     * Configura la lista de creadores para las skins.
     * 
     * @param heads Lista de nombres de jugadores para las skins
     */
    fun configure(heads: List<String>) {
        this.creatorHeads = heads.ifEmpty { listOf("Notch") }
    }
    
    /**
     * Equipa una cabeza de jugador en el slot del casco.
     * 
     * @param player Jugador que recibirá la cabeza
     * @param creatorName Nombre del jugador cuya skin se usará (opcional, aleatorio si no se especifica)
     */
    fun equipHead(player: Player, creatorName: String? = null) {
        // Seleccionar un creador aleatorio si no se especifica
        val selectedCreator = creatorName ?: creatorHeads.random()
        
        // Crear el ItemStack de la cabeza con la skin del creador
        val headItem = ItemStack(Material.PLAYER_HEAD)
        val meta = headItem.itemMeta as SkullMeta
        meta.owningPlayer = Bukkit.getOfflinePlayer(selectedCreator)
        meta.setDisplayName("§6§lCabeza del Creador")
        headItem.itemMeta = meta
        
        // Equipar en el slot del casco
        player.inventory.helmet = headItem
    }
    
    /**
     * Remueve la cabeza del slot del casco de un jugador.
     * 
     * @param player Jugador al que se le removerá la cabeza
     */
    fun removeHead(player: Player) {
        player.inventory.helmet = null
    }
    
    /**
     * Obtiene la lista de creadores configurados.
     */
    fun getCreatorHeads(): List<String> = creatorHeads
}
