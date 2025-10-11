# Plan Maestro Técnico: Implementación del Lobby y Selección de Equipos para "Cadena"

## 1. Visión General y Objetivos

Este documento detalla las especificaciones técnicas para la implementación de un sistema de lobby interactivo para el minijuego "Cadena". El objetivo es reemplazar el sistema actual de unión automática por una experiencia de usuario más intuitiva, donde los jugadores son teletransportados a una sala de espera y pueden seleccionar su equipo a través de una interfaz gráfica en su inventario.

## 2. Especificaciones de la Arquitectura

### 2.1. Flujo de Usuario

1.  El jugador ejecuta el comando `/cadena join`.
2.  El `GameManager` intercepta la solicitud, encuentra o crea una partida (`CadenaGame`) en estado `LOBBY`.
3.  El jugador es teletransportado a la ubicación del lobby de la arena, su inventario se limpia y se le pone en `GameMode.ADVENTURE`.
4.  Se le entrega en el inventario una serie de ítems (lanas de colores) que representan a los equipos disponibles.
5.  El jugador hace clic en una de las lanas para unirse al equipo correspondiente.
6.  La interfaz de inventario se actualiza para todos los jugadores en el lobby, reflejando el cambio en la composición de los equipos.
7.  Una vez que se cumplen las condiciones de inicio (ej. al menos dos equipos con dos jugadores), se inicia la cuenta atrás para comenzar la partida.

### 2.2. Modificaciones a Modelos de Datos

* **`game/Team.kt`**: La clase `Team` debe ser actualizada para incluir un identificador único y un nombre visible.

    ```kotlin
    // Propuesta de nueva estructura para Team.kt
    data class Team(
        val id: String, // "ROJO", "AZUL", etc.
        val displayName: String,
        val color: NamedTextColor,
        val material: Material,
        val players: MutableList<Player> = mutableListOf()
    )
    ```

* **`game/CadenaGame.kt`**: Debe inicializar una lista predefinida de equipos al ser creada.

### 2.3. Nuevos Componentes

* **`listeners/LobbyListener.kt`**: Un nuevo `Listener` que escuchará el evento `InventoryClickEvent`.
    * **Responsabilidad:**
        1.  Verificar si el jugador está en una partida de Cadena en estado `LOBBY`.
        2.  Cancelar el evento para evitar que el jugador mueva los ítems.
        3.  Identificar en qué equipo ha hecho clic el jugador basándose en el `Material` del ítem.
        4.  Validar si el equipo no está lleno.
        5.  Realizar el cambio de equipo del jugador.
        6.  Invocar la actualización de la UI para todos los jugadores en el lobby.

### 2.4. Modificaciones a Servicios Existentes

* **`services/GameManager.kt`**: Será modificado significativamente.
    * **`addPlayer(player)`:** Implementará el nuevo flujo de teletransportación al lobby y preparación del inventario.
    * **Nuevas Funciones:** Se crearán funciones de utilidad para generar y actualizar la UI del inventario (`giveTeamSelectionItems`, `updateAllLobbyInventories`).
    * **Condición de Inicio:** La lógica que inicia la cuenta atrás de la partida deberá ser modificada para comprobar si hay al menos dos equipos con un mínimo de dos jugadores cada uno.