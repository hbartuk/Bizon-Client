package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.data.skin.SkinCache
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin
// import org.cloudburstmc.protocol.bedrock.data.skin.ImageData // <-- Возможно, потребуется этот импорт

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

        session.displayClientMessage("§aСкин найден в кэше.")
        session.displayClientMessage("§aUUID моего игрока (из localPlayer): §b${session.localPlayer.uuid}")
        
        val skinDataSize = skin.skinData?.image?.size ?: 0 
        session.displayClientMessage("§aРазмер данных скина: §b${skinDataSize} байт.")
        
        session.displayClientMessage("§aГеометрия скина: §b${skin.geometryName}")
        session.displayClientMessage("§aID Скина (или текстуры): §b${skin.skinId}") 

        // --- ОБНОВЛЕННЫЕ ЛОГИ ДЛЯ ОТЛАДКИ (используются геттеры 'is...') ---
        session.displayClientMessage("§a--- Детали SerializedSkin ---")
        session.displayClientMessage("§a  PlayFab ID: §b${skin.playFabId}")
        session.displayClientMessage("§a  Skin Resource Patch: §b${skin.skinResourcePatch}")
        session.displayClientMessage("§a  Geometry Data (часть): §b${skin.geometryData.take(100)}...")
        session.displayClientMessage("§a  Animation Data (часть): §b${skin.animationData.take(100)}...")
        session.displayClientMessage("§a  Premium: §b${skin.isPremium()}") // <-- ИСПРАВЛЕНО
        session.displayClientMessage("§a  Persona: §b${skin.isPersona()}") // <-- ИСПРАВЛЕНО
        session.displayClientMessage("§a  Cape On Classic: §b${skin.isCapeOnClassic()}") // <-- ИСПРАВЛЕНО
        session.displayClientMessage("§a  Primary User: §b${skin.isPrimaryUser()}") // <-- ИСПРАВЛЕНО
        session.displayClientMessage("§a  Overriding Player Appearance: §b${skin.isOverridingPlayerAppearance()}") // <-- ИСПРАВЛЕНО
        session.displayClientMessage("§a  Arm Size: §b${skin.armSize}")
        val capeDataSize = skin.capeData?.image?.size ?: 0
        session.displayClientMessage("§a  Cape Data Size: §b${capeDataSize} байт.")
        session.displayClientMessage("§a--------------------------")


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
