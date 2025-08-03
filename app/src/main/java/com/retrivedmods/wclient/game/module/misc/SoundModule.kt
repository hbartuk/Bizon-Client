package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.GameSession

import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.math.vector.Vector3f

class SoundModule : Module("Sound", ModuleCategory.Misc) {

    private val workingSounds = listOf(
        "random.pop", "note.pling", "random.click", "random.orb", "mob.endermen.portal",
        "random.anvil_land", "random.break", "tile.piston.out", "mob.ghast.scream",
        "random.explode", "random.bow", "mob.zombie.say", "mob.skeleton.say",
        "random.levelup", "mob.enderdragon.growl"
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
            playSound("random.pop", 1.0f, 1.0f)
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        runOnSession {
            it.displayClientMessage("§c[SoundModule] Модуль деактивирован.")
        }
    }

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

            val playerPos = player.vec3Position ?: Vector3f.ZERO
            println("DEBUG: Позиция игрока: $playerPos")

            val playSoundPacket = PlaySoundPacket().apply {
                sound = soundName
                position = playerPos
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

    fun playLevelSound(
        soundId: Int,
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

            val packet = LevelSoundEventPacket().apply {
                sound = SoundEvent.values()[soundId] // ИСПРАВЛЕНО: Используем values()
                position = playerPos
                extraData = extraData
                identifier = identifier
                isBabySound = isBaby
                isRelativeVolumeDisabled = isGlobal
                // ИСПРАВЛЕНО: Закомментировал строку, так как она вызывает ошибку
                // if (player.uniqueEntityId > 0) {
                //     entityUniqueId = player.uniqueEntityId
                // }
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

    fun playAttackSound() {
        println("DEBUG: Играю звук атаки (SOUND_ATTACK_NODAMAGE = 42)")
        playLevelSound(42, "minecraft:player", -1, false, false)
    }

    fun playStepSound() {
        println("DEBUG: Играю звук шага (SOUND_STEP = 2)")
        playLevelSound(2, "minecraft:player", -1, false, false)
    }

    fun playHitSound() {
        println("DEBUG: Играю звук удара (SOUND_HIT = 1)")
        playLevelSound(1, "minecraft:player", -1, false, false)
    }

    fun playPlaceSound() {
        println("DEBUG: Играю звук размещения (SOUND_PLACE = 6)")
        playLevelSound(6, "minecraft:stone", -1, false, false)
    }

    fun playExplodeSound() {
        println("DEBUG: Играю звук взрыва (SOUND_EXPLODE = 48)")
        playLevelSound(48, "", -1, false, true)
    }

    fun testSounds() {
        runOnSession { session ->
            session.displayClientMessage("§e[SoundModule] §7Начинаю тест звуков...")
            
            Thread {
                workingSounds.take(5).forEachIndexed { index, sound ->
                    try {
                        Thread.sleep(1500L)
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
                        Thread.sleep(2000L)
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

    fun playRandomSound(volume: Float = 1.0f, pitch: Float = 1.0f) {
        val randomSound = workingSounds.random()
        playSound(randomSound, volume, pitch)
        
        runOnSession {
            it.displayClientMessage("§e[Random] §b$randomSound")
        }
    }

    fun playRandomLevelSound() {
        val randomSounds = listOf(42, 1, 2, 6, 17, 79, 81, 62)
        val randomId = randomSounds.random()
        playLevelSound(randomId)
        
        runOnSession {
            it.displayClientMessage("§e[RandomLevel] §bID:$randomId")
        }
    }

    fun listAvailableSounds(): List<String> = workingSounds

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
