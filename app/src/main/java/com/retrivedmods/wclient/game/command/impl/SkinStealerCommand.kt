package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.data.skin.SkinCache
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin
// import io.netty.buffer.ByteBuf // Этот импорт нам больше не нужен, если skinData не ByteBuf

class SkinStealerCommand : Command("skin", "sks") {

    override fun exec(args: Array<String>, session: GameSession) {
        if (args.isEmpty()) {
            session.displayClientMessage("§cИспользование: .skin <ник>")
            return
        }

        val targetNick = args[0]
        session.displayClientMessage("§eПытаюсь получить скин для игрока: §b$targetNick...")
        
        val normalizedTargetNick = targetNick.lowercase()
        val skin: SerializedSkin? = SkinCache.getSkin(normalizedTargetNick)

        if (skin == null) {
            session.displayClientMessage("§cСкин игрока '$targetNick' не найден в кэше! Игрок должен быть на сервере, и его данные должны быть загружены.")
            return
        }

        // --- Логирование для диагностики с исправленными названиями свойств ---
        session.displayClientMessage("§aСкин найден в кэше.")
        session.displayClientMessage("§aUUID моего игрока: §b${session.localPlayer.uuid}")
        
        // **ФИНАЛЬНОЕ ИСПРАВЛЕНИЕ ЗДЕСЬ:** // Используем skin.skinData?.size ?: 0 для получения размера ByteArray.
        // Я предполагаю, что поле называется 'skinData'. Если оно называется 'data', 'imageData'
        // или как-то ещё, замени 'skinData' на правильное название.
        val skinDataSize = skin.skinData?.size ?: 0 

        session.displayClientMessage("§aРазмер данных скина: §b${skinDataSize} байт.")
        session.displayClientMessage("§aГеометрия скина: §b${skin.geometryName}")
        session.displayClientMessage("§aID Скина (или текстуры): §b${skin.skinId}") 

        try {
            val packet = PlayerSkinPacket().apply {
                uuid = session.localPlayer.uuid
                this.skin = skin
            }
            session.serverBound(packet)

            session.displayClientMessage("§aПакет смены скина отправлен. Проверьте свой скин.")
        } catch (e: Exception) {
            session.displayClientMessage("§cОшибка смены скина: ${e.message}")
            e.printStackTrace()
        }
    }
}
