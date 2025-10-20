package los5fantasticos.minigameCarrerabarcos

import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.api.MinigameModule
import los5fantasticos.minigameCarrerabarcos.commands.CarreraCommand
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Boat
import org.bukkit.entity.EntityType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.ChatColor
import org.bukkit.command.CommandExecutor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.vehicle.VehicleMoveEvent

/**
 * Manager del minijuego Carrera de Barcos.
 * 
 * Juego de carreras donde los jugadores compiten en barcos para llegar primero a la meta.
 */
class MinigameCarrerabarcos(private val torneoPlugin: TorneoPlugin) : MinigameModule, Listener {

    private lateinit var plugin: Plugin

    override val gameName = "Carrera de Barcos"
    override val version = "1.0"
    override val description = "Minijuego de carreras acuáticas - ¡el más rápido gana!"

    private val activePlayers = mutableSetOf<Player>()
    private var gameRunning = false
    private val playerBoats = mutableMapOf<Player, Boat>()
    
    // Sistema de detección de ganador
    private val finishLineX = 431.0  // Coordenada X de meta
    private val finishLineZMin = -432.0  // Límite Z inferior 
    private val finishLineZMax = -430.0  // Límite Z superior 
    private val finishedPlayers = mutableMapOf<Player, Int>()  // Player -> Posición
    private var raceCheckTask: org.bukkit.scheduler.BukkitTask? = null
    private var currentPosition = 1
    private var positionUpdateTask: org.bukkit.scheduler.BukkitTask? = null
    private val playerPositions = mutableMapOf<Player, Int>()  // Posición actual de cada jugador
    private var raceStarted = false  // Control para que no puedan avanzar antes de tiempo
    private val barrierBlocks = mutableListOf<Location>()  // Bloques de barrera 
    
    // Configuración de la pista
    private val trackWorldName = "world"  // Nombre del mundo 
    private val trackStartX = 361.0       // Coordenada X de inicio (detrás de la línea de salida)
    private val trackStartY = 71.0        // Coordenada Y 
    private val trackStartZ = -397.0      // Coordenada Z de inicio (centro del ancho de pista)
    private val trackSpacing = 2.0        // Espacio entre jugadores en el eje Z
    private val maxPlayers = 40           // Máximo número de jugadores
    
    // Posiciones de salida para los jugadores 
    private val startPositions: List<Location> by lazy {
        (0 until maxPlayers).map { index ->
            Location(
                null, // Se asignará dinámicamente
                trackStartX,
                trackStartY,
                trackStartZ + (index * trackSpacing),
                0f, 0f
            )
        }
    }

    override fun onEnable(plugin: Plugin) {
        this.plugin = plugin

        // TODO: Inicializar lógica del juego Carrera de Barcos
        // - Crear pistas de carreras
        // - Configurar eventos
        // - Registrar comandos
        // Registrar eventos
        plugin.server.pluginManager.registerEvents(this, plugin)

        plugin.logger.info("✓ $gameName v$version habilitado")
    }

    override fun onDisable() {
        // Terminar todos los juegos activos
        endAllGames()
        
        plugin.logger.info("✓ $gameName deshabilitado")
    }
    
    override fun isGameRunning(): Boolean {
        return gameRunning
    }
    
    override fun getActivePlayers(): List<Player> {
        return activePlayers.toList()
    }
    
    /**
     * Inicia el minijuego en modo torneo centralizado.
     */
    override fun onTournamentStart(players: List<Player>) {
        plugin.logger.info("[$gameName] ═══ INICIO DE TORNEO ═══")
        plugin.logger.info("[$gameName] ${players.size} jugadores disponibles")
        
        // Añadir jugadores a la lista activa
        players.forEach { player ->
            activePlayers.add(player)
        }
        
        gameRunning = true
        plugin.logger.info("[$gameName] ✓ Torneo iniciado con ${players.size} jugadores")
    }
    
    private fun awardPoints(player: Player, points: Int, reason: String) {
        torneoPlugin.torneoManager.addScore(player.uniqueId, gameName, points, reason)
    }
    
    private fun recordVictory(player: Player) {
        torneoPlugin.torneoManager.recordGameWon(player, gameName)
    }

