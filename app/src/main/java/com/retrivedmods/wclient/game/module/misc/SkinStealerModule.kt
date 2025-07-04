package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.data.skin.SkinCache
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin

class SkinStealerModule : Module("skinstealer", ModuleCategory.Misc) {
    init {
        sendClientMessage("Debug: SkinStealerModule initialized, enabled: $isEnabled")
    }

    // Обработка команды /skin
    fun handleChatCommand(message: String) {
        sendClientMessage("Debug: Received message: $message")
        if (!isEnabled) {
            sendClientMessage("Debug: SkinStealerModule is disabled")
            return
        }
        val args = message.trim().split("\\s+".toRegex())
        if (args.isEmpty() || args[0] != "/skin") {
            sendClientMessage("Debug: Not a /skin command")
            return
        }
        if (args.size != 2) {
            sendClientMessage("§cUsage: /skin <ник>")
            return
        }
        applySkin(args[1])
    }

    // Логика применения скина
    private fun applySkin(targetNick: String) {
        sendClientMessage("Debug: Attempting to apply skin for $targetNick")
        val skin: SerializedSkin? = SkinCache.getSkin(targetNick)
        if (skin == null) {
            sendClientMessage("§cСкин игрока '$targetNick' не найден в кэше! Игрок должен быть на сервере.")
            return
        }
        try {
            val packet = PlayerSkinPacket().apply {
                uuid = session.localPlayer.uuid
                this.skin = skin
            }
            sendClientMessage("Debug: Sending skin packet for $targetNick, UUID: ${session.localPlayer.uuid}")
            session.serverBound(packet)
            session.clientBound(packet)
            sendClientMessage("§aСкин успешно изменён на скин $targetNick!")
        } catch (e: Exception) {
            sendClientMessage("§cОшибка смены скина: ${e.message}")
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return
        val packet = interceptablePacket.packet
        // Заполнение SkinCache из PlayerListPacket
        if (packet is PlayerListPacket) {
            packet.entries.forEach { entry ->
                SkinCache.putSkin(entry.name, entry.skin) // Вернулся к name
                sendClientMessage("Debug: Added skin for ${entry.name} to SkinCache")
            }
        }
        // Перехват чат-команд
        if (packet is TextPacket && packet.type == TextPacket.Type.CHAT && packet.sourceName == session.localPlayer.name) {
            handleChatCommand(packet.message)
            interceptablePacket.isCancelled = true // Попробуем isCancelled
            // Если isCancelled не работает, раскомментируй:
            // interceptablePacket.cancelled = true
        }
    }

    private fun sendClientMessage(msg: String) {
        session.displayClientMessage(msg)
    }
}
