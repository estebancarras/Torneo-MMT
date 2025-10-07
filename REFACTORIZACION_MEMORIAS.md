# Refactorización del Minijuego Memorias

## Resumen de Cambios

Se ha refactorizado completamente el minijuego "Memorias" para implementar la mecánica clásica de "encontrar pares" en lugar del sistema anterior de "replicar patrones".

## Problemas Resueltos

### 1. Mecánica de Juego Incorrecta
**Antes**: El juego generaba un patrón aleatorio que los jugadores debían replicar colocando bloques.

**Ahora**: Juego de memoria clásico donde los jugadores hacen clic en bloques ocultos para encontrar pares de colores.

### 2. Sin Condición de Finalización
**Antes**: Las rondas continuaban infinitamente sin un límite claro.

**Ahora**: El juego termina automáticamente cuando todos los pares han sido encontrados.

### 3. Sistema de Turnos Inexistente
**Antes**: No había rotación de turnos entre jugadores.

**Ahora**: Sistema de turnos implementado que rota automáticamente entre jugadores.

## Nueva Mecánica de Juego

### Tablero
- Cuadrícula de 4x4 (16 bloques)
- 8 pares de colores diferentes
- Todos los bloques comienzan ocultos (lana gris)

### Interacción
- Los jugadores hacen **clic derecho** en bloques para revelarlos
- Primer clic: Revela el primer bloque
- Segundo clic: Revela el segundo bloque

### Lógica de Pares
- **Si coinciden**: Los bloques permanecen revelados, el jugador gana 10 puntos y puede seguir jugando
- **Si no coinciden**: Después de 2 segundos, ambos bloques vuelven a ocultarse y el turno pasa al siguiente jugador

### Condición de Victoria
- El juego termina cuando todos los pares han sido encontrados
- El jugador con más puntos gana
- El ganador recibe 50 puntos de bonificación en el TorneoManager
- Todos los jugadores reciben puntos de participación

## Archivos Modificados

### 1. Game.kt
**Cambios principales**:
- Nueva estructura de datos `BoardTile` para representar cada casilla del tablero
- Sistema de turnos con `currentPlayerIndex`
- Método `generateBoard()` que crea el tablero con pares aleatorios
- Método `handleBlockClick()` que procesa los clics de los jugadores
- Lógica de verificación de pares con `checkForMatch()`
- Método `endGameWithWinner()` que determina y anuncia al ganador

**Métodos nuevos**:
- `startGame()` - Inicia el juego generando el tablero
- `handleFirstClick()` - Maneja el primer clic del turno
- `handleSecondClick()` - Maneja el segundo clic del turno
- `checkForMatch()` - Verifica si los dos bloques forman un par
- `revealTile()` - Revela un bloque mostrando su color
- `hideTile()` - Oculta un bloque volviéndolo gris
- `getCurrentPlayer()` - Obtiene el jugador del turno actual
- `nextTurn()` - Pasa al siguiente turno
- `isGameComplete()` - Verifica si todos los pares fueron encontrados
- `endGameWithWinner()` - Finaliza el juego y anuncia al ganador

### 2. PlayerListener.kt
**Cambios principales**:
- Cambio de `BlockPlaceEvent` a `PlayerInteractEvent`
- Detección de clics derechos en bloques del tablero
- Método `isGameBlock()` para verificar si un material es parte del juego

**Antes**: Los jugadores colocaban bloques para replicar el patrón.

**Ahora**: Los jugadores hacen clic derecho en bloques para revelarlos.

### 3. GameManager.kt
**Cambios principales**:
- Cambio de `game.startRound()` a `game.startGame()`
- Límite de 4 jugadores por partida (antes era 2)
- Delay de 3 segundos antes de iniciar el juego cuando hay suficientes jugadores

### 4. MemoriasManager.kt
**Cambios principales**:
- Actualización de la descripción del minijuego
- Versión actualizada a 2.0

### 5. PlayerScore.kt (Recreado)
**Archivo recreado desde cero** con todos los métodos necesarios:
- `addPoints(minigame: String, points: Int)`
- `getPointsForMinigame(minigame: String): Int`
- `incrementGamesPlayed()`
- `incrementGamesWon()`
- `getWinRate(): Double`
- Alias `playerUUID` y `playerName` para compatibilidad

## Flujo del Juego

1. **Unirse**: Los jugadores ejecutan `/pattern join` o hacen clic en un cartel `[Pattern]`
2. **Espera**: El juego espera hasta tener al menos 2 jugadores
3. **Inicio**: Después de 3 segundos, el juego genera el tablero y comienza
4. **Turnos**: Los jugadores se turnan para hacer clic en bloques
5. **Pares**: Cuando se encuentra un par, el jugador gana 10 puntos y puede seguir
6. **Finalización**: Cuando todos los pares son encontrados, se anuncia al ganador
7. **Puntos**: El ganador recibe 50 puntos extra, todos reciben puntos de participación
8. **Limpieza**: Después de 5 segundos, el tablero se limpia y los jugadores vuelven al lobby

## Configuración de Arena

Los administradores pueden configurar la arena con un solo comando:

```bash
/memorias setarena           # Crea automáticamente el arena completa
/memorias size <3-15>        # [Opcional] Cambiar tamaño del tablero (por defecto 5x5)
```

**Nota**: El comando `/memorias setarena` configura automáticamente todas las ubicaciones necesarias basándose en tu posición actual. El tablero se genera 10 bloques al frente de donde estés parado.

## Colores Disponibles

El juego utiliza 8 colores diferentes de lana:
- Rojo (RED_WOOL)
- Azul (BLUE_WOOL)
- Verde (GREEN_WOOL)
- Amarillo (YELLOW_WOOL)
- Lima (LIME_WOOL)
- Naranja (ORANGE_WOOL)
- Rosa (PINK_WOOL)
- Púrpura (PURPLE_WOOL)

## Puntuación

- **Par encontrado**: 10 puntos
- **Bonificación de victoria**: 50 puntos (al ganador)
- **Puntos de participación**: Todos los puntos acumulados durante el juego

## Compilación Exitosa

```
[INFO] BUILD SUCCESS
[INFO] Total time: 02:15 min
[INFO] Finished at: 2025-10-01T13:05:34-03:00

Reactor Summary:
✓ Torneo Core .............. SUCCESS [01:09 min]
✓ Minigame - Robar Cola .... SUCCESS [ 21.234 s]
✓ Minigame - Memorias ...... SUCCESS [ 21.429 s]
✓ Torneo Assembly .......... SUCCESS [ 19.423 s]
```

## Próximos Pasos

1. Copiar el JAR al servidor:
   ```bash
   copy torneo-assembly\target\TorneoMMT-1.0-SNAPSHOT.jar MinecraftServer\plugins\
   ```

2. Configurar la arena con los comandos de administrador

3. Probar el juego con al menos 2 jugadores

4. Verificar que:
   - Los bloques se revelan correctamente al hacer clic derecho
   - Los turnos rotan entre jugadores
   - Los pares se detectan correctamente
   - El juego termina cuando todos los pares son encontrados
   - El ganador se anuncia correctamente
   - Los puntos se otorgan al TorneoManager

## Notas Técnicas

- El sistema usa `BukkitRunnable` para delays asíncronos (revelar/ocultar bloques)
- Los bloques se identifican por coordenadas exactas (blockX, blockY, blockZ)
- El estado del juego se mantiene en la instancia de `Game`
- La limpieza automática previene bloques huérfanos en el mundo

---

**Versión**: 2.0  
**Fecha**: 2025-10-01  
**Estado**: Completado y compilado exitosamente
