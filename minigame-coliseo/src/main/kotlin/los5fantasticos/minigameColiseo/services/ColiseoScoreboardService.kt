package los5fantasticos.minigameColiseo.services

import los5fantasticos.torneo.services.GlobalScoreboardService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import java.util.UUID

/**
 * Servicio de scoreboard dedicado para el Coliseo.
 * 
 * Responsabilidades:
 * - Mostrar información de la partida en tiempo real
 * - Rastrear kills individuales
 * - Calcular puntos acumulados en el juego
 * - Coexistir con el scoreboard global del torneo
 */
class ColiseoScoreboardService(
    private val plugin: Plugin,
    private val gameManager: GameManager,
    private val teamManager: TeamManager,
    private val globalScoreboardService: GlobalScoreboardService
) {
    
    // Mapa para guardar los scoreboards individuales de cada jugador
    private val playerBoards = mutableMapOf<UUID, Scoreboard>()
    
    // Mapa para guardar las tareas de actualización por jugador
    private val updateTasks = mutableMapOf<UUID, BukkitTask>()
    
    // Estructura para almacenar las kills por jugador en la partida actual
    private val gameKills = mutableMapOf<UUID, Int>()
    
    /**
     * Registra una kill para un jugador.
     */
    fun recordKill(player: Player) {
        val currentKills = gameKills.getOrDefault(player.uniqueId, 0)
        gameKills[player.uniqueId] = currentKills + 1
    }
    
    /**
     * Reinicia el contador de kills.
     * Debe ser llamado al inicio de cada partida.
     */
    fun resetKills() {
        gameKills.clear()
    }
    
    /**
     * Muestra el scoreboard del Coliseo a un jugador.
     */
    fun showScoreboard(player: Player) {
        // Ocultar el scoreboard global
        globalScoreboardService.hideScoreboard(player)
        
        // Crear nuevo scoreboard para el jugador
        val board = Bukkit.getScoreboardManager()!!.newScoreboard
        
        // IMPORTANTE: Copiar los equipos del mainScoreboard para mantener los colores de brillo
        copyTeamsFromMainScoreboard(board)
        
        // Crear objetivo con título estilizado
        val title = Component.text("⚔ ", NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text("COLISEO", NamedTextColor.RED, TextDecoration.BOLD))
            .append(Component.text(" ⚔", NamedTextColor.GOLD, TextDecoration.BOLD))
        
        val objective = board.registerNewObjective(
            "coliseo_game",
            Criteria.DUMMY,
            title
        )
        objective.displaySlot = DisplaySlot.SIDEBAR
        
        // Aplicar el scoreboard al jugador
        player.scoreboard = board
        playerBoards[player.uniqueId] = board
        
        // Actualizar contenido inicial
        updateScoreboardContent(player, board, objective)
        
        // Iniciar tarea de actualización cada segundo (20 ticks)
        val task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            updateScoreboardContent(player, board, objective)
        }, 20L, 20L)
        
        updateTasks[player.uniqueId] = task
    }
    
    /**
     * Copia los equipos del mainScoreboard al scoreboard personalizado.
     * Esto es necesario para mantener los colores de brillo (glowing effect).
     */
    private fun copyTeamsFromMainScoreboard(targetBoard: org.bukkit.scoreboard.Scoreboard) {
        val mainScoreboard = Bukkit.getScoreboardManager()!!.mainScoreboard
        
        // Copiar equipo Élite
        mainScoreboard.getTeam("ColiseoElite")?.let { mainTeam ->
            val newTeam = targetBoard.registerNewTeam("ColiseoElite")
            // Copiar color (convertir TextColor a NamedTextColor si es necesario)
            mainTeam.color()?.let { color ->
                if (color is NamedTextColor) {
                    newTeam.color(color)
                } else {
                    newTeam.color(NamedTextColor.YELLOW) // Fallback a amarillo
                }
            }
            newTeam.prefix(mainTeam.prefix())
            newTeam.suffix(mainTeam.suffix())
            newTeam.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.NEVER)
            
            // Copiar todas las entradas (jugadores)
            mainTeam.entries.forEach { entry ->
                newTeam.addEntry(entry)
            }
        }
        
        // Copiar equipo Horda
        mainScoreboard.getTeam("ColiseoHorde")?.let { mainTeam ->
            val newTeam = targetBoard.registerNewTeam("ColiseoHorde")
            // Copiar color (convertir TextColor a NamedTextColor si es necesario)
            mainTeam.color()?.let { color ->
                if (color is NamedTextColor) {
                    newTeam.color(color)
                } else {
                    newTeam.color(NamedTextColor.WHITE) // Fallback a blanco
                }
            }
            newTeam.prefix(mainTeam.prefix())
            newTeam.suffix(mainTeam.suffix())
            newTeam.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.NEVER)
            
            // Copiar todas las entradas (jugadores)
            mainTeam.entries.forEach { entry ->
                newTeam.addEntry(entry)
            }
        }
    }
    
    /**
     * Actualiza el contenido del scoreboard de un jugador.
     */
    private fun updateScoreboardContent(player: Player, board: Scoreboard, objective: Objective) {
        val game = gameManager.getActiveGame() ?: return
        
        // Limpiar scores anteriores
        board.entries.forEach { entry ->
            board.resetScores(entry)
        }
        
        // Obtener datos actuales
        val eliteAlive = game.elitePlayers.size
        val eliteTotal = game.elitePlayers.size + game.eliminatedPlayers.count { game.isElite(it) }
        val hordeAlive = game.hordePlayers.size
        val hordeTotal = game.hordePlayers.size + game.eliminatedPlayers.count { game.isHorde(it) }
        val myKills = gameKills.getOrDefault(player.uniqueId, 0)
        val myPoints = myKills * 10
        
        var lineNumber = 10
        
        // Línea en blanco superior
        objective.getScore(getColorCode(lineNumber--)).score = lineNumber + 1
        
        // Élite Restante
        val eliteLine = Component.text("Élite: ", NamedTextColor.GOLD)
            .append(Component.text("$eliteAlive", NamedTextColor.WHITE))
            .append(Component.text(" / ", NamedTextColor.GRAY))
            .append(Component.text("$eliteTotal", NamedTextColor.WHITE))
        setScoreLine(board, objective, lineNumber--, eliteLine)
        
        // Horda Restante
        val hordeLine = Component.text("Horda: ", NamedTextColor.WHITE)
            .append(Component.text("$hordeAlive", NamedTextColor.WHITE))
            .append(Component.text(" / ", NamedTextColor.GRAY))
            .append(Component.text("$hordeTotal", NamedTextColor.WHITE))
        setScoreLine(board, objective, lineNumber--, hordeLine)
        
        // Línea en blanco
        objective.getScore(getColorCode(lineNumber--)).score = lineNumber + 1
        
        // Mis Kills
        val killsLine = Component.text("Mis Kills: ", NamedTextColor.YELLOW)
            .append(Component.text("$myKills", NamedTextColor.GREEN, TextDecoration.BOLD))
        setScoreLine(board, objective, lineNumber--, killsLine)
        
        // Puntos (Juego)
        val pointsLine = Component.text("Puntos: ", NamedTextColor.AQUA)
            .append(Component.text("$myPoints", NamedTextColor.GOLD, TextDecoration.BOLD))
        setScoreLine(board, objective, lineNumber--, pointsLine)
        
        // Línea en blanco
        objective.getScore(getColorCode(lineNumber--)).score = lineNumber + 1
        
        // Pie de página
        val footer = Component.text("Los 5 Fantásticos", NamedTextColor.GRAY, TextDecoration.ITALIC)
        setScoreLine(board, objective, lineNumber--, footer)
    }
    
    /**
     * Establece una línea del scoreboard con un componente.
     */
    private fun setScoreLine(board: Scoreboard, objective: Objective, line: Int, content: Component) {
        val entry = getColorCode(line)
        val team = board.getTeam("line_$line") ?: board.registerNewTeam("line_$line")
        team.prefix(content)
        team.addEntry(entry)
        objective.getScore(entry).score = line
    }
    
    /**
     * Obtiene un código de color único para cada línea.
     */
    private fun getColorCode(line: Int): String {
        val colors = listOf(
            "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7",
            "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"
        )
        return colors.getOrElse(line) { "§f" } + "§r"
    }
    
    /**
     * Oculta el scoreboard del Coliseo y restaura el global.
     */
    fun hideScoreboard(player: Player) {
        // Cancelar tarea de actualización
        updateTasks[player.uniqueId]?.cancel()
        updateTasks.remove(player.uniqueId)
        
        // Remover scoreboard
        playerBoards.remove(player.uniqueId)
        
        // Restaurar scoreboard global
        globalScoreboardService.showScoreboard(player)
    }
}
