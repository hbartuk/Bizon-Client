package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.data.skin.SkinCache
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket // Этот импорт оставляем, он нужен для типа пакета
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
// import org.cloudburstmc.protocol.bedrock.data.PlayerListEntry // <-- Эту строку убираем!
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin

class SkinStealerModule : Module("skinstealer", ModuleCategory.Misc) {

    init {
        sendClientMessage("§aSkinStealer инициализирован. Используйте .skin <ник> для смены скина.")
    }

    fun applySkin(targetNick: String) {
        if (!isEnabled) {
            sendClientMessage("§cМодуль SkinStealer выключен!")
            return
        }

        sendClientMessage("§eПытаюсь получить скин для игрока: §b$targetNick...")
        
        val normalizedTargetNick = targetNick.lowercase()
        val skin: SerializedSkin? = SkinCache.getSkin(normalizedTargetNick)

        if (skin == null) {
            sendClientMessage("§cСкин игрока '$targetNick' не найден в кэше! Игрок должен быть на сервере, и его данные должны быть загружены.")
            return
        }

        try {
            val packet = PlayerSkinPacket().apply {
                uuid = session.localPlayer.uuid
                this.skin = skin
            }
            session.serverBound(packet)

            sendClientMessage("§aСкин успешно изменён на скин игрока §b$targetNick!§a")
        } catch (e: Exception) {
            sendClientMessage("§cОшибка смены скина: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        if (packet is PlayerListPacket) {
            packet.entries.forEach { entry -> // <-- Здесь 'entry' все равно будет иметь тип PlayerListEntry
                val entrySkin: SerializedSkin? = entry.skin
                val entryName: String = entry.name 

                if (entrySkin != null && entryName.isNotBlank()) {
                    SkinCache.putSkin(entryName.lowercase(), entrySkin)
                }
            }
        }

        if (packet is TextPacket && packet.type == TextPacket.Type.CHAT) {
            val message = packet.message.trim()
            if (message.startsWith(".skin ", ignoreCase = true)) {
                val args = message.split("\\s+".toRegex())
                if (args.size == 2) {
                    val targetNick = args[1] 
                    applySkin(targetNick) 
                    // Если setCancelled() не работает, этот блок останется,
                    // и команда будет видна в чате.
                    // Если у тебя есть способ отмены, используй его здесь:
                    // interceptablePacket.setCancelled(true) // Или .cancel(), или .isCancelled = true
                } else {
                    sendClientMessage("§cИспользование: .skin <ник>")
                    // interceptablePacket.setCancelled(true)
                }
            }
        }
    }

    private fun sendClientMessage(msg: String) {
        session.displayClientMessage(msg)
    }
}
