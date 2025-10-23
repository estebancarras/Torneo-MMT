package yo.spray.robarCabeza.game

import los5fantasticos.torneo.util.GameTimer
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.ItemDisplay
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

/**
 * Representa una instancia de partida del minijuego Robar Cabeza.
 * 
 * Contiene todo el estado de una partida en curso:
 * - Jugadores participantes
 * - Jugadores con cabeza
 * - Entidades de visualización de cabezas
 * - Cooldowns de robo e invulnerabilidad
 * - Temporizador del juego
 * - Sistema de puntuación dinámica
 */
class RobarCabezaGame(
    val id: UUID = UUID.randomUUID(),
    var state: GameState = GameState.LOBBY
) {
    /**
     * Jugadores activos en la partida.
     */
    val players = mutableSetOf<UUID>()
    
    /**
     * Jugadores que actualmente tienen cabeza.
     */
    val playersWithTail = mutableSetOf<UUID>()
    
    /**
     * Mapa de jugadores a sus ArmorStands de cabeza (fallback).
     */
    val playerTails = mutableMapOf<UUID, ArmorStand>()
    
    /**
     * Mapa de jugadores a sus ItemDisplays de cabeza.
     */
    val playerTailDisplays = mutableMapOf<UUID, ItemDisplay>()
    
    /**
     * Cooldowns de robo por jugador (timestamp en milisegundos).
     */
    val stealCooldowns = mutableMapOf<UUID, Long>()
    
    /**
     * Cooldowns de invulnerabilidad por jugador (timestamp en milisegundos).
     * Después de robar una cabeza, el jugador es invulnerable por unos segundos.
     */
    val invulnerabilityCooldowns = mutableMapOf<UUID, Long>()
    
    /**
     * Temporizador visual de la partida (BossBar).
     */
    var gameTimer: GameTimer? = null
    
    /**
     * Tarea que otorga puntos por segundo a los jugadores con cabeza.
     */
    var pointsTickerTask: BukkitTask? = null
    
    /**
     * Contador de tiempo restante en segundos.
     */
    var countdown: Int = 0
    
    /**
     * Obtiene el número total de jugadores en la partida.
     */
    fun getTotalPlayers(): Int = players.size
    
    /**
     * Verifica si la partida tiene suficientes jugadores para comenzar.
     */
    fun hasMinimumPlayers(): Boolean = players.size >= 2
    
    /**
     * Verifica si un jugador está en cooldown de invulnerabilidad.
     * 
     * @param playerId UUID del jugador
     * @param cooldownSeconds Duración del cooldown en segundos
     * @return true si el jugador está invulnerable
     */
    fun isInvulnerable(playerId: UUID, cooldownSeconds: Int): Boolean {
        val lastTime = invulnerabilityCooldowns[playerId] ?: return false
        return System.currentTimeMillis() - lastTime < cooldownSeconds * 1000
    }
}
