package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.ModuleManager
import com.retrivedmods.wclient.game.module.misc.SoundModule
import org.cloudburstmc.protocol.bedrock.data.SoundEvent

class SoundCommand : Command("sound", "s") {

    override fun exec(args: Array<String>, session: GameSession) {
        val soundModule = ModuleManager.getModule<SoundModule>()
        if (soundModule == null) {
            session.displayClientMessage("§c[SoundCommand] Модуль SoundModule не найден!")
            return
        }

        if (!soundModule.isEnabled) {
            soundModule.isEnabled = true
            session.displayClientMessage("§a[SoundCommand] Модуль SoundModule автоматически включен.")
        }

        when (args.getOrNull(0)?.lowercase()) {
            "list" -> {
                session.displayClientMessage("§e=== Доступные звуки ===")
                val sounds = soundModule.listAvailableSounds()
                sounds.chunked(3).forEach { chunk ->
                    val line = chunk.joinToString("§7, §b") { "§b$it" }
                    session.displayClientMessage("§7$line")
                }
                session.displayClientMessage("§7Всего звуков: §e${sounds.size}")
            }

            "test" -> {
                session.displayClientMessage("§e[SoundCommand] Запускаю тест звуков...")
                soundModule.testSounds()
            }

            "random" -> {
                val volume = args.getOrNull(1)?.toFloatOrNull() ?: 1.0f
                val pitch = args.getOrNull(2)?.toFloatOrNull() ?: 1.0f
                soundModule.playRandomSound(volume, pitch)
            }

            "stopall" -> {
                soundModule.stopAllSounds()
                session.displayClientMessage("§a[SoundCommand] Отправлена команда на остановку звуков.")
            }

            "attack" -> {
                soundModule.playAttackSound()
            }

            "testlevel" -> {
                session.displayClientMessage("§e[SoundCommand] Запускаю тест LevelSound событий...")
                soundModule.testLevelSounds()
            }

            "level" -> {
                if (args.size < 2) {
                    session.displayClientMessage("§c[SoundCommand] Укажите событие для LevelSound!")
                    session.displayClientMessage("§7Пример: .sound level ATTACK_NODAMAGE")
                    return
                }

                val eventName = args[1].uppercase()
                try {
                    val soundEvent = SoundEvent.valueOf(eventName)
                    // ИСПРАВЛЕНО: Используем playLevelSound и передаем ID события
                    soundModule.playLevelSound(soundEvent.ordinal)
                    session.displayClientMessage("§a[SoundCommand] LevelSound событие: §b$eventName")
                } catch (e: IllegalArgumentException) {
                    session.displayClientMessage("§c[SoundCommand] Неизвестное событие: §7$eventName")
                    session.displayClientMessage("§7Примеры: ATTACK_NODAMAGE, BLOCK_PLACE, ITEM_USE_ON, STEP, HIT")
                }
            }
            
            else -> {
                val soundName = args.getOrNull(0) ?: run {
                    session.displayClientMessage("§c[SoundCommand] Укажите имя звука или команду.")
                    return
                }
                val volume = args.getOrNull(1)?.toFloatOrNull() ?: 1.0f
                val pitch = args.getOrNull(2)?.toFloatOrNull() ?: 1.0f

                if (volume < 0.0f || volume > 10.0f) {
                    session.displayClientMessage("§c[SoundCommand] Громкость должна быть от 0.0 до 10.0")
                    return
                }

                if (pitch < 0.1f || pitch > 2.0f) {
                    session.displayClientMessage("§c[SoundCommand] Питч должен быть от 0.1 до 2.0")
                    return
                }

                soundModule.playSoundSafe(soundName, volume, pitch)
            }
        }
    }
}
