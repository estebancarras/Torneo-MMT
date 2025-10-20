package los5fantasticos.minigameCarrerabarcos.services

import los5fantasticos.minigameCarrerabarcos.game.ArenaCarrera
import los5fantasticos.minigameCarrerabarcos.game.Carrera
import los5fantasticos.torneo.TorneoPlugin
import los5fantasticos.torneo.util.GameTimer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.GameMode
import org.bukkit.entity.Boat
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import java.time.Duration

/**
 * Gestor de la lógica de juego de carreras.
 * 
 * RESPONSABILIDADES:
 * - Gestionar carreras activas
 * - Iniciar y finalizar carreras
 * - Actualizar el progreso de los jugadores
 * - Otorgar puntos y premios
 * - Coordinar con el TorneoManager para puntuación
 * 
 * ARQUITECTURA:
 * Este es el orquestador central de la lógica de juego.
 * Separa completamente la lógica de negocio de los comandos y eventos.
 */
class GameManager(
    private val plugin: Plugin,
    private val torneoPlugin: TorneoPlugin
) {
    
    companion object {
        private const val GAME_NAME = "Carrera de Barcos"
        
        // Puntuación
        private const val PUNTOS_PRIMER_LUGAR = 100
        private const val PUNTOS_SEGUNDO_LUGAR = 75
        private const val PUNTOS_TERCER_LUGAR = 50
        private const val PUNTOS_PARTICIPACION = 25
        private const val PUNTOS_CHECKPOINT = 5
        
        // Tiempos
        private const val COUNTDOWN_SECONDS = 5
        private const val RACE_DURATION_SECONDS = 300 // 5 minutos
    }
    
    /**
     * Lista de carreras activas.
     */
    private val carrerasActivas = mutableListOf<Carrera>()
    
    /**
     * Mapa de jugadores a sus carreras activas.
     */
    private val jugadorEnCarrera = mutableMapOf<Player, Carrera>()
    
    /**
     * Mapa de temporizadores de carrera.
     */
    private val timersPorCarrera = mutableMapOf<Carrera, GameTimer>()
    
    /**
     * Inicia una nueva carrera en una arena específica.
     * 
     * @param arena Arena donde se correrá
     * @param jugadores Jugadores que participarán
     * @return La carrera creada, o null si hubo un error
     */
    fun iniciarCarrera(arena: ArenaCarrera, jugadores: List<Player>): Carrera? {
        // Validaciones
        if (!arena.isValid()) {
            plugin.logger.warning("Arena '${arena.nombre}' no está completamente configurada")
            return null
        }
        
        if (jugadores.isEmpty()) {
            plugin.logger.warning("No hay jugadores para iniciar la carrera")
            return null
        }
        
        if (jugadores.size > arena.getMaxPlayers()) {
            plugin.logger.warning("Demasiados jugadores (${jugadores.size}) para la arena '${arena.nombre}' (máx: ${arena.getMaxPlayers()})")
            return null
        }
        
        // Crear la carrera
        val carrera = Carrera(arena)
        
        // Añadir jugadores
        jugadores.forEach { player ->
            carrera.addJugador(player)
            jugadorEnCarrera[player] = carrera
        }
        
        carrerasActivas.add(carrera)
        
        plugin.logger.info("[Carrera de Barcos] Carrera iniciada en '${arena.nombre}' con ${jugadores.size} jugadores")
        
        // Teletransportar jugadores y dar barcos
        prepararJugadores(carrera)
        
        // Iniciar countdown
        iniciarCountdown(carrera)
        
        return carrera
    }
    
    /**
     * Prepara a los jugadores para la carrera (teletransporte, barcos, etc.).
     */
    private fun prepararJugadores(carrera: Carrera) {
        val jugadores = carrera.getJugadores().toList()
        val spawns = carrera.arena.spawns
        
        jugadores.forEachIndexed { index, player ->
            if (index < spawns.size) {
                val spawn = spawns[index]
                
                // Teletransportar
                player.teleport(spawn)
                
                // Configurar jugador
                player.gameMode = GameMode.ADVENTURE
                player.inventory.clear()
                player.health = 20.0
                player.foodLevel = 20
                
                // Crear barco
                val boat = spawn.world?.spawnEntity(spawn, EntityType.BOAT) as? Boat
                if (boat != null) {
                    boat.addPassenger(player)
                    carrera.setBarco(player, boat)
                }
                
                // Mensaje
                player.sendMessage(
                    Component.text("¡Prepárate para la carrera!", NamedTextColor.GOLD, TextDecoration.BOLD)
                )
            }
        }
    }
    
    /**
     * Inicia el countdown antes de comenzar la carrera con temporizador visual.
     */
    private fun iniciarCountdown(carrera: Carrera) {
        carrera.setEstado(Carrera.EstadoCarrera.INICIANDO)
        
        val jugadores = carrera.getJugadores()
        
        // Crear temporizador de countdown con BossBar
        val countdownTimer = GameTimer(
            plugin = torneoPlugin,
            durationInSeconds = COUNTDOWN_SECONDS,
            title = "§e§l⚠ PREPARÁNDOSE PARA INICIAR",
            onTick = { secondsLeft ->
                // Mostrar títulos y sonidos durante el countdown
                if (secondsLeft > 0 && secondsLeft <= 3) {
                    val title = Title.title(
                        Component.text(secondsLeft.toString(), NamedTextColor.YELLOW, TextDecoration.BOLD),
                        Component.text("Prepárate...", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
                    )
                    
                    jugadores.forEach { player ->
                        player.showTitle(title)
                        player.playSound(player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f)
                    }
                }
            },
            onFinish = {
                // ¡GO!
                val goTitle = Title.title(
                    Component.text("¡GO!", NamedTextColor.GREEN, TextDecoration.BOLD),
                    Component.text("¡Buena suerte!", NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))
                )
                
                jugadores.forEach { player ->
                    player.showTitle(goTitle)
                    player.playSound(player.location, org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f)
                }
                
                carrera.setEstado(Carrera.EstadoCarrera.EN_CURSO)
                plugin.logger.info("[Carrera de Barcos] Carrera en '${carrera.arena.nombre}' iniciada")
                
                // Iniciar temporizador de duración de la carrera
                iniciarTemporizadorCarrera(carrera)
            }
        )
        
        // Añadir todos los jugadores al temporizador
        countdownTimer.addPlayers(jugadores)
        
        // Iniciar el countdown
        countdownTimer.start()
    }
    
    /**
     * Inicia el temporizador de duración de la carrera con BossBar.
     */
    private fun iniciarTemporizadorCarrera(carrera: Carrera) {
        val jugadores = carrera.getJugadores()
        
        // Crear temporizador de duración de la carrera
        val raceTimer = GameTimer(
            plugin = torneoPlugin,
            durationInSeconds = RACE_DURATION_SECONDS,
            title = "§6§l🏁 CARRERA DE BARCOS",
            onTick = { secondsLeft ->
                // Avisos cuando queda poco tiempo
                when (secondsLeft) {
                    60 -> {
                        jugadores.forEach { player ->
                            player.sendMessage(
                                Component.text("⚠ ", NamedTextColor.GOLD)
                                    .append(Component.text("¡Queda 1 minuto!", NamedTextColor.YELLOW, TextDecoration.BOLD))
                            )
                            player.playSound(player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
                        }
                    }
                    30 -> {
                        jugadores.forEach { player ->
                            player.sendMessage(
                                Component.text("⚠ ", NamedTextColor.RED)
                                    .append(Component.text("¡Quedan 30 segundos!", NamedTextColor.YELLOW, TextDecoration.BOLD))
                            )
                            player.playSound(player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f)
                        }
                    }
                    10 -> {
                        jugadores.forEach { player ->
                            player.sendMessage(
                                Component.text("⚠ ", NamedTextColor.DARK_RED)
                                    .append(Component.text("¡ÚLTIMOS 10 SEGUNDOS!", NamedTextColor.RED, TextDecoration.BOLD))
                            )
                            player.playSound(player.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f)
                        }
                    }
                }
            },
            onFinish = {
                // Tiempo agotado - finalizar carrera
                plugin.logger.info("[Carrera de Barcos] Tiempo agotado en carrera '${carrera.arena.nombre}'")
                
                jugadores.forEach { player ->
                    player.sendMessage(
                        Component.text("⏱ ", NamedTextColor.RED, TextDecoration.BOLD)
                            .append(Component.text("¡Tiempo agotado!", NamedTextColor.YELLOW))
                    )
                }
                
                finalizarCarreraPorTiempo(carrera)
            }
        )
        
        // Añadir todos los jugadores al temporizador
        raceTimer.addPlayers(jugadores)
        
        // Guardar referencia al temporizador
        timersPorCarrera[carrera] = raceTimer
        
        // Iniciar el temporizador
        raceTimer.start()
    }
    
    /**
     * Actualiza el progreso de un jugador cuando atraviesa un checkpoint.
     * 
     * @param player Jugador que atravesó el checkpoint
     * @return true si el progreso se actualizó correctamente
     */
    fun actualizarProgresoJugador(player: Player): Boolean {
        val carrera = jugadorEnCarrera[player] ?: return false
        
        // Solo actualizar si la carrera está en curso
        if (carrera.estado != Carrera.EstadoCarrera.EN_CURSO) {
            return false
        }
        
        // Avanzar progreso
        val avanzado = carrera.avanzarProgreso(player)
        
        if (avanzado) {
            val progreso = carrera.getProgreso(player)
            val totalCheckpoints = carrera.arena.checkpoints.size
            
            // Feedback al jugador
            player.sendActionBar(
                Component.text("✓ Checkpoint $progreso/$totalCheckpoints", NamedTextColor.GREEN, TextDecoration.BOLD)
            )
            player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f)
            
            // Otorgar puntos por checkpoint
            torneoPlugin.torneoManager.addScore(
                player.uniqueId,
                GAME_NAME,
                PUNTOS_CHECKPOINT,
                "Checkpoint $progreso"
            )
            
            plugin.logger.info("[Carrera de Barcos] ${player.name} pasó checkpoint $progreso/$totalCheckpoints")
        }
        
        return avanzado
    }
    
    /**
     * Finaliza la carrera para un jugador (cuando cruza la meta).
     * 
     * @param player Jugador que cruzó la meta
     */
    fun finalizarJugador(player: Player) {
        val carrera = jugadorEnCarrera[player] ?: return
        
        // Verificar que la carrera esté en curso
        if (carrera.estado != Carrera.EstadoCarrera.EN_CURSO) {
            return
        }
        
        // Verificar que el jugador haya pasado todos los checkpoints
        if (!carrera.puedeFinalizarCarrera(player)) {
            player.sendActionBar(
                Component.text("✗ Debes pasar todos los checkpoints primero", NamedTextColor.RED)
            )
            return
        }
        
        // Marcar como finalizado
        val posicion = carrera.finalizarJugador(player)
        
        // Otorgar puntos según posición
        val puntos = when (posicion) {
            1 -> PUNTOS_PRIMER_LUGAR
            2 -> PUNTOS_SEGUNDO_LUGAR
            3 -> PUNTOS_TERCER_LUGAR
            else -> PUNTOS_PARTICIPACION
        }
        
        torneoPlugin.torneoManager.addScore(
            player.uniqueId,
            GAME_NAME,
            puntos,
            "Posición #$posicion"
        )
        
        // Registrar victoria si es primer lugar
        if (posicion == 1) {
            torneoPlugin.torneoManager.recordGameWon(player, GAME_NAME)
        }
        
        // Registrar partida jugada
        torneoPlugin.torneoManager.recordGamePlayed(player, GAME_NAME)
        
        // Anuncio
        val mensaje = when (posicion) {
            1 -> Component.text("🏆 ¡PRIMER LUGAR! 🏆", NamedTextColor.GOLD, TextDecoration.BOLD)
            2 -> Component.text("🥈 ¡SEGUNDO LUGAR! 🥈", NamedTextColor.GRAY, TextDecoration.BOLD)
            3 -> Component.text("🥉 ¡TERCER LUGAR! 🥉", NamedTextColor.YELLOW, TextDecoration.BOLD)
            else -> Component.text("¡Carrera completada!", NamedTextColor.GREEN, TextDecoration.BOLD)
        }
        
        val title = Title.title(
            mensaje,
            Component.text("+$puntos puntos", NamedTextColor.YELLOW),
            Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
        )
        
        player.showTitle(title)
        player.playSound(player.location, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
        
        // Anunciar a todos
        carrera.getJugadores().forEach { jugador ->
            jugador.sendMessage(
                Component.text("${player.name} ", NamedTextColor.YELLOW)
                    .append(Component.text("finalizó en posición #$posicion", NamedTextColor.WHITE))
            )
        }
        
        plugin.logger.info("[Carrera de Barcos] ${player.name} finalizó en posición #$posicion")
        
        // Verificar si todos terminaron
        if (carrera.todosFinalizaron()) {
            finalizarCarrera(carrera)
        }
    }
    
    /**
     * Finaliza una carrera completamente.
     */
    fun finalizarCarrera(carrera: Carrera) {
        carrera.setEstado(Carrera.EstadoCarrera.FINALIZADA)
        
        // Detener el temporizador si existe
        timersPorCarrera[carrera]?.stop()
        timersPorCarrera.remove(carrera)
        
        plugin.logger.info("[Carrera de Barcos] Carrera en '${carrera.arena.nombre}' finalizada")
        
        // Mostrar podio
        mostrarPodio(carrera)
        
        // Limpiar después de 10 segundos
        object : BukkitRunnable() {
            override fun run() {
                limpiarCarrera(carrera)
            }
        }.runTaskLater(plugin, 200L) // 10 segundos
    }
    
    /**
     * Finaliza una carrera por tiempo agotado.
     * Asigna posiciones según el progreso de cada jugador.
     */
    private fun finalizarCarreraPorTiempo(carrera: Carrera) {
        // Obtener jugadores que no han finalizado
        val jugadoresActivos = carrera.getJugadores().filter { player ->
            !carrera.getJugadoresFinalizados().contains(player)
        }
        
        // Ordenar por progreso (más checkpoints = mejor posición)
        val jugadoresOrdenados = jugadoresActivos.sortedByDescending { player ->
            carrera.getProgreso(player)
        }
        
        // Asignar posiciones finales
        jugadoresOrdenados.forEach { player ->
            val progreso = carrera.getProgreso(player)
            
            // Otorgar puntos según progreso
            val puntos = PUNTOS_PARTICIPACION + (progreso * PUNTOS_CHECKPOINT)
            
            torneoPlugin.torneoManager.addScore(
                player.uniqueId,
                GAME_NAME,
                puntos,
                "Participación ($progreso checkpoints)"
            )
            
            torneoPlugin.torneoManager.recordGamePlayed(player, GAME_NAME)
            
            player.sendMessage(
                Component.text("Progreso: ", NamedTextColor.GRAY)
                    .append(Component.text("$progreso/${carrera.arena.checkpoints.size}", NamedTextColor.YELLOW))
                    .append(Component.text(" checkpoints", NamedTextColor.GRAY))
            )
        }
        
        finalizarCarrera(carrera)
    }
    
    /**
     * Muestra el podio final de la carrera.
     */
    private fun mostrarPodio(carrera: Carrera) {
        val finalizados = carrera.getJugadoresFinalizados()
        
        val podioMessage = Component.text()
            .append(Component.text("════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.newline())
            .append(Component.text("   🏁 RESULTADOS FINALES 🏁", NamedTextColor.YELLOW, TextDecoration.BOLD))
            .append(Component.newline())
            .append(Component.text("════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.newline())
        
        finalizados.forEachIndexed { index, player ->
            val posicion = index + 1
            val emoji = when (posicion) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "  "
            }
            
            podioMessage.append(Component.text("$emoji #$posicion - ", NamedTextColor.WHITE))
                .append(Component.text(player.name, NamedTextColor.YELLOW))
                .append(Component.newline())
        }
        
        podioMessage.append(Component.text("════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
        
        // Enviar a todos los jugadores
        carrera.getJugadores().forEach { player ->
            player.sendMessage(podioMessage.build())
        }
    }
    
    /**
     * Limpia una carrera (remueve barcos, jugadores, etc.).
     */
    private fun limpiarCarrera(carrera: Carrera) {
        // Limpiar barcos
        carrera.limpiarBarcos()
        
        // Remover jugadores del mapa
        carrera.getJugadores().forEach { player ->
            jugadorEnCarrera.remove(player)
            
            // Teletransportar al lobby si existe
            carrera.arena.lobby?.let { lobby ->
                player.teleport(lobby)
            }
        }
        
        // Remover de la lista de carreras activas
        carrerasActivas.remove(carrera)
        
        plugin.logger.info("[Carrera de Barcos] Carrera en '${carrera.arena.nombre}' limpiada")
    }
    
    /**
     * Obtiene la carrera en la que está un jugador.
     */
    fun getCarreraDeJugador(player: Player): Carrera? {
        return jugadorEnCarrera[player]
    }
    
    /**
     * Verifica si un jugador está en una carrera activa.
     */
    fun estaEnCarrera(player: Player): Boolean {
        return jugadorEnCarrera.containsKey(player)
    }
    
    /**
     * Remueve a un jugador de su carrera actual.
     */
    fun removerJugadorDeCarrera(player: Player) {
        val carrera = jugadorEnCarrera[player] ?: return
        
        carrera.removeJugador(player)
        jugadorEnCarrera.remove(player)
        
        player.sendMessage(
            Component.text("Has abandonado la carrera", NamedTextColor.YELLOW)
        )
        
        // Teletransportar al lobby
        carrera.arena.lobby?.let { lobby ->
            player.teleport(lobby)
        }
        
        plugin.logger.info("[Carrera de Barcos] ${player.name} abandonó la carrera")
    }
    
    /**
     * Obtiene todas las carreras activas.
     */
    fun getCarrerasActivas(): List<Carrera> {
        return carrerasActivas.toList()
    }
    
    /**
     * Finaliza todas las carreras activas (para shutdown).
     */
    fun finalizarTodasLasCarreras() {
        // Detener todos los temporizadores
        timersPorCarrera.values.forEach { timer ->
            timer.stop()
        }
        timersPorCarrera.clear()
        
        carrerasActivas.toList().forEach { carrera ->
            limpiarCarrera(carrera)
        }
        
        carrerasActivas.clear()
        jugadorEnCarrera.clear()
        
        plugin.logger.info("[Carrera de Barcos] Todas las carreras finalizadas")
    }
    
    /**
     * Obtiene estadísticas del sistema.
     */
    fun getEstadisticas(): String {
        return buildString {
            appendLine("=== Estadísticas de Carreras ===")
            appendLine("Carreras activas: ${carrerasActivas.size}")
            appendLine("Jugadores en carreras: ${jugadorEnCarrera.size}")
            appendLine()
            
            carrerasActivas.forEach { carrera ->
                appendLine(carrera.getEstadisticas())
                appendLine()
            }
        }
    }
}
