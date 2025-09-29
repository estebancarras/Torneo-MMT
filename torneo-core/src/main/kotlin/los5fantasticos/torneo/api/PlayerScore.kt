package los5fantasticos.torneo.api

import java.util.UUID

/**
 * Modelo de datos que representa el puntaje de un jugador en el torneo.
 */
data class PlayerScore(
    val playerUUID: UUID,
    val playerName: String,
    var totalPoints: Int = 0,
    val pointsPerMinigame: MutableMap<String, Int> = mutableMapOf(),
    var gamesPlayed: Int = 0,
    var gamesWon: Int = 0
) {
    /**
     * Añade puntos al jugador para un minijuego específico.
     * 
     * @param minigameName Nombre del minijuego
     * @param points Puntos a añadir
     */
    fun addPoints(minigameName: String, points: Int) {
        totalPoints += points
        pointsPerMinigame[minigameName] = (pointsPerMinigame[minigameName] ?: 0) + points
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
     * Obtiene el ratio de victorias del jugador.
     * 
     * @return Ratio de victorias (0.0 a 1.0)
     */
    fun getWinRate(): Double {
        return if (gamesPlayed > 0) gamesWon.toDouble() / gamesPlayed.toDouble() else 0.0
    }
    
    /**
     * Obtiene los puntos del jugador en un minijuego específico.
     * 
     * @param minigameName Nombre del minijuego
     * @return Puntos en ese minijuego, o 0 si no ha jugado
     */
    fun getPointsForMinigame(minigameName: String): Int {
        return pointsPerMinigame[minigameName] ?: 0
    }
}