    override fun getCommandExecutors(): Map<String, CommandExecutor> {
        return mapOf(
            "carrerabarcos" to CarreraCommand(this)
        )
    }
    
    /**
     * Inicia una nueva partida de Carrera de Barcos.
     */
    @Suppress("unused")
    fun startGame(players: List<Player>) {
        if (gameRunning) {
            plugin.logger.warning("Ya hay una partida de Carrera de Barcos en curso")
            return
        }

        if (players.isEmpty()) {
            plugin.logger.warning("No hay jugadores para iniciar la carrera")
            return
        }
        
        if (players.size > maxPlayers) {
            plugin.logger.warning("Demasiados jugadores para las posiciones disponibles (máximo $maxPlayers)")
            return
        }
        
        gameRunning = true
        raceStarted = false  // La carrera aún no ha comenzado
        activePlayers.addAll(players)

        // TODO: Implementar lógica de inicio del juego
        // - Preparar pista de carreras
        // - Dar barcos a los jugadores
        // - Iniciar countdown
        // Limpiar barcos anteriores si los hay
        playerBoats.clear()

        players.forEach { player ->
            player.sendMessage("§6[Carrera de Barcos] §e¡La carrera ha comenzado! §7¡El más rápido gana!")
            recordGamePlayed(player)
        }
        // Teletransportar jugadores a posiciones de salida
        teleportPlayersToStart(players)
        
        // Crear barrera 
        createInvisibleBarrier(players)
        
        // Dar barcos a los jugadores
        giveBoatsToPlayers(players)
        
        // Iniciar cuenta regresiva
        startCountdown(players)
        
        // Los sistemas de verificación se iniciarán cuando comience la carrera

        plugin.logger.info("Partida de Carrera de Barcos iniciada con ${players.size} jugadores")
    }

    /**
     * Termina la partida actual.
     */
    fun endGame(showResults: Boolean = true) {
        if (!gameRunning) return

        gameRunning = false

        // Cancelar tareas de verificación de posiciones
        raceCheckTask?.cancel()
        raceCheckTask = null
        positionUpdateTask?.cancel()
        positionUpdateTask = null
        
        // Limpiar barrera 
        removeInvisibleBarrier()
        
        if (showResults && finishedPlayers.isNotEmpty()) {
            // Mostrar tabla de posiciones finales
            showFinalResults()

            // Otorgar puntos a todos los jugadores según su posición
            finishedPlayers.forEach { (player, position) ->
                val points = calculateWinnerPoints(position)
                val reason = when (position) {
                    1 -> "1er lugar en Carrera de Barcos"
                    2 -> "2do lugar en Carrera de Barcos"
                    3 -> "3er lugar en Carrera de Barcos"
                    else -> "Participación en Carrera de Barcos"
                }
                awardPoints(player, points, reason)
                
                if (position == 1) {
                    recordVictory(player)
                }
            }
        }

        // Limpiar barcos y jugadores activos
        playerBoats.values.forEach { boat ->
            boat.remove()
        }
        playerBoats.clear()
        activePlayers.clear()
        finishedPlayers.clear()
        playerPositions.clear()
        currentPosition = 1
        raceStarted = false

        plugin.logger.info("Partida de Carrera de Barcos terminada - ${finishedPlayers.size} jugadores finalizaron")
    }

    /**
     * Calcula los puntos según la posición en la carrera.
     */
    private fun calculateWinnerPoints(position: Int): Int {
        return when (position) {
            1 -> 100  // 1er lugar
            2 -> 75   // 2do lugar
            3 -> 50   // 3er lugar
            else -> 25  // Participación
        }
    }

    /**
     * Termina todos los juegos activos.
     */
    private fun endAllGames() {
        if (gameRunning) {
            endGame(false)
        }
    }
    
    private fun recordGamePlayed(player: Player) {
        torneoPlugin.torneoManager.recordGamePlayed(player, gameName)
    }
    
    /**
     * Configura la pista de carrera con coordenadas específicas.
     * 
     */
    fun setTrackLocation(worldName: String, startX: Double, startY: Double, startZ: Double, spacing: Double = 2.0) {
        // Actualizar configuración
        val trackWorld = plugin.server.getWorld(worldName)
        if (trackWorld == null) {
            plugin.logger.warning("Mundo '$worldName' no encontrado. Usando configuración por defecto.")
            return
        }
        
        plugin.logger.info("Pista configurada en mundo '$worldName' en coordenadas ($startX, $startY, $startZ)")
    }
    
