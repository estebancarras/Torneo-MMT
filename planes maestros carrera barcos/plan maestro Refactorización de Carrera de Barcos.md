Refactorización de "Carrera de Barcos"
Título: Plan Maestro de Arquitectura: Transformación de "Carrera de Barcos" de Prototipo a Módulo Configurable

ID de Característica: CARRERABARCOS-REFACTOR-01-ARCHITECTURE

Autor: Arquitecto de Plugins Kotlin

1. Filosofía de la Refactorización: Estandarización y Modularidad
El minijuego "Carrera de Barcos" actualmente existe como un prototipo funcional, o "Prueba de Concepto". Su lógica de juego principal (checkpoints, meta, puntuación) está validada, pero su implementación monolítica y con valores "hardcodeados" (quemados en el código) lo hace inflexible y viola los principios de diseño que hemos establecido como estándar en el proyecto TorneoMMT.

La filosofía de esta refactorización no es reinventar, sino estandarizar. Aplicaremos la arquitectura de servicios desacoplada y el sistema de gestión de arenas que ha demostrado ser un éxito rotundo en nuestros módulos más maduros, como "Memorias" y "Laberinto". El objetivo es transformar "Carrera de Barcos" en un módulo robusto, completamente configurable por los administradores y fácil de mantener, integrándolo plenamente en nuestro ecosistema.

2. Análisis del Estado Actual: El Prototipo Monolítico
Puntos Fuertes Existentes:

Lógica de Juego Central Probada: El sistema ya contiene la mecánica esencial de una carrera: sabe cómo registrar el paso de un jugador por una secuencia de checkpoints y cómo determinar un ganador.

Integración con el Core Funcional: El módulo ya se comunica correctamente con el TorneoManager para la asignación de puntos.

Debilidades Críticas a Resolver:

Falta de Sistema de Arenas: No existe una abstracción para una "arena" o "circuito". Todas las coordenadas (spawns, checkpoints, meta) están quemadas en el código, limitando el juego a un único mapa.

Violación del Principio de Responsabilidad Única (SRP): La lógica de juego, los comandos de jugador y la gestión de estado están mezclados, principalmente dentro de la clase MinigameCarrerabarcos, lo que dificulta enormemente la lectura y la expansión del código.

Ausencia de Persistencia: Como consecuencia de la falta de un sistema de arenas, no hay mecanismo para guardar o cargar configuraciones de circuitos.

3. Arquitectura de la Solución: La "Memorización" del Módulo
La estrategia es clara y directa: vamos a refactorizar "Carrera de Barcos" aplicando la misma arquitectura de alto rendimiento que diseñamos para "Memorias".

Paso 1: Implementar el Modelo de Datos y la Persistencia

ArenaCarrera.kt (Nueva Data Class): Se creará una clase de datos para representar un circuito. Será la "ficha técnica" de cada carrera.

Propiedades: nombre: String, lobby: Location?, spawns: MutableList<Location>, checkpoints: MutableList<Cuboid> (una lista ordenada de regiones), meta: Cuboid?.

ArenaManager.kt (Nuevo Servicio): Se creará un gestor dedicado a la persistencia.

Responsabilidades: Cargar y guardar una MutableList<ArenaCarrera> en un archivo dedicado (carrerabarcos_arenas.yml). Este servicio será el único que interactúe con el disco.

Paso 2: Abstraer la Lógica de Juego

GameManager.kt (Nuevo Servicio): Se extraerá toda la lógica de gestión de partidas activas del CarreraCommand a este nuevo servicio.

Responsabilidades: Mantener una lista de carreras en curso, gestionar los jugadores dentro de cada carrera y su progreso (en qué checkpoint están), y manejar el inicio y fin de las carreras.

Rendimiento: Implementará un Game Loop Centralizado (una única BukkitTask) para actualizar el estado de las carreras de forma eficiente, si es necesario, o dependerá de eventos.

Carrera.kt (Nueva Clase): Similar a DueloMemorias, esta clase representará una instancia activa de una carrera en una arena específica, conteniendo los jugadores y su estado de progreso.

Paso 3: Crear un Sistema de Detección por Eventos

GameListener.kt (Nuevo Listener): Para un rendimiento óptimo, la detección del paso por checkpoints y la meta se basará en eventos.

Responsabilidades: Escuchará el PlayerMoveEvent.

Lógica Eficiente: Para cada movimiento, verificará si el jugador está en una carrera. Si lo está, comprobará si la nueva posición del jugador (event.to) está dentro de la región (Cuboid) de su próximo checkpoint o de la meta. Si es así, notificará al GameManager para que actualice el progreso del jugador.

Paso 4: Reconstruir los Comandos para la Administración

La clase MinigameCarrerabarcos.kt (o un nuevo CarreraCommand.kt) será refactorizada para convertirse en la interfaz de los administradores.

Responsabilidades:

Implementar un sistema de "varita de selección" (reutilizando la arquitectura del SelectionManager de "Memorias") para definir las regiones de los checkpoints y la meta.

Proveer subcomandos claros y robustos:

/carrera creararena <nombre>

/carrera setlobby <arena>

/carrera addspawn <arena>

/carrera addcheckpoint <arena> (usará la selección de la varita)

/carrera setmeta <arena> (usará la selección de la varita)

Comandos de gestión: list, remove, etc.

4. Criterios de Aceptación
Al finalizar esta refactorización, el módulo minigame-carrerabarcos deberá:

Permitir a los administradores crear, configurar y guardar múltiples circuitos de carrera.

Tener su lógica de juego completamente desacoplada de los comandos.

Utilizar un sistema de eventos eficiente para la detección de checkpoints y metas.

Estar alineado con la arquitectura de servicios y la calidad de código de los módulos más avanzados del proyecto.