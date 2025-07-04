package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.WClient
import com.retrivedmods.wclient.game.data.skin.SkinCache
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket

class SkinStealerModule : Module("skinstealer", ModuleCategory.Misc) {

    // Вызови этот метод из обработчика чата, если модуль включён
    fun handleChatCommand(message: String) {
        if (!isEnabled) return

        val args = message.trim().split("\\s+".toRegex())
        if (args.isEmpty() || args[0] != ".skin") return

        if (args.size != 2) {
            sendClientMessage("§cUsage: .skin <ник>")
            return
        }

        val targetNick = args[1]
        val skin = SkinCache.getSkin(targetNick)

        if (skin == null) {
            sendClientMessage("§cСкин игрока '$targetNick' не найден в кэше! Игрок должен быть онлайн через прокси.")
            return
        }

        try {
            val packet = PlayerSkinPacket().apply {
                uuid = session.profile.uuid
                this.skin = skin
            }
            session.clientBound(packet)
            sendClientMessage("§aСкин успешно изменён на скин $targetNick!")
        } catch (e: Exception) {
            sendClientMessage("§cОшибка смены скина: ${e.message}")
        }
    }

    // Обязательный метод (можно оставить пустым)
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {}

    // Вспомогательный метод для вывода сообщений игроку
    private fun sendClientMessage(msg: String) {
        // Реализуй по аналогии с другими модулями (например, PlayerTracerModule)
        // Например: session.sendMessageToChat(msg)
    }
}
