package los5fantasticos.torneo.core

import los5fantasticos.torneo.api.MinigameModule
import los5fantasticos.torneo.api.PlayerScore
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.UUID

/**
 * Gestor central del torneo.
 * Responsable de:
 * - Gestionar el registro de minijuegos
 * - Mantener el sistema de puntaje global
 * - Persistir y cargar datos de jugadores
 * - Generar rankings
 */
class TorneoManager(private val plugin: Plugin) {
    
    private val minigames = mutableMapOf<String, MinigameModule>()
    private val playerScores = mutableMapOf<UUID, PlayerScore>()
    private val dataFile = File(plugin.dataFolder, "scores.yml")
    
    init {
        loadScores()
    }
    
    /**
     * Registra un nuevo minijuego en el sistema.
     * 
     * @param module Módulo del minijuego a registrar
     */
    fun registerMinigame(module: MinigameModule) {
        if (minigames.containsKey(module.name)) {
            plugin.logger.warning("El minijuego '${module.name}' ya está registrado. Sobrescribiendo...")
        }
        
        minigames[module.name] = module
        plugin.logger.info("Minijuego registrado: ${module.name} v${module.version}")
    }
    
    /**
     * Obtiene un minijuego registrado por su nombre.
     * 
     * @param name Nombre del minijuego
     * @return El módulo del minijuego, o null si no existe
     */
    fun getMinigame(name: String): MinigameModule? {
        return minigames[name]
    }
    
    /**
     * Obtiene todos los minijuegos registrados.
     * 
     * @return Lista de todos los módulos de minijuegos
     */
    fun getAllMinigames(): List<MinigameModule> {
        return minigames.values.toList()
    }
    
    /**
     * Añade puntos a un jugador desde un minijuego específico.
     * 
     * @param player Jugador que recibe los puntos
     * @param minigameName Nombre del minijuego que otorga los puntos
     * @param points Cantidad de puntos
     * @param reason Razón por la que recibe los puntos
     */
    fun addPoints(player: Player, minigameName: String, points: Int, reason: String) {
        val score = getOrCreatePlayerScore(player)
        score.addPoints(minigameName, points)
        
        // Notificar al jugador
        player.sendMessage("${ChatColor.GOLD}[Torneo] ${ChatColor.GREEN}+$points puntos ${ChatColor.GRAY}($reason)")
        player.sendMessage("${ChatColor.GRAY}Total: ${ChatColor.YELLOW}${score.totalPoints} puntos")
        
        // Guardar cambios
        saveScores()
        
        plugin.logger.info("${player.name} ganó $points puntos en $minigameName ($reason). Total: ${score.totalPoints}")
    }
    
    /**
     * Registra que un jugador ha jugado una partida.
     * 
     * @param player Jugador
     * @param minigameName Nombre del minijuego
     */
    fun recordGamePlayed(player: Player, minigameName: String) {
        val score = getOrCreatePlayerScore(player)
        score.incrementGamesPlayed()
        saveScores()
    }
    
    /**
     * Registra que un jugador ha ganado una partida.
     * 
     * @param player Jugador
     * @param minigameName Nombre del minijuego
     */
    fun recordGameWon(player: Player, minigameName: String) {
        val score = getOrCreatePlayerScore(player)
        score.incrementGamesWon()
        saveScores()
    }
    
    /**
     * Obtiene el puntaje de un jugador.
     * 
     * @param player Jugador
     * @return PlayerScore del jugador
     */
    fun getPlayerScore(player: Player): PlayerScore {
        return getOrCreatePlayerScore(player)
    }
    
    /**
     * Obtiene o crea el puntaje de un jugador.
     */
    private fun getOrCreatePlayerScore(player: Player): PlayerScore {
        return playerScores.getOrPut(player.uniqueId) {
            PlayerScore(player.uniqueId, player.name)
        }
    }
    
    /**
     * Obtiene el ranking global de jugadores ordenado por puntos.
     * 
     * @param limit Número máximo de jugadores a retornar (0 = todos)
     * @return Lista de PlayerScore ordenada por puntos descendente
     */
    fun getGlobalRanking(limit: Int = 0): List<PlayerScore> {
        val sorted = playerScores.values.sortedByDescending { it.totalPoints }
        return if (limit > 0) sorted.take(limit) else sorted
    }
    
    /**
     * Obtiene el ranking de un minijuego específico.
     * 
     * @param minigameName Nombre del minijuego
     * @param limit Número máximo de jugadores a retornar (0 = todos)
     * @return Lista de PlayerScore ordenada por puntos en ese minijuego
     */
    fun getMinigameRanking(minigameName: String, limit: Int = 0): List<PlayerScore> {
        val sorted = playerScores.values
            .filter { it.getPointsForMinigame(minigameName) > 0 }
            .sortedByDescending { it.getPointsForMinigame(minigameName) }
        return if (limit > 0) sorted.take(limit) else sorted
    }
    
