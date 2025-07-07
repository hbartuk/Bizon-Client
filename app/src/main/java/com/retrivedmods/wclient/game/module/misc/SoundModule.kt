// File: app/src/main/java/com/retrivedmods/wclient/game/module/misc/SoundModule.kt
package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory

import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class SoundModule() : Module("Sound", ModuleCategory.Misc) {

    override lateinit var session: GameSession

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val activeSounds = mutableMapOf<String, ScheduledFuture<*>>()

    private val allSoundEvents: Array<SoundEvent> = SoundEvent.values()

    override fun onEnabled() {
        super.onEnabled()
        if (isSessionCreated) {
            session.displayClientMessage("§a[SoundModule] Модуль активирован.")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        stopAllSounds()
        if (isSessionCreated) {
            session.displayClientMessage("§c[SoundModule] Модуль деактивирован. Все активные звуки остановлены.")
        }
    }

    fun playSound(
        soundId: Int,
        volume: Float,
        distance: Float,
        soundsPerSecond: Int,
        durationSeconds: Int,
        soundNameForDisplay: String = soundId.toString()
    ) {
        if (!isSessionCreated) {
            return
        }

        val stopKey = soundNameForDisplay.lowercase()

        stopSound(stopKey)

        val initialPosition = session.localPlayer.vec3Position

        session.displayClientMessage("§a[SoundModule] Воспроизведение звука: §b$soundNameForDisplay§a (громкость: §b$volume§a, дистанция: §b$distance§a, частота: §b$soundsPerSecond§a/с, длительность: §b$durationSeconds§a сек.)")

        val periodMillis = if (soundsPerSecond > 0) (1000L / soundsPerSecond) else 0L
        val extraDataValue = (distance * 1000).toInt()

        // Определяем SoundEvent до создания пакета
        val targetSoundEvent = if (soundId >= 0 && soundId < allSoundEvents.size) {
            allSoundEvents[soundId]
        } else {
            SoundEvent.UNDEFINED
        }

        val task = scheduler.scheduleAtFixedRate({
            if (isSessionCreated) {
                // Создаем пакет и присваиваем свойства явно
                val packet = LevelSoundEventPacket()
                packet.sound = targetSoundEvent // Это строка 78
                packet.position = initialPosition
                packet.volume = volume
                packet.isBabySound = false
                packet.isRelativeVolumeDisabled = false
                packet.extraData = extraDataValue

                session.serverBound(packet)
                session.clientBound(packet)
            }
        }, 0, periodMillis, TimeUnit.MILLISECONDS)

        activeSounds[stopKey] = task

        scheduler.schedule({
            stopSound(stopKey)
            if (isSessionCreated) {
                session.displayClientMessage("§a[SoundModule] Воспроизведение звука '$soundNameForDisplay' завершено.")
            }
        }, durationSeconds.toLong(), TimeUnit.SECONDS)
    }

    fun stopSound(soundIdentifier: String) {
        activeSounds.remove(soundIdentifier.lowercase())?.cancel(false)
    }

    fun stopAllSounds() {
        activeSounds.values.forEach { it.cancel(false) }
        activeSounds.clear()
        if (isSessionCreated) {
            session.displayClientMessage("§a[SoundModule] Все звуки остановлены.")
        }
    }
}
