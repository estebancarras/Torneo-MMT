package los5fantasticos.minigameCarrerabarcos.listeners

import los5fantasticos.minigameCarrerabarcos.game.Carrera
import los5fantasticos.minigameCarrerabarcos.services.GameManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.event.vehicle.VehicleDestroyEvent
import org.bukkit.entity.Boat
import org.bukkit.entity.EntityType

/**
 * Listener de eventos de juego para carreras.
 * 
 * RESPONSABILIDADES:
 * - Detectar cuando un jugador atraviesa un checkpoint
 * - Detectar cuando un jugador cruza la meta
 * - Proteger la arena (bloquear construcción/destrucción)
 * - Evitar que jugadores salgan de la región de protección
 * - Manejar desconexiones durante carreras
 * 
 * ARQUITECTURA:
 * Este listener SOLO detecta y notifica.
 * Toda la lógica de negocio está en GameManager.
 * Esto mantiene el código limpio y testeable.
 */
class GameListener(private val gameManager: GameManager) : Listener {
    
    /**
     * Detecta el movimiento de jugadores para verificar checkpoints y meta.
     * 
     * OPTIMIZACIÓN:
     * - Solo procesa si el jugador está en una carrera
     * - Solo procesa si realmente cambió de bloque
     * - Usa verificaciones rápidas antes de operaciones costosas
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val to = event.to ?: return
        val from = event.from
        
        // OPTIMIZACIÓN 1: Solo verificar si cambió de bloque
        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) {
            return
        }
        
        // OPTIMIZACIÓN 2: Solo procesar si el jugador está en una carrera
        val carrera = gameManager.getCarreraDeJugador(player) ?: return
        
        // OPTIMIZACIÓN 3: Solo procesar si la carrera está en curso
        if (carrera.estado != Carrera.EstadoCarrera.EN_CURSO) {
            return
        }
        
        // Verificar protección de región
        verificarProteccionRegion(player, carrera, to, event)
        
        // Verificar checkpoints
        verificarCheckpoint(player, carrera, to)
        
        // Verificar meta
        verificarMeta(player, carrera, to)
    }
    
    /**
     * Verifica si el jugador atravesó su próximo checkpoint.
     */
    private fun verificarCheckpoint(player: org.bukkit.entity.Player, carrera: Carrera, location: org.bukkit.Location) {
        val progresoActual = carrera.getProgreso(player)
        val checkpoints = carrera.arena.checkpoints
        
        // Verificar que no haya terminado todos los checkpoints
        if (progresoActual >= checkpoints.size) {
            return
        }
        
        // Obtener el próximo checkpoint
        val proximoCheckpoint = checkpoints[progresoActual]
        
        // DEBUG: Log para verificar detección
        // player.sendActionBar(Component.text("Checkpoint ${progresoActual + 1}: ${proximoCheckpoint.contains(location)}", NamedTextColor.GRAY))
        
        // Verificar si el jugador está dentro del checkpoint
        if (proximoCheckpoint.contains(location)) {
            // Notificar al GameManager para actualizar progreso
            gameManager.actualizarProgresoJugador(player)
        }
    }
    
    /**
     * Verifica si el jugador cruzó la meta.
     */
    private fun verificarMeta(player: org.bukkit.entity.Player, carrera: Carrera, location: org.bukkit.Location) {
        val meta = carrera.arena.meta ?: return
        
        // DEBUG: Mostrar progreso
        val progreso = carrera.getProgreso(player)
        val totalCheckpoints = carrera.arena.checkpoints.size
        
        // Verificar si el jugador puede finalizar (ha pasado todos los checkpoints)
        if (!carrera.puedeFinalizarCarrera(player)) {
            // Si está en la meta pero no ha completado checkpoints, notificar
            if (meta.contains(location)) {
                player.sendActionBar(
                    Component.text("✗ Debes pasar todos los checkpoints primero ($progreso/$totalCheckpoints)", NamedTextColor.RED)
                )
            }
            return
        }
        
        // Verificar si el jugador está en la meta
        if (meta.contains(location)) {
            // Notificar al GameManager para finalizar al jugador
            gameManager.finalizarJugador(player)
        }
    }
    
    /**
     * Verifica si el jugador intenta salir de la región de protección.
     */
    private fun verificarProteccionRegion(
        player: org.bukkit.entity.Player,
        carrera: Carrera,
        location: org.bukkit.Location,
        event: PlayerMoveEvent
    ) {
        val protectionRegion = carrera.arena.protectionRegion ?: return
        
        // Si el jugador intenta salir de la región de protección, cancelar movimiento
        if (!protectionRegion.contains(location)) {
            event.isCancelled = true
            player.sendActionBar(
                Component.text("✗ No puedes salir del área de la carrera", NamedTextColor.RED)
            )
        }
    }
    
