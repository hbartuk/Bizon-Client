package com.retrivedmods.wclient.game

import com.retrivedmods.wclient.application.AppContext
import com.retrivedmods.wclient.game.entity.LocalPlayer
import com.retrivedmods.wclient.game.world.Level
import com.mucheng.mucute.relay.MuCuteRelaySession
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket

@Suppress("MemberVisibilityCanBePrivate")
class GameSession(val muCuteRelaySession: MuCuteRelaySession) : ComposedPacketHandler {

    val localPlayer = LocalPlayer(this)

    val level = Level(this)

    private val versionName by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppContext.instance.packageManager.getPackageInfo(
            AppContext.instance.packageName, 0
        ).versionName
    }

    fun clientBound(packet: BedrockPacket) {
        muCuteRelaySession.clientBound(packet)
    }

    fun serverBound(packet: BedrockPacket) {
        muCuteRelaySession.serverBound(packet)
    }

    override fun beforePacketBound(packet: BedrockPacket): Boolean {
        localPlayer.onPacketBound(packet)
        level.onPacketBound(packet)

        val interceptablePacket = InterceptablePacket(packet)

        // --- Packet processing by modules ---
        for (module in ModuleManager.modules) {
            // Set the session for the module before using it.
            module.session = this 
            
            module.beforePacketBound(interceptablePacket)
            if (interceptablePacket.isIntercepted) {
                return true
            }
        }

        // --- Add command processing logic here ---
        if (packet is TextPacket && packet.type == TextPacket.Type.CHAT) {
            val message = packet.message.trim()
            if (message.startsWith(".")) { // Check if it starts with a command prefix
                // Split the message into command name and arguments
                val parts = message.substring(1).split(" ", limit = 2) 
                val commandName = parts[0].lowercase() // Command name in lowercase
                // Arguments: if only command name is present, no arguments
                val args = if (parts.size > 1) parts[1].split(" ").toTypedArray() else emptyArray()

                val command = ModuleManager.getCommand(commandName) // Search for the command in ModuleManager
                if (command != null) {
                    // Command found, execute it
                    command.exec(args, this) // 'this' here is the current GameSession
                    // After executing the command, intercept the packet so it doesn't show in chat
                    interceptablePacket.intercept() // <-- FIX IS HERE!
                    return true // Interrupt further packet processing, as the command is handled
                } else {
                    // Command not found - send an error message
                    displayClientMessage("§cUnknown command: §f.$commandName")
                    // Also intercept the packet so the unknown command doesn't show in chat
                    interceptablePacket.intercept() // <-- FIX IS HERE!
                    return true
                }
            }
        }
        // --- End of command processing logic ---

        return false
    }

    override fun afterPacketBound(packet: BedrockPacket) {
        for (module in ModuleManager.modules) {
            module.afterPacketBound(packet)
        }
    }

    override fun onDisconnect(reason: String) {
        localPlayer.onDisconnect()
        level.onDisconnect()

        for (module in ModuleManager.modules) {
            module.onDisconnect(reason)
        }
    }

    // Your existing displayClientMessage method
    fun displayClientMessage(message: String, type: TextPacket.Type = TextPacket.Type.RAW) {
        val textPacket = TextPacket()
        textPacket.type = type
        textPacket.isNeedsTranslation = false
        textPacket.sourceName = ""
        textPacket.message = message
        textPacket.xuid = ""
        textPacket.platformChatId = ""
        textPacket.filteredMessage = ""
        clientBound(textPacket)
    }

}
