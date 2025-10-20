package los5fantasticos.minigameCarrerabarcos.game

import los5fantasticos.torneo.util.selection.Cuboid
import org.bukkit.Location

/**
 * Representa la configuración completa de un circuito de carreras.
 * 
 * Esta clase contiene toda la información necesaria para ejecutar una carrera:
 * - Ubicaciones de spawn para los jugadores
 * - Checkpoints ordenados que los jugadores deben atravesar
 * - Región de la meta
 * - Lobby de espera
 * 
 * ARQUITECTURA:
 * Esta clase es SOLO datos. No contiene lógica de juego.
 * La lógica de carrera está en GameManager.
 */
data class ArenaCarrera(
    /**
     * Nombre único del circuito.
     */
    val nombre: String,
    
    /**
     * Ubicación del lobby de espera antes de iniciar la carrera.
     * Los jugadores esperan aquí antes de ser teletransportados a los spawns.
     */
    var lobby: Location? = null,
    
    /**
     * Lista de posiciones de inicio para los jugadores.
     * El orden determina qué jugador va en qué posición.
     */
    val spawns: MutableList<Location> = mutableListOf(),
    
    /**
     * Lista ORDENADA de checkpoints que los jugadores deben atravesar.
     * El índice en la lista determina el orden de la carrera.
     * 
     * Cada checkpoint es una región Cuboid que el jugador debe atravesar.
     */
    val checkpoints: MutableList<Cuboid> = mutableListOf(),
    
    /**
     * Región de la meta. 
     * Cuando un jugador entra en esta región después de pasar todos los checkpoints, gana.
     */
    var meta: Cuboid? = null,
    
    /**
     * Región de protección del circuito (opcional).
     * Si está configurada, los jugadores no pueden salir de esta región durante la carrera.
     */
    var protectionRegion: Cuboid? = null
) {
    
    /**
     * Verifica si el circuito está completamente configurado y listo para usarse.
     */
    fun isValid(): Boolean {
        return lobby != null 
            && spawns.isNotEmpty() 
            && checkpoints.isNotEmpty() 
            && meta != null
    }
    
    /**
     * Obtiene un resumen de la configuración del circuito.
     */
    fun getSummary(): String {
        return buildString {
            appendLine("Circuito: $nombre")
            appendLine("  Lobby: ${if (lobby != null) "✓ Configurado" else "✗ No configurado"}")
            appendLine("  Spawns: ${spawns.size} posiciones")
            appendLine("  Checkpoints: ${checkpoints.size} puntos")
            appendLine("  Meta: ${if (meta != null) "✓ Configurada" else "✗ No configurada"}")
            appendLine("  Protección: ${if (protectionRegion != null) "✓ Configurada" else "✗ No configurada"}")
            appendLine("  Estado: ${if (isValid()) "✓ Listo para usar" else "✗ Configuración incompleta"}")
        }
    }
    
    /**
     * Obtiene la capacidad máxima de jugadores (basado en spawns disponibles).
     */
    fun getMaxPlayers(): Int {
        return spawns.size
    }
}
