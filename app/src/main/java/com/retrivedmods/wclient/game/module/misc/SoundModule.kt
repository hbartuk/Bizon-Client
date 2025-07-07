// File: app/src/main/java/com/retrivedmods/wclient/game/module/impl/SoundModule.kt
package com.retrivedmods.wclient.game.module.impl // Убедитесь, что это ваш реальный пакет для модулей

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.module.Module // Ваш базовый класс Module
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class SoundModule(session: GameSession) : Module(session, "Sound", "Управление звуками клиента") {

    // Executor для планирования повторяющихся звуков
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val activeSounds = mutableMapOf<String, ScheduledFuture<*>>()

    // Словарь для маппинга названий звуков в SoundEvent
    // ЭТО ПРИМЕР! Вам нужно будет добавить больше звуков или найти полный список SoundEvent.
    private val soundEventMap = mapOf(
        "step" to SoundEvent.STEP,
        "explode" to SoundEvent.EXPLODE,
        "click" to SoundEvent.UI_BUTTON_CLICK,
        "place" to SoundEvent.PLACE,
        "break" to SoundEvent.BREAK,
        "levelup" to SoundEvent.LEVELUP,
        "attack" to SoundEvent.ATTACK_STRONG,
        "drink" to SoundEvent.DRINK
        // Добавьте сюда больше звуков по мере необходимости
    )

    override fun onEnable() {
        session.displayClientMessage("§a[SoundModule] Модуль звуков активирован.")
    }

    override fun onDisable() {
        stopAllSounds()
        session.displayClientMessage("§c[SoundModule] Модуль звуков деактивирован. Все активные звуки остановлены.")
    }

    // Метод для воспроизведения звука
    fun playSound(
        soundName: String,
        volume: Float,
        distance: Float, // Влияет на 'extraData'
        soundsPerSecond: Int,
        durationSeconds: Int
    ) {
        val soundEvent = soundEventMap[soundName.lowercase()]
        if (soundEvent == null) {
            session.displayClientMessage("§c[SoundModule] Звук '$soundName' не найден. Проверьте список доступных звуков.")
            return
        }

        // Останавливаем предыдущий звук, если он с таким же именем
        stopSound(soundName)

        val initialPosition = session.localPlayer.vec3Position // Берем текущую позицию игрока

        session.displayClientMessage("§a[SoundModule] Воспроизвожу звук: §b$soundName§a (громкость: §b$volume§a, дальность: §b$distance§a, кол-во/сек: §b$soundsPerSecond§a, длительность: §b$durationSeconds§a сек.)")

        val periodMillis = if (soundsPerSecond > 0) (1000L / soundsPerSecond) else 0L

        // extraData в LevelSoundEventPacket для дальности
        // Для многих звуков Minecraft extraData - это целое число, которое может влиять на метаданные звука.
        // Для дальности обычно используется scaling factor или просто игнорируется в клиенте.
        // В Bedrock протоколе, 'extraData' может использоваться для кодирования дальности,
        // но это зависит от конкретного SoundEvent и клиента/сервера.
        // Для простоты, мы будем использовать его как коэффициент масштабирования.
        val extraDataValue = (distance * 1000).toInt() // Пример: 1.0f дальность -> 1000 extraData

        val task = scheduler.scheduleAtFixedRate({
            val packet = LevelSoundEventPacket().apply {
                sound = soundEvent
                position = initialPosition
                volume = volume
                isBabySound = false
                isRelativeVolumeDisabled = false
                identifier = "minecraft:player" // Или пустая строка, если это общий звук
                extraData = extraDataValue // Используем для дальности
            }
            session.serverBound(packet) // Отправляем пакет на сервер (чтобы другие игроки слышали)
            session.clientBound(packet) // Отправляем пакет клиенту (чтобы вы слышали)
        }, 0, periodMillis, TimeUnit.MILLISECONDS)

        activeSounds[soundName.lowercase()] = task

        // Планируем остановку звука через заданное время
        scheduler.schedule({
            stopSound(soundName)
            session.displayClientMessage("§a[SoundModule] Звук '$soundName' завершил воспроизведение.")
        }, durationSeconds.toLong(), TimeUnit.SECONDS)
    }

    // Метод для остановки конкретного звука
    fun stopSound(soundName: String) {
        activeSounds.remove(soundName.lowercase())?.cancel(false)
    }

    // Метод для остановки всех активных звуков
    fun stopAllSounds() {
        activeSounds.values.forEach { it.cancel(false) }
        activeSounds.clear()
        session.displayClientMessage("§a[SoundModule] Все звуки остановлены.")
    }
}
