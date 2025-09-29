package los5fantasticos.memorias

import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import kotlin.random.Random

/**
 * Representa una instancia de juego de Memorias.
 */
class Game(
    private val gameManager: GameManager,
    private val memoriasManager: MemoriasManager,
    private val arena: Arena
) {
    val players = mutableListOf<Player>()
    private val pattern = mutableListOf<BlockData>()
    private val playerGuesses = mutableMapOf<Player, MutableList<BlockData>>()
    private var currentRound = 0
    private var isShowingPattern = false
    
    private val availableBlocks = listOf(
        Material.RED_WOOL,
        Material.BLUE_WOOL,
        Material.GREEN_WOOL,
        Material.YELLOW_WOOL
    )
    
    fun addPlayer(player: Player) {
        if (!players.contains(player)) {
            players.add(player)
            playerGuesses[player] = mutableListOf()
            player.teleport(arena.spawnLocation)
        }
    }
    
    fun removePlayer(player: Player) {
        players.remove(player)
        playerGuesses.remove(player)
        
        // Teleportar al lobby si existe
        gameManager.getLobbyLocation()?.let { player.teleport(it) }
    }
    
    fun startRound() {
        currentRound++
        isShowingPattern = true
        
        // Generar patrón (agregar un bloque nuevo cada ronda)
        val newBlock = BlockData(
            arena.patternLocation.clone().add(
                Random.nextDouble(-5.0, 5.0),
                0.0,
                Random.nextDouble(-5.0, 5.0)
            ),
            availableBlocks.random()
        )
        pattern.add(newBlock)
        
        // Limpiar intentos anteriores
        playerGuesses.values.forEach { it.clear() }
        
        // Mostrar patrón a los jugadores
        players.forEach { player ->
            player.sendMessage("${ChatColor.YELLOW}Ronda $currentRound - Memoriza el patrón!")
        }
        
        showPattern()
    }
    
    private fun showPattern() {
        // Mostrar bloques del patrón temporalmente
        pattern.forEach { blockData ->
            blockData.location.block.type = blockData.material
        }
        
        // Después de 5 segundos, ocultar el patrón
        object : BukkitRunnable() {
            override fun run() {
                hidePattern()
                isShowingPattern = false
                players.forEach { player ->
                    player.sendMessage("${ChatColor.GREEN}¡Ahora reproduce el patrón!")
                }
            }
        }.runTaskLater(gameManager.plugin, 100L) // 5 segundos
    }
    
    private fun hidePattern() {
        pattern.forEach { blockData ->
            blockData.location.block.type = Material.AIR
        }
    }
    
    fun handleGuessBlock(player: Player, location: Location, material: Material): Boolean {
        if (isShowingPattern) {
            player.sendMessage("${ChatColor.RED}¡Espera a que termine de mostrarse el patrón!")
            return false
        }
        
        val guesses = playerGuesses[player] ?: return false
        
        // Verificar si el bloque está en el área de adivinanza
        if (location.distance(arena.guessArea) > 10.0) {
            player.sendMessage("${ChatColor.RED}¡Coloca los bloques en el área de adivinanza!")
            return false
        }
        
        guesses.add(BlockData(location, material))
        
        // Verificar si completó el patrón
        if (guesses.size == pattern.size) {
            checkPlayerGuess(player)
        }
        
        return true
    }
    
    private fun checkPlayerGuess(player: Player) {
        val guesses = playerGuesses[player] ?: return
        
        // Verificar si el patrón coincide
        val isCorrect = guesses.zip(pattern).all { (guess, correct) ->
            guess.material == correct.material
        }
        
        if (isCorrect) {
            player.sendMessage("${ChatColor.GREEN}${ChatColor.BOLD}¡Correcto! +50 puntos")
            memoriasManager.awardPoints(player, 50, "Completó ronda $currentRound")
            
            // Iniciar siguiente ronda
            object : BukkitRunnable() {
                override fun run() {
                    if (players.contains(player)) {
                        startRound()
                    }
                }
            }.runTaskLater(gameManager.plugin, 60L) // 3 segundos
        } else {
            player.sendMessage("${ChatColor.RED}¡Incorrecto! El juego ha terminado.")
            memoriasManager.awardPoints(player, currentRound * 10, "Llegó a ronda $currentRound")
            endGameForPlayer(player)
        }
    }
    
    private fun endGameForPlayer(player: Player) {
        // Limpiar bloques del jugador
        playerGuesses[player]?.forEach { it.location.block.type = Material.AIR }
        
        removePlayer(player)
        gameManager.removePlayer(player)
    }
    
    fun endGame() {
        // Limpiar todos los bloques
        pattern.forEach { it.location.block.type = Material.AIR }
        playerGuesses.values.forEach { guesses ->
            guesses.forEach { it.location.block.type = Material.AIR }
        }
        
        // Remover todos los jugadores
        players.toList().forEach { removePlayer(it) }
    }
}
