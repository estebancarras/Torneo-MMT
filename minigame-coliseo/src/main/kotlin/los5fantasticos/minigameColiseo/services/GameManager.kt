package los5fantasticos.minigameColiseo.services

import los5fantasticos.minigameColiseo.game.ColiseoGame
import los5fantasticos.minigameColiseo.game.GameState
import los5fantasticos.minigameColiseo.game.TeamType
import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.util.GameTimer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Gestor central del juego Coliseo.
 * 
 * Responsabilidades:
 * - Gestionar el ciclo de vida de las partidas
 * - Balancear equipos dinámicamente
 * - Controlar condiciones de victoria
 * - Coordinar con otros servicios
 */
class GameManager(
    private val plugin: Plugin,
    private val torneoPlugin: TorneoPlugin,
    private val teamManager: TeamManager,
    private val kitService: KitService,
    private val arenaManager: ArenaManager,
    private val scoreService: ScoreService,
    private val gameDurationMinutes: Int
) {
    
    // Referencia al servicio de scoreboard (se inyecta después de la creación)
    private var coliseoScoreboardService: ColiseoScoreboardService? = null
    
    /**
     * Inyecta el servicio de scoreboard.
     * Necesario porque hay dependencia circular.
     */
    fun setScoreboardService(service: ColiseoScoreboardService) {
        this.coliseoScoreboardService = service
    }
    
    private var activeGame: ColiseoGame? = null
    
    /**
     * Obtiene la partida activa.
     */
    fun getActiveGame(): ColiseoGame? = activeGame
    
    /**
     * Verifica si hay una partida en curso.
     */
    fun isGameRunning(): Boolean = activeGame?.state == GameState.IN_GAME
    
    /**
     * Inicia una nueva partida con los jugadores dados.
     */
    fun startGame(players: List<Player>) {
        plugin.logger.info("[Coliseo] Iniciando partida con ${players.size} jugadores")
        
        // Verificar que haya suficientes jugadores
        if (players.size < 2) {
            plugin.logger.warning("[Coliseo] No hay suficientes jugadores (mínimo 2)")
            return
        }
        
        // Seleccionar arena
        val arena = arenaManager.getRandomArena()
        if (arena == null) {
            plugin.logger.severe("[Coliseo] No hay arenas configuradas")
            players.forEach { it.sendMessage("${ChatColor.RED}No hay arenas configuradas para el Coliseo") }
            return
        }
        
        // Crear nueva partida
        val game = ColiseoGame(arena = arena)
        activeGame = game
        
        // Reiniciar contador de kills
        coliseoScoreboardService?.resetKills()
        
        // Balancear y formar equipos
        balanceTeams(players, game)
        
        // Configurar equipos visuales
        teamManager.setupTeams(game)
        
        // Teletransportar jugadores
        teleportPlayersToSpawns(game)
        
        // Aplicar kits
        applyKits(game)
        
        // Mostrar scoreboard del Coliseo a todos los jugadores
        players.forEach { player ->
            coliseoScoreboardService?.showScoreboard(player)
        }
        
        // Iniciar cuenta regresiva
        startCountdown(game)
    }
    
    /**
     * Balancea los equipos según el ranking del torneo.
     */
    private fun balanceTeams(players: List<Player>, game: ColiseoGame) {
        val totalPlayers = players.size
        
        // Calcular tamaño del equipo élite
        val percentage = 0.25 // TODO: Leer de config
        val minElite = 1
        val maxElite = 10
        
        val calculatedEliteSize = (totalPlayers * percentage).roundToInt()
        val eliteSize = max(minElite, min(calculatedEliteSize, maxElite))
        
        plugin.logger.info("[Coliseo] Tamaño equipo Élite: $eliteSize de $totalPlayers jugadores")
        
        // Obtener ranking del torneo
        val ranking = torneoPlugin.torneoManager.getGlobalRanking()
        val rankedPlayers = players.sortedByDescending { player ->
            ranking.find { entry -> entry.uuid == player.uniqueId }?.totalPoints ?: 0
        }
        
        // Asignar mejores jugadores a Élite
        rankedPlayers.take(eliteSize).forEach { player ->
            teamManager.addToTeam(player, TeamType.ELITE, game)
        }
        
        // Resto a Horda
        rankedPlayers.drop(eliteSize).forEach { player ->
            teamManager.addToTeam(player, TeamType.HORDE, game)
        }
        
        plugin.logger.info("[Coliseo] Equipos formados - Élite: ${game.elitePlayers.size}, Horda: ${game.hordePlayers.size}")
    }
    
    /**
     * Teletransporta jugadores a sus spawns.
     */
    private fun teleportPlayersToSpawns(game: ColiseoGame) {
        val arena = game.arena ?: return
        
        // Teletransportar Élite
        game.elitePlayers.forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let { player ->
                arena.getRandomEliteSpawn()?.let { spawn ->
                    player.teleport(spawn)
                }
            }
        }
        
        // Teletransportar Horda
        game.hordePlayers.forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let { player ->
                arena.getRandomHordeSpawn()?.let { spawn ->
                    player.teleport(spawn)
                }
            }
        }
    }
    
    /**
     * Aplica los kits a los jugadores.
     */
    private fun applyKits(game: ColiseoGame) {
        game.elitePlayers.forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let { kitService.applyEliteKit(it) }
        }
        
        game.hordePlayers.forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let { kitService.applyHordeKit(it) }
        }
    }
    
    /**
     * Inicia la cuenta regresiva antes del juego.
     */
    private fun startCountdown(game: ColiseoGame) {
        game.state = GameState.STARTING
        
        var countdown = 5
        var taskId: Int? = null
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            if (countdown <= 0) {
                taskId?.let { Bukkit.getScheduler().cancelTask(it) }
                startGameTimer(game)
            } else {
                game.getAllPlayers().forEach { playerId ->
                    Bukkit.getPlayer(playerId)?.sendTitle(
                        "${ChatColor.GOLD}$countdown",
                        "${ChatColor.YELLOW}¡Prepárate!",
                        0, 20, 10
                    )
                }
                countdown--
            }
        }, 0L, 20L).taskId
    }
    
    /**
     * Inicia el temporizador del juego.
     */
    private fun startGameTimer(game: ColiseoGame) {
        game.state = GameState.IN_GAME
        
        // Anunciar inicio
        game.getAllPlayers().forEach { playerId ->
            Bukkit.getPlayer(playerId)?.sendTitle(
                "${ChatColor.RED}¡COMIENZA!",
                "",
                0, 40, 10
            )
        }
        
        // Crear temporizador
        val timer = GameTimer(
            plugin = torneoPlugin,
            durationInSeconds = gameDurationMinutes * 60,
            title = "§c§lCOLISEO",
            onFinish = {
                eliteWins(game)
            },
            onTick = { secondsLeft ->
                checkVictoryConditions(game)
            }
        )
        
        // Añadir jugadores al temporizador
        game.getAllPlayers().forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let { timer.addPlayer(it) }
        }
        
        game.gameTimer = timer
        timer.start()
    }
    
    /**
     * Verifica las condiciones de victoria.
     */
    private fun checkVictoryConditions(game: ColiseoGame) {
        if (game.state != GameState.IN_GAME) return
        
        // Si la Élite es eliminada, gana la Horda
        if (game.elitePlayers.isEmpty()) {
            hordeWins(game)
            return
        }
        
        // Si la Horda es eliminada, gana la Élite
        if (game.hordePlayers.isEmpty()) {
            eliteWins(game)
            return
        }
    }
    
    /**
     * Victoria de la Élite.
     */
    private fun eliteWins(game: ColiseoGame) {
        if (game.state == GameState.FINISHED) return
        game.state = GameState.FINISHED
        
        plugin.logger.info("[Coliseo] ¡LA ÉLITE HA GANADO!")
        
        // Anunciar victoria
        broadcastToGame(game, Component.text("═══════════════════════════════", NamedTextColor.GOLD))
        broadcastToGame(game, Component.text("¡LA ÉLITE HA GANADO!", NamedTextColor.GOLD))
        broadcastToGame(game, Component.text("═══════════════════════════════", NamedTextColor.GOLD))
        
        // Otorgar puntos
        val elitePlayers = teamManager.getElitePlayers(game)
        scoreService.awardVictoryPoints(elitePlayers, TeamType.ELITE)
        
        // Puntos de participación para la Horda
        teamManager.getHordePlayers(game).forEach { scoreService.awardParticipationPoints(it) }
        
        // Finalizar juego
        endGame(game)
    }
    
    /**
     * Victoria de la Horda.
     */
    private fun hordeWins(game: ColiseoGame) {
        if (game.state == GameState.FINISHED) return
        game.state = GameState.FINISHED
        
        plugin.logger.info("[Coliseo] ¡LA HORDA HA GANADO!")
        
        // Anunciar victoria
        broadcastToGame(game, Component.text("═══════════════════════════════", NamedTextColor.WHITE))
        broadcastToGame(game, Component.text("¡LA HORDA HA GANADO!", NamedTextColor.WHITE))
        broadcastToGame(game, Component.text("═══════════════════════════════", NamedTextColor.WHITE))
        
        // Otorgar puntos
        val hordePlayers = teamManager.getHordePlayers(game)
        scoreService.awardVictoryPoints(hordePlayers, TeamType.HORDE)
        
        // Puntos de participación para la Élite
        teamManager.getElitePlayers(game).forEach { scoreService.awardParticipationPoints(it) }
        
        // Finalizar juego
        endGame(game)
    }
    
    /**
     * Finaliza la partida.
     */
    fun endGame(game: ColiseoGame? = null) {
        val currentGame = game ?: activeGame ?: return
        currentGame.state = GameState.FINISHED
        
        // Detener temporizador
        currentGame.gameTimer?.stop()
        
        // Limpiar bloques colocados
        currentGame.placedBlocks.forEach { it.type = Material.AIR }
        currentGame.placedBlocks.clear()
        
        // Ocultar scoreboard del Coliseo y restaurar el global
        currentGame.getAllPlayers().forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let { player ->
                coliseoScoreboardService?.hideScoreboard(player)
            }
        }
        
        // Teletransportar jugadores al lobby
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            currentGame.getAllPlayers().forEach { playerId ->
                Bukkit.getPlayer(playerId)?.let { player ->
                    player.gameMode = GameMode.SURVIVAL
                    player.isGlowing = false
                    player.inventory.clear()
                    los5fantasticos.torneo.services.TournamentFlowManager.returnToLobby(player)
                }
            }
            
            // Limpiar equipos
            teamManager.cleanup()
            
            // Limpiar partida
            activeGame = null
        }, 100L)
    }
    
    /**
     * Envía un mensaje a todos los jugadores del juego.
     */
    private fun broadcastToGame(game: ColiseoGame, message: Component) {
        game.getAllPlayers().forEach { playerId ->
            Bukkit.getPlayer(playerId)?.sendMessage(message)
        }
    }
}
