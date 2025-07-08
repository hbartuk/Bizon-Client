// В ModuleManager.kt (УБЕДИТЕСЬ, ЧТО ЭТОТ МЕТОД ЕСТЬ!)
// Если его нет, добавьте его:
// inline fun <reified T : Module> getModule(): T? {
//     return modules.firstOrNull { it is T } as? T
// }

// File: com.retrivedmods.wclient.game.command.impl.SoundCommand.kt

package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.ModuleManager // УБЕДИТЕСЬ, что этот импорт есть
import com.retrivedmods.wclient.game.module.misc.SoundModule

class SoundCommand : Command("sound") { // (ваши алиасы)

    private val popularSounds = listOf( /* ... */ )

    override fun exec(args: Array<String>, session: GameSession) {
        println("DEBUG: SoundCommand.exec() called.")

        if (args.isEmpty()) { /* ... */ return }

        when (args[0].lowercase()) {
            "stopall" -> {
                // --- ИСПРАВЛЕНИЕ ЗДЕСЬ: Используем ModuleManager.getModule() ---
                val soundModule = ModuleManager.getModule<SoundModule>()
                if (soundModule == null) {
                    session.displayClientMessage("§c[SoundCommand] Модуль SoundModule не найден.")
                    return
                }
                soundModule.stopAllSounds()
                session.displayClientMessage("§a[SoundCommand] Отправлена команда на остановку всех звуков.")
            }
            else -> {
                val soundName = args[0]
                val volume = args.getOrNull(1)?.toFloatOrNull() ?: 1.0f
                val pitch = args.getOrNull(2)?.toFloatOrNull() ?: 1.0f

                // --- ИСПРАВЛЕНИЕ ЗДЕСЬ: Используем ModuleManager.getModule() ---
                val soundModule = ModuleManager.getModule<SoundModule>()

                if (soundModule == null) {
                    session.displayClientMessage("§c[SoundCommand] Модуль SoundModule не найден.")
                    println("DEBUG: SoundModule is null in SoundCommand.")
                    return
                }

                // Включаем модуль, если он выключен (логика, которую вы хотели)
                if (!soundModule.isEnabled) {
                    soundModule.isEnabled = true
                    session.displayClientMessage("Модуль SoundModule был автоматически включен.")
                    println("DEBUG: SoundModule was not enabled, enabling it now.")
                }

                println("DEBUG: Calling playSound on SoundModule.")
                soundModule.playSound(soundName, volume, pitch)
                session.displayClientMessage("§aНачинаю воспроизведение звука: §b$soundName")
            }
        }
    }
}
