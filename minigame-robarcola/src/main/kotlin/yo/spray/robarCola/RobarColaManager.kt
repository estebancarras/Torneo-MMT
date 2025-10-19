package yo.spray.robarCola

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
import los5fantasticos.torneo.util.GameTimer
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.*
import org.bukkit.block.Sign
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.joml.Quaternionf
import org.joml.Vector3f
import org.bukkit.entity.Display.Billboard
import org.bukkit.util.Transformation
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Módulo: RobarCola
 * Adaptado para funcionar dentro del sistema TorneoMMT
 * Mantiene toda la lógica del minijuego original.
 */
@Suppress("unused")
class RobarColaManager(val torneoPlugin: TorneoPlugin) : MinigameModule, Listener {

    override val gameName = "RobarCola"
    override val version = "2.0"
    override val description = "Minijuego dinámico de robar colas entre jugadores"

    private lateinit var plugin: Plugin

    // Estado del juego
    private val playersInGame = mutableSetOf<UUID>()
    private val playersWithTail = mutableSetOf<UUID>()
    private val playerTails = mutableMapOf<UUID, ArmorStand>()
    private val playerTailDisplays = mutableMapOf<UUID, ItemDisplay>()
    private val tailCooldowns = mutableMapOf<UUID, Long>()
    private var gameRunning = false
    private var countdown = 0
    private var countdownTask: BukkitRunnable? = null
    private var gameTimer: GameTimer? = null

    // Configuración
    private var gameSpawn: Location? = null
    private var lobbySpawn: Location? = null
    private val signText = "[RobarCola]"
    private val lobbySignText = "[Lobby]"
    private val tailCooldownSeconds = 3
    private val gameTimeSeconds = 120

    // ===== Ciclo de vida del módulo =====

    override fun onEnable(plugin: Plugin) {
        this.plugin = plugin
        plugin.server.pluginManager.registerEvents(this, plugin)
        loadSpawnFromConfig()
        
        // Registrar comandos
        val commandExecutor = yo.spray.robarCola.commands.RobarColaCommands(this)
        torneoPlugin.getCommand("robarcola")?.setExecutor(commandExecutor)
        
        plugin.logger.info("Módulo RobarCola habilitado correctamente dentro de TorneoMMT.")
    }

    override fun onDisable() {
        cleanupAllTails()
        countdownTask?.cancel()
        gameTimer?.stop()
        plugin.logger.info("RobarCola deshabilitado correctamente.")
    }

    override fun getActivePlayers(): List<Player> {
        return playersInGame.mapNotNull { Bukkit.getPlayer(it) }
    }

    override fun isGameRunning(): Boolean = gameRunning
    
    /**
     * Inicia el minijuego en modo torneo centralizado.
     * Todos los jugadores son añadidos al juego automáticamente.
     */
    override fun onTournamentStart(players: List<Player>) {
        plugin.logger.info("[$gameName] ═══ INICIO DE TORNEO ═══")
        plugin.logger.info("[$gameName] Añadiendo ${players.size} jugadores al juego")
        
        // Añadir todos los jugadores al juego
        players.forEach { player ->
            try {
                joinGame(player)
                plugin.logger.info("[$gameName] Jugador ${player.name} añadido al juego")
            } catch (e: Exception) {
                plugin.logger.severe("[$gameName] Error añadiendo ${player.name}: ${e.message}")
                e.printStackTrace()
            }
        }
        
        plugin.logger.info("[$gameName] ✓ Torneo iniciado con ${players.size} jugadores")
    }

    // ===== Eventos =====

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        removePlayerFromGame(event.player)
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        if (!block.type.name.contains("SIGN")) return
        val sign = block.state as? Sign ?: return

