package yo.spray.robarCola

import org.bukkit.*
import org.bukkit.block.Sign
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.joml.Quaternionf
import org.joml.Vector3f
import org.bukkit.entity.Display.Billboard
import org.bukkit.util.Transformation
import java.util.*

class RobarCola : JavaPlugin(), Listener {

    // --- Estado del juego ---
    private val playersInGame = mutableSetOf<UUID>()
    private val playersWithTail = mutableSetOf<UUID>()
    private val playerTails = mutableMapOf<UUID, ArmorStand>() // respaldo legacy
    private val playerTailDisplays = mutableMapOf<UUID, ItemDisplay>() // nuevo: ItemDisplay por jugador
    private val tailCooldowns = mutableMapOf<UUID, Long>() // cooldown para robar
    private var gameSpawn: Location? = null
    private var lobbySpawn: Location? = null
    private val signText = "[RobarCola]"
    private val lobbySignText = "[Lobby]"

    // Variables del minijuego
    private var gameRunning = false
    private var countdown = 0
    private var countdownTask: BukkitRunnable? = null

    // Configuración
    private val tailCooldownSeconds = 3 // segundos de cooldown
    private val gameTimeSeconds = 120 // duración del juego

    override fun onEnable() {
        logger.info("RobarCola plugin habilitado!")
        server.pluginManager.registerEvents(this, this)

        // Cargar spawn desde config
        loadSpawnFromConfig()

        // Comandos simples
        getCommand("giveTail")?.setExecutor { sender, _, _, args ->
            if (args.isNotEmpty()) {
                val target = Bukkit.getPlayer(args[0])
                if (target != null) {
                    giveTail(target)
                    sender.sendMessage("${ChatColor.GREEN}Le diste una cola a ${target.name}")
                } else {
                    sender.sendMessage("${ChatColor.RED}Jugador no encontrado")
                }
            } else {
                sender.sendMessage("${ChatColor.RED}Uso: /giveCola <jugador>")
            }
            true
        }

        getCommand("setGameSpawn")?.setExecutor { sender, _, _, _ ->
            if (sender is Player) {
                setGameSpawn(sender.location)
                sender.sendMessage("${ChatColor.GREEN}Spawn del minijuego establecido!")
            }
            true
        }

        getCommand("setLobby")?.setExecutor { sender, _, _, _ ->
            if (sender is Player) {
                setLobbySpawn(sender.location)
                sender.sendMessage("${ChatColor.GREEN}Spawn del lobby establecido!")
            }
            true
        }

        getCommand("startGame")?.setExecutor { sender, _, _, _ ->
            if (!gameRunning) {
                startGame()
                sender.sendMessage("${ChatColor.GREEN}¡Minijuego iniciado! (pre-cuenta)")
            } else {
                sender.sendMessage("${ChatColor.RED}¡El juego ya está en marcha!")
            }
            true
        }

        getCommand("stopGame")?.setExecutor { sender, _, _, _ ->
            if (gameRunning) {
                endGame()
                sender.sendMessage("${ChatColor.RED}¡Minijuego detenido!")
            } else {
                sender.sendMessage("${ChatColor.YELLOW}El juego no está activo.")
            }
            true
        }
    }

    override fun onDisable() {
        logger.info("RobarCola plugin deshabilitado!")
        cleanupAllTails()
        countdownTask?.cancel()
    }

    // ---- Eventos básicos ----
    @EventHandler
    fun onJoin(@Suppress("UNUSED_PARAMETER") event: PlayerJoinEvent) {
        // Unirse al juego mediante carteles (handler en interact)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        removePlayerFromGame(event.player)
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        if (isSign(block.type)) {
            val sign = block.state as? Sign ?: return
            @Suppress("DEPRECATION")
            when (sign.lines[0]) {
                signText -> joinGame(event.player)
                lobbySignText -> teleportToLobby(event.player)
            }
        }
    }

