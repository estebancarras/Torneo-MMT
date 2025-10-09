package los5fantasticos.minigameCadena.listeners

import los5fantasticos.minigameCadena.MinigameCadena
import los5fantasticos.minigameCadena.game.GameState
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

/**
 * Listener que detecta movimientos de jugadores durante el parkour.
 * 
 * Responsabilidades:
 * - Detectar caídas por debajo de la altura mínima
 * - Detectar cuando un jugador alcanza un checkpoint
 * - Detectar cuando un equipo alcanza la meta
 */
class ParkourListener(private val minigame: MinigameCadena) : Listener {
    
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        
        // Verificar si el jugador está en una partida
        val game = minigame.gameManager.getPlayerGame(player) ?: return
        
        // Solo procesar si la partida está en curso
        if (game.state != GameState.IN_GAME) {
            return
        }
        
        // Verificar si hay arena configurada
        if (game.arena == null) {
            return
        }
        
        // Optimización: Solo procesar si hubo movimiento significativo
        val from = event.from
        val to = event.to ?: return
        
        // Si no se movió (solo rotación de cabeza), ignorar
        if (from.x == to.x && from.y == to.y && from.z == to.z) {
            return
        }
        
        // Verificar caída
        minigame.parkourService.checkFall(player, game)
        
        // Verificar checkpoint (solo si se movió horizontalmente)
        if (from.x != to.x || from.z != to.z) {
            minigame.parkourService.checkCheckpoint(player, game)
        }
    }
}
