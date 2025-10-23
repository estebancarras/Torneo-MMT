package yo.spray.robarCola.game

/**
 * Estados posibles de una partida de RobarCola.
 */
enum class GameState {
    /**
     * Esperando jugadores en el lobby.
     */
    LOBBY,
    
    /**
     * Cuenta atrás antes de iniciar.
     */
    COUNTDOWN,
    
    /**
     * Partida en curso.
     */
    IN_GAME,
    
    /**
     * Partida finalizada.
     */
    FINISHED
}
