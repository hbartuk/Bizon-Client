package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.data.skin.SkinCache
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin

class SkinStealerModule : Module("skinstealer", ModuleCategory.Misc) {
    
    private var session: GameSession? = null

    init {
        // Убрали вызов sendClientMessage из блока init.
    }
    
    override fun onEnable() {
        super.onEnable()
        this.session = gameSession
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        if (packet is PlayerListPacket) {
            packet.entries.forEach { entry ->
                // Теперь компилятор будет знать, что entry.skin - это SerializedSkin
                val entrySkin: SerializedSkin? = entry.skin
                val entryName: String = entry.name 

                if (entrySkin != null && entryName.isNotBlank()) {
                    SkinCache.putSkin(entryName.lowercase(), entrySkin)
                    session?.displayClientMessage("§a[SkinStealer] Сохранён скин игрока: §b$entryName")
                }
            }
        }
        // Логика обработки TextPacket (команды .skin) УДАЛЕНА ИЗ ЭТОГО МОДУЛЯ!
        // Она теперь полностью в SkinStealerCommand и будет обрабатываться GameSession.
    }
}
