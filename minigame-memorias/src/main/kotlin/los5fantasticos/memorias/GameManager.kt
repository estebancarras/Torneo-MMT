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
    
    // Cola de espera para emparejar jugadores
    private val colaEspera = mutableListOf<Player>()
    
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
     * Añade un jugador a la cola de espera y intenta emparejarlo.
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
        
        // Añadir a la cola
        colaEspera.add(jugador)
        jugador.sendMessage(Component.text("Te has unido a la cola. Jugadores esperando: ${colaEspera.size}", NamedTextColor.GREEN))
        
        // Teleportar al lobby de la arena si existe
        arena.lobbySpawn?.let { jugador.teleport(it) }
        
        // Intentar emparejar
        intentarEmparejar()
    }
    
    /**
     * Intenta emparejar jugadores de la cola y crear duelos.
     */
    private fun intentarEmparejar() {
        val arena = arenaActual ?: return
        
        // Mientras haya al menos 2 jugadores en cola y parcelas disponibles
        while (colaEspera.size >= 2) {
            // Obtener parcela libre
            val parcela = arena.getParcelaLibre(parcelasOcupadas.keys)
            if (parcela == null) {
                // No hay parcelas disponibles
                colaEspera.forEach { jugador ->
                    jugador.sendMessage(Component.text("Todas las parcelas están ocupadas. Espera a que termine un duelo.", NamedTextColor.YELLOW))
                }
                break
            }
            
            // Tomar dos jugadores de la cola
            val jugador1 = colaEspera.removeAt(0)
            val jugador2 = colaEspera.removeAt(0)
            
            // Crear duelo
            crearDuelo(jugador1, jugador2, parcela)
        }
    }
    
    /**
     * Crea un duelo entre dos jugadores en una parcela disponible.
     */
    private fun crearDuelo(jugador1: Player, jugador2: Player, parcela: Parcela) {
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
     * Remueve un jugador de su duelo actual.
     */
    fun removePlayer(jugador: Player) {
        // Remover de cola de espera
        colaEspera.remove(jugador)
        
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
        colaEspera.clear()
        
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
            Jugadores en cola: ${colaEspera.size}
            Parcelas ocupadas: ${parcelasOcupadas.size}
            Game Loop activo: ${gameLoopTask != null && !gameLoopTask!!.isCancelled}
        """.trimIndent()
    }
}
