package los5fantasticos.memorias
// hola
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {

    private lateinit var gameManager: GameManager

    override fun onEnable() {
        gameManager = GameManager(this)
        getCommand("pattern")?.setExecutor(PatternCommand(gameManager))
        server.pluginManager.registerEvents(PlayerListener(gameManager), this)
        logger.info("El plugin de memorizar patrones se ha activado.")
    }

    override fun onDisable() {
        // Lógica de limpieza al apagar el servidor
    }
}