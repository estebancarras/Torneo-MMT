package los5fantasticos.minigameSkywars.commands

import los5fantasticos.minigameSkywars.MinigameSkywars
import los5fantasticos.torneo.TorneoPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import com.walrusone.skywarsreloaded.SkyWarsReloaded
import com.walrusone.skywarsreloaded.managers.MatchManager
import com.walrusone.skywarsreloaded.enums.GameType

/**
 * Ejecuta subcomandos para el minijuego SkyWars relacionados con el módulo Torneo.
 * Añade el subcomando: /skywars joinall [map] -> envía a TODOS los jugadores sin permisos de administrador al mapa indicado (o a uno disponible).
 */
class SkywarsCommand(
    private val manager: MinigameSkywars,
    private val torneoPlugin: TorneoPlugin
) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "joinall" -> return handleJoinAll(sender, args)
            else -> {
                sender.sendMessage(Component.text("Subcomando desconocido. Usa /skywars para ver ayuda.", NamedTextColor.RED))
                return true
            }
        }
    }

    private fun showHelp(sender: CommandSender) {
    sender.sendMessage(Component.text("§6SkyWars (Torneo) - Comandos", NamedTextColor.GOLD))
    sender.sendMessage(Component.text("/skywars joinall [map] [incluirAdmins] §7- Envía a todos los jugadores sin permisos de administrador (o a todos si incluirAdmins=true) al mapa opcionalmente indicado", NamedTextColor.YELLOW))
    }

    private fun checkAdmin(sender: CommandSender): Boolean {
        if (sender.hasPermission("torneo.admin") || sender.isOp) return true
        sender.sendMessage(Component.text("No tienes permiso para ejecutar este comando.", NamedTextColor.RED))
        return false
    }

    private fun handleJoinAll(sender: CommandSender, args: Array<out String>): Boolean {
        if (!checkAdmin(sender)) return true

        val mapName: String? = if (args.size >= 2) args[1].takeIf { !it.equals("true", true) && !it.equals("false", true) } else null
        val includeAdmins: Boolean =
            if (args.size >= 3) args[2].equals("true", true)
            else if (args.size == 2 && (args[1].equals("true", true) || args[1].equals("false", true))) args[1].equals("true", true)
            else false

        val targets = if (includeAdmins) {
            Bukkit.getOnlinePlayers().toList()
        } else {
            Bukkit.getOnlinePlayers().filter { !it.hasPermission("torneo.admin") }
        }

        if (targets.isEmpty()) {
            sender.sendMessage(Component.text("No se encontraron jugadores${if (includeAdmins) " conectados" else " sin permisos de administrador"}.", NamedTextColor.YELLOW))
            return true
        }

        var success = 0
        var failed = 0

        if (mapName != null) {
            val gMap = SkyWarsReloaded.getGameMapMgr().getMap(mapName)
            if (gMap == null) {
                sender.sendMessage(Component.text("Mapa '$mapName' no encontrado.", NamedTextColor.RED))
                return true
            }

            val alreadyIn = gMap.getAllPlayers()
            for (p in targets) {
                try {
                    // Saltar si el jugador ya está en la partida/mapa
                    if (alreadyIn.contains(p)) {
                        p.sendMessage(Component.text("§6[SkyWars] §eYa estás en la partida ${gMap.name}, skippeado.", NamedTextColor.YELLOW))
                        continue
                    }

                    val added = gMap.addPlayers(null, p)
                    if (added) {
                        p.sendMessage(Component.text("§6[SkyWars] §aHas sido enviado al mapa ${gMap.name}.", NamedTextColor.GREEN))
                        success++
                    } else {
                        p.sendMessage(Component.text("§6[SkyWars] §cNo fue posible unirte al mapa ${gMap.name}.", NamedTextColor.RED))
                        failed++
                    }
                } catch (e: Exception) {
                    torneoPlugin.logger.warning("Error intentando unir a ${p.name} al mapa $mapName: ${e.message}")
                    failed++
                }
            }
        } else {
            // Join automático usando MatchManager
            for (p in targets) {
                try {
                    val joinedMap = MatchManager.get().joinGame(p, GameType.ALL)
                    if (joinedMap != null) {
                        p.sendMessage(Component.text("§6[SkyWars] §aHas sido enviado a ${joinedMap.name}.", NamedTextColor.GREEN))
                        success++
                    } else {
                        p.sendMessage(Component.text("§6[SkyWars] §cNo fue posible encontrar una partida para unirte.", NamedTextColor.RED))
                        failed++
                    }
                } catch (e: Exception) {
                    torneoPlugin.logger.warning("Error intentando unir a ${p.name} a SkyWars: ${e.message}")
                    failed++
                }
            }
        }

        sender.sendMessage(Component.text("Operación completada. Enviados: $success. Fallidos: $failed.", NamedTextColor.YELLOW))
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String> {
        val completions = mutableListOf<String>()
        if (args.size == 1) {
            listOf("joinall").forEach { if (it.startsWith(args[0].lowercase())) completions.add(it) }
        } else if (args.size == 2 && args[0].lowercase() == "joinall") {
            try {
                val maps = SkyWarsReloaded.getGameMapMgr().getMapsCopy()
                maps.forEach { if (it.name.lowercase().startsWith(args[1].lowercase())) completions.add(it.name) }
            } catch (_: Exception) {
                // ignore
            }
            completions.addAll(listOf("true", "false").filter { it.startsWith(args[1].lowercase()) })
        } else if (args.size == 3 && args[0].lowercase() == "joinall") {
            completions.addAll(listOf("true", "false").filter { it.startsWith(args[2].lowercase()) })
        }
        return completions
    }
}
