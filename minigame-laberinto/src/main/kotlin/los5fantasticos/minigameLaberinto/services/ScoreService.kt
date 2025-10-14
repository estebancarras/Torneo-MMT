package los5fantasticos.minigameLaberinto.services

import los5fantasticos.minigameLaberinto.MinigameLaberinto
import los5fantasticos.torneo.core.TorneoManager
import org.bukkit.entity.Player

/**
 * Servicio de puntuación del minijuego Laberinto.
 * 
 * Responsable de:
 * - Otorgar puntos por completar el laberinto
 * - Otorgar puntos de participación
 * - Registrar estadísticas de juego
 */
class ScoreService(
    private val minigame: MinigameLaberinto,
    private val torneoManager: TorneoManager
) {
    
    /**
     * Puntos otorgados por completar el laberinto.
     */
    private val completionPoints = 75
    
    /**
     * Puntos otorgados por participar (no completar).
     */
    private val participationPoints = 10
    
    /**
     * Otorga puntos a un jugador por completar el laberinto.
     * 
     * @param player Jugador que completó el laberinto
     */
    fun awardCompletionPoints(player: Player) {
        torneoManager.addScore(
            player.uniqueId,
            minigame.gameName,
            completionPoints,
            "Completó el laberinto"
        )
        
        // Registrar victoria
        torneoManager.recordGameWon(player, minigame.gameName)
        
        minigame.plugin.logger.info("${player.name} recibió $completionPoints puntos por completar el laberinto")
    }
    
    /**
     * Otorga puntos de participación a un jugador.
     * 
     * @param player Jugador que participó pero no completó
     */
    fun awardParticipationPoints(player: Player) {
        torneoManager.addScore(
            player.uniqueId,
            minigame.gameName,
            participationPoints,
            "Participó en el laberinto"
        )
        
        // Registrar partida jugada
        torneoManager.recordGamePlayed(player, minigame.gameName)
        
        minigame.plugin.logger.info("${player.name} recibió $participationPoints puntos por participar en el laberinto")
    }
    
    
    /**
     * Limpia el servicio de puntuación.
     */
    fun clearAll() {
        // No hay datos persistentes que limpiar
        minigame.plugin.logger.info("ScoreService limpiado")
    }
}
