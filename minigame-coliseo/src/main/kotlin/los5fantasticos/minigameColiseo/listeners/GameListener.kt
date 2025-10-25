package los5fantasticos.minigameColiseo.listeners

import los5fantasticos.minigameColiseo.game.GameState
import los5fantasticos.minigameColiseo.services.ColiseoScoreboardService
import los5fantasticos.minigameColiseo.services.GameManager
import los5fantasticos.minigameColiseo.services.KitService
import los5fantasticos.minigameColiseo.services.ScoreService
import los5fantasticos.minigameColiseo.services.TeamManager
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.entity.Item
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.plugin.Plugin

/**
 * Listener de eventos del Coliseo.
 * 
 * Responsabilidades:
 * - Manejar muertes (eliminación permanente, sin respawn)
 * - Controlar construcción (colocación/rotura de bloques)
 * - Manejar desconexiones
 */
class GameListener(
    private val plugin: Plugin,
    private val gameManager: GameManager,
    private val teamManager: TeamManager,
    private val kitService: KitService,
    private val scoreService: ScoreService,
    private val coliseoScoreboardService: ColiseoScoreboardService
) : Listener {
    
    /**
     * Maneja la muerte de un jugador.
     * TODOS los jugadores son eliminados permanentemente (sin respawn).
     */
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val game = gameManager.getActiveGame() ?: return
        
        if (game.state != GameState.IN_GAME) return
        
        // Verificar si el jugador está en la partida
        if (!game.getAllPlayers().contains(player.uniqueId)) return
        
        // IMPORTANTE: Mantener los drops (NO limpiar event.drops)
        // Esto permite que los ítems caigan naturalmente
        event.keepInventory = false // Asegurar que los ítems caigan
        event.droppedExp = 0
        
        // Log para debug
        plugin.logger.info("[Coliseo] ${player.name} murió. Drops: ${event.drops.size} ítems")
        
        // Obtener killer para puntos y registro de kills
        val killer = player.killer
        if (killer != null && game.getAllPlayers().contains(killer.uniqueId)) {
            scoreService.awardKillPoints(killer)
            coliseoScoreboardService.recordKill(killer)
        }
        
        // Determinar equipo para el mensaje
        val teamName = if (game.isElite(player.uniqueId)) {
            "${ChatColor.GOLD}[ÉLITE]"
        } else {
            "${ChatColor.WHITE}[HORDA]"
        }
        
        plugin.logger.info("[Coliseo] Jugador eliminado: ${player.name}")
        
        // Marcar como eliminado (pero NO remover de la lista principal)
        teamManager.markAsEliminated(player.uniqueId, game)
        
        // Anunciar eliminación a todos
        game.getAllPlayers().forEach { playerId ->
            Bukkit.getPlayer(playerId)?.sendMessage(
                "$teamName ${ChatColor.WHITE}${player.name} ${ChatColor.RED}ha sido eliminado!"
            )
        }
    }
    
    /**
     * Maneja el respawn del jugador para ponerlo en modo espectador.
     * PRIORIDAD ALTA para evitar que otros listeners interfieran.
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    fun onPlayerRespawn(event: org.bukkit.event.player.PlayerRespawnEvent) {
        val player = event.player
        val game = gameManager.getActiveGame() ?: return
        
        if (game.state != GameState.IN_GAME) return
        
        // Verificar si el jugador está en la partida y fue eliminado
        if (!game.getAllPlayers().contains(player.uniqueId)) return
        if (!game.eliminatedPlayers.contains(player.uniqueId)) return
        
        plugin.logger.info("[Coliseo] ===== RESPAWN INTERCEPTADO =====")
        plugin.logger.info("[Coliseo] Jugador: ${player.name}")
        plugin.logger.info("[Coliseo] Estado: Eliminado, convirtiéndolo en espectador")
        
        // IMPORTANTE: Establecer ubicación de respawn en el spawn de espectadores
        game.arena?.let { arena ->
            val spectatorSpawn = arena.getSpectatorSpawnLocation()
            if (spectatorSpawn != null) {
                event.respawnLocation = spectatorSpawn
                plugin.logger.info("[Coliseo] Respawn location establecida: ${spectatorSpawn.blockX}, ${spectatorSpawn.blockY}, ${spectatorSpawn.blockZ}")
            } else {
                plugin.logger.warning("[Coliseo] No se pudo obtener spawn de espectador")
            }
        }
        
        // Poner en modo espectador INMEDIATAMENTE (sin delay)
        // Esto previene que otros sistemas lo procesen
        player.gameMode = GameMode.SPECTATOR
        
        // Mensaje y confirmación en el siguiente tick
        Bukkit.getScheduler().runTask(plugin, Runnable {
            player.sendMessage("")
            player.sendMessage("${ChatColor.RED}${ChatColor.BOLD}═══ ELIMINADO ═══")
            player.sendMessage("${ChatColor.GRAY}Has sido eliminado de la partida")
            player.sendMessage("${ChatColor.YELLOW}Ahora eres espectador hasta que termine el juego")
            player.sendMessage("")
            
            plugin.logger.info("[Coliseo] ${player.name} ahora es espectador en la arena")
        })
    }
    
    /**
     * Maneja la desconexión de jugadores.
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val game = gameManager.getActiveGame() ?: return
        
        if (game.state != GameState.IN_GAME) return
        
        // Si es de la Élite, contar como eliminación
        if (game.isElite(player.uniqueId)) {
            teamManager.markAsEliminated(player.uniqueId, game)
            
            game.getAllPlayers().forEach { playerId ->
                Bukkit.getPlayer(playerId)?.sendMessage(
                    "${ChatColor.GOLD}[ÉLITE] ${ChatColor.WHITE}${player.name} ${ChatColor.RED}se ha desconectado (eliminado)"
                )
            }
        }
    }
    
    /**
     * Maneja la colocación de bloques.
     */
    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val game = gameManager.getActiveGame() ?: return
        
        if (game.state != GameState.IN_GAME) return
        
        // Verificar si el jugador está en la partida
        if (!game.getAllPlayers().contains(player.uniqueId)) return
        
        val block = event.block
        val material = block.type
        
        // Verificar si el bloque es permitido
        val allowedMaterials = if (game.isElite(player.uniqueId)) {
            setOf(Material.CYAN_TERRACOTTA)
        } else {
            setOf(Material.WHITE_WOOL, Material.LAVA)
        }
        
        if (material in allowedMaterials) {
            // Añadir a la lista de bloques colocados
            game.placedBlocks.add(block)
        } else {
            // Cancelar si no es un bloque permitido
            event.isCancelled = true
            player.sendMessage("${ChatColor.RED}¡No puedes colocar ese bloque!")
        }
    }
    
    /**
     * Maneja la rotura de bloques.
     */
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val game = gameManager.getActiveGame() ?: return
        
        if (game.state != GameState.IN_GAME) return
        
        // Verificar si el jugador está en la partida
        if (!game.getAllPlayers().contains(player.uniqueId)) return
        
        val block = event.block
        
        // Solo permitir romper bloques que fueron colocados durante la partida
        if (!game.placedBlocks.contains(block)) {
            event.isCancelled = true
            player.sendMessage("${ChatColor.RED}¡Solo puedes romper bloques colocados durante la partida!")
        } else {
            // Remover de la lista
            game.placedBlocks.remove(block)
        }
    }
    
    /**
     * Rastrea los ítems que aparecen en la arena durante la partida.
     */
    @EventHandler
    fun onItemSpawn(event: ItemSpawnEvent) {
        val game = gameManager.getActiveGame() ?: return
        
        if (game.state != GameState.IN_GAME) return
        
        val item = event.entity
        val arena = game.arena ?: return
        
        // Verificar si el ítem está dentro de la arena
        // Asumiendo que la arena tiene un método para verificar si una ubicación está dentro
        // Si no existe, simplemente rastreamos todos los ítems durante la partida
        game.droppedItems.add(item)
    }
    
    /**
     * Controla el PvP entre jugadores del mismo equipo.
     * 
     * Reglas:
     * - La HORDA no puede atacarse entre sí (trabajan en equipo)
     * - La ÉLITE SÍ puede atacarse entre sí (son egoístas, cada uno por su cuenta)
     */
    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val game = gameManager.getActiveGame() ?: return
        
        if (game.state != GameState.IN_GAME) return
        
        // Verificar que ambos sean jugadores
        val damager = event.damager as? Player ?: return
        val victim = event.entity as? Player ?: return
        
        // Verificar que ambos estén en la partida
        if (!game.getAllPlayers().contains(damager.uniqueId)) return
        if (!game.getAllPlayers().contains(victim.uniqueId)) return
        
        // Determinar equipos
        val damagerIsElite = game.isElite(damager.uniqueId)
        val victimIsElite = game.isElite(victim.uniqueId)
        
        // Si ambos son de la HORDA, cancelar el daño
        if (!damagerIsElite && !victimIsElite) {
            event.isCancelled = true
            damager.sendMessage("${ChatColor.RED}¡No puedes atacar a tu compañero de la Horda!")
            plugin.logger.info("[Coliseo] PvP bloqueado: ${damager.name} intentó atacar a ${victim.name} (ambos Horda)")
            return
        }
        
        // Si ambos son de la ÉLITE, permitir el daño (son egoístas)
        if (damagerIsElite && victimIsElite) {
            // Mensaje opcional para recordar la dinámica
            if (Math.random() < 0.1) { // 10% de probabilidad para no spamear
                damager.sendMessage("${ChatColor.GOLD}${ChatColor.ITALIC}La Élite no conoce la lealtad...")
            }
            plugin.logger.info("[Coliseo] PvP permitido: ${damager.name} atacó a ${victim.name} (ambos Élite)")
        }
        
        // Si son de equipos diferentes, siempre permitir (PvP normal)
    }
}
