package los5fantasticos.minigameCadena.services

import los5fantasticos.minigameCadena.MinigameCadena
import los5fantasticos.minigameCadena.game.Team
import los5fantasticos.minigameCadena.visuals.VisualChain
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Servicio centralizado para gestionar el ciclo de vida de las cadenas visuales.
 * 
 * Este servicio actúa como orquestador, asegurando que:
 * - No se creen cadenas duplicadas entre los mismos jugadores
 * - Las cadenas se limpien correctamente cuando un jugador se desconecta
 * - Todos los recursos visuales se liberen al finalizar una partida
 * 
 * @property plugin Instancia del plugin
 */
class ChainVisualizerService(private val plugin: MinigameCadena) {
    
    /**
     * Mapa de cadenas activas, indexadas por pares de UUIDs.
     * La clave es un par ordenado (menor UUID, mayor UUID) para evitar duplicados.
     */
    private val activeChains = mutableMapOf<Pair<UUID, UUID>, VisualChain>()
    
    /**
     * Crea una clave canónica para un par de jugadores.
     * La clave siempre tiene el UUID menor primero para evitar duplicados
     * (A->B y B->A serían la misma cadena).
     * 
     * @param uuidA UUID del primer jugador
     * @param uuidB UUID del segundo jugador
     * @return Par ordenado de UUIDs
     */
    private fun getCanonicalKey(uuidA: UUID, uuidB: UUID): Pair<UUID, UUID> {
        return if (uuidA.compareTo(uuidB) < 0) {
            Pair(uuidA, uuidB)
        } else {
            Pair(uuidB, uuidA)
        }
    }
    
    /**
     * Crea todas las cadenas visuales necesarias para un equipo.
     * 
     * Cada jugador está conectado visualmente con todos los demás
     * miembros de su equipo, formando una red completa.
     * 
     * @param team Equipo para el cual crear las cadenas
     */
    fun createChainsForTeam(team: Team) {
        val players = team.getOnlinePlayers()
        
        // Se necesitan al menos 2 jugadores para crear una cadena
        if (players.size < 2) {
            return
        }
        
        // Crear cadenas entre todos los pares únicos de jugadores
        for (i in 0 until players.size) {
            for (j in i + 1 until players.size) {
                val playerA = players[i]
                val playerB = players[j]
                
                // Crear la clave canónica
                val key = getCanonicalKey(playerA.uniqueId, playerB.uniqueId)
                
                // Solo crear si no existe ya
                if (!activeChains.containsKey(key)) {
                    val chain = VisualChain(plugin, playerA, playerB)
                    chain.create()
                    activeChains[key] = chain
                }
            }
        }
    }
    
    /**
     * Destruye todas las cadenas visuales asociadas a un jugador.
     * 
     * Se llama cuando un jugador se desconecta o es eliminado de la partida.
     * 
     * @param player Jugador cuyas cadenas deben destruirse
     */
    fun destroyChainsForPlayer(player: Player) {
        // Iterar sobre una copia de las claves para evitar ConcurrentModificationException
        val keysToRemove = activeChains.keys.filter { key ->
            key.first == player.uniqueId || key.second == player.uniqueId
        }
        
        // Destruir y remover cada cadena asociada
        keysToRemove.forEach { key ->
            activeChains[key]?.destroy()
            activeChains.remove(key)
        }
    }
    
    /**
     * Limpia todas las cadenas visuales activas.
     * 
     * Se llama al finalizar una partida o al desactivar el módulo.
     */
    fun clearAllChains() {
        // Destruir todas las cadenas
        activeChains.values.forEach { chain ->
            chain.destroy()
        }
        
        // Limpiar el mapa
        activeChains.clear()
    }
    
    /**
     * Obtiene el número de cadenas visuales activas.
     * Útil para debugging y monitoreo.
     * 
     * @return Cantidad de cadenas activas
     */
    fun getActiveChainCount(): Int {
        return activeChains.size
    }
}
