// File: app/src/main/java/com/retrivedmods/wclient/game/module/misc/SoundModule.kt
package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory // Правильный путь

import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

// ИСПРАВЛЕНИЕ: Используем 'ModuleCategory.Misc' (с большой 'M')
class SoundModule() : Module("Sound", ModuleCategory.Misc) { // Теперь должно компилироваться

    override lateinit var session: GameSession

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val activeSounds = mutableMapOf<String, ScheduledFuture<*>>()

    private val soundEventMap: Map<String, SoundEvent> = emptyMap()

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
        soundId: Int, // Целочисленный ID звука
        volume: Float,
        distance: Float,
        soundsPerSecond: Int,
        durationSeconds: Int,
        soundNameForDisplay: String = soundId.toString()
    ) {
        if (!isSessionCreated) {
            return
        }

        val stopKey = soundNameForDisplay.toLowerCase() // Используем toLowerCase()

        stopSound(stopKey)

        val initialPosition = session.localPlayer.vec3Position

        session.displayClientMessage("§a[SoundModule] Воспроизведение звука: §b$soundNameForDisplay§a (громкость: §b$volume§a, дистанция: §b$distance§a, частота: §b$soundsPerSecond§a/с, длительность: §b$durationSeconds§a сек.)")

        val periodMillis = if (soundsPerSecond > 0) (1000L / soundsPerSecond) else 0L
        val extraDataValue = (distance * 1000).toInt()

        val task = scheduler.scheduleAtFixedRate({
            if (isSessionCreated) {
                val packet = LevelSoundEventPacket().apply {
                    sound = SoundEvent.from(soundId) // Используем числовой ID
                    position = initialPosition
                    volume = volume
                    isBabySound = false
                    isRelativeVolumeDisabled = false
                    extraData = extraDataValue
                }
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
        activeSounds.remove(soundIdentifier.toLowerCase())?.cancel(false)
    }

    fun stopAllSounds() {
        activeSounds.values.forEach { it.cancel(false) }
        activeSounds.clear()
        if (isSessionCreated) {
            session.displayClientMessage("§a[SoundModule] Все звуки остановлены.")
        }
    }
}
