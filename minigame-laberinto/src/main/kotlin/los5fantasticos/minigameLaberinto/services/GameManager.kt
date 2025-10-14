package los5fantasticos.minigameLaberinto.services

import los5fantasticos.minigameLaberinto.MinigameLaberinto
import los5fantasticos.minigameLaberinto.game.Arena
import los5fantasticos.minigameLaberinto.game.GameState
import los5fantasticos.minigameLaberinto.game.LaberintoGame
import los5fantasticos.torneo.util.GameTimer
import org.bukkit.Bukkit
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

/**
 * Gestor de partidas activas del minijuego Laberinto.
 * 
 * Responsable de:
 * - Crear y gestionar instancias de partidas
 * - Coordinar el flujo de estados (LOBBY -> COUNTDOWN -> IN_GAME -> FINISHED)
 * - Manejar temporizadores y transiciones de estado
 * - Gestionar la lógica de jumpscares y finalización
 */
class GameManager(private val minigame: MinigameLaberinto) {
    
    /**
     * Mapa de partidas activas por ID.
     */
    private val activeGames = mutableMapOf<String, LaberintoGame>()
    
    /**
     * Mapa de jugadores a sus partidas activas.
     */
    private val playerGames = mutableMapOf<UUID, LaberintoGame>()
    
    /**
     * Temporizador de cuenta regresiva para el inicio de partidas.
     */
    private var countdownTimer: GameTimer? = null
    
    /**
     * Temporizador de duración de la partida.
     */
    private var gameTimer: GameTimer? = null
    
    /**
     * Conjunto de carteles de Laberinto para actualización automática.
     */
    private val laberintoSigns = mutableSetOf<org.bukkit.block.Sign>()
    
    /**
     * Registra un cartel de Laberinto para actualización automática.
     * 
     * @param sign Cartel a registrar
     */
    fun registerLaberintoSign(sign: org.bukkit.block.Sign) {
        laberintoSigns.add(sign)
    }
    
    /**
     * Actualiza todos los carteles de Laberinto registrados.
     */
    private fun updateAllLaberintoSigns() {
        laberintoSigns.removeIf { sign ->
            try {
                // Verificar si el cartel sigue siendo válido
                if (!sign.block.type.name.contains("SIGN")) {
                    return@removeIf true
                }
                
                // Buscar la partida más relevante para mostrar
                val gameToShow = findBestGameToShow()
                if (gameToShow != null) {
                    updateSign(sign, gameToShow)
                } else {
                    // Si no hay partidas, mostrar estado por defecto
                    updateSignDefault(sign)
                }
                false
            } catch (_: Exception) {
                // Si hay error, remover el cartel de la lista
                true
            }
        }
    }
    
    /**
     * Encuentra la mejor partida para mostrar en los carteles.
     * 
     * @return La partida más apropiada para mostrar, o null si no hay ninguna
     */
    private fun findBestGameToShow(): LaberintoGame? {
        // Prioridad: IN_GAME > COUNTDOWN > LOBBY > FINISHED
        return activeGames.values
            .sortedWith(compareBy<LaberintoGame> { 
                when (it.state) {
                    GameState.IN_GAME -> 0
                    GameState.COUNTDOWN -> 1
                    GameState.LOBBY -> 2
                    GameState.FINISHED -> 3
                }
            }.thenByDescending { it.players.size })
            .firstOrNull()
    }
    
    /**
     * Actualiza un cartel con información de una partida específica.
     * 
     * @param sign Cartel a actualizar
     * @param game Partida a mostrar
     */
    private fun updateSign(sign: org.bukkit.block.Sign, game: LaberintoGame) {
        val stateText = when (game.state) {
            GameState.LOBBY -> "§a[ESPERANDO]"
            GameState.COUNTDOWN -> "§e[INICIANDO]"
            GameState.IN_GAME -> "§c[EN JUEGO]"
            GameState.FINISHED -> "§7[FINALIZANDO]"
        }
        
        val playerCount = game.players.size
        val maxPlayers = game.arena.maxPlayers
        
        // Actualizar el cartel con información del juego
        sign.setLine(0, "§6[Laberinto]")
        sign.setLine(1, stateText)
        sign.setLine(2, "§f$playerCount/$maxPlayers jugadores")
        sign.setLine(3, "§7Arena: ${game.arena.name}")
        
        sign.update()
    }
    
