package los5fantasticos.minigameCadena.game

import org.bukkit.entity.Player
import java.util.UUID

/**
 * Representa un equipo en el minijuego Cadena.
 * 
 * Un equipo está compuesto por 2-4 jugadores que están encadenados
 * y deben completar el parkour juntos.
 * 
 * @property id Identificador único del equipo
 * @property players Lista de UUIDs de los jugadores del equipo
 */
data class Team(
    val id: UUID = UUID.randomUUID(),
    val players: MutableList<UUID> = mutableListOf()
) {
    /**
     * Obtiene los jugadores online del equipo.
     */
    fun getOnlinePlayers(): List<Player> {
        return players.mapNotNull { uuid ->
            org.bukkit.Bukkit.getPlayer(uuid)
        }
    }
    
    /**
     * Añade un jugador al equipo.
     */
    fun addPlayer(player: Player) {
        if (players.size >= 4) {
            throw IllegalStateException("El equipo ya está completo (máximo 4 jugadores)")
        }
        players.add(player.uniqueId)
    }
    
    /**
     * Remueve un jugador del equipo.
     */
    fun removePlayer(player: Player) {
        players.remove(player.uniqueId)
    }
    
    /**
     * Verifica si el equipo está completo.
     */
    fun isFull(): Boolean = players.size >= 4
    
    /**
     * Verifica si el equipo tiene el mínimo de jugadores.
     */
    fun hasMinimumPlayers(): Boolean = players.size >= 2
}
