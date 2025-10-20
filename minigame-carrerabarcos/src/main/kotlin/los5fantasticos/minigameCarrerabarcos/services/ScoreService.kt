package los5fantasticos.minigameCarrerabarcos.services

import los5fantasticos.minigameCarrerabarcos.game.Carrera
import los5fantasticos.torneo.TorneoPlugin
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.UUID

/**
 * Servicio de puntuación dinámica para Carrera de Barcos.
 * 
 * RESPONSABILIDADES:
 * - Gestionar puntos por checkpoint (primeros en llegar)
 * - Gestionar puntos por posición final
 * - Calcular bonus por tiempo
 * - Registrar progreso de jugadores en checkpoints
 * 
 * ARQUITECTURA:
 * Este servicio mantiene el estado de quién ha cruzado cada checkpoint
 * y calcula puntuaciones dinámicas basadas en la configuración.
 * 
 * @author Los 5 Fantásticos
 * @since 2.0
 */
class ScoreService(
    private val plugin: Plugin,
    private val torneoPlugin: TorneoPlugin
) {
    
    companion object {
        private const val GAME_NAME = "Carrera de Barcos"
    }
    
    /**
     * Configuración de puntuación cargada desde carrerabarcos.yml
     */
    private var config: YamlConfiguration
    
    /**
     * Registro de jugadores que han cruzado cada checkpoint.
     * Map<CheckpointIndex, List<UUID en orden de llegada>>
     */
    private val checkpointCompletions = mutableMapOf<Int, MutableList<UUID>>()
    
    /**
     * Registro de tiempos de inicio por jugador.
     * Map<UUID, Timestamp de inicio>
     */
    private val startTimes = mutableMapOf<UUID, Long>()
    
    init {
        // Cargar configuración
        val configFile = File(plugin.dataFolder, "carrerabarcos.yml")
        
        if (!configFile.exists()) {
            plugin.saveResource("carrerabarcos.yml", false)
        }
        
        config = YamlConfiguration.loadConfiguration(configFile)
        plugin.logger.info("[Carrera de Barcos] Configuración de puntuación cargada")
    }
    
    /**
     * Reinicia el tracking para una nueva carrera.
     */
    fun resetForNewRace(carrera: Carrera) {
        checkpointCompletions.clear()
        startTimes.clear()
        
        // Inicializar listas para cada checkpoint
        for (i in 0 until carrera.arena.checkpoints.size) {
            checkpointCompletions[i] = mutableListOf()
        }
        
        // Registrar tiempo de inicio para todos los jugadores
        val now = System.currentTimeMillis()
        carrera.getJugadores().forEach { player ->
            startTimes[player.uniqueId] = now
        }
        
        plugin.logger.info("[Carrera de Barcos] ScoreService reiniciado para nueva carrera")
    }
    
    /**
     * Registra que un jugador ha cruzado un checkpoint y otorga puntos.
     * 
     * @param player Jugador que cruzó el checkpoint
     * @param checkpointIndex Índice del checkpoint (0-based)
     * @return Puntos otorgados
     */
    fun onCheckpointReached(player: Player, checkpointIndex: Int): Int {
        val uuid = player.uniqueId
        val completions = checkpointCompletions[checkpointIndex] ?: return 0
        
        // Si ya cruzó este checkpoint, no dar puntos
        if (completions.contains(uuid)) {
            return 0
        }
        
        // Registrar que cruzó
        completions.add(uuid)
        val posicion = completions.size
        
        // Calcular puntos según posición
        val puntos = when (posicion) {
            1 -> config.getInt("puntuacion.por-checkpoint-primer-lugar", 10)
            2 -> config.getInt("puntuacion.por-checkpoint-segundo-lugar", 5)
            3 -> config.getInt("puntuacion.por-checkpoint-tercer-lugar", 3)
            else -> 0
        }
        
        // Otorgar puntos si corresponde
        if (puntos > 0) {
            torneoPlugin.torneoManager.addScore(
                uuid,
                GAME_NAME,
                puntos,
                "Checkpoint ${checkpointIndex + 1} (#$posicion)"
            )
            
            plugin.logger.info("[Carrera de Barcos] ${player.name} cruzó checkpoint ${checkpointIndex + 1} en posición #$posicion (+$puntos puntos)")
        }
        
        return puntos
    }
    
    /**
     * Calcula y otorga puntos cuando un jugador cruza la meta.
     * 
     * @param player Jugador que cruzó la meta
     * @param posicionFinal Posición final (1 = primero)
     * @return Total de puntos otorgados
     */
    fun onPlayerFinished(player: Player, posicionFinal: Int): Int {
        val uuid = player.uniqueId
        var totalPuntos = 0
        
        // 1. Puntos por posición en meta
        val puntosPosicion = when (posicionFinal) {
            1 -> config.getInt("puntuacion.por-meta-primer-lugar", 100)
            2 -> config.getInt("puntuacion.por-meta-segundo-lugar", 75)
            3 -> config.getInt("puntuacion.por-meta-tercer-lugar", 50)
            4 -> config.getInt("puntuacion.por-meta-cuarto-lugar", 30)
            else -> config.getInt("puntuacion.por-participacion", 10)
        }
        
        torneoPlugin.torneoManager.addScore(
            uuid,
            GAME_NAME,
            puntosPosicion,
            "Posición #$posicionFinal"
        )
        totalPuntos += puntosPosicion
        
        // 2. Bonus por tiempo
        val startTime = startTimes[uuid]
        if (startTime != null) {
            val tiempoEnSegundos = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            val bonusBase = config.getInt("puntuacion.bonus-tiempo-base", 200)
            val multiplicador = config.getDouble("puntuacion.bonus-tiempo-multiplicador-perdida", 0.5)
            
            val bonusTiempo = maxOf(0, bonusBase - (tiempoEnSegundos * multiplicador).toInt())
            
            if (bonusTiempo > 0) {
                torneoPlugin.torneoManager.addScore(
                    uuid,
                    GAME_NAME,
                    bonusTiempo,
                    "Bonus por velocidad (${tiempoEnSegundos}s)"
                )
                totalPuntos += bonusTiempo
                
                plugin.logger.info("[Carrera de Barcos] ${player.name} recibió $bonusTiempo puntos de bonus por tiempo")
            }
        }
        
        // 3. Registrar victoria si es primer lugar
        if (posicionFinal == 1) {
            torneoPlugin.torneoManager.recordGameWon(player, GAME_NAME)
        }
        
        // 4. Registrar partida jugada
        torneoPlugin.torneoManager.recordGamePlayed(player, GAME_NAME)
        
        plugin.logger.info("[Carrera de Barcos] ${player.name} finalizó en posición #$posicionFinal (Total: $totalPuntos puntos)")
        
        return totalPuntos
    }
    
    /**
     * Obtiene la posición actual de un jugador en un checkpoint específico.
     * 
     * @param player Jugador
     * @param checkpointIndex Índice del checkpoint
     * @return Posición (1-based) o null si no ha cruzado
     */
    fun getCheckpointPosition(player: Player, checkpointIndex: Int): Int? {
        val completions = checkpointCompletions[checkpointIndex] ?: return null
        val index = completions.indexOf(player.uniqueId)
        return if (index >= 0) index + 1 else null
    }
    
    /**
     * Obtiene cuántos jugadores han cruzado un checkpoint.
     */
    fun getCheckpointCompletionCount(checkpointIndex: Int): Int {
        return checkpointCompletions[checkpointIndex]?.size ?: 0
    }
    
    /**
     * Obtiene el tiempo transcurrido para un jugador (en segundos).
     */
    fun getElapsedTime(player: Player): Int {
        val startTime = startTimes[player.uniqueId] ?: return 0
        return ((System.currentTimeMillis() - startTime) / 1000).toInt()
    }
    
    /**
     * Recarga la configuración desde el archivo.
     */
    fun reloadConfig() {
        val configFile = File(plugin.dataFolder, "carrerabarcos.yml")
        config = YamlConfiguration.loadConfiguration(configFile)
        plugin.logger.info("[Carrera de Barcos] Configuración recargada")
    }
    
    /**
     * Obtiene un valor de configuración.
     */
    fun getConfigValue(path: String, default: Any): Any {
        return when (default) {
            is Int -> config.getInt(path, default)
            is Double -> config.getDouble(path, default)
            is Boolean -> config.getBoolean(path, default)
            is String -> config.getString(path, default) ?: default
            else -> default
        }
    }
}
