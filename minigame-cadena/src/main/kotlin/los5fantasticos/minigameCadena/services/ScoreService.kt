package los5fantasticos.minigameCadena.services

import los5fantasticos.minigameCadena.MinigameCadena
import los5fantasticos.minigameCadena.config.CadenaScoreConfig
import los5fantasticos.minigameCadena.game.CadenaGame
import los5fantasticos.minigameCadena.game.Team
import los5fantasticos.torneo.core.TorneoManager
import org.bukkit.ChatColor
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Servicio de puntuación para el minijuego Cadena.
 * 
 * Sigue el patrón estandarizado de asignación de puntos:
 * - Encapsula toda la lógica de negocio sobre cuándo y cuántos puntos se otorgan
 * - Utiliza TorneoManager.addScore() como único punto de entrada para puntos
 * - Lee configuraciones desde CadenaScoreConfig
 * 
 * Sistema de puntos:
 * - Victoria (completar parkour): 100 puntos + bonus por posición
 * - 1er lugar: +50 puntos
 * - 2do lugar: +30 puntos
 * - 3er lugar: +15 puntos
 * - Checkpoint alcanzado: 5 puntos
 * - Participación: 10 puntos
 */
class ScoreService(
    private val minigame: MinigameCadena,
    private val torneoManager: TorneoManager
) {
    
    /**
     * Puntos por posición (bonus adicional a la victoria).
     */
    private val bonusByPosition = mapOf(
        1 to CadenaScoreConfig.POINTS_FIRST_PLACE,
        2 to CadenaScoreConfig.POINTS_SECOND_PLACE,
        3 to CadenaScoreConfig.POINTS_THIRD_PLACE
    )
    
    /**
     * Orden de llegada por partida.
     * Key: Game ID, Value: Lista de equipos en orden de llegada
     */
    private val finishOrder = ConcurrentHashMap<UUID, MutableList<FinishRecord>>()
    
    /**
     * Registro de finalización de un equipo.
     */
    data class FinishRecord(
        val team: Team,
        val finishTime: Long,
        val position: Int
    )
    
    /**
     * Registra que un equipo completó el recorrido.
     * 
     * @param game Partida actual
     * @param team Equipo que completó
     * @return Posición en la que terminó (1, 2, 3, etc.)
     */
    fun registerFinish(game: CadenaGame, team: Team): Int {
        val records = finishOrder.getOrPut(game.id) { mutableListOf() }
        
        val position = records.size + 1
        val record = FinishRecord(
            team = team,
            finishTime = System.currentTimeMillis(),
            position = position
        )
        
        records.add(record)
        
        // Notificar a todos los jugadores
        broadcastFinish(game, team, position)
        
        // Reproducir sonido de victoria
        team.getOnlinePlayers().forEach { player ->
            player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
        }
        
        return position
    }
    
    /**
     * Obtiene los puntos totales para una posición específica (victoria + bonus).
     */
    fun getPointsForPosition(position: Int): Int {
        val victoryPoints = CadenaScoreConfig.POINTS_VICTORY
        val bonus = bonusByPosition[position] ?: 0
        return victoryPoints + bonus
    }
    
    /**
     * Otorga puntos a un jugador por alcanzar un checkpoint.
     * 
     * @param playerUUID UUID del jugador
     */
    fun awardPointsForCheckpoint(playerUUID: UUID) {
        val points = CadenaScoreConfig.POINTS_CHECKPOINT
        torneoManager.addScore(playerUUID, minigame.gameName, points, "Checkpoint alcanzado")
    }
    
    /**
     * Otorga puntos de participación a un jugador.
     * 
     * @param playerUUID UUID del jugador
     */
    fun awardPointsForParticipation(playerUUID: UUID) {
        val points = CadenaScoreConfig.POINTS_PARTICIPATION
        torneoManager.addScore(playerUUID, minigame.gameName, points, "Participación en Cadena")
    }
    
    /**
     * Otorga puntos a un jugador por completar el parkour.
     * 
     * @param playerUUID UUID del jugador
     * @param position Posición en la que terminó (1, 2, 3, etc.)
     */
    fun awardPointsForVictory(playerUUID: UUID, position: Int) {
        val totalPoints = getPointsForPosition(position)
        val positionText = when (position) {
            1 -> "1er lugar"
            2 -> "2do lugar"
            3 -> "3er lugar"
            else -> "${position}° lugar"
        }
        torneoManager.addScore(playerUUID, minigame.gameName, totalPoints, "Completó parkour - $positionText")
    }
    
    /**
     * Obtiene el orden de llegada de una partida.
     */
    fun getFinishOrder(game: CadenaGame): List<FinishRecord> {
        return finishOrder[game.id]?.toList() ?: emptyList()
    }
    
    /**
     * Calcula y asigna puntos a todos los equipos de una partida.
     * 
     * @param game Partida finalizada
     * @return Mapa de equipo a puntos asignados
     */
    fun calculateAndAssignPoints(game: CadenaGame): Map<Team, Int> {
        val records = finishOrder[game.id] ?: emptyList()
        val pointsMap = mutableMapOf<Team, Int>()
        
        records.forEach { record ->
            val points = getPointsForPosition(record.position)
            pointsMap[record.team] = points
            
            // Asignar puntos a cada jugador del equipo usando el método estandarizado
            record.team.players.forEach { playerUUID ->
                awardPointsForVictory(playerUUID, record.position)
            }
        }
        
        // Equipos que no completaron reciben puntos de participación
        game.teams.forEach { team ->
            if (!pointsMap.containsKey(team)) {
                pointsMap[team] = 0
                // Dar puntos de participación a los jugadores
                team.players.forEach { playerUUID ->
                    awardPointsForParticipation(playerUUID)
                }
            }
        }
        
        return pointsMap
    }
    
    /**
     * Verifica si todos los equipos han completado el recorrido.
     */
    fun allTeamsFinished(game: CadenaGame): Boolean {
        val finishedCount = finishOrder[game.id]?.size ?: 0
        return finishedCount >= game.teams.size
    }
    
    /**
     * Obtiene el número de equipos que han completado.
     */
    fun getFinishedTeamsCount(game: CadenaGame): Int {
        return finishOrder[game.id]?.size ?: 0
    }
    
    /**
     * Limpia los registros de una partida.
     */
    fun clearGame(gameId: String) {
        finishOrder.remove(UUID.fromString(gameId))
    }
    
    /**
     * Limpia todos los registros.
     */
    fun clearAll() {
        finishOrder.clear()
    }
    
    /**
     * Notifica a todos los jugadores que un equipo completó.
     */
    private fun broadcastFinish(game: CadenaGame, team: Team, position: Int) {
        val points = getPointsForPosition(position)
        val positionText = when (position) {
            1 -> "${ChatColor.GOLD}${ChatColor.BOLD}1er LUGAR"
            2 -> "${ChatColor.GRAY}${ChatColor.BOLD}2do LUGAR"
            3 -> "${ChatColor.GOLD}${ChatColor.BOLD}3er LUGAR"
            else -> "${ChatColor.WHITE}${ChatColor.BOLD}${position}º LUGAR"
        }
        
        val teamNames = team.getOnlinePlayers().joinToString(", ") { it.name }
        
        game.teams.forEach { t ->
            t.getOnlinePlayers().forEach { player ->
                player.sendMessage("")
                player.sendMessage("${ChatColor.GREEN}${ChatColor.BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                player.sendMessage("$positionText ${ChatColor.YELLOW}¡Equipo completó el recorrido!")
                player.sendMessage("${ChatColor.AQUA}Jugadores: ${ChatColor.WHITE}$teamNames")
                player.sendMessage("${ChatColor.GOLD}Puntos ganados: ${ChatColor.YELLOW}$points")
                player.sendMessage("${ChatColor.GREEN}${ChatColor.BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                player.sendMessage("")
            }
        }
    }
    
    /**
     * Muestra el resumen final de la partida.
     */
    fun showFinalSummary(game: CadenaGame) {
        val records = finishOrder[game.id] ?: emptyList()
        
        game.teams.forEach { team ->
            team.getOnlinePlayers().forEach { player ->
                player.sendMessage("")
                player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}═══════════════════════════════════════")
                player.sendMessage("${ChatColor.YELLOW}${ChatColor.BOLD}        RESUMEN FINAL - CADENA")
                player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}═══════════════════════════════════════")
                player.sendMessage("")
                
                if (records.isEmpty()) {
                    player.sendMessage("${ChatColor.RED}Ningún equipo completó el recorrido.")
                } else {
                    player.sendMessage("${ChatColor.AQUA}${ChatColor.BOLD}CLASIFICACIÓN:")
                    records.forEach { record ->
                        val points = getPointsForPosition(record.position)
                        val teamNames = record.team.getOnlinePlayers().joinToString(", ") { it.name }
                        val medal = when (record.position) {
                            1 -> "🥇"
                            2 -> "🥈"
                            3 -> "🥉"
                            else -> "  "
                        }
                        player.sendMessage("${ChatColor.YELLOW}$medal ${record.position}º - ${ChatColor.WHITE}$teamNames ${ChatColor.GRAY}(${points}pts)")
                    }
                }
                
                player.sendMessage("")
                
                // Mostrar equipos que no completaron
                val unfinishedTeams = game.teams.filter { t -> 
                    records.none { it.team == t }
                }
                if (unfinishedTeams.isNotEmpty()) {
                    player.sendMessage("${ChatColor.GRAY}${ChatColor.BOLD}NO COMPLETARON:")
                    unfinishedTeams.forEach { t ->
                        val teamNames = t.getOnlinePlayers().joinToString(", ") { it.name }
                        player.sendMessage("${ChatColor.GRAY}  - $teamNames")
                    }
                    player.sendMessage("")
                }
                
                player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}═══════════════════════════════════════")
                player.sendMessage("")
            }
        }
    }
}
