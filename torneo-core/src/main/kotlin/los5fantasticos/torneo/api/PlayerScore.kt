package los5fantasticos.torneo.api

import java.util.UUID

/**
 * Representa el puntaje y estadísticas de un jugador en el torneo.
 */
data class PlayerScore(
    val uuid: UUID,
    val name: String,
    var totalPoints: Int = 0,
    val pointsPerMinigame: MutableMap<String, Int> = mutableMapOf(),
    var gamesPlayed: Int = 0,
    var gamesWon: Int = 0
) {
    // Alias para compatibilidad con código existente
    val playerUUID: UUID get() = uuid
    val playerName: String get() = name
    
    /**
     * Agrega puntos al jugador.
     */
    fun addPoints(minigame: String, points: Int) {
        totalPoints += points
        pointsPerMinigame[minigame] = (pointsPerMinigame[minigame] ?: 0) + points
    }
    
    /**
     * Obtiene los puntos de un minijuego específico.
     */
    fun getPointsForMinigame(minigame: String): Int {
        return pointsPerMinigame[minigame] ?: 0
    }
    
    /**
     * Incrementa el contador de juegos jugados.
     */
    fun incrementGamesPlayed() {
        gamesPlayed++
    }
    
    /**
     * Incrementa el contador de juegos ganados.
     */
    fun incrementGamesWon() {
        gamesWon++
    }
    
    /**
     * Calcula el ratio de victoria del jugador.
     */
    @Suppress("unused")
    fun getWinRate(): Double {
        return if (gamesPlayed > 0) {
            (gamesWon.toDouble() / gamesPlayed.toDouble()) * 100
        } else {
            0.0
        }
    }
}
