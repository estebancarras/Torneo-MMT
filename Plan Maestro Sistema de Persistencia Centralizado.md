Plan Maestro Sistema de Persistencia Centralizado
Título: Plan Maestro de Arquitectura (Fase de Estabilización): Implementación de un Ciclo de Vida de Persistencia de Datos Gestionado por el Core

ID de Característica: CORE-REFACTOR-01-PERSISTENCE-LIFECYCLE

Autor: Arquitecto de Plugins Kotlin

Fecha: 16 de octubre de 2025

1. Filosofía de Diseño: El Core como Única Fuente de Verdad del Ciclo de Vida
La fiabilidad de los datos es el pilar sobre el que se asienta todo el torneo. Un sistema que pierde la configuración de las arenas es un sistema inaceptable para un entorno de producción. El fallo actual no reside en un minijuego específico, sino en una debilidad arquitectónica en el torneo-core: su incapacidad para gestionar correctamente el ciclo de vida de los módulos que carga.

La filosofía de esta refactorización es simple y poderosa: el TorneoPlugin debe ser el único y absoluto gestor del ciclo de vida de sus módulos de minijuegos. Debe conocer cada instancia que crea, mantener una referencia a ella durante toda la sesión del servidor, y comunicarse con esa misma instancia para ordenarle que guarde sus datos al apagarse. Se eliminará por completo el anti-patrón de "instancias fantasma" (Gemelos Malvados), garantizando que la data que se guarda sea siempre la data que está en uso.

2. Arquitectura de la Solución: Un Registro de Módulos Activos
La solución es elegante y se centra en una única modificación estructural dentro del TorneoPlugin.kt.

Componente Central: torneo-core/src/main/kotlin/los5fantasticos/torneo/TorneoPlugin.kt.

Modificación Arquitectónica: Se introducirá un Registro de Módulos en la clase TorneoPlugin. Esta será una propiedad privada que almacenará una referencia a cada instancia "Real" de los módulos de minijuegos en el momento en que son cargados.

Kotlin

// En TorneoPlugin.kt
private val loadedMinigames = mutableListOf<MinigameModule>()
Esta lista se convertirá en la "fuente de verdad" de los módulos activos.

3. Refactorización del Ciclo de Vida del Plugin
La implementación se divide en dos fases críticas del ciclo de vida del TorneoPlugin.

Fase 1: Registro Durante el Arranque (onEnable)

Análisis del Problema: Actualmente, el TorneoPlugin instancia los módulos, pero "olvida" las referencias a ellos una vez que termina de cargarlos.

Solución Estratégica: Modificaremos la lógica de carga de módulos en onEnable(). Inmediatamente después de que una instancia de un MinigameModule (como MinigameMemorias) es creada y cargada, esa misma instancia será añadida a nuestra nueva lista loadedMinigames. Este paso es crucial, ya que establece la referencia persistente que usaremos durante todo el ciclo de vida.

Fase 2: Guardado Controlado Durante el Apagado (onDisable)

Análisis del Problema: El método onDisable() actual crea nuevas instancias de los módulos, las cuales están vacías, y les ordena guardar, sobrescribiendo los datos buenos.

Solución Estratégica: Se refactorizará por completo el método onDisable(). La nueva lógica ya no creará ninguna instancia. En su lugar:

Iterará sobre la lista loadedMinigames, que contiene las referencias a las instancias "Reales" y llenas de datos.

Para cada minigame en la lista, llamará a su método onUnload().

De esta manera, nos aseguramos de que es la instancia que ha estado funcionando y acumulando datos la que recibe la orden de persistir su estado en el disco.

4. El Contrato del MinigameModule: Responsabilidades Claras
Esta refactorización refuerza nuestra arquitectura modular, no la rompe. El torneo-core no sabrá cómo guardar los datos de "Memorias" o "Cadena"; eso sería una violación del encapsulamiento. El contrato sigue siendo el mismo:

Responsabilidad del Core (TorneoPlugin):

Gestionar el ciclo de vida: onLoad() al inicio, onUnload() al final.

Mantener las referencias a las instancias vivas.

Responsabilidad de Cada Módulo (MinigameModule):

En onLoad(): Implementar la lógica para cargar sus propios datos (ej. memoriasManager.loadArenas()).

En onUnload(): Implementar la lógica para guardar sus propios datos (ej. memoriasManager.saveArenas()).

El Core actúa como el director de la orquesta, que le dice al violinista "ahora toca tu solo", pero no le dice cómo mover los dedos.

5. Criterios de Aceptación
La refactorización será un éxito cuando se cumplan las siguientes condiciones:

Cualquier arena creada en CUALQUIER minijuego persiste perfectamente después de un reinicio del servidor.

El archivo de configuración de un minijuego no se vacía al apagar el servidor.

La modularidad del proyecto se mantiene: el torneo-core no contiene lógica de guardado específica de ningún minijuego.