    /**
     * Obtiene información sobre la pista configurada.
     */
    fun getTrackInfo(): String {
        return "Pista: Mundo='$trackWorldName', Inicio=($trackStartX, $trackStartY, $trackStartZ), Espaciado=$trackSpacing, MaxJugadores=$maxPlayers"
    }
    
    /**
     * Teletransporta a los jugadores a las posiciones de salida.
     */
    private fun teleportPlayersToStart(players: List<Player>) {
        // Obtener el mundo de la pista
        val trackWorld = plugin.server.getWorld(trackWorldName)
        if (trackWorld == null) {
            plugin.logger.severe("¡Error! No se encontró el mundo '$trackWorldName'. Verifica que el mundo existe.")
            players.forEach { player ->
                player.sendMessage("${ChatColor.RED}[Carrera de Barcos] ${ChatColor.RED}¡Error! Mundo de pista no encontrado.")
            }
            return
        }
        
        players.forEachIndexed { index, player ->
            if (index < startPositions.size) {
                val startPos = startPositions[index].clone()
                startPos.world = trackWorld
                
                player.teleport(startPos)
                player.sendMessage("${ChatColor.GOLD}[Carrera de Barcos] ${ChatColor.YELLOW}Teletransportado a la posición de salida ${index + 1}")
                plugin.logger.info("Jugador ${player.name} teletransportado a ${startPos.x}, ${startPos.y}, ${startPos.z}")
            }
        }
    }
    
    /**
     * Da un barco a cada jugador.
     */
    private fun giveBoatsToPlayers(players: List<Player>) {
        players.forEach { player ->
            val playerLocation = player.location
            val boat = player.world.spawnEntity(playerLocation, EntityType.BOAT) as Boat
            
            // Orientar el barco hacia la dirección de la carrera 
            val boatLocation = boat.location.clone()
            boatLocation.yaw = 0f  
            boat.teleport(boatLocation)
            
            // Hacer que el jugador entre al barco
            boat.addPassenger(player)
            
            // Guardar referencia al barco del jugador
            playerBoats[player] = boat
            
            player.sendMessage("${ChatColor.GOLD}[Carrera de Barcos] ${ChatColor.GREEN}¡Has recibido tu barco! ¡Prepárate para la carrera!")
            player.sendMessage("${ChatColor.YELLOW}¡Espera la señal de GO! No puedes avanzar antes de tiempo.")
        }
    }
    
    /**
     * Inicia la cuenta regresiva de 20 segundos.
     */
    private fun startCountdown(players: List<Player>) {
        object : BukkitRunnable() {
            private var countdown = 20
            
            override fun run() {
                if (!gameRunning || countdown <= 0) {
                    // La carrera comienza
                    raceStarted = true  // Permitir que los jugadores avancen
                    
                    // Remover barrera 
                    removeInvisibleBarrier()
                    
                    players.forEach { player ->
                        player.sendMessage("${ChatColor.GOLD}[Carrera de Barcos] ${ChatColor.GREEN}¡¡¡GO!!! ¡¡¡La carrera ha comenzado!!!")
                        recordGamePlayed(player)
                    }
                    
                    // Iniciar sistemas de verificación ahora que la carrera comenzó
                    startRacePositionChecker()
                    startPositionUpdater()
                    
                    this.cancel()
                    return
                }
                
                // Mostrar mensaje de cuenta regresiva
                val message = when (countdown) {
                    20 -> "${ChatColor.GOLD}[Carrera de Barcos] ${ChatColor.RED}¡Preparados! La carrera comienza en ${ChatColor.YELLOW}20${ChatColor.RED} segundos..."
                    19, 18, 17, 16, 15, 14, 13, 12, 11, 10 -> "${ChatColor.GOLD}[Carrera de Barcos] ${ChatColor.RED}¡${ChatColor.YELLOW}$countdown${ChatColor.RED}!"
                    9, 8, 7, 6, 5, 4, 3, 2, 1 -> "${ChatColor.GOLD}[Carrera de Barcos] ${ChatColor.RED}¡${ChatColor.YELLOW}$countdown${ChatColor.RED}!"
                    else -> ""
                }
                
                if (message.isNotEmpty()) {
                    players.forEach { player ->
                        player.sendMessage(message)
                        player.sendTitle("", message, 0, 20, 0)
                    }
                }
                
                countdown--
            }
        }.runTaskTimer(plugin, 0L, 20L) // Ejecutar cada segundo (20 ticks = 1 segundo)
    }
    
