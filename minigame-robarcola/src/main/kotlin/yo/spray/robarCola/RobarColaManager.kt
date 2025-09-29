package yo.spray.robarcola

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
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
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.joml.Quaternionf
import org.joml.Vector3f
import org.bukkit.entity.Display.Billboard
import org.bukkit.util.Transformation
import yo.spray.robarcola.commands.RobarColaCommands
import java.util.*

class RobarColaManager : MinigameModule, Listener {
    
    override val name = "RobarCola"
    override val version = "1.0"
    override val description = "Minijuego de robar colas"
    
    private lateinit var plugin: Plugin
    private lateinit var torneoPlugin: TorneoPlugin
    private lateinit var commands: RobarColaCommands
    
    // Estado del juego
    private val playersInGame = mutableSetOf<UUID>()
    private val playersWithTail = mutableSetOf<UUID>()
    private val playerTails = mutableMapOf<UUID, ArmorStand>()
    private val playerScores = mutableMapOf<UUID, Int>()
    private var gameRunning = false
    private var gameTask: BukkitRunnable? = null
    private var gameTimeRemaining = 0
    
    // Configuración
    private var gameSpawn: Location? = null
    private var lobbySpawn: Location? = null
    private val gameDuration = 300 // 5 minutos en segundos
    
    override fun onEnable(plugin: Plugin) {
        this.plugin = plugin
        this.torneoPlugin = TorneoPlugin.instance
        this.commands = RobarColaCommands(this)
        
        // Registrar eventos
        plugin.server.pluginManager.registerEvents(this, plugin)
        
        // Registrar comandos (se registran desde TorneoPlugin)
        torneoPlugin.getCommand("givetail")?.setExecutor(commands)
        torneoPlugin.getCommand("setgamespawn")?.setExecutor(commands)
        torneoPlugin.getCommand("setlobby")?.setExecutor(commands)
        torneoPlugin.getCommand("startgame")?.setExecutor(commands)
        torneoPlugin.getCommand("stopgame")?.setExecutor(commands)
        
        // Cargar configuración
        loadConfiguration()
        
        // Iniciar task de actualización de colas
        startTailUpdateTask()
        
        plugin.logger.info("$name v$version habilitado")
    }
    
    override fun onDisable() {
        if (gameRunning) {
            endGame()
        }
        removeAllTails()
        plugin.logger.info("$name deshabilitado")
    }
    
    override fun isGameRunning(): Boolean = gameRunning
    
    override fun getActivePlayers(): List<Player> {
        return playersInGame.mapNotNull { Bukkit.getPlayer(it) }
    }
    
    override fun awardPoints(player: Player, points: Int, reason: String) {
        torneoPlugin.torneoManager.addPoints(player, name, points, reason)
    }
    
    // Métodos públicos para comandos
    fun giveTailExternal(player: Player) {
        if (!playersWithTail.contains(player.uniqueId)) {
            giveTail(player)
        }
    }
    
    fun setGameSpawnExternal(location: Location) {
        gameSpawn = location
        saveConfiguration()
    }
    
    fun setLobbySpawnExternal(location: Location) {
        lobbySpawn = location
        saveConfiguration()
    }
    
    fun startGameExternal() {
        if (!gameRunning) {
            startGame()
        }
    }
    
    fun endGameExternal() {
        if (gameRunning) {
            endGame()
        }
    }
    
    // Lógica del juego
    private fun startGame() {
        if (gameSpawn == null) {
            plugin.logger.warning("No se ha establecido el spawn del juego")
            return
        }
        
        gameRunning = true
        gameTimeRemaining = gameDuration
        playersInGame.clear()
        playersWithTail.clear()
        playerScores.clear()
        
        // Agregar jugadores online al juego
        Bukkit.getOnlinePlayers().forEach { player ->
            playersInGame.add(player.uniqueId)
            player.teleport(gameSpawn!!)
            player.gameMode = GameMode.ADVENTURE
        }
        
        // Dar cola inicial a jugadores aleatorios
        val initialTailCount = (playersInGame.size * 0.3).toInt().coerceAtLeast(1)
        playersInGame.shuffled().take(initialTailCount).forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { giveTail(it) }
        }
        
