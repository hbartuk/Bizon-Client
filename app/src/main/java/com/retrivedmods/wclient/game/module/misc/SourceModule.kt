package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.InterceptablePacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket.Type
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket

class SourceModule : Module("source", ModuleCategory.Misc) {

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        // Предположим, что у тебя есть пакет типа TextPacket для отправки чата на сервер
        if (packet is TextPacket && packet.type == Type.CHAT) {
            val msg = packet.message.trim()
            if (msg.startsWith(".source")) {
                // Не отправлять этот пакет на сервер
                interceptablePacket.cancelled = true

                // Обработка команды
                val feedback = handleSourceCommand(msg)
                // Отправить результат игроку локально
                val feedbackPacket = TextPacket().apply {
                    type = Type.RAW
                    message = feedback
                    isNeedsTranslation = false
                }
                session.clientBound(feedbackPacket)
            }
        }
    }

    private fun handleSourceCommand(message: String): String {
        val args = message.split("\\s+".toRegex())
        if (args.size != 7) {
            return "§cОшибка: .source <volume> <x> <y> <z> <SOUND_TYPE> <count>"
        }
        return try {
            val volume = args[1].toFloat()
            val x = args[2].toFloat()
            val y = args[3].toFloat()
            val z = args[4].toFloat()
            val soundType = args[5].uppercase()
            val count = args[6].toInt()
            val soundEvent = SoundEvent.valueOf(soundType)
            val pos = Vector3f.from(x, y, z)
            repeat(count) {
                val packet = LevelSoundEventPacket().apply {
                    sound = soundEvent
                    position = pos
                    extraData = -1
                    isBabySound = false
                }
                session.clientBound(packet)
            }
            "§aВыполнено!"
        } catch (e: Exception) {
            "§cНе выполнено: ошибка (${e.message})"
        }
    }
}
