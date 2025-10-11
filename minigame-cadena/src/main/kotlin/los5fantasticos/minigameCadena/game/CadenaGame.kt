package los5fantasticos.minigameCadena.game

import los5fantasticos.torneo.util.GameTimer
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import java.util.UUID

/**
 * Representa una instancia de partida del minijuego Cadena.
 * 
 * Una partida gestiona el estado, equipos, temporizador y arena
 * de una sesión de juego.
 * 
 * @property id Identificador único de la partida
 * @property state Estado actual de la partida
 * @property arena Arena donde se juega la partida
 */
class CadenaGame(
    val id: UUID = UUID.randomUUID(),
    var state: GameState = GameState.LOBBY,
    var arena: Arena? = null
) {
    /**
     * Lista de equipos predefinidos para el lobby.
     * Inicializada automáticamente con 4 equipos (Rojo, Azul, Verde, Amarillo).
     */
    val teams: MutableList<Team> = mutableListOf(
        Team(
            teamId = "ROJO",
            displayName = "§cEquipo Rojo",
            color = NamedTextColor.RED,
            material = Material.RED_WOOL
        ),
        Team(
            teamId = "AZUL",
            displayName = "§9Equipo Azul",
            color = NamedTextColor.BLUE,
            material = Material.BLUE_WOOL
        ),
        Team(
            teamId = "VERDE",
            displayName = "§aEquipo Verde",
            color = NamedTextColor.GREEN,
            material = Material.GREEN_WOOL
        ),
        Team(
            teamId = "AMARILLO",
            displayName = "§eEquipo Amarillo",
            color = NamedTextColor.YELLOW,
            material = Material.YELLOW_WOOL
        )
    )
    /**
     * Temporizador visual de la partida (BossBar).
     */
    var gameTimer: GameTimer? = null
    
    /**
     * Mapa de equipos a su último checkpoint alcanzado.
     * Key: Team ID (String), Value: Índice del checkpoint (-1 = spawn inicial)
     */
    val teamCheckpoints = mutableMapOf<String, Int>()
    
    
    /**
     * Obtiene el número total de jugadores en la partida.
     */
    fun getTotalPlayers(): Int {
        return teams.sumOf { it.players.size }
    }
    
    /**
     * Verifica si la partida tiene suficientes jugadores para comenzar.
     * Se requiere al menos 1 equipo con 2 jugadores.
     */
    fun hasMinimumPlayers(): Boolean {
        val teamsWithMinPlayers = teams.count { it.hasMinimumPlayers() }
        return teamsWithMinPlayers >= 1
    }
    
    /**
     * Obtiene un equipo por su material.
     */
    fun getTeamByMaterial(material: Material): Team? {
        return teams.find { it.material == material }
    }
    
    /**
     * Obtiene un equipo por su ID.
     */
    fun getTeamById(teamId: String): Team? {
        return teams.find { it.teamId == teamId }
    }
}
