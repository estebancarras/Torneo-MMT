# ⚡ Comandos Rápidos - RobarCabeza

## 🚀 Setup Rápido (5 minutos)

### 1️⃣ Crear Arena
```bash
/robarcabeza admin create mi_arena
```

### 2️⃣ Añadir Spawns (mínimo 4)
```bash
# Párate en cada ubicación y ejecuta:
/robarcabeza admin addspawn mi_arena
/robarcabeza admin addspawn mi_arena
/robarcabeza admin addspawn mi_arena
/robarcabeza admin addspawn mi_arena
```

### 3️⃣ Definir Región
```bash
# Obtener varita
/torneo wand

# Seleccionar área (clic izq + clic der)
# Luego:
/robarcabeza admin setregion mi_arena
```

### 4️⃣ Verificar
```bash
/robarcabeza admin info mi_arena
```

### 5️⃣ ¡Jugar!
```bash
# Jugadores:
/robarcabeza join

# Admin inicia:
/robarcabeza admin startgame mi_arena
```

---

## 📋 Comandos Esenciales

### Jugadores
```bash
/robarcabeza join          # Unirse
/robarcabeza leave         # Salir
```

### Administración
```bash
/robarcabeza admin list                    # Ver arenas
/robarcabeza admin info <arena>            # Info de arena
/robarcabeza admin startgame [arena]       # Iniciar juego
/robarcabeza admin stopgame                # Detener juego
```

---

## 🎯 Mecánicas del Juego

- 🎭 **Cabeza en el casco:** Se equipa automáticamente
- 🔒 **No puedes quitártela:** Protección de inventario
- 👊 **Robo por la espalda:** Ataca por detrás para robar
- ⏱️ **Duración:** 2 minutos
- 🏆 **Puntos:** +1/segundo con cabeza, +10 por robo

---

## 🔧 Troubleshooting

**Error: "No hay arenas"**
```bash
/robarcabeza admin create arena1
```

**Error: "Sin spawns"**
```bash
/robarcabeza admin addspawn arena1
```

**Error: "Sin región"**
```bash
/torneo wand
# Seleccionar área
/robarcabeza admin setregion arena1
```

---

## 📊 Ejemplo Completo

```bash
# Setup
/robarcabeza admin create arena_test
/robarcabeza admin addspawn arena_test  # x4 veces
/torneo wand
# [Seleccionar área]
/robarcabeza admin setregion arena_test

# Verificar
/robarcabeza admin info arena_test

# Jugar
/robarcabeza join
/robarcabeza admin startgame arena_test
```

---

**¡Listo para jugar! 🎮**
