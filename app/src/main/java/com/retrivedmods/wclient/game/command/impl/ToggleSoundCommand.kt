package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.ModuleManager
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.module.misc.ToggleSoundModule

class ToggleSoundCommand : Command("togglesound", "ts") {

    override fun exec(args: Array<String>, session: GameSession) {
        val module = ModuleManager.getModule<ToggleSoundModule>() ?: run {
            session.displayClientMessage("§cМодуль ToggleSound не найден.")
            return
        }

        when (args.getOrNull(0)?.lowercase()) {
            "celestial" -> {
                module.toggleCelestial()
            }
            "nursultan" -> {
                module.toggleNursultan()
            }
            "smooth" -> {
                module.toggleSmooth()
            }
            "test" -> {
                module.testAllModes()
            }
            else -> {
                session.displayClientMessage("§e=== ToggleSound Command ===")
                session.displayClientMessage("§7Использование: .togglesound <режим>")
                session.displayClientMessage("§7Доступные режимы: §ecelestial§7, §enursultan§7, §esmooth")
                session.displayClientMessage("§7Текущий режим: §e${module.getCurrentMode()}")
            }
        }
    }
}
