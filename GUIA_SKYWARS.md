# SkyWars — Guía de Configuración

Sistema de arenas de combate aéreo para TorneoMMT basado en **SkyWarsReloaded**, totalmente configurable desde comandos en el juego.

## Descripción

SkyWars es un minijuego clásico donde los jugadores aparecen en islas separadas y deben luchar por sobrevivir, recolectando recursos de cofres, eliminando oponentes y siendo el último en pie.

Esta guía describe cómo crear, configurar y registrar arenas de SkyWarsReloaded paso a paso, asegurando que todas las funciones estén correctamente configuradas.

---

## Requisitos Previos

* Plugin **SkyWarsReloaded** instalado en el servidor.
* Plugin **TorneoMMT** para integrar puntuaciones globales.
* Permisos de administrador para ejecutar comandos `/sw` y `/swm`.

---

## Flujo de Configuración Recomendado

Puedes usar `/swm arenas` para hacer varias cosas de los comandos de abajo como registrar, modificar, revisar, y administra las arenas desde la GUI. <br>
Sigue este orden para crear correctamente una arena funcional:

1. `/sw setspawn` — Establece el lobby principal.
2. `/swm create [mapname] [environment]` — Crea una nueva arena.
3. `/swm edit [mapname]` — Editar la arena. 
4. Construye el mapa o pega tu esquema.
5. `/swm spawn player` o `/swm spawn player [team]` — Define los spawns de jugador.
6. `/swm spawn deathmatch` — Define los spawns de la fase final.
7. `/swm spawn spec` — Define el spawn de espectadores.
8. Coloca cofres en el mapa.
9. `/swm chesttype [mapname]` — Alterna entre cofres normales o de centro. `/swm chestcheck [mapname]` — Comprobar el tipo de cofre
10. `/swm save [mapname]` — Guarda la arena.
11. `/swm register [mapname]` — Registra la arena como jugable.
12. (Opcional) Edita `mapData/[mapname].yml` y ejecuta `/swm refresh [mapname]`.
13. (Opcional) Crea carteles de juego con<br>
[SW]<br>
{mapname} <br>

14. (Opcional) personaliza el loot con `/sw chestadd`.

---

## Configuración Detallada

### 1. Establecer el Lobby

Antes de todo, configura el punto de aparición del lobby:

```
/sw setspawn
```

> ⚠️ Muchos comandos no funcionarán hasta que el lobby esté definido.

---

### 2. Crear una Nueva Arena

```
/swm create [mapname] [environment]
```

**Parámetros:**

* `[mapname]`: nombre interno del mapa (sin espacios)
* `[environment]`: opcional (`normal`, `nether`, `the_end`)


---

### 3. Definir Puntos de Aparición

Usa el siguiente comando en modo edición del mapa:

```
/swm spawn [type]
```

**Tipos de spawn:**

* `player` — bloque de diamante (jugador)
* `deathmatch` — bloque de esmeralda (zona final)
* `spec` — punto de espectadores

**Asignar por equipos (opcional):**

```
/swm spawn player [team]
```

Ejemplo:

```
/swm spawn player red
/swmap spawn player blue
```

Destruye el bloque correspondiente para eliminar un spawn.

---

### 4. Cofres

Coloca cofres manualmente dentro del mapa. Cada vez que coloques uno, se registrará automáticamente.

Cambia el tipo de cofre con:

```
/swm chesttype [mapname]
```

Esto alterna entre **NORMAL** y **CENTER**.

---

### 5. Guardar y Registrar

Guarda los cambios:

```
/swm save [mapname]
```

Registra la arena para su uso:

```
/swm register [mapname]
```

> Una arena requiere **mínimo dos spawns de jugador** para poder registrarse.

Para quitarla del registro:

```
/swm unregister [mapname]
```

---

### 6. Administrar Arenas

Abre la interfaz de administración gráfica con:

```
/swm arenas
```

Desde aquí puedes revisar, editar o eliminar arenas fácilmente.

---

### 7. Editar Archivos Manualmente

Cada mapa genera un archivo YAML en `plugins/SkyWarsReloaded/mapData/[mapname].yml`.

Campos editables:

* `displayName`
* `minPlayers`
* `spawns`
* `signs`
* `events`
* `chestTiers`

Después de editar manualmente, actualiza los datos en el juego:

```
/swm refresh [mapname]
```

---

### 8. Importar Mapas Antiguos (Legacy)

Para convertir arenas de versiones antiguas (SWR 2.x):

```
/swm create [mapname]
```

Pega el esquema en el centro (x=0, z=0), luego:

```
/swm legacyload [mapname]
/swm save [mapname]
/swm register [mapname]
```

Esto escaneará beacons y cofres para convertirlos al nuevo formato.

---

### 9. Crear Carteles de Juego

Coloca el cartel en el lobby y escribe:

```
[SW]
{mapname}
```

> Reemplaza `{mapname}` por el nombre del mapa, **sin corchetes ni llaves**.

---

### 10. Personalizar Loot de Cofres (Opcional)

Agrega ítems personalizados al loot:

```
/sw chestadd [chesttype] [method] [percentage]
```

**Ejemplos:**

```
/sw chestadd normal hand 10
/sw chestadd op inv 35
```

**Parámetros:**

* `[chesttype]`: basic, basiccenter, normal, normalcenter, op, opcenter
* `[method]`: hand (ítem en mano) o inv (todo el inventario)
* `[percentage]`: 1–100 (probabilidad de aparición)

---

## Puntuación en TorneoMMT

| Acción                 | Puntos |
| ---------------------- | ------ |
| Victoria               | 100    |
| Eliminación de jugador | +10    |

Las puntuaciones se integran automáticamente al ranking global de TorneoMMT.

---

## Comandos Principales de SkyWarsReloaded

| Comando                    | Descripción                        |
| -------------------------- | ---------------------------------- |
| `/sw setspawn`             | Establece el spawn del lobby       |
| `/swm create [mapname]`    | Crea una nueva arena               |
| `/swm spawn [type]`        | Define un spawn                    |
| `/swm spawn player [team]` | Asigna spawn a un equipo           |
| `/swm save [mapname]`      | Guarda los cambios                 |
| `/swm register [mapname]`  | Registra la arena                  |
| `/swm arenas`              | Abre la interfaz de administración |
| `/sw chestadd ...`         | Agrega loot a los cofres           |
| `/swm refresh [mapname]`   | Refresca la configuración          |

---

## Notas Finales

* Usa `/swm edit [mapname]` para modificar arenas existentes.
* No borres manualmente los mundos sin antes desregistrarlos.
* Los mapas deben tener **al menos dos spawns de jugador**.

---

**Versión Compatible:** SkyWarsReloaded 5.6.34
**Última Actualización:** 2025-10-16
**Integración:** TorneoMMT v1.0-SNAPSHOT