        @Suppress("DEPRECATION")
        when (sign.getLine(0)) {
            signText -> joinGame(event.player)
            lobbySignText -> teleportToLobby(event.player)
        }
    }

    @EventHandler
    fun onAttack(event: EntityDamageByEntityEvent) {
        val victim = event.entity
        val attacker = event.damager as? Player ?: return

        // 1) Golpe a ArmorStand (robo directo)
        if (victim is ArmorStand) {
            val owner = findTailOwner(victim) ?: return
            if (System.currentTimeMillis() - (tailCooldowns[attacker.uniqueId] ?: 0) < tailCooldownSeconds * 1000) {
                attacker.sendMessage("${ChatColor.RED}¡Espera antes de robar otra cola!")
                return
            }
            if (playersWithTail.contains(owner.uniqueId)) {
                tailCooldowns[attacker.uniqueId] = System.currentTimeMillis()
                stealTail(owner, attacker)
            }
            return
        }

        // 2) Golpe por la espalda
        if (victim is Player &&
            playersInGame.contains(victim.uniqueId) &&
            playersInGame.contains(attacker.uniqueId) &&
            isBehindVictim(attacker, victim) &&
            playersWithTail.contains(victim.uniqueId)
        ) {
            tailCooldowns[attacker.uniqueId] = System.currentTimeMillis()
            stealTail(victim, attacker)
        }
    }

    // ===== Lógica del juego =====

    /**
     * Permite a un jugador unirse al juego.
     */
    fun joinGame(player: Player) {
        if (gameRunning) {
            player.sendMessage("${ChatColor.RED}¡El juego ya está en progreso!")
            return
        }
        if (gameSpawn == null) {
            player.sendMessage("${ChatColor.RED}¡El spawn del minijuego no ha sido establecido!")
            player.sendMessage("${ChatColor.YELLOW}Un administrador debe usar /robarcola setspawn primero")
            return
        }

        playersInGame.add(player.uniqueId)
        player.teleport(gameSpawn!!)
        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20 * 10, 0))
        broadcastToGame("${ChatColor.AQUA}${player.name} se unió al juego! (${playersInGame.size})")

        if (playersInGame.size >= 2 && !gameRunning) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable { startGame() }, 100L)
        }
    }

    private fun startGame() {
        if (playersInGame.isEmpty() || gameRunning) return
        gameRunning = true

        preStartCountdown {
            val randomPlayer = playersInGame.random()
            Bukkit.getPlayer(randomPlayer)?.let { giveTail(it) }

            countdown = gameTimeSeconds
            broadcastToGame("${ChatColor.YELLOW}¡Comienza el juego!")
            startCountdown()
        }
    }

    private fun preStartCountdown(onFinish: () -> Unit) {
        val steps = arrayOf("3", "2", "1", "¡Vamos!")
        object : BukkitRunnable() {
            var i = 0
            override fun run() {
                if (i >= steps.size) {
                    cancel()
                    onFinish()
                    return
                }
                playersInGame.forEach { id ->
                    Bukkit.getPlayer(id)?.sendTitle("${ChatColor.GOLD}${steps[i]}", "", 0, 20, 0)
                }
                i++
            }
        }.runTaskTimer(plugin, 0L, 20L)
    }

    private fun startCountdown() {
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
                    playersInGame.forEach { uuid ->
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
        playersInGame.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                timer.addPlayer(player)
            }
        }
        
        // Guardar referencia y iniciar
        gameTimer = timer
        timer.start()
    }

    private fun endGame() {
        gameRunning = false
        countdownTask?.cancel()
        
        // Detener temporizador visual (BossBar)
        gameTimer?.stop()
        gameTimer = null

        val winners = playersWithTail.mapNotNull { Bukkit.getPlayer(it) }
        val losers = playersInGame.filterNot { playersWithTail.contains(it) }.mapNotNull { Bukkit.getPlayer(it) }

        losers.forEach { explodePlayer(it) }
        winners.forEach { celebrateWinner(it) }

        if (winners.isNotEmpty()) {
            winners.forEach { torneoPlugin.torneoManager.recordGameWon(it, gameName) }
        }

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            playersInGame.mapNotNull { Bukkit.getPlayer(it) }.forEach { 
                // Restaurar modo de juego y teleportar
                it.gameMode = GameMode.SURVIVAL
                teleportToLobby(it) 
            }
            resetGame()
        }, 100L)
    }

    private fun resetGame() {
        playersInGame.clear()
        playersWithTail.clear()
        cleanupAllTails()
        tailCooldowns.clear()
        gameRunning = false
        countdown = 0
    }

    // ===== Utilidades del juego =====

    private fun explodePlayer(player: Player) {
        val loc = player.location
        player.world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 5)
        player.world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f)
        player.sendTitle("${ChatColor.RED}¡BOOM!", "${ChatColor.GRAY}¡No tenías cola!", 10, 40, 10)
        
        // Poner al jugador en modo espectador
        player.gameMode = GameMode.SPECTATOR
        player.sendMessage("${ChatColor.GRAY}Estás en modo espectador hasta el final del juego.")
    }

    private fun celebrateWinner(player: Player) {
        player.world.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f)
        player.sendTitle("${ChatColor.GOLD}¡VICTORIA!", "${ChatColor.YELLOW}¡Conservaste tu cola!", 10, 60, 10)
        torneoPlugin.torneoManager.addScore(player.uniqueId, gameName, 10, "Victoria")
    }

    private fun giveTail(player: Player) {
        playersWithTail.clear()
        cleanupAllTails()
        createTailDisplay(player)
        playersWithTail.add(player.uniqueId)
        
        // Sonido de nota cuando se obtiene la cola
        player.world.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f)
        
        broadcastToGame("${ChatColor.AQUA}${player.name} tiene la cola ahora!")
    }

    private fun stealTail(victim: Player, attacker: Player) {
        removeTail(victim)
        giveTail(attacker)
        attacker.sendMessage("${ChatColor.GREEN}¡Le robaste la cola a ${victim.name}!")
    }

    private fun removeTail(player: Player) {
        playersWithTail.remove(player.uniqueId)
        playerTailDisplays.remove(player.uniqueId)?.remove()
        playerTails.remove(player.uniqueId)?.remove()
    }

    private fun cleanupAllTails() {
        playerTailDisplays.values.forEach { it.remove() }
        playerTails.values.forEach { it.remove() }
        playerTailDisplays.clear()
        playerTails.clear()
        playersWithTail.clear()
    }

    private fun createTailDisplay(player: Player) {
        try {
            val display = player.world.spawn(player.location, ItemDisplay::class.java)
            display.itemStack = ItemStack(Material.CARROT_ON_A_STICK)
            display.billboard = Billboard.FIXED
            playerTailDisplays[player.uniqueId] = display

            object : BukkitRunnable() {
                override fun run() {
                    if (!playersWithTail.contains(player.uniqueId) || !player.isOnline) {
                        display.remove(); cancel(); return
                    }
                    val base = player.location
                    val yawRad = Math.toRadians((base.yaw + 180).toDouble())
                    val loc = base.clone().add(-0.3 * sin(yawRad), 1.0, 0.3 * cos(yawRad))
                    val rotation = Quaternionf().rotateY(yawRad.toFloat())
                    display.transformation = Transformation(Vector3f(loc.x.toFloat(), loc.y.toFloat(), loc.z.toFloat()), rotation, Vector3f(1f, 1f, 1f), Quaternionf())
                }
            }.runTaskTimer(plugin, 0L, 1L)
        } catch (_: Throwable) {
            plugin.logger.warning("ItemDisplay no soportado, usando ArmorStand.")
            val stand = player.world.spawn(player.location, ArmorStand::class.java)
            stand.isVisible = false
            stand.equipment?.helmet = ItemStack(Material.RED_WOOL)
            playerTails[player.uniqueId] = stand
        }
    }

    private fun isBehindVictim(attacker: Player, victim: Player): Boolean {
        val victimDir = victim.location.direction.clone().normalize()
        val toAttacker = attacker.location.toVector().subtract(victim.location.toVector()).normalize()
        val dot = victimDir.dot(toAttacker)
        return dot < -0.5 && attacker.location.distance(victim.location) <= 3.0
    }

    private fun findTailOwner(armorStand: ArmorStand): Player? {
        return playerTails.entries.firstOrNull { it.value == armorStand }?.key?.let { Bukkit.getPlayer(it) }
    }

    private fun updateGameDisplay() {
        playersInGame.forEach { uuid ->
            val player = Bukkit.getPlayer(uuid)
            if (player != null) {
                val msg = if (playersWithTail.contains(uuid))
                    "${ChatColor.GREEN}¡Tienes la cola!"
                else "${ChatColor.RED}Sin cola"
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("${ChatColor.GOLD}Tiempo: ${ChatColor.WHITE}$countdown s | $msg"))
            }
        }
    }

    private fun teleportToLobby(player: Player) {
        val spawn = lobbySpawn ?: player.world.spawnLocation
        player.teleport(spawn)
        player.sendMessage("${ChatColor.GREEN}¡Regresaste al lobby!")
    }

    private fun loadSpawnFromConfig() {
        val cfg = torneoPlugin.config
        val world = Bukkit.getWorlds().first()

        gameSpawn = if (cfg.contains("robarcola.gameSpawn.x")) {
            Location(
                world,
                cfg.getDouble("robarcola.gameSpawn.x"),
                cfg.getDouble("robarcola.gameSpawn.y"),
                cfg.getDouble("robarcola.gameSpawn.z")
            )
        } else {
            world.spawnLocation
        }

        lobbySpawn = if (cfg.contains("robarcola.lobbySpawn.x")) {
            Location(
                world,
                cfg.getDouble("robarcola.lobbySpawn.x"),
                cfg.getDouble("robarcola.lobbySpawn.y"),
                cfg.getDouble("robarcola.lobbySpawn.z")
            )
        } else {
            world.spawnLocation
        }
    }

    /**
     * Remueve a un jugador del juego.
     */
    fun removePlayerFromGame(player: Player) {
        playersInGame.remove(player.uniqueId)
        removeTail(player)
        if (gameRunning && playersInGame.size < 2) endGame()
    }

    private fun broadcastToGame(msg: String) {
        playersInGame.forEach { Bukkit.getPlayer(it)?.sendMessage(msg) }
    }

    // ===== Métodos públicos para comandos =====

    fun giveTailToPlayer(player: Player) {
        if (playersInGame.contains(player.uniqueId)) {
            giveTail(player)
        } else {
            player.sendMessage("${ChatColor.RED}¡No estás en el juego!")
        }
    }

    fun setGameSpawn(location: Location) {
        gameSpawn = location
        torneoPlugin.config.set("robarcola.gameSpawn.x", location.x)
        torneoPlugin.config.set("robarcola.gameSpawn.y", location.y)
        torneoPlugin.config.set("robarcola.gameSpawn.z", location.z)
        torneoPlugin.saveConfig()
    }

    fun setLobbySpawn(location: Location) {
        lobbySpawn = location
        torneoPlugin.config.set("robarcola.lobbySpawn.x", location.x)
        torneoPlugin.config.set("robarcola.lobbySpawn.y", location.y)
        torneoPlugin.config.set("robarcola.lobbySpawn.z", location.z)
        torneoPlugin.saveConfig()
    }

    fun startGameExternal() {
        if (!gameRunning && playersInGame.size >= 2) {
            startGame()
        }
    }

    fun endGameExternal() {
        if (gameRunning) {
            endGame()
        }
    }
}