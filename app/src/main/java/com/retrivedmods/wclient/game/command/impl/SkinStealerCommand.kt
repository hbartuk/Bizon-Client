package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command // Импортируем наш новый базовый класс Command
import com.retrivedmods.wclient.game.data.skin.SkinCache
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin

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

        try {
            val packet = PlayerSkinPacket().apply {
                uuid = session.localPlayer.uuid
                this.skin = skin
            }
            session.serverBound(packet)

            session.displayClientMessage("§aСкин успешно изменён на скин игрока §b$targetNick!§a")
        } catch (e: Exception) {
            session.displayClientMessage("§cОшибка смены скина: ${e.message}")
            e.printStackTrace()
        }
    }
}
