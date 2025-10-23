package yo.spray.robarCabeza.services

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Display.Billboard
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f

/**
 * Servicio de visualización de cabezas gigantes.
 * 
 * Responsabilidades:
 * - Crear y gestionar el ItemDisplay de la cabeza gigante
 * - Posicionar y rotar la cabeza siguiendo al jugador portador
 * - Aplicar la skin de un creador aleatorio
 * - Animar la cabeza en tiempo real
 */
class HeadVisualService(private val plugin: Plugin) {
    
    /**
     * Entidad ItemDisplay activa que representa la cabeza gigante.
     */
    private var activeVisual: ItemDisplay? = null
    
    /**
     * Jugador que actualmente porta la cabeza.
     */
    private var carrier: Player? = null
    
    /**
     * Tarea de animación que actualiza la posición y rotación cada tick.
     */
    private var animationTask: BukkitTask? = null
    
    /**
     * Factor de escala de la cabeza (1.0 = tamaño normal).
     */
    private var headScale: Float = 1.5f
    
    /**
     * Offset vertical desde la altura de los ojos del jugador.
     */
    private var yOffset: Double = -0.25
    
    /**
     * Lista de nombres de jugadores cuyas skins se usarán para las cabezas.
     */
    private var creatorHeads: List<String> = listOf("Notch")
    
    /**
     * Configura los parámetros visuales del servicio.
     * 
     * @param scale Factor de escala de la cabeza
     * @param offset Offset vertical desde los ojos del jugador
     * @param heads Lista de nombres de jugadores para las skins
     */
    fun configure(scale: Float, offset: Double, heads: List<String>) {
        this.headScale = scale
        this.yOffset = offset
        this.creatorHeads = heads.ifEmpty { listOf("Notch") }
    }
    
    /**
     * Asigna la cabeza gigante a un nuevo portador.
     * 
     * @param newCarrier Jugador que recibirá la cabeza
     */
    fun setCarrier(newCarrier: Player) {
        // Limpiar cualquier efecto anterior
        removeCarrier()
        
        // Asignar nuevo portador
        this.carrier = newCarrier
        
        // Seleccionar un creador aleatorio
        val creatorName = creatorHeads.random()
        
        // Crear el ItemStack de la cabeza con la skin del creador
        val headItem = ItemStack(Material.PLAYER_HEAD)
        val meta = headItem.itemMeta as SkullMeta
        meta.owningPlayer = Bukkit.getOfflinePlayer(creatorName)
        headItem.itemMeta = meta
        
        // Crear la entidad ItemDisplay
        val spawnLocation = newCarrier.eyeLocation.add(0.0, yOffset, 0.0)
        val display = newCarrier.world.spawn(spawnLocation, ItemDisplay::class.java)
        
        // Configurar el ItemDisplay
        display.itemStack = headItem
        display.billboard = Billboard.FIXED
        
        // Aplicar transformación inicial con escala
        val initialTransformation = Transformation(
            Vector3f(0f, 0f, 0f),  // translation
            Quaternionf(),          // leftRotation
            Vector3f(headScale, headScale, headScale),  // scale
            Quaternionf()           // rightRotation
        )
        display.transformation = initialTransformation
        
        // Guardar referencia
        this.activeVisual = display
        
        // Iniciar animación
        startAnimationTask()
    }
    
    /**
     * Inicia la tarea de animación que actualiza la posición y rotación cada tick.
     */
    private fun startAnimationTask() {
        animationTask = object : BukkitRunnable() {
            override fun run() {
                val currentCarrier = carrier
                val visual = activeVisual
                
                // Verificar que el portador y la entidad existan
                if (currentCarrier == null || !currentCarrier.isOnline || visual == null) {
                    cancel()
                    return
                }
                
                // Calcular posición precisa: ojos del jugador + offset
                val location = currentCarrier.eyeLocation.add(0.0, yOffset, 0.0)
                
                // Teletransportar el ItemDisplay a la nueva posición
                visual.teleport(location)
                
                // Actualizar rotación para que coincida con la del jugador
                val yawRadians = Math.toRadians(currentCarrier.location.yaw.toDouble()).toFloat()
                val rotation = Quaternionf().rotateY(yawRadians)
                
                // Mantener la escala actual
                val currentTransformation = visual.transformation
                visual.transformation = Transformation(
                    currentTransformation.translation,
                    rotation,
                    currentTransformation.scale,
                    currentTransformation.rightRotation
                )
            }
        }.runTaskTimer(plugin, 0L, 1L)  // Ejecutar cada tick
    }
    
    /**
     * Remueve la cabeza del portador actual y limpia todos los recursos.
     */
    fun removeCarrier() {
        // Cancelar tarea de animación
        animationTask?.cancel()
        animationTask = null
        
        // Eliminar entidad ItemDisplay
        activeVisual?.remove()
        activeVisual = null
        
        // Limpiar referencia al portador
        carrier = null
    }
    
    /**
     * Limpia todos los recursos del servicio.
     */
    fun cleanup() {
        removeCarrier()
    }
}
