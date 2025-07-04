package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.data.skin.SkinCache
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin
// Ensure you have this import for ByteBuf, if skinData is a ByteBuf
import io.netty.buffer.ByteBuf // This is often used with CloudburstMC for byte data

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

        // --- Logging for diagnosis with corrected property names ---
        session.displayClientMessage("§aСкин найден в кэше.")
        session.displayClientMessage("§aUUID моего игрока: §b${session.localPlayer.uuid}")
        
        // **CORRECTION HERE:** Use 'skin.getSkinData().readableBytes()' for ByteBuf size, or 'skin.getSkinData().length' if it's a byte array.
        // If your 'SerializedSkin' has a 'skinData' field that is a ByteBuf, this is how you get its size.
        // If it's a byte[] (byte array), use 'skin.skinData.size' or 'skin.skinData.length'
        // I'm assuming 'getSkinData()' method exists and returns a ByteBuf.
        val skinDataSize = skin.getSkinData()?.readableBytes() ?: 0 // Most common for ByteBuf
        // OR if skin.skinData is directly a byte array (byte[]):
        // val skinDataSize = skin.skinData?.size ?: 0

        session.displayClientMessage("§aРазмер данных скина: §b${skinDataSize} байт.")
        session.displayClientMessage("§aГеометрия скина: §b${skin.geometryName}")
        session.displayClientMessage("§aID Скина (или текстуры): §b${skin.skinId}") // **CORRECTION HERE: Use 'skinId' instead of 'textureName'**

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
