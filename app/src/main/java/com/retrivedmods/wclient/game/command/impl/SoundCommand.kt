// File: app/src/main/java/com/retrivedmods/wclient/game/command/impl/SoundCommand.kt
package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.module.misc.SoundModule
// SoundEvent больше не нужен для получения имени звука,
// так как мы ожидаем прямое строковое имя от пользователя.
// import org.cloudburstmc.protocol.bedrock.data.SoundEvent

class SoundCommand : Command("sound") {

    // Список популярных или часто используемых имен звуков для помощи пользователю.
    // Это не исчерпывающий список, но он дает примеры.
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
    override fun exec(args: Array<String>, session: GameSession) {
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
                // Пытаемся получить экземпляр SoundModule
                val soundModule = session.getModule(SoundModule::class.java) as? SoundModule
                if (soundModule == null) {
                    session.displayClientMessage("§c[SoundCommand] Модуль SoundModule не найден или неактивен.")
                    return
                }
                soundModule.stopAllSounds() // Вызываем функцию остановки всех звуков
                session.displayClientMessage("§a[SoundCommand] Отправлена команда на остановку всех звуков.")
            }
            // Если это не "stopall", значит, это попытка воспроизвести звук
            else -> {
                val soundName = args[0] // Получаем строковое имя звука из первого аргумента

                // Парсим остальные аргументы, используя безопасные преобразования
                // Громкость, по умолчанию 1.0f
                val volume = args.getOrNull(1)?.toFloatOrNull() ?: 1.0f
                // Частота воспроизведения (звуков в секунду), по умолчанию 1
                val soundsPerSecond = args.getOrNull(2)?.toIntOrNull() ?: 1
                // Длительность воспроизведения в секундах, по умолчанию 1
                val durationSeconds = args.getOrNull(3)?.toIntOrNull() ?: 1

                // Получаем SoundModule
                val soundModule = session.getModule(SoundModule::class.java) as? SoundModule

                if (soundModule == null) {
                    session.displayClientMessage("§c[SoundCommand] Модуль SoundModule не найден или неактивен.")
                    return
                }

                // Вызываем функцию playSound с полученными параметрами.
                // Передаем именно строковое имя звука.
                soundModule.playSound(soundName, volume, soundsPerSecond, durationSeconds)
                session.displayClientMessage("§aНачинаю воспроизведение звука: §b$soundName")
            }
        }
    }
}
