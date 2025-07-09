package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.ModuleManager // УБЕДИТЕСЬ, что этот импорт есть
import com.retrivedmods.wclient.game.module.misc.SoundModule

class SoundCommand : Command("sound", "s") { // Добавил пример алиаса "s"

    // Пример популярных звуков (пока пустой)
    private val popularSounds = listOf<String>()

    override fun exec(args: Array<String>, session: GameSession) {
        println("DEBUG: SoundCommand.exec() called. Args: ${args.joinToString(" ")}, Session: $session")

        if (args.isEmpty()) {
            session.displayClientMessage("§eИспользование: §7.sound <имя_звука> [громкость] [питч] | .sound stopall")
            return
        }

        when (args[0].lowercase()) {
            "stopall" -> {
                val soundModule = ModuleManager.getModule<SoundModule>()
                if (soundModule == null) {
                    session.displayClientMessage("§c[SoundCommand] Модуль SoundModule не найден. Невозможно остановить все звуки.")
                    println("DEBUG: SoundModule is null when trying to stop all sounds.")
                    return
                }
                soundModule.stopAllSounds()
                session.displayClientMessage("§a[SoundCommand] Отправлена команда на остановку всех звуков.")
                println("DEBUG: stopAllSounds() called on SoundModule.")
            }
            else -> {
                val soundName = args[0]
                val volume = args.getOrNull(1)?.toFloatOrNull() ?: 1.0f
                val pitch = args.getOrNull(2)?.toFloatOrNull() ?: 1.0f

                val soundModule = ModuleManager.getModule<SoundModule>()

                if (soundModule == null) {
                    session.displayClientMessage("§c[SoundCommand] Модуль SoundModule не найден. Невозможно воспроизвести звук.")
                    println("DEBUG: SoundModule is null in SoundCommand for playSound.")
                    return
                }

                // Включаем модуль, если он выключен
                if (!soundModule.isEnabled) {
                    soundModule.isEnabled = true
                    session.displayClientMessage("§a[SoundCommand] Модуль SoundModule был автоматически включен.")
                    println("DEBUG: SoundModule was not enabled, enabling it now.")
                }

                println("DEBUG: Calling playSound on SoundModule for sound: $soundName.")
                soundModule.playSound(soundName, volume, pitch)
                session.displayClientMessage("§aНачинаю воспроизведение звука: §b$soundName §7(Громкость: ${volume}, Питч: ${pitch})")
            }
        }
    }
}