    /**
     * Evita que jugadores rompan bloques durante la carrera.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        
        // Si el jugador está en una carrera
        if (gameManager.estaEnCarrera(player)) {
            // Solo permitir si tiene permiso de admin
            if (!player.hasPermission("torneo.admin.build")) {
                event.isCancelled = true
                player.sendActionBar(
                    Component.text("✗ No puedes romper bloques durante la carrera", NamedTextColor.RED)
                )
            }
        }
    }
    
    /**
     * Evita que jugadores coloquen bloques durante la carrera.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        
        // Si el jugador está en una carrera
        if (gameManager.estaEnCarrera(player)) {
            // Solo permitir si tiene permiso de admin
            if (!player.hasPermission("torneo.admin.build")) {
                event.isCancelled = true
                player.sendActionBar(
                    Component.text("✗ No puedes colocar bloques durante la carrera", NamedTextColor.RED)
                )
            }
        }
    }
    
    /**
     * Maneja la desconexión de jugadores durante una carrera.
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        // Si el jugador está en una carrera, removerlo
        if (gameManager.estaEnCarrera(player)) {
            gameManager.removerJugadorDeCarrera(player)
        }
    }
    
    /**
     * MECÁNICA ANTI-SALIDA: Evita que los jugadores se bajen del barco durante la carrera.
     * 
     * COMPORTAMIENTO:
     * - Si un jugador intenta bajarse del barco durante una carrera activa, el evento se cancela
     * - Como medida adicional de robustez, vuelve a montar al jugador en su barco
     * - Esto garantiza que bugs o interacciones no expulsen al jugador del barco
     * 
     * CRÍTICO PARA EL TORNEO:
     * Esta mecánica es fundamental para evitar que los jugadores pierdan progreso
     * por accidentes o bugs durante el evento presencial.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onVehicleExit(event: VehicleExitEvent) {
        // Solo procesar si es un jugador saliendo de un barco
        val player = event.exited as? org.bukkit.entity.Player ?: return
        val boat = event.vehicle as? Boat ?: return
        
        // Verificar si el jugador está en una carrera activa
        if (!gameManager.estaEnCarrera(player)) {
            return
        }
        
        val carrera = gameManager.getCarreraDeJugador(player) ?: return
        
        // Solo aplicar durante la carrera activa (no en lobby ni finalizada)
        if (carrera.estado != Carrera.EstadoCarrera.EN_CURSO && 
            carrera.estado != Carrera.EstadoCarrera.INICIANDO) {
            return
        }
        
        // CANCELAR el evento para evitar que se baje
        event.isCancelled = true
        
        // MEDIDA ADICIONAL: Volver a montar al jugador en el barco
        // Esto se ejecuta en el siguiente tick para garantizar que funcione
        org.bukkit.Bukkit.getScheduler().runTask(
            gameManager.plugin,
            Runnable {
                if (player.isOnline && !boat.isDead && boat.passengers.isEmpty()) {
                    boat.addPassenger(player)
                }
            }
        )
        
        // Feedback al jugador
        player.sendActionBar(
            Component.text("✗ No puedes bajarte del barco durante la carrera", NamedTextColor.RED)
        )
    }
    
    /**
     * SISTEMA DE RECUPERACIÓN: Regenera barcos destruidos durante la carrera.
     * 
     * COMPORTAMIENTO:
     * - Si el barco de un jugador es destruido durante una carrera activa
     * - Se genera un nuevo barco inmediatamente en la posición del jugador
     * - El jugador es montado automáticamente en el nuevo barco
     * - Esto evita que bugs o accidentes dejen al jugador sin barco
     * 
     * CRÍTICO PARA EL TORNEO:
     * Garantiza que los jugadores siempre tengan un barco funcional durante la carrera.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onVehicleDestroy(event: VehicleDestroyEvent) {
        val boat = event.vehicle as? Boat ?: return
        
        // Buscar si algún jugador en carrera tiene este barco
        val player = boat.passengers.firstOrNull() as? org.bukkit.entity.Player ?: return
        
        // Verificar si el jugador está en una carrera activa
        if (!gameManager.estaEnCarrera(player)) {
            return
        }
        
        val carrera = gameManager.getCarreraDeJugador(player) ?: return
        
        // Solo aplicar durante la carrera activa
        if (carrera.estado != Carrera.EstadoCarrera.EN_CURSO && 
            carrera.estado != Carrera.EstadoCarrera.INICIANDO) {
            return
        }
        
        // CANCELAR la destrucción del barco
        event.isCancelled = true
        
        // Si aún así el barco se destruye (algunos eventos no se pueden cancelar),
        // crear uno nuevo en el siguiente tick
        org.bukkit.Bukkit.getScheduler().runTask(
            gameManager.plugin,
            Runnable {
                if (player.isOnline && boat.isDead) {
                    // Crear nuevo barco en la posición del jugador
                    val newBoat = player.world.spawnEntity(
                        player.location,
                        EntityType.BOAT
                    ) as Boat
                    
                    // Montar al jugador en el nuevo barco
                    newBoat.addPassenger(player)
                    
                    // Actualizar la referencia del barco en la carrera
                    carrera.setBarco(player, newBoat)
                    
                    // Feedback al jugador
                    player.sendActionBar(
                        Component.text("✓ Barco regenerado", NamedTextColor.GREEN)
                    )
                }
            }
        )
    }
}
