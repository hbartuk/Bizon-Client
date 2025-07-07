// File: app/src/main/java/com/retrivedmods/wclient/game/module/misc/SoundModule.kt
package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory // Убедитесь, что этот путь верен

import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class SoundModule() : Module("Sound", ModuleCategory.MISC) { // Убедитесь, что ModuleCategory.MISC существует

    override lateinit var session: GameSession

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val activeSounds = mutableMapOf<String, ScheduledFuture<*>>()

    // soundEventMap теперь пуст. Вы будете передавать идентификаторы звуков напрямую.
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

    // Измененный метод playSound для приема строкового идентификатора звука
    fun playSound(
        soundIdentifier: String, // Теперь это строковый идентификатор звука (например, "minecraft:random.click")
        volume: Float,
        distance: Float,
        soundsPerSecond: Int,
        durationSeconds: Int
    ) {
        if (!isSessionCreated) {
            return
        }

        // Мы больше не ищем звук в soundEventMap, а используем переданный soundIdentifier
        if (soundIdentifier.isBlank()) {
            session.displayClientMessage("§c[SoundModule] Идентификатор звука не может быть пустым.")
            return
        }

        stopSound(soundIdentifier) // Используем soundIdentifier как ключ для остановки

        val initialPosition = session.localPlayer.vec3Position

        session.displayClientMessage("§a[SoundModule] Воспроизведение звука: §b$soundIdentifier§a (громкость: §b$volume§a, дистанция: §b$distance§a, частота: §b$soundsPerSecond§a/с, длительность: §b$durationSeconds§a сек.)")

        val periodMillis = if (soundsPerSecond > 0) (1000L / soundsPerSecond) else 0L
        val extraDataValue = (distance * 1000).toInt() // Для некоторых SoundEvent это может быть неверно, но для SOUND_DEFINITION_EVENT это обычно -1

        val task = scheduler.scheduleAtFixedRate({
            if (isSessionCreated) {
                val packet = LevelSoundEventPacket().apply {
                    sound = SoundEvent.SOUND_DEFINITION_EVENT // Используем это для воспроизведения звука по строковому идентификатору
                    position = initialPosition
                    volume = volume
                    isBabySound = false
                    isRelativeVolumeDisabled = false
                    identifier = soundIdentifier // Здесь передаем ваш строковый идентификатор звука
                    extraData = extraDataValue // Или -1, в зависимости от типа звука
                }
                session.serverBound(packet)
                session.clientBound(packet)
            }
        }, 0, periodMillis, TimeUnit.MILLISECONDS)

        activeSounds[soundIdentifier.toLowerCase()] = task // Используем soundIdentifier как ключ

        scheduler.schedule({
            stopSound(soundIdentifier)
            if (isSessionCreated) {
                session.displayClientMessage("§a[SoundModule] Воспроизведение звука '$soundIdentifier' завершено.")
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
