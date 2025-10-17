Sistema de Inicio de Ronda para "Memorias"
ID de Característica: MEMORIAS-FEAT-04-ROUND-SYSTEM

Autor: Arquitecto de Plugins Kotlin

Fecha: 17 de octubre de 2025

1. Resumen Ejecutivo
Este documento detalla la arquitectura para implementar un sistema de gestión de rondas de torneo para el minijuego "Memorias", optimizado para un evento presencial y simultáneo. Se abandonará el emparejamiento continuo en favor de un flujo controlado por el administrador. Los jugadores se registrarán en una sala de espera (staging), y un administrador iniciará el emparejamiento aleatorio y el comienzo simultáneo de todos los duelos.

Críticamente, el sistema validará que haya un número par de jugadores antes de iniciar la ronda, notificando al administrador si es necesario añadir un "comodín" para garantizar que todos los estudiantes puedan jugar. El objetivo es un inicio de ronda justo, ordenado y eficiente, sin dejar a nadie fuera.

2. Arquitectura de la Solución
La implementación se centrará en añadir una nueva lógica de "sala de espera" al GameManager y en expandir los comandos de administrador con validaciones robustas.

2.1. Refactorización del GameManager.kt

El GameManager pasará a gestionar una lista de jugadores en espera, en lugar de emparejarlos al instante.

Nueva Propiedad: jugadoresEnLobby

Se añadirá una nueva lista para mantener a los jugadores en la fase de preparación:

Kotlin

private val jugadoresEnLobby = mutableListOf<Player>()
Refactorización de joinPlayer(jugador: Player):

Su única responsabilidad será añadir al jugador a la lista jugadoresEnLobby.

Teletransportará al jugador al lobbySpawn de la arena activa.

Enviará un mensaje claro de bienvenida: "Te has unido al lobby de Memorias. Esperando a que un administrador inicie la ronda."

Iniciará la LobbyFeedbackTask si es el primer jugador en unirse.

Nueva Función: startRound():

Será la función principal que orquestará el inicio de la ronda.

Paso A (Validación Crítica):

Comprobará si el número de jugadores en jugadoresEnLobby es par. Si es impar, la función abortará y devolverá un mensaje de error específico (ej. "Número impar de jugadores (29). Se requiere un comodín.").

Comprobará que el número de parcelas disponibles sea suficiente (arena.getTotalParcelas() >= jugadoresEnLobby.size / 2). Si no, abortará con otro mensaje de error.

Paso B (Sorteo):

Creará una copia barajada de jugadoresEnLobby (shuffled()).

Paso C (Creación de Duelos):

Iterará sobre la lista barajada en pares (usando chunked(2)).

Para cada par, buscará una parcela libre y llamará a la función crearDuelo().

Paso D (Limpieza):

Vaciará la lista jugadoresEnLobby después de asignar a todos los jugadores.

Nuevo Componente: LobbyFeedbackTask

Se creará una BukkitTask privada en el GameManager.

Se iniciará cuando jugadoresEnLobby no esté vacía y se detendrá cuando se vacíe.

Cada segundo, enviará una ActionBar a todos los jugadores en la lista con el texto: Jugadores Listos: X.

2.2. Modificaciones en MemoriasCommand.kt

Nuevo Subcomando de Administrador:

Se añadirá la ruta admin startround al comando /memorias.

Este comando debe tener una guarda de permisos (memorias.admin o isOp).

Llamará a gameManager.startRound() y manejará los posibles mensajes de error, notificando al administrador de manera clara si la ronda no pudo iniciarse por un número impar de jugadores o por falta de parcelas.

3. Flujo Detallado (Caso Impar)
29 Jugadores: Ejecutan /memorias join y son añadidos a jugadoresEnLobby. Ven en su ActionBar: Jugadores Listos: 29.

Administrador: Ejecuta /memorias admin startround.

Sistema (GameManager):

startRound() detecta jugadoresEnLobby.size % 2 != 0.

La función retorna un error.

Sistema (MemoriasCommand):

Recibe el error y envía un mensaje al administrador:

[Torneo] Error: Hay un número impar de jugadores (29). Para iniciar la ronda, un organizador o 'comodín' debe unirse al lobby con /memorias join.

Organizador ("Comodín"): Ejecuta /memorias join. El contador en la ActionBar de todos sube a 30.

Administrador: Vuelve a ejecutar /memorias admin startround.

Sistema (GameManager):

La validación de número par ahora es exitosa.

Procede a barajar y emparejar a los 30 jugadores, iniciando 15 duelos simultáneamente.

4. Criterios de Aceptación
✅ Al usar /memorias join, el jugador es añadido a una sala de espera y no se le empareja.

✅ Los jugadores en espera reciben una ActionBar periódica con el conteo de jugadores.

✅ El comando /memorias admin startround falla y notifica al administrador si el número de jugadores en espera es impar.

✅ El comando /memorias admin startround falla y notifica al administrador si no hay suficientes parcelas.

✅ Al ejecutar startround con un número par de jugadores y suficientes parcelas, todos los jugadores son emparejados aleatoriamente y sus duelos comienzan.

✅ La lista de espera (jugadoresEnLobby) se vacía después de un inicio de ronda exitoso.