package yo.spray.robarCabeza.listeners

import org.bukkit.ChatColor
import org.bukkit.block.Sign
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.inventory.InventoryType.SlotType
import yo.spray.robarCabeza.services.GameManager

/**
 * Listener de eventos del juego RobarCabeza.
 * 
 * Responsabilidades:
 * - Escuchar eventos de Bukkit relacionados con el juego
 * - Delegar la lógica al GameManager
 * - Manejar interacciones de jugadores (ataques, clics, desconexiones)
 * - Proteger el inventario (evitar que se quiten la cabeza)
 * - Proteger los límites de la arena
 */
class GameListener(
    private val gameManager: GameManager
) : Listener {
    
    private val signText = "[RobarCola]"
    private val lobbySignText = "[Lobby]"
    
    /**
     * Maneja cuando un jugador se desconecta.
     */
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        gameManager.removePlayer(event.player)
    }
    
    /**
     * Maneja interacciones con carteles.
     */
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        if (!block.type.name.contains("SIGN")) return
        val sign = block.state as? Sign ?: return
        
        @Suppress("DEPRECATION")
        when (sign.getLine(0)) {
            signText -> gameManager.addPlayer(event.player)
            lobbySignText -> gameManager.teleportToLobby(event.player)
        }
    }
    
    /**
     * Maneja ataques entre jugadores y a ArmorStands.
     * Usa HIGHEST priority para capturar el evento antes que otros plugins lo cancelen.
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = false)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val victim = event.entity
        val attacker = event.damager as? Player ?: return
        
        // Verificar que haya un juego activo
        val game = gameManager.getActiveGame()
        if (game == null) {
            return
        }
        
        // Verificar que el atacante esté en el juego
        if (!game.players.contains(attacker.uniqueId)) {
            return
        }
        
        // Caso 1: Golpe a ArmorStand (robo directo)
        if (victim is ArmorStand) {
            event.isCancelled = true // Cancelar daño al ArmorStand
            handleArmorStandAttack(victim, attacker)
            return
        }
        
        // Caso 2: Golpe a otro jugador
        if (victim is Player) {
            // Verificar que la víctima esté en el juego
            if (!game.players.contains(victim.uniqueId)) {
                return
            }
            
            // Verificar si la víctima tiene cabeza
            val victimHasHead = game.playersWithTail.contains(victim.uniqueId)
            
            gameManager.plugin.logger.info("[RobarCabeza] Evento de ataque: ${attacker.name} -> ${victim.name}, víctima tiene cabeza: $victimHasHead")
            
            if (victimHasHead) {
                // La víctima tiene cabeza: FORZAR que el evento NO se cancele (permitir PvP)
                event.isCancelled = false
                gameManager.plugin.logger.info("[RobarCabeza] PvP forzado, procesando robo...")
                handlePlayerAttack(victim, attacker)
            } else {
                // La víctima NO tiene cabeza: cancelar el daño (jugadores sin cabeza no se dañan entre sí)
                event.isCancelled = true
                gameManager.plugin.logger.info("[RobarCabeza] Daño cancelado (víctima sin cabeza)")
            }
        }
    }
    
    /**
     * Maneja ataque a un ArmorStand de cabeza (legacy).
     */
    private fun handleArmorStandAttack(armorStand: ArmorStand, attacker: Player) {
        val owner = gameManager.findTailOwner(armorStand) ?: return
        
        // Intentar robar la cabeza
        gameManager.stealHead(owner, attacker)
    }
    
    /**
     * Maneja ataque de un jugador a otro.
     */
    private fun handlePlayerAttack(victim: Player, attacker: Player) {
        val game = gameManager.getActiveGame() ?: return
        
        // Verificar que la víctima tenga cabeza
        if (!game.playersWithTail.contains(victim.uniqueId)) {
            return
        }
        
        // Robar la cabeza (se puede atacar desde cualquier ángulo)
        gameManager.stealHead(victim, attacker)
    }
    
    /**
     * Protege el inventario para evitar que los jugadores se quiten la cabeza manualmente.
     */
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        
        // Verificar si el jugador está en una partida
        if (!gameManager.isPlayerInGame(player)) {
            return
        }
        
        // Verificar si está intentando interactuar con el slot del casco
        if (event.slotType == SlotType.ARMOR && event.rawSlot == 5) {
            event.isCancelled = true
            player.sendMessage("${ChatColor.RED}¡No puedes quitarte la cabeza durante el juego!")
        }
    }
    
    /**
     * Protege los límites de la arena para evitar que los jugadores salgan.
     */
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        
        // Verificar si el jugador está en una partida
        if (!gameManager.isPlayerInGame(player)) {
            return
        }
        
        // Obtener la arena activa
        val arena = gameManager.getActiveArena() ?: return
        val playRegion = arena.playRegion ?: return
        
        // Verificar si el jugador está intentando salir de la región
        val to = event.to ?: return
        
        if (!playRegion.contains(to)) {
            event.isCancelled = true
            player.sendMessage("${ChatColor.RED}¡No puedes salir de la arena!")
        }
    }
    
    /**
     * Verifica si el atacante está detrás de la víctima.
     */
    private fun isBehindVictim(attacker: Player, victim: Player): Boolean {
        val victimDir = victim.location.direction.clone().normalize()
        val toAttacker = attacker.location.toVector().subtract(victim.location.toVector()).normalize()
        val dot = victimDir.dot(toAttacker)
        return dot < -0.5 && attacker.location.distance(victim.location) <= 3.0
    }
}

