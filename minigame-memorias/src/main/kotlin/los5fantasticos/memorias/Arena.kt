package los5fantasticos.memorias

import org.bukkit.Location

/**
 * Representa una arena de Memorias que puede contener múltiples parcelas.
 * Cada parcela permite un duelo simultáneo entre dos jugadores.
 * 
 * @property nombre Nombre identificador de la arena
 * @property parcelas Lista de parcelas disponibles para duelos
 * @property lobbySpawn Punto de spawn para jugadores en espera
 */
data class Arena(
    val nombre: String,
    val parcelas: MutableList<Parcela> = mutableListOf(),
    var lobbySpawn: Location? = null
) {
    /**
     * Obtiene una parcela libre (sin duelo activo).
     * @return Parcela disponible o null si todas están ocupadas
     */
    fun getParcelaLibre(parcelasOcupadas: Set<Parcela>): Parcela? {
        return parcelas.firstOrNull { it !in parcelasOcupadas }
    }
    
    /**
     * Obtiene el número de parcelas disponibles.
     */
    fun getTotalParcelas(): Int = parcelas.size
    
    /**
     * Añade una nueva parcela a la arena.
     */
    fun addParcela(parcela: Parcela) {
        parcelas.add(parcela)
    }
    
    /**
     * Elimina una parcela de la arena.
     */
    fun removeParcela(index: Int): Boolean {
        return if (index in parcelas.indices) {
            parcelas.removeAt(index)
            true
        } else {
            false
        }
    }
}