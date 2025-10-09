package los5fantasticos.minigameCadena.game

import java.util.UUID

/**
 * Representa una instancia de partida del minijuego Cadena.
 * 
 * Una partida gestiona el estado, equipos, temporizador y arena
 * de una sesión de juego.
 * 
 * @property id Identificador único de la partida
 * @property teams Lista de equipos participantes
 * @property state Estado actual de la partida
 */
data class CadenaGame(
    val id: UUID = UUID.randomUUID(),
    val teams: MutableList<Team> = mutableListOf(),
    var state: GameState = GameState.LOBBY
) {
    /**
     * Tiempo restante en segundos.
     * TODO PR2: Implementar lógica de temporizador
     */
    var timeRemaining: Int = 300 // 5 minutos por defecto
    
    /**
     * Añade un equipo a la partida.
     */
    fun addTeam(team: Team) {
        if (state != GameState.LOBBY) {
            throw IllegalStateException("No se pueden añadir equipos cuando la partida ya comenzó")
        }
        teams.add(team)
    }
    
    /**
     * Obtiene el número total de jugadores en la partida.
     */
    fun getTotalPlayers(): Int {
        return teams.sumOf { it.players.size }
    }
    
    /**
     * Verifica si la partida tiene suficientes jugadores para comenzar.
     */
    fun hasMinimumPlayers(): Boolean {
        return teams.any { it.hasMinimumPlayers() }
    }
}
