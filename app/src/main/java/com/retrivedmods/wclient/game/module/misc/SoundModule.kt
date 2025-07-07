// File: app/src/main/java/com/retrivedmods/wclient/game/module/misc/SoundModule.kt
package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory // <-- Ensure this import is correct
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.SoundEvent // <-- CRITICAL: Ensure this import is correct and Bedrock Protocol is in your dependencies
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class SoundModule() : Module("Sound", ModuleCategory.MISC) { // <-- ModuleCategory.MISC used here

    override lateinit var session: GameSession

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val activeSounds = mutableMapOf<String, ScheduledFuture<*>>()

    private val soundEventMap = mapOf(
        "step" to SoundEvent.STEP,
        "explode" to SoundEvent.EXPLODE,
        "click" to SoundEvent.CLICK, // <-- SoundEvent.CLICK must exist here
        "place" to SoundEvent.PLACE,
        "break" to SoundEvent.BREAK,
        "levelup" to SoundEvent.LEVELUP,
        "attack" to SoundEvent.ATTACK_STRONG,
        "drink" to SoundEvent.DRINK
    )

    // 'val' cannot be reassigned: This error in previous logs was likely a typo or an attempt
    // to assign something to `soundEventMap` again outside its initialization.
    // The current code for `soundEventMap` should not cause this.
    // If it *still* causes it, you must have another line of code trying to re-assign it.

    override fun onEnabled() {
        super.onEnabled()
        if (isSessionCreated) {
            session.displayClientMessage("§a[SoundModule] Module activated.")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        stopAllSounds()
        if (isSessionCreated) {
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
            return
        }

        val soundEvent = soundEventMap[soundName.lowercase()] // lowercase() should be fine here
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
    }

    fun stopAllSounds() {
        activeSounds.values.forEach { it.cancel(false) }
        activeSounds.clear()
        if (isSessionCreated) {
            session.displayClientMessage("§a[SoundModule] All sounds stopped.")
        }
    }
}
