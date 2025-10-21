package los5fantasticos.torneo

import los5fantasticos.torneo.api.MinigameModule
import los5fantasticos.torneo.core.TorneoManager
import los5fantasticos.torneo.commands.RankingCommand
import los5fantasticos.torneo.commands.TorneoAdminCommand
import los5fantasticos.torneo.listeners.GlobalLobbyListener
import los5fantasticos.torneo.listeners.PlayerConnectionListener
import los5fantasticos.torneo.listeners.SelectionListener
import los5fantasticos.torneo.services.GlobalScoreboardService
import los5fantasticos.torneo.services.TournamentFlowManager
import los5fantasticos.torneo.util.selection.SelectionManager
import org.bukkit.ChatColor
import org.bukkit.plugin.java.JavaPlugin

/**
 * Plugin principal del sistema de torneo de Minecraft.
 * 
 * Este plugin actúa como orquestador central, gestionando:
 * - Registro y coordinación de minijuegos
 * - Sistema de puntaje global
 * - Comandos administrativos del torneo
 * 
 * Los minijuegos se registran como módulos y son gestionados por el TorneoManager.
 */
class TorneoPlugin : JavaPlugin() {
    
    companion object {
        /**
         * Instancia singleton del plugin para acceso global.
         */
        lateinit var instance: TorneoPlugin
            private set
    }
    
    /**
     * Gestor central del torneo.
     */
    lateinit var torneoManager: TorneoManager
        private set
    
    /**
     * Servicio de scoreboard global.
     */
    private lateinit var scoreboardService: GlobalScoreboardService
    
    /**
     * Mapa de módulos de minijuegos cargados.
     */
    private val minigameModules = mutableListOf<MinigameModule>()
    
    override fun onEnable() {
        instance = this
        
        logger.info("═══════════════════════════════════════")
        logger.info("  Torneo DuocUC - Sistema de Minijuegos")
        logger.info("  Versión: ${description.version}")
        logger.info("═══════════════════════════════════════")
        
        // Crear carpeta de datos si no existe
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        
        // Inicializar el gestor del torneo
        torneoManager = TorneoManager(this)
        logger.info("✓ TorneoManager inicializado")
        
        // Inicializar el servicio de scoreboard global
        scoreboardService = GlobalScoreboardService(this, torneoManager)
        scoreboardService.initialize()
        scoreboardService.startUpdating()
        logger.info("✓ GlobalScoreboardService inicializado")
        
        // Inicializar TournamentFlowManager con persistencia
        TournamentFlowManager.initialize(this)
        logger.info("✓ TournamentFlowManager inicializado con persistencia")
        
        // Registrar listeners
        val playerConnectionListener = PlayerConnectionListener(scoreboardService)
        playerConnectionListener.initialize(this) // Inicializar con la instancia del plugin
        server.pluginManager.registerEvents(playerConnectionListener, this)
        server.pluginManager.registerEvents(GlobalLobbyListener(), this)
        server.pluginManager.registerEvents(SelectionListener(), this)
        logger.info("✓ Listeners registrados")
        
        // Registrar comandos del core
        registerCoreCommands()
        logger.info("✓ Comandos del core registrados")
        
        // Cargar y registrar minijuegos
        loadMinigames()
        
        logger.info("═══════════════════════════════════════")
        logger.info("  ${ChatColor.GREEN}Plugin habilitado correctamente")
        logger.info("  Minijuegos cargados: ${minigameModules.size}")
        logger.info("═══════════════════════════════════════")
    }
    
