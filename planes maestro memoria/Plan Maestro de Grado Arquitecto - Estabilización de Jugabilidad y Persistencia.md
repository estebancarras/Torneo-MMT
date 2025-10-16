Plan Maestro de Grado Arquitecto - Estabilización de Jugabilidad y Persistencia
Título: Plan Maestro de Arquitectura (Fase 3): Estabilización de la Lógica de Juego y Robustecimiento de la Persistencia

ID de Característica: MEMORIAS-REFACTOR-03-STABILIZATION

Autor: Arquitecto de Plugins Kotlin

1. Filosofía de la Corrección: Del Prototipo al Producto Robusto
Esta fase se centra en la robustez. Un juego no solo debe funcionar en condiciones ideales, sino que también debe ser resistente a comportamientos inesperados de los jugadores y garantizar la integridad de sus datos. Nuestra filosofía será:

Curación Inteligente de Datos: El sistema debe ser lo suficientemente inteligente como para autogestionar su conjunto de datos (block-set) y prevenir problemas de jugabilidad causados por una mala configuración.

Gestión de Estado Rigurosa: El flujo de un turno de jugador debe ser atómico e ininterrumpible. Un jugador hace su jugada, y el sistema debe bloquear cualquier otra entrada hasta que esa jugada sea procesada y el turno concluya o continúe.

Persistencia Garantizada: El principio rector es simple: "Cualquier cambio hecho por un administrador debe sobrevivir a un reinicio". La persistencia no puede ser un efecto secundario; debe ser una acción explícita y garantizada.

2. Arquitectura de las Soluciones
Se implementarán tres soluciones arquitectónicas dirigidas a cada uno de los problemas identificados.

Solución #1: Filtrado Inteligente del Mazo de Bloques (BlockDeckManager.kt)

Estrategia: En lugar de depender de que el administrador elija bloques perfectos, haremos que el BlockDeckManager sea más inteligente. Se le enseñará a identificar y excluir materiales problemáticos.

Implementación:

Se creará una lista negra (blacklist) interna y privada dentro del BlockDeckManager. Esta lista contendrá los nombres de los materiales que son visualmente ambiguos o que tienen variantes problemáticas (ej. PUMPKIN, CARVED_PUMPKIN, REDSTONE_WIRE, TRIPWIRE, etc.).

Se modificará el método loadDeck(). Después de leer la lista block-set del memorias.yml, iterará sobre ella y descartará cualquier material cuyo nombre esté en la lista negra, registrando una advertencia en la consola para informar al administrador.

Adicionalmente, se podría añadir una lógica que detecte bloques con BlockData compleja (como Directional, Rotatable) y los excluya automáticamente para prevenir problemas de orientación.

Solución #2: Implementación de un "Bloqueo de Turno" (DueloMemorias.kt)

Estrategia: Se introducirá una nueva variable de estado dentro de DueloMemorias para controlar si un jugador puede interactuar con el tablero.

Implementación:

Se añadirá una nueva propiedad a DueloMemorias: private var aceptandoInput: Boolean = true.

En handlePlayerClick(), la primera guarda de seguridad será if (!aceptandoInput) return.

Inmediatamente después de que un jugador selecciona su segunda carta (antes incluso de verificar si es un par), se establecerá aceptandoInput = false.

El "bloqueo" se levantará (aceptandoInput = true) solo en dos momentos: a. Cuando el turno cambia al otro jugador. b. Después de un acierto, justo antes de que el mismo jugador pueda intentar otro par.

Solución #3: Garantizar la Persistencia (MemoriasManager.kt y MinigameMemorias.kt)

Estrategia: Se aplicará una política de "guardado inmediato" y un guardado de seguridad final.

Implementación:

Se verificará que el método saveArenas() en MemoriasManager esté correctamente implementado.

Se auditará el archivo MemoriasCommand.kt y se añadirá una llamada explícita a memoriasManager.saveArenas() al final de la ejecución de cada subcomando que modifique los datos de las arenas (creararena, parcela add, parcela remove).

Se implementará el guardado de seguridad final en la clase principal MinigameMemorias.kt, dentro del método onDisable(), que se ejecuta cuando el servidor se apaga o el plugin se deshabilita.

Con estas tres intervenciones quirúrgicas, el minijuego "Memorias" pasará de ser un prototipo con fallos a una experiencia de juego robusta, justa y estable, lista para las pruebas finales antes del torneo.