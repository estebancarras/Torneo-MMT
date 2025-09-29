package los5fantasticos.memorias
// hola
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener(private val gameManager: GameManager) : Listener {

    private val guessableBlocks = listOf(
        Material.RED_WOOL,
        Material.BLUE_WOOL,
        Material.GREEN_WOOL,
        Material.YELLOW_WOOL
    )

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        gameManager.removePlayer(event.player)
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        if (block.state is Sign) {
            val sign = block.state as Sign

            @Suppress("DEPRECATION") // ⚠️ Evita el warning por getLine()
            val line0 = sign.getLine(0)

            if (line0.equals("[Pattern]", ignoreCase = true)) {
                gameManager.joinPlayer(event.player)
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val game = gameManager.getGameByPlayer(player) ?: return

        if (!game.players.contains(player) || !guessableBlocks.contains(event.block.type)) {
            event.isCancelled = true
            return
        }

        val isSuccessfulPlace = game.handleGuessBlock(player, event.block.location, event.block.type)
        if (!isSuccessfulPlace) {
            event.isCancelled = true
        }
    }
}
