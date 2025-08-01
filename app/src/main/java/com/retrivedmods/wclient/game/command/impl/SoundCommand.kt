// File: com.retrivedmods.wclient.game.command.impl.SoundCommand.kt
package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.ModuleManager 
import com.retrivedmods.wclient.game.module.misc.SoundModule
import org.cloudburstmc.protocol.bedrock.data.SoundEvent

class SoundCommand : Command("sound", "s") { 

    override fun exec(args: Array<String>, session: GameSession) {
        println("DEBUG: SoundCommand.exec() called. Args: ${args.joinToString(" ")}, Session: $session")

        if (args.isEmpty()) {
            session.displayClientMessage("§e=== Sound Command Help ===")
            session.displayClientMessage("§7.sound <имя_звука> [громкость] [питч] §8- Воспроизвести звук")
            session.displayClientMessage("§7.sound list §8- Список доступных звуков")
            session.displayClientMessage("§7.sound test §8- Тест нескольких звуков")
            session.displayClientMessage("§7.sound random [громкость] [питч] §8- Случайный звук")
            session.displayClientMessage("§7.sound stopall §8- Остановить все звуки")
            session.displayClientMessage("§7.sound level <событие> §8- LevelSound событие")
            session.displayClientMessage("§7.sound attack §8- Быстрый звук атаки")
            session.displayClientMessage("§7.sound testlevel §8- Тест LevelSound событий")
            return
        }

        val soundModule = ModuleManager.getModule<SoundModule>()
        if (soundModule == null) {
            session.displayClientMessage("§c[SoundCommand] Модуль SoundModule не найден!")
            println("ERROR: SoundModule is null в SoundCommand")
            return
        }

        // Автоматически включаем модуль если он выключен
        if (!soundModule.isEnabled) {
            soundModule.isEnabled = true
            session.displayClientMessage("§a[SoundCommand] Модуль SoundModule автоматически включен.")
            println("DEBUG: SoundModule was disabled, enabling it now.")
        }

        when (args[0].lowercase()) {
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
                println("DEBUG: Starting sound test via SoundCommand")
                soundModule.testSounds()
            }

            "random" -> {
                val volume = args.getOrNull(1)?.toFloatOrNull() ?: 1.0f
                val pitch = args.getOrNull(2)?.toFloatOrNull() ?: 1.0f
                
                println("DEBUG: Playing random sound with volume=$volume, pitch=$pitch")
                soundModule.playRandomSound(volume, pitch)
            }

            "stopall" -> {
                println("DEBUG: Stopping all sounds via SoundCommand")
                soundModule.stopAllSounds()
                session.displayClientMessage("§a[SoundCommand] Отправлена команда на остановку звуков.")
            }

            "attack" -> {
                println("DEBUG: Playing attack sound via SoundCommand")
                soundModule.playAttackSound()
            }

            "testlevel" -> {
                session.displayClientMessage("§e[SoundCommand] Запускаю тест LevelSound событий...")
                println("DEBUG: Starting LevelSound test via SoundCommand")
                soundModule.testLevelSounds()
            }

            "level" -> {
                if (args.size < 2) {
                    session.displayClientMessage("§c[SoundCommand] Укажите событие для LevelSound!")
                    session.displayClientMessage("§7Пример: .sound level BLOCK_PLACE")
                    return
                }

                val eventName = args[1].uppercase()
                try {
                    val soundEvent = SoundEvent.valueOf(eventName)
                    println("DEBUG: Playing LevelSound event: $soundEvent")
                    soundModule.playLevelSoundAdvanced(soundEvent)
                    session.displayClientMessage("§a[SoundCommand] LevelSound событие: §b$eventName")
                } catch (e: IllegalArgumentException) {
                    session.displayClientMessage("§c[SoundCommand] Неизвестное событие: §7$eventName")
                    session.displayClientMessage("§7Примеры: ATTACK_NODAMAGE, BLOCK_PLACE, ITEM_USE_ON, STEP, HIT")
                }
            }

            "debug" -> {
                // Отладочная информация
                session.displayClientMessage("§e=== Sound Debug Info ===")
                session.displayClientMessage("§7Модуль включен: §b${soundModule.isEnabled}")
                session.displayClientMessage("§7Игрок: §b${session.localPlayer?.displayName ?: "null"}")
                session.displayClientMessage("§7Позиция: §b${session.localPlayer?.vec3Position}")
                
                // Тестовый звук для отладки
                println("DEBUG: Sending debug sound 'random.pop'")
                soundModule.playSound("random.pop", 1.0f, 1.0f)
            }

            else -> {
                // Воспроизведение конкретного звука
                val soundName = args[0]
                val volume = args.getOrNull(1)?.toFloatOrNull() ?: 1.0f
                val pitch = args.getOrNull(2)?.toFloatOrNull() ?: 1.0f

                // Валидация параметров
                if (volume < 0.0f || volume > 10.0f) {
                    session.displayClientMessage("§c[SoundCommand] Громкость должна быть от 0.0 до 10.0")
                    return
                }

                if (pitch < 0.1f || pitch > 2.0f) {
                    session.displayClientMessage("§c[SoundCommand] Питч должен быть от 0.1 до 2.0")
                    return
                }

                println("DEBUG: Playing sound via SoundCommand: $soundName (volume=$volume, pitch=$pitch)")
                
                // Используем безопасный метод воспроизведения
                soundModule.playSoundSafe(soundName, volume, pitch)
                
                session.displayClientMessage("§aВоспроизвожу звук: §b$soundName §7(V:${volume}, P:${pitch})")
            }
        }
    }
}
