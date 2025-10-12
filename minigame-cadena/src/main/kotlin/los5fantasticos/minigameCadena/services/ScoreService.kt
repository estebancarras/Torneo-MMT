package los5fantasticos.minigameCadena.services

import los5fantasticos.minigameCadena.MinigameCadena
import los5fantasticos.minigameCadena.game.CadenaGame
import los5fantasticos.minigameCadena.game.Team
import los5fantasticos.torneo.core.TorneoManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
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
 * - Lee configuraciones desde cadena.yml
 * 
 * Sistema de puntos (configurable en cadena.yml):
 * - Victoria (completar parkour): puntuacion.victoria + bonus por posición
 * - 1er lugar: +puntuacion.primer-lugar
 * - 2do lugar: +puntuacion.segundo-lugar
 * - 3er lugar: +puntuacion.tercer-lugar
 * - Checkpoint alcanzado: puntuacion.checkpoint
 * - Participación: puntuacion.participacion
 */
class ScoreService(
    private val minigame: MinigameCadena,
    private val torneoManager: TorneoManager
) {
    
    /**
     * Puntos por posición (bonus adicional a la victoria).
     * Leído dinámicamente desde cadena.yml.
     */
    private fun getBonusByPosition(position: Int): Int {
        return when (position) {
            1 -> minigame.plugin.config.getInt("puntuacion.primer-lugar", 50)
            2 -> minigame.plugin.config.getInt("puntuacion.segundo-lugar", 30)
            3 -> minigame.plugin.config.getInt("puntuacion.tercer-lugar", 15)
            else -> 0
        }
    }
    
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
        val victoryPoints = minigame.plugin.config.getInt("puntuacion.victoria", 100)
        val bonus = getBonusByPosition(position)
        return victoryPoints + bonus
    }
    
    /**
     * Otorga puntos a un jugador por alcanzar un checkpoint.
     * 
     * @param playerUUID UUID del jugador
     */
    fun awardPointsForCheckpoint(playerUUID: UUID) {
        val points = minigame.plugin.config.getInt("puntuacion.checkpoint", 5)
        torneoManager.addScore(playerUUID, minigame.gameName, points, "Checkpoint alcanzado")
    }
    
    /**
     * Otorga puntos de participación a un jugador.
     * 
     * @param playerUUID UUID del jugador
     */
    fun awardPointsForParticipation(playerUUID: UUID) {
        val points = minigame.plugin.config.getInt("puntuacion.participacion", 10)
        torneoManager.addScore(playerUUID, minigame.gameName, points, "Participación en Cadena")
    }
    
    /**
     * Otorga puntos a un jugador por completar el parkour.
     * 
     * @param playerUUID UUID del jugador
     * @param position Posición en la que terminó (1, 2, 3, etc.)
     */
    fun awardPointsForVictory(playerUUID: UUID, position: Int) {
        val points = getPointsForPosition(position)
        val positionText = when (position) {
            1 -> "1er lugar"
            2 -> "2do lugar"
            3 -> "3er lugar"
            else -> "${position}° lugar"
        }
        torneoManager.addScore(playerUUID, minigame.gameName, points, "Completó parkour en posición $positionText")
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
        val positionComponent = when (position) {
            1 -> Component.text("1er LUGAR", NamedTextColor.GOLD, TextDecoration.BOLD)
            2 -> Component.text("2do LUGAR", NamedTextColor.GRAY, TextDecoration.BOLD)
            3 -> Component.text("3er LUGAR", NamedTextColor.GOLD, TextDecoration.BOLD)
            else -> Component.text("${position}º LUGAR", NamedTextColor.WHITE, TextDecoration.BOLD)
        }
        
        val teamNames = team.getOnlinePlayers().joinToString(", ") { it.name }
        
        game.teams.forEach { t ->
            t.getOnlinePlayers().forEach { player ->
                player.sendMessage(Component.empty())
                player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GREEN, TextDecoration.BOLD))
                player.sendMessage(positionComponent
                    .append(Component.space())
                    .append(Component.text("¡Equipo completó el recorrido!", NamedTextColor.YELLOW)))
                player.sendMessage(Component.text("Jugadores: ", NamedTextColor.AQUA)
                    .append(Component.text(teamNames, NamedTextColor.WHITE)))
                player.sendMessage(Component.text("Puntos ganados: ", NamedTextColor.GOLD)
                    .append(Component.text(points.toString(), NamedTextColor.YELLOW)))
                player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GREEN, TextDecoration.BOLD))
                player.sendMessage(Component.empty())
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
                player.sendMessage(Component.empty())
                player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
                player.sendMessage(Component.text("        RESUMEN FINAL - CADENA", NamedTextColor.YELLOW, TextDecoration.BOLD))
                player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
                player.sendMessage(Component.empty())
                
                if (records.isEmpty()) {
                    player.sendMessage(Component.text("Ningún equipo completó el recorrido.", NamedTextColor.RED))
                } else {
                    player.sendMessage(Component.text("CLASIFICACIÓN:", NamedTextColor.AQUA, TextDecoration.BOLD))
                    records.forEach { record ->
                        val points = getPointsForPosition(record.position)
                        val teamNames = record.team.getOnlinePlayers().joinToString(", ") { it.name }
                        val medal = when (record.position) {
                            1 -> "🥇"
                            2 -> "🥈"
                            3 -> "🥉"
                            else -> "  "
                        }
                        player.sendMessage(Component.text("$medal ${record.position}º - ", NamedTextColor.YELLOW)
                            .append(Component.text("$teamNames ", NamedTextColor.WHITE))
                            .append(Component.text("(${points}pts)", NamedTextColor.GRAY)))
                    }
                }
                
                player.sendMessage(Component.empty())
                
                // Mostrar equipos que no completaron
                val unfinishedTeams = game.teams.filter { t -> 
                    records.none { it.team == t }
                }
                if (unfinishedTeams.isNotEmpty()) {
                    player.sendMessage(Component.text("NO COMPLETARON:", NamedTextColor.GRAY, TextDecoration.BOLD))
                    unfinishedTeams.forEach { t ->
                        val teamNames = t.getOnlinePlayers().joinToString(", ") { it.name }
                        player.sendMessage(Component.text("  - $teamNames", NamedTextColor.GRAY))
                    }
                    player.sendMessage(Component.empty())
                }
                
                player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
                player.sendMessage(Component.empty())
            }
        }
    }
}
