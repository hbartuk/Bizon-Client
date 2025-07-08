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

    // 'lateinit' указывает, что session будет инициализирована позже,
    // но до первого использования. ModuleManager заботится об этом.
    override lateinit var session: GameSession

    // Шедулер для планирования повторного воспроизведения звуков
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    // Карта для отслеживания активных запланированных звуков,
    // чтобы их можно было остановить по идентификатору.
    private val activeSounds = mutableMapOf<String, ScheduledFuture<*>>()

    /**
     * Вызывается, когда модуль включается.
     * Отправляет сообщение в чат клиента.
     */
    override fun onEnabled() {
        super.onEnabled() // Вызываем родительский метод для обработки isEnabled и отправки сообщения
        if (isSessionCreated) { // Проверяем, инициализирована ли сессия
            session.displayClientMessage("§a[SoundModule] Модуль активирован.")
        }
    }

    /**
     * Вызывается, когда модуль выключается.
     * Останавливает все активные звуки и отправляет сообщение в чат.
     */
    override fun onDisabled() {
        super.onDisabled() // Вызываем родительский метод
        stopAllSounds() // Останавливаем все звуки при деактивации модуля
        if (isSessionCreated) {
            session.displayClientMessage("§c[SoundModule] Модуль деактивирован. Все активные звуки остановлены.")
        }
    }

    /**
     * Воспроизводит указанный звук в текущей позиции игрока.
     *
     * @param soundName Строковое имя звука Minecraft Bedrock (например, "block.chest.open").
     * @param volume Громкость звука (0.0f - 1.0f).
     * @param soundsPerSecond Частота воспроизведения звука в секунду (0 для однократного воспроизведения).
     * @param durationSeconds Длительность воспроизведения звука в секундах.
     */
    fun playSound(
        soundName: String,
        volume: Float,
        soundsPerSecond: Int,
        durationSeconds: Int
    ) {
        // Проверяем, инициализирована ли сессия
        if (!isSessionCreated) {
            session.displayClientMessage("§c[SoundModule] Сессия не инициализирована. Невозможно воспроизвести звук.")
            return
        }

        val stopKey = soundName.lowercase() // Используем имя звука как ключ для остановки

        stopSound(stopKey) // Останавливаем предыдущее воспроизведение этого звука, если оно есть

        val initialPosition = session.localPlayer.vec3Position
        // Проверка на null для initialPosition, так как игрок может быть не загружен
        if (initialPosition == null) {
            session.displayClientMessage("§c[SoundModule] Невозможно воспроизвести звук: позиция игрока неизвестна.")
            return
        }

        session.displayClientMessage("§a[SoundModule] Воспроизведение звука: §b$soundName§a (громкость: §b$volume§a, частота: §b$soundsPerSecond§a/с, длительность: §b$durationSeconds§a сек.)")

        // Вычисляем период между воспроизведениями
        val periodMillis = if (soundsPerSecond > 0) (1000L / soundsPerSecond) else 0L

        // Запускаем задачу по расписанию
        val task = scheduler.scheduleAtFixedRate({
            // Убеждаемся, что сессия все еще активна перед отправкой пакета
            if (isSessionCreated) {
                val packet = PlaySoundPacket()
                packet.sound = soundName      // Устанавливаем строковое имя звука
                packet.position = initialPosition // Позиция воспроизведения звука (позиция игрока)
                packet.volume = volume        // Громкость звука
                packet.pitch = 1.0f           // Высота тона (1.0f - стандартная)

                // Отправляем пакет на сервер (чтобы другие игроки могли слышать)
                session.serverBound(packet)
                // Отправляем пакет самому себе (для локального воспроизведения)
                session.clientBound(packet)
            }
        }, 0, periodMillis, TimeUnit.MILLISECONDS) // Начинаем немедленно, повторяем каждые periodMillis

        activeSounds[stopKey] = task // Сохраняем задачу, чтобы потом её можно было отменить

        // Планируем задачу на остановку звука после указанной длительности
        if (durationSeconds > 0) {
            scheduler.schedule({
                stopSound(stopKey)
                if (isSessionCreated) {
                    session.displayClientMessage("§a[SoundModule] Воспроизведение звука '$soundName' завершено.")
                }
            }, durationSeconds.toLong(), TimeUnit.SECONDS)
        }
    }

    /**
     * Останавливает воспроизведение конкретного звука по его идентификатору.
     *
     * @param soundIdentifier Идентификатор звука (имя звука, которое использовалось при вызове playSound).
     */
    fun stopSound(soundIdentifier: String) {
        // Ищем и отменяем запланированную задачу
        activeSounds.remove(soundIdentifier.lowercase())?.cancel(false)
    }

    /**
     * Останавливает воспроизведение всех активных звуков.
     */
    fun stopAllSounds() {
        activeSounds.values.forEach { it.cancel(false) } // Отменяем все задачи
        activeSounds.clear() // Очищаем карту
        if (isSessionCreated) {
            session.displayClientMessage("§a[SoundModule] Все звуки остановлены.")
        }
    }

    /**
     * Вызывается при отключении от сервера.
     * Останавливает все активные звуки, чтобы они не продолжали играть.
     */
    override fun onDisconnect(reason: String) {
        super.onDisconnect(reason)
        stopAllSounds()
    }
}
