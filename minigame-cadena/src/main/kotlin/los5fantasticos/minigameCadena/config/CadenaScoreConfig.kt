package los5fantasticos.minigameCadena.config

/**
 * Configuración de puntuación para el minijuego Cadena.
 * 
 * Define los puntos otorgados por diferentes logros en el juego.
 * Estos valores pueden ser modificados mediante el archivo config.yml del plugin.
 */
object CadenaScoreConfig {
    
    /**
     * Puntos por completar el parkour (victoria de equipo).
     */
    const val POINTS_VICTORY = 100
    
    /**
     * Puntos por alcanzar un checkpoint.
     */
    const val POINTS_CHECKPOINT = 5
    
    /**
     * Puntos por ser el primer equipo en terminar.
     */
    const val POINTS_FIRST_PLACE = 50
    
    /**
     * Puntos por ser el segundo equipo en terminar.
     */
    const val POINTS_SECOND_PLACE = 30
    
    /**
     * Puntos por ser el tercer equipo en terminar.
     */
    const val POINTS_THIRD_PLACE = 15
    
    /**
     * Puntos por participar en una partida (independiente del resultado).
     */
    const val POINTS_PARTICIPATION = 10
}