    /**
     * Actualiza un cartel con estado por defecto cuando no hay partidas.
     * 
     * @param sign Cartel a actualizar
     */
    private fun updateSignDefault(sign: org.bukkit.block.Sign) {
        sign.setLine(0, "§6[Laberinto]")
        sign.setLine(1, "§7[NO HAY PARTIDAS]")
        sign.setLine(2, "§f0/0 jugadores")
        sign.setLine(3, "§7Haz clic para crear")
        
        sign.update()
    }
    
    /**
     * Crea una nueva partida con la arena especificada.
     * 
     * @param arena Arena para la nueva partida
     * @return La nueva partida creada, o null si no se pudo crear
     */
    fun createNewGame(arena: Arena): LaberintoGame {
        val gameId = "laberinto_${System.currentTimeMillis()}"
        val game = LaberintoGame(arena, gameId)
        
        activeGames[gameId] = game
        
        // Actualizar carteles
        updateAllLaberintoSigns()
        
        minigame.plugin.logger.info("Nueva partida creada: $gameId con arena ${arena.name}")
        return game
    }
    
    /**
     * Añade un jugador a una partida existente.
     * 
     * @param player Jugador a añadir
     * @param gameId ID de la partida
     * @return true si se añadió exitosamente, false en caso contrario
     */
    fun addPlayerToGame(player: Player, gameId: String): Boolean {
        val game = activeGames[gameId] ?: return false
        
        if (!game.addPlayer(player)) {
            return false
        }
        
        playerGames[player.uniqueId] = game
        
        // Actualizar carteles
        updateAllLaberintoSigns()
        
        // Verificar si la partida puede iniciar
        if (game.canStart()) {
            startCountdown(game)
        }
        
        return true
    }
    
    /**
     * Remueve un jugador de su partida activa.
     * 
     * @param player Jugador a remover
     * @return true si se removió exitosamente, false si no estaba en ninguna partida
     */
    fun removePlayerFromGame(player: Player): Boolean {
        val game = playerGames[player.uniqueId] ?: return false
        
        game.removePlayer(player)
        playerGames.remove(player.uniqueId)
        
        // Actualizar carteles
        updateAllLaberintoSigns()
        
        // Si no quedan jugadores, limpiar la partida
        if (game.players.isEmpty()) {
            cleanupGame(game)
        }
        
        return true
    }
    
    /**
     * Inicia la cuenta regresiva para una partida.
     * 
     * @param game Partida a iniciar
     */
    private fun startCountdown(game: LaberintoGame) {
        if (game.state != GameState.LOBBY) {
            return
        }
        
        game.setState(GameState.COUNTDOWN)
        
        // Actualizar carteles
        updateAllLaberintoSigns()
        
        // Teletransportar jugadores al punto de inicio
        game.players.forEach { player ->
            player.teleport(game.arena.startLocation)
            player.sendMessage(Component.text("¡La partida comenzará en 10 segundos!").color(NamedTextColor.GREEN))
        }
        
        // Crear temporizador de cuenta regresiva
        countdownTimer = GameTimer(
            minigame.torneoPlugin,
            10,
            "§e§lLaberinto - Iniciando...",
            onFinish = { startGame(game) },
            onTick = { secondsLeft ->
                game.players.forEach { player ->
                    player.sendMessage(Component.text("Iniciando en $secondsLeft segundos...").color(NamedTextColor.YELLOW))
                }
            }
        )
        
        countdownTimer?.addPlayers(game.players)
        countdownTimer?.start()
        
        minigame.plugin.logger.info("Cuenta regresiva iniciada para partida ${game.gameId}")
    }
    
