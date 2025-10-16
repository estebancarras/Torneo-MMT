package los5fantasticos.memorias

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * Singleton que gestiona el mazo curado de bloques para el minijuego Memorias.
 * 
 * RESPONSABILIDAD:
 * - Cargar y validar la lista de bloques desde memorias.yml
 * - Proporcionar bloques aleatorios para cada duelo
 * 
 * ARQUITECTURA:
 * Este manager se inicializa una sola vez en el onEnable del plugin
 * y persiste durante toda la vida del servidor.
 */
object BlockDeckManager {
    
    private val deck = mutableListOf<Material>()
    
    /**
     * Lista negra de bloques problemáticos que no deben usarse en el juego.
     * Incluye bloques visualmente ambiguos, no sólidos o con interacciones especiales.
     */
    private val BLACKLIST = setOf(
        // Bloques visualmente ambiguos
        "PUMPKIN", "CARVED_PUMPKIN",
        // No son bloques completos/sólidos
        "REDSTONE_WIRE", "TRIPWIRE", "GLASS_PANE",
        // Tienen inventarios o interacciones especiales
        "CHEST", "TRAPPED_CHEST", "ENDER_CHEST",
        "FURNACE", "BLAST_FURNACE", "SMOKER",
        "BREWING_STAND", "ENCHANTING_TABLE",
        // Escaleras y vallas (no son cubos completos)
        "ACACIA_STAIRS", "BIRCH_STAIRS", "DARK_OAK_STAIRS",
        "JUNGLE_STAIRS", "OAK_STAIRS", "SPRUCE_STAIRS",
        "OAK_FENCE", "SPRUCE_FENCE", "BIRCH_FENCE",
        "JUNGLE_FENCE", "ACACIA_FENCE", "DARK_OAK_FENCE",
        // Losas (no son cubos completos)
        "STONE_SLAB", "OAK_SLAB", "SPRUCE_SLAB"
    )
    
    /**
     * Carga el mazo de bloques desde la configuración.
     * 
     * VALIDACIONES:
     * - Verifica que cada string sea un Material válido
     * - Verifica que cada material sea sólido (isSolid)
     * - Registra en consola cualquier material inválido descartado
     * 
     * @param plugin Instancia del plugin para acceder a la configuración
     */
    fun loadDeck(plugin: org.bukkit.plugin.Plugin) {
        deck.clear()
        
        val configFile = File(plugin.dataFolder, "memorias.yml")
        if (!configFile.exists()) {
            plugin.logger.severe("╔════════════════════════════════════════╗")
            plugin.logger.severe("║ ERROR: memorias.yml no encontrado      ║")
            plugin.logger.severe("║ Crea el archivo con la config inicial ║")
            plugin.logger.severe("╚════════════════════════════════════════╝")
            return
        }
        
        val config = YamlConfiguration.loadConfiguration(configFile)
        val blockSetList = config.getStringList("block-set")
        
        if (blockSetList.isEmpty()) {
            plugin.logger.severe("╔════════════════════════════════════════╗")
            plugin.logger.severe("║ ERROR: block-set está vacío            ║")
            plugin.logger.severe("║ Configura al menos 20 bloques          ║")
            plugin.logger.severe("╚════════════════════════════════════════╝")
            return
        }
        
        plugin.logger.info("╔════════════════════════════════════════╗")
        plugin.logger.info("║   Cargando Mazo de Bloques Memorias   ║")
        plugin.logger.info("╚════════════════════════════════════════╝")
        
        var validCount = 0
        var invalidCount = 0
        
        for (materialName in blockSetList) {
            try {
                val material = Material.valueOf(materialName.uppercase())
                
                // VALIDACIÓN 1: Material debe ser un bloque
                if (!material.isBlock) {
                    plugin.logger.warning("[Memorias] Descartando material (no es bloque): '$materialName'")
                    invalidCount++
                    continue
                }
                
                // VALIDACIÓN 2: Material debe ser sólido
                if (!material.isSolid) {
                    plugin.logger.warning("[Memorias] Descartando material (no sólido): '$materialName'")
                    invalidCount++
                    continue
                }
                
                // VALIDACIÓN 3: Material NO debe estar en lista negra
                if (BLACKLIST.contains(material.name)) {
                    plugin.logger.warning("[Memorias] Descartando material en lista negra: '$materialName'")
                    invalidCount++
                    continue
                }
                
                deck.add(material)
                validCount++
                
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("[Memorias] Descartando material inválido: '$materialName'")
                invalidCount++
            }
        }
        
        plugin.logger.info("✓ Mazo cargado: $validCount bloques válidos")
        if (invalidCount > 0) {
            plugin.logger.warning("⚠ $invalidCount materiales descartados")
        }
        
        if (deck.size < 20) {
            plugin.logger.warning("╔════════════════════════════════════════╗")
            plugin.logger.warning("║ ADVERTENCIA: Pocos bloques ($validCount)     ║")
            plugin.logger.warning("║ Recomendado: Al menos 20 bloques       ║")
            plugin.logger.warning("╚════════════════════════════════════════╝")
        }
    }
    
    /**
     * Obtiene una lista de materiales únicos y aleatorios del mazo.
     * 
     * @param count Número de materiales únicos requeridos
     * @return Lista de materiales barajada
     * @throws IllegalStateException Si el mazo no tiene suficientes bloques
     */
    fun getShuffledDeck(count: Int): List<Material> {
        require(count > 0) { "El count debe ser positivo" }
        
        if (deck.isEmpty()) {
            throw IllegalStateException("El mazo está vacío. ¿Se llamó a loadDeck()?")
        }
        
        if (count > deck.size) {
            throw IllegalStateException(
                "Se requieren $count bloques únicos pero el mazo solo tiene ${deck.size}. " +
                "Reduce el tamaño del tablero o añade más bloques a memorias.yml"
            )
        }
        
        // Retornar una copia barajada de los primeros 'count' elementos
        return deck.shuffled().take(count)
    }
    
    /**
     * Obtiene el tamaño actual del mazo cargado.
     */
    fun getDeckSize(): Int = deck.size
    
    /**
     * Verifica si el mazo está cargado y listo para usar.
     */
    fun isReady(): Boolean = deck.isNotEmpty()
}
