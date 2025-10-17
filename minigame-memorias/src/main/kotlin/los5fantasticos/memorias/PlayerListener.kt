package los5fantasticos.memorias

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Listener actualizado para el sistema refactorizado.
 * Delega toda la lógica de juego a DueloMemorias a través del GameManager.
 * 
 * FUNCIONALIDADES:
 * - Manejo de clics en bloques del tablero
 * - Sistema de selección con varita para administradores
 * - Protección INTEGRAL de parcelas activas:
 *   * Bloqueo total de modificación de bloques (romper/colocar)
 *   * Sistema anti-escape (límites físicos de la parcela)
 *   * Inmunidad para administradores con permiso memorias.admin
 */
class PlayerListener(private val gameManager: GameManager) : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        gameManager.removePlayer(event.player)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        val player = event.player
        
        // PRIORIDAD 1: Verificar si está usando la varita de selección
        if (SelectionManager.isInSelectionMode(player)) {
            val item = player.inventory.itemInMainHand
            
            if (SelectionManager.isSelectionWand(item)) {
                event.isCancelled = true
                
                when (event.action) {
                    Action.LEFT_CLICK_BLOCK -> {
                        SelectionManager.setPos1(player, block.location)
                    }
                    Action.RIGHT_CLICK_BLOCK -> {
                        SelectionManager.setPos2(player, block.location)
                    }
                    else -> {}
                }
                return
            }
        }
        
        // PRIORIDAD 2: Continuar con lógica de juego normal
        
        // Verificar si es un cartel de unirse al juego
        if (block.state is Sign) {
            val sign = block.state as Sign

            @Suppress("DEPRECATION")
            val line0 = sign.getLine(0)

            if (line0.equals("[Memorias]", ignoreCase = true)) {
                gameManager.joinPlayer(player)
                event.isCancelled = true
                return
            }
        }
        
        // Verificar si el jugador está en un duelo activo
        val duelo = gameManager.getDueloByPlayer(player) ?: return
        
        // Solo procesar clics derechos en bloques
        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }
        
        // Verificar si el bloque es parte del tablero (lana gris o de colores)
        val blockType = block.type
        if (!isGameBlock(blockType)) {
            return
        }
        
        // Delegar el manejo del clic al duelo
        val success = duelo.handlePlayerClick(player, block.location)
        if (success) {
            event.isCancelled = true
        }
    }
    
    /**
     * Protege las parcelas activas contra destrucción de bloques.
     * 
     * LÓGICA REFACTORIZADA:
     * - NINGÚN jugador puede romper bloques dentro de una parcela con duelo activo
     * - La única interacción permitida es el clic derecho en bloques del tablero (manejado en onPlayerInteract)
     * - Administradores con permiso memorias.admin o OP pueden ignorar esta restricción
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        
        // EXCEPCIÓN: Permitir a admins romper bloques para configuración/depuración
        if (player.hasPermission("memorias.admin") || player.isOp) {
            return
        }
        
        // PROTECCIÓN TOTAL: Verificar si el bloque está en alguna parcela activa
        if (isBlockInActiveParcel(block.location)) {
            event.isCancelled = true
            player.sendActionBar(
                Component.text("✗ No puedes romper bloques en áreas de juego activas", NamedTextColor.RED)
            )
        }
    }
    
    /**
     * Protege las parcelas activas contra colocación de bloques.
     * 
     * LÓGICA IDÉNTICA A BlockBreakEvent.
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val block = event.block
        
        // EXCEPCIÓN: Permitir a admins colocar bloques para configuración/depuración
        if (player.hasPermission("memorias.admin") || player.isOp) {
            return
        }
        
        // PROTECCIÓN TOTAL: Verificar si el bloque está en alguna parcela activa
        if (isBlockInActiveParcel(block.location)) {
            event.isCancelled = true
            // Mensaje discreto (no intrusivo)
            player.sendActionBar(
                Component.text("✗ No puedes modificar bloques en áreas de juego activas", NamedTextColor.RED)
            )
        }
    }
    
    /**
     * PROTECCIÓN DE LÍMITES FÍSICOS (ANTI-ESCAPE).
     * 
     * Impide que los jugadores en duelo salgan de los límites de su parcela.
     * Usa ActionBar para mensajes discretos y no intrusivos.
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        
        // Optimización: Solo verificar si el jugador realmente se movió de bloque
        val from = event.from
        val to = event.to ?: return
        
        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) {
            return // No hubo movimiento de bloque
        }
        
        // INMUNIDAD DE ADMINISTRADOR
        if (player.hasPermission("memorias.admin") || player.isOp) {
            return
        }
        
        // Verificar si el jugador está en un duelo
        val duelo = gameManager.getDueloByPlayer(player) ?: return
        
        // Verificar si la nueva posición está fuera de la parcela
        val parcela = duelo.parcela
        
        if (!parcela.contains(to)) {
            // El jugador está intentando salir de la parcela - BLOQUEAR
            event.isCancelled = true
            
            // Mensaje discreto en ActionBar (no intrusivo)
            player.sendActionBar(
                Component.text("⚠ No puedes salir del área de juego", NamedTextColor.GOLD)
            )
        }
    }
    
    /**
     * Verifica si una ubicación pertenece a alguna parcela activa (con duelo en curso).
     * 
     * @deprecated Usar findParcelForLocation() en su lugar para mejor rendimiento
     */
    @Deprecated("Usar findParcelForLocation() para obtener la parcela directamente")
    private fun isBlockInActiveParcel(location: org.bukkit.Location): Boolean {
        return gameManager.getAllActiveDuels().any { duelo ->
            duelo.parcela.contains(location)
        }
    }
    
    /**
     * Encuentra la parcela activa que contiene una ubicación específica.
     * 
     * @param location Ubicación a verificar
     * @return La parcela que contiene la ubicación, o null si no está en ninguna parcela activa
     */
    private fun findParcelForLocation(location: org.bukkit.Location): Parcela? {
        return gameManager.getAllActiveDuels()
            .map { it.parcela }
            .firstOrNull { it.contains(location) }
    }
    
    /**
     * Verifica si un material es parte del juego de memorias.
     * Acepta el material oculto (GRAY_WOOL) y cualquier bloque sólido del mazo.
     */
    private fun isGameBlock(material: Material): Boolean {
        // Material oculto siempre es parte del juego
        if (material == Material.GRAY_WOOL) {
            return true
        }
        
        // Verificar si es un bloque sólido (todos los del mazo lo son)
        return material.isBlock && material.isSolid
    }
}

