# Guía del Administrador - TorneoMMT

## 1. Introducción

TorneoMMT es un sistema centralizado de gestión de minijuegos para servidores Minecraft basados en Paper. El plugin integra múltiples minijuegos bajo una única plataforma, manteniendo un ranking global unificado que registra el desempeño de los jugadores a través de todas las actividades del torneo.

El sistema está diseñado con una arquitectura modular que permite la gestión independiente de cada minijuego mientras mantiene la coherencia en el seguimiento de puntuaciones y la experiencia del usuario.

## 2. Instalación

### Requisitos Previos
- Servidor Minecraft con Paper 1.21 o superior
- Java 17 o superior

### Procedimiento de Instalación

1. **Detener el servidor**  
   Asegúrese de que el servidor esté completamente apagado antes de proceder con la instalación.

2. **Copiar el archivo del plugin**  
   Localice el archivo `TorneoMMT-1.0-SNAPSHOT.jar` en el directorio `torneo-assembly/target/` del proyecto compilado y cópielo a la carpeta `plugins/` de su servidor.

3. **Iniciar el servidor**  
   Al iniciar el servidor, el plugin se cargará automáticamente y creará la estructura de directorios necesaria en `plugins/TorneoMMT/`, incluyendo los archivos de configuración y almacenamiento de datos.

4. **Verificar la instalación**  
   Revise los logs del servidor para confirmar que el plugin se ha cargado correctamente. Debería ver mensajes indicando la carga exitosa de cada módulo de minijuego.

## 3. Configuración de Minijuegos

### 3.1. Minijuego: Robar la Cola

#### Descripción
Robar la Cola es un minijuego de persecución donde un jugador posee una "cola" que otros participantes deben intentar robar mediante combate cuerpo a cuerpo. El jugador que mantenga la cola durante más tiempo acumula puntos para el ranking del torneo.

#### Comandos de Configuración

**`/setlobby`**  
Establece el punto de aparición para los jugadores antes y después de cada partida. Este es el área donde los jugadores esperarán entre rondas.

**`/setgamespawn`**  
Define el punto de aparición inicial de los jugadores al comenzar una partida. Todos los participantes aparecerán en esta ubicación cuando el juego inicie.

#### Guía de Setup Rápido

1. Diríjase a la ubicación deseada para el área de espera (lobby) del minijuego.
2. Ejecute el comando `/setlobby` para establecer ese punto como lobby.
3. Navegue al centro de la arena donde se desarrollará el juego.
4. Ejecute el comando `/setgamespawn` para definir el punto de aparición durante las partidas.
5. El minijuego está ahora configurado y listo para su uso.

### 3.2. Minijuego: Memorias

#### Descripción
Memorias es un minijuego de memoria visual donde los jugadores deben observar y replicar patrones de bloques en una cuadrícula 5x5. El juego presenta rondas progresivamente más difíciles, otorgando puntos según el desempeño de cada jugador.

#### Comandos de Configuración

Todos los comandos de configuración de Memorias utilizan el prefijo `/pattern admin` seguido del subcomando correspondiente:

**`/pattern admin setlobby`**  
Establece el lobby general del minijuego donde los jugadores esperarán antes de que comience una partida.

**`/pattern admin setspawn`**  
Define el punto de aparición de los jugadores dentro de la arena de juego. Los participantes aparecerán en esta ubicación al iniciar cada ronda.

**`/pattern admin setpattern`**  
Define la esquina inferior de la cuadrícula 5x5 donde se mostrará el patrón que los jugadores deben memorizar. Esta área debe estar claramente visible para todos los participantes.

**`/pattern admin setguessarea`**  
Define la esquina inferior de la cuadrícula 5x5 donde los jugadores deben replicar el patrón observado. Esta área debe ser accesible para que los jugadores puedan interactuar con los bloques.

**`/pattern admin createarena`**  
Guarda todas las ubicaciones configuradas previamente y crea la arena como una entidad jugable. Este comando debe ejecutarse después de haber definido las cuatro ubicaciones anteriores.

#### Guía de Setup Rápido

1. Construya o seleccione un área adecuada para el minijuego, asegurándose de tener espacio para dos cuadrículas 5x5 separadas.
2. Posiciónese en el lobby deseado y ejecute `/pattern admin setlobby`.
3. Vaya al punto donde los jugadores deben aparecer durante el juego y ejecute `/pattern admin setspawn`.
4. Diríjase a la esquina inferior de la primera cuadrícula 5x5 (área de visualización del patrón) y ejecute `/pattern admin setpattern`.
5. Navegue a la esquina inferior de la segunda cuadrícula 5x5 (área de replicación) y ejecute `/pattern admin setguessarea`.
6. Finalice la configuración ejecutando `/pattern admin createarena`.
7. La arena está ahora lista para recibir jugadores.

