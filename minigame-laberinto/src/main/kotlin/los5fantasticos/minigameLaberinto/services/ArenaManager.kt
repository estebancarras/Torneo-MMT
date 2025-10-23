package los5fantasticos.minigameLaberinto.services

import los5fantasticos.minigameLaberinto.game.Arena
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.util.BoundingBox
import java.io.File

/**
 * Gestor de arenas del minijuego Laberinto.
 * 
 * Responsable de:
 * - Cargar y guardar configuraciones de arenas
 * - Gestionar la ubicación del lobby global
 * - Proporcionar acceso a las arenas configuradas
 */
class ArenaManager {
    
    /**
     * Mapa de arenas cargadas por nombre.
     */
    val arenas = mutableMapOf<String, Arena>()
    
    /**
     * Ubicación del lobby global.
     */
    private var lobbyLocation: Location? = null
    
    /**
     * Archivo de configuración de arenas.
     */
    private var arenasFile: File? = null
    
    /**
     * Inicializa el gestor de arenas.
     * 
     * @param dataFolder Carpeta de datos del plugin
     */
    fun initialize(dataFolder: File) {
        arenasFile = File(dataFolder, "laberinto_arenas.yml")
        loadArenas()
    }
    
    /**
     * Carga todas las arenas desde el archivo de configuración.
     */
    private fun loadArenas() {
        if (arenasFile == null || !arenasFile!!.exists()) {
            return
        }
        
        try {
            val config = YamlConfiguration.loadConfiguration(arenasFile!!)
            
            // Cargar ubicación del lobby
            if (config.contains("lobby")) {
                val lobbySection = config.getConfigurationSection("lobby")
                if (lobbySection != null) {
                    val worldName = lobbySection.getString("world")
                    if (worldName != null) {
                    val x = lobbySection.getDouble("x")
                    val y = lobbySection.getDouble("y")
                    val z = lobbySection.getDouble("z")
                    val yaw = lobbySection.getDouble("yaw", 0.0).toFloat()
                    val pitch = lobbySection.getDouble("pitch", 0.0).toFloat()
                    
                    val world = Bukkit.getWorld(worldName)
                        if (world != null) {
                            lobbyLocation = Location(world, x, y, z, yaw, pitch)
                        }
                    }
                }
            }
            
            // Cargar arenas
            if (config.contains("arenas")) {
                val arenasSection = config.getConfigurationSection("arenas")
                if (arenasSection != null) {
                    for (arenaName in arenasSection.getKeys(false)) {
                        val arenaSection = arenasSection.getConfigurationSection(arenaName)
                        if (arenaSection != null) {
                            val arena = loadArenaFromConfig(arenaName, arenaSection)
                            if (arena != null) {
                                arenas[arenaName] = arena
                            }
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Carga una arena específica desde la configuración.
     * 
     * @param name Nombre de la arena
     * @param config Sección de configuración de la arena
     * @return Arena cargada, o null si hay error
     */
    private fun loadArenaFromConfig(name: String, config: org.bukkit.configuration.ConfigurationSection): Arena? {
        try {
            // Cargar mundo
            val worldName = config.getString("world") ?: return null
            val world = Bukkit.getWorld(worldName) ?: return null
            
            // Cargar ubicación de inicio
            val startSection = config.getConfigurationSection("start") ?: return null
            
            val startLocation = Location(
                world,
                startSection.getDouble("x"),
                startSection.getDouble("y"),
                startSection.getDouble("z"),
                startSection.getDouble("yaw", 0.0).toFloat(),
                startSection.getDouble("pitch", 0.0).toFloat()
            )
            
            // Cargar región de meta
            val finishSection = config.getConfigurationSection("finish") ?: return null
            
            val finishRegion = BoundingBox(
                finishSection.getDouble("minX"),
                finishSection.getDouble("minY"),
                finishSection.getDouble("minZ"),
                finishSection.getDouble("maxX"),
                finishSection.getDouble("maxY"),
                finishSection.getDouble("maxZ")
            )
            
            // Cargar ubicaciones de jumpscares
            val jumpscareLocations = mutableListOf<Location>()
            if (config.contains("jumpscares")) {
                @Suppress("UNCHECKED_CAST")
                val jumpscaresList = config.getList("jumpscares") as? List<Map<String, Any>> ?: emptyList()
                jumpscaresList.forEach { jumpscareData ->
                    val jumpscareLocation = Location(
                        world,
                        jumpscareData["x"] as Double,
                        jumpscareData["y"] as Double,
                        jumpscareData["z"] as Double
                    )
                    jumpscareLocations.add(jumpscareLocation)
                }
            }
            
            // Cargar configuración de la partida
            val gameDuration = config.getInt("gameDuration", 300)
            val minPlayers = config.getInt("minPlayers", 2)
            val maxPlayers = config.getInt("maxPlayers", 8)
            
            // Cargar spectatorBounds si existe
            var spectatorBounds: BoundingBox? = null
            if (config.contains("spectatorBounds.minX") && config.contains("spectatorBounds.maxX")) {
                spectatorBounds = BoundingBox(
                    config.getDouble("spectatorBounds.minX"),
                    config.getDouble("spectatorBounds.minY"),
                    config.getDouble("spectatorBounds.minZ"),
                    config.getDouble("spectatorBounds.maxX"),
                    config.getDouble("spectatorBounds.maxY"),
                    config.getDouble("spectatorBounds.maxZ")
                )
            }
            
            return Arena(
                name = name,
                world = world,
                startLocation = startLocation,
                finishRegion = finishRegion,
                jumpscareLocations = jumpscareLocations,
                gameDuration = gameDuration,
                minPlayers = minPlayers,
                maxPlayers = maxPlayers
                , spectatorBounds = spectatorBounds
            )
            
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Guarda todas las arenas en el archivo de configuración.
     */
    fun saveArenas() {
        if (arenasFile == null) {
            return
        }
        
        try {
            // Si no hay arenas y no hay lobby configurado, no sobreescribir el archivo existente
            if (arenas.isEmpty() && lobbyLocation == null && arenasFile!!.exists()) {
                // Nada que guardar
                return
            }

            val config = YamlConfiguration()
            
            // Guardar ubicación del lobby (si existe)
            if (lobbyLocation != null) {
                config.set("lobby.world", lobbyLocation!!.world?.name)
                config.set("lobby.x", lobbyLocation!!.x)
                config.set("lobby.y", lobbyLocation!!.y)
                config.set("lobby.z", lobbyLocation!!.z)
                config.set("lobby.yaw", lobbyLocation!!.yaw.toDouble())
                config.set("lobby.pitch", lobbyLocation!!.pitch.toDouble())
            } else {
                // Si existe un archivo previo que contiene lobby, mantenerlo leyendo antes
                if (arenasFile!!.exists()) {
                    try {
                        val existing = YamlConfiguration.loadConfiguration(arenasFile!!)
                        if (existing.contains("lobby")) {
                            config.set("lobby", existing.getConfigurationSection("lobby"))
                        }
                    } catch (_: Exception) {
                        // ignore
                    }
                }
            }
            
            // Guardar arenas
            for ((name, arena) in arenas) {
                val arenaSection = config.createSection("arenas.$name")
                
                // Guardar mundo
                arenaSection.set("world", arena.world.name)
                
                // Guardar ubicación de inicio
                arenaSection.set("start.x", arena.startLocation.x)
                arenaSection.set("start.y", arena.startLocation.y)
                arenaSection.set("start.z", arena.startLocation.z)
                arenaSection.set("start.yaw", arena.startLocation.yaw.toDouble())
                arenaSection.set("start.pitch", arena.startLocation.pitch.toDouble())
                
                // Guardar región de meta
                arenaSection.set("finish.minX", arena.finishRegion.minX)
                arenaSection.set("finish.minY", arena.finishRegion.minY)
                arenaSection.set("finish.minZ", arena.finishRegion.minZ)
                arenaSection.set("finish.maxX", arena.finishRegion.maxX)
                arenaSection.set("finish.maxY", arena.finishRegion.maxY)
                arenaSection.set("finish.maxZ", arena.finishRegion.maxZ)
                
                // Guardar ubicaciones de jumpscares
                val jumpscaresList = arena.jumpscareLocations.map { location ->
                    mapOf(
                        "x" to location.x,
                        "y" to location.y,
                        "z" to location.z
                    )
                }
                arenaSection.set("jumpscares", jumpscaresList)
                
                // Guardar configuración de la partida
                arenaSection.set("gameDuration", arena.gameDuration)
                arenaSection.set("minPlayers", arena.minPlayers)
                arenaSection.set("maxPlayers", arena.maxPlayers)
                
                // Guardar lmites de espectador si existen
                arena.spectatorBounds?.let { bounds ->
                    arenaSection.set("spectatorBounds.minX", bounds.minX)
                    arenaSection.set("spectatorBounds.minY", bounds.minY)
                    arenaSection.set("spectatorBounds.minZ", bounds.minZ)
                    arenaSection.set("spectatorBounds.maxX", bounds.maxX)
                    arenaSection.set("spectatorBounds.maxY", bounds.maxY)
                    arenaSection.set("spectatorBounds.maxZ", bounds.maxZ)
                }
            }
            
            config.save(arenasFile!!)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Establece la ubicación del lobby global.
     * 
     * @param location Nueva ubicación del lobby
     */
    fun setLobbyLocation(location: Location) {
        lobbyLocation = location.clone()
    }
    
    /**
     * Obtiene la ubicación del lobby global.
     * 
     * @return Ubicación del lobby, o null si no está configurada
     */
    fun getLobbyLocation(): Location? {
        return lobbyLocation?.clone()
    }
    
    /**
     * Crea una nueva arena.
     * 
     * @param name Nombre de la arena
     * @param world Mundo donde se encuentra la arena
     * @return La arena creada
     */
    fun createArena(name: String, world: World): Arena {
        val arena = Arena(
            name = name,
            world = world,
            startLocation = Location(world, 0.0, 0.0, 0.0),
            finishRegion = BoundingBox(0.0, 0.0, 0.0, 1.0, 1.0, 1.0),
            jumpscareLocations = emptyList()
        )
        
        arenas[name] = arena
        return arena
    }
    
    /**
     * Obtiene una arena por nombre.
     * 
     * @param name Nombre de la arena
     * @return La arena, o null si no existe
     */
    fun getArena(name: String): Arena? {
        return arenas[name]
    }
    
    /**
     * Obtiene todas las arenas disponibles.
     * 
     * @return Lista de todas las arenas
     */
    fun getAllArenas(): List<Arena> {
        return arenas.values.toList()
    }
    
    /**
     * Verifica si existe una arena con el nombre especificado.
     * 
     * @param name Nombre de la arena
     * @return true si existe, false en caso contrario
     */
    fun arenaExists(name: String): Boolean {
        return arenas.containsKey(name)
    }
    
    /**
     * Elimina una arena.
     * 
     * @param name Nombre de la arena a eliminar
     * @return true si se eliminó exitosamente, false si no existía
     */
    fun removeArena(name: String): Boolean {
        return arenas.remove(name) != null
    }
    
    /**
     * Obtiene una lista de nombres de arenas disponibles.
     * 
     * @return Lista de nombres de arenas
     */
    fun getArenaNames(): List<String> {
        return arenas.keys.toList()
    }
    
    /**
     * Limpia todas las arenas cargadas.
     */
    fun clearAll() {
        arenas.clear()
        lobbyLocation = null
    }
}
