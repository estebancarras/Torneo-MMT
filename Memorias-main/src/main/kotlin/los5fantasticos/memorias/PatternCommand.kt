package los5fantasticos.memorias
// hola
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PatternCommand(private val gameManager: GameManager) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Este comando solo puede ser ejecutado por un jugador.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("Uso: /pattern join | admin")
            return true
        }

        when (args[0].lowercase()) {
            "join" -> {
                gameManager.joinPlayer(sender)
            }
            "admin" -> {
                if (args.size < 2) {
                    sender.sendMessage("Uso: /pattern admin setlobby | setspawn | setpattern | setguessarea | createarena")
                    return true
                }

                when (args[1].lowercase()) {
                    "setlobby" -> {
                        gameManager.setLobbyLocation(sender.location)
                        sender.sendMessage("Ubicación del lobby establecida.")
                    }
                    "setspawn" -> {
                        gameManager.setSpawnLocation(sender.location)
                        sender.sendMessage("Ubicación de spawn de la arena establecida.")
                    }
                    "setpattern" -> {
                        gameManager.setPatternLocation(sender.location)
                        sender.sendMessage("Ubicación del patrón de la arena establecida.")
                    }
                    "setguessarea" -> {
                        gameManager.setGuessArea(sender.location)
                        sender.sendMessage("Ubicación del área de adivinanza establecida.")
                    }
                    "createarena" -> {
                        gameManager.createArena(sender)
                    }
                    else -> sender.sendMessage("Subcomando de administración desconocido.")
                }
            }
            else -> sender.sendMessage("Comando desconocido.")
        }
        return true
    }
}