    /**
     * Muestra el ranking global a un jugador.
     * 
     * @param player Jugador que verá el ranking
     * @param limit Número de posiciones a mostrar
     */
    fun showGlobalRanking(player: Player, limit: Int = 10) {
        val ranking = getGlobalRanking(limit)
        
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}═══════════════════════════════")
        player.sendMessage("${ChatColor.YELLOW}${ChatColor.BOLD}    RANKING GLOBAL DEL TORNEO")
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}═══════════════════════════════")
        
        if (ranking.isEmpty()) {
            player.sendMessage("${ChatColor.GRAY}No hay datos de jugadores aún.")
        } else {
            ranking.forEachIndexed { index, score ->
                val position = index + 1
                val medal = when (position) {
                    1 -> "${ChatColor.GOLD}🥇"
                    2 -> "${ChatColor.GRAY}🥈"
                    3 -> "${ChatColor.GOLD}🥉"
                    else -> "${ChatColor.WHITE}#$position"
                }
                
                val isCurrentPlayer = score.playerUUID == player.uniqueId
                val nameColor = if (isCurrentPlayer) ChatColor.GREEN else ChatColor.WHITE
                val arrow = if (isCurrentPlayer) " ${ChatColor.YELLOW}◄" else ""
                
                player.sendMessage("$medal ${nameColor}${score.playerName}${ChatColor.GRAY}: ${ChatColor.YELLOW}${score.totalPoints} pts$arrow")
            }
        }
        
        player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}═══════════════════════════════")
        
        // Mostrar posición del jugador si no está en el top
        val playerScore = playerScores[player.uniqueId]
        if (playerScore != null) {
            val playerPosition = getGlobalRanking().indexOf(playerScore) + 1
            if (playerPosition > limit) {
                player.sendMessage("${ChatColor.GRAY}Tu posición: ${ChatColor.WHITE}#$playerPosition ${ChatColor.GRAY}con ${ChatColor.YELLOW}${playerScore.totalPoints} pts")
            }
        }
    }
    
    /**
     * Guarda los puntajes en el archivo de datos.
     */
    private fun saveScores() {
        try {
            val config = YamlConfiguration()
            
            playerScores.forEach { (uuid, score) ->
                val path = "players.$uuid"
                config.set("$path.name", score.playerName)
                config.set("$path.totalPoints", score.totalPoints)
                config.set("$path.gamesPlayed", score.gamesPlayed)
                config.set("$path.gamesWon", score.gamesWon)
                
                score.pointsPerMinigame.forEach { (minigame, points) ->
                    config.set("$path.minigames.$minigame", points)
                }
            }
            
            config.save(dataFile)
        } catch (e: Exception) {
            plugin.logger.severe("Error al guardar los puntajes: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Carga los puntajes desde el archivo de datos.
     */
    private fun loadScores() {
        if (!dataFile.exists()) {
            plugin.logger.info("No se encontró archivo de puntajes. Se creará uno nuevo.")
            return
        }
        
        try {
            val config = YamlConfiguration.loadConfiguration(dataFile)
            val playersSection = config.getConfigurationSection("players") ?: return
            
            playersSection.getKeys(false).forEach { uuidString ->
                val uuid = UUID.fromString(uuidString)
                val path = "players.$uuidString"
                
                val name = config.getString("$path.name") ?: "Unknown"
                val totalPoints = config.getInt("$path.totalPoints", 0)
                val gamesPlayed = config.getInt("$path.gamesPlayed", 0)
                val gamesWon = config.getInt("$path.gamesWon", 0)
                
                val pointsPerMinigame = mutableMapOf<String, Int>()
                val minigamesSection = config.getConfigurationSection("$path.minigames")
                minigamesSection?.getKeys(false)?.forEach { minigame ->
                    pointsPerMinigame[minigame] = config.getInt("$path.minigames.$minigame", 0)
                }
                
                val score = PlayerScore(uuid, name, totalPoints, pointsPerMinigame, gamesPlayed, gamesWon)
                playerScores[uuid] = score
            }
            
            plugin.logger.info("Cargados ${playerScores.size} registros de jugadores.")
        } catch (e: Exception) {
            plugin.logger.severe("Error al cargar los puntajes: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Limpia todos los datos del torneo.
     * ¡USAR CON PRECAUCIÓN!
     */
    fun resetAllScores() {
        playerScores.clear()
        saveScores()
        plugin.logger.warning("Todos los puntajes han sido reseteados.")
    }
}
