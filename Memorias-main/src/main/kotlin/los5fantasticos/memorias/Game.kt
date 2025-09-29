package los5fantasticos.memorias
// hola
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import kotlin.random.Random
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.inventory.ItemStack
import org.bukkit.Location
import kotlin.math.abs

class Game(private val gameManager: GameManager, private val arena: Arena) {

    val players = mutableSetOf<Player>()
    private val scores = mutableMapOf<Player, Int>()
    private var currentPattern: Pattern? = null
    private var currentTurn: Player? = null

    val patternSize = 25
    private val materials = listOf(Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL, Material.YELLOW_WOOL)

    private val playerGuesses = mutableMapOf<Player, MutableList<BlockData>>()

    fun addPlayer(player: Player) {
        players.add(player)
        scores[player] = 0
    }

    fun removePlayer(player: Player) {
        players.remove(player)
        scores.remove(player)
        player.sendMessage("Has sido removido de la partida.")
        if (players.size < 2) {
            endGame()
        }
    }

    fun startRound() {
        players.forEach {
            it.teleport(arena.spawnLocation)
            it.sendMessage("¡Partida encontrada! Prepárate.")
            it.gameMode = GameMode.SURVIVAL
            it.health = 20.0
            it.isInvulnerable = true
            givePlayerGuessBlocks(it)
        }

        object : BukkitRunnable() {
            var countdown = 5
            override fun run() {
                if (countdown <= 0) {
                    players.forEach { it.sendMessage("¡GO! El patrón ha sido mostrado.") }
                    currentPattern = generateRandomPattern()
                    showPattern()
                    this.cancel()
                    return
                }
                players.forEach { it.sendMessage("La partida inicia en $countdown...") }
                countdown--
            }
        }.runTaskTimer(gameManager.plugin, 0L, 20L)
    }

    private fun givePlayerGuessBlocks(player: Player) {
        player.inventory.clear()
        materials.forEach {
            val itemStack = ItemStack(it, 64)
            player.inventory.addItem(itemStack)
        }
    }

    private fun showPattern() {
        val pattern = currentPattern ?: return
        for ((index, blockData) in pattern.blockData.withIndex()) {
            val xOffset = index % 5
            val zOffset = index / 5
            val location = arena.patternLocation.clone().add(xOffset.toDouble(), 0.0, zOffset.toDouble())
            location.block.type = blockData.material
        }
        players.forEach { it.sendMessage("¡Memoriza el patrón!") }

        object : BukkitRunnable() {
            override fun run() {
                hidePattern()
                nextTurn()
            }
        }.runTaskLater(gameManager.plugin, 20L * 5)
    }

    private fun hidePattern() {
        val pattern = currentPattern ?: return
        for ((index, _) in pattern.blockData.withIndex()) {
            val xOffset = index % 5
            val zOffset = index / 5
            val location = arena.patternLocation.clone().add(xOffset.toDouble(), 0.0, zOffset.toDouble())
            location.block.type = Material.AIR
        }
    }

    fun nextTurn() {
        val nextPlayer = players.random()
        currentTurn = nextPlayer
        players.forEach { it.sendMessage("Es el turno de ${nextPlayer.name}.") }
        nextPlayer.sendMessage("Coloca los bloques en el área de adivinanza para replicar el patrón.")
    }

    fun handleGuessBlock(player: Player, location: Location, material: Material): Boolean {
        if (player != currentTurn) {
            player.sendMessage("No es tu turno.")
            return false
        }

        if (!isWithinGuessArea(location)) {
            player.sendMessage("Solo puedes colocar bloques en el área de adivinanza.")
            return false
        }

        val relativeLocation = location.clone().subtract(arena.guessArea)
        val x = relativeLocation.blockX
        val z = relativeLocation.blockZ
        val index = x + z * 5

        // CORREGIDO: Usando un rango para una mejor legibilidad y rendimiento
        if (index !in 0 until patternSize) {
            player.sendMessage("No puedes colocar bloques fuera de la cuadrícula.")
            return false
        }

        val guesses = playerGuesses.getOrPut(player) { MutableList(patternSize) { BlockData(Material.AIR) } }
        guesses[index] = BlockData(material)

        if (!guesses.any { it.material == Material.AIR }) {
            val guessedPattern = Pattern(guesses)
            if (isPatternCorrect(guessedPattern)) {
                player.sendMessage("¡Correcto! Has ganado 1 punto.")
                addScore(player, 1)
            } else {
                player.sendMessage("Incorrecto. Pierdes 1 punto.")
                addScore(player, -1)
            }

            resetGuessBlocks(player)
            nextTurn()
        } else {
            player.sendMessage("Bloques colocados: ${guesses.count { it.material != Material.AIR }} de $patternSize")
        }
        return true
    }

    private fun isWithinGuessArea(location: Location): Boolean {
        val guessArea = arena.guessArea
        return abs(location.x - guessArea.x) < 5 &&
                abs(location.y - guessArea.y) < 1 &&
                abs(location.z - guessArea.z) < 5
    }

    private fun isPatternCorrect(guessedPattern: Pattern): Boolean {
        return guessedPattern.blockData == currentPattern?.blockData
    }

    private fun addScore(player: Player, points: Int) {
        scores[player] = scores.getOrDefault(player, 0) + points
        player.sendMessage("Tu puntaje actual es: ${scores[player]}")
    }

    private fun resetGuessBlocks(player: Player) {
        playerGuesses.remove(player)
        for (i in 0 until patternSize) {
            val xOffset = i % 5
            val zOffset = i / 5
            val location = arena.guessArea.clone().add(xOffset.toDouble(), 0.0, zOffset.toDouble())
            location.block.type = Material.AIR
        }
    }

    private fun endGame() {
        val winner = scores.maxByOrNull { it.value }?.key
        players.forEach {
            it.sendMessage("La partida ha terminado.")
            if (winner != null) {
                it.sendMessage("¡El ganador es ${winner.name} con ${scores[winner]} puntos!")
            } else {
                it.sendMessage("La partida terminó sin un ganador.")
            }
            it.sendMessage("Puntajes finales: $scores")
        }

        object : BukkitRunnable() {
            var countdown = 20
            override fun run() {
                if (countdown <= 0) {
                    players.forEach {
                        it.isInvulnerable = false
                        it.inventory.clear()
                        it.gameMode = GameMode.SURVIVAL
                        val lobbyLocation = gameManager.getLobbyLocation()
                        if (lobbyLocation != null) {
                            it.teleport(lobbyLocation)
                        } else {
                            it.teleport(Bukkit.getWorlds()[0].spawnLocation)
                        }
                        gameManager.removePlayer(it)
                    }
                    this.cancel()
                    return
                }
                players.forEach { it.sendMessage("Teletransportando al lobby en $countdown...") }
                countdown--
            }
        }.runTaskTimer(gameManager.plugin, 0L, 20L)
    }

    private fun generateRandomPattern(): Pattern {
        val patternList = (1..patternSize).map {
            BlockData(materials[Random.nextInt(materials.size)])
        }
        return Pattern(patternList)
    }
}