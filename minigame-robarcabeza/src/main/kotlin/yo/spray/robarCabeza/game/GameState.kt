package yo.spray.robarCabeza.game

/**
 * Estados posibles de una partida de Robar Cabeza.
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