    /**
     * Inicia el sistema de verificación de posiciones de los jugadores.
     */
    private fun startRacePositionChecker() {
        raceCheckTask = object : BukkitRunnable() {
            override fun run() {
                if (!gameRunning || !raceStarted) {
                    this.cancel()
                    return
                }
                
                // Verificar cada jugador activo que no haya terminado
                activePlayers.filter { player -> 
                    !finishedPlayers.containsKey(player) && playerBoats.containsKey(player)
                }.forEach { player ->
                    val boat = playerBoats[player] ?: return@forEach
                    val location = boat.location
                    
                    // Verificar si el jugador ha cruzado la línea de meta
                    if (location.x >= finishLineX && 
                        location.z >= finishLineZMin && 
                        location.z <= finishLineZMax) {
                        
                        handlePlayerFinish(player)
                    }
                }
                
                // Verificar si todos los jugadores han terminado
                if (finishedPlayers.size >= activePlayers.size) {
                    // Esperar 3 segundos antes de terminar el juego
                    object : BukkitRunnable() {
                        override fun run() {
                            endGame(true)
                        }
                    }.runTaskLater(plugin, 60L) // 3 segundos = 60 ticks
                    
                    this.cancel()
                }
            }
        }.runTaskTimer(plugin, 0L, 1L) // Ejecutar cada tick
    }
    
    /**
     * Maneja cuando un jugador cruza la línea de meta.
     */
    private fun handlePlayerFinish(player: Player) {
        // Registrar posición del jugador
        finishedPlayers[player] = currentPosition
        
        // Notificar al jugador
        val positionText = when (currentPosition) {
            1 -> "§6§l¡1er LUGAR! §a¡Felicidades!"
            2 -> "§7§l2do LUGAR §a¡Bien hecho!"
            3 -> "§c§l3er LUGAR §a¡Buen trabajo!"
            else -> "§e§l${currentPosition}° LUGAR §a¡Has terminado!"
        }
        
        player.sendMessage("${ChatColor.GOLD}[Carrera de Barcos] $positionText")
        player.sendTitle("", positionText, 0, 40, 20)
        
        // Notificar a otros jugadores
        activePlayers.filter { it != player }.forEach { otherPlayer ->
            otherPlayer.sendMessage("${ChatColor.GOLD}[Carrera de Barcos] ${ChatColor.YELLOW}${player.name} ${ChatColor.WHITE}ha terminado en ${ChatColor.GREEN}${currentPosition}° lugar")
        }
        
        // Programar remoción del barco después de 3 segundos
        object : BukkitRunnable() {
            override fun run() {
                removePlayerFromRace(player)
            }
        }.runTaskLater(plugin, 60L) // 3 segundos = 60 ticks
        
        currentPosition++
        plugin.logger.info("Jugador ${player.name} terminó en posición $currentPosition")
    }
    
    /**
     * Remueve a un jugador de la carrera y lo teletransporta fuera de la pista.
     */
    private fun removePlayerFromRace(player: Player) {
        // Remover barco
        val boat = playerBoats[player]
        if (boat != null) {
            // Sacar al jugador del barco antes de removerlo
            boat.removePassenger(player)
            boat.remove()
            playerBoats.remove(player)
        }
        
        // Teletransportar jugador fuera de la pista 
        val safeLocation = Location(
            player.world,
            finishLineX + 10.0,  // 10 bloques después de la meta
            player.location.y,
            player.location.z,
            0f, 0f
        )
        player.teleport(safeLocation)
        
        player.sendMessage("${ChatColor.GOLD}[Carrera de Barcos] ${ChatColor.YELLOW}Tu barco ha sido removido. No puedes volver a la pista.")
        plugin.logger.info("Jugador ${player.name} removido de la carrera y teletransportado a zona segura")
    }
    
