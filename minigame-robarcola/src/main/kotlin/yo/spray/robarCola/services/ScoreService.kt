package yo.spray.robarCola.services

import los5fantasticos.torneo.TorneoPlugin
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Servicio de puntuación para el minijuego RobarCola.
 * 
 * Responsabilidades:
 * - Otorgar puntos a los jugadores según sus acciones
 * - Gestionar puntuación dinámica por tiempo
 * - Registrar victorias y partidas jugadas
 * - Centralizar la lógica de puntuación del torneo
 */
class ScoreService(
    private val torneoPlugin: TorneoPlugin,
    private val gameName: String
) {
    
    private val torneoManager = torneoPlugin.torneoManager
    
    /**
     * Mapa de puntos acumulados por jugador en la partida actual.
     * Se usa para determinar el ranking final.
     */
    private val sessionScores = mutableMapOf<UUID, Int>()
    
    /**
     * Otorga puntos por retener la cola durante un segundo.
     * 
     * @param player Jugador que retiene la cola
     */
    fun awardPointsForHolding(player: Player) {
        torneoManager.addScore(
            player.uniqueId,
            gameName,
            RobarColaScoreConfig.POINTS_PER_SECOND,
            "Retener cola"
        )
        
        // Actualizar puntos de sesión
        sessionScores[player.uniqueId] = (sessionScores[player.uniqueId] ?: 0) + RobarColaScoreConfig.POINTS_PER_SECOND
    }
    
    /**
     * Otorga puntos bonus por robar una cola exitosamente.
     * 
     * @param player Jugador que robó la cola
     */
    fun awardPointsForSteal(player: Player) {
        torneoManager.addScore(
            player.uniqueId,
            gameName,
            RobarColaScoreConfig.POINTS_STEAL_BONUS,
            "Robo de cola"
        )
        
        // Actualizar puntos de sesión
        sessionScores[player.uniqueId] = (sessionScores[player.uniqueId] ?: 0) + RobarColaScoreConfig.POINTS_STEAL_BONUS
    }
    
    /**
     * Otorga puntos bonus al finalizar el juego según el ranking.
     * 
     * @param player Jugador
     * @param position Posición en el ranking (1, 2, 3)
     */
    fun awardPointsForFinalPosition(player: Player, position: Int) {
        val points = when (position) {
            1 -> RobarColaScoreConfig.POINTS_FIRST_PLACE
            2 -> RobarColaScoreConfig.POINTS_SECOND_PLACE
            3 -> RobarColaScoreConfig.POINTS_THIRD_PLACE
            else -> 0
        }
        
        if (points > 0) {
            torneoManager.addScore(
                player.uniqueId,
                gameName,
                points,
                "Posición #$position"
            )
            
            if (position == 1) {
                torneoManager.recordGameWon(player, gameName)
            }
        }
    }
    
    /**
     * Otorga puntos por participar en la partida.
     * 
     * @param player Jugador participante
     */
    fun awardPointsForParticipation(player: Player) {
        torneoManager.addScore(
            player.uniqueId,
            gameName,
            RobarColaScoreConfig.POINTS_PARTICIPATION,
            "Participación"
        )
        torneoManager.recordGamePlayed(player, gameName)
    }
    
    /**
     * Obtiene el ranking de jugadores ordenado por puntos de sesión.
     * 
     * @return Lista de pares (UUID, puntos) ordenada de mayor a menor
     */
    fun getSessionRanking(): List<Pair<UUID, Int>> {
        return sessionScores.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
    }
    
    /**
     * Obtiene los puntos de sesión de un jugador.
     * 
     * @param playerId UUID del jugador
     * @return Puntos acumulados en la sesión
     */
    fun getSessionScore(playerId: UUID): Int {
        return sessionScores[playerId] ?: 0
    }
    
    /**
     * Reinicia los puntos de sesión para una nueva partida.
     */
    fun resetSessionScores() {
        sessionScores.clear()
    }
    
    /**
     * Registra que un jugador jugó una partida.
     * 
     * @param player Jugador
     */
    fun recordGamePlayed(player: Player) {
        torneoManager.recordGamePlayed(player, gameName)
    }
}
