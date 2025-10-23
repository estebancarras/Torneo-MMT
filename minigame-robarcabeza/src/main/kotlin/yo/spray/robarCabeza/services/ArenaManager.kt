package yo.spray.robarCabeza.services

import los5fantasticos.torneo.util.selection.Cuboid
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import yo.spray.robarCabeza.game.Arena
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestiona las arenas del minijuego RobarCabeza.
 * 
 * Responsabilidades:
 * - Crear y eliminar arenas
 * - Almacenar configuración de arenas
 * - Proporcionar arenas disponibles para partidas
 * - Persistencia de arenas en disco
 */
class ArenaManager {
    
    /**
     * Archivo de configuración donde se guardan las arenas.
     */
    private var configFile: File? = null
    
    /**
     * Mapa de arenas por nombre.
     */
    private val arenas = ConcurrentHashMap<String, Arena>()
    
    /**
     * Crea una nueva arena.
     * 
     * @param name Nombre de la arena
     * @return Arena creada
     */
    fun createArena(name: String): Arena {
        val arena = Arena(name = name)
        arenas[name] = arena
        return arena
    }
    
    /**
     * Elimina una arena.
     * 
     * @param name Nombre de la arena
     * @return true si se eliminó, false si no existía
     */
    fun deleteArena(name: String): Boolean {
        return arenas.remove(name) != null
    }
    
    /**
     * Obtiene una arena por nombre.
     * 
     * @param name Nombre de la arena
     * @return Arena o null si no existe
     */
    fun getArena(name: String): Arena? {
        return arenas[name]
    }
    
    /**
     * Obtiene todas las arenas.
     */
    fun getAllArenas(): List<Arena> {
        return arenas.values.toList()
    }
    
    /**
     * Obtiene una arena aleatoria disponible.
     */
    fun getRandomArena(): Arena? {
        return arenas.values.randomOrNull()
    }
    
    /**
     * Verifica si existe una arena.
     */
    fun arenaExists(name: String): Boolean {
        return arenas.containsKey(name)
    }
    
    /**
     * Obtiene el número de arenas configuradas.
     */
    fun getArenaCount(): Int {
        return arenas.size
    }
    
    /**
     * Limpia todas las arenas (para reinicio).
     */
    fun clearAll() {
        arenas.clear()
    }
    
    // ===== Persistencia =====
    
    /**
     * Inicializa el sistema de persistencia.
     */
    fun initialize(dataFolder: File) {
        configFile = File(dataFolder, "robarcabeza_arenas.yml")
        loadArenas()
    }
    
    /**
     * Guarda todas las arenas en el archivo de configuración.
     */
    fun saveArenas() {
        val file = configFile ?: return
        val config = YamlConfiguration()
        
        // Guardar cada arena
        arenas.forEach { (name, arena) ->
            val path = "arenas.$name"
            
            // Guardar spawns
            arena.spawns.forEachIndexed { index, spawn ->
                config.set("$path.spawns.$index.world", spawn.world?.name)
                config.set("$path.spawns.$index.x", spawn.x)
                config.set("$path.spawns.$index.y", spawn.y)
                config.set("$path.spawns.$index.z", spawn.z)
                config.set("$path.spawns.$index.yaw", spawn.yaw)
                config.set("$path.spawns.$index.pitch", spawn.pitch)
            }
            
            // Guardar región de juego
            arena.playRegion?.let { region ->
                config.set("$path.region.world", region.world.name)
                config.set("$path.region.x1", region.minX)
                config.set("$path.region.y1", region.minY)
                config.set("$path.region.z1", region.minZ)
                config.set("$path.region.x2", region.maxX)
                config.set("$path.region.y2", region.maxY)
                config.set("$path.region.z2", region.maxZ)
            }
        }
        
        try {
            config.save(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Carga todas las arenas desde el archivo de configuración.
     */
    private fun loadArenas() {
        val file = configFile ?: return
        
        if (!file.exists()) {
            return
        }
        
        val config = YamlConfiguration.loadConfiguration(file)
        
        // Cargar arenas
        val arenasSection = config.getConfigurationSection("arenas") ?: return
        
        for (arenaName in arenasSection.getKeys(false)) {
            val path = "arenas.$arenaName"
            
            // Crear arena
            val arena = Arena(name = arenaName)
            
            // Cargar spawns
            val spawnsSection = config.getConfigurationSection("$path.spawns")
            if (spawnsSection != null) {
                val spawnIndices = spawnsSection.getKeys(false).mapNotNull { it.toIntOrNull() }.sorted()
                
                for (index in spawnIndices) {
                    val worldName = config.getString("$path.spawns.$index.world") ?: continue
                    val world = Bukkit.getWorld(worldName) ?: continue
                    
                    val spawn = Location(
                        world,
                        config.getDouble("$path.spawns.$index.x"),
                        config.getDouble("$path.spawns.$index.y"),
                        config.getDouble("$path.spawns.$index.z"),
                        config.getDouble("$path.spawns.$index.yaw").toFloat(),
                        config.getDouble("$path.spawns.$index.pitch").toFloat()
                    )
                    
                    arena.spawns.add(spawn)
                }
            }
            
            // Cargar región de juego
            if (config.contains("$path.region")) {
                val worldName = config.getString("$path.region.world") ?: continue
                val world = Bukkit.getWorld(worldName) ?: continue
                
                val x1 = config.getInt("$path.region.x1")
                val y1 = config.getInt("$path.region.y1")
                val z1 = config.getInt("$path.region.z1")
                val x2 = config.getInt("$path.region.x2")
                val y2 = config.getInt("$path.region.y2")
                val z2 = config.getInt("$path.region.z2")
                
                arena.playRegion = Cuboid(
                    world,
                    x1, y1, z1,
                    x2, y2, z2
                )
            }
            
            arenas[arenaName] = arena
        }
    }
}
