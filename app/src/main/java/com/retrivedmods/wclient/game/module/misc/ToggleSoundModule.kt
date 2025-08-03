package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.GameSession

import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.math.vector.Vector3f

class ToggleSoundModule : Module("ToggleSound", ModuleCategory.Misc) {

    // Настройки звуковых наборов
    private var celestialMode = true
    private var nursultanMode = false
    private var smoothMode = false
    private var soundsEnabled = false

    // Наборы звуков (адаптированные под Nukkit-MOT)
    object SoundSets {
        val CELESTIAL = listOf(
            "note.pling",
            "random.orb",
            "mob.endermen.portal",
            "random.levelup"
        )

        val NURSULTAN = listOf(
            "random.pop",
            "random.click",
            "tile.piston.out",
            "random.bow"
        )

        val SMOOTH = listOf(
            "random.break",
            "mob.ghast.scream",
            "random.explode",
            "random.anvil_land"
        )
    }

    override fun initialize() {
        super.initialize()
        runOnSession {
            it.displayClientMessage("§a[ToggleSound] Модуль инициализирован.")
            it.displayClientMessage("§7Режимы: Celestial, Nursultan, Smooth")
        }
    }

    override fun onEnabled() {
        super.onEnabled()
        soundsEnabled = true
        
        runOnSession {
            it.displayClientMessage("§a[ToggleSound] Звуки включены!")
            
            // Воспроизводим звук включения в зависимости от режима
            when {
                celestialMode -> {
                    it.displayClientMessage("§b[Mode] Celestial активен")
                    playRandomFromSet(SoundSets.CELESTIAL)
                }
                nursultanMode -> {
                    it.displayClientMessage("§e[Mode] Nursultan активен")
                    playRandomFromSet(SoundSets.NURSULTAN)
                }
                smoothMode -> {
                    it.displayClientMessage("§d[Mode] Smooth активен")
                    playRandomFromSet(SoundSets.SMOOTH)
                }
            }
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        soundsEnabled = false
        
        runOnSession {
            it.displayClientMessage("§c[ToggleSound] Звуки отключены.")
        }
    }

    /**
     * Переключение режима Celestial
     */
    fun toggleCelestial() {
        celestialMode = true
        nursultanMode = false
        smoothMode = false
        
        runOnSession {
            it.displayClientMessage("§b[ToggleSound] Режим: Celestial")
            if (soundsEnabled) {
                playRandomFromSet(SoundSets.CELESTIAL)
            }
        }
    }

    /**
     * Переключение режима Nursultan
     */
    fun toggleNursultan() {
        celestialMode = false
        nursultanMode = true
        smoothMode = false
        
        runOnSession {
            it.displayClientMessage("§e[ToggleSound] Режим: Nursultan")
            if (soundsEnabled) {
                playRandomFromSet(SoundSets.NURSULTAN)
            }
        }
    }

    /**
     * Переключение режима Smooth
     */
    fun toggleSmooth() {
        celestialMode = false
        nursultanMode = false
        smoothMode = true
        
        runOnSession {
            it.displayClientMessage("§d[ToggleSound] Режим: Smooth")
            if (soundsEnabled) {
                playRandomFromSet(SoundSets.SMOOTH)
            }
        }
    }

    /**
     * Воспроизведение случайного звука из набора
     */
    private fun playRandomFromSet(soundSet: List<String>) {
        if (!soundsEnabled) return
        
        val randomSound = soundSet.random()
        playSound(randomSound, 1.0f, 1.0f)
        
        runOnSession {
            it.displayClientMessage("§7♪ $randomSound")
        }
    }

    /**
     * Воспроизведение звука (исправленная версия для Nukkit-MOT)
     */
    private fun playSound(soundName: String, volume: Float = 1.0f, pitch: Float = 1.0f) {
        println("DEBUG: ToggleSound playing: $soundName")

        runOnSession { currentSession ->
            val player = currentSession.localPlayer ?: return@runOnSession

            val playerPos = player.vec3Position ?: Vector3f.ZERO

            val playSoundPacket = PlaySoundPacket().apply {
                sound = soundName
                position = playerPos
                volume = volume.coerceIn(0.0f, 10.0f)
                pitch = pitch.coerceIn(0.1f, 2.0f)
            }

            try {
                currentSession.serverBound(playSoundPacket)
                println("DEBUG: ToggleSound packet sent: $soundName")
            } catch (e: Exception) {
                println("ERROR: ToggleSound failed to send: ${e.message}")
            }
        }
    }

    /**
     * Воспроизведение звука при событии (например, включение модуля)
     */
    fun playToggleSound() {
        if (!soundsEnabled) return

        when {
            celestialMode -> playRandomFromSet(SoundSets.CELESTIAL)
            nursultanMode -> playRandomFromSet(SoundSets.NURSULTAN)
            smoothMode -> playRandomFromSet(SoundSets.SMOOTH)
            else -> playSound("note.pling", 1.0f, 1.0f)
        }
    }

    /**
     * Воспроизведение звука уведомления
     */
    fun playNotificationSound() {
        if (!soundsEnabled) return

        // Особые звуки для уведомлений
        val notificationSounds = listOf(
            "random.orb",
            "note.pling",
            "random.levelup"
        )

        val sound = notificationSounds.random()
        playSound(sound, 0.8f, 1.2f)
        
        runOnSession {
            it.displayClientMessage("§a[Notification] §7♪ $sound")
        }
    }

    /**
     * Проверка состояния звуков
     */
    fun isSoundsEnabled(): Boolean = soundsEnabled

    /**
     * Получить текущий активный режим
     */
    fun getCurrentMode(): String {
        return when {
            celestialMode -> "Celestial"
            nursultanMode -> "Nursultan"
            smoothMode -> "Smooth"
            else -> "None"
        }
    }

    /**
     * Получить звуки текущего режима
     */
    fun getCurrentSoundSet(): List<String> {
        return when {
            celestialMode -> SoundSets.CELESTIAL
            nursultanMode -> SoundSets.NURSULTAN
            smoothMode -> SoundSets.SMOOTH
            else -> emptyList()
        }
    }

    /**
     * Тест всех режимов подряд
     */
    fun testAllModes() {
        runOnSession { session ->
            session.displayClientMessage("§e[ToggleSound] Тестирую все режимы...")
            
            Thread {
                try {
                    // Тест Celestial
                    toggleCelestial()
                    Thread.sleep(2000)
                    
                    // Тест Nursultan
                    toggleNursultan()
                    Thread.sleep(2000)
                    
                    // Тест Smooth
                    toggleSmooth()
                    Thread.sleep(2000)
                    
                    runOnSession {
                         it.displayClientMessage("§a[ToggleSound] Тест завершен!")
                    }
                } catch (e: Exception) {
                    println("ERROR в testAllModes: ${e.message}")
                }
            }.start()
        }
    }
}
