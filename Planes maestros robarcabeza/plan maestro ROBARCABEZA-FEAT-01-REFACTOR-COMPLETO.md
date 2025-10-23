Minijuego "Robar la Cabeza"
ID de Característica: ROBARCABEZA-FEAT-01-REFACTOR-COMPLETO

Autor: Arquitecto de Plugins Kotlin (revisado por Gemini)

Fecha: 23 de octubre de 2025

1. Filosofía de Diseño: De "Atrapa la Bandera" a "Rey de la Colina" Dinámico
El estado actual del juego es un prototipo funcional, pero su mecánica de "el último que la tiene, gana" no es ideal para un torneo competitivo. Este plan maestro transformará "Robar la Cabeza" en un juego de puntuación acumulativa, donde el objetivo no es solo tener la cabeza al final, sino retenerla el mayor tiempo posible para acumular puntos.

Abandonaremos la estructura monolítica actual en favor del patrón de diseño modular (GameManager, ScoreService, etc.) que ha demostrado ser exitoso en el resto del proyecto. El resultado será un minijuego más justo, estratégico y alineado con la calidad de TorneoMMT.

2. Concepto del Juego y Mecánicas Clave
Objetivo: Acumular la mayor cantidad de puntos posible dentro de un tiempo límite (ej. 5 minutos).

Inicio de Partida: Al iniciar, se asignan aleatoriamente 2 "cabezas" entre los jugadores.

Puntuación:

Los jugadores con la cabeza ganan puntos pasivamente por cada segundo que la retienen.

Robar una cabeza a otro jugador otorga un bonus de puntos instantáneo.

Ganador: Al finalizar el tiempo, los jugadores con los 3 puntajes más altos son declarados ganadores.

3. Arquitectura de la Solución (La Gran Refactorización)
El paso más importante es reestructurar el módulo minigame-robarcola para que siga el patrón de tus otros minijuegos.

Paso 1: Crear la Nueva Estructura de Archivos Crea los siguientes archivos nuevos dentro del módulo. La lógica de RobarColaManager.kt será migrada a estas nuevas clases.

game/Partida.kt: Una clase de datos que representa el estado de una única partida.

Kotlin

data class Partida(
    val id: UUID = UUID.randomUUID(),
    val jugadores: MutableSet<UUID>,
    var jugadoresConCabeza: MutableSet<UUID> = mutableSetOf(),
    var estado: GameState = GameState.INICIANDO
)
services/GameManager.kt: El nuevo orquestador. Gestionará la partida activa, los temporizadores y la lógica principal del juego.

services/ScoreService.kt: Gestionará toda la lógica de puntuación.

services/ArenaManager.kt: Se encargará de guardar y cargar las ubicaciones de la arena en un archivo robarcabeza.yml.

RobarCabezaManager.kt: La clase principal (MinigameModule) será renombrada y simplificada. Su única responsabilidad será inicializar los nuevos servicios.

Paso 2: Migrar la Lógica Existente Mueve las funcionalidades de RobarColaManager.kt a sus nuevas ubicaciones:

A GameManager.kt:

La lógica de startGame(), endGame(), y resetGame().

El GameTimer principal de la partida.

La gestión de playersInGame y playersWithHead (ahora dentro de la clase Partida).

La lógica de createTailDisplay y cleanupAllTails.

A ArenaManager.kt:

La lógica de setGameSpawn, setLobbySpawn y loadSpawnFromConfig.

A GameListener.kt (nuevo archivo):

Los EventHandler para PlayerQuitEvent, PlayerInteractEvent y EntityDamageByEntityEvent que actualmente están en RobarColaManager.

4. Plan de Trabajo por Fases (Milestones)
Este es el camino recomendado para implementar los cambios de forma ordenada.

Fase 1: La Refactorización Arquitectónica

Objetivo: Reestructurar el código sin añadir nuevas funcionalidades.

Tareas:

Crea la nueva estructura de archivos (GameManager, ScoreService, etc.).

Mueve la lógica existente de RobarColaManager.kt a las nuevas clases.

Renombra el módulo y las clases de "RobarCola" a "RobarCabeza".

Verificación: Al final de esta fase, el juego debe funcionar exactamente como antes, pero con el código organizado en la nueva arquitectura.

Fase 2: Implementación del Sistema de Puntuación Dinámica

Objetivo: Introducir la nueva mecánica de puntuación.

Tareas:

Crear robarcabeza.yml: Externaliza todos los valores de puntos (puntos/segundo, bonus por robo, bonus por racha, bonus finales) a este archivo.

Implementar ScoreService.kt: Crea los métodos para otorgar puntos (awardPointsForHolding, awardPointsForSteal, etc.), asegurándote de que todos llamen a torneoManager.addScore().

Crear el "Ticker" de Puntos: En GameManager.kt, implementa una BukkitTask (runTaskTimer) que se ejecute cada 20 ticks (1 segundo). Esta tarea llamará a scoreService.awardPointsForHolding() para cada jugador que tenga la cabeza.

Integrar Puntos por Robo: En el GameListener, cuando se detecte un robo exitoso, llama a scoreService.awardPointsForSteal().

Fase 3: Refinamiento de Mecánicas de Juego

Objetivo: Mejorar la experiencia de juego y añadir los detalles finales.

Tareas:

Múltiples Cabezas Iniciales: Modifica GameManager.startGame() para que seleccione 2 o 3 jugadores al azar para darles la cabeza al inicio.

Cooldown de Invulnerabilidad: Implementa un Map<UUID, Long> en el GameManager. Cuando un jugador roba una cabeza, guarda su UUID y el tiempo actual. En el GameListener, antes de procesar un robo, verifica si el portador está en el cooldown.

Efectos Visuales:

Cambia el ItemDisplay a Material.PLAYER_HEAD.

Aplica el efecto de GLOW al jugador que recibe la cabeza y quítaselo cuando la pierde.

Final de Partida Mejorado: En GameManager.endGame(), obtén el ranking final de la partida desde el TorneoManager (usando los puntos de este juego), anuncia a los 3 ganadores en el chat y teletransporta a todos los jugadores al lobby principal con TournamentFlowManager.returnToLobby(player).

5. Criterios de Aceptación (Checklist Final)
El minijuego estará completo cuando:

[ ] El código esté completamente refactorizado siguiendo la arquitectura del proyecto.

[ ] La partida comience asignando 2-3 cabezas aleatoriamente.

[ ] Los jugadores con cabeza ganen puntos por cada segundo.

[ ] Robar una cabeza otorgue un bonus de puntos.

[ ] El portador de la cabeza tenga un efecto de GLOW y un PLAYER_HEAD flotante.

[ ] Exista un cooldown de invulnerabilidad después de robar.

[ ] Al finalizar el tiempo, se declaren los 3 ganadores y todos regresen al lobby global.

[ ] Todos los valores (puntos, tiempos, etc.) sean configurables en un archivo .yml.