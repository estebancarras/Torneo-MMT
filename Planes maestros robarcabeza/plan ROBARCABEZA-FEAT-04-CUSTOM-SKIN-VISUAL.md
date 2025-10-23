"Robar la Cabeza del Creador"
ID de Característica: ROBARCABEZA-FEAT-04-CUSTOM-SKIN-VISUAL (Revisión 2)

Autor: Arquitecto de Plugins Kotlin

Fecha: 23 de octubre de 2025

1. Resumen Ejecutivo (Revisado)
Este documento detalla la versión final de la evolución del minijuego RobarCola a "Robar la Cabeza del Creador". Se implementará una mecánica visual única: el jugador "portador" tendrá su cabeza visualmente reemplazada por una versión gigante de la cabeza de un Creador del torneo. Este efecto se logrará renderizando una entidad ItemDisplay personalizada directamente en la posición del casco del jugador, siguiendo cada uno de sus movimientos y rotaciones a la perfección.

El sistema de puntuación será dinámico, otorgando puntos por cada segundo de posesión, y el feedback para el portador será un temporizador ascendente en pantalla. El objetivo es crear una experiencia de juego visualmente impactante, donde el portador se convierte en un "cabezón" fácilmente identificable, haciendo la jugabilidad intuitiva y muy divertida.

2. Filosofía de Diseño: El Casco Trofeo
La jugabilidad se centra en convertir al portador en un "trofeo viviente". La cabeza gigante de un creador no es un ítem que flota, sino que actúa como un "casco trofeo" que reemplaza visualmente la cabeza del jugador. Esta aproximación:

No interfiere con el gameplay: No ocupa slots de inventario ni afecta la armadura real del jugador.

Es un objetivo claro y cómico: El efecto de "cabezón" es inconfundible y añade un toque de humor.

Añade Personalidad: Usar las skins de los organizadores crea una conexión directa y memorable con los jugadores.

La mecánica de "recompensa continua" por tiempo de posesión se mantiene como el núcleo competitivo del juego.

3. Arquitectura de la Solución (Refinada)
La implementación se centrará en un VisualService avanzado, capaz de generar y posicionar con precisión las cabezas personalizadas.

Fase 1: Creación del Servicio de Visualización (HeadVisualService)
Objetivo: Encapsular toda la lógica para renderizar y animar la cabeza gigante en la posición exacta del casco del jugador.

Creación de HeadVisualService.kt:

Ubicación: minigame-robarcola/src/main/kotlin/yo/spray/robarCola/services/.

Responsabilidad: Orquestar el efecto visual de la cabeza.

Métodos Clave:

setCarrier(newCarrier: Player):

Limpia cualquier efecto visual anterior.

Selecciona un nombre de la lista de creadores configurada.

Crea un ItemStack de PLAYER_HEAD con la skin del creador seleccionado usando SkullMeta.

Crea la entidad ItemDisplay, le asigna el ItemStack de la cabeza.

Aplica una Transformation a la entidad para escalarla a un tamaño mayor (ej. scale = Vector3f(1.5f, 1.5f, 1.5f)).

Inicia la tarea de animación (animationTask).

removeCarrier(): Detiene la animación y elimina la entidad ItemDisplay.

startAnimationTask():

Inicia un BukkitRunnable que se ejecuta cada tick.

En cada ejecución, calcula la posición precisa de la cabeza del portador. La mejor base es carrier.getEyeLocation(), ajustada con un pequeño offset vertical para que se asiente donde iría el casco.

Teletransporta el ItemDisplay a esta posición precisa.

Actualiza la Transformation de la entidad para que su rotación (leftRot) coincida con la rotación de la cabeza del jugador (carrier.location.yaw).

Fase 2: Integración con la Lógica de Juego y Puntuación por Tiempo
Objetivo: Conectar el VisualService al motor de juego y mantener el sistema de puntuación dinámico. (Esta fase no sufre cambios respecto al plan anterior).

Refactorización del GameManager.kt:

Se inyectará una instancia del HeadVisualService.

Las llamadas para asignar el rol de portador se delegarán a headVisualService.setCarrier(player).

La lógica del GameTimer para actualizar el ActionBar y el mapa timeWithHead se mantiene.

Expansión de RobarColaGame.kt:

La propiedad val timeWithHead = mutableMapOf<UUID, Int>() para el seguimiento del tiempo sigue siendo la correcta.

Actualización del ScoreService.kt:

El método calculateAndAwardPoints(game: RobarColaGame) se mantiene. Otorgará puntos al final de la partida basándose en los segundos acumulados en timeWithHead.

Fase 3: Configuración y Pulido
Objetivo: Dar a los administradores control total sobre las cabezas y el efecto visual.

Creación de robarcola.yml:

Ubicación: src/main/resources/.

Se diseñará para ser flexible y fácil de usar:

YAML

# robarcola.yml
visuals:
  scale: 1.5          # Factor de escala de la cabeza (1.0 es normal)
  y-offset: -0.25     # Ajuste vertical desde la altura de los ojos del jugador para asentar la cabeza.

  # Lista de nombres de jugadores (skins) para usar como cabezas.
  creator-heads:
    - "Notch"
    - "jeb_"
    # Nombres de los creadores del torneo
    - "Brocoly776" 
    - "citt"

scoring:
  points-per-second: 1
Implementar Carga de Configuración:

RobarColaManager cargará robarcola.yml en onEnable y pasará los valores a los servicios correspondientes.

4. Criterios de Aceptación (Definitivos)
Al iniciar el juego, el portador tiene una cabeza de skin personalizada y agrandada colocada directamente sobre su cabeza, reemplazando visualmente la original.

El efecto visual sigue los movimientos y la rotación de la cabeza del portador de forma fluida y precisa, sin flotar por encima.

Al robarla, el efecto se transfiere instantáneamente al nuevo portador, potencialmente cambiando a otra cabeza de la lista de creadores.

El ActionBar del portador muestra un temporizador ascendente de su tiempo de posesión.

Al finalizar la partida, los puntos se asignan en el ranking global en función del tiempo total que cada jugador mantuvo la cabeza.

La lista de nicks para las cabezas y los parámetros visuales (escala, offset) son completamente configurables en robarcola.yml.