package los5fantasticos.minigameCadena.services

import los5fantasticos.minigameCadena.MinigameCadena
import los5fantasticos.minigameCadena.game.Arena
import los5fantasticos.minigameCadena.game.CadenaGame
import los5fantasticos.minigameCadena.game.GameState
import los5fantasticos.minigameCadena.game.Team
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player

/**
 * Servicio que gestiona la lógica de parkour del minijuego Cadena.
 * 
 * Responsabilidades:
 * - Detectar caídas de jugadores
 * - Detectar cuando un jugador alcanza un checkpoint
 * - Reiniciar equipos al último checkpoint
 * - Detectar cuando un equipo completa el parkour
 */
class ParkourService(private val minigame: MinigameCadena) {
    
    /**
     * Verifica si un jugador ha caído por debajo de la altura mínima.
     * Si es así, reinicia todo el equipo al último checkpoint.
     * 
     * @param player Jugador a verificar
     * @param game Partida actual
     */
    fun checkFall(player: Player, game: CadenaGame) {
        val arena = game.arena ?: return
        
        // Verificar si el jugador cayó
        if (!arena.isBelowMinHeight(player.location)) {
            return
        }
        
        // Obtener el equipo del jugador
        val team = minigame.gameManager.getPlayerTeam(player) ?: return
        
        // Reiniciar todo el equipo
        respawnTeam(team, game, arena)
        
        // Notificar
        team.getOnlinePlayers().forEach { p ->
            p.sendMessage("${ChatColor.RED}¡${player.name} cayó! El equipo ha sido reiniciado.")
            p.playSound(p.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f)
        }
    }
    
    /**
     * Verifica si un jugador ha alcanzado un checkpoint.
     * 
     * @param player Jugador a verificar
     * @param game Partida actual
     */
    fun checkCheckpoint(player: Player, game: CadenaGame) {
        val arena = game.arena ?: return
        val team = minigame.gameManager.getPlayerTeam(player) ?: return
        
        // Obtener el checkpoint actual del equipo
        val currentCheckpoint = game.teamCheckpoints.getOrDefault(team.id, -1)
        
        // Verificar el siguiente checkpoint
        val nextCheckpointIndex = currentCheckpoint + 1
        
        // Si ya alcanzaron todos los checkpoints, verificar la meta
        if (nextCheckpointIndex >= arena.getCheckpointCount()) {
            checkFinish(player, game)
            return
        }
        
        // Obtener la ubicación del siguiente checkpoint
        val checkpointLocation = arena.getCheckpoint(nextCheckpointIndex) ?: return
        
        // Verificar si el jugador está cerca del checkpoint
        if (!arena.isNearCheckpoint(player.location, checkpointLocation)) {
            return
        }
        
        // Verificar si TODO el equipo está cerca del checkpoint
        val allNear = team.getOnlinePlayers().all { p ->
            arena.isNearCheckpoint(p.location, checkpointLocation)
        }
        
        if (!allNear) {
            // Notificar que deben estar todos juntos
            player.sendMessage("${ChatColor.YELLOW}¡Espera a tu equipo! Todos deben llegar juntos al checkpoint.")
            return
        }
        
        // Actualizar checkpoint del equipo
        game.teamCheckpoints[team.id] = nextCheckpointIndex
        
        // Notificar al equipo
        team.getOnlinePlayers().forEach { p ->
            p.sendMessage("${ChatColor.GREEN}${ChatColor.BOLD}✓ Checkpoint ${nextCheckpointIndex + 1}/${arena.getCheckpointCount()} alcanzado!")
            p.playSound(p.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f)
        }
    }
    
