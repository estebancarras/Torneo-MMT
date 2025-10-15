package los5fantasticos.memorias

/**
 * Estados posibles de un duelo de Memorias.
 * 
 * El flujo de estados es:
 * MEMORIZANDO -> JUGANDO -> FINALIZADO
 * 
 * @author Los 5 Fantásticos
 * @since 1.0
 */
enum class DueloEstado {
    /**
     * Fase inicial donde todos los bloques están visibles.
     * Los jugadores tienen tiempo para memorizar las posiciones.
     */
    MEMORIZANDO,
    
    /**
     * Fase de juego activo donde los jugadores se turnan
     * para encontrar pares de bloques.
     */
    JUGANDO,
    
    /**
     * Estado final cuando el duelo ha terminado.
     * No se procesan más acciones de juego.
     */
    FINALIZADO
}
