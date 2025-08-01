/**
     * Получить список доступных звуков
     */
    fun listAvailableSounds(): List<String> = workingSounds

    /**
     * Тест различных SoundEvent'ов
     */
    fun testLevelSounds() {
        val testEvents = listOf(
            SoundEvent.ATTACK_NODAMAGE,
            SoundEvent.BLOCK_PLACE,
            SoundEvent.ITEM_USE_ON,
            SoundEvent.STEP,
            SoundEvent.HIT,
            SoundEvent.ENTITY_HURT
        )

        runOnSession { session ->
            session.displayClientMessage("§e[SoundModule] Тест LevelSound событий...")
            
            Thread {
                testEvents.forEachIndexed { index, event ->
                    try {
                        Thread.sleep(2000L) // Пауза между звуками
                        playLevelSoundAdvanced(event)
                        
                        runOnSession { 
                            it.displayClientMessage("§7[${index + 1}/${testEvents.size}] §b$event")
                        }
                    } catch (e: Exception) {
                        println("ERROR в testLevelSounds: ${e.message}")
                    }
                }
                
                runOnSession {
                    it.displayClientMessage("§a[SoundModule] Тест LevelSound завершен!")
                }
            }.start()
        }
    }package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.GameSession

import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.math.vector.Vector3f

class SoundModule : Module("Sound", ModuleCategory.Misc) {

    // Список проверенных звуков для Nukkit-MOT
    private val workingSounds = listOf(
        "random.pop",
        "note.pling",
        "random.click",
        "random.orb",
        "mob.endermen.portal",
        "random.anvil_land",
        "random.break",
        "tile.piston.out",
        "mob.ghast.scream",
        "random.explode",
        "random.bow",
        "mob.zombie.say",
        "mob.skeleton.say",
        "random.levelup",
        "mob.enderdragon.growl"
    )

    override fun initialize() {
        super.initialize()
        runOnSession {
            it.displayClientMessage("§a[SoundModule] Модуль звуков успешно инициализирован.")
            it.displayClientMessage("§7Доступные звуки: ${workingSounds.size}")
        }
    }

