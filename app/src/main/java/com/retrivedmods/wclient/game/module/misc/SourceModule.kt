package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.module.Category
import com.retrivedmods.wclient.game.module.Module
import com.retrivedmods.wclient.utils.SoundUtils
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.protocol.bedrock.BedrockServerSession

class SourceModule : Module(
    name = "Source",
    description = "Проигрывает выбранный звук на выбранных координатах по вашей команде",
    category = Category.MISC
) {
    // Метод, который должен вызываться при парсинге инпута игрока (например, из чата)
    // context: строка команды, session: сессия игрока
    fun onChatCommand(input: String, session: BedrockServerSession) {
        // Проверяем, включён ли модуль
        if (!this.enabled) return

        val args = input.trim().split("\\s+".toRegex())
        if (args.isEmpty() || args[0] != ".source") return

        if (args.size != 7) {
            // Сообщение игроку о неправильном синтаксисе (можно заменить на sendMessage)
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

            // Парсим тип звука
            val soundEvent = SoundEvent.valueOf(soundType)

            val pos = Vector3f(x, y, z)

            for (i in 1..count) {
                SoundUtils.source(
                    session,
                    soundEvent,
                    pos,
                    pitch = 1.0f,
                    volume = volume
                )
            }
            // Можно отправить сообщение игроку об успешном воспроизведении
        } catch (e: Exception) {
            println("Ошибка в параметрах команды .source: ${e.message}")
        }
    }
}
