package yo.spray.robarCola.services

/**
 * Configuración de puntuación para el minijuego RobarCola.
 * 
 * Centraliza todos los valores de puntos otorgados en el juego,
 * facilitando su modificación y mantenimiento.
 * 
 * Sistema de puntuación dinámica:
 * - Los jugadores ganan puntos por cada segundo que retienen la cola
 * - Bonus por robar la cola a otro jugador
 * - Bonus final para los top 3 jugadores
 */
object RobarColaScoreConfig {
    /**
     * Puntos otorgados por segundo al retener la cola.
     */
    const val POINTS_PER_SECOND = 1
    
    /**
     * Puntos bonus por robar una cola exitosamente.
     */
    const val POINTS_STEAL_BONUS = 5
    
    /**
     * Puntos bonus para el primer lugar al final del juego.
     */
    const val POINTS_FIRST_PLACE = 20
    
    /**
     * Puntos bonus para el segundo lugar al final del juego.
     */
    const val POINTS_SECOND_PLACE = 10
    
    /**
     * Puntos bonus para el tercer lugar al final del juego.
     */
    const val POINTS_THIRD_PLACE = 5
    
    /**
     * Puntos otorgados por participar en la partida.
     */
    const val POINTS_PARTICIPATION = 2
    
    /**
     * Cooldown de invulnerabilidad después de robar (en segundos).
     */
    const val INVULNERABILITY_COOLDOWN_SECONDS = 5
    
    /**
     * Número de cabezas iniciales a asignar al inicio del juego.
     */
    const val INITIAL_HEADS_COUNT = 2
}
