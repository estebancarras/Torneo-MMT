package yo.spray.robarCabeza.services

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

/**
 * Representa un ítem del kit con sus propiedades.
 */
data class KitItem(
    val type: PotionEffectType,
    val level: Int,
    val slot: Int,
    val cooldown: Int
)

/**
 * Servicio que gestiona el kit de pociones con sistema de cooldowns.
 * 
 * Responsabilidades:
 * - Cargar configuración del kit desde robarcabeza.yml
 * - Entregar el kit completo a los jugadores
 * - Gestionar cooldowns de pociones usadas
 * - Reaparición automática de pociones después del cooldown
 * - Limpiar cooldowns al finalizar partida
 */
class ItemKitService(private val plugin: Plugin) {
    
    /**
     * Lista de ítems que componen el kit.
     */
    private val kitItems = mutableListOf<KitItem>()
    
    /**
     * Mapa para rastrear los cooldowns activos por jugador y por slot.
     * UUID del jugador -> (Slot -> Tarea de cooldown)
     */
    private val playerCooldowns = mutableMapOf<UUID, MutableMap<Int, BukkitTask>>()
    
    /**
     * Carga la configuración del kit desde el archivo de configuración.
     */
    fun loadKitFromConfig(config: FileConfiguration) {
        kitItems.clear()
        
        val items = config.getStringList("potion-kit.items")
        
        if (items.isEmpty()) {
            plugin.logger.warning("[ItemKitService] No se encontraron ítems en potion-kit.items")
            return
        }
        
        items.forEach { itemString ->
            try {
                val parts = itemString.split(":")
                if (parts.size != 4) {
                    plugin.logger.warning("[ItemKitService] Formato inválido: $itemString")
                    return@forEach
                }
                
                val potionType = PotionEffectType.getByName(parts[0])
                if (potionType == null) {
                    plugin.logger.warning("[ItemKitService] Tipo de poción inválido: ${parts[0]}")
                    return@forEach
                }
                
                val level = parts[1].toIntOrNull() ?: 0
                val slot = parts[2].toIntOrNull() ?: 0
                val cooldown = parts[3].toIntOrNull() ?: 15
                
                val kitItem = KitItem(potionType, level, slot, cooldown)
                kitItems.add(kitItem)
                
                plugin.logger.info("[ItemKitService] Kit item cargado: ${potionType.name} nivel $level en slot $slot con cooldown de ${cooldown}s")
            } catch (e: Exception) {
                plugin.logger.severe("[ItemKitService] Error parseando ítem: $itemString - ${e.message}")
            }
        }
        
        plugin.logger.info("[ItemKitService] Kit cargado con ${kitItems.size} ítems")
    }
    
    /**
     * Entrega el kit completo a un jugador.
     */
    fun giveFullKit(player: Player) {
        // Limpiar inventario
        player.inventory.clear()
        
        // Dar cada ítem del kit
        kitItems.forEach { item ->
            val potion = createPotionItem(item)
            player.inventory.setItem(item.slot, potion)
        }
        
        player.sendMessage("${ChatColor.GREEN}¡Has recibido tu kit de habilidades!")
        plugin.logger.info("[ItemKitService] Kit entregado a ${player.name}")
    }
    
    /**
     * Crea una poción arrojadiza basada en un KitItem.
     */
    private fun createPotionItem(item: KitItem): ItemStack {
        val potion = ItemStack(Material.SPLASH_POTION, 1)
        val meta = potion.itemMeta as PotionMeta
        
        // Añadir el efecto personalizado
        meta.addCustomEffect(PotionEffect(item.type, 20 * 10, item.level), true)
        
        // Nombre personalizado
        val effectName = item.type.name.lowercase().replace("_", " ").capitalize()
        meta.setDisplayName("${ChatColor.AQUA}$effectName ${getRomanNumeral(item.level + 1)}")
        
        // Lore con información del cooldown
        meta.lore = listOf(
            "${ChatColor.GRAY}Cooldown: ${ChatColor.WHITE}${item.cooldown}s",
            "${ChatColor.DARK_GRAY}Poción arrojadiza"
        )
        
        potion.itemMeta = meta
        return potion
    }
    
    /**
     * Convierte un número a numeral romano.
     */
    private fun getRomanNumeral(num: Int): String {
        return when (num) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            5 -> "V"
            else -> num.toString()
        }
    }
    
    /**
     * Maneja el uso de una poción del kit.
     */
    fun handlePotionUse(player: Player, slot: Int) {
        // Buscar el KitItem correspondiente al slot
        val kitItem = kitItems.firstOrNull { it.slot == slot }
        
        if (kitItem == null) {
            plugin.logger.warning("[ItemKitService] No se encontró KitItem para slot $slot")
            return
        }
        
        plugin.logger.info("[ItemKitService] ${player.name} usó poción en slot $slot (${kitItem.type.name})")
        
        // Iniciar cooldown
        startCooldown(player, kitItem)
        
        // Mensaje al jugador
        player.sendMessage("${ChatColor.YELLOW}Habilidad en cooldown: ${ChatColor.WHITE}${kitItem.cooldown}s")
    }
    
    /**
     * Inicia el cooldown para una poción específica.
     */
    private fun startCooldown(player: Player, item: KitItem) {
        // Cancelar cooldown anterior si existe
        playerCooldowns[player.uniqueId]?.get(item.slot)?.cancel()
        
        // Crear nueva tarea de cooldown
        val task = object : BukkitRunnable() {
            override fun run() {
                // Verificar que el jugador siga online
                if (!player.isOnline) {
                    cancel()
                    return
                }
                
                // Reaparece la poción en el slot
                val potion = createPotionItem(item)
                player.inventory.setItem(item.slot, potion)
                
                // Mensaje y sonido
                player.sendMessage("${ChatColor.GREEN}✓ Habilidad recargada!")
                player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f)
                
                // Remover del mapa de cooldowns
                playerCooldowns[player.uniqueId]?.remove(item.slot)
                
                plugin.logger.info("[ItemKitService] Poción recargada para ${player.name} en slot ${item.slot}")
            }
        }
        
        // Programar la tarea (cooldown en ticks: segundos * 20)
        val scheduledTask = task.runTaskLater(plugin, (item.cooldown * 20).toLong())
        
        // Guardar en el mapa
        playerCooldowns.computeIfAbsent(player.uniqueId) { mutableMapOf() }[item.slot] = scheduledTask
    }
    
    /**
     * Limpia todos los cooldowns de un jugador.
     */
    fun clearCooldowns(player: Player) {
        val cooldowns = playerCooldowns.remove(player.uniqueId)
        
        if (cooldowns != null) {
            cooldowns.values.forEach { it.cancel() }
            plugin.logger.info("[ItemKitService] Cooldowns limpiados para ${player.name}")
        }
    }
    
    /**
     * Verifica si un ítem es parte del kit.
     */
    fun isKitItem(slot: Int): Boolean {
        return kitItems.any { it.slot == slot }
    }
    
    /**
     * Limpia todos los cooldowns activos (al deshabilitar el plugin).
     */
    fun clearAll() {
        playerCooldowns.values.forEach { cooldownMap ->
            cooldownMap.values.forEach { it.cancel() }
        }
        playerCooldowns.clear()
        plugin.logger.info("[ItemKitService] Todos los cooldowns limpiados")
    }
}
