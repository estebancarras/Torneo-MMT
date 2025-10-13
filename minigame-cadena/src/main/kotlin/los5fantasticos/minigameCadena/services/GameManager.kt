package los5fantasticos.minigameCadena.services

import los5fantasticos.minigameCadena.MinigameCadena
import los5fantasticos.minigameCadena.game.CadenaGame
import los5fantasticos.minigameCadena.game.GameState
import los5fantasticos.minigameCadena.game.Team
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
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
class GameManager(
    private val minigame: MinigameCadena? = null,
    private val chainVisualizerService: ChainVisualizerService? = null
) {
    
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
            val result = addPlayerToGame(player, newGame)
            
            // Verificar si debe iniciar cuenta regresiva
            if (result) {
                minigame?.checkStartCountdown(newGame)
            }
            
            return result
        }
        
        val result = addPlayerToGame(player, game)
        
        // Verificar si debe iniciar cuenta regresiva
        if (result) {
            minigame?.checkStartCountdown(game)
        }
        
        return result
    }
    
    /**
     * Añade un jugador a una partida específica.
     * El jugador NO es añadido automáticamente a un equipo - debe elegir uno usando la UI.
     * Se encarga de teletransportar al jugador al lobby y entregarle la UI de selección.
     */
    private fun addPlayerToGame(player: Player, game: CadenaGame): Boolean {
        // Verificar si hay espacio en la partida (máximo 16 jugadores = 4 equipos × 4 jugadores)
        if (game.getTotalPlayers() >= 16) {
            return false
        }
        
        // Registrar jugador en la partida (SIN añadirlo a un equipo todavía)
        playerToGame[player.uniqueId] = game.id
        
        // Teletransportar al lobby (si está configurado)
        minigame?.arenaManager?.getLobbyLocation()?.let { lobbyLocation ->
            player.teleport(lobbyLocation)
        }
        
        // Limpiar inventario y entregar UI de selección de equipos
        player.inventory.clear()
        giveTeamSelectionItems(player, game)
        
        // Notificar al jugador
        player.sendMessage("${ChatColor.GREEN}¡Bienvenido al lobby de Cadena!")
        player.sendMessage("${ChatColor.YELLOW}Selecciona tu equipo haciendo clic en una lana de color.")
        
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
        
        // Destruir cadenas visuales del jugador si la partida está en curso
        if (game.state == GameState.IN_GAME) {
            chainVisualizerService?.destroyChainsForPlayer(player)
        }
        
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
        
        // Detener temporizador visual (BossBar)
        game.gameTimer?.stop()
        game.gameTimer = null
        
        // Limpiar todas las cadenas visuales de la partida
        chainVisualizerService?.clearAllChains()
        
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
    
    // ===== Funciones de UI del Lobby =====
    
    /**
     * Entrega los ítems de selección de equipo al jugador.
     * Cada ítem es una lana del color del equipo con información dinámica.
     */
    private fun giveTeamSelectionItems(player: Player, game: CadenaGame) {
        // Crear ítem para cada equipo
        game.teams.forEachIndexed { index, team ->
            val item = ItemStack(team.material)
            val meta = item.itemMeta
            
            // Nombre del equipo
            meta?.setDisplayName(team.displayName)
            
            // Lore con información de jugadores
            val playerCount = team.players.size
            val maxPlayers = 4
            val lore = mutableListOf<String>()
            
            lore.add("${ChatColor.GRAY}Jugadores: ${ChatColor.WHITE}$playerCount / $maxPlayers")
            lore.add("")
            
            if (team.players.isEmpty()) {
                lore.add("${ChatColor.YELLOW}¡Sé el primero en unirte!")
            } else {
                lore.add("${ChatColor.GRAY}Miembros:")
                team.getOnlinePlayers().forEach { p ->
                    lore.add("${ChatColor.WHITE}  • ${p.name}")
                }
            }
            
            lore.add("")
            if (team.isFull()) {
                lore.add("${ChatColor.RED}¡Equipo completo!")
            } else {
                lore.add("${ChatColor.GREEN}Click para unirte")
            }
            
            meta?.lore = lore
            item.itemMeta = meta
            
            // Colocar en el inventario (slots 2, 3, 4, 5)
            player.inventory.setItem(index + 2, item)
        }
    }
    
    /**
     * Actualiza la UI del inventario para todos los jugadores en el lobby.
     * Debe ser llamado cada vez que un jugador cambia de equipo.
     */
    fun updateAllLobbyInventories(game: CadenaGame) {
        if (game.state != GameState.LOBBY) {
            return
        }
        
        // Obtener todos los jugadores de esta partida (incluyendo los que no han elegido equipo)
        val playersInGame = playerToGame.filter { it.value == game.id }
            .mapNotNull { org.bukkit.Bukkit.getPlayer(it.key) }
        
        // Actualizar inventario de cada jugador
        playersInGame.forEach { player ->
            player.inventory.clear()
            giveTeamSelectionItems(player, game)
        }
    }
}
