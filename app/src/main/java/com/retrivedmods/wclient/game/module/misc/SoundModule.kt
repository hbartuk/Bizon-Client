package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.GameSession // Убедитесь, что GameSession импортирован правильно

import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.math.vector.Vector3f

class SoundModule : Module("Sound", ModuleCategory.Misc) {

    override fun initialize() {
        super.initialize()
        runOnSession {
            it.displayClientMessage("§a[SoundModule] Модуль звуков успешно инициализирован.")
        }
    }

    override fun onEnabled() {
        super.onEnabled()
        runOnSession {
            it.displayClientMessage("§a[SoundModule] Дополнительная логика при активации.")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        runOnSession {
            it.displayClientMessage("§c[SoundModule] Дополнительная логика при деактивации.")
        }
    }

    /**
     * Воспроизведение обычного пользовательского звука по названию.
     * Отправляется на сервер, чтобы другие игроки тоже слышали.
     */
    fun playSound(soundName: String, volume: Float = 1.0f, pitch: Float = 1.0f) {
        println("DEBUG: SoundModule.playSound() вызван для звука: $soundName (Громкость: $volume, Тональность: $pitch)")

        runOnSession { currentSession ->
            val playerPos: Vector3f? = currentSession.localPlayer?.vec3Position
            if (playerPos == null) {
                currentSession.displayClientMessage("§c[SoundModule] Позиция игрока недоступна. Невозможно воспроизвести звук для сервера.")
                println("ERROR: Позиция игрока равна null в playSound(). Невозможно отправить запрос звука на сервер: $soundName")
                return@runOnSession
            }

            val playSoundPacket = PlaySoundPacket().apply {
                setSound(soundName)
                setPosition(playerPos)
                setVolume(volume)
                setPitch(pitch)
            }

            // *** ИЗМЕНЕНИЕ ЗДЕСЬ: Используем serverBound() для отправки на сервер ***
            currentSession.serverBound(playSoundPacket)

            currentSession.displayClientMessage("§a[SoundModule] Попытка отправить запрос звука серверу: §b$soundName")
            println("DEBUG: PlaySoundPacket отправлен на сервер для звука: $soundName с громкостью $volume и тональностью $pitch.")
        }
    }

    /**
     * Воспроизведение системного игрового звука через LevelSoundEventPacket.
     * Отправляется на сервер, чтобы другие игроки тоже слышали.
     */
    fun playLevelSound(
        soundEvent: SoundEvent,
        identifier: String = "",
        extraData: Int = -1
    ) {
        println("DEBUG: SoundModule.playLevelSound() вызван для события: $soundEvent")

        runOnSession { currentSession ->
            val playerPos: Vector3f? = currentSession.localPlayer?.vec3Position
            if (playerPos == null) {
                currentSession.displayClientMessage("§c[SoundModule] Позиция игрока недоступна. Невозможно воспроизвести событийный звук для сервера.")
                println("ERROR: Позиция игрока равна null в playLevelSound(). Невозможно отправить запрос события на сервер: $soundEvent")
                return@runOnSession
            }

            val packet = LevelSoundEventPacket().apply {
                sound = soundEvent
                position = playerPos
                this.extraData = extraData
                this.identifier = identifier
                // babySound, relativeVolumeDisabled, entityUniqueId НЕ ТРОГАТЬ! (приватные)
            }

            // *** ИЗМЕНЕНИЕ ЗДЕСЬ: Используем serverBound() для отправки на сервер ***
            currentSession.serverBound(packet)

            currentSession.displayClientMessage("§a[SoundModule] Попытка отправить событийный звук серверу: §b$soundEvent")
            println("DEBUG: LevelSoundEventPacket отправлен на сервер для события: $soundEvent на позиции $playerPos.")
        }
    }

    /**
     * Заглушка для остановки всех звуков (реализация зависит от логики клиента/прокси).
     */
    fun stopAllSounds() {
        runOnSession { currentSession ->
            currentSession.displayClientMessage("§e[SoundModule] Заглушка: функция 'stopAllSounds' не имеет прямой реализации в MCBE. Нужна кастомная логика.")
            println("DEBUG: stopAllSounds() вызвана. Нет прямого пакета MCBE. Требуется кастомная реализация.")
        }
    }
}
