Título: Plan Maestro de Implementación: Sistema de Cadena Visual de Alto Rendimiento

ID de Característica: CADENA-FEAT-01-VISUALCHAIN

Autor: Arquitecto de Plugins Kotlin

Fecha: 13 de octubre de 2025

Resumen Ejecutivo:
Este documento detalla el diseño arquitectónico para la implementación de un efecto visual de "cadena" que conecte a los jugadores de un mismo equipo en el minijuego Cadena. La implementación se basará en tecnologías modernas de PaperMC, específicamente las entidades ItemDisplay y la biblioteca de matemáticas 3D JOML, para garantizar un rendimiento óptimo y un efecto visual fluido y preciso. El desarrollo se dividirá en tres fases para aislar la complejidad y asegurar la calidad en cada etapa: 1) Creación del Componente Visual Central, 2) Desarrollo del Servicio Orquestador y 3) Integración en el Ciclo de Vida del Juego.

1. Fundamento Tecnológico:

Entidades ItemDisplay: Se utilizarán como el elemento de renderizado principal. Su naturaleza no física (sin IA, colisiones o gravedad) las convierte en la opción ideal para crear efectos visuales sin sobrecargar el servidor.

Biblioteca JOML: Se empleará para realizar todos los cálculos de transformaciones 3D (traslación, rotación, escala). Su inclusión en PaperMC nos proporciona un conjunto de herramientas matemáticas estándar en la industria para este tipo de tareas.

2. Arquitectura por Fases:

Fase 1: El Componente Atómico (VisualChain.kt)

Se creará una clase encapsulada que representará una única cadena visual entre dos jugadores.

Responsabilidad Única: Esta clase contendrá toda la lógica matemática y de la API de Bukkit para:

Generar una entidad ItemDisplay que muestre un ítem configurable (ej. Material.CHAIN).

Calcular en tiempo real la posición (punto medio), la escala (distancia) y la rotación (lookAlong) entre los dos jugadores.

Aplicar estos cálculos como una Transformation a la entidad ItemDisplay.

Gestionar su propio ciclo de vida (creación, actualización periódica y destrucción).

Fase 2: El Servicio Orquestador (ChainVisualizerService.kt)

Se creará un servicio de alto nivel que gestionará la colección completa de VisualChain activas durante una partida.

Responsabilidades:

Actuar como el único punto de entrada para el resto del plugin.

Mantener un registro de las VisualChain activas para evitar duplicados y facilitar su gestión.

Contener la lógica de negocio para crear y destruir las cadenas de un equipo completo.

Proveer métodos para una limpieza completa de todas las entidades visuales al final de una partida.

Fase 3: La Integración (GameManager.kt)

La fase final consistirá en conectar el ChainVisualizerService con la máquina de estados del juego ya existente en el GameManager.

Puntos de Integración:

Inicio de la Partida: Cuando el estado cambie a IN_GAME, se invocará al servicio para crear las cadenas de todos los equipos.

Durante la Partida: Cuando un jugador sea eliminado o se desconecte, el servicio deberá ser notificado para destruir las cadenas asociadas a ese jugador.

Fin de la Partida: Cuando el estado cambie a FINISHED, se invocará al servicio para realizar una limpieza total, garantizando que no queden entidades huérfanas en el mundo.