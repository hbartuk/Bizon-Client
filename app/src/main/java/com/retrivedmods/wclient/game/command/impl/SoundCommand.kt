// File: app/src/main/java/com/retrivedmods/wclient/game/command/impl/SoundCommand.kt
package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.module.misc.SoundModule // <-- Убедитесь, что этот путь к SoundModule правильный

class SoundCommand : Command("sound") {

    override fun exec(args: Array<String>, session: GameSession) {
        if (args.isEmpty()) {
            session.displayClientMessage("§cИспользование: §7.sound <name> [volume] [distance] [sounds/sec] [duration(sec)]")
            session.displayClientMessage("§eДоступные звуки (пример): §bstep, explode, click, place, break, levelup, attack, drink")
            return
        }

        val soundName = args[0]
        val volume = args.getOrNull(1)?.toFloatOrNull() ?: 1.0f
        val distance = args.getOrNull(2)?.toFloatOrNull() ?: 16.0f
        val soundsPerSecond = args.getOrNull(3)?.toIntOrNull() ?: 1
        val durationSeconds = args.getOrNull(4)?.toIntOrNull() ?: 1

        // Метод getModule ожидает Class<T>, SoundModule::class.java правилен
        val soundModule = session.getModule(SoundModule::class.java)

        if (soundModule == null) {
            session.displayClientMessage("§c[SoundCommand] Модуль SoundModule не найден или неактивен.")
            return
        }

        if (soundName.lowercase() == "stopall") { // `lowercase()` должна быть доступна здесь
            soundModule.stopAllSounds() // Этот метод должен быть корректно распознан
            session.displayClientMessage("§a[SoundCommand] Отправлена команда на остановку всех звуков.")
            return
        }

        soundModule.playSound(soundName, volume, distance, soundsPerSecond, durationSeconds) // Этот метод должен быть корректно распознан
    }
}
