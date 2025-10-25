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
     */
    @EventHandler
    fun onPlayerRespawn(event: org.bukkit.event.player.PlayerRespawnEvent) {
        val player = event.player
        val game = gameManager.getActiveGame() ?: return
        
        if (game.state != GameState.IN_GAME) return
        
        // Verificar si el jugador está en la partida y fue eliminado
        if (!game.getAllPlayers().contains(player.uniqueId)) return
        if (!game.eliminatedPlayers.contains(player.uniqueId)) return
        
        plugin.logger.info("[Coliseo] Respawn de ${player.name} - convirtiéndolo en espectador")
        
        // Establecer ubicación de respawn en la arena
        game.arena?.let { arena ->
            val centerSpawn = arena.eliteSpawns.firstOrNull() ?: arena.hordeSpawns.firstOrNull()
            centerSpawn?.let { 
                event.respawnLocation = it
            }
        }
        
        // Poner en modo espectador inmediatamente después del respawn
        Bukkit.getScheduler().runTask(plugin, Runnable {
            player.gameMode = GameMode.SPECTATOR
            player.sendMessage("${ChatColor.RED}Has sido eliminado. Ahora eres espectador.")
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
}
