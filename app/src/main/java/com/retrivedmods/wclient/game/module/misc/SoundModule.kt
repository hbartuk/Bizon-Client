// File: com.retrivedmods.wclient.game.module.misc.SoundModule.kt
package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.GameSession 

import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket
import org.cloudburstmc.math.vector.Vector3f // Для координат игрока

class SoundModule : Module("Sound", ModuleCategory.Misc) {

    // Вспомогательный метод для безопасного доступа к сессии
    private fun runOnSession(action: (GameSession) -> Unit) {
        if (this::session.isInitialized) {
            action(session)
        } else {
            // Только для отладки, не спамить в чат клиента, если сессия недоступна.
            println("DEBUG: Session not initialized for SoundModule yet.")
        }
    }

    override fun initialize() {
        super.initialize()
        runOnSession {
            it.displayClientMessage("§a[SoundModule] Модуль Sound проинициализирован. Сессия доступна.")
        }
    }

    override fun onEnabled() {
        super.onEnabled()
        runOnSession {
            it.displayClientMessage("§a[SoundModule] Модуль Sound активирован. Сессия доступна.")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        runOnSession {
            it.displayClientMessage("§c[SoundModule] Модуль Sound деактивирован.")
        }
    }

    fun playSound(soundName: String, volume: Float, pitch: Float) {
        println("DEBUG: SoundModule.playSound() called for sound: $soundName (Volume: $volume, Pitch: $pitch)")

        runOnSession { currentSession ->
            if (currentSession.muCuteRelaySession == null) {
                currentSession.displayClientMessage("§c[SoundModule] MuCuteRelaySession недоступна для воспроизведения звука.")
                println("ERROR: currentSession.muCuteRelaySession is null in playSound(). Cannot play sound: $soundName")
                return@runOnSession // Выходим из лямбды, а не из функции
            }

            // Пытаемся получить позицию игрока безопасно
            val playerPos: Vector3f? = currentSession.localPlayer?.position
            if (playerPos == null) {
                currentSession.displayClientMessage("§c[SoundModule] Позиция игрока недоступна. Невозможно воспроизвести звук.")
                println("ERROR: Player position is null in playSound(). Cannot play sound: $soundName")
                return@runOnSession
            }

            val playSoundPacket = PlaySoundPacket().apply {
                // Использование сеттеров вместо прямых свойств
                // Если 'setSoundIdentifier' не существует, попробуйте 'setSoundId'
                setSoundIdentifier(soundName) 
                
                // Использование сеттеров для координат.
                // Если это не работает, PlaySoundPacket может иметь метод setPosition(Vector3f)
                setX(playerPos.getX()) // Используем getX(), getY(), getZ()
                setY(playerPos.getY())
                setZ(playerPos.getZ())

                setVolume(volume)
                setPitch(pitch)
            }

            currentSession.clientBound(playSoundPacket)

            currentSession.displayClientMessage("§a[SoundModule] Попытка воспроизвести звук: §b$soundName")
            println("DEBUG: PlaySoundPacket sent for sound: $soundName with volume $volume and pitch $pitch.")
        }
    }

    fun stopAllSounds() {
        runOnSession { currentSession ->
            if (currentSession.muCuteRelaySession == null) {
                currentSession.displayClientMessage("§c[SoundModule] Сессия или релей-сессия не активны для остановки звуков.")
                println("DEBUG: Cannot stop all sounds, currentSession.muCuteRelaySession not initialized.")
                return@runOnSession
            }
            
            currentSession.displayClientMessage("§e[SoundModule] Заглушка: Функция 'stopAllSounds' не имеет прямой реализации в MCBE. Необходима пользовательская логика.")
            println("DEBUG: stopAllSounds() called. No direct MCBE packet. Custom implementation needed.")
        }
    }
}