    /**
     * Inicia la partida después de la cuenta regresiva.
     * 
     * @param game Partida a iniciar
     */
    private fun startGame(game: LaberintoGame) {
        game.setState(GameState.IN_GAME)
        
        // Actualizar carteles
        updateAllLaberintoSigns()
        
        // Enviar mensaje de inicio
        game.players.forEach { player ->
            player.sendMessage(Component.text("¡¡¡COMIENZA EL LABERINTO!!!").color(NamedTextColor.GREEN))
            player.sendMessage(Component.text("Encuentra la salida antes de que se acabe el tiempo!").color(NamedTextColor.YELLOW))
            player.sendMessage(Component.text("¡Cuidado con los sustos!").color(NamedTextColor.RED))
        }
        
        // Crear temporizador de duración de la partida
        gameTimer = GameTimer(
            minigame.torneoPlugin,
            game.arena.gameDuration,
            "§c§lLaberinto - Tiempo restante",
            onFinish = { endGame(game) },
            onTick = { secondsLeft ->
                // Mostrar advertencias de tiempo
                when (secondsLeft) {
                    60 -> game.players.forEach { it.sendMessage(Component.text("¡Queda 1 minuto!").color(NamedTextColor.YELLOW)) }
                    30 -> game.players.forEach { it.sendMessage(Component.text("¡Quedan 30 segundos!").color(NamedTextColor.GOLD)) }
                    10 -> game.players.forEach { it.sendMessage(Component.text("¡Quedan 10 segundos!").color(NamedTextColor.RED)) }
                }
            }
        )
        
        gameTimer?.addPlayers(game.players)
        gameTimer?.start()
        
        minigame.plugin.logger.info("Partida iniciada: ${game.gameId}")
    }
    
    /**
     * Finaliza una partida y calcula puntuaciones.
     * 
     * @param game Partida a finalizar
     */
    private fun endGame(game: LaberintoGame) {
        game.setState(GameState.FINISHED)
        
        // Actualizar carteles
        updateAllLaberintoSigns()
        
        // Calcular puntuaciones
        val finishedPlayers = game.getFinishedPlayers()
        val unfinishedPlayers = game.getUnfinishedPlayers()
        
        // Otorgar puntos
        finishedPlayers.forEach { player ->
            minigame.scoreService.awardCompletionPoints(player)
        }
        
        unfinishedPlayers.forEach { player ->
            minigame.scoreService.awardParticipationPoints(player)
        }
        
        // Mostrar resultados
        showGameResults(game, finishedPlayers, unfinishedPlayers)
        
        // Teletransportar jugadores de vuelta al lobby después de un delay
        Bukkit.getScheduler().runTaskLater(minigame.plugin, Runnable {
            teleportPlayersToLobby(game)
            cleanupGame(game)
        }, 200L) // 10 segundos
        
        minigame.plugin.logger.info("Partida finalizada: ${game.gameId}")
    }
    
    /**
     * Muestra los resultados de la partida a todos los jugadores.
     * 
     * @param game Partida finalizada
     * @param finishedPlayers Jugadores que completaron el laberinto
     * @param unfinishedPlayers Jugadores que no completaron el laberinto
     */
    private fun showGameResults(game: LaberintoGame, finishedPlayers: List<Player>, unfinishedPlayers: List<Player>) {
        game.players.forEach { player ->
            player.sendMessage(Component.text("═══════════════════════════════════════").color(NamedTextColor.GOLD))
            player.sendMessage(Component.text("           RESULTADOS DEL LABERINTO").color(NamedTextColor.GOLD))
            player.sendMessage(Component.text("═══════════════════════════════════════").color(NamedTextColor.GOLD))
            
            if (finishedPlayers.isNotEmpty()) {
                player.sendMessage(Component.text("¡GANADORES! (75 puntos):").color(NamedTextColor.GREEN))
                finishedPlayers.forEach { winner ->
                    player.sendMessage(Component.text("  ✓ ${winner.name}").color(NamedTextColor.GREEN))
                }
            }
            
            if (unfinishedPlayers.isNotEmpty()) {
                player.sendMessage(Component.text("Participantes (10 puntos):").color(NamedTextColor.YELLOW))
                unfinishedPlayers.forEach { participant ->
                    player.sendMessage(Component.text("  - ${participant.name}").color(NamedTextColor.YELLOW))
                }
            }
            
            player.sendMessage(Component.text("═══════════════════════════════════════").color(NamedTextColor.GOLD))
        }
    }
    
    /**
     * Teletransporta todos los jugadores de vuelta al lobby.
     * 
     * @param game Partida
     */
    private fun teleportPlayersToLobby(game: LaberintoGame) {
        val lobbyLocation = minigame.arenaManager.getLobbyLocation()
        if (lobbyLocation != null) {
            game.players.forEach { player ->
                // Restaurar modo de juego a supervivencia
                player.gameMode = org.bukkit.GameMode.SURVIVAL
                player.teleport(lobbyLocation)
                player.sendMessage(Component.text("¡Gracias por jugar Laberinto!").color(NamedTextColor.GREEN))
            }
        }
    }
    
