package los5fantasticos.minigameColiseo.services

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.minigameColiseo.game.TeamType
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player

/**
 * Servicio de puntuación del Coliseo.
 * 
 * Responsabilidades:
 * - Otorgar puntos por victoria
 * - Otorgar puntos por participación
 * - Otorgar puntos por kills
 */
class ScoreService(
    private val torneoPlugin: TorneoPlugin,
    private val config: FileConfiguration,
    private val gameName: String = "Coliseo"
) {
    
    private val eliteVictoryPoints = config.getInt("scoring.elite-victory", 50)
    private val hordeVictoryPoints = config.getInt("scoring.horde-victory", 30)
    private val participationPoints = config.getInt("scoring.participation", 5)
    private val perKillPoints = config.getInt("scoring.per-kill", 2)
    
    /**
     * Otorga puntos de victoria al equipo ganador.
     */
    fun awardVictoryPoints(players: List<Player>, teamType: TeamType) {
        val points = when (teamType) {
            TeamType.ELITE -> eliteVictoryPoints
            TeamType.HORDE -> hordeVictoryPoints
        }
        
        players.forEach { player ->
            torneoPlugin.torneoManager.addScore(
                player.uniqueId,
                gameName,
                points,
                "Victoria ${teamType.name}"
            )
            torneoPlugin.torneoManager.recordGameWon(player, gameName)
        }
    }
    
    /**
     * Otorga puntos de participación.
     */
    fun awardParticipationPoints(player: Player) {
        torneoPlugin.torneoManager.addScore(
            player.uniqueId,
            gameName,
            participationPoints,
            "Participación"
        )
        torneoPlugin.torneoManager.recordGamePlayed(player, gameName)
    }
    
    /**
     * Otorga puntos por kill.
     */
    fun awardKillPoints(killer: Player) {
        torneoPlugin.torneoManager.addScore(
            killer.uniqueId,
            gameName,
            perKillPoints,
            "Eliminación"
        )
    }
}
