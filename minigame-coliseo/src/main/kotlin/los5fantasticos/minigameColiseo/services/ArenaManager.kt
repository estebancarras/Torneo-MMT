package los5fantasticos.minigameColiseo.services

import los5fantasticos.minigameColiseo.game.Arena
import los5fantasticos.torneo.util.selection.Cuboid
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * Gestor de arenas del Coliseo.
 * 
 * Responsabilidades:
 * - Crear y eliminar arenas
 * - Guardar y cargar arenas desde archivo
 * - Gestionar spawns de equipos
 */
class ArenaManager {
    private val arenas = mutableMapOf<String, Arena>()
    private lateinit var configFile: File
    
    /**
     * Inicializa el gestor y carga las arenas.
     */
    fun initialize(dataFolder: File) {
        configFile = File(dataFolder, "coliseo_arenas.yml")
        if (!configFile.exists()) {
            dataFolder.mkdirs()
            configFile.createNewFile()
        }
        loadArenas()
    }
    
    /**
     * Crea una nueva arena.
     */
    fun createArena(name: String): Arena {
        val arena = Arena(name)
        arenas[name] = arena
        return arena
    }
    
    /**
     * Obtiene una arena por nombre.
     */
    fun getArena(name: String): Arena? = arenas[name]
    
    /**
     * Obtiene una arena aleatoria válida.
     */
    fun getRandomArena(): Arena? {
        return arenas.values.filter { it.isValid() }.randomOrNull()
    }
    
    /**
     * Obtiene todas las arenas.
     */
    fun getAllArenas(): List<Arena> = arenas.values.toList()
    
    /**
     * Verifica si existe una arena.
     */
    fun arenaExists(name: String): Boolean = arenas.containsKey(name)
    
    /**
     * Elimina una arena.
     */
    fun deleteArena(name: String) {
        arenas.remove(name)
        saveArenas()
    }
    
    /**
     * Guarda todas las arenas en el archivo.
     */
    fun saveArenas() {
        val config = YamlConfiguration()
        
        arenas.forEach { (name, arena) ->
            // Guardar spawns élite
            arena.eliteSpawns.forEachIndexed { index, spawn ->
                config.set("arenas.$name.elite-spawns.$index.world", spawn.world.name)
                config.set("arenas.$name.elite-spawns.$index.x", spawn.x)
                config.set("arenas.$name.elite-spawns.$index.y", spawn.y)
                config.set("arenas.$name.elite-spawns.$index.z", spawn.z)
                config.set("arenas.$name.elite-spawns.$index.yaw", spawn.yaw)
                config.set("arenas.$name.elite-spawns.$index.pitch", spawn.pitch)
            }
            
            // Guardar spawns horda
            arena.hordeSpawns.forEachIndexed { index, spawn ->
                config.set("arenas.$name.horde-spawns.$index.world", spawn.world.name)
                config.set("arenas.$name.horde-spawns.$index.x", spawn.x)
                config.set("arenas.$name.horde-spawns.$index.y", spawn.y)
                config.set("arenas.$name.horde-spawns.$index.z", spawn.z)
                config.set("arenas.$name.horde-spawns.$index.yaw", spawn.yaw)
                config.set("arenas.$name.horde-spawns.$index.pitch", spawn.pitch)
            }
            
            // Guardar región
            arena.playRegion?.let { region ->
                config.set("arenas.$name.region.world", region.world.name)
                config.set("arenas.$name.region.minX", region.minX)
                config.set("arenas.$name.region.minY", region.minY)
                config.set("arenas.$name.region.minZ", region.minZ)
                config.set("arenas.$name.region.maxX", region.maxX)
                config.set("arenas.$name.region.maxY", region.maxY)
                config.set("arenas.$name.region.maxZ", region.maxZ)
            }
        }
        
        config.save(configFile)
    }
    
    /**
     * Carga las arenas desde el archivo.
     */
    private fun loadArenas() {
        val config = YamlConfiguration.loadConfiguration(configFile)
        val arenasSection = config.getConfigurationSection("arenas") ?: return
        
        for (arenaName in arenasSection.getKeys(false)) {
            val arena = Arena(arenaName)
            
            // Cargar spawns élite
            val eliteSection = config.getConfigurationSection("arenas.$arenaName.elite-spawns")
            eliteSection?.getKeys(false)?.forEach { key ->
                val worldName = config.getString("arenas.$arenaName.elite-spawns.$key.world") ?: return@forEach
                val world = Bukkit.getWorld(worldName) ?: return@forEach
                val x = config.getDouble("arenas.$arenaName.elite-spawns.$key.x")
                val y = config.getDouble("arenas.$arenaName.elite-spawns.$key.y")
                val z = config.getDouble("arenas.$arenaName.elite-spawns.$key.z")
                val yaw = config.getDouble("arenas.$arenaName.elite-spawns.$key.yaw").toFloat()
                val pitch = config.getDouble("arenas.$arenaName.elite-spawns.$key.pitch").toFloat()
                arena.eliteSpawns.add(Location(world, x, y, z, yaw, pitch))
            }
            
            // Cargar spawns horda
            val hordeSection = config.getConfigurationSection("arenas.$arenaName.horde-spawns")
            hordeSection?.getKeys(false)?.forEach { key ->
                val worldName = config.getString("arenas.$arenaName.horde-spawns.$key.world") ?: return@forEach
                val world = Bukkit.getWorld(worldName) ?: return@forEach
                val x = config.getDouble("arenas.$arenaName.horde-spawns.$key.x")
                val y = config.getDouble("arenas.$arenaName.horde-spawns.$key.y")
                val z = config.getDouble("arenas.$arenaName.horde-spawns.$key.z")
                val yaw = config.getDouble("arenas.$arenaName.horde-spawns.$key.yaw").toFloat()
                val pitch = config.getDouble("arenas.$arenaName.horde-spawns.$key.pitch").toFloat()
                arena.hordeSpawns.add(Location(world, x, y, z, yaw, pitch))
            }
            
            // Cargar región
            val regionSection = config.getConfigurationSection("arenas.$arenaName.region")
            if (regionSection != null) {
                val worldName = config.getString("arenas.$arenaName.region.world") ?: continue
                val world = Bukkit.getWorld(worldName) ?: continue
                val minX = config.getInt("arenas.$arenaName.region.minX")
                val minY = config.getInt("arenas.$arenaName.region.minY")
                val minZ = config.getInt("arenas.$arenaName.region.minZ")
                val maxX = config.getInt("arenas.$arenaName.region.maxX")
                val maxY = config.getInt("arenas.$arenaName.region.maxY")
                val maxZ = config.getInt("arenas.$arenaName.region.maxZ")
                arena.playRegion = Cuboid(world, minX, minY, minZ, maxX, maxY, maxZ)
            }
            
            arenas[arenaName] = arena
        }
    }
}