    // ---- Ataques: robo por ArmorStand o por espalda al jugador ----
    @EventHandler
    fun onAttack(event: EntityDamageByEntityEvent) {
        val victim = event.entity

        // 1) Si atacan un ItemDisplay/ArmorStand (legacy)
        if (victim is ArmorStand && event.damager is Player) {
            val attacker = event.damager as Player
            val attackerUuid = attacker.uniqueId
            val now = System.currentTimeMillis()
            val last = tailCooldowns[attackerUuid] ?: 0
            if (now - last < tailCooldownSeconds * 1000) {
                val remaining = tailCooldownSeconds - ((now - last) / 1000)
                attacker.sendMessage("${ChatColor.RED}¡Debes esperar ${remaining}s antes de robar otra cola!")
                event.isCancelled = true
                return
            }
            val owner = findTailOwner(victim)
            if (owner != null && playersWithTail.contains(owner.uniqueId)) {
                tailCooldowns[attackerUuid] = now
                stealTail(owner, attacker)
                event.isCancelled = true
                return
            }
        }

        // 2) Robar golpeando la ESPALDA del jugador (hitbox jugador)
        if (victim is Player && event.damager is Player) {
            val attacker = event.damager as Player
            if (playersInGame.contains(victim.uniqueId) && playersInGame.contains(attacker.uniqueId)) {
                val attackerUuid = attacker.uniqueId
                val now = System.currentTimeMillis()
                val last = tailCooldowns[attackerUuid] ?: 0
                if (now - last < tailCooldownSeconds * 1000) {
                    val remaining = tailCooldownSeconds - ((now - last) / 1000)
                    attacker.sendMessage("${ChatColor.RED}¡Debes esperar ${remaining}s antes de robar otra cola!")
                    return
                }

                if (playersWithTail.contains(victim.uniqueId) && isBehindVictim(attacker, victim)) {
                    tailCooldowns[attackerUuid] = now
                    stealTail(victim, attacker)
                    event.isCancelled = true // evitar daño si quieres
                    return
                }
            }
        }

        // Permitir PvP normal fuera de condiciones especificas (si se requiere)
    }

    // ---- Helpers ----

    private fun isSign(material: Material): Boolean {
        return material.name.contains("SIGN")
    }

    private fun isBehindVictim(attacker: Player, victim: Player): Boolean {
        // Normaliza vectores y usa dot product para saber si el atacante está detrás
        val victimDir = victim.location.direction.clone().normalize()
        val toAttackerVec = attacker.location.toVector().subtract(victim.location.toVector()).normalize()
        val dot = victimDir.dot(toAttackerVec)
        val distance = attacker.location.distance(victim.location)
        // dot < -0.5 => aproximadamente detrás (ajustable). Limitar distancia para evitar robos desde muy lejos.
        return dot < -0.5 && distance <= 3.0
    }

