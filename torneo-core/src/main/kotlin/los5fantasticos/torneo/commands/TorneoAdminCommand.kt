package los5fantasticos.torneo.commands

import los5fantasticos.torneo.core.TorneoManager
import los5fantasticos.torneo.services.TournamentFlowManager
import los5fantasticos.torneo.util.selection.SelectionManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Comandos de administración para el sistema de torneo centralizado.
 * 
 * COMANDOS:
 * - /torneo wand: Obtener varita de selección
 * - /torneo setlobbyregion: Establecer región del lobby global
 * - /torneo addspawn: Añadir spawn al lobby
 * - /torneo clearspawns: Limpiar spawns del lobby
 * - /torneo start <minigame>: Iniciar minijuego para todos
 * - /torneo end: Finalizar minijuego activo
 * - /torneo status: Ver estado del torneo
 */
class TorneoAdminCommand(
    private val torneoManager: TorneoManager
) : CommandExecutor, TabCompleter {
    
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // Verificar permisos
        if (!sender.hasPermission("torneo.admin") && !sender.isOp) {
            sender.sendMessage(Component.text("✗ No tienes permiso para usar este comando", NamedTextColor.RED))
            return true
        }
        
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "wand" -> return handleWand(sender)
            "setlobbyregion" -> return handleSetLobbyRegion(sender)
            "addspawn" -> return handleAddSpawn(sender)
            "clearspawns" -> return handleClearSpawns(sender)
            "start" -> return handleStart(sender, args)
            "end" -> return handleEnd(sender)
            "status" -> return handleStatus(sender)
            else -> {
                sender.sendMessage(Component.text("✗ Subcomando desconocido", NamedTextColor.RED))
                showHelp(sender)
                return true
            }
        }
    }
    
    private fun showHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("╔════════════════════════════════════╗", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("║  Comandos de Admin - Torneo MMT  ║", NamedTextColor.YELLOW, TextDecoration.BOLD))
        sender.sendMessage(Component.text("╚════════════════════════════════════╝", NamedTextColor.GOLD))
        sender.sendMessage(Component.empty())
        sender.sendMessage(Component.text("Configuración del Lobby:", NamedTextColor.AQUA, TextDecoration.BOLD))
        sender.sendMessage(Component.text("  /torneo wand", NamedTextColor.YELLOW)
            .append(Component.text(" - Obtener varita de selección", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  /torneo setlobbyregion", NamedTextColor.YELLOW)
            .append(Component.text(" - Establecer región del lobby", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  /torneo addspawn", NamedTextColor.YELLOW)
            .append(Component.text(" - Añadir spawn al lobby", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  /torneo clearspawns", NamedTextColor.YELLOW)
            .append(Component.text(" - Limpiar spawns del lobby", NamedTextColor.GRAY)))
        sender.sendMessage(Component.empty())
        sender.sendMessage(Component.text("Control del Torneo:", NamedTextColor.AQUA, TextDecoration.BOLD))
        sender.sendMessage(Component.text("  /torneo start <minigame>", NamedTextColor.YELLOW)
            .append(Component.text(" - Iniciar minijuego", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  /torneo end", NamedTextColor.YELLOW)
            .append(Component.text(" - Finalizar minijuego activo", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  /torneo status", NamedTextColor.YELLOW)
            .append(Component.text(" - Ver estado del torneo", NamedTextColor.GRAY)))
        sender.sendMessage(Component.empty())
    }
    
    private fun handleWand(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("✗ Solo jugadores pueden usar este comando", NamedTextColor.RED))
            return true
        }
        
        sender.inventory.addItem(SelectionManager.selectionWand)
        sender.sendMessage(Component.text("✓ Varita de selección entregada", NamedTextColor.GREEN))
        sender.sendMessage(Component.text("Clic Izquierdo: Pos1 | Clic Derecho: Pos2", NamedTextColor.YELLOW))
        
        return true
    }
    
    private fun handleSetLobbyRegion(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("✗ Solo jugadores pueden usar este comando", NamedTextColor.RED))
            return true
        }
        
        val cuboid = SelectionManager.getSelection(sender)
        if (cuboid == null) {
            sender.sendMessage(Component.text("✗ No tienes una selección válida", NamedTextColor.RED))
            sender.sendMessage(Component.text("1. Usa /torneo wand para obtener la varita", NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("2. Clic izquierdo y derecho para seleccionar región", NamedTextColor.YELLOW))
            return true
        }
        
        TournamentFlowManager.setLobbyRegion(cuboid)
        SelectionManager.clearSelection(sender)
        
        sender.sendMessage(Component.text("✓ Región del lobby establecida", NamedTextColor.GREEN))
        sender.sendMessage(Component.text("  Centro: ${formatLocation(cuboid.getCenter())}", NamedTextColor.GRAY))
        
        return true
    }
    
    private fun handleAddSpawn(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("✗ Solo jugadores pueden usar este comando", NamedTextColor.RED))
            return true
        }
        
        TournamentFlowManager.addLobbySpawn(sender.location)
        
        val totalSpawns = TournamentFlowManager.getLobbySpawns().size
        sender.sendMessage(Component.text("✓ Spawn añadido al lobby", NamedTextColor.GREEN))
        sender.sendMessage(Component.text("  Total de spawns: $totalSpawns", NamedTextColor.GRAY))
        
        return true
    }
    
    private fun handleClearSpawns(sender: CommandSender): Boolean {
        TournamentFlowManager.clearLobbySpawns()
        sender.sendMessage(Component.text("✓ Todos los spawns del lobby han sido eliminados", NamedTextColor.YELLOW))
        return true
    }
    
    private fun handleStart(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 2) {
            sender.sendMessage(Component.text("✗ Uso: /torneo start <minigame>", NamedTextColor.RED))
            sender.sendMessage(Component.text("Minijuegos disponibles:", NamedTextColor.YELLOW))
            torneoManager.getAvailableMinigames().forEach { minigame ->
                sender.sendMessage(Component.text("  - ${minigame.gameName}", NamedTextColor.GRAY))
            }
            return true
        }
        
        // Unir todos los argumentos después de "start" para soportar nombres con espacios
        // Ejemplo: /torneo start Carrera de Barcos -> "Carrera de Barcos"
        val minigameName = args.drop(1).joinToString(" ")
        
        sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("  Iniciando Minijuego", NamedTextColor.YELLOW, TextDecoration.BOLD))
        sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("  Buscando: '$minigameName'", NamedTextColor.GRAY))
        
        val error = TournamentFlowManager.startMinigame(minigameName, torneoManager)
        
        if (error != null) {
            sender.sendMessage(Component.text("✗ $error", NamedTextColor.RED))
            sender.sendMessage(Component.empty())
            sender.sendMessage(Component.text("Minijuegos disponibles:", NamedTextColor.YELLOW))
            torneoManager.getAvailableMinigames().forEach { minigame ->
                sender.sendMessage(Component.text("  - ${minigame.gameName}", NamedTextColor.GRAY))
            }
        } else {
            sender.sendMessage(Component.text("✓ Minijuego iniciado exitosamente", NamedTextColor.GREEN))
            sender.sendMessage(Component.text("  Usa /torneo end para finalizarlo", NamedTextColor.GRAY))
        }
        
        sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.GOLD))
        
        return true
    }
    
    private fun handleEnd(sender: CommandSender): Boolean {
        sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("  Finalizando Minijuego", NamedTextColor.YELLOW, TextDecoration.BOLD))
        sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.GOLD))
        
        val error = TournamentFlowManager.endCurrentMinigame()
        
        if (error != null) {
            sender.sendMessage(Component.text("✗ $error", NamedTextColor.RED))
        } else {
            sender.sendMessage(Component.text("✓ Minijuego finalizado exitosamente", NamedTextColor.GREEN))
            sender.sendMessage(Component.text("  Jugadores devueltos al lobby", NamedTextColor.GRAY))
        }
        
        sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.GOLD))
        
        return true
    }
    
    private fun handleStatus(sender: CommandSender): Boolean {
        sender.sendMessage(Component.text("╔════════════════════════════════════╗", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("║     Estado del Torneo MMT        ║", NamedTextColor.YELLOW, TextDecoration.BOLD))
        sender.sendMessage(Component.text("╚════════════════════════════════════╝", NamedTextColor.GOLD))
        sender.sendMessage(Component.empty())
        
        // Lobby
        val lobbyRegion = TournamentFlowManager.getLobbyRegion()
        val lobbySpawns = TournamentFlowManager.getLobbySpawns().size
        val playersInLobby = TournamentFlowManager.getPlayersInLobby().size
        
        sender.sendMessage(Component.text("Lobby Global:", NamedTextColor.AQUA, TextDecoration.BOLD))
        sender.sendMessage(Component.text("  Región configurada: ", NamedTextColor.GRAY)
            .append(Component.text(if (lobbyRegion != null) "✓ Sí" else "✗ No", if (lobbyRegion != null) NamedTextColor.GREEN else NamedTextColor.RED)))
        sender.sendMessage(Component.text("  Spawns configurados: $lobbySpawns", NamedTextColor.GRAY))
        sender.sendMessage(Component.text("  Jugadores en lobby: $playersInLobby", NamedTextColor.GRAY))
        sender.sendMessage(Component.empty())
        
        // Minijuego activo
        val activeMinigame = TournamentFlowManager.activeMinigame
        sender.sendMessage(Component.text("Minijuego Activo:", NamedTextColor.AQUA, TextDecoration.BOLD))
        if (activeMinigame != null) {
            val activePlayers = activeMinigame.getActivePlayers().size
            sender.sendMessage(Component.text("  Nombre: ${activeMinigame.gameName}", NamedTextColor.GREEN))
            sender.sendMessage(Component.text("  Jugadores activos: $activePlayers", NamedTextColor.GRAY))
        } else {
            sender.sendMessage(Component.text("  Ninguno", NamedTextColor.GRAY))
        }
        sender.sendMessage(Component.empty())
        
        // Minijuegos disponibles
        val availableMinigames = torneoManager.getAvailableMinigames()
        sender.sendMessage(Component.text("Minijuegos Disponibles: ${availableMinigames.size}", NamedTextColor.AQUA, TextDecoration.BOLD))
        availableMinigames.forEach { minigame ->
            sender.sendMessage(Component.text("  • ${minigame.gameName}", NamedTextColor.GRAY))
        }
        
        sender.sendMessage(Component.empty())
        return true
    }
    
    private fun formatLocation(loc: org.bukkit.Location): String {
        return "(${loc.blockX}, ${loc.blockY}, ${loc.blockZ})"
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (args.size == 1) {
            return listOf("wand", "setlobbyregion", "addspawn", "clearspawns", "start", "end", "status")
                .filter { it.startsWith(args[0].lowercase()) }
        }
        
        if (args.size == 2 && args[0].equals("start", true)) {
            return torneoManager.getAvailableMinigames()
                .map { it.gameName }
                .filter { it.lowercase().startsWith(args[1].lowercase()) }
        }
        
        return emptyList()
    }
}
