package los5fantasticos.minigameCadena.commands

import los5fantasticos.minigameCadena.MinigameCadena
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Comando principal del minijuego Cadena.
 * 
 * Maneja los subcomandos:
 * - `/cadena join` - Unirse a una partida
 * - `/cadena admin` - Comandos de administración (futuro)
 */
class CadenaCommand(private val minigame: MinigameCadena) : CommandExecutor, TabCompleter {
    
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Este comando solo puede ser ejecutado por jugadores.")
            return true
        }
        
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "join" -> handleJoin(sender)
            "admin" -> handleAdmin(sender, args.drop(1).toTypedArray())
            else -> sendHelp(sender)
        }
        
        return true
    }
    
    /**
     * Maneja el subcomando `/cadena join`.
     */
    private fun handleJoin(player: Player) {
        // PR1: Implementación básica con mensaje placeholder
        player.sendMessage("${ChatColor.GRAY}Te has unido a la cola de Cadena.")
        
        // TODO PR2: Integrar con LobbyManager para añadir al jugador a una partida
    }
    
    /**
     * Maneja el subcomando `/cadena admin`.
     */
    private fun handleAdmin(player: Player, args: Array<String>) {
        // Verificar permisos
        if (!player.hasPermission("cadena.admin")) {
            player.sendMessage("${ChatColor.RED}No tienes permiso para usar comandos de administrador.")
            return
        }
        
        // PR1: Implementación básica con mensaje placeholder
        player.sendMessage("${ChatColor.RED}Comandos de administrador no implementados.")
        
        // TODO PR6: Implementar subcomandos de administración
        // - createarena <nombre>
        // - setlobby
        // - setspawn
        // - addcheckpoint
        // - setfinish
    }
    
    /**
     * Muestra la ayuda del comando.
     */
    private fun sendHelp(player: Player) {
        player.sendMessage("")
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}━━━━━━━━━━ Cadena ━━━━━━━━━━")
        player.sendMessage("${ChatColor.YELLOW}/cadena join ${ChatColor.GRAY}- Unirse a una partida")
        player.sendMessage("${ChatColor.YELLOW}/cadena admin ${ChatColor.GRAY}- Comandos de administración")
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        player.sendMessage("")
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            val subcommands = mutableListOf("join")
            if (sender.hasPermission("cadena.admin")) {
                subcommands.add("admin")
            }
            return subcommands.filter { it.startsWith(args[0].lowercase()) }
        }
        
        if (args.size == 2 && args[0].equals("admin", ignoreCase = true)) {
            // TODO PR6: Añadir tab completion para subcomandos de admin
            return emptyList()
        }
        
        return emptyList()
    }
}
