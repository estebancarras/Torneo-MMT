package los5fantasticos.memorias
// hola
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.random.Random

class GameManager(val plugin: Plugin) {

    private val activeGames = mutableMapOf<UUID, Game>()
    private val playerToGameMap = mutableMapOf<UUID, Game>()

    private var lobbyLocation: Location? = null
    private var spawnLocation: Location? = null
    private var patternLocation: Location? = null
    private var guessArea: Location? = null

    private val arenas = mutableListOf<Arena>()

    // Métodos para que los administradores configuren el juego
    fun setLobbyLocation(location: Location) {
        this.lobbyLocation = location
    }

    fun setSpawnLocation(location: Location) {
        this.spawnLocation = location
    }

    fun setPatternLocation(location: Location) {
        this.patternLocation = location
    }

    fun setGuessArea(location: Location) {
        this.guessArea = location
    }

    fun createArena(sender: Player) {
        if (spawnLocation == null || patternLocation == null || guessArea == null) {
            sender.sendMessage("Faltan ubicaciones para crear la arena. Usa /pattern admin set<location>.")
            return
        }
        val newArena = Arena(spawnLocation!!, patternLocation!!, guessArea!!)
        arenas.add(newArena)
        sender.sendMessage("Arena creada con éxito.")
    }

    fun joinPlayer(player: Player) {
        if (arenas.isEmpty()) {
            player.sendMessage("No hay arenas disponibles. Un administrador debe crearlas.")
            return
        }
        val game = activeGames.values.firstOrNull { it.players.size < 2 } ?: createNewGame()
        game.addPlayer(player)
        playerToGameMap[player.uniqueId] = game
        player.sendMessage("Te has unido a la cola. ¡Esperando a otro jugador!")
        if (game.players.size >= 2) {
            game.startRound()
        }
    }

    private fun createNewGame(): Game {
        val newGame = Game(this, arenas[Random.nextInt(arenas.size)])
        activeGames[UUID.randomUUID()] = newGame
        return newGame
    }

    fun getGameByPlayer(player: Player): Game? {
        return playerToGameMap[player.uniqueId]
    }

    fun removePlayer(player: Player) {
        val game = playerToGameMap.remove(player.uniqueId) ?: return
        game.removePlayer(player)
        if (game.players.isEmpty()) {
            val gameId = activeGames.entries.firstOrNull { it.value == game }?.key ?: return
            activeGames.remove(gameId)
        }
    }

    fun getLobbyLocation(): Location? {
        return lobbyLocation
    }
}