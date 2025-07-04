package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket

class SourceModule : Module("source", ModuleCategory.Misc) {

    // Этот метод вызывай из общего обработчика чата, если модуль включён
    fun handleChatCommand(message: String) {
        if (!isEnabled) return

        val args = message.trim().split("\\s+".toRegex())
        if (args.isEmpty() || args[0] != ".source") return

        if (args.size != 7) {
            sendClientMessage("§cUsage: .source <volume> <x> <y> <z> <SOUND_TYPE> <count>")
            return
        }

        try {
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
            sendClientMessage("§aPlayed $soundType at $x $y $z volume $volume ($count times)")
        } catch (e: Exception) {
            sendClientMessage("§cОшибка: ${e.message}")
        }
    }

    // Вспомогательный метод для вывода сообщений игроку
    private fun sendClientMessage(msg: String) {
        // Используй аналогично PlayerTracerModule для отправки сообщений
        // Например, через TextPacket или напрямую в чат клиента
    }
}
