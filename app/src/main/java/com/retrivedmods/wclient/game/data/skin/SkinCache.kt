package com.retrivedmods.wclient.game.data.skin

import org.cloudburstmc.protocol.bedrock.data.skin.Skin

object SkinCache {
    private val skinMap: MutableMap<String, Skin> = mutableMapOf()

    fun putSkin(nick: String, skin: Skin) {
        skinMap[nick.lowercase()] = skin
    }

    fun getSkin(nick: String): Skin? {
        return skinMap[nick.lowercase()]
    }
}
