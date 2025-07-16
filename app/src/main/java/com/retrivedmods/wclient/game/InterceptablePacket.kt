// File: app/src/main/java/com/retrivedmods/wclient/game/InterceptablePacket.kt
package com.retrivedmods.wclient.game

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket

interface InterceptablePacket {
    val packet: BedrockPacket
    // Метод fun setPacket(newPacket: BedrockPacket) должен быть УДАЛЕН отсюда
}
