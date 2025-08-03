package com.retrivedmods.wclient.game.module.misc

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

            // Получаем позицию игрока
            val playerPos = player.vec3Position ?: Vector3f.ZERO
            println("DEBUG: Позиция игрока: $playerPos")

            // Создаем пакет звука согласно структуре Nukkit-MOT
            val playSoundPacket = PlaySoundPacket().apply {
                // name в Nukkit-MOT
                sound = soundName
                // Позиция в блочных координатах (Nukkit-MOT использует int x,y,z)
                position = playerPos
                // volume и pitch как float
                volume = volume.coerceIn(0.0f, 10.0f)
                pitch = pitch.coerceIn(0.1f, 2.0f)
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
     * Адаптировано под структуру Nukkit-MOT.
     */
    fun playLevelSound(
        soundId: Int, // Используем int ID вместо SoundEvent
        identifier: String = "",
        extraData: Int = -1,
        isBaby: Boolean = false,
        isGlobal: Boolean = false
    ) {
        println("DEBUG: === НАЧАЛО ОТПРАВКИ LEVEL SOUND ===")
        println("DEBUG: SoundID: $soundId, Identifier: $identifier, ExtraData: $extraData")

        runOnSession { currentSession ->
            val player = currentSession.localPlayer
            if (player == null) {
                currentSession.displayClientMessage("§c[SoundModule] Игрок недоступен для LevelSound.")
                println("ERROR: Игрок равен null в playLevelSound!")
                return@runOnSession
            }

            val playerPos = player.vec3Position ?: Vector3f.ZERO

            // Создаем пакет согласно структуре Nukkit-MOT LevelSoundEventPacket
            val packet = LevelSoundEventPacket().apply {
                // Используем int sound ID
                sound = SoundEvent.fromId(soundId) ?: SoundEvent.ATTACK_NODAMAGE
                position = playerPos
                extraData = extraData
                identifier = identifier
                isBabySound = isBaby
                isRelativeVolumeDisabled = isGlobal
                // entityUniqueId доступен только в протоколе >= v1_21_70_24
                if (player.uniqueEntityId > 0) {
                    entityUniqueId = player.uniqueEntityId
                }
            }

            try {
                println("DEBUG: LevelSoundEventPacket создан, отправляю...")
                currentSession.serverBound(packet)
                println("DEBUG: LevelSoundEventPacket успешно отправлен!")
                
                currentSession.displayClientMessage("§a[LevelSound] §bID:$soundId §7отправлен на сервер")
            } catch (e: Exception) {
                println("ERROR: Ошибка отправки LevelSoundEventPacket: ${e.message}")
                currentSession.displayClientMessage("§c[LevelSound] Ошибка: ${e.message}")
            }

            println("DEBUG: === КОНЕЦ ОТПРАВКИ LEVEL SOUND ===")
        }
    }

    /**
     * Быстрый способ воспроизвести звук атаки
     */
    fun playAttackSound() {
        println("DEBUG: Играю звук атаки (SOUND_ATTACK_NODAMAGE = 42)")
        playLevelSound(42, "minecraft:player", -1, false, false) // SOUND_ATTACK_NODAMAGE = 42
    }

    /**
     * Воспроизведение звука шага
     */
    fun playStepSound() {
        println("DEBUG: Играю звук шага (SOUND_STEP = 2)")
        playLevelSound(2, "minecraft:player", -1, false, false) // SOUND_STEP = 2
    }

    /**
     * Воспроизведение звука удара
     */
    fun playHitSound() {
        println("DEBUG: Играю звук удара (SOUND_HIT = 1)")
        playLevelSound(1, "minecraft:player", -1, false, false) // SOUND_HIT = 1
    }

    /**
     * Воспроизведение звука размещения блока
     */
    fun playPlaceSound() {
        println("DEBUG: Играю звук размещения (SOUND_PLACE = 6)")
        playLevelSound(6, "minecraft:stone", -1, false, false) // SOUND_PLACE = 6
    }

    /**
     * Воспроизведение звука взрыва
     */
    fun playExplodeSound() {
        println("DEBUG: Играю звук взрыва (SOUND_EXPLODE = 48)")
        playLevelSound(48, "", -1, false, true) // SOUND_EXPLODE = 48, глобальный
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
     * Тест LevelSound событий с правильными ID
     */
    fun testLevelSounds() {
        val testSounds = mapOf(
            42 to "ATTACK_NODAMAGE",
            1 to "HIT", 
            2 to "STEP",
            6 to "PLACE",
            17 to "HURT",
            79 to "POP"
        )

        runOnSession { session ->
            session.displayClientMessage("§e[SoundModule] Тест LevelSound событий...")
            
            Thread {
                testSounds.entries.forEachIndexed { index, (soundId, name) ->
                    try {
                        Thread.sleep(2000L) // Пауза между звуками
                        playLevelSound(soundId)
                        
                        runOnSession { 
                            it.displayClientMessage("§7[${index + 1}/${testSounds.size}] §b$name §8(ID:$soundId)")
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
     * Случайный LevelSound
     */
    fun playRandomLevelSound() {
        val randomSounds = listOf(42, 1, 2, 6, 17, 79, 81, 62) // Популярные звуки
        val randomId = randomSounds.random()
        playLevelSound(randomId)
        
        runOnSession {
            it.displayClientMessage("§e[RandomLevel] §bID:$randomId")
        }
    }

    /**
     * Получить список доступных звуков
     */
    fun listAvailableSounds(): List<String> = workingSounds

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
     * Создание звукового спама (осторожно!)
     */
    fun playSoundSpam(soundName: String, count: Int = 5, delayMs: Long = 100L) {
        runOnSession { session ->
            session.displayClientMessage("§e[SoundSpam] Начинаю спам: §b$soundName §7x$count")
            
            Thread {
                repeat(count) { i ->
                    try {
                        playSound(soundName, 1.0f, 1.0f)
                        if (i < count - 1) Thread.sleep(delayMs)
                    } catch (e: Exception) {
                        println("ERROR в playSoundSpam: ${e.message}")
                    }
                }
                
                runOnSession {
                    it.displayClientMessage("§a[SoundSpam] Спам завершен!")
                }
            }.start()
        }
    }

    /**
     * Заглушка для остановки всех звуков
     */
    fun stopAllSounds() {
        runOnSession { currentSession ->
            try {
                val silentPacket = PlaySoundPacket().apply {
                    sound = "random.click"
                    position = currentSession.localPlayer?.vec3Position ?: Vector3f.ZERO
                    volume = 0.0f
                    pitch = 1.0f
                }
                currentSession.serverBound(silentPacket)
                
                currentSession.displayClientMessage("§e[SoundModule] Попытка заглушить звуки")
                println("DEBUG: stopAllSounds() - отправлен тихий пакет")
            } catch (e: Exception) {
                currentSession.displayClientMessage("§c[SoundModule] Ошибка при заглушении: ${e.message}")
                println("ERROR в stopAllSounds: ${e.message}")
            }
        }
    }
}
