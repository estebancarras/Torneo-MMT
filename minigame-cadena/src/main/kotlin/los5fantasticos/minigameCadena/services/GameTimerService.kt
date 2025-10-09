package los5fantasticos.minigameCadena.services

import los5fantasticos.minigameCadena.MinigameCadena
import los5fantasticos.minigameCadena.game.CadenaGame
import los5fantasticos.minigameCadena.game.GameState
import org.bukkit.ChatColor
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestiona los temporizadores de las partidas.
 * 
 * Cada partida tiene un límite de tiempo de 5 minutos.
 * Se muestran avisos a los 3 minutos, 1 minuto, 30s, 10s y cuenta regresiva final.
 */
class GameTimerService(private val minigame: MinigameCadena) {
    
    /**
     * Duración de la partida en segundos (5 minutos).
     */
    private val gameDuration = 300 // 5 minutos
    
    /**
     * Tareas de temporizador activas por partida.
     * Key: Game ID
     */
    private val timerTasks = ConcurrentHashMap<UUID, BukkitTask>()
    
    /**
     * Tiempo restante por partida en segundos.
     * Key: Game ID
     */
    private val timeRemaining = ConcurrentHashMap<UUID, Int>()
    
    /**
     * Inicia el temporizador de una partida.
     */
    fun startTimer(game: CadenaGame) {
        // Cancelar temporizador existente si hay
        stopTimer(game)
        
        // Inicializar tiempo
        timeRemaining[game.id] = gameDuration
        
        // Crear tarea que se ejecuta cada segundo
        val task = object : Runnable {
            override fun run() {
                val remaining = timeRemaining[game.id] ?: return
                
                // Verificar si la partida sigue activa
                if (game.state != GameState.IN_GAME) {
                    stopTimer(game)
                    return
                }
                
                // Decrementar tiempo
                val newRemaining = remaining - 1
                timeRemaining[game.id] = newRemaining
                
                // Mostrar avisos en momentos clave
                when (newRemaining) {
                    180 -> notifyTime(game, "3 minutos", ChatColor.YELLOW)
                    60 -> notifyTime(game, "1 minuto", ChatColor.GOLD)
                    30 -> notifyTime(game, "30 segundos", ChatColor.RED)
                    10, 9, 8, 7, 6, 5, 4, 3, 2, 1 -> {
                        notifyCountdown(game, newRemaining)
                    }
                    0 -> {
                        // Tiempo agotado
                        onTimeUp(game)
                        stopTimer(game)
                        return
                    }
                }
            }
        }
        
        // Ejecutar cada segundo (20 ticks)
        val bukkitTask = minigame.plugin.server.scheduler.runTaskTimer(
            minigame.plugin,
            task,
            20L,
            20L
        )
        
        timerTasks[game.id] = bukkitTask
        
        // Notificar inicio
        game.teams.forEach { team ->
            team.getOnlinePlayers().forEach { player ->
                player.sendMessage("${ChatColor.AQUA}⏱ Temporizador iniciado: ${ChatColor.WHITE}5 minutos")
            }
        }
    }
    
    /**
     * Detiene el temporizador de una partida.
     */
    fun stopTimer(game: CadenaGame) {
        timerTasks.remove(game.id)?.cancel()
        timeRemaining.remove(game.id)
    }
    
    /**
     * Obtiene el tiempo restante de una partida en segundos.
     */
    fun getTimeRemaining(game: CadenaGame): Int {
        return timeRemaining[game.id] ?: 0
    }
    
    /**
     * Formatea el tiempo en formato MM:SS.
     */
    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%d:%02d", minutes, secs)
    }
    
    /**
     * Notifica tiempo restante a todos los jugadores.
     */
    private fun notifyTime(game: CadenaGame, timeText: String, color: ChatColor) {
        game.teams.forEach { team ->
            team.getOnlinePlayers().forEach { player ->
                player.sendMessage("${color}⏱ Tiempo restante: $timeText")
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
            }
        }
    }
    
    /**
     * Notifica cuenta regresiva final.
     */
    private fun notifyCountdown(game: CadenaGame, seconds: Int) {
        val color = when {
            seconds <= 3 -> ChatColor.DARK_RED
            seconds <= 5 -> ChatColor.RED
            else -> ChatColor.GOLD
        }
        
        game.teams.forEach { team ->
            team.getOnlinePlayers().forEach { player ->
                player.sendTitle(
                    "$color${ChatColor.BOLD}$seconds",
                    "${ChatColor.YELLOW}¡Apresúrense!",
                    5,
                    15,
                    5
                )
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f)
            }
        }
    }
    
    /**
     * Maneja cuando se acaba el tiempo.
     */
    private fun onTimeUp(game: CadenaGame) {
        game.teams.forEach { team ->
            team.getOnlinePlayers().forEach { player ->
                player.sendTitle(
                    "${ChatColor.RED}${ChatColor.BOLD}¡TIEMPO AGOTADO!",
                    "${ChatColor.YELLOW}Finalizando partida...",
                    10,
                    40,
                    10
                )
                player.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f)
            }
        }
        
        // Finalizar la partida
        minigame.plugin.server.scheduler.runTaskLater(minigame.plugin, Runnable {
            endGame(game)
        }, 60L) // 3 segundos después
    }
    
    /**
     * Finaliza una partida por tiempo agotado.
     */
    private fun endGame(game: CadenaGame) {
        // Cambiar estado
        game.state = GameState.FINISHED
        
        // Calcular y asignar puntos
        val scoreService = minigame.scoreService
        scoreService.calculateAndAssignPoints(game)
        
        // Mostrar resumen
        scoreService.showFinalSummary(game)
        
        // Desactivar servicios
        minigame.chainService.stopChaining(game)
        
        // Limpiar después de 10 segundos
        minigame.plugin.server.scheduler.runTaskLater(minigame.plugin, Runnable {
            minigame.gameManager.endGame(game)
        }, 200L)
    }
    
    /**
     * Limpia todos los temporizadores.
     */
    fun clearAll() {
        timerTasks.values.forEach { it.cancel() }
        timerTasks.clear()
        timeRemaining.clear()
    }
}
