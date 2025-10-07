package los5fantasticos.torneo.api

import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * Interfaz que deben implementar todos los módulos de minijuegos.
 * Define el contrato para la inicialización, limpieza y gestión de minijuegos.
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
     * Notifica al minijuego que un jugador ha ganado puntos.
     * El minijuego debe comunicarse con el TorneoManager para actualizar el puntaje global.
     * 
     * @param player Jugador que ganó puntos
     * @param points Cantidad de puntos ganados
     * @param reason Razón por la que ganó puntos (ej: "Victoria", "Objetivo completado")
     */
    fun awardPoints(player: Player, points: Int, reason: String)
}
