package yo.spray.robarCabeza.services

/**
 * Configuración de puntuación para el minijuego Robar Cabeza.
 * 
 * Centraliza todos los valores de puntos otorgados en el juego,
 * facilitando su modificación y mantenimiento.
 * 
 * Sistema de puntuación dinámica:
 * - Los jugadores ganan puntos por cada segundo que retienen la cabeza
 * - Bonus por robar la cabeza a otro jugador
 * - Bonus final para los top 3 jugadores
 */
object RobarCabezaScoreConfig {
    /**
     * Puntos otorgados por segundo al retener la cabeza.
     * Este valor puede ser configurado desde robarcabeza.yml.
     */
    var POINTS_PER_SECOND = 1
    
    /**
     * Puntos bonus por robar una cabeza exitosamente.
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
