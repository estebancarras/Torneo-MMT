package los5fantasticos.minigameLaberinto.game

import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*

/**
 * Representa una instancia activa de una partida de Laberinto.
 * 
 * Gestiona el estado de la partida, los jugadores participantes,
 * y el progreso individual de cada jugador.
 */
class LaberintoGame(
    val arena: Arena,
    val gameId: String = UUID.randomUUID().toString()
) {
    
    /**
     * Estado actual de la partida.
     */
    var state: GameState = GameState.LOBBY
        private set
    
    /**
     * Lista de jugadores participantes.
     */
    val players = mutableListOf<Player>()
    
    /**
     * Mapa de progreso de cada jugador.
     * Clave: UUID del jugador, Valor: información de progreso
     */
    private val playerProgress = mutableMapOf<UUID, PlayerProgress>()
    
    /**
     * Cooldowns de jumpscares por jugador y ubicación.
     * Evita que se activen múltiples jumpscares en la misma zona.
     */
    private val jumpscareCooldowns = mutableMapOf<UUID, MutableMap<Location, Long>>()
    
    /**
     * Añade un jugador a la partida.
     * 
     * @param player Jugador a añadir
     * @return true si se añadió exitosamente, false si ya está en la partida o está llena
     */
    fun addPlayer(player: Player): Boolean {
        if (players.contains(player)) {
            return false
        }
        
        if (players.size >= arena.maxPlayers) {
            return false
        }
        
        players.add(player)
        playerProgress[player.uniqueId] = PlayerProgress()
        jumpscareCooldowns[player.uniqueId] = mutableMapOf()
        
        return true
    }
    
    /**
     * Remueve un jugador de la partida.
     * 
     * @param player Jugador a remover
     * @return true si se removió exitosamente, false si no estaba en la partida
     */
    fun removePlayer(player: Player): Boolean {
        if (!players.contains(player)) {
            return false
        }
        
        players.remove(player)
        playerProgress.remove(player.uniqueId)
        jumpscareCooldowns.remove(player.uniqueId)
        
        return true
    }
    
    /**
     * Marca a un jugador como finalizado.
     * 
     * @param player Jugador que finalizó
     */
    fun markPlayerFinished(player: Player) {
        playerProgress[player.uniqueId]?.hasFinished = true
    }
    
    /**
     * Verifica si un jugador ha finalizado.
     * 
     * @param player Jugador a verificar
     * @return true si ha finalizado, false en caso contrario
     */
    fun hasPlayerFinished(player: Player): Boolean {
        return playerProgress[player.uniqueId]?.hasFinished ?: false
    }
    
    /**
     * Obtiene el progreso de un jugador.
     * 
     * @param player Jugador
     * @return Progreso del jugador, o null si no está en la partida
     */
    fun getPlayerProgress(player: Player): PlayerProgress? {
        return playerProgress[player.uniqueId]
    }
    
    /**
     * Verifica si un jugador puede activar un jumpscare en una ubicación específica.
     * 
     * @param player Jugador
     * @param location Ubicación del jumpscare
     * @param cooldownSeconds Cooldown en segundos (por defecto 30)
     * @return true si puede activar el jumpscare, false en caso contrario
     */
    fun canActivateJumpscare(player: Player, location: Location, cooldownSeconds: Int = 30): Boolean {
        val playerCooldowns = jumpscareCooldowns[player.uniqueId] ?: return true
        val lastActivation = playerCooldowns[location] ?: return true
        
        val currentTime = System.currentTimeMillis()
        val cooldownMillis = cooldownSeconds * 1000L
        
        return (currentTime - lastActivation) >= cooldownMillis
    }
    
    /**
     * Registra la activación de un jumpscare para un jugador en una ubicación.
     * 
     * @param player Jugador
     * @param location Ubicación del jumpscare
     */
    fun recordJumpscareActivation(player: Player, location: Location) {
        val playerCooldowns = jumpscareCooldowns[player.uniqueId] ?: return
        playerCooldowns[location] = System.currentTimeMillis()
    }
    
    /**
     * Cambia el estado de la partida.
     * 
     * @param newState Nuevo estado
     */
    fun setState(newState: GameState) {
        state = newState
    }
    
    /**
     * Verifica si la partida puede iniciar.
     * 
     * @return true si tiene suficientes jugadores, false en caso contrario
     */
    fun canStart(): Boolean {
        return players.size >= arena.minPlayers && state == GameState.LOBBY
    }
    
    /**
     * Obtiene la lista de jugadores que han finalizado.
     * 
     * @return Lista de jugadores que completaron el laberinto
     */
    fun getFinishedPlayers(): List<Player> {
        return players.filter { hasPlayerFinished(it) }
    }
    
    /**
     * Obtiene la lista de jugadores que no han finalizado.
     * 
     * @return Lista de jugadores que no completaron el laberinto
     */
    fun getUnfinishedPlayers(): List<Player> {
        return players.filter { !hasPlayerFinished(it) }
    }
    
    /**
     * Limpia todos los datos de la partida.
     */
    fun clear() {
        players.clear()
        playerProgress.clear()
        jumpscareCooldowns.clear()
        state = GameState.LOBBY
    }
}

/**
 * Información de progreso de un jugador en la partida.
 */
data class PlayerProgress(
    var hasFinished: Boolean = false,
    var jumpscaresTriggered: Int = 0,
    var startTime: Long = System.currentTimeMillis()
)