## 4. Gestión de Partidas y Jugadores

### Comandos de RobarCola

**`/startgame`**  
Inicia una partida de Robar la Cola manualmente. Útil para comenzar el juego en momentos específicos sin esperar la cola automática de jugadores.

**`/stopgame`**  
Fuerza la detención inmediata de una partida en curso. Este comando debe usarse en situaciones de emergencia o cuando sea necesario interrumpir el juego por razones administrativas.

**`/givetail <jugador>`**  
Otorga la "cola" a un jugador específico durante una partida activa. Este comando permite a los administradores intervenir en el desarrollo del juego si es necesario.

### Comandos de Memorias

**`/pattern join`**  
Permite a un jugador unirse a la cola de espera para la próxima partida de Memorias. Los jugadores deben ejecutar este comando para participar en el minijuego.

## 5. Comandos del Torneo (Core)

### Sistema de Ranking

**`/ranking`**  
Muestra el ranking global del torneo con los 10 mejores jugadores ordenados por puntuación total acumulada en todos los minijuegos.

**`/ranking top <número>`**  
Muestra un número específico de jugadores en el ranking global. Por ejemplo, `/ranking top 20` mostrará los 20 mejores jugadores del torneo.

**`/ranking <nombre_del_minijuego>`**  
Muestra el ranking específico para un minijuego particular. Los nombres válidos son:
- `robarcola` - Ranking del minijuego Robar la Cola
- `memorias` - Ranking del minijuego Memorias

Este comando permite a los jugadores y administradores ver el desempeño específico en cada actividad del torneo.

## 6. Gestión de Datos

### Almacenamiento de Puntuaciones

El sistema almacena automáticamente todas las puntuaciones de los jugadores en el archivo `plugins/TorneoMMT/scores.yml`. Este archivo se actualiza en tiempo real conforme los jugadores acumulan puntos en los diferentes minijuegos.

### Respaldo y Mantenimiento

Se recomienda realizar respaldos periódicos del archivo `scores.yml` para prevenir pérdida de datos. El archivo puede copiarse manualmente mientras el servidor está en ejecución sin afectar el funcionamiento del plugin.

### Advertencia sobre Edición Manual

La edición manual del archivo `scores.yml` debe realizarse con extrema precaución. Cualquier error de sintaxis YAML o inconsistencia en los datos puede causar corrupción del archivo y pérdida de información del ranking.

Si es necesario editar el archivo manualmente:
1. Detenga completamente el servidor
2. Realice un respaldo del archivo original
3. Edite el archivo con un editor de texto que respete el formato YAML
4. Verifique la sintaxis antes de reiniciar el servidor
5. Monitoree los logs al reiniciar para detectar posibles errores de carga

### Reinicio de Datos

Para reiniciar completamente el ranking del torneo, detenga el servidor y elimine o renombre el archivo `scores.yml`. Al reiniciar, el plugin creará un nuevo archivo con datos en blanco.

## 7. Solución de Problemas

### El plugin no carga correctamente
- Verifique que está utilizando Paper 1.21 o superior
- Confirme que Java 17 o superior está instalado
- Revise los logs del servidor para identificar errores específicos

### Los comandos no funcionan
- Asegúrese de tener los permisos de operador en el servidor
- Verifique que el plugin esté habilitado con el comando `/plugins`

### Las ubicaciones no se guardan
- Confirme que está ejecutando los comandos en el mundo correcto
- Verifique que tiene permisos de escritura en la carpeta `plugins/TorneoMMT/`

### Los puntos no se registran
- Revise que el archivo `scores.yml` no esté corrupto
- Verifique los logs para detectar errores de escritura
- Confirme que el plugin tiene permisos para modificar archivos

## 8. Soporte y Contacto

Para reportar problemas, solicitar nuevas funcionalidades o contribuir al desarrollo del proyecto, consulte el repositorio oficial del proyecto o contacte al equipo de desarrollo.

---

**Versión del documento:** 1.0  
**Última actualización:** 2025-09-29  
**Compatible con:** TorneoMMT 1.0-SNAPSHOT
