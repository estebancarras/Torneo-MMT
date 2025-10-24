package los5fantasticos.minigameColiseo.game

/**
 * Estados posibles de una partida de Coliseo.
 */
enum class GameState {
    /** Esperando jugadores en el lobby */
    WAITING,
    
    /** Cuenta regresiva antes de iniciar */
    STARTING,
    
    /** Partida en curso */
    IN_GAME,
    
    /** Partida finalizada */
    FINISHED
}
