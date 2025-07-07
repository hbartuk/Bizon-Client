// File: app/src/main/java/com/retrivedmods/wclient/game/command/impl/SoundCommand.kt
package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.module.misc.SoundModule // <-- Убедитесь, что этот путь к SoundModule правильный

class SoundCommand : Command("sound") {

    override fun exec(args: Array<String>, session: GameSession) {
        if (args.isEmpty()) {
            session.displayClientMessage("§cИспользование: §7.sound <ID_звука> [громкость] [дистанция] [частота] [длительность]")
            session.displayClientMessage("§eДля остановки всех звуков: §b.sound stopall")
            session.displayClientMessage("§eПример ID звука: §b4 (это для RANDOM_CLICK в старых версиях)")
            return
        }

        // ИСПРАВЛЕНИЕ: Используем java.lang.String.toLowerCase()
        when (java.lang.String.toLowerCase(args[0])) {
            "stopall" -> {
                val soundModule = session.getModule(SoundModule::class.java) as? SoundModule
                if (soundModule == null) {
                    session.displayClientMessage("§c[SoundCommand] Модуль SoundModule не найден или неактивен.")
                    return
                }
                soundModule.stopAllSounds()
                session.displayClientMessage("§a[SoundCommand] Отправлена команда на остановку всех звуков.")
            }
            else -> {
                // ИСПРАВЛЕНИЕ: Теперь мы ожидаем Integer (ID звука)
                val soundId = args[0].toIntOrNull()
                if (soundId == null) {
                    session.displayClientMessage("§cНеверный ID звука. Используйте числовой ID (например, 4) или 'stopall'.")
                    return
                }

                val volume = args.getOrNull(1)?.toFloatOrNull() ?: 1.0f
                val distance = args.getOrNull(2)?.toFloatOrNull() ?: 16.0f
                val soundsPerSecond = args.getOrNull(3)?.toIntOrNull() ?: 1
                val durationSeconds = args.getOrNull(4)?.toIntOrNull() ?: 1

                val soundModule = session.getModule(SoundModule::class.java) as? SoundModule

                if (soundModule == null) {
                    session.displayClientMessage("§c[SoundCommand] Модуль SoundModule не найден или неактивен.")
                    return
                }

                // Вызываем playSound с целочисленным ID
                soundModule.playSound(soundId, volume, distance, soundsPerSecond, durationSeconds)
                session.displayClientMessage("§aНачинаю воспроизведение звука с ID: §b$soundId")
            }
        }
    }
}
