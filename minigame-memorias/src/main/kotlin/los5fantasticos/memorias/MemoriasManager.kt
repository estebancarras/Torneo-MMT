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
        
        // FASE 1 [CRÍTICO]: Cargar mazo de bloques
        BlockDeckManager.loadDeck(plugin)
        
        if (!BlockDeckManager.isReady()) {
            plugin.logger.severe("╔════════════════════════════════════════╗")
            plugin.logger.severe("║ ERROR CRÍTICO: Mazo de bloques vacío   ║")
            plugin.logger.severe("║ El minijuego no puede iniciarse        ║")
            plugin.logger.severe("╚════════════════════════════════════════╝")
            return
        }
        
        // Inicializar archivo de arenas con nombre único para este minijuego
        plugin.dataFolder.mkdirs()
        arenasFile = File(plugin.dataFolder, "memorias_arenas.yml")
        
        // CRÍTICO: Solo crear archivo si no existe, pero NO sobrescribir
        if (!arenasFile.exists()) {
            arenasFile.createNewFile()
            plugin.logger.info("[Memorias] Archivo arenas.yml creado")
        }
        
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile)
        plugin.logger.info("[Memorias] Archivo memorias_arenas.yml cargado desde: ${arenasFile.absolutePath}")
        
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
        plugin.logger.info("  - Mazo de bloques: ${BlockDeckManager.getDeckSize()} bloques únicos")
        plugin.logger.info("  - ${arenas.size} arenas cargadas")
        plugin.logger.info("  - Sistema de Game Loop centralizado activo")
    }
    
    override fun onDisable() {
        plugin.logger.info("[Memorias] ═══ INICIANDO SECUENCIA DE APAGADO ═══")
        plugin.logger.info("[Memorias] Estado de inicialización:")
        plugin.logger.info("[Memorias]   - arenasFile: ${::arenasFile.isInitialized}")
        plugin.logger.info("[Memorias]   - gameManager: ${::gameManager.isInitialized}")
        plugin.logger.info("[Memorias]   - Arenas en memoria: ${arenas.size}")
        
        // Guardar arenas (solo si se inicializó correctamente)
        if (::arenasFile.isInitialized) {
            plugin.logger.info("[Memorias] Llamando a guardarArenas()...")
            guardarArenas()
            plugin.logger.info("[Memorias] Configuración de arenas guardada en: ${arenasFile.absolutePath}")
        } else {
            plugin.logger.warning("[Memorias] ⚠ arenasFile NO inicializado, no se puede guardar")
        }
        
        // Terminar todos los juegos activos (solo si se inicializó)
        if (::gameManager.isInitialized) {
            gameManager.endAllGames()
        }
        
        // Limpiar selecciones
        SelectionManager.cleanup()
        
        plugin.logger.info("[Memorias] ✓ $gameName deshabilitado")
        plugin.logger.info("[Memorias] ═══ SECUENCIA DE APAGADO COMPLETADA ═══")
    }
    
    override fun isGameRunning(): Boolean {
        if (!::gameManager.isInitialized) return false
        return gameManager.getStats().contains("Duelos activos: ")
                && !gameManager.getStats().contains("Duelos activos: 0")
    }
    
    override fun getActivePlayers(): List<Player> {
        if (!::gameManager.isInitialized) return emptyList()
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
                    
                    if (corner1 != null && corner2 != null) {
                        val cuboid = Cuboid.fromLocations(corner1, corner2)
                        val parcela = Parcela(cuboid)
                        arena.addParcela(parcela)
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("Error cargando parcela $parcelaId de arena $nombreArena: ${e.message}")
                }
            }
            
            arenas[nombreArena] = arena
        }
        
        plugin.logger.info("Cargadas ${arenas.size} arenas desde memorias_arenas.yml")
    }
    
    /**
     * Guarda las arenas en el archivo de configuración.
     */
    fun guardarArenas() {
        try {
            plugin.logger.info("[Memorias] Iniciando guardado de arenas...")
            plugin.logger.info("[Memorias] Total de arenas a guardar: ${arenas.size}")
            
            // PROTECCIÓN: No sobrescribir con archivo vacío si no hay arenas
            if (arenas.isEmpty()) {
                plugin.logger.warning("[Memorias] No hay arenas para guardar, se omite el guardado")
                return
            }
            
            // CRÍTICO: Limpiar la configuración existente sin perder la referencia al archivo
            arenasConfig.getKeys(false).forEach { key -> arenasConfig.set(key, null) }
            
            for ((nombreArena, arena) in arenas) {
                val arenaPath = "arenas.$nombreArena"
                plugin.logger.info("[Memorias] Guardando arena: $nombreArena")
                
                // Guardar lobby spawn
                arena.lobbySpawn?.let {
                    arenasConfig.set("$arenaPath.lobby", it)
                    plugin.logger.info("[Memorias]   - Lobby guardado")
                }
                
                // Guardar parcelas
                plugin.logger.info("[Memorias]   - Guardando ${arena.parcelas.size} parcelas")
                arena.parcelas.forEachIndexed { index, parcela ->
                    val parcelaPath = "$arenaPath.parcelas.parcela$index"
                    arenasConfig.set("$parcelaPath.corner1", Location(
                        parcela.region.world,
                        parcela.region.minX.toDouble(),
                        parcela.region.minY.toDouble(),
                        parcela.region.minZ.toDouble()
                    ))
                    arenasConfig.set("$parcelaPath.corner2", Location(
                        parcela.region.world,
                        parcela.region.maxX.toDouble(),
                        parcela.region.maxY.toDouble(),
                        parcela.region.maxZ.toDouble()
                    ))
                }
            }
            
            plugin.logger.info("[Memorias] Escribiendo archivo: ${arenasFile.absolutePath}")
            arenasConfig.save(arenasFile)
            plugin.logger.info("[Memorias] ✓ Arenas guardadas exitosamente")
            
        } catch (e: Exception) {
            plugin.logger.severe("[Memorias] ✗ ERROR CRÍTICO guardando arenas:")
            plugin.logger.severe("[Memorias]   Mensaje: ${e.message}")
            plugin.logger.severe("[Memorias]   Tipo: ${e.javaClass.simpleName}")
            e.printStackTrace()
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
