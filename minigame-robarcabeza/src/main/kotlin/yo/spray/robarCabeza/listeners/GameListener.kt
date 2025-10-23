package yo.spray.robarCabeza.listeners

import org.bukkit.ChatColor
import org.bukkit.block.Sign
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import yo.spray.robarCabeza.services.GameManager

/**
 * Listener de eventos del juego RobarCola.
 * 
 * Responsabilidades:
 * - Escuchar eventos de Bukkit relacionados con el juego
 * - Delegar la lógica al GameManager
 * - Manejar interacciones de jugadores (ataques, clics, desconexiones)
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
     */
    @EventHandler
    fun onAttack(event: EntityDamageByEntityEvent) {
        val victim = event.entity
        val attacker = event.damager as? Player ?: return
        
        // Caso 1: Golpe a ArmorStand (robo directo)
        if (victim is ArmorStand) {
            handleArmorStandAttack(victim, attacker)
            return
        }
        
        // Caso 2: Golpe por la espalda a otro jugador
        if (victim is Player) {
            handlePlayerAttack(victim, attacker)
        }
    }
    
    /**
     * Maneja ataque a un ArmorStand de cola.
     */
    private fun handleArmorStandAttack(armorStand: ArmorStand, attacker: Player) {
        val owner = gameManager.findTailOwner(armorStand) ?: return
        
        // Intentar robar la cola
        gameManager.stealTail(owner, attacker)
    }
    
    /**
     * Maneja ataque de un jugador a otro.
     */
    private fun handlePlayerAttack(victim: Player, attacker: Player) {
        val game = gameManager.getActiveGame() ?: return
        
        // Verificar que ambos estén en el juego
        if (!game.players.contains(victim.uniqueId) || !game.players.contains(attacker.uniqueId)) {
            return
        }
        
        // Verificar que la víctima tenga cola
        if (!game.playersWithTail.contains(victim.uniqueId)) {
            return
        }
        
        // Verificar que el ataque sea por la espalda
        if (!isBehindVictim(attacker, victim)) {
            return
        }
        
        // Robar la cola
        gameManager.stealTail(victim, attacker)
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

