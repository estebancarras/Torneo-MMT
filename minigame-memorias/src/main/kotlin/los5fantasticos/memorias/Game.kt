package los5fantasticos.memorias

import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent

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
            winner.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}¡${player.name} se desconectó! Ganaste por abandono.")
            endGameWithWinner(winner)
            return
        }
        
        // Si no hay suficientes jugadores, terminar el juego
        if (gameStarted && players.size < 2) {
            players.forEach { it.sendMessage("${ChatColor.RED}No hay suficientes jugadores. El juego se ha cancelado.") }
            endGame()
            return
        }
        
        // Si era el jugador actual y quedan jugadores, pasar al siguiente
        if (wasCurrentPlayer && players.isNotEmpty()) {
            val nextPlayer = getCurrentPlayer()
            if (nextPlayer != null) {
                players.forEach { p ->
                    p.sendMessage("${ChatColor.YELLOW}${ChatColor.BOLD}Turno de: ${nextPlayer.name}")
                }
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
                            player.sendTitle(
                                "${ChatColor.GOLD}${ChatColor.BOLD}$countdown",
                                "${ChatColor.YELLOW}Preparándose...",
                                5, 15, 5
                            )
                            player.playSound(player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f)
                        }
                        countdown--
                    }
                    0 -> {
                        players.forEach { player ->
                            player.sendTitle(
                                "${ChatColor.GREEN}${ChatColor.BOLD}¡COMENZÓ!",
                                "${ChatColor.YELLOW}¡Encuentra los pares! (4 intentos)",
                                5, 20, 5
                            )
                            player.playSound(player.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
                            player.sendMessage("${ChatColor.GREEN}${ChatColor.BOLD}¡El juego de Memorias ha comenzado!")
                            player.sendMessage("${ChatColor.YELLOW}Tienes 4 intentos para encontrar los pares.")
                            player.sendMessage("${ChatColor.YELLOW}Haz clic derecho en un bloque gris para revelarlo.")
                        }
                        
                        // Anunciar el primer turno
                        val firstPlayer = getCurrentPlayer()
                        if (firstPlayer != null) {
                            players.forEach { player ->
                                player.sendMessage("${ChatColor.YELLOW}${ChatColor.BOLD}Turno de: ${firstPlayer.name}")
                            }
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
                        players.forEach { p ->
                            p.sendMessage("${ChatColor.RED}¡Se agotó el tiempo del turno de ${currentPlayer.name}!")
                        }
                        
                        // Pasar al siguiente turno
                        nextTurn()
                        resetTurnTimer()
                        
                        // Anunciar el siguiente turno
                        val nextPlayer = getCurrentPlayer()
                        if (nextPlayer != null) {
                            players.forEach { p ->
                                p.sendMessage("${ChatColor.YELLOW}${ChatColor.BOLD}Turno de: ${nextPlayer.name}")
                            }
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
                attemptsLeft > 2 -> ChatColor.GREEN
                attemptsLeft > 0 -> ChatColor.YELLOW
                else -> ChatColor.RED
            }
            
            val turnIndicator = if (player == currentPlayer) {
                "${ChatColor.GOLD}${ChatColor.BOLD}TU TURNO ${ChatColor.GRAY}| "
            } else {
                "${ChatColor.GRAY}Esperando... ${ChatColor.GRAY}| "
            }
            
            val timeColor = when {
                turnTimeLeft > 20 -> ChatColor.GREEN
                turnTimeLeft > 10 -> ChatColor.YELLOW
                else -> ChatColor.RED
            }
            
            val message = turnIndicator +
                         "$timeColor${ChatColor.BOLD}Tiempo: ${ChatColor.WHITE}${turnTimeLeft}s ${ChatColor.GRAY}| " +
                         "${ChatColor.AQUA}Pares: ${ChatColor.WHITE}$score ${ChatColor.GRAY}| " +
                         "$attemptsColor${ChatColor.BOLD}Intentos: $attemptsLeft/$maxAttempts"
            
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(message))
        }
    }
    
    /**
     * Maneja el clic de un jugador en un bloque.
     */
    fun handleBlockClick(player: Player, clickedLocation: Location): Boolean {
        if (!gameStarted) {
            player.sendMessage("${ChatColor.RED}El juego aún no ha comenzado.")
            return false
        }
        
        // Verificar que sea el turno del jugador
        val currentPlayer = getCurrentPlayer()
        if (currentPlayer != player) {
            player.sendMessage("${ChatColor.RED}¡No es tu turno! Espera a que ${currentPlayer?.name} termine.")
            return false
        }
        
        // Verificar intentos del jugador actual
        val attempts = playerAttempts[player] ?: 0
        if (attempts >= maxAttempts) {
            player.sendMessage("${ChatColor.RED}¡Ya usaste tus $maxAttempts intentos!")
            nextTurn() // Pasar al siguiente jugador
            return false
        }
        
        // Buscar el tile
        val tile = findTileByLocation(clickedLocation) ?: return false
        
        // Verificar si ya está revelado permanentemente
        if (tile.isRevealed) {
            player.sendMessage("${ChatColor.RED}Este par ya fue encontrado.")
            return false
        }
        
        // Verificar si es el primer o segundo clic del intento
        if (!isWaitingForSecondClick) {
            // Primer clic del intento
            revealTileTemporarily(tile)
            firstSelectedBlock = tile
            isWaitingForSecondClick = true
            player.sendMessage("${ChatColor.YELLOW}Selecciona un segundo bloque.")
            return true
        } else {
            // Segundo clic del intento
            if (tile.location == firstSelectedBlock?.location) {
                player.sendMessage("${ChatColor.RED}No puedes seleccionar el mismo bloque dos veces.")
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
                
                players.forEach { p ->
                    p.sendMessage("${ChatColor.GREEN}${ChatColor.BOLD}✓ ${player.name} encontró un par! (${score} pares)")
                }
                
                // Efectos de sonido mejorados para encontrar par
                player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f)
                player.playSound(player.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f)
                player.playSound(player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f)
                
                // Limpiar selección
                resetSelection()
                
                // El jugador puede seguir jugando (turno extra por encontrar par)
                player.sendMessage("${ChatColor.GREEN}¡Encontraste un par! Puedes seguir jugando.")
                
                // Verificar si todos los jugadores terminaron sus intentos
                checkIfAllPlayersFinished()
            } else {
                // No es un par - ocultar después de 2 segundos
                players.forEach { p ->
                    p.sendMessage("${ChatColor.RED}✗ ${player.name} no encontró un par. (${maxAttempts - playerAttempts[player]!!} intentos restantes)")
                }
                
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
                            players.forEach { p ->
                                p.sendMessage("${ChatColor.YELLOW}${ChatColor.BOLD}Turno de: ${nextPlayer.name}")
                            }
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
            players.forEach { player ->
                player.sendMessage("${ChatColor.RED}${ChatColor.BOLD}¡Todos los jugadores terminaron sus intentos!")
            }
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
                    player.sendTitle(
                        "${ChatColor.GOLD}${ChatColor.BOLD}¡VICTORIA!",
                        "${ChatColor.YELLOW}¡Ganaste el juego!",
                        10, 60, 10
                    )
                    // Efectos de sonido de victoria
                    player.playSound(player.location, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
                    player.playSound(player.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f)
                    player.playSound(player.location, org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f)
                } else {
                    player.sendTitle(
                        "${ChatColor.RED}${ChatColor.BOLD}¡PERDISTE!",
                        "${ChatColor.GRAY}${winner.name} ganó el juego",
                        10, 60, 10
                    )
                    // Efectos de sonido de derrota
                    player.playSound(player.location, org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f)
                    player.playSound(player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f)
                }
                
                // Mensaje en el chat
                player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}=============================")
                player.sendMessage("${ChatColor.GREEN}${ChatColor.BOLD}¡Juego terminado!")
                player.sendMessage("")
                player.sendMessage("${ChatColor.YELLOW}Resultados finales:")
                
                playerScores.entries.sortedByDescending { it.value }.forEach { entry ->
                    val prefix = if (entry.key == winner) "${ChatColor.GOLD}★ " else "${ChatColor.GRAY}  "
                    player.sendMessage("$prefix${entry.key.name}: ${entry.value} pares encontrados")
                }
                
                player.sendMessage("")
                player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}¡${winner.name} es el ganador!")
                player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}=============================")
            }
            
            // Otorgar puntos
            memoriasManager.awardPoints(winner, 50, "Ganador del juego de Memorias")
            memoriasManager.recordVictory(winner)
            
            players.forEach { player ->
                val pairsFound = playerScores[player] ?: 0
                if (pairsFound > 0) {
                    memoriasManager.awardPoints(player, pairsFound * 5, "Participación en Memorias")
                }
                memoriasManager.recordGamePlayed(player)
            }
        }
        
        // Mostrar temporizador de regreso al lobby
        var countdown = 5
        object : BukkitRunnable() {
            override fun run() {
                if (countdown > 0) {
                    players.forEach { player ->
                        player.sendMessage("${ChatColor.YELLOW}${ChatColor.BOLD}Regresando al lobby en $countdown segundos...")
                        player.playSound(player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f)
                    }
                    countdown--
                } else {
                    // Teleportar a todos al lobby
                    val lobbyLoc = gameManager.getLobbyLocation()
                    if (lobbyLoc != null) {
                        players.toList().forEach { player ->
                            player.teleport(lobbyLoc)
                            player.sendMessage("${ChatColor.GREEN}${ChatColor.BOLD}¡Has sido enviado al lobby!")
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
        winner.sendTitle(
            "${ChatColor.GOLD}${ChatColor.BOLD}¡VICTORIA!",
            "${ChatColor.YELLOW}Ganaste por abandono del oponente",
            10, 40, 10
        )
        winner.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}¡Felicidades! Has ganado el juego de Memorias!")
        
        // Efectos de sonido de victoria por abandono
        winner.playSound(winner.location, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
        winner.playSound(winner.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f)
        
        // Otorgar puntos
        memoriasManager.awardPoints(winner, 50, "Victoria")
        memoriasManager.recordVictory(winner)
        memoriasManager.recordGamePlayed(winner)
        
        // Mostrar temporizador de regreso al lobby
        var countdown = 5
        object : BukkitRunnable() {
            override fun run() {
                if (countdown > 0) {
                    winner.sendMessage("${ChatColor.YELLOW}${ChatColor.BOLD}Regresando al lobby en $countdown segundos...")
                    winner.playSound(winner.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f)
                    countdown--
                } else {
                    // Teleportar al lobby
                    gameManager.getLobbyLocation()?.let { 
                        winner.teleport(it)
                        winner.sendMessage("${ChatColor.GREEN}${ChatColor.BOLD}¡Has sido enviado al lobby!")
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
