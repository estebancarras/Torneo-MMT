package los5fantasticos.minigameColiseo.game

import los5fantasticos.torneo.util.GameTimer
import org.bukkit.block.Block
import java.util.UUID

/**
 * Representa una instancia de partida del Coliseo.
 * 
 * Contiene todo el estado de una partida en curso:
 * - Equipos y jugadores
 * - Arena activa
 * - Bloques colocados
 * - Temporizador
 */
class ColiseoGame(
    val id: UUID = UUID.randomUUID(),
    var state: GameState = GameState.WAITING,
    var arena: Arena? = null
) {
    /**
     * Jugadores del equipo Élite.
     */
    val elitePlayers = mutableSetOf<UUID>()
    
    /**
     * Jugadores del equipo Horda.
     */
    val hordePlayers = mutableSetOf<UUID>()
    
    /**
     * Jugadores eliminados (para tracking).
     */
    val eliminatedPlayers = mutableSetOf<UUID>()
    
    /**
     * Bloques colocados durante la partida (para limpieza).
     */
    val placedBlocks = mutableSetOf<Block>()
    
    /**
     * Ítems dropeados durante la partida (para limpieza).
     */
    val droppedItems = mutableListOf<org.bukkit.entity.Item>()
    
    /**
     * Temporizador visual de la partida.
     */
    var gameTimer: GameTimer? = null
    
    /**
     * Estado original del gamerule keepInventory (para restaurarlo al finalizar).
     */
    var originalKeepInventory: Boolean = false
    
    /**
     * Obtiene el número total de jugadores.
     */
    fun getTotalPlayers(): Int = elitePlayers.size + hordePlayers.size
    
    /**
     * Todos los jugadores que participaron en la partida (vivos y eliminados).
     * Esta lista NO se modifica cuando un jugador muere.
     */
    val allParticipants = mutableSetOf<UUID>()
    
    /**
     * Obtiene todos los jugadores de la partida (vivos y eliminados).
     */
    fun getAllPlayers(): Set<UUID> = allParticipants
    
    /**
     * Verifica si un jugador está en el equipo Élite.
     */
    fun isElite(playerId: UUID): Boolean = elitePlayers.contains(playerId)
    
    /**
     * Verifica si un jugador está en el equipo Horda.
     */
    fun isHorde(playerId: UUID): Boolean = hordePlayers.contains(playerId)
    
    /**
     * Verifica si la partida tiene suficientes jugadores.
     */
    fun hasMinimumPlayers(): Boolean = getTotalPlayers() >= 4
}
