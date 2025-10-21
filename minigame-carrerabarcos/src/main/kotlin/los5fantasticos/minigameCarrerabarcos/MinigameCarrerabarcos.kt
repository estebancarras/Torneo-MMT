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
     * INTEGRACIÓN CON TORNEO:
     * Este método es llamado por TournamentFlowManager cuando un administrador
     * ejecuta /torneo start carrerabarcos. Todos los jugadores online son enviados
     * a la carrera simultáneamente.
     * 
     * COMPORTAMIENTO:
     * 1. Carga las arenas configuradas
     * 2. Busca la primera arena válida (con lobby, spawns, checkpoints y meta)
     * 3. Inicia la carrera para todos los jugadores
     * 4. Si no hay arenas válidas, registra un error detallado
     */
    override fun onTournamentStart(players: List<Player>) {
        plugin.logger.info("[$gameName] ═══════════════════════════════════")
        plugin.logger.info("[$gameName] INICIO DE TORNEO - CARRERA DE BARCOS")
        plugin.logger.info("[$gameName] ${players.size} jugadores disponibles")
        plugin.logger.info("[$gameName] ═══════════════════════════════════")
        
        // Validación: Verificar que hay jugadores
        if (players.isEmpty()) {
            plugin.logger.warning("[$gameName] ✗ No hay jugadores para iniciar la carrera")
            return
        }
        
        // Cargar arenas de manera segura bajo demanda
        try {
            arenaManager.loadArenas()
        } catch (e: Exception) {
            plugin.logger.severe("[$gameName] ✗ Error crítico al cargar arenas: ${e.message}")
            e.printStackTrace()
            
            // Notificar a los jugadores
            players.forEach { player ->
                player.sendMessage(
                    net.kyori.adventure.text.Component.text("✗ Error al cargar la arena de carreras", net.kyori.adventure.text.format.NamedTextColor.RED)
                )
            }
            return
        }
        
        // Buscar primera arena válida
        val arena = arenaManager.getAllArenas().firstOrNull { it.isValid() }
        
        if (arena == null) {
            plugin.logger.severe("[$gameName] ═══════════════════════════════════")
            plugin.logger.severe("[$gameName] ✗ ERROR: No hay arenas configuradas válidas")
            plugin.logger.severe("[$gameName] Una arena válida requiere:")
            plugin.logger.severe("[$gameName]   - Lobby configurado (/carrera setlobby)")
            plugin.logger.severe("[$gameName]   - Al menos 1 spawn (/carrera addspawn)")
            plugin.logger.severe("[$gameName]   - Al menos 1 checkpoint (/carrera addcheckpoint)")
            plugin.logger.severe("[$gameName]   - Meta configurada (/carrera setmeta)")
            plugin.logger.severe("[$gameName] ═══════════════════════════════════")
            
            // Notificar a los jugadores
            players.forEach { player ->
                player.sendMessage(
                    net.kyori.adventure.text.Component.text("✗ La arena de carreras no está configurada", net.kyori.adventure.text.format.NamedTextColor.RED)
                )
                player.sendMessage(
                    net.kyori.adventure.text.Component.text("Contacta a un administrador", net.kyori.adventure.text.format.NamedTextColor.GRAY)
                )
            }
            return
        }
        
        plugin.logger.info("[$gameName] Arena seleccionada: '${arena.nombre}'")
        plugin.logger.info("[$gameName]   - Spawns: ${arena.spawns.size}")
        plugin.logger.info("[$gameName]   - Checkpoints: ${arena.checkpoints.size}")
        plugin.logger.info("[$gameName]   - Capacidad máxima: ${arena.getMaxPlayers()} jugadores")
        
        // Iniciar carrera
        val carrera = gameManager.iniciarCarrera(arena, players)
        
        if (carrera != null) {
            plugin.logger.info("[$gameName] ✓ Carrera iniciada exitosamente")
            plugin.logger.info("[$gameName]   - Arena: '${arena.nombre}'")
            plugin.logger.info("[$gameName]   - Jugadores: ${players.size}")
            plugin.logger.info("[$gameName]   - Estado: ${carrera.estado}")
            plugin.logger.info("[$gameName] ═══════════════════════════════════")
        } else {
            plugin.logger.severe("[$gameName] ✗ Error al iniciar la carrera")
            plugin.logger.severe("[$gameName] Revisa los logs anteriores para más detalles")
            
            // Notificar a los jugadores
            players.forEach { player ->
                player.sendMessage(
                    net.kyori.adventure.text.Component.text("✗ Error al iniciar la carrera", net.kyori.adventure.text.format.NamedTextColor.RED)
                )
            }
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