    override fun onEnabled() {
        super.onEnabled()
        runOnSession {
            it.displayClientMessage("§a[SoundModule] Модуль активирован.")
            // Тестовый звук при включении
            playSound("random.pop", 1.0f, 1.0f)
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        runOnSession {
            it.displayClientMessage("§c[SoundModule] Модуль деактивирован.")
        }
    }

    /**
     * Воспроизведение обычного пользовательского звука по названию.
     * Оптимизировано для работы с Nukkit-MOT.
     */
    fun playSound(soundName: String, volume: Float = 1.0f, pitch: Float = 1.0f) {
        println("DEBUG: === НАЧАЛО ОТПРАВКИ ЗВУКА ===")
        println("DEBUG: Звук: $soundName, Громкость: $volume, Питч: $pitch")

        runOnSession { currentSession ->
            val player = currentSession.localPlayer
            println("DEBUG: Игрок найден: ${player != null}")
            
            if (player == null) {
                currentSession.displayClientMessage("§c[SoundModule] Игрок недоступен.")
                println("ERROR: Игрок равен null!")
                return@runOnSession
            }

            // Получаем точную позицию игрока
            val playerPos = try {
                player.vec3Position ?: Vector3f.from(
                    player.position.x,
                    player.position.y,
                    player.position.z
                )
            } catch (e: Exception) {
                println("ERROR: Ошибка получения позиции: ${e.message}")
                Vector3f.ZERO
            }

            println("DEBUG: Позиция игрока: $playerPos")

            // Создаем пакет звука
            val playSoundPacket = PlaySoundPacket().apply {
                setSound(soundName)
                setPosition(playerPos)
                setVolume(volume.coerceIn(0.0f, 10.0f)) // Ограничиваем громкость
                setPitch(pitch.coerceIn(0.1f, 2.0f))   // Ограничиваем питч
            }

            try {
                println("DEBUG: Пакет создан, отправляю на сервер...")
                currentSession.serverBound(playSoundPacket)
                println("DEBUG: Пакет успешно отправлен!")
                
                currentSession.displayClientMessage("§a[Sound] §b$soundName §7отправлен §8(V:$volume P:$pitch)")
            } catch (e: Exception) {
                println("ERROR: Ошибка отправки пакета: ${e.message}")
                currentSession.displayClientMessage("§c[Sound] Ошибка отправки: ${e.message}")
            }

            println("DEBUG: === КОНЕЦ ОТПРАВКИ ЗВУКА ===")
        }
    }

    /**
     * Воспроизведение системного игрового звука через LevelSoundEventPacket.
     * Более надежный способ для некоторых серверов.
     */
    fun playLevelSound(
        soundEvent: SoundEvent,
        identifier: String = "",
        extraData: Int = -1,
        volume: Float = 1.0f
    ) {
        println("DEBUG: === НАЧАЛО ОТПРАВКИ LEVEL SOUND ===")
        println("DEBUG: SoundEvent: $soundEvent, Identifier: $identifier, ExtraData: $extraData")

        runOnSession { currentSession ->
            val player = currentSession.localPlayer
            if (player == null) {
                currentSession.displayClientMessage("§c[SoundModule] Игрок недоступен для LevelSound.")
                println("ERROR: Игрок равен null в playLevelSound!")
                return@runOnSession
            }

            val playerPos = try {
                player.vec3Position ?: Vector3f.from(
                    player.position.x,
                    player.position.y,
                    player.position.z
                )
            } catch (e: Exception) {
                println("ERROR: Ошибка получения позиции в LevelSound: ${e.message}")
                Vector3f.ZERO
            }

            val packet = LevelSoundEventPacket().apply {
                sound = soundEvent
                position = playerPos
                this.extraData = extraData
                this.identifier = identifier
                // Правильные поля согласно структуре пакета
                babySound = false
                relativeVolumeDisabled = false
                entityUniqueId = player.runtimeEntityId ?: -1L
            }

            try {
                println("DEBUG: LevelSoundEventPacket создан, отправляю...")
                currentSession.serverBound(packet)
                println("DEBUG: LevelSoundEventPacket успешно отправлен!")
                
                currentSession.displayClientMessage("§a[LevelSound] §b$soundEvent §7отправлен на сервер")
            } catch (e: Exception) {
                println("ERROR: Ошибка отправки LevelSoundEventPacket: ${e.message}")
                currentSession.displayClientMessage("§c[LevelSound] Ошибка: ${e.message}")
            }

            println("DEBUG: === КОНЕЦ ОТПРАВКИ LEVEL SOUND ===")
        }
    }

    /**
     * Тестирование различных звуков подряд
     */
    fun testSounds() {
        runOnSession { session ->
            session.displayClientMessage("§e[SoundModule] §7Начинаю тест звуков...")
            
            Thread {
                workingSounds.take(5).forEachIndexed { index, sound ->
                    try {
                        Thread.sleep(1500L) // Пауза между звуками
                        playSound(sound, 1.0f, 1.0f)
                        
                        runOnSession { 
                            it.displayClientMessage("§7[${index + 1}/5] Тест: §b$sound")
                        }
                    } catch (e: Exception) {
                        println("ERROR в testSounds: ${e.message}")
                    }
                }
                
                runOnSession {
                    it.displayClientMessage("§a[SoundModule] §7Тест завершен!")
                }
            }.start()
        }
    }

    /**
     * Случайный звук из списка
     */
    fun playRandomSound(volume: Float = 1.0f, pitch: Float = 1.0f) {
        val randomSound = workingSounds.random()
        playSound(randomSound, volume, pitch)
        
        runOnSession {
            it.displayClientMessage("§e[Random] §b$randomSound")
        }
    }

    /**
     * Быстрый способ воспроизвести звук атаки (как в примере)
     */
    fun playAttackSound() {
        runOnSession { currentSession ->
            val player = currentSession.localPlayer
            if (player == null) {
                currentSession.displayClientMessage("§c[SoundModule] Игрок недоступен для звука атаки.")
                return@runOnSession
            }

            val packet = LevelSoundEventPacket().apply {
                sound = SoundEvent.ATTACK_NODAMAGE
                position = player.vec3Position ?: Vector3f.ZERO
                extraData = -1
                identifier = "minecraft:player"
                babySound = false
                relativeVolumeDisabled = false
                entityUniqueId = player.runtimeEntityId ?: -1L
            }

            try {
                currentSession.serverBound(packet)
                currentSession.displayClientMessage("§a[AttackSound] Звук атаки отправлен!")
                println("DEBUG: AttackSound отправлен с правильной структурой пакета")
            } catch (e: Exception) {
                println("ERROR: Ошибка отправки AttackSound: ${e.message}")
                currentSession.displayClientMessage("§c[AttackSound] Ошибка: ${e.message}")
            }
        }
    }

    /**
     * Универсальный метод для LevelSoundEvent с полным контролем параметров
     */
    fun playLevelSoundAdvanced(
        soundEvent: SoundEvent,
        identifier: String = "minecraft:player",
        extraData: Int = -1,
        babySound: Boolean = false,
        relativeVolumeDisabled: Boolean = false
    ) {
        println("DEBUG: === ADVANCED LEVEL SOUND ===")
        println("DEBUG: Event: $soundEvent, ID: $identifier, Extra: $extraData")

        runOnSession { currentSession ->
            val player = currentSession.localPlayer
            if (player == null) {
                currentSession.displayClientMessage("§c[SoundModule] Игрок недоступен.")
                return@runOnSession
            }

            val packet = LevelSoundEventPacket().apply {
                sound = soundEvent
                position = player.vec3Position ?: Vector3f.ZERO
                this.extraData = extraData
                this.identifier = identifier
                this.babySound = babySound
                this.relativeVolumeDisabled = relativeVolumeDisabled
                this.entityUniqueId = player.runtimeEntityId ?: -1L
            }

            try {
                currentSession.serverBound(packet)
                currentSession.displayClientMessage("§a[AdvancedLevel] §b$soundEvent §7отправлен")
                println("DEBUG: AdvancedLevelSound успешно отправлен")
            } catch (e: Exception) {
                println("ERROR: Ошибка в AdvancedLevelSound: ${e.message}")
                currentSession.displayClientMessage("§c[AdvancedLevel] Ошибка: ${e.message}")
            }
        }
    }

    /**
     * Воспроизвести звук с проверкой на существование
     */
    fun playSoundSafe(soundName: String, volume: Float = 1.0f, pitch: Float = 1.0f) {
        if (workingSounds.contains(soundName)) {
            playSound(soundName, volume, pitch)
        } else {
            runOnSession {
                it.displayClientMessage("§c[Sound] Неизвестный звук: §7$soundName")
                it.displayClientMessage("§7Используй: §e.sound list §7для просмотра доступных")
            }
        }
    }

    /**
     * Заглушка для остановки всех звуков
     */
    fun stopAllSounds() {
        runOnSession { currentSession ->
            // В Bedrock Edition нет прямого способа остановить все звуки
            // Можно попробовать отправить пакет с нулевой громкостью
            try {
                val silentPacket = PlaySoundPacket().apply {
                    setSound("random.click")
                    setPosition(currentSession.localPlayer?.vec3Position ?: Vector3f.ZERO)
                    setVolume(0.0f)
                    setPitch(1.0f)
                }
                currentSession.serverBound(silentPacket)
                
                currentSession.displayClientMessage("§e[SoundModule] Попытка заглушить звуки (ограниченная поддержка)")
                println("DEBUG: stopAllSounds() - отправлен тихий пакет")
            } catch (e: Exception) {
                currentSession.displayClientMessage("§c[SoundModule] Ошибка при заглушении: ${e.message}")
                println("ERROR в stopAllSounds: ${e.message}")
            }
        }
    }
}
