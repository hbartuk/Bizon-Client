// File: app/src/main/java/com/retrivedmods/wclient/game/command/impl/SoundCommand.kt
package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.module.misc.SoundModule // <-- Correct import for SoundModule (from misc package)

class SoundCommand : Command("sound") {

    override fun exec(args: Array<String>, session: GameSession) {
        if (args.isEmpty()) {
            session.displayClientMessage("§cUsage: §7.sound <name> [volume] [distance] [sounds/sec] [duration(sec)]")
            session.displayClientMessage("§eAvailable sounds (example): §bstep, explode, click, place, break, levelup, attack, drink")
            return
        }

        val soundName = args[0]
        val volume = args.getOrNull(1)?.toFloatOrNull() ?: 1.0f
        val distance = args.getOrNull(2)?.toFloatOrNull() ?: 16.0f
        val soundsPerSecond = args.getOrNull(3)?.toIntOrNull() ?: 1
        val durationSeconds = args.getOrNull(4)?.toIntOrNull() ?: 1

        // The getModule method expects a Class<T>, SoundModule::class.java is correct
        val soundModule = session.getModule(SoundModule::class.java)

        if (soundModule == null) {
            session.displayClientMessage("§c[SoundCommand] SoundModule not found or inactive.")
            return
        }

        if (soundName.lowercase() == "stopall") { // `lowercase()` should be available here
            soundModule.stopAllSounds() // This method should now be correctly recognized
            session.displayClientMessage("§a[SoundCommand] Command sent to stop all sounds.")
            return
        }

        soundModule.playSound(soundName, volume, distance, soundsPerSecond, durationSeconds) // This method should now be correctly recognized
    }
}
