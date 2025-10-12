package los5fantasticos.minigameCadena.services

import los5fantasticos.minigameCadena.MinigameCadena
import los5fantasticos.minigameCadena.game.CadenaGame
import los5fantasticos.minigameCadena.game.GameState
import los5fantasticos.minigameCadena.game.Team
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.util.UUID

/**
 * Servicio que gestiona el encadenamiento entre jugadores de un equipo.
 * 
 * Responsabilidades:
 * - Mantener a los jugadores dentro de una distancia máxima
 * - Aplicar fuerzas de arrastre suaves cuando se alejan
 * - Calcular el centro de masa del equipo
 * - Gestionar el encadenamiento durante la partida
 * 
 * Física:
 * - No usa teleport() para evitar rubber-banding
 * - Usa setVelocity() para un arrastre suave y natural
 * - La fuerza aumenta proporcionalmente a la distancia
 * - Reduce la fuerza si el jugador está en el aire (saltando)
 */
class ChainService(private val minigame: MinigameCadena) {
    
    /**
     * Configuración: Distancia máxima permitida entre jugadores (en bloques).
     * Leído desde cadena.yml: fisica-cadena.distancia-maxima
     */
    internal val maxDistance: Double
        get() = minigame.plugin.config.getDouble("fisica-cadena.distancia-maxima", 6.0)
    
    /**
     * Configuración: Fuerza base del tirón (multiplicador de velocidad).
     * Leído desde cadena.yml: fisica-cadena.fuerza-atraccion
     */
    internal val pullStrength: Double
        get() = minigame.plugin.config.getDouble("fisica-cadena.fuerza-atraccion", 0.3)
    
    /**
     * Configuración: Reducción de fuerza cuando el jugador está en el aire.
     * Leído desde cadena.yml: fisica-cadena.reduccion-fuerza-aire
     */
    private val airReduction: Double
        get() = minigame.plugin.config.getDouble("fisica-cadena.reduccion-fuerza-aire", 0.5)
    
    /**
     * Configuración: Cada cuántos ticks se ejecuta la tarea.
     * Leído desde cadena.yml: fisica-cadena.intervalo-ticks
     */
    private val tickInterval: Long
        get() = minigame.plugin.config.getLong("fisica-cadena.intervalo-ticks", 3)
    
    /**
     * Tareas activas de encadenamiento por partida.
     */
    private val activeTasks = mutableMapOf<UUID, BukkitTask>()
    
    /**
     * Inicia el encadenamiento para una partida.
     * 
     * @param game Partida para la cual activar el encadenamiento
     */
    fun startChaining(game: CadenaGame) {
        // Verificar que la partida esté en juego
        if (game.state != GameState.IN_GAME) {
            return
        }
        
        // Verificar si ya hay una tarea activa
        if (activeTasks.containsKey(game.id)) {
            return
        }
        
        // Crear tarea de encadenamiento
        val task = object : BukkitRunnable() {
            override fun run() {
                // Verificar si la partida sigue activa
                if (game.state != GameState.IN_GAME) {
                    cancel()
                    activeTasks.remove(game.id)
                    return
                }
                
                // Aplicar encadenamiento a cada equipo
                game.teams.forEach { team ->
                    applyChainPhysics(team)
                }
            }
        }
        
        // Ejecutar cada tickInterval ticks
        val bukkitTask = task.runTaskTimer(minigame.plugin, 0L, tickInterval)
        activeTasks[game.id] = bukkitTask
    }
    
    /**
     * Detiene el encadenamiento para una partida.
     * 
     * @param game Partida para la cual detener el encadenamiento
     */
    fun stopChaining(game: CadenaGame) {
        val task = activeTasks.remove(game.id)
        task?.cancel()
    }
    
    /**
     * Aplica la física de encadenamiento a un equipo.
     * 
     * @param team Equipo al cual aplicar el encadenamiento
     */
    private fun applyChainPhysics(team: Team) {
        val players = team.getOnlinePlayers()
        
        // Si hay menos de 2 jugadores, no hay encadenamiento
        if (players.size < 2) {
            return
        }
        
        // Aplicar fuerza entre cada par de jugadores
        for (i in players.indices) {
            for (j in i + 1 until players.size) {
                val player1 = players[i]
                val player2 = players[j]
                
                applyPullForceBetweenPlayers(player1, player2)
            }
        }
    }
    
    /**
     * Aplica una fuerza de tirón entre dos jugadores si están demasiado lejos.
     * 
     * @param player1 Primer jugador
     * @param player2 Segundo jugador
     */
    private fun applyPullForceBetweenPlayers(player1: Player, player2: Player) {
        val loc1 = player1.location
        val loc2 = player2.location
        
        // Calcular distancia entre los dos jugadores
        val distance = loc1.distance(loc2)
        
        // Si están dentro del rango permitido, no hacer nada
        if (distance <= maxDistance) {
            return
        }
        
        // Calcular la distancia excedida
        val excessDistance = distance - maxDistance
        
        // Calcular fuerza base
        val forceMagnitude = excessDistance * pullStrength
        
        // Aplicar fuerza a player1 hacia player2
        applyPullToPlayer(player1, loc2, forceMagnitude)
        
        // Aplicar fuerza a player2 hacia player1
        applyPullToPlayer(player2, loc1, forceMagnitude)
    }
    
    /**
     * Aplica una fuerza de tirón a un jugador hacia una ubicación.
     * 
     * @param player Jugador al cual aplicar la fuerza
     * @param targetLocation Ubicación objetivo
     * @param forceMagnitude Magnitud de la fuerza
     */
    private fun applyPullToPlayer(player: Player, targetLocation: Location, forceMagnitude: Double) {
        val playerLoc = player.location
        
        // Calcular vector de dirección hacia el objetivo
        val direction = targetLocation.toVector().subtract(playerLoc.toVector())
        
        // Normalizar el vector (longitud = 1)
        direction.normalize()
        
        // Aplicar el vector de fuerza
        val pullVector = direction.multiply(forceMagnitude)
        
        // Reducir la fuerza si el jugador está en el aire
        if (!player.isOnGround) {
            pullVector.multiply(airReduction)
        }
        
        // Aplicar la velocidad al jugador
        // Sumar a la velocidad actual para no interrumpir el movimiento
        val currentVelocity = player.velocity
        val newVelocity = currentVelocity.add(pullVector)
        
        // Limitar la velocidad máxima para evitar lanzamientos extremos
        val maxVelocity = 2.0
        if (newVelocity.length() > maxVelocity) {
            newVelocity.normalize().multiply(maxVelocity)
        }
        
        player.velocity = newVelocity
    }
    
    /**
     * Verifica si una partida tiene encadenamiento activo.
     */
    fun isChainActive(game: CadenaGame): Boolean {
        return activeTasks.containsKey(game.id)
    }
    
    /**
     * Limpia todas las tareas activas.
     */
    fun clearAll() {
        activeTasks.values.forEach { it.cancel() }
        activeTasks.clear()
    }
}