    override fun onDisable() {
        logger.info("═══════════════════════════════════════")
        logger.info("  Iniciando secuencia de apagado de TorneoMMT")
        logger.info("═══════════════════════════════════════")
        
        // Detener el servicio de scoreboard primero
        if (::scoreboardService.isInitialized) {
            logger.info("Deteniendo servicio de scoreboard...")
            scoreboardService.shutdown()
            logger.info("✓ Scoreboard service detenido")
        }
        
        logger.info("Deshabilitando y guardando datos de ${minigameModules.size} minijuegos...")
        
        // Iterar sobre las instancias REALES y VIVAS que hemos guardado durante onEnable()
        // Esto garantiza que se guarden los datos correctos, no instancias vacías
        for (module in minigameModules) {
            try {
                logger.info("Descargando módulo '${module.gameName}'...")
                module.onDisable() // Cada módulo guarda sus propios datos en su onDisable()
                logger.info("✓ ${module.gameName} deshabilitado y datos guardados")
            } catch (e: Exception) {
                logger.severe("✗ ERROR CRÍTICO al descargar '${module.gameName}': ${e.message}")
                e.printStackTrace()
            }
        }
        
        // Limpiar la lista de módulos para liberar memoria
        minigameModules.clear()
        logger.info("✓ Lista de módulos limpiada")
        
        // Guardar los datos globales del core (puntajes del torneo)
        if (::torneoManager.isInitialized) {
            logger.info("Guardando puntajes globales del torneo...")
            torneoManager.saveScores()
            logger.info("✓ Puntajes del torneo guardados")
        }
        
        // Limpiar sistemas centralizados
        logger.info("Limpiando sistemas centralizados...")
        TournamentFlowManager.cleanup()
        SelectionManager.cleanup()
        logger.info("✓ Sistemas centralizados limpiados")
        
        logger.info("═══════════════════════════════════════")
        logger.info("  ${ChatColor.GREEN}TorneoMMT deshabilitado correctamente")
        logger.info("  Todos los datos han sido persistidos")
        logger.info("═══════════════════════════════════════")
    }
    
    /**
     * Registra los comandos del núcleo del torneo.
     */
    private fun registerCoreCommands() {
        // Comando de ranking
        getCommand("ranking")?.setExecutor(RankingCommand(torneoManager))
        
        // Comando de administración del torneo
        val torneoAdminCmd = TorneoAdminCommand(torneoManager)
        getCommand("torneo")?.apply {
            setExecutor(torneoAdminCmd)
            tabCompleter = torneoAdminCmd
        }
    }
    
    /**
     * Carga y registra todos los módulos de minijuegos.
     * 
     * Los minijuegos se cargan dinámicamente mediante reflexión.
     * Cada minijuego debe implementar la interfaz MinigameModule.
     */
    private fun loadMinigames() {
        logger.info("Cargando módulos de minijuegos...")
        
        // Intentar cargar RobarCola
        try {
            val robarColaClass = Class.forName("yo.spray.robarCola.RobarColaManager")
            val constructor = robarColaClass.getConstructor(TorneoPlugin::class.java)
            val robarColaModule = constructor.newInstance(this) as MinigameModule
            
            registerMinigame(robarColaModule)
        } catch (_: ClassNotFoundException) {
            logger.warning("⚠ Módulo RobarCola no encontrado (esto es normal si no está compilado)")
        } catch (e: Exception) {
            logger.severe("✗ Error al cargar RobarCola: ${e.message}")
            e.printStackTrace()
        }
        
        // Intentar cargar Memorias
        try {
            val memoriasClass = Class.forName("los5fantasticos.memorias.MemoriasManager")
            val constructor = memoriasClass.getConstructor(TorneoPlugin::class.java)
            val memoriasModule = constructor.newInstance(this) as MinigameModule
            
            registerMinigame(memoriasModule)
        } catch (_: ClassNotFoundException) {
            logger.warning("⚠ Módulo Memorias no encontrado (esto es normal si no está compilado)")
        } catch (e: Exception) {
            logger.severe("✗ Error al cargar Memorias: ${e.message}")
            e.printStackTrace()
        }
        
        // Intentar cargar SkyWars
        try {
            val skywarsClass = Class.forName("los5fantasticos.minigameSkywars.MinigameSkywars")
            val constructor = skywarsClass.getConstructor(TorneoPlugin::class.java)
            val skywarsModule = constructor.newInstance(this) as MinigameModule
            
            registerMinigame(skywarsModule)
        } catch (_: ClassNotFoundException) {
            logger.warning("⚠ Módulo SkyWars no encontrado")
        } catch (e: Exception) {
            logger.warning("⚠ Error al cargar SkyWars: ${e.message}")
            e.printStackTrace()
        }
        
        // Intentar cargar Laberinto
        try {
            val laberintoClass = Class.forName("los5fantasticos.minigameLaberinto.MinigameLaberinto")
            val constructor = laberintoClass.getConstructor(TorneoPlugin::class.java)
            val laberintoModule = constructor.newInstance(this) as MinigameModule
            
            registerMinigame(laberintoModule)
        } catch (_: ClassNotFoundException) {
            logger.warning("⚠ Módulo Laberinto no encontrado")
        } catch (e: Exception) {
            logger.warning("⚠ Error al cargar Laberinto: ${e.message}")
            e.printStackTrace()
        }
        
        // Intentar cargar Carrera de Barcos
        try {
            val carrerabarcosClass = Class.forName("los5fantasticos.minigameCarrerabarcos.MinigameCarrerabarcos")
            val constructor = carrerabarcosClass.getConstructor(TorneoPlugin::class.java)
            val carrerabarcosModule = constructor.newInstance(this) as MinigameModule
            
            registerMinigame(carrerabarcosModule)
        } catch (_: ClassNotFoundException) {
            logger.warning("⚠ Módulo Carrera de Barcos no encontrado")
        } catch (e: Exception) {
            logger.warning("⚠ Error al cargar Carrera de Barcos: ${e.message}")
            e.printStackTrace()
        }
        
        // Intentar cargar Cadena
        try {
            val cadenaClass = Class.forName("los5fantasticos.minigameCadena.MinigameCadena")
            val constructor = cadenaClass.getConstructor(TorneoPlugin::class.java)
            val cadenaModule = constructor.newInstance(this) as MinigameModule
            
            registerMinigame(cadenaModule)
        } catch (_: ClassNotFoundException) {
            logger.warning("⚠ Módulo Cadena no encontrado")
        } catch (e: Exception) {
            logger.warning("⚠ Error al cargar Cadena: ${e.message}")
            e.printStackTrace()
        }
        
        // Intentar cargar Hunger Games
        try {
            val hungergamesClass = Class.forName("los5fantasticos.minigameHungergames.MinigameHungergames")
            val constructor = hungergamesClass.getConstructor(TorneoPlugin::class.java)
            val hungergamesModule = constructor.newInstance(this) as MinigameModule
            
            registerMinigame(hungergamesModule)
        } catch (_: ClassNotFoundException) {
            logger.warning("⚠ Módulo Hunger Games no encontrado")
        } catch (e: Exception) {
            logger.warning("⚠ Error al cargar Hunger Games: ${e.message}")
            e.printStackTrace()
        }
        
        if (minigameModules.isEmpty()) {
            logger.warning("⚠ No se cargaron minijuegos. Verifica que los módulos estén compilados correctamente.")
        }
    }
    
