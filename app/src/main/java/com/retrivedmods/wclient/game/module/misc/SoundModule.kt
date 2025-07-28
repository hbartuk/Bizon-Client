package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.GameSession

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
     * Используется для ambient, музыки, стандартных звуков блоков и мобов.
     */
    fun playSound(soundName: String, volume: Float, pitch: Float) {
        println("DEBUG: SoundModule.playSound() вызван для звука: $soundName (Громкость: $volume, Тональность: $pitch)")

        runOnSession { currentSession ->
            if (currentSession.muCuteRelaySession == null) {
                currentSession.displayClientMessage("§c[SoundModule] MuCuteRelaySession недоступна для воспроизведения звука.")
                println("ERROR: currentSession.muCuteRelaySession равна null в playSound(). Невозможно воспроизвести звук: $soundName")
                return@runOnSession
            }

            val localPlayer = currentSession.localPlayer
            val playerPos: Vector3f? = localPlayer?.vec3Position

            if (playerPos == null) {
                currentSession.displayClientMessage("§c[SoundModule] Позиция игрока недоступна. Невозможно воспроизвести звук.")
                println("ERROR: Позиция игрока равна null в playSound(). Невозможно воспроизвести звук: $soundName")
                return@runOnSession
            }

            val playSoundPacket = PlaySoundPacket().apply {
                setSound(soundName)
                setPosition(playerPos)
                setVolume(volume)
                setPitch(pitch)
            }

            currentSession.clientBound(playSoundPacket)
            currentSession.displayClientMessage("§a[SoundModule] Попытка воспроизвести звук: §b$soundName")
            println("DEBUG: PlaySoundPacket отправлен для звука: $soundName с громкостью $volume и тональностью $pitch.")
        }
    }

    /**
     * Воспроизведение системного игрового звука через LevelSoundEventPacket.
     * Используется для звуков, связанных с игровыми событиями (колокол, смерть дракона, удары, взрывы и др.).
     */
    fun playLevelSound(
        soundEvent: SoundEvent,
        identifier: String = "",
        extraData: Int = -1,
        babySound: Boolean = false,
        relativeVolumeDisabled: Boolean = false,
        entityUniqueId: Long = 0L
    ) {
        println("DEBUG: SoundModule.playLevelSound() вызван для события: $soundEvent")

        runOnSession { currentSession ->
            if (currentSession.muCuteRelaySession == null) {
                currentSession.displayClientMessage("§c[SoundModule] MuCuteRelaySession недоступна для воспроизведения событийного звука.")
                println("ERROR: currentSession.muCuteRelaySession равна null в playLevelSound(). Невозможно воспроизвести событие: $soundEvent")
                return@runOnSession
            }

            val localPlayer = currentSession.localPlayer
            val playerPos: Vector3f? = localPlayer?.vec3Position

            if (playerPos == null) {
                currentSession.displayClientMessage("§c[SoundModule] Позиция игрока недоступна. Невозможно воспроизвести событийный звук.")
                println("ERROR: Позиция игрока равна null в playLevelSound(). Невозможно воспроизвести событие: $soundEvent")
                return@runOnSession
            }

            val packet = LevelSoundEventPacket().apply {
                sound = soundEvent
                position = playerPos
                this.extraData = extraData
                this.identifier = identifier
                this.babySound = babySound
                this.relativeVolumeDisabled = relativeVolumeDisabled
                this.entityUniqueId = entityUniqueId
            }

            currentSession.clientBound(packet)
            currentSession.displayClientMessage("§a[SoundModule] Попытка воспроизвести событийный звук: §b$soundEvent")
            println("DEBUG: LevelSoundEventPacket отправлен для события: $soundEvent на позиции $playerPos.")
        }
    }

    /**
     * Заглушка для остановки всех звуков (реализация зависит от логики клиента/прокси).
     */
    fun stopAllSounds() {
        runOnSession { currentSession ->
            if (currentSession.muCuteRelaySession == null) {
                currentSession.displayClientMessage("§c[SoundModule] Сессия или прокси не активны для остановки звуков.")
                println("DEBUG: Невозможно остановить все звуки, currentSession.muCuteRelaySession не инициализирована.")
                return@runOnSession
            }

            currentSession.displayClientMessage("§e[SoundModule] Заглушка: функция 'stopAllSounds' не имеет прямой реализации в MCBE. Нужна кастомная логика.")
            println("DEBUG: stopAllSounds() вызвана. Нет прямого пакета MCBE. Требуется кастомная реализация.")
        }
    }
}
