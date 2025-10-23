package yo.spray.robarCabeza.services

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.util.GameTimer
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Display.Billboard
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import yo.spray.robarCabeza.game.Arena
import yo.spray.robarCabeza.game.GameState
import yo.spray.robarCabeza.game.RobarCabezaGame
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gestor central de la lógica del juego RobarCabeza.
 * 
 * Responsabilidades:
 * - Gestionar el ciclo de vida de las partidas
 * - Controlar el estado del juego
 * - Manejar la lógica de cabezas (dar, robar, remover)
 * - Gestionar temporizadores y countdowns
 * - Coordinar con el ScoreService para puntuación
 * - Coordinar con el VisualService para efectos visuales
 * - Gestionar arenas de juego
 */
class GameManager(
    private val plugin: Plugin,
    private val torneoPlugin: TorneoPlugin,
    scoreService: ScoreService,
    private val visualService: VisualService,
    private val arenaManager: ArenaManager
) {
    
    private val scoreService: ScoreService = scoreService
    
    /**
     * Partida activa actual.
     */
    private var activeGame: RobarCabezaGame? = null
    
    /**
     * Arena activa de la partida actual.
     */
    private var activeArena: Arena? = null
    
    /**
     * Configuración del juego.
     */
    private val tailCooldownSeconds = 3
    private val gameTimeSeconds = 120
    
    /**
     * Ubicaciones del juego (legacy - para compatibilidad).
     */
    var gameSpawn: Location? = null
    var lobbySpawn: Location? = null
    
    /**
     * Obtiene la partida activa.
     */
    fun getActiveGame(): RobarCabezaGame? = activeGame
    
    /**
     * Verifica si hay una partida en curso.
     */
    fun isGameRunning(): Boolean = activeGame?.state == GameState.IN_GAME
    
    /**
     * Obtiene la lista de jugadores activos.
     */
    fun getActivePlayers(): List<Player> {
        return activeGame?.players?.mapNotNull { Bukkit.getPlayer(it) } ?: emptyList()
    }
    
    /**
     * Verifica si un jugador está en una partida.
     */
    fun isPlayerInGame(player: Player): Boolean {
        return activeGame?.players?.contains(player.uniqueId) == true
    }
    
    /**
     * Obtiene la arena activa.
     */
    fun getActiveArena(): Arena? = activeArena
    
    /**
     * Añade un jugador al juego.
     */
    fun addPlayer(player: Player): Boolean {
        val game = activeGame
        
        // Si hay una partida en curso, no permitir unirse
        if (game != null && game.state != GameState.LOBBY) {
            player.sendMessage("${ChatColor.RED}¡El juego ya está en progreso!")
            return false
        }
        
        // Verificar que el spawn esté configurado
        if (gameSpawn == null) {
            player.sendMessage("${ChatColor.RED}¡El spawn del minijuego no ha sido establecido!")
            player.sendMessage("${ChatColor.YELLOW}Un administrador debe usar /robarcola setspawn primero")
            return false
        }
        
        // Crear nueva partida si no existe
        val currentGame = game ?: createNewGame()
        
        // Añadir jugador
        currentGame.players.add(player.uniqueId)
        player.teleport(gameSpawn!!)
        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20 * 10, 0))
        
        broadcastToGame("${ChatColor.AQUA}${player.name} se unió al juego! (${currentGame.getTotalPlayers()})")
        
        // Iniciar juego si hay suficientes jugadores
        if (currentGame.hasMinimumPlayers() && currentGame.state == GameState.LOBBY) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable { startGame() }, 100L)
        }
        
        return true
    }
    
    /**
     * Crea una nueva partida.
     */
    private fun createNewGame(): RobarCabezaGame {
        val game = RobarCabezaGame()
        activeGame = game
        return game
    }
    
    /**
     * Inicia la partida con una arena específica.
     */
    fun startGame(arena: Arena? = null) {
        val game = activeGame ?: return
        
        if (game.players.isEmpty() || game.state != GameState.LOBBY) {
            return
        }
        
        // Seleccionar arena (usar la proporcionada o una aleatoria)
        val selectedArena = arena ?: arenaManager.getRandomArena()
        
        if (selectedArena == null) {
            broadcastToGame("${ChatColor.RED}¡No hay arenas configuradas! Contacta a un administrador.")
            return
        }
        
        // Verificar que la arena tenga spawns configurados
        if (selectedArena.spawns.isEmpty()) {
            broadcastToGame("${ChatColor.RED}¡La arena no tiene spawns configurados!")
            return
        }
        
        activeArena = selectedArena
        game.state = GameState.COUNTDOWN
        
        // Reiniciar puntos de sesión
        scoreService.resetSessionScores()
        
        // Teletransportar jugadores a spawns aleatorios de la arena
        val playersList = game.players.mapNotNull { Bukkit.getPlayer(it) }
        playersList.forEach { player ->
            val randomSpawn = selectedArena.spawns.random()
            player.teleport(randomSpawn)
        }
        
        preStartCountdown {
            game.state = GameState.IN_GAME
            
            // Dar cabezas a múltiples jugadores aleatorios
            val headsCount = minOf(RobarCabezaScoreConfig.INITIAL_HEADS_COUNT, playersList.size)
            
            val selectedPlayers = game.players.toList().shuffled().take(headsCount)
            selectedPlayers.forEach { playerId ->
                Bukkit.getPlayer(playerId)?.let { giveHead(it) }
            }
            
            game.countdown = gameTimeSeconds
            broadcastToGame("${ChatColor.YELLOW}¡Comienza el juego! ${ChatColor.GOLD}$headsCount jugadores tienen cabeza!")
            broadcastToGame("${ChatColor.GRAY}Arena: ${ChatColor.WHITE}${selectedArena.name}")
            startCountdown()
            startPointsTicker()
        }
    }
    
    /**
     * Cuenta atrás pre-inicio (3, 2, 1, ¡Vamos!).
     */
    private fun preStartCountdown(onFinish: () -> Unit) {
        val game = activeGame ?: return
        val steps = arrayOf("3", "2", "1", "¡Vamos!")
        
        object : BukkitRunnable() {
            var i = 0
            override fun run() {
                if (i >= steps.size) {
                    cancel()
                    onFinish()
                    return
                }
                game.players.forEach { id ->
                    Bukkit.getPlayer(id)?.sendTitle("${ChatColor.GOLD}${steps[i]}", "", 0, 20, 0)
                }
                i++
            }
        }.runTaskTimer(plugin, 0L, 20L)
    }
    
    /**
     * Inicia el temporizador del juego.
     */
    private fun startCountdown() {
        val game = activeGame ?: return
        
        // Crear temporizador visual con BossBar
        val timer = GameTimer(
            plugin = torneoPlugin,
            durationInSeconds = gameTimeSeconds,
            title = "§c§l🎯 Robar Cola",
            onFinish = {
                // Cuando el tiempo se agota, finalizar el juego
                endGame()
            },
            onTick = { secondsLeft ->
                // Actualizar display del juego
                updateGameDisplay()
                
                // Reproducir sonido en los últimos 10 segundos
                if (secondsLeft <= 10 && secondsLeft > 0) {
                    game.players.forEach { uuid ->
                        Bukkit.getPlayer(uuid)?.playSound(
                            Bukkit.getPlayer(uuid)!!.location,
                            Sound.BLOCK_NOTE_BLOCK_PLING,
                            1.0f,
                            2.0f
                        )
                    }
                }
            }
        )
        
        // Añadir todos los jugadores al temporizador
        game.players.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                timer.addPlayer(player)
            }
        }
        
        // Guardar referencia y iniciar
        game.gameTimer = timer
        timer.start()
    }
    
    /**
     * Finaliza la partida.
     */
    fun endGame() {
        val game = activeGame ?: return
        
        game.state = GameState.FINISHED
        
        // Detener temporizador visual (BossBar) y ticker de puntos
        game.gameTimer?.stop()
        game.gameTimer = null
        game.pointsTickerTask?.cancel()
        game.pointsTickerTask = null
        
        // Obtener ranking final
        val ranking = scoreService.getSessionRanking()
        
        // Anunciar ganadores
        broadcastToGame("${ChatColor.GOLD}${ChatColor.BOLD}========== FINAL DEL JUEGO ==========")
        
        if (ranking.isNotEmpty()) {
            val top3 = ranking.take(3)
            top3.forEachIndexed { index, (playerId, points) ->
                val player = Bukkit.getPlayer(playerId)
                val position = index + 1
                val medal = when (position) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> ""
                }
                
                if (player != null) {
                    broadcastToGame("${ChatColor.YELLOW}$medal #$position: ${ChatColor.WHITE}${player.name} ${ChatColor.GRAY}($points puntos)")
                    
                    // Otorgar bonus por posición
                    scoreService.awardPointsForFinalPosition(player, position)
                    
                    // Efectos visuales para ganadores
                    when (position) {
                        1 -> celebrateWinner(player, "¡CAMPEÓN!")
                        2 -> celebrateWinner(player, "¡2do Lugar!")
                        3 -> celebrateWinner(player, "¡3er Lugar!")
                    }
                }
            }
        }
        
        broadcastToGame("${ChatColor.GOLD}${ChatColor.BOLD}====================================")
        
        // Otorgar puntos de participación a todos
        game.players.mapNotNull { Bukkit.getPlayer(it) }.forEach { player ->
            scoreService.awardPointsForParticipation(player)
        }
        
        // Teleportar jugadores de vuelta al lobby después de un delay
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            game.players.mapNotNull { Bukkit.getPlayer(it) }.forEach { player ->
                player.gameMode = GameMode.SURVIVAL
                player.removePotionEffect(PotionEffectType.GLOWING)
                teleportToLobby(player)
            }
            resetGame()
        }, 100L)
    }
    
    /**
     * Resetea el estado del juego.
     */
    private fun resetGame() {
        val game = activeGame ?: return
        
        // Limpiar todas las cabezas
        cleanupAllTails()
        
        // Limpiar la partida y arena
        activeGame = null
        activeArena = null
    }
    
    /**
     * Da la cabeza a un jugador.
     */
    fun giveHead(player: Player) {
        val game = activeGame ?: return
        
        // Añadir cabeza al jugador (no limpiar las demás, ahora hay múltiples cabezas)
        game.playersWithTail.add(player.uniqueId)
        
        // Equipar cabeza en el slot del casco
        visualService.equipHead(player)
        
        // Aplicar efecto de GLOW
        player.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, Int.MAX_VALUE, 0, false, false))
        
        // Sonido de nota cuando se obtiene la cabeza
        player.world.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f)
        
        player.sendMessage("${ChatColor.GREEN}¡Ahora tienes la cabeza del creador! ${ChatColor.YELLOW}Ganas ${RobarCabezaScoreConfig.POINTS_PER_SECOND} punto(s) por segundo")
    }
    
    /**
     * Roba la cabeza de un jugador a otro.
     */
    fun stealHead(victim: Player, attacker: Player) {
        val game = activeGame ?: return
        
        // Verificar cooldown de robo
        if (System.currentTimeMillis() - (game.tailCooldowns[attacker.uniqueId] ?: 0) < tailCooldownSeconds * 1000) {
            attacker.sendMessage("${ChatColor.RED}¡Espera antes de robar otra cabeza!")
            return
        }
        
        // Verificar que la víctima tenga cabeza
        if (!game.playersWithTail.contains(victim.uniqueId)) {
            return
        }
        
        // Verificar invulnerabilidad de la víctima
        if (game.isInvulnerable(victim.uniqueId, RobarCabezaScoreConfig.INVULNERABILITY_COOLDOWN_SECONDS)) {
            attacker.sendMessage("${ChatColor.YELLOW}¡${victim.name} es invulnerable!")
            return
        }
        
        // Actualizar cooldowns
        game.tailCooldowns[attacker.uniqueId] = System.currentTimeMillis()
        game.invulnerabilityCooldowns[attacker.uniqueId] = System.currentTimeMillis()
        
        // Remover cabeza de la víctima
        removeHead(victim)
        
        // Dar cabeza al atacante
        giveHead(attacker)
        
        // Otorgar puntos por robo
        scoreService.awardPointsForSteal(attacker)
        
        attacker.sendMessage("${ChatColor.GREEN}¡Le robaste la cabeza a ${victim.name}! ${ChatColor.GOLD}+${RobarCabezaScoreConfig.POINTS_STEAL_BONUS} puntos")
        victim.sendMessage("${ChatColor.RED}¡${attacker.name} te robó la cabeza!")
    }
    
    /**
     * Remueve la cabeza de un jugador.
     */
    private fun removeHead(player: Player) {
        val game = activeGame ?: return
        
        game.playersWithTail.remove(player.uniqueId)
        
        // Remover cabeza del slot del casco
        visualService.removeHead(player)
        
        // Limpiar referencias antiguas (por compatibilidad)
        game.playerTailDisplays.remove(player.uniqueId)?.remove()
        game.playerTails.remove(player.uniqueId)?.remove()
        
        // Remover efecto de GLOW
        player.removePotionEffect(PotionEffectType.GLOWING)
    }
    
    /**
     * Limpia todas las colas del juego.
     */
    private fun cleanupAllTails() {
        val game = activeGame ?: return
        
        game.playerTailDisplays.values.forEach { it.remove() }
        game.playerTails.values.forEach { it.remove() }
        game.playerTailDisplays.clear()
        game.playerTails.clear()
        game.playersWithTail.clear()
    }
    
    /**
     * Crea la visualización de cola para un jugador.
     */
    private fun createTailDisplay(player: Player) {
        val game = activeGame ?: return
        
        try {
            val display = player.world.spawn(player.location, ItemDisplay::class.java)
            display.itemStack = ItemStack(Material.PLAYER_HEAD)
            display.billboard = Billboard.FIXED
            game.playerTailDisplays[player.uniqueId] = display
            
            object : BukkitRunnable() {
                override fun run() {
                    if (!game.playersWithTail.contains(player.uniqueId) || !player.isOnline) {
                        display.remove()
                        cancel()
                        return
                    }
                    val base = player.location
                    val yawRad = Math.toRadians((base.yaw + 180).toDouble())
                    val loc = base.clone().add(-0.3 * sin(yawRad), 2.5, 0.3 * cos(yawRad))
                    val rotation = Quaternionf().rotateY(yawRad.toFloat())
                    display.transformation = Transformation(
                        Vector3f(loc.x.toFloat(), loc.y.toFloat(), loc.z.toFloat()),
                        rotation,
                        Vector3f(0.5f, 0.5f, 0.5f),
                        Quaternionf()
                    )
                }
            }.runTaskTimer(plugin, 0L, 1L)
        } catch (_: Throwable) {
            plugin.logger.warning("ItemDisplay no soportado, usando ArmorStand.")
            val stand = player.world.spawn(player.location, ArmorStand::class.java)
            stand.isVisible = false
            stand.equipment?.helmet = ItemStack(Material.PLAYER_HEAD)
            game.playerTails[player.uniqueId] = stand
        }
    }
    
    /**
     * Inicia el ticker que otorga puntos por segundo a los jugadores con cola.
     */
    private fun startPointsTicker() {
        val game = activeGame ?: return
        
        // Cancelar ticker anterior si existe
        game.pointsTickerTask?.cancel()
        
        // Crear nuevo ticker que se ejecuta cada segundo (20 ticks)
        game.pointsTickerTask = object : BukkitRunnable() {
            override fun run() {
                // Verificar que el juego siga activo
                if (game.state != GameState.IN_GAME) {
                    cancel()
                    return
                }
                
                // Otorgar puntos a cada jugador con cola
                game.playersWithTail.forEach { playerId ->
                    Bukkit.getPlayer(playerId)?.let { player ->
                        scoreService.awardPointsForHolding(player)
                        
                        // Mostrar partículas alrededor del jugador
                        player.world.spawnParticle(
                            Particle.VILLAGER_HAPPY,
                            player.location.add(0.0, 2.0, 0.0),
                            3,
                            0.3,
                            0.3,
                            0.3,
                            0.0
                        )
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L) // Ejecutar cada segundo
    }
    
    /**
     * Remueve un jugador del juego.
     */
    fun removePlayer(player: Player) {
        val game = activeGame ?: return
        
        game.players.remove(player.uniqueId)
        removeHead(player)
        
        // Si quedan menos de 2 jugadores, finalizar el juego
        if (game.state == GameState.IN_GAME && game.players.size < 2) {
            endGame()
        }
    }
    
    /**
     * Efectos visuales para jugador sin cola al final.
     */
    private fun explodePlayer(player: Player) {
        val loc = player.location
        player.world.spawnParticle(Particle.SMOKE_LARGE, loc, 10, 0.5, 0.5, 0.5, 0.05)
        player.world.playSound(loc, Sound.ENTITY_VILLAGER_NO, 1f, 0.8f)
    }
    
    /**
     * Efectos visuales para jugador ganador.
     */
    private fun celebrateWinner(player: Player, title: String = "¡VICTORIA!") {
        player.world.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f)
        player.world.spawnParticle(Particle.FIREWORKS_SPARK, player.location.add(0.0, 1.0, 0.0), 50, 0.5, 0.5, 0.5, 0.1)
        player.sendTitle("${ChatColor.GOLD}$title", "${ChatColor.YELLOW}¡Felicidades!", 10, 60, 10)
    }
    
    /**
     * Actualiza el display del juego (action bar).
     */
    private fun updateGameDisplay() {
        val game = activeGame ?: return
        
        game.players.forEach { uuid ->
            val player = Bukkit.getPlayer(uuid)
            if (player != null) {
                val sessionScore = scoreService.getSessionScore(uuid)
                val msg = if (game.playersWithTail.contains(uuid))
                    "${ChatColor.GREEN}¡Tienes cola! ${ChatColor.GOLD}+${RobarCabezaScoreConfig.POINTS_PER_SECOND}/s"
                else "${ChatColor.RED}Sin cola"
                player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    TextComponent("${ChatColor.GOLD}Puntos: ${ChatColor.WHITE}$sessionScore ${ChatColor.GRAY}| $msg")
                )
            }
        }
    }
    
    /**
     * Teletransporta un jugador al lobby.
     */
    fun teleportToLobby(player: Player) {
        val spawn = lobbySpawn ?: player.world.spawnLocation
        player.teleport(spawn)
        player.sendMessage("${ChatColor.GREEN}¡Regresaste al lobby!")
    }
    
    /**
     * Envía un mensaje a todos los jugadores del juego.
     */
    private fun broadcastToGame(msg: String) {
        val game = activeGame ?: return
        game.players.forEach { Bukkit.getPlayer(it)?.sendMessage(msg) }
    }
    
    /**
     * Encuentra el dueño de un ArmorStand de cola.
     */
    fun findTailOwner(armorStand: ArmorStand): Player? {
        val game = activeGame ?: return null
        return game.playerTails.entries.firstOrNull { it.value == armorStand }?.key?.let { Bukkit.getPlayer(it) }
    }
    
    /**
     * Limpia todos los recursos del juego.
     */
    fun clearAll() {
        activeGame?.let { game ->
            game.gameTimer?.stop()
            game.pointsTickerTask?.cancel()
            cleanupAllTails()
            
            // Limpiar efectos de los jugadores
            game.players.mapNotNull { Bukkit.getPlayer(it) }.forEach { player ->
                player.removePotionEffect(PotionEffectType.GLOWING)
            }
        }
        activeGame = null
        scoreService.resetSessionScores()
    }
}

