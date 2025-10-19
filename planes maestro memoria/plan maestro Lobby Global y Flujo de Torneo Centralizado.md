Lobby Global y Flujo de Torneo Centralizado
ID de Característica: CORE-FEAT-02-TOURNAMENT-FLOW

Autor: Arquitecto de Plugins Kotlin

Fecha: 17 de octubre de 2025

1. Resumen Ejecutivo
Este documento detalla la arquitectura para una refactorización mayor del proyecto "TorneoMMT", introduciendo un sistema de Lobby Global centralizado y un flujo de torneo controlado por administradores. Se abandonará el modelo actual donde los jugadores se unen a los minijuegos de forma individual. En su lugar, todos los participantes residirán en un mapa de lobby persistente y protegido (la réplica del Duoc UC). Los organizadores del torneo utilizarán comandos centralizados para iniciar cada minijuego, momento en el cual el sistema teletransportará a todos los jugadores a la actividad correspondiente y los devolverá al lobby al finalizar. Esta implementación mejorará drásticamente la experiencia de usuario, eliminará la confusión y otorgará a los administradores un control total sobre el ritmo y la estructura del evento.

2. Arquitectura de la Solución
La implementación se basa en la creación de nuevos servicios y componentes en el torneo-core para centralizar la gestión del flujo del evento y la protección del lobby, así como la adaptación de los módulos de minijuegos a esta nueva arquitectura.

2.1. Refactorización: Promoción de Herramientas a torneo-core

Para evitar la duplicación de código y seguir las buenas prácticas, las herramientas de selección de región, que son de utilidad general, serán movidas al módulo torneo-core.

Acción: Mover los archivos SelectionManager.kt y Cuboid.kt desde minigame-memorias/src/main/kotlin/los5fantasticos/memorias/ a un nuevo paquete en el core: torneo-core/src/main/kotlin/los5fantasticos/torneo/util/selection/.

Resultado: Estas clases se convierten en una utilidad compartida disponible para todo el proyecto (por ejemplo, para definir la región del lobby o futuras arenas que necesiten selecciones).

2.2. Nuevo Servicio: TournamentFlowManager.kt en torneo-core

Este será el nuevo "director de orquesta" del torneo.

Ubicación: torneo-core/src/main/kotlin/los5fantasticos/torneo/services/TournamentFlowManager.kt

Responsabilidades:

Estado del Torneo: Mantendrá el estado actual del torneo (ej. IN_LOBBY, IN_GAME) y una referencia al minijuego activo.

Gestión del Lobby Global:

Almacenará la región protegida: private var globalLobbyRegion: Cuboid? = null.

Almacenará la lista de puntos de aparición: private val globalLobbySpawns = mutableListOf<Location>().

Métodos de Orquestación:

startMinigame(minigameName: String): Reúne a todos los jugadores online, valida que el minijuego exista y llama a minigameModule.onTournamentStart(players).

returnToLobby(player: Player): Teletransporta a un jugador a uno de los globalLobbySpawns de forma aleatoria. Los minijuegos llamarán a esta función cuando un jugador termine su participación.

endCurrentMinigame(): Termina forzosamente el juego actual y devuelve a todos los jugadores al lobby.

Métodos de Configuración (para Admins): setLobbyRegion(cuboid: Cuboid), addLobbySpawn(location: Location), clearLobbySpawns().

2.3. Nuevo Listener: GlobalLobbyListener.kt en torneo-core

Este listener se encargará de proteger el lobby del Duoc UC.

Ubicación: torneo-core/src/main/kotlin/los5fantasticos/torneo/listeners/GlobalLobbyListener.kt

Lógica:

Escuchará los eventos BlockBreakEvent, BlockPlaceEvent.

Para cada evento, comprobará si el jugador está en el estado IN_LOBBY (a través del TournamentFlowManager).

Si el jugador está en el lobby, cancelará el evento para impedir la modificación del mapa.

Bypass de Admin: Los jugadores con el permiso torneo.admin.build podrán ignorar esta protección.

2.4. Expansión de la Interfaz MinigameModule.kt

Para que el core pueda "entregar" los jugadores a cada minijuego, la interfaz necesita un nuevo método.

Ubicación: torneo-core/src/main/kotlin/los5fantasticos/torneo/api/MinigameModule.kt

Nuevo Método:

Kotlin

/**
 * Llamado por el TournamentFlowManager para iniciar este minijuego.
 * A partir de este momento, el módulo toma control de los jugadores recibidos.
 * @param players La lista de todos los jugadores que participarán.
 */
fun onTournamentStart(players: List<Player>)
2.5. Nuevos Comandos Centralizados: TorneoAdminCommand.kt

Un nuevo comando para que los administradores gestionen todo el flujo del torneo.

Ubicación: torneo-core/src/main/kotlin/los5fantasticos/torneo/commands/TorneoAdminCommand.kt

Comando Principal: /torneo (se puede fusionar con el RankingCommand existente para unificar).

Subcomandos de Admin:

/torneo wand: Da al admin la varita de selección del SelectionManager.

/torneo setlobbyregion: Establece la región del lobby usando la selección de la varita.

/torneo addspawn: Añade la ubicación actual como un punto de aparición del lobby.

/torneo clearspawns: Borra todos los puntos de aparición del lobby.

/torneo start <minijuego>: Comando clave. Inicia el minijuego especificado para todos los jugadores. Tendrá autocompletado para los nombres de los juegos.

/torneo end: Comando de emergencia para terminar la partida actual y devolver a todos al lobby.

2.6. Adaptación de los Módulos de Minijuegos (Ej: MemoriasManager.kt)

Implementar onTournamentStart(players: List<Player>): La lógica que estaba en /memorias join y GameManager.joinPlayer (añadir a la lista de espera, teletransportar al lobby del minijuego) se moverá aquí.

Desactivar /join: El comando /memorias join se eliminará o se cambiará para mostrar un mensaje informativo ("Espera a que un administrador inicie el juego.").

Modificar Flujo de Finalización: Al terminar un duelo, el GameManager de "Memorias" ya no teletransportará a los jugadores a su propio lobby, sino que llamará a tournamentFlowManager.returnToLobby(player).

3. Criterios de Aceptación
✅ El SelectionManager y Cuboid han sido movidos exitosamente al torneo-core y el proyecto compila.

✅ Los administradores pueden definir una región de lobby protegida usando /torneo wand y /torneo setlobbyregion.

✅ Los jugadores no administradores no pueden romper bloques ni colocar bloques dentro de la región del lobby.

✅ Los administradores pueden añadir múltiples puntos de aparición en el lobby con /torneo addspawn.

✅ El comando /torneo start <minijuego> transfiere exitosamente a todos los jugadores del servidor al minijuego correspondiente.

✅ Cada módulo de minijuego implementa onTournamentStart y maneja correctamente la recepción de jugadores.

✅ Al finalizar una partida en un minijuego, los jugadores son teletransportados de vuelta a uno de los puntos de aparición aleatorios en el lobby global.

✅ Los comandos /join de los minijuegos individuales han sido desactivados o actualizados.

Este plan establece una base arquitectónica sólida y profesional para el torneo, mejorando la experiencia de todos los involucrados y facilitando la gestión del evento.