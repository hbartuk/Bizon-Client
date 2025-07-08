// File: app/src/main/java/com/retrivedmods/wclient/game/command/impl/SoundCommand.kt
package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.ModuleManager
import com.retrivedmods.wclient.game.module.misc.SoundModule

class SoundCommand : Command("sound") { // Убедитесь, что ваш базовый Command инициализируется с алиасом/именем

    // Список популярных или часто используемых имен звуков для помощи пользователю.
    private val popularSounds = listOf(
        "block.chest.open",
        "block.chest.close",
        "random.explode",
        "mob.cow.ambient",
        "mob.sheep.say",
        "item.trident.throw",
        "ambient.weather.thunder",
        "item.bottle.fill"
    )

    /**
     * Выполняет команду .sound.
     * Позволяет воспроизводить звуки Minecraft Bedrock по их строковому имени.
     *
     * Использование: .sound <имя_звука> [громкость] [частота] [длительность]
     * Например: .sound block.chest.open 1.0 1 5
     * Для остановки всех звуков: .sound stopall
     */
    // *** ИЗМЕНЕНИЕ ЗДЕСЬ: МЕТОД ДОЛЖЕН НАЗЫВАТЬСЯ exec И ПРИНИМАТЬ Array<String> И GameSession ***
    override fun exec(args: Array<String>, session: GameSession) { // Вернули exec и Array<String>
        println("DEBUG: SoundCommand.exec() called.") // Обновлено логирование

        // Если аргументы не предоставлены, показать справку по использованию
        if (args.isEmpty()) {
            session.displayClientMessage("§cИспользование: §7.sound <имя_звука> [громкость] [частота] [длительность]")
            session.displayClientMessage("§eДля остановки всех звуков: §b.sound stopall")
            session.displayClientMessage("§eПримеры имен звуков: §b${popularSounds.joinToString(", ")}")
            session.displayClientMessage("§7Полный список имен звуков можно найти в ресурсах игры или на wiki Bedrock протокола.")
            return
        }

        // Обработка подкоманд, таких как "stopall"
        when (args[0].lowercase()) {
            "stopall" -> {
                val soundModule = session.getModule(SoundModule::class.java) as? SoundModule // Используем session.getModule
                if (soundModule == null) {
                    session.displayClientMessage("§c[SoundCommand] Модуль SoundModule не найден или неактивен.")
                    return
                }
                soundModule.stopAllSounds()
                session.displayClientMessage("§a[SoundCommand] Отправлена команда на остановку всех звуков.")
            }
            else -> {
                val soundName = args[0]
                val volume = args.getOrNull(1)?.toFloatOrNull() ?: 1.0f
                // Частота и длительность, если ваш playSound принимает их.
                // В SoundModule мы изменили playSound на volume, pitch.
                // Возможно, вам нужно будет решить, как эти параметры будут маппиться.
                // Пока оставим их как volume и pitch для простоты, если это то, что вы имели в виду.
                // Или если SoundModule.playSound принимает только volume и pitch, то args[2] и args[3] могут быть pitch.
                val pitch = args.getOrNull(2)?.toFloatOrNull() ?: 1.0f // Используем args[2] как pitch

                val soundModule = session.getModule(SoundModule::class.java) as? SoundModule // Используем session.getModule

                if (soundModule == null) {
                    session.displayClientMessage("§c[SoundCommand] Модуль SoundModule не найден или неактивен.")
                    println("DEBUG: SoundModule is null in SoundCommand.")
                    return
                }

                // Убедимся, что модуль включен
                if (!soundModule.isEnabled) {
                    soundModule.isEnabled = true // Включаем модуль, если он выключен
                    session.displayClientMessage("Модуль SoundModule был автоматически включен.")
                    println("DEBUG: SoundModule was not enabled, enabling it now.")
                }

                println("DEBUG: Calling playSound on SoundModule.")
                // *** ИЗМЕНЕНИЕ ЗДЕСЬ: Передача soundName, volume, pitch ***
                soundModule.playSound(soundName, volume, pitch) // Передаем volume и pitch
                session.displayClientMessage("§aНачинаю воспроизведение звука: §b$soundName")
            }
        }
    }
}