    /**
     * Activa un jumpscare para un jugador.
     * 
     * @param player Jugador afectado
     * @param location Ubicación del jumpscare
     */
    fun triggerJumpscare(player: Player, location: org.bukkit.Location) {
        val game = playerGames[player.uniqueId] ?: return
        
        if (!game.canActivateJumpscare(player, location)) {
            return
        }
        
        // Registrar la activación
        game.recordJumpscareActivation(player, location)
        
        // Aplicar efectos del jumpscare
        player.playSound(location, Sound.ENTITY_GHAST_SCREAM, 1.0f, 0.8f)
        player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 100, 1)) // 5 segundos
        
        // Aplicar cara de calabaza por 5 segundos
        player.inventory.helmet = org.bukkit.inventory.ItemStack(org.bukkit.Material.CARVED_PUMPKIN)
        
        // Remover cara de calabaza después de 5 segundos
        Bukkit.getScheduler().runTaskLater(minigame.plugin, Runnable {
            if (player.inventory.helmet?.type == org.bukkit.Material.CARVED_PUMPKIN) {
                player.inventory.helmet = null
            }
        }, 100L) // 5 segundos
        
        // Actualizar progreso del jugador
        val progress = game.getPlayerProgress(player)
        progress?.let { it.jumpscaresTriggered += 1 }
        
        player.sendMessage(Component.text("¡¡¡SUSTO!!!").color(NamedTextColor.RED))
        
        minigame.plugin.logger.info("Jumpscare activado para ${player.name} en ${location.x}, ${location.y}, ${location.z}")
    }
    
    /**
     * Marca a un jugador como finalizado.
     * 
     * @param player Jugador que finalizó
     */
    fun markPlayerFinished(player: Player) {
        val game = playerGames[player.uniqueId] ?: return
        
        if (game.state != GameState.IN_GAME) {
            return
        }
        
        game.markPlayerFinished(player)
        player.sendMessage(Component.text("¡¡¡FELICIDADES!!! ¡Has completado el laberinto!").color(NamedTextColor.GREEN))
        
        // Activar modo espectador
        player.gameMode = org.bukkit.GameMode.SPECTATOR
        player.sendMessage(Component.text("Ahora estás en modo espectador. ¡Puedes volar y ver a otros jugadores!").color(NamedTextColor.AQUA))
        
        // Notificar a otros jugadores
        game.players.filter { it != player }.forEach { otherPlayer ->
            otherPlayer.sendMessage(Component.text("${player.name} ha completado el laberinto!").color(NamedTextColor.YELLOW))
        }
        
        minigame.plugin.logger.info("${player.name} completó el laberinto en partida ${game.gameId}")
    }
    
    /**
     * Obtiene la partida activa de un jugador.
     * 
     * @param player Jugador
     * @return La partida del jugador, o null si no está en ninguna
     */
    fun getPlayerGame(player: Player): LaberintoGame? {
        return playerGames[player.uniqueId]
    }
    
    /**
     * Obtiene todas las partidas activas.
     * 
     * @return Lista de partidas activas
     */
    fun getActiveGames(): List<LaberintoGame> {
        return activeGames.values.toList()
    }
    
    /**
     * Limpia una partida específica.
     * 
     * @param game Partida a limpiar
     */
    private fun cleanupGame(game: LaberintoGame) {
        // Remover jugadores del mapa
        game.players.forEach { player ->
            playerGames.remove(player.uniqueId)
        }
        
        // Limpiar temporizadores
        countdownTimer?.stop()
        gameTimer?.stop()
        
        // Remover de partidas activas
        activeGames.remove(game.gameId)
        
        // Limpiar datos de la partida
        game.clear()
        
        minigame.plugin.logger.info("Partida limpiada: ${game.gameId}")
    }
    
    /**
     * Limpia todas las partidas activas.
     */
    fun clearAll() {
        activeGames.values.forEach { game ->
            cleanupGame(game)
        }
        activeGames.clear()
        playerGames.clear()
        
        countdownTimer?.stop()
        gameTimer?.stop()
        
        minigame.plugin.logger.info("Todos los juegos han sido limpiados")
    }
    
}
