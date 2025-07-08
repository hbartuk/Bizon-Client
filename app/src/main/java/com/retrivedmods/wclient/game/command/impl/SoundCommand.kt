// File: app/src/main/java/com/retrivedmods/wclient/game/command/impl/SoundCommand.kt
package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.module.misc.SoundModule
import org.cloudburstmc.protocol.bedrock.data.SoundEvent // ВАЖНО: ЯВНО ДОБАВЛЕН ИМПОРТ SoundEvent

class SoundCommand : Command("sound") {

    override fun exec(args: Array<String>, session: GameSession) {
        if (args.isEmpty()) {
            session.displayClientMessage("§cИспользование: §7.sound <ID_звука> [громкость] [частота] [длительность]")
            session.displayClientMessage("§eДля остановки всех звуков: §b.sound stopall")
            session.displayClientMessage("§eID звука - это число от 0 до ${SoundEvent.values().size - 1}. Посмотрите SoundEvent.java для точного соответствия ID.")
            session.displayClientMessage("§eПример: §b.sound 0 (ITEM_USE_ON)§e, §b.sound 5 (BREAK)§e, §b.sound 39 (ATTACK_NODAMAGE)")
            return
        }

        when (args[0].lowercase()) {
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
                val soundId = args[0].toIntOrNull()
                if (soundId == null) {
                    session.displayClientMessage("§cНеверный ID звука. Используйте числовой ID или 'stopall'.")
                    return
                }

                // --- САМОЕ ВАЖНОЕ ИЗМЕНЕНИЕ ЗДЕСЬ ---
                val soundEventValues = SoundEvent.values()
                if (soundId < 0 || soundId >= soundEventValues.size) {
                    session.displayClientMessage("§cНеверный ID звука. Диапазон: 0 до ${soundEventValues.size - 1}.")
                    return
                }
                // Получаем строковое имя звука из SoundEvent
                val soundName = soundEventValues[soundId].name.lowercase().replace("_", ".")
                    // В Bedrock протоколе имена звуков часто используют точки вместо подчеркиваний
                    // Это может потребовать дополнительной логики, если SoundEvent.name()
                    // не совпадает с реальными именами звуков в игре.
                    // Например, SoundEvent.BLOCK_BONE_BLOCK_BREAK_SOUND -> "block.bone_block.break"

                val volume = args.getOrNull(1)?.toFloatOrNull() ?: 1.0f
                // val distance = args.getOrNull(2)?.toFloatOrNull() ?: 16.0f // Удалено, т.к. не используется в PlaySoundPacket
                val soundsPerSecond = args.getOrNull(2)?.toIntOrNull() ?: 1 // Индекс изменился
                val durationSeconds = args.getOrNull(3)?.toIntOrNull() ?: 1 // Индекс изменился

                val soundModule = session.getModule(SoundModule::class.java) as? SoundModule

                if (soundModule == null) {
                    session.displayClientMessage("§c[SoundCommand] Модуль SoundModule не найден или неактивен.")
                    return
                }

                // Передаем строковое имя звука
                soundModule.playSound(soundName, volume, soundsPerSecond, durationSeconds)
                session.displayClientMessage("§aНачинаю воспроизведение звука: §b$soundName §a(ID: §b$soundId§a)")
            }
        }
    }
}
