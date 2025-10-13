package los5fantasticos.minigameCadena.visuals

import los5fantasticos.minigameCadena.MinigameCadena
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * Representa una cadena visual entre dos jugadores.
 * 
 * Esta clase es completamente autónoma y encapsula toda la lógica
 * necesaria para renderizar y actualizar una cadena entre dos puntos
 * en el espacio 3D usando ItemDisplay y transformaciones JOML.
 * 
 * @property plugin Instancia del plugin
 * @property playerA Primer jugador de la cadena
 * @property playerB Segundo jugador de la cadena
 */
class VisualChain(
    private val plugin: MinigameCadena,
    private val playerA: Player,
    private val playerB: Player
) {
    private var itemDisplay: ItemDisplay? = null
    private var updateTask: BukkitTask? = null
    
    // Frecuencia de actualización configurable, por defecto 10 veces/segundo (cada 2 ticks)
    private val updatePeriodTicks = plugin.plugin.config.getLong("visuales.cadena.tick-rate", 2L)
    
    /**
     * Crea la entidad ItemDisplay y comienza su actualización periódica.
     */
    fun create() {
        // Obtener el material desde la configuración
        val materialName = plugin.plugin.config.getString("visuales.cadena.material", "CHAIN")
        val material = try {
            Material.valueOf(materialName ?: "CHAIN")
        } catch (e: IllegalArgumentException) {
            plugin.plugin.logger.warning("Material inválido '$materialName' en configuración. Usando CHAIN por defecto.")
            Material.CHAIN
        }
        
        // Crear el ItemDisplay en la ubicación del jugador A
        itemDisplay = playerA.world.spawn(playerA.location, ItemDisplay::class.java) { display ->
            // Configuración del ItemDisplay
            display.setItemStack(ItemStack(material))
            display.billboard = Display.Billboard.FIXED
            display.interpolationDelay = -1
            display.interpolationDuration = 1
            display.viewRange = 64.0f
        }
        
        // Iniciar la actualización periódica
        startUpdating()
    }
    
    /**
     * Inicia la tarea de actualización periódica de la transformación.
     */
    private fun startUpdating() {
        updateTask = object : BukkitRunnable() {
            override fun run() {
                // Verificar si ambos jugadores siguen online
                if (!playerA.isOnline || !playerB.isOnline) {
                    destroy()
                    return
                }
                
                // Actualizar la transformación
                updateTransformation()
            }
        }.runTaskTimer(plugin.plugin, 0L, updatePeriodTicks)
    }
    
    /**
     * Actualiza la transformación de la entidad ItemDisplay para
     * renderizar la cadena entre los dos jugadores.
     * 
     * Implementa la lógica de transformación 3D según el documento técnico:
     * 1. Traslación al punto medio entre los jugadores
     * 2. Escala para ajustar la longitud de la cadena
     * 3. Rotación para alinear con la dirección de la cadena
     */
    private fun updateTransformation() {
        val display = itemDisplay ?: return
        
        // 1. Convertir localizaciones a vectores JOML
        val locA = Vector3d(
            playerA.location.x,
            playerA.location.y + 1.0, // Ajuste de altura a nivel del torso
            playerA.location.z
        )
        val locB = Vector3d(
            playerB.location.x,
            playerB.location.y + 1.0, // Ajuste de altura a nivel del torso
            playerB.location.z
        )
        
        // 2. TRASLACIÓN: Calcular el punto medio
        val midPoint = Vector3f(
            locA.add(locB).mul(0.5).x.toFloat(),
            locA.y.toFloat(),
            locA.z.toFloat()
        )
        
        // Recalcular locA y locB después de la modificación
        val locAOriginal = Vector3d(
            playerA.location.x,
            playerA.location.y + 1.0,
            playerA.location.z
        )
        val locBOriginal = Vector3d(
            playerB.location.x,
            playerB.location.y + 1.0,
            playerB.location.z
        )
        
        // Calcular punto medio correctamente
        val midPointCalculated = Vector3f(
            ((locAOriginal.x + locBOriginal.x) * 0.5).toFloat(),
            ((locAOriginal.y + locBOriginal.y) * 0.5).toFloat(),
            ((locAOriginal.z + locBOriginal.z) * 0.5).toFloat()
        )
        
        // 3. ESCALA: Calcular la distancia euclidiana
        val distance = locAOriginal.distance(locBOriginal)
        val scale = Vector3f(
            0.1f,  // Grosor de la cadena (eje X)
            0.1f,  // Grosor de la cadena (eje Y)
            distance.toFloat()  // Longitud de la cadena (eje Z)
        )
        
        // 4. ROTACIÓN: Calcular el vector de dirección normalizado
        val direction = Vector3f(
            (locBOriginal.x - locAOriginal.x).toFloat(),
            (locBOriginal.y - locAOriginal.y).toFloat(),
            (locBOriginal.z - locAOriginal.z).toFloat()
        ).normalize()
        
        // Crear el cuaternión de rotación usando lookAlong
        // El vector "up" es (0, 1, 0) por defecto
        val rotation = Quaternionf().lookAlong(direction, Vector3f(0.0f, 1.0f, 0.0f))
        
        // 5. APLICACIÓN: Construir y aplicar la transformación
        val transformation = Transformation(
            midPointCalculated,
            rotation,
            scale,
            Quaternionf() // Sin rotación adicional (identidad)
        )
        
        // Actualizar la entidad con la nueva transformación
        display.transformation = transformation
    }
    
    /**
     * Destruye la cadena visual y limpia todos los recursos.
     * Seguro de llamar múltiples veces.
     */
    fun destroy() {
        // Cancelar la tarea de actualización
        updateTask?.let { task ->
            if (!task.isCancelled) {
                task.cancel()
            }
        }
        updateTask = null
        
        // Eliminar la entidad del mundo
        itemDisplay?.remove()
        itemDisplay = null
    }
}
