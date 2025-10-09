package los5fantasticos.minigameCadena.services

import los5fantasticos.minigameCadena.MinigameCadena
import los5fantasticos.minigameCadena.game.CadenaGame
import los5fantasticos.minigameCadena.game.GameState
import org.bukkit.ChatColor
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

/**
 * Gestiona el lobby y la cuenta atrás antes de iniciar una partida.
 * 
 * Responsabilidades:
 * - Monitorear partidas en lobby
 * - Iniciar cuenta atrás cuando hay suficientes jugadores
 * - Notificar a los jugadores del progreso
 * - Iniciar la partida al finalizar la cuenta atrás
 */
class LobbyManager(
    private val plugin: MinigameCadena,
    private val gameManager: GameManager
) {
    
    /**
     * Tiempo de cuenta atrás en segundos.
     */
    private val countdownTime = 30
    
    /**
     * Tareas de cuenta atrás activas por partida.
     */
    private val countdownTasks = mutableMapOf<java.util.UUID, BukkitTask>()
    
    /**
     * Inicia el monitoreo de una partida en lobby.
     * Si hay suficientes jugadores, inicia la cuenta atrás.
     * 
     * @param game Partida a monitorear
     */
    fun checkAndStartCountdown(game: CadenaGame) {
        // Verificar que la partida esté en lobby
        if (game.state != GameState.LOBBY) {
            return
        }
        
        // Verificar si ya hay una cuenta atrás activa
        if (countdownTasks.containsKey(game.id)) {
            return
        }
        
        // Verificar si hay suficientes jugadores
        if (!game.hasMinimumPlayers()) {
            return
        }
        
        // Iniciar cuenta atrás
        startCountdown(game)
    }
    
    /**
     * Inicia la cuenta atrás para una partida.
     */
    private fun startCountdown(game: CadenaGame) {
        // Cambiar estado a COUNTDOWN
        if (!gameManager.startGame(game)) {
            return
        }
        
        // Notificar a todos los jugadores
        broadcastToGame(game, "${ChatColor.GREEN}${ChatColor.BOLD}¡La partida comenzará en $countdownTime segundos!")
        playSound(game, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
        
        // Crear tarea de cuenta atrás
        val task = object : BukkitRunnable() {
            var timeLeft = countdownTime
            
            override fun run() {
                when {
                    // Verificar si la partida sigue existiendo
                    game.state != GameState.COUNTDOWN -> {
                        cancel()
                        countdownTasks.remove(game.id)
                        return
                    }
                    
                    // Verificar si hay suficientes jugadores
                    !game.hasMinimumPlayers() -> {
                        broadcastToGame(game, "${ChatColor.RED}No hay suficientes jugadores. Cuenta atrás cancelada.")
                        game.state = GameState.LOBBY
                        cancel()
                        countdownTasks.remove(game.id)
                        return
                    }
                    
                    // Tiempo agotado - iniciar partida
                    timeLeft <= 0 -> {
                        startGameplay(game)
                        cancel()
                        countdownTasks.remove(game.id)
                        return
                    }
                    
                    // Notificaciones en momentos clave
                    timeLeft in listOf(30, 20, 10, 5, 4, 3, 2, 1) -> {
                        val color = when {
                            timeLeft <= 3 -> ChatColor.RED
                            timeLeft <= 5 -> ChatColor.GOLD
                            else -> ChatColor.YELLOW
                        }
                        broadcastToGame(game, "$color${ChatColor.BOLD}$timeLeft...")
                        
                        val pitch = when {
                            timeLeft <= 3 -> 2.0f
                            timeLeft <= 5 -> 1.5f
                            else -> 1.0f
                        }
                        playSound(game, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch)
                    }
                }
                
                timeLeft--
            }
        }
        
        // Ejecutar cada segundo
        val bukkitTask = task.runTaskTimer(plugin.getPlugin(), 0L, 20L)
        countdownTasks[game.id] = bukkitTask
    }
    
    /**
     * Inicia el gameplay de una partida.
     * TODO PR4: Teletransportar a arena y activar mecánicas de parkour
     */
    private fun startGameplay(game: CadenaGame) {
        game.state = GameState.IN_GAME
        
        // Notificar inicio
        broadcastToGame(game, "${ChatColor.GREEN}${ChatColor.BOLD}¡LA PARTIDA HA COMENZADO!")
        broadcastToGame(game, "${ChatColor.YELLOW}¡Mantén la cadena unida y completa el parkour!")
        playSound(game, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f)
        
        // PR3: Activar ChainService para esta partida
        plugin.chainService.startChaining(game)
        broadcastToGame(game, "${ChatColor.AQUA}✓ Encadenamiento activado - Distancia máxima: ${plugin.chainService.getMaxDistance()} bloques")
        
        // TODO PR4: Teletransportar jugadores al spawn de la arena
        // Por ahora, solo notificamos
        broadcastToGame(game, "${ChatColor.GRAY}[PR3] Teletransporte a arena pendiente (PR4)")
        
        // TODO PR5: Iniciar temporizador de partida
        broadcastToGame(game, "${ChatColor.GRAY}[PR3] Temporizador de partida pendiente (PR5)")
    }
    
    /**
     * Cancela la cuenta atrás de una partida.
     */
    fun cancelCountdown(game: CadenaGame) {
        val task = countdownTasks.remove(game.id)
        task?.cancel()
        
        if (game.state == GameState.COUNTDOWN) {
            game.state = GameState.LOBBY
        }
    }
    
    /**
     * Envía un mensaje a todos los jugadores de una partida.
     */
    private fun broadcastToGame(game: CadenaGame, message: String) {
        game.teams.forEach { team ->
            team.getOnlinePlayers().forEach { player ->
                player.sendMessage("${ChatColor.GOLD}[Cadena] ${ChatColor.RESET}$message")
            }
        }
    }
    
    /**
     * Reproduce un sonido para todos los jugadores de una partida.
     */
    private fun playSound(game: CadenaGame, sound: Sound, volume: Float, pitch: Float) {
        game.teams.forEach { team ->
            team.getOnlinePlayers().forEach { player ->
                player.playSound(player.location, sound, volume, pitch)
            }
        }
    }
    
    /**
     * Limpia todas las cuentas atrás activas.
     */
    fun clearAll() {
        countdownTasks.values.forEach { it.cancel() }
        countdownTasks.clear()
    }
    
    /**
     * Obtiene el plugin asociado (necesario para BukkitRunnable).
     */
    private fun MinigameCadena.getPlugin(): org.bukkit.plugin.Plugin {
        return this.plugin
    }
}
