package com.retrivedmods.wclient.game.data.skin

import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin

object SkinCache {
    private val skinMap: MutableMap<String, SerializedSkin> = mutableMapOf()

    fun putSkin(nick: String, skin: SerializedSkin) {
        skinMap[nick.lowercase()] = skin
    }

    fun getSkin(nick: String): SerializedSkin? {
        return skinMap[nick.lowercase()]
    }
}
