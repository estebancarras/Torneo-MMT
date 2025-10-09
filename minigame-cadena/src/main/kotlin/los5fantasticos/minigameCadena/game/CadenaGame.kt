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
 * @property arena Arena donde se juega la partida
 */
data class CadenaGame(
    val id: UUID = UUID.randomUUID(),
    val teams: MutableList<Team> = mutableListOf(),
    var state: GameState = GameState.LOBBY,
    var arena: Arena? = null
) {
    /**
     * Tiempo restante en segundos.
     * TODO PR5: Implementar lógica de temporizador
     */
    var timeRemaining: Int = 300 // 5 minutos por defecto
    
    /**
     * Mapa de equipos a su último checkpoint alcanzado.
     * Key: Team ID, Value: Índice del checkpoint (-1 = spawn inicial)
     */
    val teamCheckpoints = mutableMapOf<UUID, Int>()
    
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
