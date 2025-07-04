// File: app/src/main/java/com/retrivedmods/wclient/game/module/misc/SkinStealerModule.kt

package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.data.skin.SkinCache
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket // Этот импорт можно убрать, т.к. PlayerSkinPacket теперь используется в SkinStealerCommand
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket // Этот импорт можно убрать, т.к. TextPacket больше не обрабатывается здесь
import org.cloudburstmc.protocol.bedrock.data.PlayerListEntry 
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin // Этот импорт можно убрать, т.к. SerializedSkin теперь используется в SkinStealerCommand

class SkinStealerModule : Module("skinstealer", ModuleCategory.Misc) {

    init {
        // sendClientMessage("§aSkinStealer инициализирован. Используйте .skin <ник> для смены скина.")
        // Сообщение теперь будет отправляться только при активации или первом использовании.
        // Или вообще убрать это приветствие, т.к. теперь это просто сборщик скинов.
    }

    // Метод applySkin() больше не нужен здесь, т.к. он перенесен в SkinStealerCommand
    // fun applySkin(targetNick: String) { /* ... */ }

    /**
     * Перехватывает входящие и исходящие пакеты для обработки.
     * Заполняет SkinCache.
     * @param interceptablePacket Пакет, который можно перехватить.
     */
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        // Обрабатываем PlayerListPacket для заполнения SkinCache.
        if (packet is PlayerListPacket) {
            packet.entries.forEach { entry ->
                val entrySkin: SerializedSkin? = entry.skin
                val entryName: String = entry.name 

                if (entrySkin != null && entryName.isNotBlank()) {
                    SkinCache.putSkin(entryName.lowercase(), entrySkin)
                }
            }
        }

        // Логика обработки TextPacket (.skin команды) УДАЛЕНА ИЗ ЭТОГО МОДУЛЯ!
        // Она теперь в SkinStealerCommand.
        /*
        if (packet is TextPacket && packet.type == TextPacket.Type.CHAT) {
            val message = packet.message.trim()
            if (message.startsWith(".skin ", ignoreCase = true)) {
                // ... (вся старая логика парсинга команды)
            }
        }
        */
    }

    // Этот метод sendClientMessage() также можно удалить, если он больше не используется в этом модуле.
    // Если SkinStealerModule сам не будет выводить сообщения, то он ему не нужен.
    private fun sendClientMessage(msg: String) {
        session.displayClientMessage(msg)
    }
}
