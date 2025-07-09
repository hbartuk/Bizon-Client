// File: /home/runner/work/WClient-/WClient-/app/src/main/java/com/retrivedmods/wclient/game/module/misc/SoundModule.kt
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
            it.displayClientMessage("§a[SoundModule] Sound module specifically initialized.")
        }
    }

    override fun onEnabled() {
        super.onEnabled()
        runOnSession {
            it.displayClientMessage("§a[SoundModule] Additional logic on activation.")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        runOnSession {
            it.displayClientMessage("§c[SoundModule] Additional logic on deactivation.")
        }
    }

    fun playSound(soundName: String, volume: Float, pitch: Float) {
        println("DEBUG: SoundModule.playSound() called for sound: $soundName (Volume: $volume, Pitch: $pitch)")

        runOnSession { currentSession ->
            if (currentSession.muCuteRelaySession == null) {
                currentSession.displayClientMessage("§c[SoundModule] MuCuteRelaySession is unavailable for sound playback.")
                println("ERROR: currentSession.muCuteRelaySession is null in playSound(). Cannot play sound: $soundName")
                return@runOnSession
            }

            val localPlayer = currentSession.localPlayer
            // *** ВОТ ОНО! Используем ПРАВИЛЬНОЕ свойство 'vec3Position' ***
            val playerPos: Vector3f? = localPlayer?.vec3Position

            if (playerPos == null) {
                currentSession.displayClientMessage("§c[SoundModule] Player position is unavailable. Cannot play sound.")
                println("ERROR: Player position is null in playSound(). Cannot play sound: $soundName")
                return@runOnSession
            }

            val playSoundPacket = PlaySoundPacket().apply {
                // Используем ТОЧНЫЕ сеттеры, подтвержденные из PlaySoundPacket.java
                this.setSound(soundName)
                this.setPosition(playerPos) // Vector3f устанавливается целиком
                this.setVolume(volume)
                this.setPitch(pitch)
            }

            currentSession.clientBound(playSoundPacket)

            currentSession.displayClientMessage("§a[SoundModule] Attempting to play sound: §b$soundName")
            println("DEBUG: PlaySoundPacket sent for sound: $soundName with volume $volume and pitch $pitch.")
        }
    }

    fun stopAllSounds() {
        runOnSession { currentSession ->
            if (currentSession.muCuteRelaySession == null) {
                currentSession.displayClientMessage("§c[SoundModule] Session or relay session is not active to stop sounds.")
                println("DEBUG: Cannot stop all sounds, currentSession.muCuteRelaySession not initialized.")
                return@runOnSession
            }

            currentSession.displayClientMessage("§e[SoundModule] Placeholder: 'stopAllSounds' function has no direct MCBE implementation. Custom logic is needed.")
            println("DEBUG: stopAllSounds() called. No direct MCBE packet. Custom implementation needed.")
        }
    }
}
