package los5fantasticos.minigameCarrerabarcos.commands

import los5fantasticos.minigameCarrerabarcos.MinigameCarrerabarcos
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Comando para administrar el minijuego Carrera de Barcos.
 */
class CarreraCommand(private val minigame: MinigameCarrerabarcos) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("torneo.admin")) {
            sender.sendMessage("${ChatColor.RED}No tienes permisos para usar este comando.")
            return true
        }
        
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "info" -> showInfo(sender)
            "test" -> testMinigame(sender)
            "start" -> startMinigame(sender)
            "stop" -> stopMinigame(sender)
            "help" -> showHelp(sender)
            else -> {
                sender.sendMessage("${ChatColor.RED}Comando no reconocido. Usa ${ChatColor.YELLOW}/carrerabarcos help${ChatColor.RED} para ver la ayuda.")
            }
        }
        
        return true
    }
    
    private fun showHelp(sender: CommandSender) {
        sender.sendMessage("${ChatColor.GOLD}=== Comandos de Carrera de Barcos ===")
        sender.sendMessage("${ChatColor.YELLOW}/carrerabarcos info${ChatColor.WHITE} - Muestra información de la pista")
        sender.sendMessage("${ChatColor.YELLOW}/carrerabarcos test${ChatColor.WHITE} - Inicia un juego de prueba (solo tú)")
        sender.sendMessage("${ChatColor.YELLOW}/carrerabarcos start${ChatColor.WHITE} - Inicia el juego con TODOS los jugadores del servidor")
        sender.sendMessage("${ChatColor.YELLOW}/carrerabarcos stop${ChatColor.WHITE} - Termina la carrera en curso")
        sender.sendMessage("${ChatColor.YELLOW}/carrerabarcos help${ChatColor.WHITE} - Muestra esta ayuda")
    }
    
    private fun showInfo(sender: CommandSender) {
        sender.sendMessage("${ChatColor.GOLD}=== Información de la Pista ===")
        sender.sendMessage("${ChatColor.WHITE}${minigame.getTrackInfo()}")
        sender.sendMessage("${ChatColor.GRAY}Para cambiar las coordenadas, edita el archivo MinigameCarrerabarcos.kt")
    }
    
    private fun testMinigame(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Este comando solo puede ser usado por jugadores.")
            return
        }
        
        // Crear una lista de jugadores de prueba (solo el que ejecuta el comando)
        val testPlayers = listOf(sender)
        
        sender.sendMessage("${ChatColor.GREEN}Iniciando juego de prueba...")
        sender.sendMessage("${ChatColor.YELLOW}¡Prepárate! Serás teletransportado a la pista.")
        
        // Iniciar el juego
        minigame.startGame(testPlayers)
    }
    
    private fun startMinigame(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Este comando solo puede ser usado por jugadores.")
            return
        }
        
        // Obtener todos los jugadores online del servidor
        val allPlayers = sender.server.onlinePlayers.toList()
        
        if (allPlayers.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}No hay jugadores online para iniciar el juego.")
            return
        }
        
        sender.sendMessage("${ChatColor.GREEN}Iniciando carrera con TODOS los jugadores del servidor...")
        sender.sendMessage("${ChatColor.YELLOW}Participantes: ${allPlayers.size} jugadores")
        
        // Notificar a todos los jugadores
        allPlayers.forEach { player ->
            player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}=== CARRERA DE BARCOS ===")
            player.sendMessage("${ChatColor.GREEN}¡Has sido invitado a participar en la Carrera de Barcos!")
            player.sendMessage("${ChatColor.YELLOW}Serás teletransportado a la pista en breve...")
        }
        
        // Iniciar el juego con todos los jugadores
        minigame.startGame(allPlayers)
    }
    
    private fun stopMinigame(sender: CommandSender) {
        if (!minigame.isGameRunning()) {
            sender.sendMessage("${ChatColor.RED}No hay ninguna carrera en curso para terminar.")
            return
        }
        
        sender.sendMessage("${ChatColor.GREEN}Terminando la carrera en curso...")
        
        // Notificar a todos los jugadores activos
        val activePlayers = minigame.getActivePlayers()
        activePlayers.forEach { player ->
            player.sendMessage("${ChatColor.GOLD}[Carrera de Barcos] ${ChatColor.RED}La carrera ha sido terminada por un administrador.")
        }
        
        // Terminar el juego sin mostrar resultados
        minigame.endGame(false)
        
        sender.sendMessage("${ChatColor.GREEN}¡Carrera terminada exitosamente!")
        sender.sendMessage("${ChatColor.YELLOW}Jugadores afectados: ${activePlayers.size}")
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (!sender.hasPermission("torneo.admin")) {
            return emptyList()
        }
        
        if (args.size == 1) {
            return listOf("info", "test", "start", "stop", "help").filter { it.startsWith(args[0].lowercase()) }
        }
        
        return emptyList()
    }
}