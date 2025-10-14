package los5fantasticos.memorias

import org.bukkit.Location

/**
 * Representa una parcela de juego individual dentro de una arena.
 * Cada parcela puede albergar un duelo entre dos jugadores.
 * 
 * @property regionTablero Región donde se genera el tablero de juego
 * @property spawn1 Punto de aparición del jugador 1
 * @property spawn2 Punto de aparición del jugador 2
 */
data class Parcela(
    val regionTablero: Cuboid,
    val spawn1: Location,
    val spawn2: Location
) {
    /**
     * Obtiene el centro del tablero para generar el grid.
     */
    fun getCentroTablero(): Location {
        return regionTablero.getCenter()
    }
    
    /**
     * Verifica si una ubicación pertenece al tablero de esta parcela.
     */
    fun isInTablero(location: Location): Boolean {
        return regionTablero.contains(location)
    }
}
