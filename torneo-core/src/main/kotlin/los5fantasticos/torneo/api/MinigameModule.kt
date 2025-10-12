package los5fantasticos.torneo.api

import org.bukkit.command.CommandExecutor
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * Interfaz que deben implementar todos los módulos de minijuegos.
 * Define el contrato para la inicialización, limpieza y gestión de minijuegos.
 * 
 * Esta interfaz sigue el patrón Service Provider Interface (SPI) de Java,
 * permitiendo el descubrimiento automático de módulos mediante ServiceLoader.
 * 
 * NOTA: La asignación de puntos NO es responsabilidad de esta interfaz.
 * Cada minijuego debe implementar su propio ScoreService dedicado que use
 * TorneoManager.addScore(UUID, ...) como punto único de entrada.
 */
interface MinigameModule {
    
    /**
     * Nombre único del minijuego
     */
    val gameName: String
    
    /**
     * Versión del minijuego
     */
    val version: String
    
    /**
     * Descripción del minijuego
     */
    val description: String
    
    /**
     * Inicializa el minijuego.
     * Se llama cuando el plugin principal se habilita.
     * 
     * @param plugin Instancia del plugin principal
     */
    fun onEnable(plugin: Plugin)
    
    /**
     * Limpia y deshabilita el minijuego.
     * Se llama cuando el plugin principal se deshabilita.
     */
    fun onDisable()
    
    /**
     * Verifica si el minijuego está actualmente en ejecución.
     * 
     * @return true si hay una partida activa, false en caso contrario
     */
    fun isGameRunning(): Boolean
    
    /**
     * Obtiene la lista de jugadores actualmente participando en el minijuego.
     * 
     * @return Lista de jugadores en el juego
     */
    fun getActivePlayers(): List<Player>
    
    /**
     * Proporciona los ejecutores de comandos que este minijuego necesita registrar.
     * 
     * El mapa devuelto asocia nombres de comandos con sus ejecutores correspondientes.
     * El TorneoPlugin se encargará de registrar estos comandos centralizadamente.
     * 
     * @return Mapa de nombre de comando a CommandExecutor. Vacío si no hay comandos.
     */
    fun getCommandExecutors(): Map<String, CommandExecutor> = emptyMap()
}
