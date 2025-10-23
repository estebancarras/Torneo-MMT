# 🎮 Guía de Configuración - RobarCabeza v5.0

## 📋 Índice
1. [Requisitos Previos](#requisitos-previos)
2. [Configuración Inicial](#configuración-inicial)
3. [Creación de Arenas](#creación-de-arenas)
4. [Comandos Disponibles](#comandos-disponibles)
5. [Flujo de Juego](#flujo-de-juego)
6. [Solución de Problemas](#solución-de-problemas)

---

## 🔧 Requisitos Previos

Antes de configurar el minijuego, asegúrate de:
- ✅ Tener el plugin TorneoMMT instalado y funcionando
- ✅ Tener permisos de administrador (`robarcabeza.admin` o OP)
- ✅ Tener un mundo preparado para el minijuego

---

## ⚙️ Configuración Inicial

### Paso 1: Verificar la Instalación

```
/robarcabeza admin list
```

Este comando mostrará las arenas configuradas. Si es la primera vez, estará vacío.

---

## 🏟️ Creación de Arenas

### Paso 2: Crear una Arena Nueva

```
/robarcabeza admin create <nombre>
```

**Ejemplo:**
```
/robarcabeza admin create arena1
```

**Resultado:**
```
✓ Arena 'arena1' creada
Usa los siguientes comandos para configurarla:
  /robarcabeza admin addspawn <arena> - Añadir spawns
  /robarcabeza admin setregion <arena> - Establecer región
```

---

### Paso 3: Añadir Puntos de Spawn

Los spawns son las ubicaciones donde aparecerán los jugadores al inicio de la partida.

**Proceso:**
1. Párate en la ubicación donde quieres un spawn
2. Ejecuta:
   ```
   /robarcabeza admin addspawn arena1
   ```
3. Repite para añadir más spawns (recomendado: mínimo 4-8 spawns)

**Ejemplo:**
```
/robarcabeza admin addspawn arena1
✓ Spawn 1 añadido a 'arena1'
Ubicación: 100, 64, 200
```

**💡 Tip:** Añade spawns distribuidos por toda el área de juego para evitar que los jugadores aparezcan muy juntos.

---

### Paso 4: Establecer la Región de Juego

La región define los límites del área donde los jugadores pueden moverse durante la partida.

**Proceso:**

1. **Obtener la varita de selección:**
   ```
   /torneo wand
   ```

2. **Seleccionar el área:**
   - **Clic Izquierdo** en un bloque = Posición 1 (esquina inferior)
   - **Clic Derecho** en un bloque = Posición 2 (esquina superior opuesta)

3. **Asignar la región a la arena:**
   ```
   /robarcabeza admin setregion arena1
   ```

**Ejemplo:**
```
✓ Posición 1 establecida: (50, 60, 150)
✓ Posición 2 establecida: (150, 80, 250)
¡Selección completa!

/robarcabeza admin setregion arena1
✓ Región establecida para 'arena1'
Tamaño: 101x21x101
```

**⚠️ Importante:** La región debe ser lo suficientemente grande para que los jugadores puedan moverse y perseguirse.

---

### Paso 5: Verificar la Configuración

```
/robarcabeza admin info arena1
```

**Resultado esperado:**
```
━━━━━━ Arena: arena1 ━━━━━━
Spawns: 6
  1. 100, 64, 200
  2. 110, 64, 210
  3. 120, 64, 200
  4. 130, 64, 210
  5. 140, 64, 200
  6. 150, 64, 210
Región: 101x21x101
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## 📝 Comandos Disponibles

### Comandos de Jugador

| Comando | Descripción |
|---------|-------------|
| `/robarcabeza join` | Unirse a una partida |
| `/robarcabeza leave` | Salir de una partida |

---

### Comandos de Administración - Gestión de Arenas

| Comando | Descripción | Ejemplo |
|---------|-------------|---------|
| `/robarcabeza admin create <nombre>` | Crear una arena nueva | `/robarcabeza admin create arena1` |
| `/robarcabeza admin list` | Listar todas las arenas | `/robarcabeza admin list` |
| `/robarcabeza admin delete <nombre>` | Eliminar una arena | `/robarcabeza admin delete arena1` |
| `/robarcabeza admin info <arena>` | Ver información de una arena | `/robarcabeza admin info arena1` |
| `/robarcabeza admin addspawn <arena>` | Añadir spawn en tu ubicación actual | `/robarcabeza admin addspawn arena1` |
| `/robarcabeza admin setregion <arena>` | Establecer región con tu selección | `/robarcabeza admin setregion arena1` |

---

### Comandos de Administración - Control de Partida

| Comando | Descripción | Ejemplo |
|---------|-------------|---------|
| `/robarcabeza admin startgame [arena]` | Iniciar juego manualmente | `/robarcabeza admin startgame arena1` |
| `/robarcabeza admin stopgame` | Detener juego actual | `/robarcabeza admin stopgame` |
| `/robarcabeza admin givehead <jugador>` | Dar cabeza a un jugador | `/robarcabeza admin givehead Steve` |

---

## 🎯 Flujo de Juego

### Inicio de Partida

1. **Los jugadores se unen:**
   ```
   /robarcabeza join
   ```

2. **El administrador inicia el juego:**
   ```
   /robarcabeza admin startgame arena1
   ```
   
   O si no especificas arena, se seleccionará una aleatoria:
   ```
   /robarcabeza admin startgame
   ```

3. **Cuenta atrás:**
   - Los jugadores son teletransportados a spawns aleatorios de la arena
   - Aparece una cuenta atrás: 3... 2... 1... ¡Vamos!

4. **Inicio del juego:**
   - Algunos jugadores aleatorios reciben la "Cabeza del Creador" (equipada en el slot del casco)
   - Los jugadores con cabeza brillan (efecto GLOWING)
   - Comienza el temporizador de 2 minutos

---

### Durante la Partida

**Objetivo:** Robar la cabeza de otros jugadores atacándolos por la espalda.

**Mecánicas:**
- ✅ **Cabeza equipada:** Los jugadores con cabeza la llevan en el slot del casco
- ✅ **Protección de inventario:** No puedes quitarte la cabeza manualmente
- ✅ **Robo por la espalda:** Debes atacar a un jugador por detrás para robarle la cabeza
- ✅ **Puntos por segundo:** Ganas puntos cada segundo que tengas la cabeza
- ✅ **Bonus por robo:** Ganas puntos extra al robar una cabeza
- ✅ **Cooldown:** Hay un cooldown de 3 segundos entre robos
- ✅ **Invulnerabilidad:** Después de que te roben, eres invulnerable por unos segundos
- ✅ **Límites de arena:** No puedes salir de la región configurada

**Puntuación:**
- 🏆 **Por segundo con cabeza:** +1 punto/segundo (configurable)
- 🏆 **Por robar cabeza:** +10 puntos (configurable)
- 🏆 **Bonus por posición final:**
  - 1er lugar: +50 puntos
  - 2do lugar: +30 puntos
  - 3er lugar: +20 puntos
- 🏆 **Participación:** +5 puntos

---

### Final de Partida

1. **El tiempo se agota** (2 minutos)
2. **Se muestra el ranking:**
   ```
   ========== FINAL DEL JUEGO ==========
   🥇 #1: Steve (125 puntos)
   🥈 #2: Alex (98 puntos)
   🥉 #3: Notch (76 puntos)
   ====================================
   ```
3. **Los jugadores son teletransportados al lobby**

---

## 🔧 Configuración Avanzada

### Archivo: `robarcabeza.yml`

```yaml
# Configuración de visuales
visuals:
  # Lista de nombres de jugadores cuyas skins se usarán para las cabezas
  creator-heads:
    - "Notch"
    - "Herobrine"
    - "Steve"
    - "Alex"

# Configuración de puntuación
scoring:
  # Puntos ganados por segundo al tener la cabeza
  points-per-second: 1
  
  # Puntos ganados al robar una cabeza
  points-steal-bonus: 10
  
  # Puntos por posición final
  points-first-place: 50
  points-second-place: 30
  points-third-place: 20
  
  # Puntos por participación
  points-participation: 5

# Configuración de juego
game:
  # Duración de la partida en segundos
  duration: 120
  
  # Número de jugadores que empiezan con cabeza
  initial-heads-count: 2
  
  # Cooldown entre robos en segundos
  steal-cooldown: 3
  
  # Invulnerabilidad después de ser robado (segundos)
  invulnerability-cooldown: 5
```

---

## 🛠️ Solución de Problemas

### Problema: "No hay arenas configuradas"

**Solución:**
```
/robarcabeza admin create arena1
/robarcabeza admin addspawn arena1
/robarcabeza admin setregion arena1
```

---

### Problema: "La arena no tiene spawns configurados"

**Solución:**
Añade al menos 2 spawns:
```
/robarcabeza admin addspawn arena1
```

---

### Problema: "No puedes salir de la arena"

**Causa:** Estás intentando salir de la región configurada durante una partida.

**Solución:** Esto es intencional. La región mantiene a los jugadores dentro del área de juego.

---

### Problema: "No puedo quitarme la cabeza"

**Causa:** La protección de inventario evita que te quites la cabeza manualmente.

**Solución:** Esto es intencional. Solo puedes perder la cabeza si otro jugador te la roba.

---

## 📊 Ejemplo de Configuración Completa

```bash
# 1. Crear arena
/robarcabeza admin create arena_principal

# 2. Añadir 8 spawns (párate en cada ubicación)
/robarcabeza admin addspawn arena_principal
/robarcabeza admin addspawn arena_principal
/robarcabeza admin addspawn arena_principal
/robarcabeza admin addspawn arena_principal
/robarcabeza admin addspawn arena_principal
/robarcabeza admin addspawn arena_principal
/robarcabeza admin addspawn arena_principal
/robarcabeza admin addspawn arena_principal

# 3. Obtener varita y seleccionar región
/torneo wand
# Clic izquierdo en esquina 1
# Clic derecho en esquina 2
/robarcabeza admin setregion arena_principal

# 4. Verificar configuración
/robarcabeza admin info arena_principal

# 5. Iniciar partida de prueba
/robarcabeza join
/robarcabeza admin startgame arena_principal
```

---

## 🎮 Integración con el Sistema de Torneo

El minijuego se integra automáticamente con el sistema de torneo centralizado:

```bash
# Iniciar torneo con RobarCabeza
/torneo start robarcabeza

# Los jugadores son añadidos automáticamente
# La arena se selecciona aleatoriamente
# Los puntos se registran en el scoreboard global
```

---

## 📝 Notas Finales

- ✅ **Múltiples arenas:** Puedes crear varias arenas y el sistema seleccionará una aleatoria
- ✅ **Persistencia:** Las arenas se guardan en `robarcabeza_arenas.yml`
- ✅ **Escalabilidad:** Soporta cualquier número de jugadores (limitado por los spawns)
- ✅ **Compatibilidad:** Funciona con el sistema de puntuación global del torneo

---

## 🆘 Soporte

Si encuentras problemas:
1. Verifica los logs del servidor
2. Asegúrate de tener permisos de administrador
3. Verifica que las arenas estén correctamente configuradas con `/robarcabeza admin info <arena>`

---

**¡Disfruta del minijuego RobarCabeza! 🎉**
