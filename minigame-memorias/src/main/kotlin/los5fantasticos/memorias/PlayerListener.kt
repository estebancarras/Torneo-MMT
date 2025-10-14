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
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Listener actualizado para el sistema refactorizado.
 * Delega toda la lógica de juego a DueloMemorias a través del GameManager.
 * 
 * FUNCIONALIDADES:
 * - Manejo de clics en bloques del tablero
 * - Sistema de selección con varita para administradores
 * - Protección de parcelas activas contra modificaciones no autorizadas
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
     * LÓGICA:
     * - Si el jugador está en un duelo: Solo puede romper bloques FUERA de su tablero
     * - Si el jugador NO está en un duelo: No puede romper bloques en NINGUNA parcela activa
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        
        // Permitir a admins romper bloques
        if (player.hasPermission("memorias.admin") || player.isOp) {
            return
        }
        
        // Obtener duelo del jugador
        val dueloJugador = gameManager.getDueloByPlayer(player)
        
        if (dueloJugador != null) {
            // El jugador está en un duelo
            // Solo puede interactuar con bloques DENTRO de su parcela (tablero del juego)
            val parcela = dueloJugador.parcela
            
            if (!parcela.isInTablero(block.location)) {
                // Bloque fuera del tablero - cancelar
                event.isCancelled = true
                player.sendMessage(
                    Component.text("✗ No puedes romper bloques fuera del tablero durante el duelo", NamedTextColor.RED)
                )
            }
            // Si está dentro del tablero, el juego maneja la lógica
        } else {
            // El jugador NO está en un duelo
            // Verificar si el bloque pertenece a alguna parcela activa
            if (isBlockInActiveParcel(block.location)) {
                event.isCancelled = true
                player.sendMessage(
                    Component.text("✗ No puedes romper bloques en parcelas de juego activas", NamedTextColor.RED)
                )
            }
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
        
        // Permitir a admins colocar bloques
        if (player.hasPermission("memorias.admin") || player.isOp) {
            return
        }
        
        // Obtener duelo del jugador
        val dueloJugador = gameManager.getDueloByPlayer(player)
        
        if (dueloJugador != null) {
            // El jugador está en un duelo
            // No puede colocar bloques en ninguna parte
            event.isCancelled = true
            player.sendMessage(
                Component.text("✗ No puedes colocar bloques durante un duelo", NamedTextColor.RED)
            )
        } else {
            // El jugador NO está en un duelo
            // Verificar si el bloque pertenece a alguna parcela activa
            if (isBlockInActiveParcel(block.location)) {
                event.isCancelled = true
                player.sendMessage(
                    Component.text("✗ No puedes colocar bloques en parcelas de juego activas", NamedTextColor.RED)
                )
            }
        }
    }
    
    /**
     * Verifica si una ubicación pertenece a alguna parcela activa (con duelo en curso).
     */
    private fun isBlockInActiveParcel(location: org.bukkit.Location): Boolean {
        return gameManager.getAllActiveDuels().any { duelo ->
            duelo.parcela.isInTablero(location)
        }
    }
    
    /**
     * Verifica si un material es parte del juego de memorias.
     */
    private fun isGameBlock(material: Material): Boolean {
        return material == Material.GRAY_WOOL ||
               material == Material.RED_WOOL ||
               material == Material.BLUE_WOOL ||
               material == Material.GREEN_WOOL ||
               material == Material.YELLOW_WOOL ||
               material == Material.LIME_WOOL ||
               material == Material.ORANGE_WOOL ||
               material == Material.PINK_WOOL ||
               material == Material.PURPLE_WOOL
    }
}

