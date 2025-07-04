package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.data.skin.SkinCache
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import org.cloudburstmc.protocol.bedrock.data.PlayerListEntry
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin

class SkinStealerModule : Module("skinstealer", ModuleCategory.Misc) {

    init {
        sendClientMessage("§aSkinStealer инициализирован. Используйте .skin <ник> для смены скина.")
    }

    /**
     * Применяет скин указанного игрока к текущему игроку.
     * Скин берется из SkinCache.
     * @param targetNick Ник игрока, скин которого нужно "украсть".
     */
    fun applySkin(targetNick: String) {
        if (!isEnabled) {
            sendClientMessage("§cМодуль SkinStealer выключен!")
            return
        }

        sendClientMessage("§eПытаюсь получить скин для игрока: §b$targetNick...")
        
        // **Приводим ник к нижнему регистру перед поиском в кэше**
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

    /**
     * Перехватывает входящие и исходящие пакеты для обработки.
     * Заполняет SkinCache и обрабатывает команды чата.
     * @param interceptablePacket Пакет, который можно перехватить и, возможно, отменить.
     */
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        // Обрабатываем PlayerListPacket для заполнения SkinCache.
        if (packet is PlayerListPacket) {
            packet.entries.forEach { entry ->
                val entrySkin: SerializedSkin? = entry.skin
                val entryName: String = entry.name 

                // **Приводим ник к нижнему регистру перед сохранением в кэше**
                if (entrySkin != null && entryName.isNotBlank()) {
                    SkinCache.putSkin(entryName.lowercase(), entrySkin)
                }
            }
        }

        // Обрабатываем TextPacket (сообщения чата) для вызова команды .skin.
        if (packet is TextPacket && packet.type == TextPacket.Type.CHAT) {
            val message = packet.message.trim()
            if (message.startsWith(".skin ", ignoreCase = true)) {
                val args = message.split("\\s+".toRegex())
                if (args.size == 2) {
                    val targetNick = args[1] 
                    applySkin(targetNick) 
                    interceptablePacket.setCancelled(true) 
                } else {
                    sendClientMessage("§cИспользование: .skin <ник>")
                    interceptablePacket.setCancelled(true)
                }
            }
        }
    }

    /**
     * Отправляет сообщение в клиентский чат.
     * @param msg Сообщение для отображения.
     */
    private fun sendClientMessage(msg: String) {
        session.displayClientMessage(msg)
    }
}
