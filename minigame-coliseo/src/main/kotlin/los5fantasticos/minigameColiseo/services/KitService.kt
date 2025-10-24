package los5fantasticos.minigameColiseo.services

import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * Servicio de kits del Coliseo.
 * 
 * Responsabilidades:
 * - Cargar configuración de kits
 * - Crear y aplicar kits a jugadores
 * - Parsear encantamientos y efectos
 */
class KitService(private val config: FileConfiguration) {
    
    /**
     * Aplica el kit de Élite a un jugador.
     */
    fun applyEliteKit(player: Player) {
        player.inventory.clear()
        
        // Armadura
        player.inventory.helmet = parseArmor("kits.elite.armor.helmet")
        player.inventory.chestplate = parseArmor("kits.elite.armor.chestplate")
        player.inventory.leggings = parseArmor("kits.elite.armor.leggings")
        player.inventory.boots = parseArmor("kits.elite.armor.boots")
        
        // Armas
        val sword = parseWeapon("kits.elite.weapons.sword")
        if (sword != null) player.inventory.addItem(sword)
        
        if (config.getBoolean("kits.elite.weapons.shield", false)) {
            player.inventory.setItemInOffHand(ItemStack(Material.SHIELD))
        }
        
        // Items
        val items = config.getStringList("kits.elite.items")
        items.forEach { itemStr ->
            parseItem(itemStr)?.let { player.inventory.addItem(it) }
        }
        
        // Efectos
        val effects = config.getStringList("kits.elite.effects")
        effects.forEach { effectStr ->
            parseEffect(effectStr)?.let { player.addPotionEffect(it) }
        }
    }
    
    /**
     * Aplica el kit de Horda a un jugador.
     */
    fun applyHordeKit(player: Player) {
        player.inventory.clear()
        
        // Armadura
        player.inventory.helmet = parseArmor("kits.horde.armor.helmet")
        player.inventory.chestplate = parseArmor("kits.horde.armor.chestplate")
        player.inventory.leggings = parseArmor("kits.horde.armor.leggings")
        player.inventory.boots = parseArmor("kits.horde.armor.boots")
        
        // Armas
        val sword = parseWeapon("kits.horde.weapons.sword")
        if (sword != null) player.inventory.addItem(sword)
        
        val crossbow = parseWeapon("kits.horde.weapons.crossbow")
        if (crossbow != null) player.inventory.addItem(crossbow)
        
        // Items
        val items = config.getStringList("kits.horde.items")
        items.forEach { itemStr ->
            parseItem(itemStr)?.let { player.inventory.addItem(it) }
        }
    }
    
    /**
     * Parsea una pieza de armadura desde la configuración.
     * Formato: "MATERIAL:ENCANTAMIENTO:NIVEL" o "MATERIAL"
     */
    private fun parseArmor(path: String): ItemStack? {
        val armorStr = config.getString(path) ?: return null
        val parts = armorStr.split(":")
        
        val material = Material.getMaterial(parts[0]) ?: return null
        val item = ItemStack(material)
        
        // Aplicar encantamiento si existe
        if (parts.size >= 3) {
            val enchantName = parts[1]
            val level = parts[2].toIntOrNull() ?: 1
            val enchant = Enchantment.getByName(enchantName)
            if (enchant != null) {
                item.addUnsafeEnchantment(enchant, level)
            }
        }
        
        return item
    }
    
    /**
     * Parsea un arma desde la configuración.
     */
    private fun parseWeapon(path: String): ItemStack? {
        val weaponStr = config.getString(path) ?: return null
        val parts = weaponStr.split(":")
        
        val material = Material.getMaterial(parts[0]) ?: return null
        val item = ItemStack(material)
        
        // Aplicar encantamiento si existe
        if (parts.size >= 3) {
            val enchantName = parts[1]
            val level = parts[2].toIntOrNull() ?: 1
            val enchant = Enchantment.getByName(enchantName)
            if (enchant != null) {
                item.addUnsafeEnchantment(enchant, level)
            }
        }
        
        return item
    }
    
    /**
     * Parsea un item desde la configuración.
     * Formato: "MATERIAL:CANTIDAD" o "MATERIAL,EFECTO:CANTIDAD"
     */
    private fun parseItem(itemStr: String): ItemStack? {
        val parts = itemStr.split(":")
        val materialPart = parts[0]
        val amount = parts.getOrNull(1)?.toIntOrNull() ?: 1
        
        val material = Material.getMaterial(materialPart) ?: return null
        return ItemStack(material, amount)
    }
    
    /**
     * Parsea un efecto de poción desde la configuración.
     * Formato: "EFECTO:NIVEL"
     */
    private fun parseEffect(effectStr: String): PotionEffect? {
        val parts = effectStr.split(":")
        val effectName = parts[0]
        val level = parts.getOrNull(1)?.toIntOrNull() ?: 0
        
        val effectType = PotionEffectType.getByName(effectName) ?: return null
        return PotionEffect(effectType, Int.MAX_VALUE, level, false, false)
    }
}