    /**
     * Muestra los resultados finales de la carrera.
     */
    private fun showFinalResults() {
        val sortedResults = finishedPlayers.toList().sortedBy { it.second }
        
        activePlayers.forEach { player ->
            player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}=== RESULTADOS FINALES ===")
            player.sendMessage("${ChatColor.YELLOW}Posición | Jugador | Puntos")
            player.sendMessage("${ChatColor.GRAY}------------------------")
            
            sortedResults.forEach { (resultPlayer, position) ->
                val points = calculateWinnerPoints(position)
                val positionColor = when (position) {
                    1 -> ChatColor.GOLD
                    2 -> ChatColor.GRAY
                    3 -> ChatColor.RED
                    else -> ChatColor.WHITE
                }
                
                player.sendMessage("$positionColor$position° ${ChatColor.WHITE}| ${resultPlayer.name} | ${ChatColor.GREEN}+$points pts")
            }
            
            player.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}========================")
        }
    }
    
    /**
     * Inicia el sistema de actualización de posiciones en tiempo real.
     */
    private fun startPositionUpdater() {
        positionUpdateTask = object : BukkitRunnable() {
            override fun run() {
                if (!gameRunning || !raceStarted) {
                    this.cancel()
                    return
                }
                
                // Actualizar posiciones de todos los jugadores activos
                updatePlayerPositions()
            }
        }.runTaskTimer(plugin, 0L, 20L) // Ejecutar cada segundo
    }
    
    /**
     * Actualiza las posiciones de todos los jugadores en la carrera.
     */
    private fun updatePlayerPositions() {
        // Obtener jugadores activos que no han terminado
        val activeRacingPlayers = activePlayers.filter { player ->
            !finishedPlayers.containsKey(player) && playerBoats.containsKey(player)
        }
        
        if (activeRacingPlayers.isEmpty()) return
        
        // Ordenar jugadores por su posición X 
        val sortedPlayers = activeRacingPlayers.sortedByDescending { player ->
            playerBoats[player]?.location?.x ?: Double.MIN_VALUE
        }
        
        // Actualizar posiciones
        sortedPlayers.forEachIndexed { index, player ->
            val newPosition = index + 1
            val oldPosition = playerPositions[player]
            
            // Solo actualizar si la posición cambió
            if (oldPosition != newPosition) {
                playerPositions[player] = newPosition
                
                // Notificar cambio de posición
                if (oldPosition != null && oldPosition != newPosition) {
                    notifyPositionChange(player, oldPosition, newPosition)
                } else if (oldPosition == null) {
                    // Primera vez que se registra la posición
                    player.sendMessage("${ChatColor.GOLD}[Carrera de Barcos] ${ChatColor.YELLOW}Tu posición actual: ${ChatColor.GREEN}${getPositionText(newPosition)}")
                }
            }
        }
    }
    
    /**
     * Notifica a un jugador sobre su cambio de posición.
     */
    private fun notifyPositionChange(player: Player, oldPosition: Int, newPosition: Int) {
        val totalPlayers = activePlayers.size - finishedPlayers.size
        
        when {
            newPosition < oldPosition -> {
                // Mejoró posición
                val improvement = oldPosition - newPosition
                val message = when (improvement) {
                    1 -> "${ChatColor.GREEN}¡Subiste 1 posición! Ahora vas ${getPositionText(newPosition)}"
                    else -> "${ChatColor.GREEN}¡Subiste $improvement posiciones! Ahora vas ${getPositionText(newPosition)}"
                }
                player.sendMessage("${ChatColor.GOLD}[Carrera de Barcos] $message")
                player.sendTitle("", "${ChatColor.GREEN}¡${getPositionText(newPosition)}!", 0, 30, 10)
            }
            newPosition > oldPosition -> {
                // Empeoró posición
                val decline = newPosition - oldPosition
                val message = when (decline) {
                    1 -> "${ChatColor.RED}Bajaste 1 posición. Ahora vas ${getPositionText(newPosition)}"
                    else -> "${ChatColor.RED}Bajaste $decline posiciones. Ahora vas ${getPositionText(newPosition)}"
                }
                player.sendMessage("${ChatColor.GOLD}[Carrera de Barcos] $message")
            }
        }
        
        // Mostrar posición actual
        player.sendMessage("${ChatColor.GRAY}[Posición] ${ChatColor.YELLOW}${getPositionText(newPosition)} ${ChatColor.GRAY}($newPosition/$totalPlayers)")
    }
    
    /**
     * Obtiene el texto de la posición con formato.
     */
    private fun getPositionText(position: Int): String {
        return when (position) {
            1 -> "${ChatColor.GOLD}1° LUGAR"
            2 -> "${ChatColor.GRAY}2° LUGAR"
            3 -> "${ChatColor.RED}3° LUGAR"
            else -> "${ChatColor.WHITE}${position}° LUGAR"
        }
    }
    
    /**
     * Crea una barrera para prevenir el avance prematuro.
     */
    private fun createInvisibleBarrier(players: List<Player>) {
        val trackWorld = plugin.server.getWorld(trackWorldName) ?: return
        
        // Crear barrera en todo el ancho de la pista 
        val barrierX = 362.0  // Línea de salida exacta
        val barrierZMin = -398.0  // Ancho mínimo de la pista
        val barrierZMax = -396.0  // Ancho máximo de la pista
        
        // Crear barrera en todo el ancho de la pista
        for (z in barrierZMin.toInt()..barrierZMax.toInt()) {
            // Crear barrera vertical (4 bloques de alto para mayor seguridad)
            for (y in 0..3) {
                val barrierLocation = Location(
                    trackWorld,
                    barrierX,
                    71.0 + y,  // Altura fija de la pista
                    z.toDouble()
                )
                
                // Guardar el bloque original y colocar barrera 
                val block = barrierLocation.block
                barrierBlocks.add(barrierLocation)
                
                // Usar bloques visibles para debug (GLASS para que sea visible)
                block.type = Material.GLASS
            }
        }
        
        plugin.logger.info("Barrera de vidrio creada para ${players.size} jugadores")
        
        // Notificar a los jugadores que la barrera está activa
        players.forEach { player ->
            player.sendMessage("${ChatColor.GOLD}[Carrera de Barcos] ${ChatColor.YELLOW}Barrera de seguridad activada. ¡Espera el GO!")
        }
    }
    
    /**
     * Remueve la barrera 
     */
    private fun removeInvisibleBarrier() {
        barrierBlocks.forEach { location ->
            try {
                location.block.type = Material.AIR
            } catch (e: Exception) {
                plugin.logger.warning("Error al remover barrera en ${location.x}, ${location.y}, ${location.z}")
            }
        }
        barrierBlocks.clear()
        plugin.logger.info("Barrera removida")
    }
    
    /**
     * Controla el movimiento de los barcos para prevenir avance prematuro.
     * (Método de respaldo en caso de que la barrera no funcione)
     */
    @EventHandler
    fun onVehicleMove(event: VehicleMoveEvent) {
        if (!gameRunning || raceStarted) return
        
        val vehicle = event.vehicle
        if (vehicle !is Boat) return
        
        // Verificar si el barco pertenece a un jugador de la carrera
        val player = vehicle.passengers.firstOrNull { it is org.bukkit.entity.Player } as? org.bukkit.entity.Player ?: return
        if (!activePlayers.contains(player)) return
        
        // Si el jugador intenta avanzar antes de tiempo, cancelar el movimiento
        val from = event.from
        val to = event.to
        
        // Si se movió hacia adelante (X mayor), regresarlo inmediatamente
        if (to.x > from.x) {
            // Teletransportar de vuelta a la posición original
            vehicle.teleport(from)
            
            player.sendMessage("${ChatColor.RED}[Carrera de Barcos] ${ChatColor.YELLOW}¡TRAMPA DETECTADA! ¡Espera la señal de GO!")
            player.sendMessage("${ChatColor.RED}No puedes avanzar antes de que comience la carrera.")
            
            // Efecto visual para mostrar que es trampa
            player.sendTitle("${ChatColor.RED}¡TRAMPA!", "${ChatColor.YELLOW}Espera el GO", 0, 20, 10)
            
            plugin.logger.info("Trampa detectada: ${player.name} intentó avanzar antes del GO")
        }
    }
}