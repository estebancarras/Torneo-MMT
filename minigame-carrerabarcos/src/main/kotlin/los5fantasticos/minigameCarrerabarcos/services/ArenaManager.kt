package los5fantasticos.minigameCarrerabarcos.services

import los5fantasticos.minigameCarrerabarcos.game.ArenaCarrera
import los5fantasticos.torneo.util.selection.Cuboid
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File

/**
 * Gestor de arenas de carreras.
 * 
 * RESPONSABILIDADES:
 * - Cargar y guardar circuitos desde/hacia carrerabarcos_arenas.yml
 * - Gestionar el registro de arenas en memoria
 * - Proporcionar acceso a las arenas configuradas
 * 
 * PATRÓN DE DISEÑO:
 * Este servicio sigue el patrón Repository, separando la lógica de persistencia
 * de la lógica de juego.
 */
class ArenaManager(private val plugin: Plugin) {
    
    // Mapa de arenas cargadas: nombre -> ArenaCarrera
    private val arenas = mutableMapOf<String, ArenaCarrera>()
    
    // Archivo de configuración
    private lateinit var arenasFile: File
    private lateinit var arenasConfig: YamlConfiguration
    
    /**
     * Inicializa el gestor de arenas.
     * Crea el archivo de configuración si no existe y carga las arenas.
     */
    fun initialize() {
        // Crear carpeta de datos si no existe
        plugin.dataFolder.mkdirs()
        
        // Inicializar archivo de arenas
        arenasFile = File(plugin.dataFolder, "carrerabarcos_arenas.yml")
        
        if (!arenasFile.exists()) {
            arenasFile.createNewFile()
            plugin.logger.info("[Carrera de Barcos] Archivo carrerabarcos_arenas.yml creado")
        }
        
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile)
        plugin.logger.info("[Carrera de Barcos] Archivo de arenas cargado desde: ${arenasFile.absolutePath}")
        
