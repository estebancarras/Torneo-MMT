package los5fantasticos.minigameLaberinto.game

/**
 * Estados posibles de una partida de Laberinto.
 * 
 * Define el ciclo de vida de una partida desde el lobby hasta la finalización.
 */
enum class GameState {
    /**
     * Estado inicial donde los jugadores se unen a la partida.
     * Se espera que se cumplan las condiciones mínimas para iniciar.
     */
    LOBBY,
    
    /**
     * Estado de cuenta regresiva antes de iniciar la partida.
     * Los jugadores son teletransportados al punto de inicio.
     */
    COUNTDOWN,
    
    /**
     * Estado activo del juego donde los jugadores compiten.
     * Se pueden activar jumpscares y los jugadores pueden finalizar.
     */
    IN_GAME,
    
    /**
     * Estado final donde se calculan puntuaciones y se muestran resultados.
     * Los jugadores son teletransportados de vuelta al lobby.
     */
    FINISHED
}
