package los5fantasticos.torneo.services

import los5fantasticos.torneo.core.TorneoManager
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
import org.bukkit.scoreboard.Team

/**
 * Servicio global de scoreboard para el torneo.
 * 
 * Responsabilidades:
 * - Crear y mantener un scoreboard único para todos los jugadores
 * - Actualizar el ranking top 10 cada 4 segundos
 * - Usar técnica de Team para evitar parpadeo en las actualizaciones
 * - Usar Adventure API exclusivamente (CERO ChatColor)
 * 
 * Arquitectura:
 * - Un único Scoreboard compartido
 * - Teams invisibles pre-registrados para cada línea
 * - Actualización mediante modificación de prefix/suffix de Teams
 * - Todos los textos son Component (Adventure API)
 */
class GlobalScoreboardService(
    private val plugin: Plugin,
    private val torneoManager: TorneoManager
) {
    
    private lateinit var scoreboard: Scoreboard
    private lateinit var objective: Objective
    private val lineTeams = mutableMapOf<Int, Team>()
    private var updateTask: BukkitTask? = null
    
    /**
     * Inicializa el servicio de scoreboard.
     * Crea el scoreboard, objetivo y teams pre-configurados.
     */
    fun initialize() {
        // Crear scoreboard único
        scoreboard = Bukkit.getScoreboardManager()!!.newScoreboard
        
        // Crear objetivo con título estilizado usando Adventure API
        val title = Component.text("⭐ ", NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text("TORNEO ", NamedTextColor.YELLOW, TextDecoration.BOLD))
            .append(Component.text("⭐", NamedTextColor.GOLD, TextDecoration.BOLD))
        
        objective = scoreboard.registerNewObjective(
            "torneo_global",
            Criteria.DUMMY,
            title
        )
        objective.displaySlot = DisplaySlot.SIDEBAR
        
        // Pre-registrar teams para cada línea (10 líneas de ranking + espacios)
        for (i in 0..12) {
            val team = scoreboard.registerNewTeam("line_$i")
            team.addEntry(getColorCode(i))
            lineTeams[i] = team
        }
        
        plugin.logger.info("✓ GlobalScoreboardService inicializado")
    }
    
    /**
     * Asigna el scoreboard a un jugador.
     * 
     * @param player Jugador que verá el scoreboard
     */
    fun showToPlayer(player: Player) {
        player.scoreboard = scoreboard
    }
    
    /**
     * Inicia la tarea repetitiva de actualización.
     * Se ejecuta cada 4 segundos (80 ticks).
     */
    fun startUpdating() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            updateScoreboard()
        }, 0L, 80L) // 80 ticks = 4 segundos
        
        plugin.logger.info("✓ Tarea de actualización de scoreboard iniciada")
    }
    
    /**
     * Actualiza el contenido del scoreboard con el top 10.
     * Usa la técnica de Teams para evitar parpadeo.
     * Todos los textos usan Adventure API.
     */
    private fun updateScoreboard() {
        val ranking = torneoManager.getGlobalRanking(10)
        
        // Línea 12: Espacio superior
        setLine(12, Component.empty())
        
        // Líneas 11-2: Top 10 jugadores
        ranking.forEachIndexed { index, playerScore ->
            val position = index + 1
            val medal = when (position) {
                1 -> Component.text("1°", NamedTextColor.GOLD, TextDecoration.BOLD)
                2 -> Component.text("2°", NamedTextColor.GRAY, TextDecoration.BOLD)
                3 -> Component.text("3°", NamedTextColor.GOLD, TextDecoration.BOLD)
                else -> Component.text("${position}°", NamedTextColor.WHITE)
            }
            
            val name = playerScore.playerName.let {
                if (it.length > 12) it.substring(0, 12) else it
            }
            
            val line = Component.text("  ")
                .append(medal)
                .append(Component.text(" "))
                .append(Component.text(name, NamedTextColor.WHITE))
                .append(Component.text(" - ", NamedTextColor.GRAY))
                .append(Component.text("${playerScore.totalPoints}pts", NamedTextColor.YELLOW))
            
            setLine(11 - index, line)
        }
        
        // Limpiar líneas no usadas si hay menos de 10 jugadores
        for (i in (10 - ranking.size) downTo 2) {
            setLine(i, Component.empty())
        }
        
        // Línea 1: Espacio inferior
        setLine(1, Component.empty())
        
        // Línea 0: Pie de página
        val footer = Component.text("Actualizado cada 4s", NamedTextColor.GRAY, TextDecoration.ITALIC)
        setLine(0, footer)
    }
    
    /**
     * Establece el contenido de una línea usando Teams con Adventure API.
     * 
     * @param line Número de línea (0-12)
     * @param content Contenido de la línea (Component)
     */
    private fun setLine(line: Int, content: Component) {
        val team = lineTeams[line] ?: return
        val entry = getColorCode(line)
        
        // Establecer el score para posicionar la línea
        objective.getScore(entry).score = line
        
        // Actualizar el contenido mediante prefix usando Adventure API
        team.prefix(content)
    }
    
    /**
     * Obtiene un código de color único para cada línea.
     * Usado como entrada invisible del team.
     */
    private fun getColorCode(line: Int): String {
        val colors = listOf(
            "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7",
            "§8", "§9", "§a", "§b", "§c"
        )
        return colors.getOrElse(line) { "§f" } + "§r"
    }
    
    /**
     * Detiene el servicio y cancela la tarea de actualización.
     */
    fun shutdown() {
        updateTask?.cancel()
        updateTask = null
        plugin.logger.info("✓ GlobalScoreboardService detenido")
    }
}
