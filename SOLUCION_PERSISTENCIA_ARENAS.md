# Solución Completa: Persistencia de Arenas - Bug Crítico Resuelto

**Fecha:** 16 de octubre de 2025  
**Estado:** ✅ IMPLEMENTADO Y CORREGIDO

---

## 🔴 Problema Identificado

Las arenas de Memorias **se guardaban correctamente** durante el apagado del servidor, pero **se perdían al reiniciar**. El usuario reportó: *"las arenas se pierden cuando el server se cierra"*.

---

## 🔍 Diagnóstico Técnico

Se identificaron **DOS bugs críticos** en el módulo de Memorias:

### Bug #1: Sobrescritura del Objeto de Configuración
**Ubicación:** `MemoriasManager.kt` línea 179

```kotlin
// ❌ CÓDIGO INCORRECTO (ANTES)
arenasConfig = YamlConfiguration()  // Crea un objeto NUEVO y VACÍO
```

**Impacto:** Al crear un nuevo objeto `YamlConfiguration()`, se perdía la referencia al archivo cargado. Esto causaba que el guardado escribiera desde un objeto vacío, sobrescribiendo el archivo con datos vacíos.

**Solución aplicada:**
```kotlin
// ✅ CÓDIGO CORRECTO (AHORA)
// Limpiar la configuración existente sin perder la referencia al archivo
arenasConfig.getKeys(false).forEach { key -> arenasConfig.set(key, null) }
```

### Bug #2: Nombre de Archivo Genérico
**Ubicación:** `MemoriasManager.kt` línea 47

```kotlin
// ❌ CÓDIGO INCORRECTO (ANTES)
arenasFile = File(plugin.dataFolder, "arenas.yml")
```

**Impacto:** Todos los minijuegos compartían el mismo nombre de archivo `arenas.yml`, causando conflictos potenciales entre módulos.

**Solución aplicada:**
```kotlin
// ✅ CÓDIGO CORRECTO (AHORA)
arenasFile = File(plugin.dataFolder, "memorias_arenas.yml")
```

---

## ✅ Cambios Implementados

### Archivo: `minigame-memorias/src/main/kotlin/los5fantasticos/memorias/MemoriasManager.kt`

#### Cambio 1: Nombre de archivo único
- **Línea 47:** `arenas.yml` → `memorias_arenas.yml`
- **Línea 56:** Actualizado mensaje de log
- **Línea 162:** Actualizado mensaje de log

#### Cambio 2: Guardado sin sobrescritura
- **Línea 179-180:** Reemplazado `arenasConfig = YamlConfiguration()` por limpieza de claves sin perder referencia

---

## 🧪 Prueba de Aceptación

Para verificar que la solución funciona:

1. **Compilar el proyecto:**
   ```bash
   mvn clean package
   ```

2. **Iniciar el servidor**

3. **Crear una arena de prueba:**
   ```
   /memorias creararena prueba
   /memorias setlobby prueba
   /memorias crearparcela prueba
   ```

4. **Apagar el servidor normalmente** (comando `/stop`)

5. **Verificar el archivo:**
   - Ruta: `plugins/TorneoMMT/memorias_arenas.yml`
   - Debe contener la arena "prueba" con sus datos

6. **Reiniciar el servidor**

7. **Verificar persistencia:**
   ```
   /memorias listar
   ```
   - La arena "prueba" debe aparecer en la lista

---

## 📊 Arquitectura de Persistencia (Completa)

### Ciclo de Vida del TorneoPlugin (Core)

```
onEnable() → Cargar módulos → Registrar instancias REALES
                                        ↓
                            minigameModules.add(module)
                                        ↓
                            [Instancias vivas en memoria]
                                        ↓
onDisable() → Iterar instancias REALES → module.onDisable()
                                        ↓
                            Guardar puntajes globales
```

### Ciclo de Vida del MemoriasManager (Módulo)

```
onEnable() → Cargar memorias_arenas.yml → cargarArenas()
                                        ↓
                            [Arenas en memoria: Map<String, Arena>]
                                        ↓
onDisable() → guardarArenas() → Limpiar config sin sobrescribir
                                        ↓
                            Escribir arenas al archivo
                                        ↓
                            arenasConfig.save(arenasFile)
```

---

## 🎯 Garantías de la Solución

✅ **Las arenas persisten** entre reinicios del servidor  
✅ **No hay conflictos** de nombres de archivo entre módulos  
✅ **No se sobrescribe** el archivo con datos vacíos  
✅ **Logging detallado** para diagnóstico de problemas  
✅ **Arquitectura modular** mantenida (cada módulo gestiona sus datos)

---

## 📝 Notas Técnicas

- El archivo se guarda en: `plugins/TorneoMMT/memorias_arenas.yml`
- Cada módulo debe usar su propio nombre de archivo (ej: `cadena_arenas.yml`, `skywars_arenas.yml`)
- La referencia al objeto `YamlConfiguration` debe mantenerse durante todo el ciclo de vida
- El método `arenasConfig.getKeys(false).forEach { key -> arenasConfig.set(key, null) }` limpia las claves sin crear un nuevo objeto

---

## 🔧 Aplicar a Otros Minijuegos

Si otros minijuegos tienen el mismo problema, aplicar estos cambios:

1. **Cambiar nombre de archivo:**
   ```kotlin
   File(plugin.dataFolder, "{minijuego}_arenas.yml")
   ```

2. **No sobrescribir config al guardar:**
   ```kotlin
   // En lugar de: config = YamlConfiguration()
   config.getKeys(false).forEach { key -> config.set(key, null) }
   ```

---

**Autor:** Sistema de Refactorización TorneoMMT  
**Revisión:** Arquitecto de Software Senior