    private fun joinGame(player: Player) {
        if (gameRunning) {
            player.sendMessage("${ChatColor.RED}¡El juego ya esta en progreso!")
            return
        }
        if (gameSpawn == null) {
            player.sendMessage("${ChatColor.RED}¡El spawn del minijuego no ha sido establecido!")
            return
        }

        playersInGame.add(player.uniqueId)
        player.teleport(gameSpawn!!)
        player.sendMessage("${ChatColor.GREEN}¡Te uniste al minijuego de Robar Cola!")
        player.sendMessage("${ChatColor.YELLOW}Esperando que el juego comience...")
        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20 * 10, 0))

        broadcastToGame("${ChatColor.AQUA}${player.name} se unió al juego! (${playersInGame.size} jugadores)")

        // Auto start si hay >= 2 jugadores
        if (playersInGame.size >= 2 && !gameRunning) {
            Bukkit.getScheduler().runTaskLater(this, Runnable {
                if (playersInGame.size >= 2 && !gameRunning) {
                    startGame()
                }
            }, 100L) // espera 5s (100 ticks)
        }
    }

    // ---- Inicio del juego con pre-cuenta (3,2,1,Vamos) ----
    private fun startGame() {
        if (playersInGame.isEmpty() || gameRunning) return
        gameRunning = true

        preStartCountdown {
            // Dar cola inicial
            val randomPlayer = playersInGame.random()
            Bukkit.getPlayer(randomPlayer)?.let { giveTail(it) }

            // Inicio real
            countdown = gameTimeSeconds
            broadcastToGame("${ChatColor.RED}¡Tienes $countdown segundos para conseguir la cola!")
            broadcastToGame("${ChatColor.YELLOW}¡Solo quien tenga la cola al final sobrevivirá!")
            startCountdown()
        }
    }

    private fun preStartCountdown(onFinish: () -> Unit) {
        val steps = arrayOf("3", "2", "1", "¡Vamos (●'◡'●)!")
        object : BukkitRunnable() {
            var idx = 0
            override fun run() {
                if (idx >= steps.size) {
                    cancel()
                    spawnConfettiForAll()
                    onFinish()
                    return
                }
                val text = steps[idx]
                playersInGame.forEach { uuid ->
                    Bukkit.getPlayer(uuid)?.let { p ->
                        p.sendTitle("${ChatColor.GOLD}$text", "", 0, 20, 0)
                        p.playSound(p.location, Sound.UI_BUTTON_CLICK, 1f, 1.2f)
                    }
                }
                idx++
            }
        }.runTaskTimer(this, 0L, 20L)
    }

    private fun spawnConfettiForAll() {
        playersInGame.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { p ->
                val loc = p.location.clone().add(0.0, 1.5, 0.0)
                p.world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 60, 1.0, 1.0, 1.0, 0.1)
                p.world.spawnParticle(Particle.CRIT, loc, 40, 1.0, 1.0, 1.0, 0.05)
                p.world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1.2f)
                p.world.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
            }
        }
    }

    // ---- Countdown principal del juego ----
    private fun startCountdown() {
        countdownTask = object : BukkitRunnable() {
            override fun run() {
                if (!gameRunning) {
                    cancel()
                    return
                }
                countdown--
                when (countdown) {
                    30, 15, 10, 5, 4, 3, 2, 1 -> {
                        broadcastToGame("${ChatColor.RED}¡$countdown segundos restantes!")
                        playersInGame.forEach { uuid ->
                            Bukkit.getPlayer(uuid)?.playSound(
                                Bukkit.getPlayer(uuid)!!.location,
                                Sound.BLOCK_NOTE_BLOCK_PLING,
                                1.0f,
                                1.0f
                            )
                        }
                    }
                    0 -> {
                        endGame()
                        cancel()
                        return
                    }
                }
                updateGameDisplay()
            }
        }
        countdownTask?.runTaskTimer(this, 0L, 20L)
    }

    // ---- Final del juego: explotados -> SPECTATOR; luego reset y teleport al lobby ----
    private fun endGame() {
        gameRunning = false
        countdownTask?.cancel()

        val winners = mutableListOf<Player>()
        val losers = mutableListOf<Player>()

        playersInGame.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { p ->
                if (playersWithTail.contains(uuid)) winners.add(p) else losers.add(p)
            }
        }

        losers.forEach { player ->
            explodePlayer(player)
            // Poner espectador
            player.gameMode = GameMode.SPECTATOR
        }

        winners.forEach { celebrateWinner(it) }

        if (winners.isNotEmpty()) {
            val winnerNames = winners.joinToString(", ") { it.name }
            broadcastToGame("${ChatColor.GOLD}¡Ganador(es): $winnerNames!")
        } else {
            broadcastToGame("${ChatColor.RED}¡No hay ganadores!")
        }

        // 5s después, llevar a todos al lobby y reset
        Bukkit.getScheduler().runTaskLater(this, Runnable {
            playersInGame.forEach { uuid ->
                Bukkit.getPlayer(uuid)?.let { gp ->
                    cleanPlayerEffects(gp)
                    if (gp.gameMode == GameMode.SPECTATOR) gp.gameMode = GameMode.SURVIVAL
                    teleportToLobby(gp)
                }
            }
            resetGame()
        }, 100L)
    }

    private fun explodePlayer(player: Player) {
        val loc = player.location
        player.world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 5)
        player.world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f)
        player.sendTitle("${ChatColor.RED}¡BOOM!", "${ChatColor.GRAY}¡No tenías cola!", 10, 40, 10)
        player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 60, 0))
        player.playSound(loc, Sound.ENTITY_WITHER_DEATH, 0.5f, 0.5f)
    }

    private fun celebrateWinner(player: Player) {
        val loc = player.location
        player.world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 20)
        player.world.spawnParticle(Particle.HEART, loc, 10)
        player.world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f)
        player.world.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
        player.sendTitle("${ChatColor.GOLD}¡VICTORIA!", "${ChatColor.YELLOW}¡Conservaste tu cola!", 10, 60, 10)
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 100, 1))
        player.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, 100, 0))
    }

    private fun resetGame() {
        cleanupAllTails()
        tailCooldowns.clear()
        playersInGame.clear()
        playersWithTail.clear()
        broadcastServer("${ChatColor.GREEN}¡El minijuego de Robar Cola ha terminado!")
    }

    private fun removePlayerFromGame(player: Player) {
        val uuid = player.uniqueId
        playersInGame.remove(uuid)
        if (playersWithTail.contains(uuid)) removeTail(player)
        if (gameRunning && playersInGame.size < 2) {
            broadcastToGame("${ChatColor.RED}¡No hay suficientes jugadores! Terminando juego...")
            endGame()
        }
    }

    private fun findTailOwner(armorStand: ArmorStand): Player? {
        for ((playerUuid, tailArmorStand) in playerTails) {
            if (tailArmorStand == armorStand) {
                return Bukkit.getPlayer(playerUuid)
            }
        }
        return null
    }

    // ---- Sistema de colas: usar ItemDisplay (más visible) con respaldo ArmorStand ----

    private fun giveTail(player: Player) {
        // Limpiar colas anteriores (sólo 1 dueño a la vez en el juego)
        playersWithTail.clear()
        cleanupAllTails()

        // Quitar displays legados en jugadores en juego
        playersInGame.forEach { uuid -> Bukkit.getPlayer(uuid)?.let { removeTailDisplay(it) } }

        playersWithTail.add(player.uniqueId)

        // Crear display moderno
        createTailDisplay(player)

        // Sonidos y efectos
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.8f)
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.5f)
        player.world.spawnParticle(Particle.VILLAGER_HAPPY, player.location.add(0.0, 1.0, 0.0), 8)
        player.sendMessage("${ChatColor.GOLD}¡Ahora tienes la cola!")
        player.sendTitle("${ChatColor.GOLD}¡COLA OBTENIDA!", "${ChatColor.YELLOW}¡Protégela!", 10, 30, 10)

        if (gameRunning) broadcastToGame("${ChatColor.AQUA}¡${player.name} ahora tiene la cola!")
    }

    private fun stealTail(victim: Player, attacker: Player) {
        removeTail(victim)
        giveTail(attacker)
        victim.sendMessage("${ChatColor.RED}¡${attacker.name} te robó la cola!")
        attacker.sendMessage("${ChatColor.GREEN}¡Le robaste la cola a ${victim.name}!")
        victim.playSound(victim.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f)
    }

    // --- ItemDisplay: crea una "cola" visible pegada al jugador ---
    private fun tailItemStack(): ItemStack {
        // El material base puede ser cualquiera (ej: CARROT_ON_A_STICK o STICK)
        val it = ItemStack(Material.CARROT_ON_A_STICK)
        val meta = it.itemMeta
        meta?.setDisplayName("${ChatColor.GOLD}Cola")

        // Aquí conectas con tu modelo cola.json
        // Asegúrate de que el override en stick.json o carrot_on_a_stick.json tiene este CustomModelData
        meta?.setCustomModelData(9001)

        it.itemMeta = meta
        return it
    }

    private fun createTailDisplay(player: Player) {
        removeTailDisplay(player)

        // Spawn ItemDisplay (Paper/1.20+). Si falla en tu servidor, uso ArmorStand en alternativa.
        try {
            val display = player.world.spawn(player.location, ItemDisplay::class.java)
            display.itemStack = tailItemStack()
            display.isPersistent = false
            display.billboard = Billboard.FIXED
            display.interpolationDuration = 1

            playerTailDisplays[player.uniqueId] = display

            object : BukkitRunnable() {
                override fun run() {
                    if (!playersWithTail.contains(player.uniqueId) || !player.isOnline) {
                        display.remove()
                        cancel()
                        return
                    }
                    val base = player.location
                    val dir = base.direction.clone().normalize()
                    val behind = dir.multiply(-0.35)
                    val loc = base.clone().add(behind).add(0.0, 0.9, 0.0)

                    val yawRad = Math.toRadians((base.yaw + 180).toDouble()).toFloat()
                    val rot = Quaternionf().rotateY(yawRad)

                    val translation = Vector3f(loc.x.toFloat(), loc.y.toFloat(), loc.z.toFloat())
                    val scale = Vector3f(1.0f, 1.0f, 1.0f) // ajusta si tu modelo necesita escala
                    display.transformation = Transformation(translation, rot, scale, Quaternionf())
                }
            }.runTaskTimer(this, 0L, 1L)
        } catch (_: Throwable) {
            // Si ItemDisplay no está soportado: fallback a ArmorStand
            logger.warning("ItemDisplay no soportado en este servidor; usando ArmorStand fallback.")
            createTailArmorStand(player)
        }
    }

    private fun removeTailDisplay(player: Player) {
        playerTailDisplays.remove(player.uniqueId)?.remove()
    }

    // --- ArmorStand legacy (fallback) ---
    private fun createTailArmorStand(player: Player) {
        removeTailArmorStand(player)
        val loc = player.location.clone()
        val behind = player.location.direction.multiply(-0.3)
        loc.add(behind)
        loc.y += 0.8
        val armorStand = player.world.spawn(loc, ArmorStand::class.java)
        armorStand.isVisible = false
        armorStand.isSmall = true
        armorStand.setGravity(false)
        armorStand.isInvulnerable = false
        armorStand.customName = "${ChatColor.GOLD}Cola"
        armorStand.isCustomNameVisible = false
        armorStand.setArms(false)
        armorStand.setBasePlate(false)
        val banner = ItemStack(Material.RED_BANNER)
        armorStand.equipment?.setItemInMainHand(banner)
        playerTails[player.uniqueId] = armorStand

        object : BukkitRunnable() {
            override fun run() {
                if (!playersWithTail.contains(player.uniqueId) || !player.isOnline) {
                    armorStand.remove()
                    cancel()
                    return
                }
                val newLoc = player.location.clone()
                val behindVector = player.location.direction.multiply(-0.3)
                newLoc.add(behindVector)
                newLoc.y += 0.8
                newLoc.yaw = player.location.yaw + 180
                newLoc.pitch = 0.0f
                armorStand.teleport(newLoc)
            }
        }.runTaskTimer(this, 0L, 1L)
    }

    private fun removeTailArmorStand(player: Player) {
        playerTails[player.uniqueId]?.remove()
        playerTails.remove(player.uniqueId)
    }

    private fun removeTail(player: Player) {
        playersWithTail.remove(player.uniqueId)
        removeTailArmorStand(player)
        removeTailDisplay(player)
    }

    private fun cleanupAllTails() {
        playerTails.values.forEach { it.remove() }
        playerTails.clear()
        playerTailDisplays.values.forEach { it.remove() }
        playerTailDisplays.clear()
        playersWithTail.clear()
    }

    // ---- Visual/UX updates ----
    private fun updateGameDisplay() {
        playersInGame.forEach { uuid ->
            val player = Bukkit.getPlayer(uuid)
            if (player != null) {
                val hasScore = if (playersWithTail.contains(uuid))
                    "${ChatColor.GREEN}¡Tienes la cola!"
                else
                    "${ChatColor.RED}Sin cola"
                player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    TextComponent("${ChatColor.GOLD}Tiempo: ${ChatColor.WHITE}$countdown${ChatColor.GRAY}s | $hasScore")
                )
            }
        }
    }

    private fun broadcastToGame(message: String) {
        playersInGame.forEach { uuid -> Bukkit.getPlayer(uuid)?.sendMessage(message) }
    }

    private fun broadcastServer(message: String) {
        Bukkit.broadcastMessage(message)
    }

    // ---- Spawns en config ----
    private fun setGameSpawn(location: Location) {
        gameSpawn = location.clone()
        config.set("gameSpawn.world", location.world?.name)
        config.set("gameSpawn.x", location.x)
        config.set("gameSpawn.y", location.y)
        config.set("gameSpawn.z", location.z)
        config.set("gameSpawn.yaw", location.yaw)
        config.set("gameSpawn.pitch", location.pitch)
        saveConfig()
    }

    private fun setLobbySpawn(location: Location) {
        lobbySpawn = location.clone()
        config.set("lobbySpawn.world", location.world?.name)
        config.set("lobbySpawn.x", location.x)
        config.set("lobbySpawn.y", location.y)
        config.set("lobbySpawn.z", location.z)
        config.set("lobbySpawn.yaw", location.yaw)
        config.set("lobbySpawn.pitch", location.pitch)
        saveConfig()
    }

    private fun loadSpawnFromConfig() {
        if (config.contains("gameSpawn.world")) {
            val worldName = config.getString("gameSpawn.world")
            val world = Bukkit.getWorld(worldName!!)
            if (world != null) {
                gameSpawn = Location(
                    world,
                    config.getDouble("gameSpawn.x"),
                    config.getDouble("gameSpawn.y"),
                    config.getDouble("gameSpawn.z"),
                    config.getDouble("gameSpawn.yaw").toFloat(),
                    config.getDouble("gameSpawn.pitch").toFloat()
                )
            }
        }
        if (config.contains("lobbySpawn.world")) {
            val worldName = config.getString("lobbySpawn.world")
            val world = Bukkit.getWorld(worldName!!)
            if (world != null) {
                lobbySpawn = Location(
                    world,
                    config.getDouble("lobbySpawn.x"),
                    config.getDouble("lobbySpawn.y"),
                    config.getDouble("lobbySpawn.z"),
                    config.getDouble("lobbySpawn.yaw").toFloat(),
                    config.getDouble("lobbySpawn.pitch").toFloat()
                )
            }
        }
    }

    private fun cleanPlayerEffects(player: Player) {
        player.removePotionEffect(PotionEffectType.SPEED)
        player.removePotionEffect(PotionEffectType.GLOWING)
        player.removePotionEffect(PotionEffectType.REGENERATION)
        player.removePotionEffect(PotionEffectType.BLINDNESS)
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(""))
    }

    private fun teleportToLobby(player: Player) {
        // Si el juego está activo, remover de la lista de juego
        if (gameRunning) removePlayerFromGame(player)
        if (lobbySpawn != null) {
            player.teleport(lobbySpawn!!)
            player.sendMessage("${ChatColor.GREEN}¡Regresaste al lobby!")
        } else {
            player.teleport(player.world.spawnLocation)
            player.sendMessage("${ChatColor.YELLOW}¡Regresaste al spawn del mundo!")
        }
    }
}
