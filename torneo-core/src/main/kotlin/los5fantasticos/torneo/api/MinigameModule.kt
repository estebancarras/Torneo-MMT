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
     * Inicia el minijuego en modo torneo para una lista de jugadores.
     * 
     * FLUJO CENTRALIZADO:
     * Este método es llamado por el TournamentFlowManager cuando un administrador
     * ejecuta /torneo start <minigame>. Todos los jugadores online son enviados
     * al minijuego simultáneamente.
     * 
     * IMPLEMENTACIÓN RECOMENDADA:
     * - Añadir todos los jugadores a la sala de espera del minijuego
     * - Teletransportarlos al lobby del minijuego
     * - NO iniciar el juego automáticamente - esperar comando admin específico
     * 
     * @param players Lista de jugadores que participarán en el minijuego
     */
    fun onTournamentStart(players: List<Player>)
    
    /**
     * Proporciona los ejecutores de comandos que este minijuego necesita registrar.
     * 
     * El mapa devuelto asocia nombres de comandos con sus ejecutores correspondientes.
     * El TorneoPlugin se encargará de registrar estos comandos centralizadamente.
     * 
     * @return Mapa de nombre de comando a CommandExecutor. Vacío si no hay comandos.
     */
    fun getCommandExecutors(): Map<String, CommandExecutor> = emptyMap()
    
    /**
     * Finaliza todas las partidas activas del minijuego sin deshabilitar el módulo.
     * 
     * Este método es llamado por /torneo end para terminar las partidas en curso
     * pero mantener el módulo habilitado y su configuración (arenas, etc.) intacta.
     * 
     * IMPLEMENTACIÓN RECOMENDADA:
     * - Finalizar todas las partidas activas
     * - Devolver jugadores al lobby
     * - Limpiar estado de partidas pero NO configuración persistente
     * - NO llamar a onDisable() internamente
     */
    fun endAllGames() {
        // Implementación por defecto: no hace nada
        // Los módulos que necesiten esta funcionalidad deben sobrescribirla
    }
}