        // Iniciar timer del juego
        startGameTimer()
        
        Bukkit.broadcastMessage("${ChatColor.GREEN}${ChatColor.BOLD}¡El juego de Robar Cola ha comenzado!")
        Bukkit.broadcastMessage("${ChatColor.YELLOW}Duración: ${gameDuration / 60} minutos")
    }
    
    private fun endGame() {
        gameRunning = false
        gameTask?.cancel()
        gameTask = null
        
        // Calcular ganadores y otorgar puntos
        val sortedPlayers = playerScores.entries.sortedByDescending { it.value }
        
        sortedPlayers.forEachIndexed { index, entry ->
            val player = Bukkit.getPlayer(entry.key) ?: return@forEachIndexed
            val points = when (index) {
                0 -> 100 // Primer lugar
                1 -> 75  // Segundo lugar
                2 -> 50  // Tercer lugar
                else -> 25 // Participación
            }
            
            awardPoints(player, points, "Posición ${index + 1} en RobarCola")
            torneoPlugin.torneoManager.recordGamePlayed(player, name)
            
            if (index == 0) {
                torneoPlugin.torneoManager.recordGameWon(player, name)
            }
        }
        
        // Anunciar ganadores
        Bukkit.broadcastMessage("${ChatColor.GOLD}${ChatColor.BOLD}=== JUEGO TERMINADO ===")
        sortedPlayers.take(3).forEachIndexed { index, entry ->
            val player = Bukkit.getPlayer(entry.key)
            val medal = when (index) {
                0 -> "${ChatColor.GOLD}🥇"
                1 -> "${ChatColor.GRAY}🥈"
                2 -> "${ChatColor.YELLOW}🥉"
                else -> ""
            }
            Bukkit.broadcastMessage("$medal ${ChatColor.WHITE}${player?.name}: ${ChatColor.AQUA}${entry.value} puntos")
        }
        
        // Limpiar
        removeAllTails()
        playersInGame.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                player.gameMode = GameMode.SURVIVAL
                lobbySpawn?.let { player.teleport(it) }
            }
        }
        
        playersInGame.clear()
        playersWithTail.clear()
        playerScores.clear()
    }
    
    private fun startGameTimer() {
        gameTask = object : BukkitRunnable() {
            override fun run() {
                if (!gameRunning) {
                    cancel()
                    return
                }
                
                gameTimeRemaining--
                
                // Mostrar tiempo restante en la action bar
                if (gameTimeRemaining % 10 == 0 || gameTimeRemaining <= 10) {
                    val minutes = gameTimeRemaining / 60
                    val seconds = gameTimeRemaining % 60
                    val timeText = String.format("%02d:%02d", minutes, seconds)
                    
                    playersInGame.forEach { uuid ->
                        Bukkit.getPlayer(uuid)?.spigot()?.sendMessage(
                            ChatMessageType.ACTION_BAR,
                            TextComponent("${ChatColor.YELLOW}Tiempo restante: ${ChatColor.WHITE}$timeText")
                        )
                    }
                }
                
                if (gameTimeRemaining <= 0) {
                    endGame()
                    cancel()
                }
            }
        }
        gameTask?.runTaskTimer(plugin, 0L, 20L) // Cada segundo
    }
    
    private fun giveTail(player: Player) {
        if (playersWithTail.contains(player.uniqueId)) return
        
        playersWithTail.add(player.uniqueId)
        
        // Crear ArmorStand como cola
        val tail = player.world.spawn(player.location, ArmorStand::class.java).apply {
            isVisible = false
            isSmall = true
            setGravity(false)
            isMarker = true
            
            // Equipar con bloque de lana naranja
            equipment?.helmet = ItemStack(Material.ORANGE_WOOL)
        }
        
        playerTails[player.uniqueId] = tail
        
        // Efectos visuales
        player.world.spawnParticle(Particle.FLAME, player.location, 20, 0.5, 0.5, 0.5, 0.1)
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f)
        
        player.sendMessage("${ChatColor.GOLD}¡Ahora tienes una cola! ${ChatColor.RED}¡Cuidado, otros jugadores intentarán robártela!")
    }
    
    private fun removeTail(player: Player) {
        playersWithTail.remove(player.uniqueId)
        playerTails.remove(player.uniqueId)?.remove()
    }
    
    private fun removeAllTails() {
        playerTails.values.forEach { it.remove() }
        playerTails.clear()
        playersWithTail.clear()
    }
    
    private fun updateTailPositions() {
        playersWithTail.forEach { uuid ->
            val player = Bukkit.getPlayer(uuid) ?: return@forEach
            val tail = playerTails[uuid] ?: return@forEach
            
            // Posicionar la cola detrás del jugador
            val direction = player.location.direction.normalize().multiply(-0.5)
            val tailLocation = player.location.clone().add(direction).add(0.0, 0.5, 0.0)
            tail.teleport(tailLocation)
        }
    }
    
    // Event Handlers
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (gameRunning) {
            event.player.sendMessage("${ChatColor.YELLOW}Hay un juego de RobarCola en progreso")
        }
    }
    
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        if (playersInGame.contains(uuid)) {
            removeTail(event.player)
            playersInGame.remove(uuid)
            playerScores.remove(uuid)
        }
    }
    
    @EventHandler
    fun onPlayerDamagePlayer(event: EntityDamageByEntityEvent) {
        if (!gameRunning) return
        
        val attacker = event.damager as? Player ?: return
        val victim = event.entity as? Player ?: return
        
        if (!playersInGame.contains(attacker.uniqueId) || !playersInGame.contains(victim.uniqueId)) return
        
        // Si la víctima tiene cola, el atacante se la roba
        if (playersWithTail.contains(victim.uniqueId)) {
            event.isCancelled = true
            
            removeTail(victim)
            giveTail(attacker)
            
            // Actualizar puntuación
            playerScores[attacker.uniqueId] = (playerScores[attacker.uniqueId] ?: 0) + 10
            
            attacker.sendMessage("${ChatColor.GREEN}¡Robaste la cola de ${victim.name}! +10 puntos")
            victim.sendMessage("${ChatColor.RED}${attacker.name} te robó la cola!")
            
            // Efectos
            victim.world.spawnParticle(Particle.CLOUD, victim.location, 15, 0.5, 0.5, 0.5, 0.1)
            victim.playSound(victim.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
        }
    }
    
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!gameRunning) return
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        
        val block = event.clickedBlock ?: return
        if (block.type != Material.OAK_SIGN && block.type != Material.OAK_WALL_SIGN) return
        
        val sign = block.state as? Sign ?: return
        if (sign.getLine(0) != "[RobarCola]") return
        
        val player = event.player
        if (!playersInGame.contains(player.uniqueId)) {
            player.sendMessage("${ChatColor.RED}No estás en el juego")
            return
        }
        
        // Dar cola si no tiene
        if (!playersWithTail.contains(player.uniqueId)) {
            giveTail(player)
        }
    }
    
    // Configuración
    private fun loadConfiguration() {
        val config = plugin.config
        
        if (config.contains("robarcola.gamespawn")) {
            gameSpawn = config.getLocation("robarcola.gamespawn")
        }
        
        if (config.contains("robarcola.lobbyspawn")) {
            lobbySpawn = config.getLocation("robarcola.lobbyspawn")
        }
    }
    
    private fun saveConfiguration() {
        val config = plugin.config
        
        gameSpawn?.let { config.set("robarcola.gamespawn", it) }
        lobbySpawn?.let { config.set("robarcola.lobbyspawn", it) }
        
        plugin.saveConfig()
    }
    
    private fun startTailUpdateTask() {
        // Actualizar posiciones de colas cada tick
        object : BukkitRunnable() {
            override fun run() {
                if (gameRunning) {
                    updateTailPositions()
                }
            }
        }.runTaskTimer(plugin, 0L, 1L)
    }
}
