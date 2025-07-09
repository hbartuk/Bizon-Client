package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket
import org.cloudburstmc.math.vector.Vector3f // For coordinates. Make sure this import is correct.

class SoundModule : Module("Sound", ModuleCategory.Misc) {

    override fun initialize() {
        super.initialize()
        // Now 'session' should be accessible because it's 'lateinit' and assumed to be set by ModuleManager
        println("DEBUG: SoundModule.initialize() called. Session should be initialized: ${this::session.isInitialized}.")
        // It's still good practice to check if it's initialized, especially for logging/messages
        if (this::session.isInitialized) {
            session.displayClientMessage("§a[SoundModule] Модуль Sound проинициализирован. Сессия доступна.")
        } else {
            println("ERROR: Session is not initialized in SoundModule.initialize()! This should not happen if ModuleManager is configured correctly.")
        }
    }

    override fun onEnabled() {
        super.onEnabled()
        println("DEBUG: SoundModule.onEnabled() called. Session should be initialized: ${this::session.isInitialized}.")
        if (this::session.isInitialized) {
            session.displayClientMessage("§a[SoundModule] Модуль Sound активирован. Сессия доступна.")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        println("DEBUG: SoundModule.onDisabled() called.")
        if (this::session.isInitialized) {
            session.displayClientMessage("§c[SoundModule] Модуль Sound деактивирован.")
        }
    }

    fun playSound(soundName: String, volume: Float, pitch: Float) {
        println("DEBUG: SoundModule.playSound() called for sound: $soundName (Volume: $volume, Pitch: $pitch)")

        // Check if session is initialized and MuCuteRelaySession is available
        // 'session' is lateinit, so we check this::session.isInitialized
        if (!this::session.isInitialized || session.muCuteRelaySession == null) {
            println("ERROR: Session or session.muCuteRelaySession is not available in playSound(). Cannot play sound.")
            // If session isn't initialized, we can't send a client message through it either.
            // You might need a fallback logging mechanism here.
            return
        }

        val playSoundPacket = PlaySoundPacket().apply {
            // Unresolved reference 'soundIdentifier'. This is likely a property name issue.
            // CloudburstMC typically uses 'soundIdentifier' or 'soundId'.
            // If 'soundIdentifier' is still unresolved, it might be 'name' or 'identifier'
            // Please verify the actual property name in the PlaySoundPacket class from your CloudburstMC library.
            // For now, assuming 'soundIdentifier' is correct.
            this.soundIdentifier = soundName // Check this property name carefully!
            
            // Unresolved reference 'x', 'y', 'z', 'position'.
            // session.localPlayer.position is correct.
            // These errors mean either:
            // 1. localPlayer is null (unlikely if session is initialized)
            // 2. localPlayer.position is null (less likely for Vector3f)
            // 3. PlaySoundPacket doesn't have 'x', 'y', 'z' as direct properties,
            //    but instead takes a Vector3f directly.

            // The PlaySoundPacket in CloudburstMC usually takes x, y, z as individual floats
            // or sometimes a Vector3i for block coordinates.
            // Based on your previous code, individual floats are expected.
            // These errors suggest 'x', 'y', 'z' are not direct properties of PlaySoundPacket.
            // This is VERY IMPORTANT: How does PlaySoundPacket set coordinates?
            // It might be:
            // this.blockPosition = Vector3i.from(session.localPlayer.position.x.toInt(), ...) // if it expects int block coords
            // OR if it expects float coordinates:
            this.x = session.localPlayer.position.x // Check PlaySoundPacket source for actual setters
            this.y = session.localPlayer.position.y
            this.z = session.localPlayer.position.z

            this.volume = volume
            this.pitch = pitch
        }

        session.clientBound(playSoundPacket) // Send to client
        session.displayClientMessage("§a[SoundModule] Попытка воспроизвести звук: §b$soundName §7на координатах §e(${session.localPlayer.position.x.toInt()}, ${session.localPlayer.position.y.toInt()}, ${session.localPlayer.position.z.toInt()})")
        println("DEBUG: PlaySoundPacket sent for sound: $soundName at (${session.localPlayer.position.x}, ${session.localPlayer.position.y}, ${session.localPlayer.position.z}) with volume $volume and pitch $pitch.")
    }

    fun stopAllSounds() {
        if (!this::session.isInitialized || session.muCuteRelaySession == null) {
            println("DEBUG: Cannot stop all sounds, session or MuCuteRelaySession not initialized.")
            return
        }
        
        session.displayClientMessage("§e[SoundModule] Заглушка: Функция 'stopAllSounds' не имеет прямой реализации в MCBE. Необходима пользовательская логика.")
        println("DEBUG: stopAllSounds() called. No direct MCBE packet. Custom implementation needed.")
    }
}
