Refactorización del Motor de Juego de "Carrera de Barcos"
ID de Característica: CARRERABARCOS-FEAT-03-GAME-ENGINE-REFACTOR

Autor: Arquitecto de Plugins Kotlin

Fecha: 20 de octubre de 2025

1. Resumen Ejecutivo
Este documento detalla una refactorización arquitectónica crítica para el minijuego "Carrera de Barcos". Las pruebas de juego han revelado problemas fundamentales en el núcleo de la lógica: las partidas se inician de forma individual en lugar de en un grupo unificado, el progreso de los jugadores (checkpoints y meta) no se detecta, y no hay feedback visual del temporizador. Este plan abordará estos problemas de raíz mediante la introducción de un GameManager centralizado, la implementación de un RaceListener para el seguimiento de jugadores y la correcta integración del temporizador visual, alineando el minijuego con los patrones de diseño robustos y probados del resto del proyecto.

2. Arquitectura de la Solución
La implementación se centrará en reestructurar la lógica de juego para que sea gestionada por un motor central (GameManager) y un sistema de seguimiento de jugadores activo (RaceListener).

2.1. Centralización de la Lógica en un GameManager

Actualmente, la lógica de inicio está dispersa y es incorrecta. La centralizaremos para seguir el patrón de nuestros otros módulos.

Nuevo Archivo: minigame-carrerabarcos/src/main/kotlin/los5fantasticos/minigameCarrerabarcos/services/GameManager.kt.

Responsabilidades:

Gestionará la carrera activa: private var activeRace: Race? = null.

Tendrá una función startRace(players: List<Player>, track: RaceTrack) que:

Cree una única instancia de una nueva clase Race (que contendrá a todos los jugadores y el estado de la carrera).

Teletransporte a todos los jugadores al spawn de la pista.

Inicie el temporizador de cuenta atrás y el temporizador de duración de la carrera, asegurándose de que la BossBar se muestre a todos los participantes.

Refactorización de MinigameCarrerabarcos.kt: La función onTournamentStart ya no creará carreras. Su única responsabilidad será obtener la pista activa y llamar a gameManager.startRace() con la lista de jugadores.

2.2. Implementación del RaceListener para el Seguimiento de Jugadores

Esta es la pieza más crítica que falta. Es el "motor" que hace que el juego funcione.

Nuevo Archivo: minigame-carrerabarcos/src/main/kotlin/los5fantasticos/minigameCarrerabarcos/listeners/RaceListener.kt.

Lógica:

Debe ser registrado en el onEnable del módulo.

onPlayerMove(event: PlayerMoveEvent):

Comprobará si el jugador (event.player) está participando en la activeRace del GameManager. Si no, no hará nada.

Si está en la carrera, obtendrá el estado de ese jugador (su próximo checkpoint objetivo).

Comprobará si la nueva ubicación del jugador (event.to) está dentro de la región de su checkpoint objetivo.

Si lo está: Actualizará el progreso del jugador al siguiente checkpoint, le enviará un feedback visual/sonoro (ej. ActionBar con "Checkpoint 2/5") y le asignará el siguiente checkpoint como objetivo.

Comprobará si la nueva ubicación está dentro de la región de la finishline y si el jugador ha pasado por todos los checkpoints. Si es así, registrará su tiempo de finalización, determinará su posición y lo marcará como "finalizado".

2.3. Creación de una Clase RacePlayer

Para gestionar el estado individual de cada corredor de forma limpia, crearemos una clase contenedora.

Nuevo Archivo: minigame-carrerabarcos/src/main/kotlin/los5fantasticos/minigameCarrerabarcos/game/RacePlayer.kt.

Propiedades:

Kotlin

data class RacePlayer(
    val player: Player,
    var nextCheckpointIndex: Int = 0,
    var finishTime: Long? = null,
    var finalPosition: Int? = null
)
Uso: El GameManager y el RaceListener usarán esta clase para saber exactamente en qué punto de la carrera se encuentra cada jugador.

2.4. Implementación del Temporizador de Carrera (Visual)

Para mantener el ritmo del torneo y la experiencia de usuario.

Acción: En el GameManager, al iniciar la carrera, se crearán dos instancias de nuestro GameTimer del torneo-core.

CountdownTimer (5 segundos): Se mostrará a los jugadores antes de empezar. Su onFinish callback desbloqueará el movimiento de los barcos.

RaceDurationTimer (configurable, ej. 5 minutos): Se iniciará justo después del countdown. Su BossBar mostrará el "Tiempo Restante". Su onFinish callback forzará el fin de la carrera, asignando posiciones basadas en el último checkpoint alcanzado.

Feedback: Es crucial que la lógica para addPlayer a la BossBar del GameTimer se use correctamente para que todos los corredores vean el temporizador.

3. Criterios de Aceptación
✅ Al usar /torneo start carrerabarcos, se crea una única carrera para todos los jugadores.

✅ Todos los jugadores son teletransportados al spawn de la pista activa configurada.

✅ Se muestra una cuenta atrás de inicio en la BossBar a todos los jugadores.

✅ Durante la carrera, se muestra un temporizador de tiempo restante en la BossBar.

✅ Cuando un jugador atraviesa un checkpoint en el orden correcto, su progreso se actualiza y recibe un feedback.

✅ Cuando un jugador cruza la línea de meta después de todos los checkpoints, la partida termina para él y se registra su posición.

✅ La partida finaliza para todos cuando el temporizador de duración llega a cero o cuando un número definido de jugadores ha terminado.

✅ Al finalizar, los jugadores son devueltos correctamente al lobby global.