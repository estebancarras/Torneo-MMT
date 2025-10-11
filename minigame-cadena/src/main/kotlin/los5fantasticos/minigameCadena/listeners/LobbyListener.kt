package los5fantasticos.minigameCadena.listeners

import los5fantasticos.minigameCadena.MinigameCadena
import los5fantasticos.minigameCadena.game.GameState
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

/**
 * Listener que maneja la interacción del jugador en el lobby.
 * 
 * Responsabilidades:
 * - Detectar clics en los ítems de selección de equipo
 * - Validar y cambiar jugadores entre equipos
 * - Actualizar la UI del inventario para todos los jugadores
 */
class LobbyListener(private val minigame: MinigameCadena) : Listener {
    
    /**
     * Maneja los clics en el inventario del jugador.
     * Solo procesa clics cuando el jugador está en el lobby de una partida.
     */
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        
        // Verificar si el jugador está en una partida
        val game = minigame.gameManager.getPlayerGame(player) ?: return
        
        // Solo procesar si la partida está en estado LOBBY
        if (game.state != GameState.LOBBY) {
            return
        }
        
        // Cancelar el evento para evitar que se mueva el ítem
        event.isCancelled = true
        
        // Obtener el ítem clickeado
        val clickedItem = event.currentItem ?: return
        val material = clickedItem.type
        
        // Verificar si es un ítem de lana (selección de equipo)
        if (!isWoolMaterial(material)) {
            return
        }
        
        // Obtener el equipo correspondiente al material
        val targetTeam = game.getTeamByMaterial(material)
        
        if (targetTeam == null) {
            player.sendMessage("${ChatColor.RED}Error: Equipo no encontrado.")
            return
        }
        
        // Verificar si el equipo está lleno
        if (targetTeam.isFull()) {
            player.sendMessage("${ChatColor.RED}¡El equipo está completo! (4/4 jugadores)")
            player.playSound(player.location, org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            return
        }
        
        // Obtener el equipo actual del jugador
        val currentTeam = minigame.gameManager.getPlayerTeam(player)
        
        // Verificar si ya está en ese equipo
        if (currentTeam?.teamId == targetTeam.teamId) {
            player.sendMessage("${ChatColor.YELLOW}Ya estás en ${targetTeam.displayName}${ChatColor.YELLOW}!")
            return
        }
        
        // Remover del equipo actual si existe
        currentTeam?.removePlayer(player)
        
        // Añadir al nuevo equipo
        targetTeam.addPlayer(player)
        
        // Notificar al jugador
        player.sendMessage("${ChatColor.GREEN}¡Te has unido a ${targetTeam.displayName}${ChatColor.GREEN}!")
        player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f)
        
        // Actualizar la UI del inventario para todos los jugadores en el lobby
        minigame.gameManager.updateAllLobbyInventories(game)
        
        // Verificar si debe iniciar cuenta regresiva
        minigame.checkStartCountdown(game)
    }
    
    /**
     * Verifica si un material es un tipo de lana.
     */
    private fun isWoolMaterial(material: Material): Boolean {
        return material == Material.RED_WOOL ||
               material == Material.BLUE_WOOL ||
               material == Material.GREEN_WOOL ||
               material == Material.YELLOW_WOOL
    }
}
