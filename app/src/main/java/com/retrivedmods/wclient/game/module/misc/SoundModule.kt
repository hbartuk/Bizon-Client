// File: app/src/main/java/com/retrivedmods/wclient/game/module/misc/SoundModule.kt
package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory

import org.cloudburstmc.math.vector.Vector3f
// import org.cloudburstmc.protocol.bedrock.data.SoundEvent // Это больше не нужно для PlaySoundPacket
// import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket // Это больше не нужно
import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket // <-- Импортируйте PlaySoundPacket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class SoundModule() : Module("Sound", ModuleCategory.Misc) {

    override lateinit var session: GameSession

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val activeSounds = mutableMapOf<String, ScheduledFuture<*>>()

    // private val allSoundEvents: Array<SoundEvent> = SoundEvent.values() // Это больше не нужно

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
        soundId: Int, // Этот параметр теперь может быть не совсем актуален, если sound - это String
        volume: Float,
        distance: Float, // Этот параметр тоже может быть не совсем актуален для PlaySoundPacket
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
        // val extraDataValue = (distance * 1000).toInt() // Не нужно для PlaySoundPacket

        // PlaySoundPacket использует String для имени звука, а не SoundEvent
        // Вам нужно будет решить, как преобразовать soundId в String имя звука
        val targetSoundName: String = soundNameForDisplay // Или как-то иначе получить имя звука

        val task = scheduler.scheduleAtFixedRate({
            if (isSessionCreated) {
                // Создаем пакет PlaySoundPacket
                val packet = PlaySoundPacket()
                packet.sound = targetSoundName // Имя звука (String)
                packet.position = initialPosition
                packet.volume = volume
                packet.pitch = 1.0f // Установите желаемый pitch, например, по умолчанию 1.0f

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
