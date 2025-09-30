# TorneoMMT

Sistema centralizado de gestión de minijuegos para servidores Minecraft con ranking global unificado.

## Descripción

TorneoMMT es un plugin modular para servidores Paper que integra múltiples minijuegos bajo una plataforma única, manteniendo un sistema de puntuación global que rastrea el desempeño de los jugadores a través de todas las actividades del torneo.

## Características

- **Arquitectura Modular**: Sistema extensible que permite agregar nuevos minijuegos sin modificar el código base
- **Ranking Global Unificado**: Seguimiento centralizado de puntuaciones entre todos los minijuegos
- **Persistencia Automática**: Almacenamiento automático de datos en formato YAML
- **Sistema de Comandos Unificado**: Todos los comandos registrados en un único punto de entrada
- **Estadísticas Detalladas**: Seguimiento de partidas jugadas, ganadas y ratio de victoria por jugador

## Minijuegos Incluidos

### Robar la Cola
Minijuego de persecución donde los jugadores compiten por mantener la "cola" el mayor tiempo posible. El jugador con la cola acumula puntos mientras la retiene.

**Comandos:**
- `/givetail <jugador>` - Otorgar cola a un jugador
- `/setgamespawn` - Configurar punto de aparición en juego
- `/setlobby` - Configurar punto de aparición del lobby
- `/startgame` - Iniciar partida manualmente
- `/stopgame` - Detener partida en curso

### Memorias
Juego de memoria visual donde los jugadores deben observar y replicar patrones de bloques en una cuadrícula 5x5. La dificultad aumenta progresivamente con cada ronda.

**Comandos:**
- `/pattern join` - Unirse a la cola de espera
- `/pattern admin setlobby` - Configurar lobby
- `/pattern admin setspawn` - Configurar spawn de arena
- `/pattern admin setpattern` - Configurar área de visualización de patrón
- `/pattern admin setguessarea` - Configurar área de replicación
- `/pattern admin createarena` - Crear arena con las ubicaciones configuradas

## Requisitos

- **Servidor**: Paper 1.21 o superior
- **Java**: JDK 17 o superior
- **Maven**: 3.6+ (para compilación)

## Instalación

### Opción 1: Usar el JAR Precompilado

1. Descargar `TorneoMMT-1.0-SNAPSHOT.jar` de la carpeta `torneo-assembly/target/`
2. Copiar el archivo a la carpeta `plugins/` del servidor
3. Reiniciar el servidor

### Opción 2: Compilar desde el Código Fuente

```bash
# Clonar el repositorio
git clone https://github.com/estebancarras/Torneo-MMT.git
cd TorneoMMT

# Compilar el proyecto
mvn clean install -DskipTests

# El JAR estará en torneo-assembly/target/TorneoMMT-1.0-SNAPSHOT.jar
```

## Configuración

Al iniciar por primera vez, el plugin creará la carpeta `plugins/TorneoMMT/` con los archivos de configuración necesarios:

- `scores.yml` - Almacenamiento de puntuaciones de jugadores
- `config.yml` - Configuración general del plugin (si aplica)

Para configurar cada minijuego, consulte la [Guía del Administrador](GUIA_ADMINISTRADOR.md).

## Comandos del Sistema

### Ranking Global

- `/ranking` - Mostrar top 10 jugadores del ranking global
- `/ranking top <N>` - Mostrar top N jugadores
- `/ranking <minijuego>` - Mostrar ranking específico de un minijuego
  - Minijuegos disponibles: `robarcola`, `memorias`

## Arquitectura del Proyecto

```
TorneoMMT/
├── torneo-core/              # Plugin principal (JavaPlugin)
│   ├── api/                  # Interfaces para extensión
│   │   ├── MinigameModule.kt
│   │   └── PlayerScore.kt
│   ├── core/
│   │   └── TorneoManager.kt  # Gestor de puntuaciones
│   ├── commands/
│   │   └── RankingCommand.kt
│   └── TorneoPlugin.kt       # Clase principal del plugin
│
├── minigame-robarcola/       # Módulo: Robar la Cola
│   └── RobarColaManager.kt
│
├── minigame-memorias/        # Módulo: Memorias
│   ├── MemoriasManager.kt
│   ├── GameManager.kt
│   └── Game.kt
│
└── torneo-assembly/          # Empaquetado final
    └── pom.xml               # Genera el JAR unificado
```

## Desarrollo

### Añadir un Nuevo Minijuego

1. **Crear un nuevo módulo Maven** en la raíz del proyecto
2. **Implementar la interfaz `MinigameModule`**:
   ```kotlin
   class MiMinijuegoManager : MinigameModule {
       override fun onEnable(plugin: TorneoPlugin) {
           // Lógica de inicialización
       }
       
       override fun onDisable() {
           // Lógica de limpieza
       }
       
       override fun getName(): String = "MiMinijuego"
       override fun getVersion(): String = "1.0"
   }
   ```
3. **Registrar comandos** en `torneo-core/src/main/resources/plugin.yml`
4. **Agregar dependencia** en `torneo-assembly/pom.xml`
5. **Compilar** con `mvn clean install`

### Otorgar Puntos a Jugadores

```kotlin
// Desde cualquier módulo de minijuego
val torneoManager = plugin.torneoManager
torneoManager.addPoints(player.uniqueId, 100, "mi_minijuego")
```

## Documentación Adicional

- [Guía del Administrador](GUIA_ADMINISTRADOR.md) - Configuración detallada de minijuegos
- [Resumen de Refactorización](REFACTORIZACION_RESUMEN.md) - Detalles técnicos de la arquitectura
- [Limpieza Completada](LIMPIEZA_COMPLETADA.md) - Historial de cambios estructurales

## Solución de Problemas

### El plugin no carga
- Verificar que está usando Paper 1.21+
- Confirmar que Java 17+ está instalado
- Revisar logs del servidor para errores específicos

### Los comandos no responden
- Verificar permisos de operador
- Confirmar que el plugin está habilitado: `/plugins`

### Los puntos no se guardan
- Verificar permisos de escritura en `plugins/TorneoMMT/`
- Revisar que `scores.yml` no esté corrupto
- Consultar logs para errores de I/O

## Contribuir

Las contribuciones son bienvenidas. Por favor:

1. Fork el repositorio
2. Crear una rama para tu feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit tus cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Crear un Pull Request

## Licencia

Este proyecto es de código abierto y está disponible bajo la licencia MIT.

## Autores

- **Equipo Los 5 Fantásticos** - Desarrollo inicial
- **Spray** - Módulo RobarCola

## Soporte

Para reportar bugs o solicitar nuevas funcionalidades, por favor abrir un issue en el repositorio de GitHub.

---

**Versión Actual**: 1.0-SNAPSHOT  
**Última Actualización**: 2025-09-29
