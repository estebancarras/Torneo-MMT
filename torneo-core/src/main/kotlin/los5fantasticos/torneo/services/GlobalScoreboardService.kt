package los5fantasticos.torneo.services

import los5fantasticos.torneo.core.TorneoManager
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
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
 * 
 * Arquitectura:
 * - Un único Scoreboard compartido
 * - Teams invisibles pre-registrados para cada línea
 * - Actualización mediante modificación de prefix/suffix de Teams
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
        
        // Crear objetivo con título estilizado
        objective = scoreboard.registerNewObjective(
            "torneo_global",
            "dummy",
            "${ChatColor.GOLD}${ChatColor.BOLD}⭐ ${ChatColor.YELLOW}${ChatColor.BOLD}TORNEO ${ChatColor.GOLD}${ChatColor.BOLD}⭐"
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
     */
    private fun updateScoreboard() {
        val ranking = torneoManager.getGlobalRanking(10)
        
        // Línea 12: Espacio superior
        setLine(12, "")
        
        // Líneas 11-2: Top 10 jugadores
        ranking.forEachIndexed { index, playerScore ->
            val position = index + 1
            val medal = when (position) {
                1 -> "§6§l1°"
                2 -> "§7§l2°"
                3 -> "§6§l3°"
                else -> "§f${position}°"
            }
            
            val name = playerScore.playerName.let {
                if (it.length > 12) it.substring(0, 12) else it
            }
            
            val line = "  $medal §f$name §7- §e${playerScore.totalPoints}pts"
            setLine(11 - index, line)
        }
        
        // Limpiar líneas no usadas si hay menos de 10 jugadores
        for (i in (10 - ranking.size) downTo 2) {
            setLine(i, "")
        }
        
        // Línea 1: Espacio inferior
        setLine(1, "")
        
        // Línea 0: Pie de página
        setLine(0, "§7§oActualizado cada 4s")
    }
    
    /**
     * Establece el contenido de una línea usando Teams.
     * 
     * @param line Número de línea (0-12)
     * @param content Contenido de la línea
     */
    private fun setLine(line: Int, content: String) {
        val team = lineTeams[line] ?: return
        val entry = getColorCode(line)
        
        // Establecer el score para posicionar la línea
        objective.getScore(entry).score = line
        
        // Actualizar el contenido mediante prefix (limitado a 64 caracteres)
        team.prefix = if (content.length > 64) content.substring(0, 64) else content
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
