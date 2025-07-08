// File: com.retrivedmods.wclient.game.GameSession.kt

package com.retrivedmods.wclient.game

import com.mucheng.mucute.relay.MuCuteRelaySession
// --- ADD THESE IMPORTS ---
import com.mucheng.mucute.relay.listener.PacketListener // Assuming this is the correct path for PacketListener
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket // This is typically the base class for all Bedrock packets
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.RespawnPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket // For displayClientMessage
// --- END ADD THESE IMPORTS ---

class GameSession(
    val muCuteRelaySession: MuCuteRelaySession
) : PacketListener { // Ensure PacketListener is properly imported

    var playerX: Double = 0.0
    var playerY: Double = 0.0
    var playerZ: Double = 0.0

    fun displayClientMessage(message: String) {
        println("CLIENT MESSAGE: $message")
        val textPacket = TextPacket().apply {
            type = TextPacket.Type.CHAT // Or TextPacket.Type.SYSTEM_MESSAGE, depending on desired display
            this.message = message
        }
        muCuteRelaySession.clientBound(textPacket) // Use muCuteRelaySession to send
    }

    override fun onPacketIn(packet: BedrockPacket) { // Change 'Packet' to 'BedrockPacket' (or whatever base class CloudburstMC uses)
        when (packet) {
            is MovePlayerPacket -> {
                playerX = packet.position.x.toDouble()
                playerY = packet.position.y.toDouble()
                playerZ = packet.position.z.toDouble()
            }
            is RespawnPacket -> {
                playerX = packet.position.x.toDouble()
                playerY = packet.position.y.toDouble()
                playerZ = packet.position.z.toDouble()
            }
            // ... other packets
        }
    }

    override fun onPacketOut(packet: BedrockPacket) { // Change 'Packet' to 'BedrockPacket'
        // ... logic for outgoing packets
    }
}
