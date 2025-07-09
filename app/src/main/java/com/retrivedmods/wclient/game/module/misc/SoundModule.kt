// File: com.retrivedmods.wclient.game.module.misc.SoundModule.kt
package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.GameSession 

import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket
import org.cloudburstmc.math.vector.Vector3f 

class SoundModule : Module("Sound", ModuleCategory.Misc) {

    // runOnSession теперь определён в базовом классе Module

    override fun initialize() {
        super.initialize() // Это вызовет initialize() из Module, которая уже отправляет сообщение
        // Дополнительная логика инициализации для SoundModule, если требуется
        runOnSession { 
            // Это сообщение будет показано только после инициализации сессии
            it.displayClientMessage("§a[SoundModule] Модуль Sound специфически проинициализирован.")
        }
    }

    override fun onEnabled() {
        super.onEnabled() // Это вызовет sendToggleMessage из Module
        // Дополнительная логика или сообщения при включении SoundModule
        runOnSession { 
            it.displayClientMessage("§a[SoundModule] Дополнительная логика при активации.") 
        }
    }

    override fun onDisabled() {
        super.onDisabled() // Это вызовет sendToggleMessage из Module
        // Дополнительная логика или сообщения при выключении SoundModule
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

            // Проверяем localPlayer на null
            val localPlayer = currentSession.localPlayer 
            // Убедитесь, что у localPlayer есть свойство 'position' типа Vector3f.
            // Если нет, проверьте исходники LocalPlayer для правильного имени.
            val playerPos: Vector3f? = localPlayer?.position 

            if (playerPos == null) {
                currentSession.displayClientMessage("§c[SoundModule] Позиция игрока недоступна. Невозможно воспроизвести звук.")
                println("ERROR: Player position is null in playSound(). Cannot play sound: $soundName")
                return@runOnSession
            }

            val playSoundPacket = PlaySoundPacket().apply {
                // Если компилятор по-прежнему ругается на soundIdentifier, x, y, z,
                // это означает, что в вашей версии CloudburstMC у PlaySoundPacket
                // другие названия публичных полей.
                // В этом случае вам нужно будет открыть определение PlaySoundPacket (Ctrl+Click на PlaySoundPacket)
                // и использовать ТОЧНЫЕ имена полей, которые там указаны.
                this.soundIdentifier = soundName 
                this.x = playerPos.x 
                this.y = playerPos.y
                this.z = playerPos.z
                this.volume = volume
                this.pitch = pitch
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
