package los5fantasticos.minigameColiseo.listeners

import los5fantasticos.minigameColiseo.game.GameState
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
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.plugin.Plugin

/**
 * Listener de eventos del Coliseo.
 * 
 * Responsabilidades:
 * - Manejar muertes y respawns
 * - Controlar construcción (colocación/rotura de bloques)
 * - Manejar desconexiones
 */
class GameListener(
    private val plugin: Plugin,
    private val gameManager: GameManager,
    private val teamManager: TeamManager,
    private val kitService: KitService,
    private val scoreService: ScoreService
) : Listener {
    
    /**
     * Maneja la muerte de un jugador.
     */
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val game = gameManager.getActiveGame() ?: return
        
        if (game.state != GameState.IN_GAME) return
        
        // Verificar si el jugador está en la partida
        if (!game.getAllPlayers().contains(player.uniqueId)) return
        
        // Limpiar drops
        event.drops.clear()
        event.droppedExp = 0
        
        // Obtener killer para puntos
        val killer = player.killer
        if (killer != null && game.getAllPlayers().contains(killer.uniqueId)) {
            scoreService.awardKillPoints(killer)
        }
        
        // Verificar si es de la Élite
        if (game.isElite(player.uniqueId)) {
            handleEliteDeath(player, game)
        } else if (game.isHorde(player.uniqueId)) {
            handleHordeDeath(player, game)
        }
    }
    
    /**
     * Maneja la muerte de un jugador Élite.
     */
    private fun handleEliteDeath(player: Player, game: los5fantasticos.minigameColiseo.game.ColiseoGame) {
        plugin.logger.info("[Coliseo] Jugador Élite eliminado: ${player.name}")
        
        // Marcar como eliminado
        teamManager.markAsEliminated(player.uniqueId, game)
        
        // Poner en modo espectador
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            player.gameMode = GameMode.SPECTATOR
            player.sendMessage("${ChatColor.RED}Has sido eliminado. Ahora eres espectador.")
        }, 1L)
        
        // Anunciar eliminación
        game.getAllPlayers().forEach { playerId ->
            Bukkit.getPlayer(playerId)?.sendMessage(
                "${ChatColor.GOLD}[ÉLITE] ${ChatColor.WHITE}${player.name} ${ChatColor.RED}ha sido eliminado!"
            )
        }
    }
    
    /**
     * Maneja la muerte de un jugador Horda.
     */
    private fun handleHordeDeath(player: Player, game: los5fantasticos.minigameColiseo.game.ColiseoGame) {
        plugin.logger.info("[Coliseo] Jugador Horda eliminado: ${player.name} (reaparecerá)")
        
        // La Horda reaparece
        player.sendMessage("${ChatColor.YELLOW}Reaparecerás en 3 segundos...")
    }
    
    /**
     * Maneja el respawn de jugadores.
     */
    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val game = gameManager.getActiveGame() ?: return
        
        if (game.state != GameState.IN_GAME) return
        
        // Solo la Horda reaparece
        if (game.isHorde(player.uniqueId)) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                // Teletransportar a spawn de Horda
                val spawn = game.arena?.getRandomHordeSpawn()
                if (spawn != null) {
                    player.teleport(spawn)
                    
                    // Reaplicar kit
                    kitService.applyHordeKit(player)
                    
                    player.sendMessage("${ChatColor.GREEN}¡Has reaparecido!")
                }
            }, 60L) // 3 segundos
        }
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
}
