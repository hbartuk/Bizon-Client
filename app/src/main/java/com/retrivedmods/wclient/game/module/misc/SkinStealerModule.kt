package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.data.skin.SkinCache
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import org.cloudburstmc.protocol.bedrock.data.PlayerListEntry // <- THIS IMPORT IS CRITICAL. Double-check your build.gradle and clean!
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin

class SkinStealerModule : Module("skinstealer", ModuleCategory.Misc) {

    init {
        sendClientMessage("§aSkinStealer initialized. Use .skin <nickname> to change skin.")
    }

    /**
     * Applies the skin of the specified player to the current player.
     * The skin is retrieved from the SkinCache.
     * @param targetNick The nickname of the player whose skin to "steal".
     */
    fun applySkin(targetNick: String) {
        if (!isEnabled) {
            sendClientMessage("§cSkinStealer module is disabled!")
            return
        }

        sendClientMessage("§eAttempting to get skin for player: §b$targetNick...")
        
        // Normalize the nickname to lowercase for consistent cache lookups.
        val normalizedTargetNick = targetNick.lowercase()
        val skin: SerializedSkin? = SkinCache.getSkin(normalizedTargetNick)

        if (skin == null) {
            sendClientMessage("§cSkin for player '$targetNick' not found in cache! Player must be on the server and their data loaded.")
            return
        }

        try {
            // Create a PlayerSkinPacket to change the skin.
            val packet = PlayerSkinPacket().apply {
                uuid = session.localPlayer.uuid // Use the UUID of the current local player.
                this.skin = skin // Assign the "stolen" skin.
            }
            // Send the packet to the server. The server should then broadcast this change to other players.
            session.serverBound(packet)

            sendClientMessage("§aSkin successfully changed to §b$targetNick's§a skin!")
        } catch (e: Exception) {
            // Handle any errors during the skin change process.
            sendClientMessage("§cSkin change error: ${e.message}")
            e.printStackTrace() // Print stack trace for detailed debugging.
        }
    }

    /**
     * Intercepts incoming and outgoing packets for processing.
     * Populates the SkinCache and handles chat commands.
     * @param interceptablePacket The packet that can be intercepted.
     */
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        // Process PlayerListPacket to populate the SkinCache.
        if (packet is PlayerListPacket) {
            packet.entries.forEach { entry ->
                val entrySkin: SerializedSkin? = entry.skin // Get the skin from the player list entry.
                val entryName: String = entry.name // Get the player's nickname from the entry.

                // If skin and name are valid, add them to the cache, converting name to lowercase.
                if (entrySkin != null && entryName.isNotBlank()) {
                    SkinCache.putSkin(entryName.lowercase(), entrySkin)
                }
            }
        }

        // Process TextPacket (chat messages) to handle the .skin command.
        if (packet is TextPacket && packet.type == TextPacket.Type.CHAT) {
            val message = packet.message.trim()
            // Check if the message starts with ".skin " (case-insensitive).
            if (message.startsWith(".skin ", ignoreCase = true)) {
                val args = message.split("\\s+".toRegex()) // Split the message into arguments.
                if (args.size == 2) {
                    val targetNick = args[1] // Get the target nickname from arguments.
                    applySkin(targetNick) // Call the skin change function.
                    // REMOVED: Packet cancellation logic, as no valid method was found in InterceptablePacket.
                    // The command message will now be visible in chat.
                } else {
                    sendClientMessage("§cUsage: .skin <nickname>")
                    // REMOVED: Packet cancellation logic.
                }
            }
        }
    }

    /**
     * Sends a message to the client's chat.
     * @param msg The message to display.
     */
    private fun sendClientMessage(msg: String) {
        session.displayClientMessage(msg)
    }
}
