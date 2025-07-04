package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.protocol.bedrock.BedrockServerSession

class SourceModule : Module(
    "Source",
    "Проигрывает выбранный звук на выбранных координатах по вашей команде",
    Category.MISC
) {
    // Вставь сюда свой SoundUtils или реализуй метод прямо тут
    fun onChatCommand(input: String, session: BedrockServerSession) {
        if (!this.enabled) return

        val args = input.trim().split("\\s+".toRegex())
        if (args.isEmpty() || args[0] != ".source") return

        if (args.size != 7) {
            println("Использование: .source (громкость) (x) (y) (z) (тип_звука) (кол-во)")
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
            val pos = Vector3f(x, y, z)

            repeat(count) {
                playSound(session, soundEvent, pos, volume)
            }
        } catch (e: Exception) {
            println("Ошибка в параметрах команды .source: ${e.message}")
        }
    }

    private fun playSound(
        session: BedrockServerSession,
        sound: SoundEvent,
        position: Vector3f,
        volume: Float
    ) {
        val packet = org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket().apply {
            this.sound = sound
            this.position = position
            this.extraData = -1
            this.pitch = 1.0f
            this.volume = volume
            this.isBabySound = false
            this.relativeVolumeDisabled = false
        }
        session.sendPacket(packet)
    }
}
