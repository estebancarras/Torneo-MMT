package los5fantasticos.minigameCadena.visuals

import los5fantasticos.minigameCadena.MinigameCadena
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f

/**
 * Representa una cadena visual entre dos jugadores usando ItemDisplay.
 */
class VisualChain(
    private val plugin: MinigameCadena,
    private val playerA: Player,
    private val playerB: Player
) {
    private var itemDisplay: ItemDisplay? = null
    private var updateTask: BukkitTask? = null
    
    private val updatePeriodTicks = plugin.plugin.config.getLong("visuales.cadena.tick-rate", 2L)
    
    fun create() {
        val materialName = plugin.plugin.config.getString("visuales.cadena.material", "CHAIN")
        val material = try {
            Material.valueOf(materialName ?: "CHAIN")
        } catch (e: IllegalArgumentException) {
            plugin.plugin.logger.warning("Material inválido '$materialName'. Usando CHAIN.")
            Material.CHAIN
        }
        
        // Crear ItemDisplay en la posición del jugador A
        itemDisplay = playerA.world.spawn(playerA.location, ItemDisplay::class.java) { display ->
            display.itemStack = ItemStack(material)
            display.billboard = Display.Billboard.FIXED
            display.interpolationDelay = -1
            display.interpolationDuration = 1
            display.viewRange = 128.0f
            display.brightness = Display.Brightness(15, 15)
        }
        
        plugin.plugin.logger.info("[VisualChain] Cadena creada: ${playerA.name} <-> ${playerB.name}")
        
        // Actualizar inmediatamente la primera vez
        updateTransformation()
        
        startUpdating()
    }
    
    private fun startUpdating() {
        updateTask = object : BukkitRunnable() {
            override fun run() {
                if (!playerA.isOnline || !playerB.isOnline) {
                    destroy()
                    return
                }
                updateTransformation()
            }
        }.runTaskTimer(plugin.plugin, 0L, updatePeriodTicks)
    }
    
    private fun updateTransformation() {
        val display = itemDisplay ?: return
        
        // Calcular punto medio entre jugadores (con ajuste de altura)
        val locA = playerA.location.clone().add(0.0, 1.0, 0.0)
        val locB = playerB.location.clone().add(0.0, 1.0, 0.0)
        
        val midX = (locA.x + locB.x) / 2.0
        val midY = (locA.y + locB.y) / 2.0
        val midZ = (locA.z + locB.z) / 2.0
        
        // Teleportar la entidad al punto medio
        val midLocation = Location(playerA.world, midX, midY, midZ)
        display.teleport(midLocation)
        
        // Calcular distancia
        val distance = locA.distance(locB).toFloat()
        
        // Calcular dirección
        val dx = (locB.x - locA.x).toFloat()
        val dy = (locB.y - locA.y).toFloat()
        val dz = (locB.z - locA.z).toFloat()
        val direction = Vector3f(dx, dy, dz).normalize()
        
        // Transformación LOCAL (sin traslación, ya que usamos teleport)
        val translation = Vector3f(0f, 0f, 0f)
        val scale = Vector3f(0.2f, 0.2f, distance)
        
        val sourceVector = Vector3f(0.0f, 0.0f, 1.0f)
        val leftRot = Quaternionf().rotationTo(sourceVector, direction)
        val rightRot = Quaternionf().rotateX(Math.toRadians(90.0).toFloat())
        
        val transformation = Transformation(translation, leftRot, scale, rightRot)
        display.transformation = transformation
    }
    
    fun destroy() {
        updateTask?.let { task ->
            if (!task.isCancelled) {
                task.cancel()
            }
        }
        updateTask = null
        
        itemDisplay?.remove()
        itemDisplay = null
        
        plugin.plugin.logger.info("[VisualChain] Cadena destruida: ${playerA.name} <-> ${playerB.name}")
    }
}
