package los5fantasticos.memorias

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Orquestador central del minijuego Memorias.
 * Gestiona múltiples duelos simultáneos mediante un Game Loop centralizado.
 * 
 * ARQUITECTURA:
 * - UNA SOLA BukkitTask que actualiza todos los duelos activos
 * - Los duelos NO crean sus propias tasks
 * - Soporte para alta concurrencia mediante ConcurrentHashMap
 */
class GameManager(
    val plugin: Plugin,
    private val memoriasManager: MemoriasManager
) {
    // Duelos activos indexados por UUID único
    private val duelosActivos = ConcurrentHashMap<UUID, DueloMemorias>()
    
    // Mapeo de jugadores a sus duelos
    private val jugadorADuelo = ConcurrentHashMap<UUID, UUID>()
    
    // Parcelas ocupadas (para evitar colisiones)
    private val parcelasOcupadas = ConcurrentHashMap<Parcela, UUID>()
    
    // Game Loop centralizado - LA ÚNICA BukkitTask
    private var gameLoopTask: BukkitTask? = null
    
    // Arena configurada
    private var arenaActual: Arena? = null
    
    // ===== SISTEMA DE SALA DE ESPERA (LOBBY) =====
    // Jugadores esperando en el lobby para que un admin inicie la ronda
    private val jugadoresEnLobby = mutableListOf<Player>()
    
    // Task de feedback del lobby (muestra contador de jugadores)
    private var lobbyFeedbackTask: BukkitTask? = null
    
    /**
     * Establece la arena actual del juego.
     */
    fun setArena(arena: Arena) {
        this.arenaActual = arena
        plugin.logger.info("Arena '${arena.nombre}' configurada con ${arena.getTotalParcelas()} parcelas")
    }
    
    /**
     * Obtiene la arena actual.
     */
    fun getArena(): Arena? = arenaActual
    
    /**
     * Inicia el Game Loop centralizado.
     * Se ejecuta cada 2 ticks (10 veces por segundo).
     */
    fun iniciarGameLoop() {
        // Si ya existe, no crear otro
        if (gameLoopTask != null && !gameLoopTask!!.isCancelled) {
            plugin.logger.warning("Game Loop ya está activo")
            return
        }
        
        gameLoopTask = object : BukkitRunnable() {
            override fun run() {
                // Iterar sobre todos los duelos activos
                duelosActivos.values.forEach { duelo ->
                    try {
                        duelo.actualizar()
                        
                        // Si el duelo terminó, programar limpieza
                        if (duelo.getEstado() == DueloEstado.FINALIZADO) {
                            programarLimpiezaDuelo(duelo)
                        }
                    } catch (e: Exception) {
                        plugin.logger.severe("Error actualizando duelo: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L) // Cada 2 ticks
        
        plugin.logger.info("Game Loop iniciado (actualización cada 2 ticks)")
    }
    
    /**
     * Detiene el Game Loop centralizado.
     */
    fun detenerGameLoop() {
        gameLoopTask?.cancel()
        gameLoopTask = null
        plugin.logger.info("Game Loop detenido")
    }
    
    /**
     * Añade un jugador a la sala de espera (lobby).
     * NO empareja automáticamente - espera a que un admin inicie la ronda.
     */
    fun joinPlayer(jugador: Player) {
        // Verificar que hay arena configurada
        val arena = arenaActual
        if (arena == null) {
            jugador.sendMessage(Component.text("No hay arena configurada. Contacta a un administrador.", NamedTextColor.RED))
            return
        }
        
        // Verificar que hay parcelas disponibles
        if (arena.getTotalParcelas() == 0) {
            jugador.sendMessage(Component.text("El arena no tiene parcelas disponibles.", NamedTextColor.RED))
            return
        }
        
        // Verificar que no esté ya en un duelo
        if (jugadorADuelo.containsKey(jugador.uniqueId)) {
            jugador.sendMessage(Component.text("Ya estás en un duelo activo.", NamedTextColor.YELLOW))
            return
        }
        
        // Verificar que no esté ya en el lobby
        if (jugadoresEnLobby.contains(jugador)) {
            jugador.sendMessage(Component.text("Ya estás en el lobby esperando.", NamedTextColor.YELLOW))
            return
        }
        
        // Añadir al lobby
        jugadoresEnLobby.add(jugador)
        jugador.sendMessage(
            Component.text("✓ Te has unido al lobby de Memorias.", NamedTextColor.GREEN)
                .append(Component.newline())
                .append(Component.text("Esperando a que un administrador inicie la ronda...", NamedTextColor.YELLOW))
        )
        
        // Teleportar al lobby de la arena si existe
        arena.lobbySpawn?.let { jugador.teleport(it) }
        
        // Iniciar feedback del lobby si es el primer jugador
        if (jugadoresEnLobby.size == 1) {
            iniciarLobbyFeedback()
        }
    }
    
    /**
     * Inicia el sistema de feedback del lobby.
     * Muestra un contador de jugadores listos en la ActionBar.
     */
    private fun iniciarLobbyFeedback() {
        // Cancelar tarea anterior si existe
        lobbyFeedbackTask?.cancel()
        
        lobbyFeedbackTask = object : BukkitRunnable() {
            override fun run() {
                // Si no hay jugadores, detener la tarea
                if (jugadoresEnLobby.isEmpty()) {
                    cancel()
                    lobbyFeedbackTask = null
                    return
                }
                
                // Enviar ActionBar a todos los jugadores en el lobby
                val mensaje = Component.text("⏳ Jugadores Listos: ${jugadoresEnLobby.size}", NamedTextColor.YELLOW)
                jugadoresEnLobby.forEach { jugador ->
                    jugador.sendActionBar(mensaje)
                }
            }
        }.runTaskTimer(plugin, 0L, 20L) // Cada segundo
    }
    
    /**
     * Inicia una ronda de torneo con todos los jugadores en el lobby.
     * 
     * VALIDACIONES:
     * 1. Número par de jugadores (si es impar, requiere comodín)
     * 2. Suficientes parcelas disponibles
     * 
     * @return Mensaje de error si falla, null si tiene éxito
     */
    fun startRound(): String? {
        val arena = arenaActual ?: return "Error: No hay arena configurada"
        
        // VALIDACIÓN 1: Verificar que hay jugadores
        if (jugadoresEnLobby.isEmpty()) {
            return "Error: No hay jugadores en el lobby"
        }
        
        // VALIDACIÓN 2: Número par de jugadores
        if (jugadoresEnLobby.size % 2 != 0) {
            return "Error: Hay un número impar de jugadores (${jugadoresEnLobby.size}). Se requiere que un comodín se una."
        }
        
        // VALIDACIÓN 3: Suficientes parcelas
        val duelosNecesarios = jugadoresEnLobby.size / 2
        val parcelasLibres = arena.parcelas.filter { !parcelasOcupadas.containsKey(it) }
        
        if (parcelasLibres.size < duelosNecesarios) {
            return "Error: No hay suficientes parcelas libres. Se necesitan $duelosNecesarios pero solo hay ${parcelasLibres.size} disponibles."
        }
        
        // EMPAREJAMIENTO ALEATORIO
        val jugadoresBarajados = jugadoresEnLobby.shuffled()
        val parejas = jugadoresBarajados.chunked(2)
        
        // CREACIÓN DE DUELOS
        var duelosCreados = 0
        parejas.forEachIndexed { index, pareja ->
            if (pareja.size == 2) {
                val jugador1 = pareja[0]
                val jugador2 = pareja[1]
                val parcela = parcelasLibres[index]
                
                crearDuelo(jugador1, jugador2, parcela)
                duelosCreados++
            }
        }
        
        // LIMPIEZA: Vaciar el lobby
        jugadoresEnLobby.clear()
        
        // Detener feedback del lobby
        lobbyFeedbackTask?.cancel()
        lobbyFeedbackTask = null
        
        // Éxito - devolver null
        return null
    }
    
    /**
     * Crea un duelo entre dos jugadores en una parcela disponible.
     * NOTA: Ahora es pública para ser usada por startRound()
     */
    fun crearDuelo(jugador1: Player, jugador2: Player, parcela: Parcela) {
        val dueloId = UUID.randomUUID()
        val torneoManager = memoriasManager.torneoPlugin.torneoManager
        val duelo = DueloMemorias(jugador1, jugador2, parcela, plugin, torneoManager)
        
        // Registrar duelo
        duelosActivos[dueloId] = duelo
        jugadorADuelo[jugador1.uniqueId] = dueloId
        jugadorADuelo[jugador2.uniqueId] = dueloId
        parcelasOcupadas[parcela] = dueloId
        
        // Notificar
        val mensaje = Component.text("¡Duelo iniciado! ${jugador1.name} vs ${jugador2.name}", NamedTextColor.GREEN)
        jugador1.sendMessage(mensaje)
        jugador2.sendMessage(mensaje)
        
        plugin.logger.info("Duelo creado: ${jugador1.name} vs ${jugador2.name} (ID: $dueloId)")
        
        // Iniciar Game Loop si no está activo
        if (gameLoopTask == null || gameLoopTask!!.isCancelled) {
            iniciarGameLoop()
        }
    }
    
    /**
     * Programa la limpieza de un duelo finalizado.
     * Espera 5 segundos antes de limpiar para dar tiempo a los jugadores de ver los resultados.
     */
    private fun programarLimpiezaDuelo(duelo: DueloMemorias) {
        object : BukkitRunnable() {
            override fun run() {
                limpiarDuelo(duelo)
            }
        }.runTaskLater(plugin, 100L) // 5 segundos
    }
    
    /**
     * Limpia un duelo: otorga puntos, limpia el tablero y libera recursos.
     */
    private fun limpiarDuelo(duelo: DueloMemorias) {
        // Encontrar ID del duelo
        val dueloId = duelosActivos.entries.firstOrNull { it.value == duelo }?.key ?: return
        
        // Otorgar puntos al ganador
        val ganador = duelo.getGanador()
        if (ganador != null) {
            memoriasManager.torneoPlugin.torneoManager.addScore(
                ganador.uniqueId,
                memoriasManager.gameName,
                50,
                "Victoria en duelo de Memorias"
            )
            memoriasManager.recordVictory(ganador)
        }
        
        // Otorgar puntos de participación
        val player1 = duelo.player1
        val player2 = duelo.player2
        
        listOf(player1, player2).forEach { jugador ->
            val puntosParticipacion = duelo.getPuntuacion(jugador) * 5
            if (puntosParticipacion > 0) {
                memoriasManager.torneoPlugin.torneoManager.addScore(
                    jugador.uniqueId,
                    memoriasManager.gameName,
                    puntosParticipacion,
                    "Participación en duelo de Memorias"
                )
            }
            memoriasManager.recordGamePlayed(jugador)
        }
        
        // Teleportar jugadores al lobby
        val arena = arenaActual
        arena?.lobbySpawn?.let { lobby ->
            if (player1.isOnline) {
                player1.teleport(lobby)
                player1.sendMessage(Component.text("¡Has sido enviado al lobby!", NamedTextColor.GREEN))
            }
            if (player2.isOnline) {
                player2.teleport(lobby)
                player2.sendMessage(Component.text("¡Has sido enviado al lobby!", NamedTextColor.GREEN))
            }
        }
        
        // Limpiar tablero
        duelo.limpiarTablero()
        
        // Liberar recursos
        jugadorADuelo.remove(player1.uniqueId)
        jugadorADuelo.remove(player2.uniqueId)
        parcelasOcupadas.entries.removeIf { it.value == dueloId }
        duelosActivos.remove(dueloId)
        
        plugin.logger.info("Duelo limpiado: ${player1.name} vs ${player2.name}")
        
        // Detener Game Loop si no hay más duelos
        if (duelosActivos.isEmpty()) {
            detenerGameLoop()
        }
    }
    
    /**
     * Remueve un jugador de su duelo actual o del lobby.
     */
    fun removePlayer(jugador: Player) {
        // Remover del lobby si está ahí
        jugadoresEnLobby.remove(jugador)
        
        // Buscar duelo del jugador
        val dueloId = jugadorADuelo[jugador.uniqueId] ?: return
        val duelo = duelosActivos[dueloId] ?: return
        
        // Determinar ganador (el otro jugador)
        val ganador = if (duelo.player1 == jugador) duelo.player2 else duelo.player1
        
        // Notificar abandono
        ganador.sendMessage(Component.text("${jugador.name} abandonó el duelo. ¡Ganaste por abandono!", NamedTextColor.GOLD))
        
        // Limpiar inmediatamente
        limpiarDuelo(duelo)
    }
    
    /**
     * Obtiene el duelo de un jugador.
     */
    fun getDueloByPlayer(jugador: Player): DueloMemorias? {
        val dueloId = jugadorADuelo[jugador.uniqueId] ?: return null
        return duelosActivos[dueloId]
    }
    
    /**
     * Finaliza todos los duelos activos y limpia recursos.
     */
    fun endAllGames() {
        duelosActivos.values.toList().forEach { duelo ->
            duelo.limpiarTablero()
        }
        
        duelosActivos.clear()
        jugadorADuelo.clear()
        parcelasOcupadas.clear()
        jugadoresEnLobby.clear()
        
        detenerGameLoop()
        
        plugin.logger.info("Todos los duelos finalizados")
    }
    
    /**
     * Obtiene todos los duelos activos.
     * Utilizado por el sistema de protección de parcelas.
     */
    fun getAllActiveDuels(): Collection<DueloMemorias> {
        return duelosActivos.values
    }
    
    /**
     * Obtiene estadísticas del Game Manager.
     */
    fun getStats(): String {
        return """
            Duelos activos: ${duelosActivos.size}
            Jugadores en duelo: ${jugadorADuelo.size}
            Jugadores en lobby: ${jugadoresEnLobby.size}
            Parcelas ocupadas: ${parcelasOcupadas.size}
            Game Loop activo: ${gameLoopTask != null && !gameLoopTask!!.isCancelled}
        """.trimIndent()
    }
}
