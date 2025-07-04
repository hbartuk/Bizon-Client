package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.data.skin.SkinCache
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import org.cloudburstmc.protocol.bedrock.data.PlayerListEntry // Убедись, что этот импорт верный!
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin

class SkinStealerModule : Module("skinstealer", ModuleCategory.Misc) {

    // WClient, похоже, не имеет стандартных onEnable/onDisable в Module.
    // Используем init для инициализации.
    init {
        sendClientMessage("§aSkinStealer инициализирован. Используйте .skin <ник>.")
        // Здесь можно добавить логику активации, если модуль может быть выключен/включен.
    }

    // Если WClient.game.Module не имеет onEnable/onDisable, просто удаляем их.
    // Если есть onActivate/onDeactivate, используй их. Для примера я их убрал.

    // Логика применения скина
    fun applySkin(targetNick: String) {
        // Проверка isEnabled в Module должна быть встроена или сделана через внешнюю проверку
        // Если isEnabled это поле в Module, то оно должно работать.
        if (!isEnabled) {
            sendClientMessage("§cМодуль SkinStealer выключен!")
            return
        }

        sendClientMessage("§eПытаюсь получить скин для игрока: §b$targetNick...")
        val skin: SerializedSkin? = SkinCache.getSkin(targetNick)

        if (skin == null) {
            sendClientMessage("§cСкин игрока '$targetNick' не найден в кэше! Игрок должен быть на сервере и его данные должны быть загружены.")
            return
        }

        try {
            val packet = PlayerSkinPacket().apply {
                uuid = session.localPlayer.uuid // UUID твоего игрока
                this.skin = skin
            }
            session.serverBound(packet) // Отправляем пакет на сервер

            sendClientMessage("§aСкин успешно изменён на скин игрока §b$targetNick!§a")

            // Важно: чтобы скин был виден другим, сервер должен разослать PlayerListPacket с обновленным скином.
            // Если сервер этого не делает автоматически, то скин будет виден другим только после переподключения.
            // Мы уже отправили PlayerSkinPacket на сервер. Этого, по идее, должно быть достаточно.
        } catch (e: Exception) {
            sendClientMessage("§cОшибка смены скина: ${e.message}")
            e.printStackTrace() // Для более подробного дебага
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        // Заполнение SkinCache из PlayerListPacket
        if (packet is PlayerListPacket) {
            packet.entries.forEach { entry ->
                val entrySkin: SerializedSkin? = entry.skin
                // В PlayerListEntry обычно есть поля username, xuid, и displayName.
                // Если они недоступны напрямую как свойства, попробуй геттеры:
                val entryName: String? = entry.displayName // Попробуй получить display name
                    ?: entry.username // Если display name нет, попробуй username
                    ?: entry.xuid // Если и того нет, используй xuid (хотя это не ник)

                if (entrySkin != null && !entryName.isNullOrBlank()) {
                    SkinCache.putSkin(entryName, entrySkin)
                }
            }
        }

        // --- Обработка команд через TextPacket ---
        if (packet is TextPacket && packet.type == TextPacket.Type.CHAT) {
            // Проверяем, что сообщение отправил именно твой клиент
            // (хотя session.localPlayer.name может быть пустым, если ты его не установил).
            // Лучше ориентироваться на то, что сообщение приходит от "тебя" или от сервера,
            // а ты сам его сгенерировал.
            // Для простоты, пока будем считать, что ты вводишь команду сам.
            val message = packet.message.trim()
            if (message.startsWith(".skin ", ignoreCase = true)) {
                val args = message.split("\\s+".toRegex())
                if (args.size == 2) {
                    val targetNick = args[1]
                    applySkin(targetNick)
                    interceptablePacket.cancelled = true // Отменяем отправку команды в чат
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

    // Если нет базового класса Command, то внутренний класс SkinCommand нам не нужен.
    // Вся логика обработки команды будет в beforePacketBound.
}
