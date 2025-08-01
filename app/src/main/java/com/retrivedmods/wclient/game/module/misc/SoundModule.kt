// File: app/src/main/java/com/retrivedmods/wclient/game/module/misc/SoundModule.kt
package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.GameSession
import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.math.vector.Vector3f
import kotlin.random.Random

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

    override fun onEnabled() {
        super.onEnabled()
        // Метод runOnSession больше не нужен, так как onEnabled вызывается только
        // когда session уже инициализирован.
        session?.displayClientMessage("§a[SoundModule] Модуль активирован.")
        // Тестовый звук при включении
        playSound("random.pop", 1.0f, 1.0f)
    }

    override fun onDisabled() {
        super.onDisabled()
        session?.displayClientMessage("§c[SoundModule] Модуль деактивирован.")
    }

    /**
     * Воспроизведение обычного пользовательского звука по названию.
     * Оптимизировано для работы с Nukkit-MOT.
     */
    fun playSound(soundName: String, volume: Float = 1.0f, pitch: Float = 1.0f) {
        val currentSession = session ?: return
        val player = currentSession.localPlayer
        if (player == null) {
            currentSession.displayClientMessage("§c[SoundModule] Игрок недоступен.")
            return
        }

        val playerPos = player.vec3Position ?: Vector3f.ZERO
        val playSoundPacket = PlaySoundPacket().apply {
            setSound(soundName)
            setPosition(playerPos)
            setVolume(volume.coerceIn(0.0f, 10.0f))
            setPitch(pitch.coerceIn(0.1f, 2.0f))
        }

        currentSession.serverBound(playSoundPacket)
        currentSession.displayClientMessage("§a[Sound] §b$soundName §7отправлен §8(V:$volume P:$pitch)")
    }

    /**
     * Воспроизведение системного игрового звука через LevelSoundEventPacket.
     * Более надежный способ для некоторых серверов.
     */
    fun playLevelSound(
        soundEvent: SoundEvent,
        identifier: String = "",
        extraData: Int = -1
    ) {
        val currentSession = session ?: return
        val player = currentSession.localPlayer
        if (player == null) {
            currentSession.displayClientMessage("§c[SoundModule] Игрок недоступен для LevelSound.")
            return
        }

        val playerPos = player.vec3Position ?: Vector3f.ZERO
        val packet = LevelSoundEventPacket().apply {
            sound = soundEvent
            position = playerPos
            this.extraData = extraData
            this.identifier = identifier
            babySound = false
            relativeVolumeDisabled = false
            entityUniqueId = player.runtimeEntityId ?: -1L
        }

        currentSession.serverBound(packet)
        currentSession.displayClientMessage("§a[LevelSound] §b$soundEvent §7отправлен на сервер")
    }

    /**
     * Получить список доступных звуков
     */
    fun listAvailableSounds(): List<String> = workingSounds

    /**
     * Тестирование различных звуков подряд
     */
    fun testSounds() {
        val currentSession = session ?: return
        currentSession.displayClientMessage("§e[SoundModule] §7Начинаю тест звуков...")

        Thread {
            workingSounds.take(5).forEachIndexed { index, sound ->
                try {
                    Thread.sleep(1500L)
                    playSound(sound, 1.0f, 1.0f)
                    session?.displayClientMessage("§7[${index + 1}/5] Тест: §b$sound")
                } catch (e: Exception) {
                    println("ERROR в testSounds: ${e.message}")
                }
            }
            session?.displayClientMessage("§a[SoundModule] §7Тест завершен!")
        }.start()
    }

    /**
     * Случайный звук из списка
     */
    fun playRandomSound(volume: Float = 1.0f, pitch: Float = 1.0f) {
        val randomSound = workingSounds.random()
        playSound(randomSound, volume, pitch)
        session?.displayClientMessage("§e[Random] §b$randomSound")
    }

    /**
     * Быстрый способ воспроизвести звук атаки (как в примере)
     */
    fun playAttackSound() {
        val currentSession = session ?: return
        val player = currentSession.localPlayer
        if (player == null) {
            currentSession.displayClientMessage("§c[SoundModule] Игрок недоступен для звука атаки.")
            return
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

        currentSession.serverBound(packet)
        currentSession.displayClientMessage("§a[AttackSound] Звук атаки отправлен!")
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
        val currentSession = session ?: return
        val player = currentSession.localPlayer
        if (player == null) {
            currentSession.displayClientMessage("§c[SoundModule] Игрок недоступен.")
            return
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

        currentSession.serverBound(packet)
        currentSession.displayClientMessage("§a[AdvancedLevel] §b$soundEvent §7отправлен")
    }

    /**
     * Воспроизвести звук с проверкой на существование
     */
    fun playSoundSafe(soundName: String, volume: Float = 1.0f, pitch: Float = 1.0f) {
        val currentSession = session ?: return
        if (workingSounds.contains(soundName)) {
            playSound(soundName, volume, pitch)
        } else {
            currentSession.displayClientMessage("§c[Sound] Неизвестный звук: §7$soundName")
            currentSession.displayClientMessage("§7Используй: §e.sound list §7для просмотра доступных")
        }
    }

    /**
     * Заглушка для остановки всех звуков
     */
    fun stopAllSounds() {
        val currentSession = session ?: return
        val playerPos = currentSession.localPlayer?.vec3Position ?: Vector3f.ZERO
        
        val silentPacket = PlaySoundPacket().apply {
            setSound("random.click")
            setPosition(playerPos)
            setVolume(0.0f)
            setPitch(1.0f)
        }
        currentSession.serverBound(silentPacket)
        currentSession.displayClientMessage("§e[SoundModule] Попытка заглушить звуки (ограниченная поддержка)")
    }
}
