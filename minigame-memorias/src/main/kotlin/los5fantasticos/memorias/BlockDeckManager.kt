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
                
                // VALIDACIÓN CRÍTICA: Solo bloques sólidos
                if (!material.isSolid) {
                    plugin.logger.warning("⚠ Material descartado (no sólido): $materialName")
                    invalidCount++
                    continue
                }
                
                deck.add(material)
                validCount++
                
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("⚠ Material inválido descartado: $materialName")
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
