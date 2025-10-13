package los5fantasticos.minigameCadena.services

import los5fantasticos.minigameCadena.MinigameCadena
import los5fantasticos.minigameCadena.game.CadenaGame
import los5fantasticos.minigameCadena.game.GameState
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
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
        broadcastToGame(game, Component.text("¡La partida comenzará en $countdownTime segundos!", NamedTextColor.GREEN, TextDecoration.BOLD))
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
                        broadcastToGame(game, Component.text("No hay suficientes jugadores. Cuenta atrás cancelada.", NamedTextColor.RED))
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
                            timeLeft <= 3 -> NamedTextColor.RED
                            timeLeft <= 5 -> NamedTextColor.GOLD
                            else -> NamedTextColor.YELLOW
                        }
                        broadcastToGame(game, Component.text("$timeLeft...", color, TextDecoration.BOLD))
                        
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
     */
    private fun startGameplay(game: CadenaGame) {
        game.state = GameState.IN_GAME
        
        // Limpiar inventarios de todos los jugadores (quitar lanas de selección)
        game.teams.forEach { team ->
            team.getOnlinePlayers().forEach { player ->
                player.inventory.clear()
            }
        }
        
        // Notificar inicio
        broadcastToGame(game, Component.text("¡LA PARTIDA HA COMENZADO!", NamedTextColor.GREEN, TextDecoration.BOLD))
        broadcastToGame(game, Component.text("¡Mantén la cadena unida y completa el parkour!", NamedTextColor.YELLOW))
        playSound(game, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f)
        
        // PR4: Teletransportar jugadores al spawn de la arena
        if (game.arena != null) {
            plugin.parkourService.teleportAllTeamsToSpawn(game)
            broadcastToGame(game, Component.text("✓ Teletransportados al spawn de la arena", NamedTextColor.AQUA))
        } else {
            // Si no hay arena configurada, usar ubicación actual como arena temporal
            broadcastToGame(game, Component.text("⚠ No hay arena configurada - Usando ubicación actual", NamedTextColor.YELLOW))
            broadcastToGame(game, Component.text("Usa /cadena admin para configurar una arena", NamedTextColor.GRAY))
        }
        
        // PR3: Activar ChainService para esta partida
        plugin.chainService.startChaining(game)
        broadcastToGame(game, Component.text("✓ Encadenamiento activado - Distancia máxima: ${plugin.chainService.maxDistance} bloques", NamedTextColor.AQUA))
        
        // Crear e iniciar temporizador visual con BossBar
        startGameTimer(game)
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
    private fun broadcastToGame(game: CadenaGame, message: Component) {
        val fullMessage = Component.text("[Cadena] ", NamedTextColor.GOLD).append(message)
        game.teams.forEach { team ->
            team.getOnlinePlayers().forEach { player ->
                player.sendMessage(fullMessage)
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
     * Inicia el temporizador visual de la partida.
     */
    private fun startGameTimer(game: CadenaGame) {
        // Crear temporizador de 5 minutos (300 segundos)
        val timer = los5fantasticos.torneo.util.GameTimer(
            plugin = plugin.torneoPlugin,
            durationInSeconds = 60, // ⬅️ CAMBIA ESTE VALOR PARA PRUEBAS
            title = "§6§l⏱ Parkour en Cadena",
            onFinish = {
                // Cuando el tiempo se agota, finalizar la partida
                handleTimeUp(game)
            },
            onTick = { secondsLeft ->
                // Reproducir sonido en los últimos 10 segundos
                if (secondsLeft <= 10 && secondsLeft > 0) {
                    playSound(game, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f)
                }
            }
        )
        
        // Añadir todos los jugadores al temporizador
        game.teams.forEach { team ->
            team.getOnlinePlayers().forEach { player ->
                timer.addPlayer(player)
            }
        }
        
        // Guardar referencia y iniciar
        game.gameTimer = timer
        timer.start()
        
        broadcastToGame(game, Component.text("✓ Temporizador iniciado - Tiempo límite: 5 minutos", NamedTextColor.AQUA))
    }
    
    /**
     * Maneja cuando se acaba el tiempo de una partida.
     */
    private fun handleTimeUp(game: CadenaGame) {
        // Verificar que la partida sigue activa
        if (game.state != GameState.IN_GAME) {
            return
        }
        
        broadcastToGame(game, Component.empty())
        broadcastToGame(game, Component.text("⏰ ¡TIEMPO AGOTADO!", NamedTextColor.RED, TextDecoration.BOLD))
        broadcastToGame(game, Component.text("La partida ha terminado por límite de tiempo.", NamedTextColor.YELLOW))
        broadcastToGame(game, Component.empty())
        
        // Reproducir sonido de finalización
        playSound(game, Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f)
        
        // Finalizar la partida a través del ParkourService
        plugin.parkourService.handleTimeUp(game)
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
