Plan Maestro - Refactorización de Arquitectura de "Memorias"
Título: Plan Maestro (Fase 1): Refactorización Arquitectónica para Concurrencia en Minijuego "Memorias"

ID de Característica: MEMORIAS-REFACTOR-01-CONCURRENCIA

Autor: Arquitecto de Plugins Kotlin

Fecha: 14 de octubre de 2025

Resumen Ejecutivo:
Este documento define la primera fase de la reconstrucción del minijuego "Memorias". El objetivo de esta fase es exclusivamente refactorizar la arquitectura del núcleo del minijuego para que sea capaz de soportar una alta concurrencia (10-20 duelos simultáneos) de manera eficiente. Se abandonará el insostenible patrón de "una tarea por acción" en favor de un diseño profesional basado en un Game Loop Centralizado. La implementación de nuevas características de juego (turnos, temporizadores, etc.) queda explícitamente fuera del alcance de esta fase y se abordará posteriormente sobre la base sólida que aquí se construya.

1. Arquitectura de Destino:

El Orquestador (GameManager.kt): Será rediseñado para gestionar una colección de duelos y ejecutar el Game Loop principal. Su responsabilidad es la gestión macro, no la lógica de un duelo.

El Controlador de Duelo (DueloMemorias.kt): Una nueva clase que encapsulará el estado y la lógica de un único duelo 1v1. Será una entidad pasiva cuya lógica temporal es impulsada externamente por el Game Loop del orquestador.

El Modelo de Arena (Arena.kt, Parcela.kt): La estructura de datos de las arenas será modificada para soportar un modelo de "arena con múltiples parcelas", donde cada parcela es un espacio de juego para un duelo.

2. El Patrón "Game Loop" Centralizado:

El GameManager instanciará una única BukkitTask repetitiva.

Esta tarea iterará sobre todos los duelos activos y llamará a un método actualizar() en cada uno.

La clase DueloMemorias no debe crear ninguna BukkitTask. Su método actualizar() se convertirá en el corazón de su lógica, pero será invocado externamente.

3. Criterios de Aceptación para la Fase 1:

La estructura de clases (GameManager, DueloMemorias, Parcela, Arena) debe estar implementada según el nuevo diseño.

El GameManager debe ser capaz de iniciar un Game Loop, crear instancias de DueloMemorias y llamar a sus métodos actualizar() en cada tick del bucle.

La lógica de juego existente (hacer clic y revelar un bloque) debe ser migrada a la nueva estructura y seguir funcionando (aunque sin turnos ni temporizadores por ahora).

Los comandos de administración deben ser actualizados para permitir la creación de una arena y la definición de múltiples parcelas dentro de ella.