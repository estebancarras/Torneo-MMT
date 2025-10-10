package los5fantasticos.torneo.util

import los5fantasticos.torneo.TorneoPlugin
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

/**
 * Sistema de temporizador visual global y reutilizable utilizando BossBar.
 * 
 * Esta clase proporciona una forma estandarizada de mostrar temporizadores
 * en la parte superior de la pantalla de los jugadores durante las partidas.
 * 
 * Características:
 * - Visualización mediante BossBar de Bukkit API
 * - Actualización automática cada segundo
 * - Callbacks personalizables (onFinish, onTick)
 * - Gestión automática de jugadores
 * - Limpieza automática de recursos
 * 
 * @param plugin Instancia del plugin principal para registrar tareas
 * @param durationInSeconds Duración total del temporizador en segundos
 * @param title Título principal que aparecerá en la BossBar
 * @param onFinish Función que se ejecutará cuando el temporizador llegue a cero
 * @param onTick Función opcional que se ejecutará cada segundo con los segundos restantes
 * 
 * @author Los 5 Fantásticos
 * @since 1.0
 */
class GameTimer(
    private val plugin: TorneoPlugin,
    private val durationInSeconds: Int,
    private val title: String,
    private val onFinish: () -> Unit,
    private val onTick: (Int) -> Unit = {}
) {
    
    /**
     * BossBar que se mostrará a los jugadores.
     */
    private val bossBar: BossBar = Bukkit.createBossBar(
        title,
        BarColor.GREEN,
        BarStyle.SOLID
    )
    
    /**
     * Tarea de Bukkit que gestiona la cuenta atrás.
     */
    private var timerTask: BukkitTask? = null
    
    /**
     * Segundos restantes en el temporizador.
     */
    private var secondsLeft: Int = durationInSeconds
    
    /**
     * Indica si el temporizador está actualmente en ejecución.
     */
    private var isRunning: Boolean = false
    
    /**
     * Añade un jugador al temporizador, mostrándole la BossBar.
     * 
     * @param player Jugador al que mostrar la BossBar
     */
    fun addPlayer(player: Player) {
        bossBar.addPlayer(player)
    }
    
    /**
     * Añade múltiples jugadores al temporizador.
     * 
     * @param players Colección de jugadores a añadir
     */
    fun addPlayers(players: Collection<Player>) {
        players.forEach { addPlayer(it) }
    }
    
    /**
     * Remueve un jugador del temporizador, ocultándole la BossBar.
     * 
     * @param player Jugador al que ocultar la BossBar
     */
    fun removePlayer(player: Player) {
        bossBar.removePlayer(player)
    }
    
    /**
     * Inicia la cuenta atrás del temporizador.
     * 
     * El temporizador se actualizará cada segundo (20 ticks), actualizando:
     * - El progreso de la BossBar
     * - El texto mostrando el tiempo restante
     * - El color de la barra según el tiempo restante
     * 
     * Cuando el tiempo llegue a cero, se ejecutará la función onFinish.
     */
    fun start() {
        if (isRunning) {
            plugin.logger.warning("Intento de iniciar un GameTimer que ya está en ejecución")
            return
        }
        
        isRunning = true
        secondsLeft = durationInSeconds
        
        // Actualizar inmediatamente
        updateBossBar()
        
        // Crear tarea que se ejecuta cada segundo
        timerTask = object : BukkitRunnable() {
            override fun run() {
                secondsLeft--
                
                // Actualizar BossBar
                updateBossBar()
                
                // Llamar callback onTick
                onTick(secondsLeft)
                
                // Verificar si terminó
                if (secondsLeft <= 0) {
                    isRunning = false
                    cancel()
                    
                    // Ocultar BossBar a todos los jugadores
                    bossBar.removeAll()
                    
                    // Llamar callback onFinish
                    onFinish()
                }
            }
        }.runTaskTimer(plugin, 20L, 20L) // Cada segundo (20 ticks)
    }
    
    /**
     * Detiene el temporizador y oculta la BossBar a todos los jugadores.
     * 
     * Este método debe ser llamado cuando la partida termina antes de que
     * el tiempo se agote, para limpiar correctamente los recursos.
     */
    fun stop() {
        if (!isRunning) {
            return
        }
        
        isRunning = false
        
        // Cancelar tarea
        timerTask?.cancel()
        timerTask = null
        
        // Ocultar BossBar a todos los jugadores
        bossBar.removeAll()
    }
    
    /**
     * Actualiza el contenido de la BossBar (texto, progreso y color).
     */
    private fun updateBossBar() {
        // Calcular progreso (0.0 a 1.0)
        val progress = secondsLeft.toDouble() / durationInSeconds.toDouble()
        bossBar.progress = progress.coerceIn(0.0, 1.0)
        
        // Actualizar color según tiempo restante
        val color = when {
            secondsLeft <= 10 -> BarColor.RED
            secondsLeft <= 30 -> BarColor.YELLOW
            secondsLeft <= 60 -> BarColor.YELLOW
            else -> BarColor.GREEN
        }
        bossBar.color = color
        
        // Formatear tiempo restante (MM:SS)
        val minutes = secondsLeft / 60
        val seconds = secondsLeft % 60
        val timeText = String.format("%d:%02d", minutes, seconds)
        
        // Actualizar título con tiempo restante
        val updatedTitle = "$title - §eTiempo restante: §f§l$timeText"
        bossBar.setTitle(updatedTitle)
    }
    
    /**
     * Obtiene los segundos restantes en el temporizador.
     * 
     * @return Segundos restantes
     */
    fun getSecondsLeft(): Int = secondsLeft
    
    /**
     * Verifica si el temporizador está en ejecución.
     * 
     * @return true si está en ejecución, false en caso contrario
     */
    fun isRunning(): Boolean = isRunning
}
