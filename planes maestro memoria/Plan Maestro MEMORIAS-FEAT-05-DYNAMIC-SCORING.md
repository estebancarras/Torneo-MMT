Plan Maestro: Sistema de Puntuación Dinámica para "Memorias"
ID de Característica: MEMORIAS-FEAT-05-DYNAMIC-SCORING

Autor: Arquitecto de Plugins Kotlin

Fecha: 17 de octubre de 2025

1. Resumen Ejecutivo
Este documento detalla la arquitectura para refactorizar el sistema de puntuación del minijuego "Memorias". Se abandonará el modelo actual, que solo otorga puntos al final del duelo, en favor de un sistema de puntuación en tiempo real que recompensa el rendimiento de los jugadores durante la partida. La nueva lógica, denominada "Sistema Completo con Bonus de Rendimiento", asignará puntos por cada par encontrado e introducirá bonificaciones por rachas de aciertos, por ser el primero en anotar y por lograr remontadas espectaculares. El objetivo es aumentar la competitividad, la emoción y la justicia de cada duelo, asegurando que la puntuación final refleje fielmente la habilidad demostrada por cada participante en la única ronda que jugarán.

2. Arquitectura de la Solución
La implementación se centrará en externalizar los nuevos valores de puntuación a la configuración y en enriquecer la lógica de la clase DueloMemorias.kt para que gestione el estado y la asignación de puntos dinámicamente.

2.1. Externalización de la Configuración (memorias.yml)

Para mantener la flexibilidad, todos los nuevos valores de puntuación serán configurables. La sección puntuacion en minigame-memorias/src/main/resources/memorias.yml será expandida:

YAML

puntuacion:
  # Puntos otorgados durante el duelo
  por-par-encontrado: 2
  bonus-por-racha: 2       # Puntos extra por cada acierto consecutivo en el mismo turno
  bonus-primer-acierto: 5  # Bonus único para el primer jugador que encuentra un par
  
  # Puntos otorgados al finalizar el duelo
  por-victoria: 20
  por-participacion: 5
  bonus-remontada: 10      # Bonus si un jugador gana tras ir perdiendo por 3 o más pares
2.2. Modificaciones en DueloMemorias.kt

La clase DueloMemorias será el cerebro de la nueva lógica. Requerirá nuevas propiedades para rastrear el estado de las rachas y los bonus.

Nuevas Propiedades de Estado:

private var rachaActual = mutableMapOf(player1.uniqueId to 0, player2.uniqueId to 0): Un mapa para rastrear la racha de aciertos consecutivos de cada jugador en su turno actual.

private var primerAciertoOtorgado = false: Un flag booleano para asegurar que el bonus "Primer Acierto" se entregue solo una vez.

Refactorización de la Lógica de Puntuación:

verificarPar(): Este método será el punto central de la asignación de puntos en tiempo real. Cuando un jugador encuentra un par:

Bonus "Primer Acierto": Se comprobará si primerAciertoOtorgado es false. Si lo es, se otorgarán los puntos del bonus-primer-acierto al jugador y se cambiará el flag a true.

Puntos por Par: Se otorgarán los puntos base de por-par-encontrado.

Bonus por Racha: Se incrementará el contador de rachaActual para ese jugador. Si la racha es mayor a 1, se otorgarán los puntos del bonus-por-racha.

Llamada al Core: Cada asignación de puntos se hará mediante una llamada a torneoManager.addScore(), respetando la arquitectura del proyecto.

Cuando un jugador falla un par o cambia el turno: Se deberá reiniciar el contador de rachaActual para ese jugador a 0. Esto se implementará en la lógica de verificarPar() (en el else) y en cambiarTurno().

Lógica de Bonus Finales (finalizarDueloPorCompletado()):

Bonus de Remontada: Antes de declarar al ganador, se comprobará el estado de las puntuaciones. Si el jugador ganador tenía 3 o más pares menos que su oponente en algún punto y aun así ganó, se le otorgará el bonus-remontada. Esto requerirá registrar el puntaje justo antes de que el ganador iniciara su racha final.

Se mantendrá la lógica de otorgar los puntos finales de por-victoria y por-participacion.

3. Criterios de Aceptación
✅ Todos los nuevos valores de puntuación están definidos y se leen correctamente desde memorias.yml.

✅ Un jugador recibe puntos (por-par-encontrado) inmediatamente después de encontrar un par.

✅ El primer jugador en encontrar un par en un duelo recibe un bonus único (bonus-primer-acierto).

✅ Un jugador que encuentra dos o más pares consecutivos en el mismo turno recibe un bonus adicional (bonus-por-racha) por cada par a partir del segundo.

✅ La racha de un jugador se reinicia a cero si falla un par o si su turno termina.

✅ Un jugador que gana el duelo después de haber estado en una desventaja significativa (3+ pares) recibe un bonus de remontada.

✅ Todos los puntos se asignan a través del TorneoManager y se reflejan correctamente en el ranking global.

✅ El sistema de puntuación final (victoria/participación) sigue funcionando como complemento a los puntos obtenidos durante la partida.