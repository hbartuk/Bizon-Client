// File: app/src/main/java/com/retrivedmods/wclient/game/module/misc/SoundModule.kt
package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.GameSession 

import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket
import org.cloudburstmc.math.vector.Vector3f 

class SoundModule : Module("Sound", ModuleCategory.Misc) {

    override fun initialize() {
        super.initialize() 
        runOnSession { 
            it.displayClientMessage("§a[SoundModule] Модуль Sound специфически проинициализирован.")
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

    fun playSound(soundName: String, volume: Float, pitch: Float) {
        println("DEBUG: SoundModule.playSound() called for sound: $soundName (Volume: $volume, Pitch: $pitch)")

        runOnSession { currentSession ->
            if (currentSession.muCuteRelaySession == null) {
                currentSession.displayClientMessage("§c[SoundModule] MuCuteRelaySession недоступна для воспроизведения звука.")
                println("ERROR: currentSession.muCuteRelaySession is null in playSound(). Cannot play sound: $soundName")
                return@runOnSession 
            }

            val localPlayer = currentSession.localPlayer 
            // *** ИСПРАВЛЕНИЕ #1: Теперь мы знаем, что у LocalPlayer, вероятно, есть метод getPosition(),
            // который возвращает Vector3f. Если нет, вам нужно будет это проверить.
            val playerPos: Vector3f? = localPlayer?.getPosition() 

            if (playerPos == null) {
                currentSession.displayClientMessage("§c[SoundModule] Позиция игрока недоступна. Невозможно воспроизвести звук.")
                println("ERROR: Player position is null in playSound(). Cannot play sound: $soundName")
                return@runOnSession
            }

            val playSoundPacket = PlaySoundPacket().apply {
                // *** ИСПРАВЛЕНИЕ #2: Используем ТОЧНЫЕ сеттеры, сгенерированные Lombok.
                this.setSound(soundName)
                this.setPosition(playerPos) // Vector3f устанавливается целиком
                this.setVolume(volume)
                this.setPitch(pitch)
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
