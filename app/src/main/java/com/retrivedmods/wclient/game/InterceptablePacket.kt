// File: app/src/main/java/com/retrivedmods/wclient/game/InterceptablePacket.kt
package com.retrivedmods.wclient.game

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket

class InterceptablePacket(val packet: BedrockPacket) {
    var isIntercepted: Boolean = false
        private set

    fun intercept() {
        isIntercepted = true
    }
}
