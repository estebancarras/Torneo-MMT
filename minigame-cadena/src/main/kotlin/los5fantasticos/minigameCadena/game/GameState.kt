package los5fantasticos.minigameCadena.game

/**
 * Estados posibles de una partida de Cadena.
 */
enum class GameState {
    /**
     * Lobby de espera - Los jugadores se están uniendo y formando equipos.
     */
    LOBBY,
    
    /**
     * Cuenta atrás antes de iniciar - La partida está por comenzar.
     */
    COUNTDOWN,
    
    /**
     * Partida en progreso - Los equipos están completando el parkour.
     */
    IN_GAME,
    
    /**
     * Partida finalizada - Todos los equipos terminaron o se agotó el tiempo.
     */
    FINISHED
}
