package los5fantasticos.torneo.services

import los5fantasticos.torneo.core.TorneoManager
import los5fantasticos.torneo.api.MinigameModule
import los5fantasticos.torneo.util.selection.Cuboid
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File

/**
 * Gestor centralizado del flujo de torneo.
 * 
 * ARQUITECTURA:
 * - Mantiene un lobby global donde residen todos los jugadores entre partidas
 * - Controla el inicio y fin de minijuegos de forma centralizada
 * - Los administradores tienen control total sobre el flujo del torneo
 * 
 * FLUJO:
 * 1. Jugadores esperan en el lobby global (mapa del Duoc UC)
 * 2. Admin ejecuta /torneo start <minigame>
 * 3. Todos los jugadores online son enviados al minijuego
 * 4. Al terminar, los jugadores regresan automáticamente al lobby
 */
object TournamentFlowManager {
    
    // ===== CONFIGURACIÓN DEL LOBBY GLOBAL =====
    private val globalLobbySpawns = mutableListOf<Location>()
    private var globalLobbyRegion: Cuboid? = null
    
    // ===== ESTADO DEL TORNEO =====
    var activeMinigame: MinigameModule? = null
        private set
    
    // ===== PERSISTENCIA =====
    private var plugin: Plugin? = null
    private lateinit var configFile: File
    private lateinit var config: YamlConfiguration
    
    /**
     * Inicializa el sistema de persistencia.
     */
    fun initialize(plugin: Plugin) {
        this.plugin = plugin
        
        // Crear archivo de configuración
        configFile = File(plugin.dataFolder, "lobby_config.yml")
        if (!configFile.exists()) {
            configFile.createNewFile()
            Bukkit.getLogger().info("[TournamentFlow] Archivo lobby_config.yml creado")
        }
        
        config = YamlConfiguration.loadConfiguration(configFile)
        
        // Cargar configuración guardada
        loadConfig()
    }
    
    /**
     * Establece la región del lobby global.
     * Esta región será protegida contra modificaciones.
     */
    fun setLobbyRegion(cuboid: Cuboid) {
        globalLobbyRegion = cuboid
        saveConfig() // Auto-guardar
        Bukkit.getLogger().info("[TournamentFlow] Región del lobby establecida")
    }
    
    /**
     * Obtiene la región del lobby global.
     */
    fun getLobbyRegion(): Cuboid? = globalLobbyRegion
    
    /**
     * Añade un punto de spawn al lobby global.
     */
    fun addLobbySpawn(location: Location) {
        globalLobbySpawns.add(location)
        saveConfig() // Auto-guardar
        Bukkit.getLogger().info("[TournamentFlow] Spawn añadido al lobby (total: ${globalLobbySpawns.size})")
    }
    
    /**
     * Limpia todos los spawns del lobby.
     */
    fun clearLobbySpawns() {
        globalLobbySpawns.clear()
        saveConfig() // Auto-guardar
        Bukkit.getLogger().info("[TournamentFlow] Spawns del lobby limpiados")
    }
    
    /**
     * Obtiene la lista de spawns del lobby.
     */
    fun getLobbySpawns(): List<Location> = globalLobbySpawns.toList()
    
    /**
     * Obtiene todos los jugadores que están en el lobby.
     * (Jugadores online que NO están en un minijuego activo)
     */
    fun getPlayersInLobby(): List<Player> {
        val activePlayers = activeMinigame?.getActivePlayers() ?: emptyList()
        return Bukkit.getOnlinePlayers().filter { !activePlayers.contains(it) }
    }
    
