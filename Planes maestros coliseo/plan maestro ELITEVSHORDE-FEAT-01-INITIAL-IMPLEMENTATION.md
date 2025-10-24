Implementación Integral de "La Élite contra la Horda"
ID de Característica: ELITEVSHORDE-FEAT-01-INITIAL-IMPLEMENTATION

Autor: Arquitecto de Plugins Kotlin

Fecha: 24 de octubre de 2025

1. Resumen Ejecutivo
Este documento detalla la arquitectura y el diseño para la creación de un nuevo minijuego de equipo asimétrico llamado "La Élite contra la Horda". El concepto central es un modo de supervivencia donde un pequeño equipo de los mejores jugadores del torneo (La Élite) debe resistir el asalto de un equipo mucho más grande compuesto por el resto de los participantes (La Horda).

La implementación se destacará por su sistema de balanceo dinámico, que ajusta el tamaño de los equipos en función del número total de jugadores. Los equipos recibirán kits de equipamiento completamente asimétricos y configurables, diseñados para fomentar un estilo de juego de "asedio contra bastión". Además, se incluirá una mecánica de construcción táctica que permitirá a los jugadores modificar el terreno del coliseo en tiempo real. Este minijuego será un módulo independiente dentro del ecosistema TorneoMMT, aprovechando su arquitectura de servicios existente.

2. Filosofía de Diseño
Tensión Asimétrica: La diversión principal nace del desequilibrio fundamental: la habilidad y el poder concentrados de la Élite contra la fuerza bruta y el número de la Horda. Cada equipo debe explotar sus ventajas únicas para ganar.

Inteligencia Dinámica: El juego es consciente del estado del torneo. Al seleccionar a los mejores jugadores del ranking para la Élite y al ajustar el tamaño de los equipos en cada partida, cada enfrentamiento será único y un reflejo fiel de la competición.

Entorno Táctico: El coliseo no es solo un escenario, es un recurso. La capacidad de colocar y destruir bloques transforma el combate en una guerra de posiciones, donde el control del terreno es tan importante como la puntería.

3. Arquitectura de la Solución
La implementación se realizará en un nuevo módulo Maven (minigame-elitevshorde) y seguirá una estructura de servicios por fases.

Fase 1: Creación del Módulo y Servicios Base
Crear el Módulo Maven: minigame-elitevshorde.

Clase Principal: EliteVsHordeModule.kt, que implementará MinigameModule.

Crear los Skeletons de Servicios: Se crearán las clases vacías para los servicios principales en el paquete services:

GameManager: Orquestador del ciclo de vida del juego.

TeamManager: Gestionará las listas de jugadores de cada equipo.

KitService: Responsable de la creación y entrega de los kits.

ArenaManager: Gestionará la creación y persistencia de las arenas.

ScoreService: Calculará y asignará los puntos.

Fase 2: Implementación del Balanceo y Formación de Equipos
Configuración en elitevshorde.yml: Se añadirá una sección para el balanceo.

YAML

team-balance:
  percentage: 0.25
  min-elite-players: 3
  max-elite-players: 10
Lógica en GameManager.kt:

Al iniciar la partida, el GameManager contará a los participantes (totalPlayers).

Calculará el eliteTeamSize usando la fórmula: clamp(round(totalPlayers * percentage), min-elite-players, max-elite-players).

Consultará torneoManager.getTopScores() para obtener el ranking.

Iterará sobre los participantes y los asignará al TeamManager en eliteTeam o hordeTeam según su ranking.

Fase 3: Implementación del Sistema de Kits Asimétricos
Configuración en elitevshorde.yml: Se añadirá una sección kits detallada.

YAML

kits:
  elite:
    armor: "DIAMOND:PROTECTION_ENVIRONMENTAL:2" # Tipo:Encantamiento:Nivel
    sword: "DIAMOND:DAMAGE_ALL:2"
    shield: true
    items:
      - "GOLDEN_APPLE:1"
      - "SPLASH_POTION,HEAL:1" # Tipo,Efecto:Cantidad
      - "CYAN_TERRACOTTA:16"
    effects:
      - "SLOW:0" # Lentitud I
  horde:
    armor: "IRON"
    sword: "IRON"
    crossbow: "CROSSBOW:PIERCING:1"
    items:
      - "ARROW:16"
      - "COOKED_BEEF:8"
      - "LAVA_BUCKET:1"
      - "WHITE_WOOL:32"
Lógica en KitService.kt:

Tendrá métodos para parsear esta configuración compleja, incluyendo encantamientos y efectos de poción.

Creará los métodos applyEliteKit(player) y applyHordeKit(player) que equipan a los jugadores con los ítems generados.

Fase 4: Desarrollo del Motor de Juego y Ciclo de Vida
GameManager.kt:

Gestionará un GameTimer para la duración de la partida.

Controlará el estado del juego: WAITING, STARTING, IN_GAME, FINISHED.

Verificará constantemente las condiciones de victoria: teamManager.getElitePlayers().isEmpty() o gameTimer.isFinished().

GameListener.kt:

PlayerDeathEvent:

Si el muerto es de la Élite, se le pone en modo espectador y se notifica al TeamManager.

Si es de la Horda, se le hace respawn en uno de los horde-spawns después de un breve delay.

PlayerQuitEvent: Si un jugador de la Élite se desconecta, se considera una eliminación.

Fase 5: Sistema de Arenas y Construcción
Arena.kt (Modelo de Datos):

name: String, eliteSpawns: MutableList<Location>, hordeSpawns: MutableList<Location>, playRegion: Cuboid.

ArenaManager.kt:

Modelado a partir de los ArenaManager existentes, con comandos para create, delete, addelitespawn, addhordespawn y setregion.

Lógica de Construcción:

Se registrará un Set<Block> en la clase del juego (EliteVsHordeGame).

BlockPlaceEvent: Si el bloque es uno de los permitidos por los kits, se añade a este Set.

BlockBreakEvent: Solo se permite romper bloques que estén en este Set.

En GameManager.endGame(): Se itera sobre el Set y se revierte cada bloque a Material.AIR para limpiar la arena automáticamente.

4. Criterios de Aceptación
✅ Se puede crear y configurar arenas con spawns diferenciados para Élite y Horda.

✅ Al iniciar la partida, los equipos se forman y balancean automáticamente según las reglas definidas.

✅ Los jugadores reciben sus kits completos y correctos, incluyendo armas, armaduras, ítems de utilidad y bloques.

✅ La Horda puede reaparecer; la Élite no.

✅ Los jugadores pueden colocar y romper los bloques de sus kits, y estos desaparecen al final de la partida.

✅ El juego finaliza correctamente cuando la Élite es eliminada o cuando el tiempo se agota.

✅ Se asignan los puntos correspondientes a los ganadores y participantes.