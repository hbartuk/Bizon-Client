// File: app/src/main/java/com/retrivedmods/wclient/game/module/misc/SoundModule.kt
package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory // Correct path, ensure MISC exists

import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.SoundEvent // This import is still needed for other SoundEvent values if you use them.
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

// Line 15: Fix for "Unresolved reference 'MISC'."
class SoundModule() : Module("Sound", ModuleCategory.MISC) {

    override lateinit var session: GameSession

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val activeSounds = mutableMapOf<String, ScheduledFuture<*>>()

    // soundEventMap is intentionally empty.
    // The 'val' cannot be reassigned error (Line 72) likely came from external code trying to re-assign this,
    // or from an older Kotlin where map initialization was different. This current syntax is standard.
    private val soundEventMap: Map<String, SoundEvent> = emptyMap() // Keep this as val and initialized once.

    override fun onEnabled() {
        super.onEnabled()
        if (isSessionCreated) {
            session.displayClientMessage("§a[SoundModule] Модуль активирован.")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        stopAllSounds()
        if (isSessionCreated) {
            session.displayClientMessage("§c[SoundModule] Модуль деактивирован. Все активные звуки остановлены.")
        }
    }

    // Now accepts an integer soundId directly for compatibility with older bedrock-codec
    fun playSound(
        soundId: Int, // <-- IMPORTANT: This is now an INTEGER Sound ID!
        volume: Float,
        distance: Float,
        soundsPerSecond: Int,
        durationSeconds: Int,
        soundNameForDisplay: String = soundId.toString() // Optional: provide a name for display messages
    ) {
        if (!isSessionCreated) {
            return
        }

        // We use soundNameForDisplay for stopping, to avoid issues if soundId is not unique in activeSounds map
        val stopKey = soundNameForDisplay.lowercase() // Use lowercase for consistency

        stopSound(stopKey)

        val initialPosition = session.localPlayer.vec3Position

        session.displayClientMessage("§a[SoundModule] Воспроизведение звука: §b$soundNameForDisplay§a (громкость: §b$volume§a, дистанция: §b$distance§a, частота: §b$soundsPerSecond§a/с, длительность: §b$durationSeconds§a сек.)")

        val periodMillis = if (soundsPerSecond > 0) (1000L / soundsPerSecond) else 0L
        val extraDataValue = (distance * 1000).toInt() // This might need adjustment per SoundEvent in older versions

        val task = scheduler.scheduleAtFixedRate({
            if (isSessionCreated) {
                val packet = LevelSoundEventPacket().apply {
                    // Line 70: Fixed by using SoundEvent.from(int id)
                    // This is the fallback for when SOUND_DEFINITION_EVENT is not available.
                    // You MUST provide a valid integer ID.
                    sound = SoundEvent.from(soundId)
                    position = initialPosition
                    volume = volume
                    isBabySound = false
                    isRelativeVolumeDisabled = false
                    // identifier is not typically used for simple SoundEvent IDs
                    // unless it's a specific older version quirk. Removing for now.
                    // identifier = ""
                    extraData = extraDataValue
                }
                session.serverBound(packet)
                session.clientBound(packet)
            }
        }, 0, periodMillis, TimeUnit.MILLISECONDS)

        // Line 83: Fix for "'fun String.toLowerCase(): String' is deprecated. Use lowercase() instead."
        activeSounds[stopKey] = task

        scheduler.schedule({
            stopSound(stopKey)
            if (isSessionCreated) {
                session.displayClientMessage("§a[SoundModule] Воспроизведение звука '$soundNameForDisplay' завершено.")
            }
        }, durationSeconds.toLong(), TimeUnit.SECONDS)
    }

    // Line 94: Fix for "'fun String.toLowerCase(): String' is deprecated. Use lowercase() instead."
    fun stopSound(soundIdentifier: String) { // Now accepts string key, could be sound ID or custom name
        activeSounds.remove(soundIdentifier.lowercase())?.cancel(false)
    }

    fun stopAllSounds() {
        activeSounds.values.forEach { it.cancel(false) }
        activeSounds.clear()
        if (isSessionCreated) {
            session.displayClientMessage("§a[SoundModule] Все звуки остановлены.")
        }
    }
}
