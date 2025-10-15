Plan Maestro (Fase 2): Implementación de Lógica de Duelo Competitivo

ID de Característica: MEMORIAS-FEAT-03-GAME-LOGIC

Autor: Arquitecto de Plugins Kotlin

Fecha: 14 de octubre de 2025

Resumen Ejecutivo:
Este documento detalla la implementación de las características de juego centrales para el minijuego "Memorias". Sobre la base de la arquitectura de Game Loop ya establecida, esta fase introducirá una máquina de estados para los duelos, un sistema de turnos, una fase inicial de memorización y temporizadores individuales para cada jugador. El objetivo es transformar el prototipo funcional en una experiencia de juego competitiva, trepidante y clara para los participantes, con un feedback visual constante que informe del estado de la partida.

1. Arquitectura de la Lógica de Juego:

Toda la nueva lógica residirá dentro de la clase DueloMemorias.kt, que actúa como el controlador para un único duelo. El GameManager seguirá siendo el orquestador que impulsa las actualizaciones a través del Game Loop, pero no contendrá lógica específica del juego.

Máquina de Estados del Duelo: Se introducirá un enum DueloEstado para gestionar el flujo de un duelo:

MEMORIZANDO: La fase inicial donde ambos jugadores ven el patrón.

JUGANDO: La fase principal donde los jugadores se turnan para encontrar pares.

FINALIZADO: El estado final una vez que se determina un ganador.

Sistema de Turnos:

DueloMemorias mantendrá una referencia al UUID del jugador cuyo turno está activo (turnoActual).

La lógica de interacción con los bloques (handlePlayerClick) deberá validar que el clic proviene del jugador correcto.

Un fallo al encontrar un par resultará en un cambio de turno.

Temporizadores Individuales:

Cada instancia de DueloMemorias contendrá dos objetos GameTimer (de nuestro torneo-core), uno por jugador.

Al cambiar de turno, el temporizador del jugador anterior se pausará y el del jugador actual se reanudará.

El GameTimer del jugador activo será actualizado en cada tick del Game Loop a través del método actualizar() de DueloMemorias.

Feedback Visual (Action Bar):

El método actualizar() será el responsable de enviar información persistente a la action bar de los jugadores, mostrando de quién es el turno y el tiempo restante de cada uno. Este método es el más eficiente en términos de rendimiento de red.

2. Flujo de un Duelo:

El GameManager crea una instancia de DueloMemorias. Su estado inicial es MEMORIZANDO.

El Game Loop llama a duelo.actualizar(). En el estado MEMORIZANDO, se muestran todos los bloques y un contador de tiempo interno decrementa.

Cuando el contador de memorización llega a cero, el estado cambia a JUGANDO. Los bloques se ocultan, se asigna el turno al player1 y su GameTimer se reanuda.

El Game Loop sigue llamando a duelo.actualizar(). En el estado JUGANDO, se actualiza el GameTimer del jugador activo y se envía el feedback a las action bars.

Si el jugador activo falla un par, se cambia el turno, se pausan/reanudan los temporizadores correspondientes.

El duelo termina cuando un jugador se queda sin tiempo o se encuentran todos los pares. El estado cambia a FINALIZADO.

3. Criterios de Aceptación:

Al iniciar un duelo, existe un período de tiempo configurable donde los jugadores pueden ver el patrón.

Los jugadores solo pueden interactuar con el tablero durante su turno.

Un fallo al encontrar un par cede el turno al oponente.

Cada jugador tiene un temporizador individual que solo avanza durante su turno.

Los jugadores reciben información visual constante en sus action bars sobre el estado del turno y los tiempos.

Todos los nuevos parámetros de juego (tiempo de memorización, tiempo por jugador) son configurables en un archivo memorias.yml.