        // NO cargar arenas automáticamente en onEnable para evitar bloqueos
        // Solo cargar cuando sea necesario
        plugin.logger.info("[Carrera de Barcos] Arenas serán cargadas bajo demanda")
    }
    
    /**
     * Carga todas las arenas desde el archivo de configuración de manera segura.
     */
    fun loadArenas() {
        arenas.clear()
        
        val arenasSection = arenasConfig.getConfigurationSection("arenas") ?: run {
            plugin.logger.info("[Carrera de Barcos] No hay arenas guardadas")
            return
        }
        
        for (arenaName in arenasSection.getKeys(false)) {
            try {
                val arenaSection = arenasSection.getConfigurationSection(arenaName) ?: continue
                
                val arena = ArenaCarrera(nombre = arenaName)
                
                // Cargar lobby con validación segura
                if (arenaSection.contains("lobby")) {
                    arena.lobby = deserializeLocationSafe(arenaSection.getConfigurationSection("lobby")!!)
                }
                
                // Cargar spawns con validación segura
                val spawnsSection = arenaSection.getConfigurationSection("spawns")
                if (spawnsSection != null) {
                    for (key in spawnsSection.getKeys(false)) {
                        val spawnSection = spawnsSection.getConfigurationSection(key) ?: continue
                        val spawnLocation = deserializeLocationSafe(spawnSection)
                        if (spawnLocation != null) {
                            arena.spawns.add(spawnLocation)
                        } else {
                            plugin.logger.warning("[Carrera de Barcos] Spawn inválido ignorado en arena '$arenaName'")
                        }
                    }
                }
                
                // Cargar checkpoints con validación segura
                val checkpointsSection = arenaSection.getConfigurationSection("checkpoints")
                if (checkpointsSection != null) {
                    for (key in checkpointsSection.getKeys(false)) {
                        val checkpointSection = checkpointsSection.getConfigurationSection(key) ?: continue
                        try {
                            val checkpoint = deserializeCuboidSafe(checkpointSection)
                            if (checkpoint != null) {
                                arena.checkpoints.add(checkpoint)
                            } else {
                                plugin.logger.warning("[Carrera de Barcos] Checkpoint inválido ignorado en arena '$arenaName'")
                            }
                        } catch (e: Exception) {
                            plugin.logger.warning("[Carrera de Barcos] Error al cargar checkpoint en arena '$arenaName': ${e.message}")
                        }
                    }
                }
                
                // Cargar meta con validación segura
                if (arenaSection.contains("meta")) {
                    arena.meta = deserializeCuboidSafe(arenaSection.getConfigurationSection("meta")!!)
                }
                
                // Cargar región de protección con validación segura
                if (arenaSection.contains("protection")) {
                    arena.protectionRegion = deserializeCuboidSafe(arenaSection.getConfigurationSection("protection")!!)
                }
                
                arenas[arenaName] = arena
                plugin.logger.info("[Carrera de Barcos] Arena cargada: $arenaName (${arena.spawns.size} spawns, ${arena.checkpoints.size} checkpoints)")
                
            } catch (e: Exception) {
                plugin.logger.severe("[Carrera de Barcos] Error al cargar arena '$arenaName': ${e.message}")
                e.printStackTrace()
            }
        }
        
        plugin.logger.info("[Carrera de Barcos] ${arenas.size} arenas cargadas")
    }
    
    /**
     * Guarda todas las arenas en el archivo de configuración.
     */
    fun saveArenas() {
        try {
            // Limpiar configuración existente
            arenasConfig.set("arenas", null)
            
            // Guardar cada arena
            for ((nombre, arena) in arenas) {
                val path = "arenas.$nombre"
                
                // Guardar lobby
                arena.lobby?.let { lobby ->
                    serializeLocation(lobby, arenasConfig, "$path.lobby")
                }
                
                // Guardar spawns
                arena.spawns.forEachIndexed { index, spawn ->
                    serializeLocation(spawn, arenasConfig, "$path.spawns.$index")
                }
                
                // Guardar checkpoints
                arena.checkpoints.forEachIndexed { index, checkpoint ->
                    serializeCuboid(checkpoint, arenasConfig, "$path.checkpoints.$index")
                }
                
                // Guardar meta
                arena.meta?.let { meta ->
                    serializeCuboid(meta, arenasConfig, "$path.meta")
                }
                
                // Guardar región de protección
                arena.protectionRegion?.let { protection ->
                    serializeCuboid(protection, arenasConfig, "$path.protection")
                }
            }
            
            // Guardar archivo
            arenasConfig.save(arenasFile)
            plugin.logger.info("[Carrera de Barcos] ${arenas.size} arenas guardadas en ${arenasFile.absolutePath}")
            
        } catch (e: Exception) {
            plugin.logger.severe("[Carrera de Barcos] Error al guardar arenas: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Crea una nueva arena.
     */
    fun createArena(nombre: String): ArenaCarrera? {
        if (arenas.containsKey(nombre)) {
            return null // Arena ya existe
        }
        
        val arena = ArenaCarrera(nombre = nombre)
        arenas[nombre] = arena
        return arena
    }
    
    /**
     * Obtiene una arena por su nombre.
     */
    fun getArena(nombre: String): ArenaCarrera? {
        return arenas[nombre]
    }
    
    /**
     * Obtiene todas las arenas.
     */
    fun getAllArenas(): Collection<ArenaCarrera> {
        return arenas.values
    }
    
    /**
     * Elimina una arena.
     */
    fun removeArena(nombre: String): Boolean {
        return arenas.remove(nombre) != null
    }
    
    /**
     * Verifica si existe una arena con el nombre dado.
     */
    fun hasArena(nombre: String): Boolean {
        return arenas.containsKey(nombre)
    }
    
    // ========== MÉTODOS DE SERIALIZACIÓN ==========
    
    private fun serializeLocation(location: Location, config: YamlConfiguration, path: String) {
        config.set("$path.world", location.world?.name)
        config.set("$path.x", location.x)
        config.set("$path.y", location.y)
        config.set("$path.z", location.z)
        config.set("$path.yaw", location.yaw)
        config.set("$path.pitch", location.pitch)
    }
    
    private fun deserializeLocationSafe(section: org.bukkit.configuration.ConfigurationSection): Location? {
        try {
            val worldName = section.getString("world") ?: "world"
            val world = Bukkit.getWorld(worldName) ?: return null
            
            val x = section.getDouble("x")
            val y = section.getDouble("y")
            val z = section.getDouble("z")
            
            // Validar que el chunk esté cargado o sea seguro de cargar
            val chunkX = (x / 16).toInt()
            val chunkZ = (z / 16).toInt()
            
            // Solo crear la location si el chunk está cargado o es cercano al spawn
            if (world.isChunkLoaded(chunkX, chunkZ) || world.spawnLocation.distanceSquared(org.bukkit.Location(world, x, y, z)) < 10000) {
                return Location(
                    world,
                    x, y, z,
                    section.getDouble("yaw").toFloat(),
                    section.getDouble("pitch").toFloat()
                )
            } else {
                plugin.logger.warning("[Carrera de Barcos] Chunk no cargado para ubicación ($x, $y, $z), ignorando")
                return null
            }
        } catch (e: Exception) {
            plugin.logger.warning("[Carrera de Barcos] Error al deserializar ubicación: ${e.message}")
            return null
        }
    }
    
    private fun deserializeCuboidSafe(section: org.bukkit.configuration.ConfigurationSection): Cuboid? {
        try {
            val worldName = section.getString("world") ?: "world"
            val world = Bukkit.getWorld(worldName) ?: return null
            
            val minX = section.getInt("minX")
            val minY = section.getInt("minY")
            val minZ = section.getInt("minZ")
            val maxX = section.getInt("maxX")
            val maxY = section.getInt("maxY")
            val maxZ = section.getInt("maxZ")
            
            // Validar que al menos uno de los chunks esté cargado
            val chunksToCheck = listOf(
                (minX / 16) to (minZ / 16),
                (maxX / 16) to (maxZ / 16),
                ((minX + maxX) / 2 / 16) to ((minZ + maxZ) / 2 / 16)
            )
            
            val hasLoadedChunk = chunksToCheck.any { (cx, cz) -> world.isChunkLoaded(cx, cz) }
            
            if (hasLoadedChunk) {
                return Cuboid(world, minX, minY, minZ, maxX, maxY, maxZ)
            } else {
                plugin.logger.warning("[Carrera de Barcos] Ningún chunk cargado para cuboid, ignorando")
                return null
            }
        } catch (e: Exception) {
            plugin.logger.warning("[Carrera de Barcos] Error al deserializar cuboid: ${e.message}")
            return null
        }
    }
    
    private fun deserializeLocation(section: org.bukkit.configuration.ConfigurationSection): Location {
        val worldName = section.getString("world") ?: "world"
        val world = Bukkit.getWorld(worldName) ?: Bukkit.getWorlds()[0]
        
        return Location(
            world,
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z"),
            section.getDouble("yaw").toFloat(),
            section.getDouble("pitch").toFloat()
        )
    }
    
    private fun serializeCuboid(cuboid: Cuboid, config: YamlConfiguration, path: String) {
        config.set("$path.world", cuboid.world.name)
        config.set("$path.minX", cuboid.minX)
        config.set("$path.minY", cuboid.minY)
        config.set("$path.minZ", cuboid.minZ)
        config.set("$path.maxX", cuboid.maxX)
        config.set("$path.maxY", cuboid.maxY)
        config.set("$path.maxZ", cuboid.maxZ)
    }
    
    private fun deserializeCuboid(section: org.bukkit.configuration.ConfigurationSection): Cuboid {
        val worldName = section.getString("world") ?: "world"
        val world = Bukkit.getWorld(worldName) ?: Bukkit.getWorlds()[0]
        
        return Cuboid(
            world = world,
            minX = section.getInt("minX"),
            minY = section.getInt("minY"),
            minZ = section.getInt("minZ"),
            maxX = section.getInt("maxX"),
            maxY = section.getInt("maxY"),
            maxZ = section.getInt("maxZ")
        )
    }
}