    /**
     * Verifica si un equipo ha alcanzado la meta.
     * 
     * @param player Jugador a verificar
     * @param game Partida actual
     */
    private fun checkFinish(player: Player, game: CadenaGame) {
        val arena = game.arena ?: return
        val team = minigame.gameManager.getPlayerTeam(player) ?: return
        
        // Verificar si el jugador está cerca de la meta
        if (!arena.isNearCheckpoint(player.location, arena.finishLocation)) {
            return
        }
        
        // Verificar si TODO el equipo está cerca de la meta
        val allNear = team.getOnlinePlayers().all { p ->
            arena.isNearCheckpoint(p.location, arena.finishLocation)
        }
        
        if (!allNear) {
            // Notificar que deben estar todos juntos
            player.sendMessage("${ChatColor.YELLOW}¡Espera a tu equipo! Todos deben cruzar la meta juntos.")
            return
        }
        
        // PR5: Registrar finalización y asignar puntos
        val position = minigame.scoreService.registerFinish(game, team)
        val points = minigame.scoreService.getPointsForPosition(position)
        
        // Verificar si todos los equipos han terminado
        if (minigame.scoreService.allTeamsFinished(game)) {
            // Finalizar la partida
            finishGame(game)
        }
    }
    
    /**
     * Reinicia un equipo a su último checkpoint o al spawn inicial.
     * 
     * @param team Equipo a reiniciar
     * @param game Partida actual
     * @param arena Arena de la partida
     */
    private fun respawnTeam(team: Team, game: CadenaGame, arena: Arena) {
        // Obtener el último checkpoint del equipo
        val checkpointIndex = game.teamCheckpoints.getOrDefault(team.id, -1)
        
        // Determinar ubicación de respawn
        val respawnLocation = if (checkpointIndex == -1) {
            // Respawn inicial
            arena.spawnLocation
        } else {
            // Último checkpoint
            arena.getCheckpoint(checkpointIndex) ?: arena.spawnLocation
        }
        
        // Teletransportar a todos los jugadores del equipo
        team.getOnlinePlayers().forEach { player ->
            player.teleport(respawnLocation)
            
            // Resetear velocidad para evitar que sigan cayendo
            player.velocity = org.bukkit.util.Vector(0, 0, 0)
            
            // Efecto visual
            player.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)
        }
    }
    
    /**
     * Teletransporta un equipo a una ubicación específica.
     * 
     * @param team Equipo a teletransportar
     * @param location Ubicación destino
     */
    fun teleportTeam(team: Team, location: Location) {
        team.getOnlinePlayers().forEach { player ->
            player.teleport(location)
            player.velocity = org.bukkit.util.Vector(0, 0, 0)
        }
    }
    
    /**
     * Teletransporta todos los equipos de una partida al spawn de la arena.
     * 
     * @param game Partida actual
     */
    fun teleportAllTeamsToSpawn(game: CadenaGame) {
        val arena = game.arena ?: return
        
        game.teams.forEach { team ->
            teleportTeam(team, arena.spawnLocation)
            
            // Inicializar checkpoint del equipo
            game.teamCheckpoints[team.id] = -1
        }
    }
    
    /**
     * Finaliza una partida cuando todos los equipos han completado o se acabó el tiempo.
     */
    private fun finishGame(game: CadenaGame) {
        // Cambiar estado
        game.state = GameState.FINISHED
        
        // Detener temporizador
        minigame.gameTimerService.stopTimer(game)
        
        // Calcular y asignar puntos
        minigame.scoreService.calculateAndAssignPoints(game)
        
        // Mostrar resumen
        minigame.scoreService.showFinalSummary(game)
        
        // Desactivar servicios
        minigame.chainService.stopChaining(game)
        
        // Registrar partidas jugadas para todos los jugadores
        game.teams.forEach { team ->
            team.getOnlinePlayers().forEach { player ->
                minigame.torneoPlugin.torneoManager.recordGamePlayed(player, minigame.gameName)
            }
        }
        
        // Limpiar después de 10 segundos
        minigame.plugin.server.scheduler.runTaskLater(minigame.plugin, Runnable {
            minigame.gameManager.endGame(game)
        }, 200L)
    }
}