    /**
     * Registra un módulo de minijuego en el sistema.
     * 
     * @param module Módulo del minijuego a registrar
     */
    fun registerMinigame(module: MinigameModule) {
        try {
            // Registrar en el TorneoManager
            torneoManager.registerMinigame(module)
            
            // Inicializar el módulo
            module.onEnable(this)
            
            // Registrar comandos del módulo
            val commandExecutors = module.getCommandExecutors()
            commandExecutors.forEach { (commandName, executor) ->
                val command = getCommand(commandName)
                if (command != null) {
                    command.setExecutor(executor)
                    if (executor is org.bukkit.command.TabCompleter) {
                        command.tabCompleter = executor
                    }
                    logger.info("  ✓ Comando '/$commandName' registrado")
                } else {
                    logger.warning("  ⚠ Comando '/$commandName' no encontrado en plugin.yml")
                }
            }
            
            // Añadir a la lista de módulos cargados
            minigameModules.add(module)
            
            logger.info("✓ Minijuego cargado: ${module.gameName} v${module.version}")
            logger.info("  Descripción: ${module.description}")
        } catch (e: Exception) {
            logger.severe("✗ Error al registrar minijuego ${module.gameName}: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Obtiene un módulo de minijuego por su nombre.
     * 
     * @param name Nombre del minijuego
     * @return El módulo del minijuego, o null si no existe
     */
    @Suppress("unused")
    fun getMinigame(name: String): MinigameModule? {
        return minigameModules.find { it.gameName.equals(name, ignoreCase = true) }
    }
    
    /**
     * Obtiene todos los módulos de minijuegos cargados.
     * 
     * @return Lista de módulos de minijuegos
     */
    @Suppress("unused")
    fun getAllMinigames(): List<MinigameModule> {
        return minigameModules.toList()
    }
}
