// File: app/src/main/java/com/retrivedmods/wclient/game/module/misc/SoundModule.kt
package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory

import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class SoundModule() : Module("Sound", ModuleCategory.Misc) {

    override lateinit var session: GameSession

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val activeSounds = mutableMapOf<String, ScheduledFuture<*>>()

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

    // Изменена сигнатура функции: теперь принимает soundName (String)
    fun playSound(
        soundName: String, // <--- Имя звука (строка)
        volume: Float,
        // distance: Float, // Этот параметр не используется в PlaySoundPacket, его можно убрать
        soundsPerSecond: Int,
        durationSeconds: Int
    ) {
        if (!isSessionCreated) {
            return
        }

        val stopKey = soundName.lowercase()

        stopSound(stopKey)

        val initialPosition = session.localPlayer.vec3Position
        // Проверка на null для initialPosition
        if (initialPosition == null) {
            session.displayClientMessage("§c[SoundModule] Невозможно воспроизвести звук: позиция игрока неизвестна.")
            return
        }


        session.displayClientMessage("§a[SoundModule] Воспроизведение звука: §b$soundName§a (громкость: §b$volume§a, частота: §b$soundsPerSecond§a/с, длительность: §b$durationSeconds§a сек.)")

        val periodMillis = if (soundsPerSecond > 0) (1000L / soundsPerSecond) else 0L

        val task = scheduler.scheduleAtFixedRate({
            if (isSessionCreated) {
                val packet = PlaySoundPacket()
                packet.sound = soundName // <--- Теперь это уже корректное строковое имя звука
                packet.position = initialPosition
                packet.volume = volume
                packet.pitch = 1.0f // Можно сделать настраиваемым, если нужно

                session.serverBound(packet) // Отправляем на сервер (если хотим, чтобы слышали другие)
                session.clientBound(packet) // Отправляем себе (для локального воспроизведения)
            }
        }, 0, periodMillis, TimeUnit.MILLISECONDS)

        activeSounds[stopKey] = task

        // Планируем остановку звука
        scheduler.schedule({
            stopSound(stopKey)
            if (isSessionCreated) {
                session.displayClientMessage("§a[SoundModule] Воспроизведение звука '$soundName' завершено.")
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
