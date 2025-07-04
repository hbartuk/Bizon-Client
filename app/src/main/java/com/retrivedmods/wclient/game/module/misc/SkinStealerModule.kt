package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.data.skin.SkinCache
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin

class SkinStealerModule : Module("skinstealer", ModuleCategory.Misc) {

    fun handleChatCommand(message: String) {
        if (!isEnabled) return

        val args = message.trim().split("\\s+".toRegex())
        if (args.isEmpty() || args[0] != ".skin") return

        if (args.size != 2) {
            sendClientMessage("§cUsage: .skin <ник>")
            return
        }

        val targetNick = args[1]
        val skin: SerializedSkin? = SkinCache.getSkin(targetNick)

        if (skin == null) {
            sendClientMessage("§cСкин игрока '$targetNick' не найден в кэше! Игрок должен быть онлайн через прокси.")
            return
        }

        try {
            // Используй session из Module (у тебя он, скорее всего, protected или public)
            val packet = PlayerSkinPacket().apply {
                uuid = session.localPlayer.uuid
                this.skin = skin
            }
            session.clientBound(packet)
            sendClientMessage("§aСкин успешно изменён на скин $targetNick!")
        } catch (e: Exception) {
            sendClientMessage("§cОшибка смены скина: ${e.message}")
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {}

    private fun sendClientMessage(msg: String) {
        session.displayClientMessage(msg)
    }
}
