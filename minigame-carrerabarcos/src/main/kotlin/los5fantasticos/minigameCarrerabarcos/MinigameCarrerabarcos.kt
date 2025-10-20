package los5fantasticos.minigameCarrerabarcos

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
import los5fantasticos.minigameCarrerabarcos.commands.CarreraCommand
import los5fantasticos.minigameCarrerabarcos.services.ArenaManager
import los5fantasticos.minigameCarrerabarcos.services.GameManager
import los5fantasticos.minigameCarrerabarcos.listeners.GameListener
import org.bukkit.command.CommandExecutor
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * Módulo principal del minijuego Carrera de Barcos (REFACTORIZADO).
 * 
 * ARQUITECTURA MODULAR:
 * - ArenaManager: Gestión y persistencia de circuitos
 * - GameManager: Lógica de juego y carreras activas
 * - GameListener: Detección de eventos (checkpoints, meta)
 * - CarreraCommand: Interfaz de comandos para admin y jugadores
 * 
 * FILOSOFÍA:
 * - Cero hardcodeo: Todas las configuraciones se definen mediante comandos
 * - Separación de responsabilidades: Cada servicio tiene un propósito único
 * - Persistencia garantizada: Los circuitos se guardan automáticamente
 */
class MinigameCarrerabarcos(private val torneoPlugin: TorneoPlugin) : MinigameModule {

    private lateinit var plugin: Plugin
    private lateinit var arenaManager: ArenaManager
    private lateinit var gameManager: GameManager

    override val gameName = "Carrera de Barcos"
    override val version = "2.0"
    override val description = "Minijuego de carreras acuáticas configurable - ¡el más rápido gana!"

    override fun onEnable(plugin: Plugin) {
        this.plugin = plugin
        
        // FASE 1: Inicializar ArenaManager y cargar circuitos
        arenaManager = ArenaManager(plugin)
        arenaManager.initialize()
        
        // FASE 2: Inicializar GameManager
        gameManager = GameManager(plugin, torneoPlugin)
        
        // FASE 3: Registrar listeners
        plugin.server.pluginManager.registerEvents(GameListener(gameManager), plugin)
        
        plugin.logger.info("✓ $gameName v$version habilitado")
        plugin.logger.info("  - ${arenaManager.getAllArenas().size} circuitos cargados")
        plugin.logger.info("  - Arquitectura modular activa")
    }

    override fun onDisable() {
        // Guardar arenas antes de desactivar
        if (::arenaManager.isInitialized) {
            arenaManager.saveArenas()
        }
        
        // Finalizar todas las carreras activas
        if (::gameManager.isInitialized) {
            gameManager.finalizarTodasLasCarreras()
        }
        
        plugin.logger.info("✓ $gameName deshabilitado")
    }
    
    override fun isGameRunning(): Boolean {
        return if (::gameManager.isInitialized) {
            gameManager.getCarrerasActivas().isNotEmpty()
        } else {
            false
        }
    }
    
    override fun getActivePlayers(): List<Player> {
        return if (::gameManager.isInitialized) {
            gameManager.getCarrerasActivas().flatMap { it.getJugadores() }
        } else {
            emptyList()
        }
    }
    
    /**
     * Inicia el minijuego en modo torneo centralizado.
     * 
     * COMPORTAMIENTO:
     * Intenta iniciar una carrera en la primera arena válida disponible.
     * Si no hay arenas configuradas, registra un error.
     */
    override fun onTournamentStart(players: List<Player>) {
        plugin.logger.info("[$gameName] ═══ INICIO DE TORNEO ===")
        plugin.logger.info("[$gameName] ${players.size} jugadores disponibles")
        
        // Cargar arenas de manera segura bajo demanda
        try {
            arenaManager.loadArenas()
        } catch (e: Exception) {
            plugin.logger.severe("[$gameName] ✗ Error al cargar arenas: ${e.message}")
            return
        }
        
        // Buscar primera arena válida
        val arena = arenaManager.getAllArenas().firstOrNull { it.isValid() }
        
        if (arena == null) {
            plugin.logger.severe("[$gameName] ✗ No hay arenas configuradas válidas")
            plugin.logger.severe("[$gameName] Los administradores deben crear y configurar arenas con /carrera")
            return
        }
        
        // Iniciar carrera
        val carrera = gameManager.iniciarCarrera(arena, players)
        
        if (carrera != null) {
            plugin.logger.info("[$gameName] ✓ Carrera iniciada en '${arena.nombre}' con ${players.size} jugadores")
        } else {
            plugin.logger.severe("[$gameName] ✗ Error al iniciar la carrera")
        }
    }
    
    override fun getCommandExecutors(): Map<String, CommandExecutor> {
        return if (::arenaManager.isInitialized && ::gameManager.isInitialized) {
            mapOf(
                "carrera" to CarreraCommand(arenaManager, gameManager)
            )
        } else {
            emptyMap()
        }
    }
}
