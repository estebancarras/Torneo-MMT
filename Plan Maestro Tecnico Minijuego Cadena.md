Plan Maestro Técnico: Minijuego "Cadena"
Este documento detalla la arquitectura y el plan de implementación para el módulo del minijuego cooperativo "Cadena".

1. Requisitos
Funcionales: Coinciden con la descripción proporcionada: equipos de 2-4 jugadores, encadenamiento permanente, parkour cooperativo con reinicio en checkpoints, tiempo límite, puntuación por orden de llegada y un lobby de espera.

No Funcionales:

Rendimiento: La lógica del encadenamiento no debe impactar significativamente el rendimiento del servidor (TPS). Las operaciones deben ser eficientes.

Escalabilidad: El sistema debe poder gestionar múltiples partidas simultáneas en diferentes arenas sin conflictos de estado.

Configurabilidad: Todas las variables clave del juego (distancia de la cadena, puntos, tiempos) deben ser configurables a través de un archivo config.yml.

Mantenibilidad: El código debe ser limpio, bien documentado y seguir la arquitectura modular del proyecto TorneoMMT.

2. Diseño de la Arquitectura del Módulo
La estructura de paquetes sugerida es excelente y la adoptaremos:

/minigame-cadena
└── src/main/kotlin/los5fantasticos/minigameCadena/
    ├── commands/         # Comandos para administradores y jugadores.
    ├── listeners/        # Listeners para eventos de Bukkit (PlayerMoveEvent, PlayerQuitEvent).
    ├── game/             # Clases que gestionan el estado de una partida (CadenaGame, Team).
    ├── services/         # Lógica de negocio desacoplada (ChainService, ParkourService).
    ├── storage/          # Gestión de configuración y arenas (ArenaManager).
    └── MinigameCadena.kt # Clase principal del módulo, implementa MinigameModule.
3. Especificación Detallada de Mecánicas
Encadenamiento (ChainService):

Estrategia: Se creará un BukkitRunnable síncrono por cada partida en curso, que se ejecutará cada pocos ticks (ej. cada 2-3 ticks).

Física de Arrastre: No se usará teleport() para evitar una experiencia brusca (rubber-banding). En su lugar, si un jugador excede la maxDistance de su "centro de masa" del equipo, se le aplicará un vector de velocidad con player.setVelocity(). La fuerza de este vector será suave y configurable (pullStrength).

Centro de Masa: El punto de referencia para el arrastre será el promedio de las ubicaciones de todos los miembros del equipo, recalculado en cada ejecución de la tarea.

Casos Borde: Si un jugador está en el aire (saltando), la fuerza de arrastre debe ser menor para no interrumpir el parkour. Si un jugador se desconecta, la partida para su equipo se pausa o finaliza según la configuración.

Gestión de Partida (game/):

CadenaGame: Una clase que representa una instancia de una partida. Contendrá el estado (LOBBY, IN_GAME, FINISHED), la lista de equipos, el temporizador y la arena en la que se juega.

Team: Una clase de datos que agrupa a los jugadores de un equipo y almacena su progreso (último checkpoint, tiempo de finalización).

Arena: Clase de datos cargada desde la configuración que contiene todas las ubicaciones (lobby, spawn, checkpoints, meta).

Comandos y Configuración:

/cadena join: Añadirá al jugador a una partida en estado LOBBY.

/cadena admin createarena <nombre>, setlobby, setspawn, addcheckpoint, setfinish: Comandos para que los administradores definan las arenas de forma interactiva en el juego.

config.yml: Almacenará la configuración global del minijuego y una lista de todas las arenas creadas.

4. Plan de Trabajo por Milestones (Pull Requests)
PR1: Estructura y Comandos Básicos (Complejidad: Rápido)

Tareas: Crear la estructura de paquetes, la clase principal MinigameCadena, y los comandos /cadena join y /cadena admin (inicialmente, solo con mensajes de placeholder).

Verificación: Compilar y ver los comandos registrados en el juego.

PR2: Sistema de Lobby y Equipos (Complejidad: Media)

Tareas: Implementar un LobbyManager. El comando /cadena join ahora añade jugadores a una cola. Implementar la lógica para formar equipos y un temporizador para iniciar la partida.

Verificación: Múltiples jugadores pueden unirse, se forman equipos y la partida transiciona de LOBBY a IN_GAME.

PR3: Mecánica de Encadenamiento (Complejidad: Complejo)

Tareas: Crear el ChainService. Implementar el BukkitRunnable y la lógica de "arrastre" suave usando player.setVelocity().

Verificación: Iniciar una partida. Los jugadores deben ser visiblemente arrastrados entre sí si intentan separarse.

PR4: Lógica de Parkour (Caídas y Checkpoints) (Complejidad: Media)

Tareas: Implementar un ParkourService y la configuración de arenas en config.yml. Crear listeners para PlayerMoveEvent para detectar caídas y llegada a checkpoints. Implementar la lógica de reinicio del equipo.

Verificación: Si un jugador cae, todo el equipo es teletransportado al último checkpoint.

PR5: Lógica de Finalización y Puntuación (Complejidad: Media)

Tareas: Implementar el temporizador de la partida. Detectar cuando un equipo completo cruza la línea de meta. Al finalizar la partida, calcular el orden, asignar puntos a través del TorneoManager del torneo-core y mostrar un resumen.

Verificación: Los equipos completan el recorrido y los puntos se asignan correctamente en el ranking global.

PR6: Comandos de Administrador y Persistencia (Complejidad: Media)

Tareas: Implementar la funcionalidad completa de los subcomandos de /cadena admin para crear/gestionar arenas. Guardar las arenas creadas en un archivo arenas.yml.

Verificación: Se puede crear y guardar una arena funcional completamente desde el juego.

5. Tests y QA
Unit Tests (JUnit/Mockk): Es crucial testear la lógica matemática del ChainService (cálculo de vectores) y la asignación de puntos del ScoreService.

Playtesting Manual (Checklist):

¿El arrastre de la cadena se siente fluido o es frustrante?

¿Qué ocurre si un jugador se desconecta en medio de un salto?

¿La detección de checkpoints es precisa?

¿Se asignan los puntos correctamente al final de la partida a todos los miembros del equipo?