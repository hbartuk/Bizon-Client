package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.ModuleManager
import com.retrivedmods.wclient.game.module.misc.SoundModule

class SoundCommand : Command("sound", "Play a client-side sound.", "s") {

    override fun execute(session: GameSession, args: List<String>) {
        println("DEBUG: SoundCommand.execute() called.")
        if (args.size < 3) {
            session.displayClientMessage("Использование: .sound <название_звука> <громкость> <высота_тона>")
            return
        }

        val soundName = args[0]
        val volume = args[1].toFloatOrNull()
        val pitch = args[2].toFloatOrNull()

        if (volume == null || pitch == null) {
            session.displayClientMessage("Неверный формат громкости или высоты тона. Используйте числа.")
            return
        }

        // Получаем SoundModule из ModuleManager
        val soundModule = ModuleManager.modules.firstOrNull { it is SoundModule } as? SoundModule

        if (soundModule == null) {
            session.displayClientMessage("Модуль SoundModule не найден или неактивен.")
            println("DEBUG: SoundModule is null in SoundCommand.")
            return
        }

        // Убедимся, что модуль включен
        if (!soundModule.isEnabled) {
            soundModule.isEnabled = true // Включаем модуль, если он выключен
            session.displayClientMessage("Модуль SoundModule был автоматически включен.")
            println("DEBUG: SoundModule was not enabled, enabling it now.")
        }

        println("DEBUG: Calling playSound on SoundModule.")
        soundModule.playSound(soundName, volume, pitch)
    }
}
