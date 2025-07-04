package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command // Импортируем наш базовый класс Command
import com.retrivedmods.wclient.game.data.skin.SkinCache
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin

class SkinStealerCommand : Command("skin", "sks") {

    override fun exec(args: Array<String>, session: GameSession) {
        if (args.isEmpty()) {
            session.displayClientMessage("§cИспользование: .skin <ник>")
            return
        }

        val targetNick = args[0] // Первый аргумент - это ник
        session.displayClientMessage("§eПытаюсь получить скин для игрока: §b$targetNick...")
        
        val normalizedTargetNick = targetNick.lowercase()
        val skin: SerializedSkin? = SkinCache.getSkin(normalizedTargetNick)

        if (skin == null) {
            session.displayClientMessage("§cСкин игрока '$targetNick' не найден в кэше! Игрок должен быть на сервере, и его данные должны быть загружены.")
            return
        }

        // --- Добавляем логирование для диагностики ---
        session.displayClientMessage("§aСкин найден в кэше.")
        session.displayClientMessage("§aUUID моего игрока: §b${session.localPlayer.uuid}")
        
        // Пожалуйста, проверь название поля с данными скина в твоем классе SerializedSkin.
        // Это может быть skinData, data, imageData или что-то другое.
        // Используй то, которое содержит массив байтов изображения.
        val skinDataSize = skin.skinData?.size ?: 0 // Используй 'skinData' или 'data' или другое название поля в твоём SerializedSkin
        session.displayClientMessage("§aРазмер данных скина: §b${skinDataSize} байт.")
        session.displayClientMessage("§aГеометрия скина: §b${skin.geometryName}")
        session.displayClientMessage("§aID текстуры: §b${skin.textureName}") // Если SerializedSkin имеет такое поле.

        try {
            val packet = PlayerSkinPacket().apply {
                uuid = session.localPlayer.uuid // UUID твоего игрока
                this.skin = skin // Украденный скин
            }
            session.serverBound(packet) // Отправляем пакет на сервер

            session.displayClientMessage("§aПакет смены скина отправлен. Проверьте свой скин на себе или попросите другого игрока посмотреть на вас.")
        } catch (e: Exception) {
            session.displayClientMessage("§cОшибка смены скина: ${e.message}")
            e.printStackTrace() // Выводим стек-трейс в консоль для детальной отладки
        }
    }
}
