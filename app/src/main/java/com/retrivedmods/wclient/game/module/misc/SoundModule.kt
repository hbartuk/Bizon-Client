// File: app/src/main/java/com/retrivedmods/wclient/game/module/misc/SoundModule.kt (path should be ...misc/SoundModule.kt)
package com.retrivedmods.wclient.game.module.misc // Correct package for misc module

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.Module // <-- Correct import for your base Module class
import com.retrivedmods.wclient.game.ModuleCategory // Assuming this import exists and is correct
import org.cloudburstmc.math.vector.Vector3f // Should be correct
import org.cloudburstmc.protocol.bedrock.data.SoundEvent // This import is crucial for SoundEvent
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

// SoundModule now correctly inherits from Module, no session in its constructor
class SoundModule() : Module("Sound", ModuleCategory.MISC) { // Use ModuleCategory.MISC, not RENDER for SoundModule

    // Override lateinit var session to make it explicit, though not strictly required if already in Module
    override lateinit var session: GameSession

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val activeSounds = mutableMapOf<String, ScheduledFuture<*>>()

    private val soundEventMap = mapOf(
        "step" to SoundEvent.STEP,
        "explode" to SoundEvent.EXPLODE,
        "click" to SoundEvent.CLICK, // Keeping CLICK as an example Bedrock SoundEvent
        "place" to SoundEvent.PLACE,
        "break" to SoundEvent.BREAK,
        "levelup" to SoundEvent.LEVELUP,
        "attack" to SoundEvent.ATTACK_STRONG,
        "drink" to SoundEvent.DRINK
        // "ui_button_click" to SoundEvent.UI_BUTTON_CLICK // REMOVED: UI_BUTTON_CLICK is not a standard SoundEvent
        // If you need custom sounds, you'll need a different mechanism or to ensure UI_BUTTON_CLICK is a valid enum value.
    )

    // IMPORTANT: Changed to onEnabled() and onDisabled() to match your Module.kt
    override fun onEnabled() {
        super.onEnabled() // Call base implementation for toggle message
        if (isSessionCreated) { // Ensure session is ready before using
            session.displayClientMessage("§a[SoundModule] Module activated.")
        }
    }

    // IMPORTANT: Changed to onEnabled() and onDisabled() to match your Module.kt
    override fun onDisabled() {
        super.onDisabled() // Call base implementation for toggle message
        stopAllSounds()
        if (isSessionCreated) { // Ensure session is ready before using
            session.displayClientMessage("§c[SoundModule] Module deactivated. All active sounds stopped.")
        }
    }

    fun playSound(
        soundName: String,
        volume: Float,
        distance: Float,
        soundsPerSecond: Int,
        durationSeconds: Int
    ) {
        if (!isSessionCreated) {
            // No session, cannot play sounds or display messages
            return
        }

        val soundEvent = soundEventMap[soundName.lowercase()]
        if (soundEvent == null) {
            session.displayClientMessage("§c[SoundModule] Sound '$soundName' not found. Check available sounds.")
            return
        }

        stopSound(soundName)

        val initialPosition = session.localPlayer.vec3Position

        session.displayClientMessage("§a[SoundModule] Playing sound: §b$soundName§a (vol: §b$volume§a, dist: §b$distance§a, rate: §b$soundsPerSecond§a/s, duration: §b$durationSeconds§a sec.)")

        val periodMillis = if (soundsPerSecond > 0) (1000L / soundsPerSecond) else 0L

        val extraDataValue = (distance * 1000).toInt()

        val task = scheduler.scheduleAtFixedRate({
            // Ensure session is still valid inside the lambda
            if (isSessionCreated) {
                val packet = LevelSoundEventPacket().apply {
                    sound = soundEvent
                    position = initialPosition
                    volume = volume
                    isBabySound = false
                    isRelativeVolumeDisabled = false
                    identifier = "minecraft:player"
                    extraData = extraDataValue
                }
                session.serverBound(packet)
                session.clientBound(packet)
            }
        }, 0, periodMillis, TimeUnit.MILLISECONDS)

        activeSounds[soundName.lowercase()] = task

        scheduler.schedule({
            stopSound(soundName)
            if (isSessionCreated) {
                session.displayClientMessage("§a[SoundModule] Sound '$soundName' finished playing.")
            }
        }, durationSeconds.toLong(), TimeUnit.SECONDS)
    }

    fun stopSound(soundName: String) {
        activeSounds.remove(soundName.lowercase())?.cancel(false)
        // No session message here as it might be called on module disable
    }

    fun stopAllSounds() {
        activeSounds.values.forEach { it.cancel(false) }
        activeSounds.clear()
        if (isSessionCreated) {
            session.displayClientMessage("§a[SoundModule] All sounds stopped.")
        }
    }
}