    /**
     * Inicia un minijuego para todos los jugadores online.
     * 
     * @param minigameName Nombre del minijuego a iniciar
     * @param torneoManager Instancia del TorneoManager
     * @return Mensaje de error si falla, null si tiene éxito
     */
    fun startMinigame(minigameName: String, torneoManager: TorneoManager): String? {
        // Verificar que no hay un minijuego activo
        if (activeMinigame != null) {
            return "Error: Ya hay un minijuego activo (${activeMinigame?.gameName}). Usa /torneo end primero."
        }
        
        // Buscar el minijuego por nombre
        val minigame = torneoManager.getMinigameByName(minigameName)
        if (minigame == null) {
            return "Error: Minijuego '$minigameName' no encontrado."
        }
        
        // Obtener todos los jugadores online
        val players = Bukkit.getOnlinePlayers().toList()
        
        if (players.isEmpty()) {
            return "Error: No hay jugadores online para iniciar el minijuego."
        }
        
        // Establecer como minijuego activo
        activeMinigame = minigame
        
        // Iniciar el minijuego para todos los jugadores
        try {
            minigame.onTournamentStart(players)
            
            Bukkit.getLogger().info("[TournamentFlow] Minijuego '${minigame.gameName}' iniciado con ${players.size} jugadores")
            
            // Notificar a todos
            val mensaje = Component.text("═══════════════════════════════════", NamedTextColor.GOLD)
                .append(Component.newline())
                .append(Component.text("  ¡Iniciando ${minigame.gameName}!", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("═══════════════════════════════════", NamedTextColor.GOLD))
            
            players.forEach { it.sendMessage(mensaje) }
            
            return null // Éxito
        } catch (e: Exception) {
            activeMinigame = null
            Bukkit.getLogger().severe("[TournamentFlow] Error iniciando minijuego: ${e.message}")
            e.printStackTrace()
            return "Error: Fallo al iniciar el minijuego. Revisa la consola."
        }
    }
    
    /**
     * Teletransporta un jugador de vuelta al lobby global.
     */
    fun returnToLobby(player: Player) {
        if (globalLobbySpawns.isEmpty()) {
            Bukkit.getLogger().warning("[TournamentFlow] No hay spawns configurados en el lobby")
            return
        }
        
        // Elegir un spawn aleatorio (o el primero si solo hay uno)
        val spawn = globalLobbySpawns.random()
        player.teleport(spawn)
        
        player.sendMessage(
            Component.text("✓ Has regresado al lobby", NamedTextColor.GREEN)
        )
    }
    
    /**
     * Finaliza el minijuego activo y devuelve a todos los jugadores al lobby.
     */
    fun endCurrentMinigame(): String? {
        val minigame = activeMinigame
        if (minigame == null) {
            return "Error: No hay ningún minijuego activo."
        }
        
        try {
            // Obtener jugadores activos antes de desactivar
            val activePlayers = minigame.getActivePlayers()
            
            // Desactivar el minijuego
            minigame.onDisable()
            
            // Devolver jugadores al lobby
            activePlayers.forEach { player ->
                returnToLobby(player)
            }
            
            Bukkit.getLogger().info("[TournamentFlow] Minijuego '${minigame.gameName}' finalizado. ${activePlayers.size} jugadores devueltos al lobby")
            
            // Limpiar estado
            activeMinigame = null
            
            // Notificar
            val mensaje = Component.text("═══════════════════════════════════", NamedTextColor.GOLD)
                .append(Component.newline())
                .append(Component.text("  ${minigame.gameName} Finalizado", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("═══════════════════════════════════", NamedTextColor.GOLD))
            
            Bukkit.getOnlinePlayers().forEach { it.sendMessage(mensaje) }
            
            return null // Éxito
        } catch (e: Exception) {
            Bukkit.getLogger().severe("[TournamentFlow] Error finalizando minijuego: ${e.message}")
            e.printStackTrace()
            return "Error: Fallo al finalizar el minijuego. Revisa la consola."
        }
    }
    
    /**
     * Limpia todos los datos al desactivar el plugin.
     */
    fun cleanup() {
        // NO limpiar spawns ni región, solo el estado activo
        activeMinigame = null
        Bukkit.getLogger().info("[TournamentFlow] Estado activo limpiado")
    }
    
    /**
     * Guarda la configuración del lobby en el archivo.
     */
    private fun saveConfig() {
        if (plugin == null) return
        
        try {
            // Limpiar configuración existente
            config.set("lobby", null)
            
            // Guardar región del lobby
            globalLobbyRegion?.let { region ->
                config.set("lobby.region.world", region.world.name)
                config.set("lobby.region.minX", region.minX)
                config.set("lobby.region.minY", region.minY)
                config.set("lobby.region.minZ", region.minZ)
                config.set("lobby.region.maxX", region.maxX)
                config.set("lobby.region.maxY", region.maxY)
                config.set("lobby.region.maxZ", region.maxZ)
            }
            
            // Guardar spawns
            globalLobbySpawns.forEachIndexed { index, spawn ->
                config.set("lobby.spawns.$index.world", spawn.world.name)
                config.set("lobby.spawns.$index.x", spawn.x)
                config.set("lobby.spawns.$index.y", spawn.y)
                config.set("lobby.spawns.$index.z", spawn.z)
                config.set("lobby.spawns.$index.yaw", spawn.yaw)
                config.set("lobby.spawns.$index.pitch", spawn.pitch)
            }
            
            config.save(configFile)
            Bukkit.getLogger().info("[TournamentFlow] Configuración del lobby guardada")
            
        } catch (e: Exception) {
            Bukkit.getLogger().severe("[TournamentFlow] Error al guardar configuración: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Carga la configuración del lobby desde el archivo.
     */
    private fun loadConfig() {
        try {
            // Cargar región del lobby
            val regionSection = config.getConfigurationSection("lobby.region")
            if (regionSection != null) {
                val worldName = regionSection.getString("world") ?: "world"
                val world = Bukkit.getWorld(worldName)
                
                if (world != null) {
                    val minX = regionSection.getInt("minX")
                    val minY = regionSection.getInt("minY")
                    val minZ = regionSection.getInt("minZ")
                    val maxX = regionSection.getInt("maxX")
                    val maxY = regionSection.getInt("maxY")
                    val maxZ = regionSection.getInt("maxZ")
                    
                    globalLobbyRegion = Cuboid(world, minX, minY, minZ, maxX, maxY, maxZ)
                    Bukkit.getLogger().info("[TournamentFlow] Región del lobby cargada")
                } else {
                    Bukkit.getLogger().warning("[TournamentFlow] Mundo '$worldName' no encontrado para la región del lobby")
                }
            }
            
            // Cargar spawns
            val spawnsSection = config.getConfigurationSection("lobby.spawns")
            if (spawnsSection != null) {
                for (key in spawnsSection.getKeys(false)) {
                    val spawnSection = spawnsSection.getConfigurationSection(key) ?: continue
                    
                    val worldName = spawnSection.getString("world") ?: "world"
                    val world = Bukkit.getWorld(worldName)
                    
                    if (world != null) {
                        val x = spawnSection.getDouble("x")
                        val y = spawnSection.getDouble("y")
                        val z = spawnSection.getDouble("z")
                        val yaw = spawnSection.getDouble("yaw").toFloat()
                        val pitch = spawnSection.getDouble("pitch").toFloat()
                        
                        val location = Location(world, x, y, z, yaw, pitch)
                        globalLobbySpawns.add(location)
                    } else {
                        Bukkit.getLogger().warning("[TournamentFlow] Mundo '$worldName' no encontrado para spawn $key")
                    }
                }
                
                Bukkit.getLogger().info("[TournamentFlow] ${globalLobbySpawns.size} spawns del lobby cargados")
            }
            
            if (globalLobbyRegion == null && globalLobbySpawns.isEmpty()) {
                Bukkit.getLogger().info("[TournamentFlow] No hay configuración de lobby guardada")
            }
            
        } catch (e: Exception) {
            Bukkit.getLogger().severe("[TournamentFlow] Error al cargar configuración: ${e.message}")
            e.printStackTrace()
        }
    }
}
