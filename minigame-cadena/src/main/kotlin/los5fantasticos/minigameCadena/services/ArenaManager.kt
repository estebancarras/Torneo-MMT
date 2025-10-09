package los5fantasticos.minigameCadena.services

import los5fantasticos.minigameCadena.game.Arena
import org.bukkit.Location
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestiona las arenas del minijuego Cadena.
 * 
 * Responsabilidades:
 * - Crear y eliminar arenas
 * - Almacenar configuración de arenas
 * - Proporcionar arenas disponibles para partidas
 * - Gestionar arena en edición
 */
class ArenaManager {
    
    /**
     * Mapa de arenas por nombre.
     */
    private val arenas = ConcurrentHashMap<String, Arena>()
    
    /**
     * Arena que está siendo editada actualmente.
     * Key: Nombre del jugador editor
     */
    private val editingArenas = ConcurrentHashMap<String, Arena>()
    
    /**
     * Crea una nueva arena en modo edición.
     * 
     * @param name Nombre de la arena
     * @param spawnLocation Ubicación inicial del spawn
     * @return Arena creada
     */
    fun createArena(name: String, spawnLocation: Location): Arena {
        val arena = Arena(
            name = name,
            spawnLocation = spawnLocation.clone(),
            finishLocation = spawnLocation.clone(), // Temporal, se configurará después
            minHeight = spawnLocation.y - 10.0
        )
        
        return arena
    }
    
    /**
     * Guarda una arena en el manager.
     * 
     * @param arena Arena a guardar
     */
    fun saveArena(arena: Arena) {
        arenas[arena.name] = arena
    }
    
    /**
     * Elimina una arena.
     * 
     * @param name Nombre de la arena
     * @return true si se eliminó, false si no existía
     */
    fun deleteArena(name: String): Boolean {
        return arenas.remove(name) != null
    }
    
    /**
     * Obtiene una arena por nombre.
     * 
     * @param name Nombre de la arena
     * @return Arena o null si no existe
     */
    fun getArena(name: String): Arena? {
        return arenas[name]
    }
    
    /**
     * Obtiene todas las arenas.
     */
    fun getAllArenas(): List<Arena> {
        return arenas.values.toList()
    }
    
    /**
     * Obtiene una arena aleatoria disponible.
     */
    fun getRandomArena(): Arena? {
        return arenas.values.randomOrNull()
    }
    
    /**
     * Verifica si existe una arena.
     */
    fun arenaExists(name: String): Boolean {
        return arenas.containsKey(name)
    }
    
    /**
     * Inicia la edición de una arena para un jugador.
     * 
     * @param playerName Nombre del jugador
     * @param arena Arena a editar
     */
    fun startEditing(playerName: String, arena: Arena) {
        editingArenas[playerName] = arena
    }
    
    /**
     * Finaliza la edición de una arena para un jugador.
     * 
     * @param playerName Nombre del jugador
     */
    fun stopEditing(playerName: String) {
        editingArenas.remove(playerName)
    }
    
    /**
     * Obtiene la arena que está editando un jugador.
     * 
     * @param playerName Nombre del jugador
     * @return Arena en edición o null
     */
    fun getEditingArena(playerName: String): Arena? {
        return editingArenas[playerName]
    }
    
    /**
     * Verifica si un jugador está editando una arena.
     */
    fun isEditing(playerName: String): Boolean {
        return editingArenas.containsKey(playerName)
    }
    
    /**
     * Obtiene el número de arenas configuradas.
     */
    fun getArenaCount(): Int {
        return arenas.size
    }
    
    /**
     * Limpia todas las arenas (para reinicio).
     */
    fun clearAll() {
        arenas.clear()
        editingArenas.clear()
    }
}
