Plan Maestro - Mejoras de Administración y Protección para "Memorias"
Título: Plan Maestro (Fase 1.5): Herramientas de Administración y Protección de Arenas

ID de Característica: MEMORIAS-FEAT-02-ADMIN-TOOLS

Autor: Arquitecto de Plugins Kotlin

Fecha: 14 de octubre de 2025

Resumen Ejecutivo:
Este documento detalla la implementación de un conjunto de características de calidad de vida y de integridad de juego para el minijuego "Memorias". Habiendo completado la refactorización arquitectónica a un modelo de Game Loop, el siguiente paso es dotar a los administradores de herramientas eficientes para la configuración de las arenas y asegurar que la lógica del juego no pueda ser alterada por los jugadores. Las tres características clave a implementar son: 1) un sistema de protección de parcelas para prevenir la construcción/destrucción no autorizada, 2) una herramienta de "varita" para la selección visual de regiones, y 3) una expansión de los comandos para la gestión detallada de las parcelas.

1. Arquitectura de las Nuevas Características:

Servicio de Selección (SelectionManager.kt): Para evitar contaminar la clase principal o los listeners con estado de administrador, se creará un nuevo servicio singleton.

Responsabilidades:

Mantener un registro de los administradores que están actualmente en "modo selección".

Almacenar temporalmente las dos posiciones (pos1, pos2) seleccionadas por cada administrador.

Proveer métodos para que los comandos puedan obtener la selección de un administrador y crear una región (Cuboid) a partir de ella.

Expansión del PlayerListener.kt: El listener existente será ampliado para manejar las nuevas interacciones.

Responsabilidades:

Interceptar los PlayerInteractEvent para la lógica de la "varita de selección".

Interceptar los BlockBreakEvent y BlockPlaceEvent para la lógica de protección de parcelas.

La lógica de protección debe ser eficiente, consultando al GameManager para determinar si un bloque está dentro de una parcela activa.

Refactorización del MemoriasCommand.kt: El TabExecutor actual será expandido para incluir los nuevos subcomandos de gestión de parcelas, mejorando la usabilidad para los administradores.

2. Flujo de Trabajo del Administrador (Visión de UX):

Un administrador ejecuta /memorias varita para recibir la herramienta de selección.

Selecciona dos puntos de una parcela haciendo clic izquierdo y derecho. Recibe feedback en el chat por cada selección.

Ejecuta /memorias parcela add <arena> para crear la parcela basada en su selección.

Si comete un error, puede usar /memorias parcela remove <arena> <índice> para eliminarla o /memorias parcela setspawn1/2 <arena> <índice> para ajustar los puntos de aparición.

3. Criterios de Aceptación:

Los jugadores no pueden romper ni colocar bloques dentro de la región de ninguna parcela de Memorias, a menos que sea una interacción permitida por la lógica del juego.

Los administradores pueden obtener y usar la varita para definir regiones de parcelas de forma visual.

Los administradores tienen a su disposición comandos para eliminar y editar parcelas existentes, y estos cambios se persisten correctamente en arenas.yml.