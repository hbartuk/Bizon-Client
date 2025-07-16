// File: app/src/main/java/com/retrivedmods/wclient/game/InterceptablePacketImpl.kt
package com.retrivedmods.wclient.game

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket

// Это простая реализация InterceptablePacket
class InterceptablePacketImpl(
    override val packet: BedrockPacket,
    override var isIntercepted: Boolean = false // По умолчанию не перехвачен
) : InterceptablePacket
