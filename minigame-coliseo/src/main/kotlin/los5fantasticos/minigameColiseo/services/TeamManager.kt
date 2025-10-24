package los5fantasticos.minigameColiseo.services

import los5fantasticos.minigameColiseo.game.ColiseoGame
import los5fantasticos.minigameColiseo.game.TeamType
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import java.util.UUID

/**
 * Gestor de equipos del Coliseo.
 * 
 * Responsabilidades:
 * - Asignar jugadores a equipos
 * - Gestionar efectos visuales (brillo)
 * - Tracking de jugadores eliminados
 */
class TeamManager(private val plugin: org.bukkit.plugin.Plugin) {
    
    private var eliteTeam: Team? = null
    private var hordeTeam: Team? = null
    
    /**
     * Inicializa los equipos de Bukkit para efectos visuales.
     */
    fun setupTeams(game: ColiseoGame) {
        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        
        // Limpiar equipos existentes
        scoreboard.getTeam("ColiseoElite")?.unregister()
        scoreboard.getTeam("ColiseoHorde")?.unregister()
        
        // Crear equipo Élite
        eliteTeam = scoreboard.registerNewTeam("ColiseoElite").apply {
            color(NamedTextColor.GOLD)
            setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
            prefix(net.kyori.adventure.text.Component.text("[ÉLITE] ", NamedTextColor.GOLD))
        }
        
        // Crear equipo Horda
        hordeTeam = scoreboard.registerNewTeam("ColiseoHorde").apply {
            color(NamedTextColor.WHITE)
            setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
            prefix(net.kyori.adventure.text.Component.text("[HORDA] ", NamedTextColor.WHITE))
        }
        
        // Añadir jugadores a los equipos
        game.elitePlayers.forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let { player ->
                eliteTeam?.addEntry(player.name)
                player.isGlowing = true
            }
        }
        
        game.hordePlayers.forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let { player ->
                hordeTeam?.addEntry(player.name)
                player.isGlowing = true
            }
        }
    }
    
    /**
     * Añade un jugador a un equipo.
     */
    fun addToTeam(player: Player, teamType: TeamType, game: ColiseoGame) {
        when (teamType) {
            TeamType.ELITE -> {
                game.elitePlayers.add(player.uniqueId)
                eliteTeam?.addEntry(player.name)
                player.isGlowing = true
                player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}¡Eres parte de LA ÉLITE!")
            }
            TeamType.HORDE -> {
                game.hordePlayers.add(player.uniqueId)
                hordeTeam?.addEntry(player.name)
                player.isGlowing = true
                player.sendMessage("${ChatColor.WHITE}${ChatColor.BOLD}¡Eres parte de LA HORDA!")
            }
        }
    }
    
    /**
     * Marca un jugador como eliminado.
     */
    fun markAsEliminated(playerId: UUID, game: ColiseoGame) {
        game.eliminatedPlayers.add(playerId)
        
        // Remover del equipo activo
        if (game.elitePlayers.contains(playerId)) {
            game.elitePlayers.remove(playerId)
        } else if (game.hordePlayers.contains(playerId)) {
            game.hordePlayers.remove(playerId)
        }
    }
    
    /**
     * Obtiene los jugadores activos del equipo Élite.
     */
    fun getElitePlayers(game: ColiseoGame): List<Player> {
        return game.elitePlayers.mapNotNull { Bukkit.getPlayer(it) }
    }
    
    /**
     * Obtiene los jugadores activos del equipo Horda.
     */
    fun getHordePlayers(game: ColiseoGame): List<Player> {
        return game.hordePlayers.mapNotNull { Bukkit.getPlayer(it) }
    }
    
    /**
     * Limpia los equipos y efectos visuales.
     */
    fun cleanup() {
        eliteTeam?.unregister()
        hordeTeam?.unregister()
        eliteTeam = null
        hordeTeam = null
    }
}
