Plan Maestro - Servicio de Scoreboard Global y Asignación de Puntos (Versión 2.0)
Título: Plan Maestro de Implementación: Servicio de Scoreboard Global y Estandarización de Puntuación

ID de Característica: CORE-FEAT-01-SCOREBOARD (Revisión 2)

Autor: Arquitecto de Plugins Kotlin

Fecha: 11 de octubre de 2025

Resumen Ejecutivo:
Este documento detalla el diseño técnico para un sistema integral de puntuación del torneo. Consta de dos partes críticas: (1) un servicio de scoreboard global para la visualización de datos en tiempo real y (2) un patrón de diseño estandarizado para la asignación de puntos que debe ser implementado por todos los módulos de minijuegos. El objetivo es crear un sistema cohesivo, robusto y fácil de mantener, donde el torneo-core gestiona la visualización y persistencia, y los minijuegos actúan como fuentes de datos consistentes.

1. Arquitectura y Ubicación (Scoreboard Service):

Módulo: La funcionalidad de visualización residirá completamente dentro del torneo-core.

Componente Principal: Se creará el servicio GlobalScoreboardService, único responsable de la gestión del scoreboard.

Dependencias: El servicio dependerá del TorneoManager para obtener los datos.

2. Diseño Técnico del Scoreboard:

2.1. Objeto Único: Se instanciará un único Scoreboard para ser mostrado a todos los jugadores.

2.2. Objetivo (Objective): Se registrará un Objective en el DisplaySlot.SIDEBAR con un título estilizado mediante la API Adventure.

2.3. Técnica Anti-Parpadeo (Teams): La actualización de las líneas se realizará modificando el prefijo y sufijo de Team invisibles pre-registrados, garantizando una actualización visual fluida.

3. Flujo de Datos y Ciclo de Vida (Scoreboard Service):

3.1. Inicialización: El GlobalScoreboardService se inicializa en el onEnable del TorneoPlugin.

3.2. Tarea de Actualización: Una tarea repetitiva (cada 4 segundos) consultará al TorneoManager por el top 10 y actualizará los Team del scoreboard.

3.3. Asignación: Un PlayerConnectionListener asignará el scoreboard a los jugadores al conectarse.

4. Estandarización de Asignación de Puntos (Patrón para Minijuegos):

4.1. Fundamento: Para garantizar la integridad de los datos que alimentan el scoreboard, todos los módulos de minijuegos (minigame-*) deben seguir un patrón estricto de asignación de puntos.

4.2. El Contrato: El método TorneoManager.addScore(playerUUID, points, reason) es el único punto de entrada autorizado para otorgar puntos en todo el proyecto.

4.3. Patrón de Implementación Obligatorio:

Inyección de Dependencia: La clase principal de cada minijuego recibirá la instancia del TorneoManager desde el torneo-core.

Servicio de Puntuación Dedicado: Cada módulo de minijuego debe implementar su propio servicio de puntuación (ej. SkywarsScoreService, CadenaScoreService). Esta clase encapsulará toda la lógica de negocio sobre cuándo y cuántos puntos se otorgan.

Configuración Externalizada: Los valores numéricos de los puntos (ej. puntos-por-victoria: 100, puntos-por-kill: 10) deben estar definidos en un archivo de configuración propio del minijuego (ej. skywars.yml), no hardcodeados.

Flujo de Responsabilidad: El GameManager del minijuego detecta un evento puntuable (victoria, kill, etc.), invoca al ScoreService local, y este último, tras leer la configuración, llama al torneoManager.addScore().