package los5fantasticos.minigameLaberinto.listeners

import los5fantasticos.minigameLaberinto.MinigameLaberinto
import los5fantasticos.minigameLaberinto.game.GameState
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent

/**
 * Listener principal para eventos del minijuego Laberinto.
 * 
 * Maneja:
 * - Movimiento de jugadores (jumpscares y finalización)
 * - Interacciones con carteles de unión al juego
 */
class GameListener(private val minigame: MinigameLaberinto) : Listener {
    
    /**
     * Maneja el movimiento de jugadores para detectar jumpscares y finalización.
     * 
     * @param event Evento de movimiento del jugador
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val from = event.from
        val to = event.to
        
        // Solo procesar si el jugador realmente se movió
        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) {
            return
        }
        
        val game = minigame.gameManager.getPlayerGame(player) ?: return
        
        // Verificar límites de espectador si el jugador está en modo espectador
        if (player.gameMode == org.bukkit.GameMode.SPECTATOR && game.hasPlayerFinished(player)) {
            if (!game.arena.isWithinSpectatorBounds(to)) {
                // Teletransportar de vuelta al centro del área
                val center = game.arena.startLocation
                player.teleport(center)
                player.sendMessage(Component.text("No puedes salir del área del minijuego!").color(NamedTextColor.RED))
                return
            }
        }
        
        // Solo procesar en estado IN_GAME
        if (game.state != GameState.IN_GAME) {
            return
        }
        
        // Verificar si el jugador ya finalizó
        if (game.hasPlayerFinished(player)) {
            return
        }
        
        // Verificar finalización del laberinto
        if (game.arena.isInFinishRegion(to)) {
            minigame.gameManager.markPlayerFinished(player)
            return
        }
        
        // Verificar jumpscares
        val nearestJumpscare = game.arena.getNearestJumpscareLocation(to)
        if (nearestJumpscare != null) {
            minigame.gameManager.triggerJumpscare(player, nearestJumpscare)
        }
    }
    
    /**
     * Maneja las interacciones con carteles para unirse al juego.
     * 
     * @param event Evento de interacción del jugador
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val clickedBlock = event.clickedBlock ?: return
        
        // Verificar si es un cartel con el texto [Laberinto]
        if (clickedBlock.type.name.contains("SIGN")) {
            val sign = clickedBlock.state as? org.bukkit.block.Sign ?: return
            
            val firstLine = sign.getLine(0)
            if (firstLine.contains("[Laberinto]", ignoreCase = true)) {
                event.isCancelled = true
                
                // Registrar el cartel para actualización automática
                minigame.gameManager.registerLaberintoSign(sign)
                
                // Verificar si el jugador ya está en una partida
                if (minigame.gameManager.getPlayerGame(player) != null) {
                    player.sendMessage(Component.text("Ya estás en una partida de Laberinto!").color(NamedTextColor.RED))
                    return
                }
                
                // Buscar una partida disponible o crear una nueva
                val availableGame = findAvailableGame()
                if (availableGame != null) {
                    if (minigame.gameManager.addPlayerToGame(player, availableGame.gameId)) {
                        player.sendMessage(Component.text("¡Te has unido a una partida de Laberinto!").color(NamedTextColor.GREEN))
                        player.sendMessage(Component.text("Jugadores en la partida: ${availableGame.players.size}/${availableGame.arena.maxPlayers}").color(NamedTextColor.YELLOW))
                        
                        if (availableGame.canStart()) {
                            player.sendMessage(Component.text("¡La partida comenzará pronto!").color(NamedTextColor.GREEN))
                        } else {
                            val needed = availableGame.arena.minPlayers - availableGame.players.size
                            player.sendMessage(Component.text("Se necesitan $needed jugadores más para comenzar").color(NamedTextColor.YELLOW))
                        }
                        
                        // Actualizar el cartel
                        updateSign(sign, availableGame)
                    } else {
                        player.sendMessage(Component.text("No se pudo unir a la partida. Puede estar llena.").color(NamedTextColor.RED))
                    }
                } else {
                    // Crear una nueva partida automáticamente desde el cartel
                    val availableArenas = minigame.arenaManager.getAllArenas().filter { it.isComplete() }
                    if (availableArenas.isNotEmpty()) {
                        val arena = availableArenas.first()
                        val newGame = minigame.gameManager.createNewGame(arena)
                        if (minigame.gameManager.addPlayerToGame(player, newGame.gameId)) {
                            player.sendMessage(Component.text("¡Te has unido a una nueva partida de Laberinto!").color(NamedTextColor.GREEN))
                            player.sendMessage(Component.text("Arena: ${arena.name}").color(NamedTextColor.YELLOW))
                            player.sendMessage(Component.text("Jugadores: 1/${arena.maxPlayers}").color(NamedTextColor.YELLOW))
                            
                            val needed = arena.minPlayers - 1
                            player.sendMessage(Component.text("Se necesitan $needed jugadores más para comenzar").color(NamedTextColor.YELLOW))
                            
                            // Actualizar el cartel
                            updateSign(sign, newGame)
                        } else {
                            player.sendMessage(Component.text("No se pudo crear una nueva partida.").color(NamedTextColor.RED))
                        }
                    } else {
                        player.sendMessage(Component.text("No hay arenas configuradas. Usa /laberinto admin create para crear una arena.").color(NamedTextColor.RED))
                    }
                }
            }
        }
    }
    
    /**
     * Maneja la rotura de bloques para prevenir que los jugadores rompan bloques durante el juego.
     * 
     * @param event Evento de rotura de bloque
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val game = minigame.gameManager.getPlayerGame(player) ?: return
        
        // Solo prevenir rotura de bloques durante el juego
        if (game.state == GameState.IN_GAME) {
            event.isCancelled = true
            player.sendMessage(Component.text("No puedes romper bloques durante el juego!").color(NamedTextColor.RED))
        }
    }
    
    /**
     * Actualiza un cartel con información de la partida.
     * 
     * @param sign Cartel a actualizar
     * @param game Partida asociada
     */
    private fun updateSign(sign: org.bukkit.block.Sign, game: los5fantasticos.minigameLaberinto.game.LaberintoGame) {
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
     * Busca una partida disponible para unirse.
     * 
     * @return Una partida disponible, o null si no hay ninguna
     */
    private fun findAvailableGame(): los5fantasticos.minigameLaberinto.game.LaberintoGame? {
        return minigame.gameManager.getActiveGames()
            .firstOrNull { it.state == GameState.LOBBY && it.players.size < it.arena.maxPlayers }
    }
}
