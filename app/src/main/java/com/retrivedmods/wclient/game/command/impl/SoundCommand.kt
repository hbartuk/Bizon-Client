// File: app/src/main/java/com/retrivedmods/wclient/game/command/impl/SoundCommand.kt
package com.retrivedmods.wclient.game.command.impl // Ваш пакет для команд

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.module.impl.SoundModule // Импорт вашего нового модуля звуков

class SoundCommand : Command("sound") {

    override fun exec(args: Array<String>, session: GameSession) {
        if (args.isEmpty()) {
            session.displayClientMessage("§cИспользование: §7.sound <название> [громкость] [дальность] [звуков/сек] [длительность(сек)]")
            session.displayClientMessage("§eДоступные звуки (пример): §bstep, explode, click, place, break, levelup, attack, drink")
            return
        }

        val soundName = args[0]
        val volume = args.getOrNull(1)?.toFloatOrNull() ?: 1.0f // Громкость по умолчанию 1.0
        val distance = args.getOrNull(2)?.toFloatOrNull() ?: 16.0f // Дальность по умолчанию 16 блоков
        val soundsPerSecond = args.getOrNull(3)?.toIntOrNull() ?: 1 // По умолчанию 1 звук в секунду
        val durationSeconds = args.getOrNull(4)?.toIntOrNull() ?: 1 // По умолчанию 1 секунда

        // Получаем SoundModule из списка модулей
        val soundModule = session.getModule(SoundModule::class.java) // Предполагаем, что у GameSession есть метод getModule

        if (soundModule == null) {
            session.displayClientMessage("§c[SoundCommand] Модуль SoundModule не найден или неактивен.")
            return
        }

        if (soundName.lowercase() == "stopall") {
            soundModule.stopAllSounds()
            session.displayClientMessage("§a[SoundCommand] Отправлена команда на остановку всех звуков.")
            return
        }

        soundModule.playSound(soundName, volume, distance, soundsPerSecond, durationSeconds)
    }
}
