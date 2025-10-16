package los5fantasticos.memorias

import org.bukkit.Location

/**
 * Representa una parcela de juego individual dentro de una arena.
 * 
 * FILOSOFÍA DE DISEÑO [CONVENCIÓN SOBRE CONFIGURACIÓN]:
 * La parcela solo define la región del tablero. Los spawns de jugadores
 * y la disposición exacta del tablero se calculan AUTOMÁTICAMENTE en tiempo
 * de ejecución por el DueloMemorias.
 * 
 * @property region Región cúbica donde se generará el tablero de juego
 */
data class Parcela(
    val region: Cuboid
) {
    /**
     * Obtiene el centro geométrico (X, Z) de la región.
     * Usado para centrar el tablero de juego.
     */
    fun getCentroXZ(): Pair<Double, Double> {
        val center = region.getCenter()
        return Pair(center.x, center.z)
    }
    
    /**
     * Obtiene la coordenada Y mínima de la región.
     * Usado como nivel del suelo para el tablero.
     */
    fun getYSuelo(): Int {
        return region.minY
    }
    
    /**
     * Verifica si una ubicación pertenece a esta parcela.
     */
    fun contains(location: Location): Boolean {
        return region.contains(location)
    }
}
