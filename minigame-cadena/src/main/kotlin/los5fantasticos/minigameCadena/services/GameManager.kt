package los5fantasticos.minigameCadena.services

import los5fantasticos.minigameCadena.MinigameCadena
import los5fantasticos.minigameCadena.game.CadenaGame
import los5fantasticos.minigameCadena.game.GameState
import los5fantasticos.minigameCadena.game.Team
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestiona todas las partidas activas del minijuego Cadena.
 * 
 * Responsabilidades:
 * - Crear y gestionar partidas
 * - Asignar jugadores a equipos
 * - Controlar el ciclo de vida de las partidas
 * - Mantener el estado global del juego
 */
class GameManager(private val minigame: MinigameCadena? = null) {
    
    /**
     * Mapa de partidas activas por ID.
     */
    private val activeGames = ConcurrentHashMap<UUID, CadenaGame>()
    
    /**
     * Mapa de jugadores a partidas (para búsqueda rápida).
     */
    private val playerToGame = ConcurrentHashMap<UUID, UUID>()
    
    /**
     * Partida en lobby esperando jugadores.
     * Solo puede haber una partida en lobby a la vez.
     */
    private var lobbyGame: CadenaGame? = null
    
    /**
     * Configuración: Número mínimo de jugadores para iniciar.
     */
    var minPlayers: Int = 2
        private set
    
    /**
     * Configuración: Número máximo de jugadores por partida.
     */
    var maxPlayers: Int = 8
        private set
    
    /**
     * Configuración: Tamaño máximo de equipo.
     */
    var maxTeamSize: Int = 4
        private set
    
    /**
     * Añade un jugador a una partida.
     * Si no hay partida en lobby, crea una nueva.
     * 
     * @param player Jugador a añadir
     * @return true si se añadió exitosamente, false si ya está en una partida
     */
    fun addPlayer(player: Player): Boolean {
        // Verificar si el jugador ya está en una partida
        if (playerToGame.containsKey(player.uniqueId)) {
            return false
        }
        
        // Obtener o crear partida en lobby
        val game = lobbyGame ?: createLobbyGame()
        
        // Verificar si la partida está llena
        if (game.getTotalPlayers() >= maxPlayers) {
            // Crear nueva partida si la actual está llena
            val newGame = createLobbyGame()
            return addPlayerToGame(player, newGame)
        }
        
        return addPlayerToGame(player, game)
    }
    
    /**
     * Añade un jugador a una partida específica.
     */
    private fun addPlayerToGame(player: Player, game: CadenaGame): Boolean {
        // Buscar un equipo con espacio o crear uno nuevo
        val team = game.teams.find { !it.isFull() } ?: run {
            val newTeam = Team()
            game.addTeam(newTeam)
            newTeam
        }
        
        // Añadir jugador al equipo
        team.addPlayer(player)
        
        // Registrar en mapas
        playerToGame[player.uniqueId] = game.id
        
        return true
    }
    
    /**
     * Crea una nueva partida en estado LOBBY.
     */
    private fun createLobbyGame(): CadenaGame {
        val game = CadenaGame()
        
        // Asignar arena aleatoria si hay arenas disponibles
        minigame?.arenaManager?.getRandomArena()?.let { arena ->
            game.arena = arena
        }
        
        activeGames[game.id] = game
        lobbyGame = game
        return game
    }
    
    /**
     * Remueve un jugador de su partida actual.
     * 
     * @param player Jugador a remover
     * @return true si se removió exitosamente
     */
    fun removePlayer(player: Player): Boolean {
        val gameId = playerToGame.remove(player.uniqueId) ?: return false
        val game = activeGames[gameId] ?: return false
        
        // Remover del equipo
        game.teams.forEach { team ->
            team.removePlayer(player)
        }
        
        // Limpiar equipos vacíos
        game.teams.removeIf { it.players.isEmpty() }
        
        // Si la partida está en lobby y se quedó vacía, eliminarla
        if (game.state == GameState.LOBBY && game.getTotalPlayers() == 0) {
            activeGames.remove(gameId)
            if (lobbyGame?.id == gameId) {
                lobbyGame = null
            }
        }
        
        return true
    }
    
    /**
     * Obtiene la partida en la que está un jugador.
     */
    fun getPlayerGame(player: Player): CadenaGame? {
        val gameId = playerToGame[player.uniqueId] ?: return null
        return activeGames[gameId]
    }
    
    /**
     * Obtiene el equipo de un jugador.
     */
    fun getPlayerTeam(player: Player): Team? {
        val game = getPlayerGame(player) ?: return null
        return game.teams.find { it.players.contains(player.uniqueId) }
    }
    
    /**
     * Inicia una partida (transición de LOBBY a COUNTDOWN).
     * 
     * @param game Partida a iniciar
     * @return true si se inició exitosamente
     */
    fun startGame(game: CadenaGame): Boolean {
        if (game.state != GameState.LOBBY) {
            return false
        }
        
        if (!game.hasMinimumPlayers()) {
            return false
        }
        
        game.state = GameState.COUNTDOWN
        
        // Si esta era la partida en lobby, limpiar referencia
        if (lobbyGame?.id == game.id) {
            lobbyGame = null
        }
        
        return true
    }
    
    /**
     * Finaliza una partida y limpia sus recursos.
     * 
     * @param game Partida a finalizar
     */
    fun endGame(game: CadenaGame) {
        game.state = GameState.FINISHED
        
        // PR3: Detener encadenamiento
        minigame?.chainService?.stopChaining(game)
        
        // Limpiar jugadores
        game.teams.forEach { team ->
            team.players.forEach { playerId ->
                playerToGame.remove(playerId)
            }
        }
        
        // Remover de activas
        activeGames.remove(game.id)
        
        // Limpiar servicios
        minigame?.scoreService?.clearGame(game.id.toString())
        
        // Si era la partida de lobby, limpiar referencia
        if (lobbyGame?.id == game.id) {
            lobbyGame = null
        }
    }
    
    /**
     * Obtiene todas las partidas activas.
     */
    fun getActiveGames(): List<CadenaGame> {
        return activeGames.values.toList()
    }
    
    /**
     * Obtiene la partida actual en lobby.
     */
    fun getLobbyGame(): CadenaGame? {
        return lobbyGame
    }
    
    /**
     * Verifica si un jugador está en una partida.
     */
    fun isPlayerInGame(player: Player): Boolean {
        return playerToGame.containsKey(player.uniqueId)
    }
    
    /**
     * Limpia todas las partidas (para deshabilitación del plugin).
     */
    fun clearAll() {
        activeGames.clear()
        playerToGame.clear()
        lobbyGame = null
    }
}
