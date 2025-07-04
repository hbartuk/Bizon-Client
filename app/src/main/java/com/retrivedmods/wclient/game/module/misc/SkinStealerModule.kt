package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.data.skin.SkinCache // Убедись, что этот класс существует и работает!
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import org.cloudburstmc.protocol.bedrock.data.PlayerListEntry // Это подтвержденный импорт
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin

class SkinStealerModule : Module("skinstealer", ModuleCategory.Misc) {

    init {
        sendClientMessage("§aSkinStealer инициализирован. Используйте .skin <ник>.")
    }

    // Логика применения скина
    fun applySkin(targetNick: String) {
        if (!isEnabled) { // isEnabled - это свойство, унаследованное от Module
            sendClientMessage("§cМодуль SkinStealer выключен!")
            return
        }

        sendClientMessage("§eПытаюсь получить скин для игрока: §b$targetNick...")
        // SkinCache.getSkin(targetNick) должен возвращать SerializedSkin или null.
        // Убедись, что SkinCache правильно заполняется.
        val skin: SerializedSkin? = SkinCache.getSkin(targetNick)

        if (skin == null) {
            sendClientMessage("§cСкин игрока '$targetNick' не найден в кэше! Игрок должен быть на сервере и его данные должны быть загружены.")
            return
        }

        try {
            val packet = PlayerSkinPacket().apply {
                uuid = session.localPlayer.uuid // UUID текущего игрока из сессии
                this.skin = skin
            }
            session.serverBound(packet) // Отправляем пакет на сервер

            sendClientMessage("§aСкин успешно изменён на скин игрока §b$targetNick!§a")

            // Важно: чтобы скин был виден другим, сервер должен разослать PlayerListPacket с обновленным скином.
            // Отправка PlayerSkinPacket на сервер - это то, что мы можем сделать на стороне клиента.
            // Видимость для других зависит от логики сервера.
        } catch (e: Exception) {
            sendClientMessage("§cОшибка смены скина: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        // Заполнение SkinCache из PlayerListPacket
        if (packet is PlayerListPacket) {
            packet.entries.forEach { entry ->
                val entrySkin: SerializedSkin? = entry.skin
                // Используем .name, так как это подтверждено PlayerTracerModule
                val entryName: String = entry.name // Используем свойство name

                if (entrySkin != null && entryName.isNotBlank()) {
                    SkinCache.putSkin(entryName, entrySkin)
                }
            }
        }

        // --- Обработка команд через TextPacket ---
        if (packet is TextPacket && packet.type == TextPacket.Type.CHAT) {
            val message = packet.message.trim()
            if (message.startsWith(".skin ", ignoreCase = true)) {
                val args = message.split("\\s+".toRegex())
                if (args.size == 2) {
                    val targetNick = args[1]
                    applySkin(targetNick)
                    // Используем .cancelled, как это предполагается
                    interceptablePacket.cancelled = true
                } else {
                    sendClientMessage("§cИспользование: .skin <ник>")
                    interceptablePacket.cancelled = true
                }
            }
        }
    }

    private fun sendClientMessage(msg: String) {
        session.displayClientMessage(msg)
    }
}
