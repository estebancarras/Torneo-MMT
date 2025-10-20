package los5fantasticos.minigameCarrerabarcos.listeners

import los5fantasticos.minigameCarrerabarcos.game.Carrera
import los5fantasticos.minigameCarrerabarcos.services.GameManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Listener de eventos de juego para carreras.
 * 
 * RESPONSABILIDADES:
 * - Detectar cuando un jugador atraviesa un checkpoint
 * - Detectar cuando un jugador cruza la meta
 * - Manejar desconexiones durante carreras
 * 
 * ARQUITECTURA:
 * Este listener SOLO detecta y notifica.
 * Toda la lógica de negocio está en GameManager.
 * Esto mantiene el código limpio y testeable.
 */
class GameListener(private val gameManager: GameManager) : Listener {
    
    /**
     * Detecta el movimiento de jugadores para verificar checkpoints y meta.
     * 
     * OPTIMIZACIÓN:
     * - Solo procesa si el jugador está en una carrera
     * - Solo procesa si realmente cambió de bloque
     * - Usa verificaciones rápidas antes de operaciones costosas
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val to = event.to ?: return
        val from = event.from
        
        // OPTIMIZACIÓN 1: Solo verificar si cambió de bloque
        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) {
            return
        }
        
        // OPTIMIZACIÓN 2: Solo procesar si el jugador está en una carrera
        val carrera = gameManager.getCarreraDeJugador(player) ?: return
        
        // OPTIMIZACIÓN 3: Solo procesar si la carrera está en curso
        if (carrera.estado != Carrera.EstadoCarrera.EN_CURSO) {
            return
        }
        
        // Verificar checkpoints
        verificarCheckpoint(player, carrera, to)
        
        // Verificar meta
        verificarMeta(player, carrera, to)
    }
    
    /**
     * Verifica si el jugador atravesó su próximo checkpoint.
     */
    private fun verificarCheckpoint(player: org.bukkit.entity.Player, carrera: Carrera, location: org.bukkit.Location) {
        val progresoActual = carrera.getProgreso(player)
        val checkpoints = carrera.arena.checkpoints
        
        // Verificar que no haya terminado todos los checkpoints
        if (progresoActual >= checkpoints.size) {
            return
        }
        
        // Obtener el próximo checkpoint
        val proximoCheckpoint = checkpoints[progresoActual]
        
        // Verificar si el jugador está dentro del checkpoint
        if (proximoCheckpoint.contains(location)) {
            // Notificar al GameManager para actualizar progreso
            gameManager.actualizarProgresoJugador(player)
        }
    }
    
    /**
     * Verifica si el jugador cruzó la meta.
     */
    private fun verificarMeta(player: org.bukkit.entity.Player, carrera: Carrera, location: org.bukkit.Location) {
        val meta = carrera.arena.meta ?: return
        
        // Verificar si el jugador puede finalizar (ha pasado todos los checkpoints)
        if (!carrera.puedeFinalizarCarrera(player)) {
            return
        }
        
        // Verificar si el jugador está en la meta
        if (meta.contains(location)) {
            // Notificar al GameManager para finalizar al jugador
            gameManager.finalizarJugador(player)
        }
    }
    
    /**
     * Maneja la desconexión de jugadores durante una carrera.
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        // Si el jugador está en una carrera, removerlo
        if (gameManager.estaEnCarrera(player)) {
            gameManager.removerJugadorDeCarrera(player)
        }
    }
}
