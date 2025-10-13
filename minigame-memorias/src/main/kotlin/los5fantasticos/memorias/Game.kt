package los5fantasticos.memorias

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.time.Duration

/**
 * Representa una instancia de juego de Memorias - Encuentra los pares.
 * 
 * MECÁNICA SIMPLIFICADA:
 * - Todos los jugadores juegan simultáneamente
 * - Cada jugador tiene máximo 4 intentos (4 pares de clics)
 * - El tablero se genera al inicio y permanece durante todo el juego
 * - El jugador que encuentre más pares gana
 */
class Game(
    private val gameManager: GameManager,
    private val memoriasManager: MemoriasManager,
    private val arena: Arena,
    private val gridSize: Int = 5
) {
    val players = mutableListOf<Player>()
    private val playerScores = mutableMapOf<Player, Int>() // Pares encontrados por jugador
    private val playerAttempts = mutableMapOf<Player, Int>() // Intentos usados (máximo 4)
    private val maxAttempts = 4 // Máximo 4 intentos por jugador
    
    // Sistema de turnos alternados
    private var currentPlayerIndex = 0
    private var isWaitingForSecondClick = false
    private var firstSelectedBlock: BoardTile? = null
    private var secondSelectedBlock: BoardTile? = null
    
    // Tablero de juego (NUNCA se elimina durante el juego)
    private val board = mutableListOf<BoardTile>()
    private val hiddenMaterial = Material.GRAY_WOOL
    private var gameStarted = false
    private var turnTimeLeft = 30 // Tiempo por turno (30 segundos)
    private var gameTimer: org.bukkit.scheduler.BukkitTask? = null
    
    // Colores disponibles para los pares
    private val availableColors = listOf(
        Material.RED_WOOL,
        Material.BLUE_WOOL,
        Material.GREEN_WOOL,
        Material.YELLOW_WOOL,
        Material.LIME_WOOL,
        Material.ORANGE_WOOL,
        Material.PINK_WOOL,
        Material.PURPLE_WOOL
    )
    
    /**
     * Representa una casilla del tablero.
     */
    data class BoardTile(
        val location: Location,
        val colorMaterial: Material, // Color real del bloque
        var isRevealed: Boolean = false,
        val pairId: Int
    )
    
    fun addPlayer(player: Player) {
        if (!players.contains(player)) {
            players.add(player)
            playerScores[player] = 0
            playerAttempts[player] = 0
            player.teleport(arena.spawnLocation)
        }
    }
    
    /**
     * Obtiene el jugador actual del turno.
     */
    private fun getCurrentPlayer(): Player? {
        if (players.isEmpty()) return null
        
        // Asegurar que el índice esté dentro del rango
        if (currentPlayerIndex >= players.size) {
            currentPlayerIndex = 0
        }
        
        return players[currentPlayerIndex]
    }
    
    /**
     * Avanza al siguiente turno.
     */
    private fun nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
        isWaitingForSecondClick = false
        firstSelectedBlock = null
        secondSelectedBlock = null
        resetTurnTimer() // Reiniciar el timer para el nuevo turno
    }
    
    fun removePlayer(player: Player) {
        val wasCurrentPlayer = (getCurrentPlayer() == player)
        
        players.remove(player)
        playerScores.remove(player)
        playerAttempts.remove(player)
        
        // Limpiar selección si era el jugador actual
        if (wasCurrentPlayer) {
            resetSelection()
        }
        
        // Ajustar el índice del jugador actual si es necesario
        if (currentPlayerIndex >= players.size) {
            currentPlayerIndex = 0
        }
        
        // Verificar si queda solo un jugador
        if (gameStarted && players.size == 1) {
            val winner = players.first()
            val msg = Component.text("¡${player.name} se desconectó! Ganaste por abandono.", NamedTextColor.GOLD, TextDecoration.BOLD)
            winner.sendMessage(msg)
            endGameWithWinner(winner)
            return
        }
        
        // Si no hay suficientes jugadores, terminar el juego
        if (gameStarted && players.size < 2) {
            val msg = Component.text("No hay suficientes jugadores. El juego se ha cancelado.", NamedTextColor.RED)
            players.forEach { it.sendMessage(msg) }
            endGame()
            return
        }
        
        // Si era el jugador actual y quedan jugadores, pasar al siguiente
        if (wasCurrentPlayer && players.isNotEmpty()) {
            val nextPlayer = getCurrentPlayer()
            if (nextPlayer != null) {
                val msg = Component.text("Turno de: ${nextPlayer.name}", NamedTextColor.YELLOW, TextDecoration.BOLD)
                players.forEach { p -> p.sendMessage(msg) }
            }
        }
        
        // Teleportar al lobby si existe
        gameManager.getLobbyLocation()?.let { player.teleport(it) }
    }
    
    /**
     * Inicia el juego con cuenta regresiva.
     */
    fun startGame() {
        // Generar el tablero PRIMERO
        generateBoard()
        
        // Iniciar cuenta regresiva
        var countdown = 3
        
        object : BukkitRunnable() {
            override fun run() {
                when (countdown) {
                    3, 2, 1 -> {
                        players.forEach { player ->
                            val mainTitle = Component.text("$countdown", NamedTextColor.GOLD, TextDecoration.BOLD)
                            val subtitle = Component.text("Preparándose...", NamedTextColor.YELLOW)
                            val times = Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(750), Duration.ofMillis(250))
                            player.showTitle(Title.title(mainTitle, subtitle, times))
                            player.playSound(player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f)
                        }
                        countdown--
                    }
                    0 -> {
                        players.forEach { player ->
                            val mainTitle = Component.text("¡COMENZÓ!", NamedTextColor.GREEN, TextDecoration.BOLD)
                            val subtitle = Component.text("¡Encuentra los pares! (4 intentos)", NamedTextColor.YELLOW)
                            val times = Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(1), Duration.ofMillis(250))
                            player.showTitle(Title.title(mainTitle, subtitle, times))
                            player.playSound(player.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
                            player.sendMessage(Component.text("¡El juego de Memorias ha comenzado!", NamedTextColor.GREEN, TextDecoration.BOLD))
                            player.sendMessage(Component.text("Tienes 4 intentos para encontrar los pares.", NamedTextColor.YELLOW))
                            player.sendMessage(Component.text("Haz clic derecho en un bloque gris para revelarlo.", NamedTextColor.YELLOW))
                        }
                        
                        // Anunciar el primer turno
                        val firstPlayer = getCurrentPlayer()
                        if (firstPlayer != null) {
                            val msg = Component.text("Turno de: ${firstPlayer.name}", NamedTextColor.YELLOW, TextDecoration.BOLD)
                            players.forEach { player -> player.sendMessage(msg) }
                        }
                        
                        // Iniciar el juego
                        gameStarted = true
                        startGameTimer()
                        cancel()
                    }
                    else -> cancel()
                }
            }
        }.runTaskTimer(gameManager.plugin, 0L, 20L)
    }
    
    /**
     * Genera el tablero de juego con pares de colores.
     */
    private fun generateBoard() {
        board.clear()
        
        val totalTiles = gridSize * gridSize
        val pairsNeeded = totalTiles / 2
        
        // Crear lista de pares
        val pairs = mutableListOf<Int>()
        for (i in 0 until pairsNeeded) {
            pairs.add(i)
            pairs.add(i)
        }
        
        // Si hay un número impar de casillas, agregar una más
        if (totalTiles % 2 != 0) {
            pairs.add(pairsNeeded)
        }
        
        pairs.shuffle()
        
        // Generar el tablero centrado en la ubicación
        val centerLocation = arena.tableroLocation.clone()
        val offset = (gridSize - 1) / 2.0
        var pairIndex = 0
        
        for (x in 0 until gridSize) {
            for (z in 0 until gridSize) {
                if (pairIndex >= pairs.size) break
                
                val tileLocation = centerLocation.clone().add(
                    (x - offset), 0.0, (z - offset)
                )
                
                val colorMaterial = availableColors[pairs[pairIndex] % availableColors.size]
                
                val tile = BoardTile(
                    location = tileLocation,
                    colorMaterial = colorMaterial,
                    isRevealed = false,
                    pairId = pairs[pairIndex]
                )
                
                board.add(tile)
                
                // Colocar bloque gris inicialmente
                tileLocation.block.type = hiddenMaterial
                
                pairIndex++
            }
        }
    }
    
    /**
     * Inicia el timer del juego que actualiza el hotbar.
     */
    private fun startGameTimer() {
        gameTimer?.cancel()
        gameTimer = object : BukkitRunnable() {
            override fun run() {
                if (!gameStarted || players.isEmpty()) {
                    cancel()
                    return
                }
                
                turnTimeLeft--
                updateHotbar()
                
                // Si se agotó el tiempo del turno
                if (turnTimeLeft <= 0) {
                    val currentPlayer = getCurrentPlayer()
                    if (currentPlayer != null) {
                        val timeoutMsg = Component.text("¡Se agotó el tiempo del turno de ${currentPlayer.name}!", NamedTextColor.RED)
                        players.forEach { p -> p.sendMessage(timeoutMsg) }
                        
                        // Pasar al siguiente turno
                        nextTurn()
                        resetTurnTimer()
                        
                        // Anunciar el siguiente turno
                        val nextPlayer = getCurrentPlayer()
                        if (nextPlayer != null) {
                            val turnMsg = Component.text("Turno de: ${nextPlayer.name}", NamedTextColor.YELLOW, TextDecoration.BOLD)
                            players.forEach { p -> p.sendMessage(turnMsg) }
                        }
                    }
                }
            }
        }.runTaskTimer(gameManager.plugin, 0L, 20L)
    }
    
    /**
     * Reinicia el timer del turno.
     */
    private fun resetTurnTimer() {
        turnTimeLeft = 30 // 30 segundos por turno
    }
    
    /**
     * Actualiza el hotbar de todos los jugadores.
     */
    private fun updateHotbar() {
        val currentPlayer = getCurrentPlayer()
        players.forEach { player ->
            val attempts = playerAttempts[player] ?: 0
            val score = playerScores[player] ?: 0
            val attemptsLeft = maxAttempts - attempts
            
            val attemptsColor = when {
                attemptsLeft > 2 -> NamedTextColor.GREEN
                attemptsLeft > 0 -> NamedTextColor.YELLOW
                else -> NamedTextColor.RED
            }
            
            val turnIndicator = if (player == currentPlayer) {
                Component.text("TU TURNO", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .append(Component.text(" | ", NamedTextColor.GRAY))
            } else {
                Component.text("Esperando...", NamedTextColor.GRAY)
                    .append(Component.text(" | ", NamedTextColor.GRAY))
            }
            
            val timeColor = when {
                turnTimeLeft > 20 -> NamedTextColor.GREEN
                turnTimeLeft > 10 -> NamedTextColor.YELLOW
                else -> NamedTextColor.RED
            }
            
            val message = turnIndicator
                .append(Component.text("Tiempo: ", timeColor, TextDecoration.BOLD))
                .append(Component.text("${turnTimeLeft}s", NamedTextColor.WHITE))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.text("Pares: ", NamedTextColor.AQUA))
                .append(Component.text("$score", NamedTextColor.WHITE))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.text("Intentos: ", attemptsColor, TextDecoration.BOLD))
                .append(Component.text("$attemptsLeft/$maxAttempts", attemptsColor))
            
            player.sendActionBar(message)
        }
    }
    
    /**
     * Maneja el clic de un jugador en un bloque.
     */
    fun handleBlockClick(player: Player, clickedLocation: Location): Boolean {
        if (!gameStarted) {
            player.sendMessage(Component.text("El juego aún no ha comenzado.", NamedTextColor.RED))
            return false
        }
        
        // Verificar que sea el turno del jugador
        val currentPlayer = getCurrentPlayer()
        if (currentPlayer != player) {
            player.sendMessage(Component.text("¡No es tu turno! Espera a que ${currentPlayer?.name} termine.", NamedTextColor.RED))
            return false
        }
        
        // Verificar intentos del jugador actual
        val attempts = playerAttempts[player] ?: 0
        if (attempts >= maxAttempts) {
            player.sendMessage(Component.text("¡Ya usaste tus $maxAttempts intentos!", NamedTextColor.RED))
            nextTurn() // Pasar al siguiente jugador
            return false
        }
        
        // Buscar el tile
        val tile = findTileByLocation(clickedLocation) ?: return false
        
        // Verificar si ya está revelado permanentemente
        if (tile.isRevealed) {
            player.sendMessage(Component.text("Este par ya fue encontrado.", NamedTextColor.RED))
            return false
        }
        
        // Verificar si es el primer o segundo clic del intento
        if (!isWaitingForSecondClick) {
            // Primer clic del intento
            revealTileTemporarily(tile)
            firstSelectedBlock = tile
            isWaitingForSecondClick = true
            player.sendMessage(Component.text("Selecciona un segundo bloque.", NamedTextColor.YELLOW))
            return true
        } else {
            // Segundo clic del intento
            if (tile.location == firstSelectedBlock?.location) {
                player.sendMessage(Component.text("No puedes seleccionar el mismo bloque dos veces.", NamedTextColor.RED))
                return false
            }
            
            revealTileTemporarily(tile)
            secondSelectedBlock = tile
            
            // Incrementar intentos
            playerAttempts[player] = attempts + 1
            
            // Verificar si es un par
            val firstBlock = firstSelectedBlock
            if (firstBlock != null && tile.pairId == firstBlock.pairId) {
                // ¡PAR ENCONTRADO!
                tile.isRevealed = true
                firstBlock.isRevealed = true
                
                val score = (playerScores[player] ?: 0) + 1
                playerScores[player] = score
                
                val pairMsg = Component.text("✓ ${player.name} encontró un par! (${score} pares)", NamedTextColor.GREEN, TextDecoration.BOLD)
                players.forEach { p -> p.sendMessage(pairMsg) }
                
                // Efectos de sonido mejorados para encontrar par
                player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f)
                player.playSound(player.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f)
                player.playSound(player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f)
                
                // Limpiar selección
                resetSelection()
                
                // El jugador puede seguir jugando (turno extra por encontrar par)
                player.sendMessage(Component.text("¡Encontraste un par! Puedes seguir jugando.", NamedTextColor.GREEN))
                
                // Verificar si todos los jugadores terminaron sus intentos
                checkIfAllPlayersFinished()
            } else {
                // No es un par - ocultar después de 2 segundos
                val attemptsLeft = maxAttempts - playerAttempts[player]!!
                val noMatchMsg = Component.text("✗ ${player.name} no encontró un par. ($attemptsLeft intentos restantes)", NamedTextColor.RED)
                players.forEach { p -> p.sendMessage(noMatchMsg) }
                
                // Efectos de sonido para no encontrar par
                player.playSound(player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f)
                player.playSound(player.location, org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f)
                
                object : BukkitRunnable() {
                    override fun run() {
                        hideTile(tile)
                        firstSelectedBlock?.let { hideTile(it) }
                        resetSelection()
                        
                        // Pasar al siguiente turno
                        nextTurn()
                        
                        // Anunciar el siguiente turno
                        val nextPlayer = getCurrentPlayer()
                        if (nextPlayer != null) {
                            val turnMsg = Component.text("Turno de: ${nextPlayer.name}", NamedTextColor.YELLOW, TextDecoration.BOLD)
                            players.forEach { p -> p.sendMessage(turnMsg) }
                        }
                        
                        // Verificar si todos terminaron
                        checkIfAllPlayersFinished()
                    }
                }.runTaskLater(gameManager.plugin, 40L) // 2 segundos
            }
            
            return true
        }
    }
    
    /**
     * Resetea la selección actual.
     */
    private fun resetSelection() {
        isWaitingForSecondClick = false
        firstSelectedBlock = null
        secondSelectedBlock = null
    }
    
    /**
     * Busca un tile por ubicación.
     */
    private fun findTileByLocation(location: Location): BoardTile? {
        return board.firstOrNull { tile ->
            val tileLoc = tile.location
            location.blockX == tileLoc.blockX &&
            location.blockY == tileLoc.blockY &&
            location.blockZ == tileLoc.blockZ
        }
    }
    
    /**
     * Revela un tile temporalmente (muestra su color).
     */
    private fun revealTileTemporarily(tile: BoardTile) {
        tile.location.block.type = tile.colorMaterial
    }
    
    /**
     * Oculta un tile (vuelve a gris).
     */
    private fun hideTile(tile: BoardTile) {
        if (!tile.isRevealed) {
            tile.location.block.type = hiddenMaterial
        }
    }
    
    /**
     * Verifica si todos los jugadores terminaron sus intentos.
     */
    private fun checkIfAllPlayersFinished() {
        if (allPlayersFinished()) {
            val msg = Component.text("¡Todos los jugadores terminaron sus intentos!", NamedTextColor.RED, TextDecoration.BOLD)
            players.forEach { player -> player.sendMessage(msg) }
            endGameWithWinner()
        }
    }
    
    /**
     * Verifica si todos los jugadores terminaron sus intentos.
     */
    private fun allPlayersFinished(): Boolean {
        return players.all { (playerAttempts[it] ?: 0) >= maxAttempts }
    }
    
    /**
     * Verifica si el juego está completo (todos los pares encontrados).
     */
    private fun isGameComplete(): Boolean {
        return board.all { it.isRevealed }
    }
    
    /**
     * Declara un ganador público.
     */
    fun declareWinner(winner: Player) {
        endGameWithWinner(winner)
    }
    
    /**
     * Finaliza el juego y anuncia al ganador.
     */
    private fun endGameWithWinner() {
        gameStarted = false
        gameTimer?.cancel()
        gameTimer = null
        
        // Encontrar al ganador (el que más pares encontró)
        val winner = playerScores.maxByOrNull { it.value }?.key
        
        if (winner != null) {
            // Mostrar títulos con efectos de sonido mejorados
            players.forEach { player ->
                if (player == winner) {
                    val mainTitle = Component.text("¡VICTORIA!", NamedTextColor.GOLD, TextDecoration.BOLD)
                    val subtitle = Component.text("¡Ganaste el juego!", NamedTextColor.YELLOW)
                    val times = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                    player.showTitle(Title.title(mainTitle, subtitle, times))
                    // Efectos de sonido de victoria
                    player.playSound(player.location, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
                    player.playSound(player.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f)
                    player.playSound(player.location, org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f)
                } else {
                    val mainTitle = Component.text("¡PERDISTE!", NamedTextColor.RED, TextDecoration.BOLD)
                    val subtitle = Component.text("${winner.name} ganó el juego", NamedTextColor.GRAY)
                    val times = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                    player.showTitle(Title.title(mainTitle, subtitle, times))
                    // Efectos de sonido de derrota
                    player.playSound(player.location, org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f)
                    player.playSound(player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f)
                }
                
                // Mensaje en el chat
                player.sendMessage(Component.text("=============================", NamedTextColor.GOLD, TextDecoration.BOLD))
                player.sendMessage(Component.text("¡Juego terminado!", NamedTextColor.GREEN, TextDecoration.BOLD))
                player.sendMessage(Component.empty())
                player.sendMessage(Component.text("Resultados finales:", NamedTextColor.YELLOW))
                
                playerScores.entries.sortedByDescending { it.value }.forEach { entry ->
                    val color = if (entry.key == winner) NamedTextColor.GOLD else NamedTextColor.GRAY
                    val prefix = if (entry.key == winner) "★ " else "  "
                    player.sendMessage(Component.text("$prefix${entry.key.name}: ${entry.value} pares encontrados", color))
                }
                
                player.sendMessage(Component.empty())
                player.sendMessage(Component.text("¡${winner.name} es el ganador!", NamedTextColor.GOLD, TextDecoration.BOLD))
                player.sendMessage(Component.text("=============================", NamedTextColor.GOLD, TextDecoration.BOLD))
            }
            
            // Otorgar puntos
            memoriasManager.torneoPlugin.torneoManager.addScore(winner.uniqueId, memoriasManager.gameName, 50, "Ganador del juego de Memorias")
            memoriasManager.recordVictory(winner)
            
            players.forEach { player ->
                val pairsFound = playerScores[player] ?: 0
                if (pairsFound > 0) {
                    memoriasManager.torneoPlugin.torneoManager.addScore(player.uniqueId, memoriasManager.gameName, pairsFound * 5, "Participación en Memorias")
                }
                memoriasManager.recordGamePlayed(player)
            }
        }
        
        // Mostrar temporizador de regreso al lobby
        var countdown = 5
        object : BukkitRunnable() {
            override fun run() {
                if (countdown > 0) {
                    val msg = Component.text("Regresando al lobby en $countdown segundos...", NamedTextColor.YELLOW, TextDecoration.BOLD)
                    players.forEach { player ->
                        player.sendMessage(msg)
                        player.playSound(player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f)
                    }
                    countdown--
                } else {
                    // Teleportar a todos al lobby
                    val lobbyLoc = gameManager.getLobbyLocation()
                    if (lobbyLoc != null) {
                        val tpMsg = Component.text("¡Has sido enviado al lobby!", NamedTextColor.GREEN, TextDecoration.BOLD)
                        players.toList().forEach { player ->
                            player.teleport(lobbyLoc)
                            player.sendMessage(tpMsg)
                            player.playSound(player.location, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)
                        }
                    }
                    endGame()
                    cancel()
                }
            }
        }.runTaskTimer(gameManager.plugin, 0L, 20L) // Cada segundo
    }
    
    /**
     * Finaliza el juego con un ganador específico (por abandono).
     */
    private fun endGameWithWinner(winner: Player) {
        gameStarted = false
        gameTimer?.cancel()
        gameTimer = null
        
        // Mostrar título de victoria con efectos de sonido
        val mainTitle = Component.text("¡VICTORIA!", NamedTextColor.GOLD, TextDecoration.BOLD)
        val subtitle = Component.text("Ganaste por abandono del oponente", NamedTextColor.YELLOW)
        val times = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500))
        winner.showTitle(Title.title(mainTitle, subtitle, times))
        winner.sendMessage(Component.text("¡Felicidades! Has ganado el juego de Memorias!", NamedTextColor.GOLD, TextDecoration.BOLD))
        
        // Efectos de sonido de victoria por abandono
        winner.playSound(winner.location, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
        winner.playSound(winner.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f)
        
        // Otorgar puntos
        memoriasManager.torneoPlugin.torneoManager.addScore(winner.uniqueId, memoriasManager.gameName, 50, "Victoria")
        memoriasManager.recordVictory(winner)
        memoriasManager.recordGamePlayed(winner)
        
        // Mostrar temporizador de regreso al lobby
        var countdown = 5
        object : BukkitRunnable() {
            override fun run() {
                if (countdown > 0) {
                    winner.sendMessage(Component.text("Regresando al lobby en $countdown segundos...", NamedTextColor.YELLOW, TextDecoration.BOLD))
                    winner.playSound(winner.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f)
                    countdown--
                } else {
                    // Teleportar al lobby
                    gameManager.getLobbyLocation()?.let { 
                        winner.teleport(it)
                        winner.sendMessage(Component.text("¡Has sido enviado al lobby!", NamedTextColor.GREEN, TextDecoration.BOLD))
                        winner.playSound(winner.location, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)
                    }
                    endGame()
                    cancel()
                }
            }
        }.runTaskTimer(gameManager.plugin, 0L, 20L) // Cada segundo
    }
    
    /**
     * Finaliza el juego y limpia el tablero.
     */
    fun endGame() {
        gameStarted = false
        gameTimer?.cancel()
        gameTimer = null
        
        // Limpiar todos los bloques del tablero
        board.forEach { tile ->
            tile.location.block.type = Material.AIR
        }
        
        // Teleportar a todos al lobby
        val lobbyLoc = gameManager.getLobbyLocation()
        if (lobbyLoc != null) {
            players.toList().forEach { player ->
                player.teleport(lobbyLoc)
            }
        }
        
        // Remover todos los jugadores
        players.toList().forEach { player ->
            removePlayer(player)
            gameManager.removePlayer(player)
        }
        
        board.clear()
    }
    
    fun endAllGames() {
        endGame()
    }
}
