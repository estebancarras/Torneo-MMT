package los5fantasticos.memorias

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File

/**
 * Manager del minijuego Memorias (REFACTORIZADO).
 * 
 * Sistema de duelos 1v1 con Game Loop centralizado.
 * Soporta múltiples arenas y parcelas para alta concurrencia.
 */
class MemoriasManager(internal val torneoPlugin: TorneoPlugin) : MinigameModule {
    
    override val gameName = "Memorias"
    override val version = "2.0"
    override val description = "Minijuego de memoria donde debes encontrar todos los pares de colores"
    
    private lateinit var plugin: Plugin
    private lateinit var gameManager: GameManager
    
    // Sistema de persistencia de arenas
    private val arenas = mutableMapOf<String, Arena>()
    private lateinit var arenasFile: File
    private lateinit var arenasConfig: YamlConfiguration
    
    override fun onEnable(plugin: Plugin) {
        this.plugin = plugin
        
        // Inicializar archivo de arenas
        arenasFile = File(plugin.dataFolder, "arenas.yml")
        if (!arenasFile.exists()) {
            plugin.dataFolder.mkdirs()
            arenasFile.createNewFile()
        }
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile)
        
        // Cargar arenas
        cargarArenas()
        
        // Inicializar el GameManager REFACTORIZADO
        gameManager = GameManager(plugin, this)
        
        // Seleccionar primera arena disponible
        arenas.values.firstOrNull()?.let { gameManager.setArena(it) }
        
        // Registrar eventos
        plugin.server.pluginManager.registerEvents(PlayerListener(gameManager), plugin)
        
        // Registrar comandos REFACTORIZADOS
        val commandExecutor = los5fantasticos.memorias.commands.MemoriasCommand(gameManager, this)
        torneoPlugin.getCommand("memorias")?.setExecutor(commandExecutor)
        torneoPlugin.getCommand("memorias")?.tabCompleter = commandExecutor
        
        plugin.logger.info("✓ $gameName v$version habilitado")
        plugin.logger.info("  - ${arenas.size} arenas cargadas")
        plugin.logger.info("  - Sistema de Game Loop centralizado activo")
    }
    
    override fun onDisable() {
        // Guardar arenas
        guardarArenas()
        
        // Terminar todos los juegos activos
        gameManager.endAllGames()
        
        plugin.logger.info("✓ $gameName deshabilitado")
    }
    
    override fun isGameRunning(): Boolean {
        return gameManager.getStats().contains("Duelos activos: ")
                && !gameManager.getStats().contains("Duelos activos: 0")
    }
    
    override fun getActivePlayers(): List<Player> {
        // Implementación básica - el GameManager no expone directamente la lista
        return emptyList()
    }
    
    /**
     * Carga las arenas desde el archivo de configuración.
     */
    private fun cargarArenas() {
        arenas.clear()
        
        val arenasSection = arenasConfig.getConfigurationSection("arenas") ?: return
        
        for (nombreArena in arenasSection.getKeys(false)) {
            val arenaSection = arenasSection.getConfigurationSection(nombreArena) ?: continue
            
            val arena = Arena(nombreArena)
            
            // Cargar lobby spawn
            if (arenaSection.contains("lobby")) {
                arena.lobbySpawn = arenaSection.getLocation("lobby")
            }
            
            // Cargar parcelas
            val parcelasSection = arenaSection.getConfigurationSection("parcelas") ?: continue
            
            for (parcelaId in parcelasSection.getKeys(false)) {
                val parcelaSection = parcelasSection.getConfigurationSection(parcelaId) ?: continue
                
                try {
                    val corner1 = parcelaSection.getLocation("corner1")
                    val corner2 = parcelaSection.getLocation("corner2")
                    val spawn1 = parcelaSection.getLocation("spawn1")
                    val spawn2 = parcelaSection.getLocation("spawn2")
                    
                    if (corner1 != null && corner2 != null && spawn1 != null && spawn2 != null) {
                        val cuboid = Cuboid.fromLocations(corner1, corner2)
                        val parcela = Parcela(cuboid, spawn1, spawn2)
                        arena.addParcela(parcela)
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("Error cargando parcela $parcelaId de arena $nombreArena: ${e.message}")
                }
            }
            
            arenas[nombreArena] = arena
        }
        
        plugin.logger.info("Cargadas ${arenas.size} arenas desde arenas.yml")
    }
    
    /**
     * Guarda las arenas en el archivo de configuración.
     */
    fun guardarArenas() {
        arenasConfig = YamlConfiguration()
        
        for ((nombreArena, arena) in arenas) {
            val arenaPath = "arenas.$nombreArena"
            
            // Guardar lobby spawn
            arena.lobbySpawn?.let {
                arenasConfig.set("$arenaPath.lobby", it)
            }
            
            // Guardar parcelas
            arena.parcelas.forEachIndexed { index, parcela ->
                val parcelaPath = "$arenaPath.parcelas.parcela$index"
                arenasConfig.set("$parcelaPath.corner1", Location(
                    parcela.regionTablero.world,
                    parcela.regionTablero.minX.toDouble(),
                    parcela.regionTablero.minY.toDouble(),
                    parcela.regionTablero.minZ.toDouble()
                ))
                arenasConfig.set("$parcelaPath.corner2", Location(
                    parcela.regionTablero.world,
                    parcela.regionTablero.maxX.toDouble(),
                    parcela.regionTablero.maxY.toDouble(),
                    parcela.regionTablero.maxZ.toDouble()
                ))
                arenasConfig.set("$parcelaPath.spawn1", parcela.spawn1)
                arenasConfig.set("$parcelaPath.spawn2", parcela.spawn2)
            }
        }
        
        try {
            arenasConfig.save(arenasFile)
        } catch (e: Exception) {
            plugin.logger.severe("Error guardando arenas: ${e.message}")
        }
    }
    
    /**
     * Agrega una arena al sistema.
     */
    fun agregarArena(arena: Arena) {
        arenas[arena.nombre] = arena
        guardarArenas()
    }
    
    /**
     * Obtiene una arena por nombre.
     */
    fun obtenerArena(nombre: String): Arena? {
        return arenas[nombre]
    }
    
    /**
     * Obtiene todas las arenas.
     */
    fun obtenerArenas(): List<Arena> {
        return arenas.values.toList()
    }
    
    /**
     * Registra una victoria en el torneo.
     */
    fun recordVictory(player: Player) {
        torneoPlugin.torneoManager.recordGameWon(player, gameName)
        torneoPlugin.torneoManager.recordGamePlayed(player, gameName)
    }
    
    /**
     * Registra una partida jugada.
     */
    fun recordGamePlayed(player: Player) {
        torneoPlugin.torneoManager.recordGamePlayed(player, gameName)
    }
}
