Plan Maestro de Grado Arquitecto - Implementación Integral de "Memorias"
Título: Plan Maestro de Arquitectura (Versión Final): Sistema de "Cartas de Bloques" con Configuración de Parcela Totalmente Automatizada

ID de Característica: MEMORIAS-FEAT-INTEGRAL-01

Autor: Arquitecto de Plugins Kotlin

1. Filosofía de Diseño: La Fusión de la Experiencia y la Eficiencia
Esta es la visión definitiva para el minijuego "Memorias". Fusiona dos filosofías de diseño críticas en una única arquitectura cohesiva:

Experiencia de Jugador Nativa y Rica: Abandonamos las complejidades de las texturas personalizadas en favor de una solución que es a la vez más simple, robusta y auténtica a la experiencia de Minecraft. Utilizaremos un "mazo" de bloques sólidos, curado por nosotros y definido en la configuración, para crear tableros de juego visualmente diversos, claros y siempre justos. Esto garantiza una jugabilidad intuitiva y un rendimiento óptimo.

Eficiencia Operacional Máxima para el Administrador: La configuración de las 20 parcelas de duelo no puede ser una tarea tediosa. Adoptaremos el principio de "Convención sobre Configuración". El administrador se limitará a definir el espacio físico de cada parcela. A partir de ahí, el sistema debe ser lo suficientemente "inteligente" como para generar automáticamente la disposición del juego: el tablero se centrará en la parcela y los puntos de aparición de los jugadores se calcularán de forma algorítmica y lógica en relación con el tablero.

El resultado es un sistema que ofrece una experiencia de alta calidad al jugador y un proceso de configuración de "un solo paso" para el administrador.

2. Arquitectura de la Solución (Integral)
Esta arquitectura integra tanto el nuevo sistema visual como la automatización de la configuración.

El Gestor del Mazo de Bloques (BlockDeckManager.kt - Nuevo Servicio):

Principio de Diseño: Se creará un servicio singleton (object en Kotlin) para centralizar toda la lógica relacionada con el "mazo" de bloques.

Responsabilidades:

Carga y Validación: Al iniciar el plugin, leerá la lista de nombres de materiales de la nueva sección block-set en memorias.yml. Es crucial que valide cada material: debe existir en Bukkit y cumplir con el criterio material.isSolid(). Los materiales inválidos serán descartados y se registrará una advertencia en la consola.

Provisión de Mazos: Expondrá un método getShuffledDeck(count: Int) que devolverá una lista de Materiales únicos y aleatorios, listos para que DueloMemorias los utilice.

El Controlador de Duelo (DueloMemorias.kt - Cerebro de la Jugabilidad):

Refactorización del Modelo de Datos: Su tablero lógico interno será una estructura simple que asocie cada Location del tablero con su Material correspondiente (Map<Location, Material>).

Lógica de Disposición Automática (Nuevo setupDuelo()): Al crearse una instancia de DueloMemorias, un nuevo método setupDuelo() será el responsable de toda la disposición del juego:

Geometría del Tablero: Calculará el punto central (X, Z) de la parcela.region y usará la Y mínima como el suelo para generar una cuadrícula 2D perfecta (ej. 10x10).

Geometría de Spawns: A partir de la geometría del tablero recién calculada, determinará las dos posiciones de spawn de los jugadores (ej. 2 bloques detrás de los bordes opuestos del tablero) y su orientación (yaw) para que miren hacia el tablero.

Generación del Mazo del Duelo: Solicitará los materiales únicos al BlockDeckManager, los duplicará para crear los pares, los barajará y construirá el tableroLogico.

Finalmente, teletransportará a los jugadores a sus posiciones de spawn calculadas.

Implementación de la Lógica de Juego: Sobre esta base, se implementará la máquina de estados completa (MEMORIZANDO, JUGANDO, FINALIZADO), el sistema de turnos, la lógica de aciertos/fallos, los temporizadores con BossBar y la conexión final con el TorneoManager para la puntuación.

La Configuración (memorias.yml):

Rol: Será el panel de control del diseñador.

Estructura: Contendrá la lista block-set para que el administrador defina el mazo, y la sección game-settings para ajustar tiempos y puntos.

Simplificación de Datos y Comandos:

Parcela.kt: La data class se simplificará a su forma más pura: data class Parcela(val region: Cuboid). Se elimina toda referencia a los spawns.

MemoriasCommand.kt: Se eliminarán todos los subcomandos setspawn. El flujo de creación de una parcela se reduce a un solo comando: /memorias parcela add <arena>.

Este plan integral representa la visión final y más refinada para el minijuego "Memorias". Cubre todos los aspectos: jugabilidad, rendimiento y administración.