package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.ModuleManager
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import com.retrivedmods.wclient.game.command.Command

// Updated Placeholder for BaritoneModule
class BaritoneModule : Module("baritone", ModuleCategory.Misc) {
    fun handleGotoCommand(message: String) { session.displayClientMessage("§7[Baritone] Goto command received: $message"); }

    // Implement abstract members from Module
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        // No operation for placeholder
    }
    override fun afterPacketBound(packet: org.cloudburstmc.protocol.bedrock.packet.BedrockPacket) {
        // No operation for placeholder
    }
    override fun onDisconnect(reason: String) {
        // No operation for placeholder
    }
}

// Updated Placeholder for ReplayModule
class ReplayModule : Module("replay", ModuleCategory.Misc) {
    fun startRecording() { session.displayClientMessage("§7[Replay] Recording started."); }
    fun startPlayback() { session.displayClientMessage("§7[Replay] Playback started."); }
    fun stopRecording() { session.displayClientMessage("§7[Replay] Recording stopped."); }
    fun saveReplay(name: String) { session.displayClientMessage("§7[Replay] Replay '$name' saved."); }
    fun loadReplay(name: String) { session.displayClientMessage("§7[Replay] Replay '$name' loaded."); }
    
    // Add the missing stopPlayback method
    fun stopPlayback() { session.displayClientMessage("§7[Replay] Playback stopped."); }

    // Implement abstract members from Module
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        // No operation for placeholder
    }
    override fun afterPacketBound(packet: org.cloudburstmc.protocol.bedrock.packet.BedrockPacket) {
        // No operation for placeholder
    }
    override fun onDisconnect(reason: String) {
        // No operation for placeholder
    }
}


// Main command handler class (no changes here from previous version)
class CommandHandlerModule : Module("command_handler", ModuleCategory.Misc, true, true) {
    private val prefix = "."

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet is TextPacket && packet.type == TextPacket.Type.CHAT) {
            val message = packet.message
            if (!message.startsWith(prefix)) return

            interceptablePacket.intercept()

            val rawArgs = message.substring(prefix.length).trim()
            val parts = rawArgs.split(" ", limit = 2)
            val commandName = parts[0].lowercase()
            val commandArgs = if (parts.size > 1) parts[1].split(" ").toTypedArray() else emptyArray()

            val registeredCommand = ModuleManager.getCommand(commandName)
            if (registeredCommand != null) {
                registeredCommand.exec(commandArgs, session)
                return
            }

            when (commandName) {
                "help" -> {
                    displayHelp(commandArgs.getOrNull(0))
                }
                "goto" -> {
                    val baritoneModule = ModuleManager.modules.find { it is BaritoneModule } as? BaritoneModule
                    if (baritoneModule == null) {
                        session.displayClientMessage("§cBaritone module not found")
                        return
                    }
                    baritoneModule.handleGotoCommand(message)
                }
                "replay" -> {
                    val replayModule = ModuleManager.modules.find { it is ReplayModule } as? ReplayModule
                    if (replayModule == null) {
                        session.displayClientMessage("§cReplay module not found")
                        return
                    }

                    when (commandArgs.getOrNull(0)?.lowercase()) {
                        "record" -> replayModule.startRecording()
                        "play" -> replayModule.startPlayback()
                        "stop" -> {
                            replayModule.stopRecording()
                            // No longer an "Unresolved reference"
                            replayModule.stopPlayback() 
                        }
                        "save" -> {
                            val name = commandArgs.getOrNull(1)
                            if (name == null) {
                                session.displayClientMessage("§cUsage: .replay save <name>")
                                return
                            }
                            replayModule.saveReplay(name)
                        }
                        "load" -> {
                            val name = commandArgs.getOrNull(1)
                            if (name == null) {
                                session.displayClientMessage("§cUsage: .replay load <name>")
                                return
                            }
                            replayModule.loadReplay(name)
                        }
                        else -> {
                            session.displayClientMessage("""
                                §l§b[Replay] §r§7Commands:
                                §f.replay record §7- Start recording
                                §f.replay play §7- Play last recording  
                                §f.replay stop §7- Stop recording/playback
                                §f.replay save <name> §7- Save recording
                                §f.replay load <name> §7- Load recording
                            """.trimIndent())
                        }
                    }
                }
                else -> {
                    val module = ModuleManager.modules.find { it.name.equals(commandName, ignoreCase = true) }
                    if (module != null && !module.private) {
                        module.isEnabled = !module.isEnabled
                        session.displayClientMessage("§l§b[WClient] §r§aModule §f${module.name} §a" + if (module.isEnabled) "включен" else "выключен")
                    } else {
                        session.displayClientMessage("§l§b[WClient] §r§cМодуль или команда не найдены: §f.$commandName")
                    }
                }
            }
        }
    }

    private fun displayHelp(category: String?) {
        val header = """
            §l§b[WClient] §r§7Module List
            §7Commands:
            §f.help <category> §7- View modules in a category
            §f.<module> §7- Toggle a module
            §f.help §7- Show all categories
            §r§7
        """.trimIndent()

        session.displayClientMessage(header)

        if (category != null) {
            try {
                val moduleCategory = ModuleCategory.valueOf(category.uppercase())
                displayCategoryModules(moduleCategory)
            } catch (e: IllegalArgumentException) {
                session.displayClientMessage("§cНеверная категория: $category")
                session.displayClientMessage("§7Доступные категории: ${ModuleCategory.entries.joinToString("§f, §7") { it.name.lowercase() }}")
            }
            return
        }

        ModuleCategory.entries.forEach { cat ->
            displayCategoryModules(cat)
        }
    }

    private fun displayCategoryModules(category: ModuleCategory) {
        val modules = ModuleManager.modules
            .filterNot { it.private }
            .filter { it.category == category }

        if (modules.isEmpty()) return

        session.displayClientMessage("§l§b§m--------------------")
        session.displayClientMessage("§l§b${category.name} Modules:")
        session.displayClientMessage("§r§7")

        modules.chunked(3).forEach { row ->
            val formattedRow = row.joinToString("   ") { module ->
                val status = if (module.isEnabled) "§a✔️" else "§c✘"
                "$status §f${module.name}"
            }
            session.displayClientMessage(formattedRow)
        }

        session.displayClientMessage("§r§7")
    }
}
