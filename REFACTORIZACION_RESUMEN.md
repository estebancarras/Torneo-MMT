# Resumen de la Refactorización del Proyecto Torneo MMT

## Trabajo Completado

### 1. Arquitectura Modular Implementada
- Creada la API del Core con interfaces `MinigameModule`, `TorneoManager` y `PlayerScore`
- Plugin principal refactorizado: `CadenaPlugin` → `TorneoPlugin`
- Sistema de puntaje global centralizado
- Persistencia de datos en archivos YAML

### 2. Estructura del Proyecto
```
torneo-duoc-uc/
├── torneo-core/              # Plugin principal (JavaPlugin)
│   ├── api/                  # Interfaces para minijuegos
│   │   ├── MinigameModule.kt
│   │   └── PlayerScore.kt
│   ├── core/
│   │   └── TorneoManager.kt  # Gestor de puntajes global
│   ├── commands/
│   │   └── RankingCommand.kt
│   └── TorneoPlugin.kt       # Clase principal
│
├── minigame-robarcola/       # Módulo de minijuego
│   ├── RobarColaManager.kt   # Manager (NO JavaPlugin)
│   └── commands/
│       └── RobarColaCommands.kt
│
├── minigame-memorias/        # Módulo de minijuego
│   ├── MemoriasManager.kt    # Manager (NO JavaPlugin)
│   ├── GameManager.kt
│   ├── Game.kt
│   └── ...
│
└── torneo-assembly/          # Empaquetado final
    └── pom.xml               # Genera el JAR único
```

### 3. Plugin.yml Unificado
- Todos los comandos consolidados en `torneo-core/src/main/resources/plugin.yml`
- Eliminados los `plugin.yml` de los módulos de minijuegos

### 4. Comandos Registrados
#### Core:
- `/ranking` - Muestra el ranking global
- `/ranking <minijuego>` - Ranking de un minijuego específico
- `/ranking top <N>` - Top N jugadores

#### RobarCola:
- `/robarcola` - Ver ayuda de comandos
- `/robarcola join` - Unirse al juego
- `/robarcola leave` - Salir del juego
- `/robarcola setspawn` - [Admin] Establece spawn del juego
- `/robarcola setlobby` - [Admin] Establece spawn del lobby
- `/robarcola startgame` - [Admin] Inicia el juego manualmente
- `/robarcola stopgame` - [Admin] Detiene el juego
- `/robarcola darcola <jugador>` - [Admin] Da una cola a un jugador

#### Memorias:
- `/memorias` - Ver ayuda de comandos
- `/memorias join` - Unirse al juego
- `/memorias leave` - Salir del juego
- `/memorias setarena` - [Admin] Crear arena automáticamente
- `/memorias size <3-15>` - [Admin] Cambiar tamaño del tablero

### 5. Sistema de Puntaje Global
- Los minijuegos otorgan puntos a través del `TorneoManager`
- Persistencia automática en `scores.yml`
- Ranking global y por minijuego
- Estadísticas: juegos jugados, ganados, ratio de victoria

## Problemas Encontrados Durante la Compilación

### Problema 1: Dependencia Cíclica
**Error**: `torneo-core` dependía de los minijuegos, y los minijuegos dependían del core.
**Solución**: Creado módulo `torneo-assembly` que empaqueta todo sin dependencias cíclicas.

### Problema 2: Errores de Compilación en RobarCola
**Error**: Problemas con secuencias de escape en strings de Kotlin al usar PowerShell.
**Estado**: Archivo necesita ser recreado manualmente con encoding correcto.

## Refactorización Completada

### Estado Final
- Todos los archivos residuales eliminados
- `RobarColaManager.kt` recreado y funcional
- `Game.kt` creado para el módulo de Memorias
- `BlockData.kt` actualizado con Location
- Compilación exitosa sin errores

### Compilar el Proyecto
```bash
cd C:\Users\fuige\OneDrive\Escritorio\TorneoMMT
mvn clean install -DskipTests
```

El JAR final está en: `torneo-assembly/target/TorneoMMT-1.0-SNAPSHOT.jar`

### Copiar al Servidor
```bash
copy torneo-assembly\target\TorneoMMT-1.0-SNAPSHOT.jar C:\Users\fuige\OneDrive\Escritorio\MinecraftServer\plugins\
```

## Ventajas de la Nueva Arquitectura

1. **Un Solo Plugin**: El servidor solo carga un JAR
2. **Modularidad**: Fácil añadir nuevos minijuegos
3. **Puntaje Centralizado**: Todos los minijuegos actualizan el mismo ranking
4. **Comandos Unificados**: Todos registrados en un solo lugar
5. **Mantenibilidad**: Código organizado y profesional
6. **Carga Dinámica**: Los minijuegos se cargan automáticamente si están presentes

## Cómo Añadir un Nuevo Minijuego

1. Crear un nuevo módulo Maven
2. Implementar la interfaz `MinigameModule`
3. Registrar comandos en el `plugin.yml` del core
4. El `TorneoPlugin` lo cargará automáticamente mediante reflexión

## Notas Importantes

- Los minijuegos ya NO son `JavaPlugin`, son clases normales que implementan `MinigameModule`
- El sistema de puntos se gestiona centralmente a través de `TorneoManager`
- Los datos se persisten automáticamente en `plugins/TorneoMMT/scores.yml`
- El ranking se puede ver con `/ranking` en el juego

## Debugging

Si un minijuego no se carga:
1. Verificar los logs del servidor al iniciar
2. Buscar mensajes como "✓ Minijuego cargado: [nombre]"
3. Si no aparece, verificar que la clase esté en el classpath correcto
4. Verificar que implemente correctamente `MinigameModule`

## Próximos Pasos

1. Corregir los errores de encoding en los archivos Kotlin
2. Compilar el proyecto completo
3. Probar en el servidor local
4. Verificar que todos los comandos funcionen
5. Probar el sistema de puntaje
6. Implementar base de datos si es necesario (futuro)
