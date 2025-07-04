package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.data.skin.SkinCache
import com.retrivedmods.wclient.game.command.Command // Предполагаем, что у тебя есть этот импорт
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import org.cloudburstmc.protocol.bedrock.data.PlayerListEntry
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin
import org.cloudburstmc.protocol.bedrock.data.PlayerListEntry.skin // Это может понадобиться для доступа к скину напрямую

class SkinStealerModule : Module("skinstealer", ModuleCategory.Misc) {

    // Включаем модуль по умолчанию, если это необходимо
    init {
        // Убедись, что модуль зарегистрирован как CommandExecutor
        // Это может быть сделано через какой-то CommandManager или в главном классе WClient
        // Например: WClient.commandManager.register(this)
    }

    override fun onEnable() {
        sendClientMessage("§aSkinStealer включен. Используйте .skin <ник> для смены скина.")
        // Регистрируем команду при включении модуля, если это не делается автоматически
        // WClient.commandManager.registerCommand(SkinCommand()) // Предполагаем, что SkinCommand это отдельный класс
    }

    override fun onDisable() {
        sendClientMessage("§cSkinStealer выключен.")
        // Отменяем регистрацию команды при выключении модуля, если это необходимо
        // WClient.commandManager.unregisterCommand(SkinCommand())
    }

    // Логика применения скина
    fun applySkin(targetNick: String) {
        if (!isEnabled) {
            sendClientMessage("§cМодуль SkinStealer выключен!")
            return
        }

        sendClientMessage("§eПытаюсь получить скин для игрока: §b$targetNick...")
        val skin: SerializedSkin? = SkinCache.getSkin(targetNick)

        if (skin == null) {
            sendClientMessage("§cСкин игрока '$targetNick' не найден в кэше! Игрок должен быть на сервере и его данные должны быть загружены.")
            return
        }

        try {
            // Отправляем PlayerSkinPacket серверу
            val packet = PlayerSkinPacket().apply {
                uuid = session.localPlayer.uuid // Используем UUID текущего игрока
                this.skin = skin
            }
            session.serverBound(packet) // Отправляем пакет на сервер

            sendClientMessage("§aСкин успешно изменён на скин игрока §b$targetNick!§a")

            // Важно: чтобы скин был виден другим, сервер должен разослать PlayerListPacket с обновленным скином.
            // На стороне клиента мы можем попытаться форсировать это, но это зависит от логики сервера.
            // Возможно, потребуется обновить свой собственный PlayerListEntry в кэше клиента.
            // Если сервер не обновляет, то скин будет виден только после переподключения или обновления PlayerListPacket от сервера.

            // Опционально: можно попытаться обновить свой собственный PlayerListEntry в локальном кэше,
            // чтобы клиент сразу видел изменение. Однако это не гарантирует, что другие игроки увидят.
            // session.localPlayer.updateSkin(skin) // Если у тебя есть такой метод
        } catch (e: Exception) {
            sendClientMessage("§cОшибка смены скина: ${e.message}")
            e.printStackTrace() // Для более подробного дебага
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        // Заполнение SkinCache из PlayerListPacket
        if (packet is PlayerListPacket) {
            packet.entries.forEach { entry ->
                // Важно: PlayerListEntry содержит SerializedSkin.
                // Убедись, что ты правильно извлекаешь имя/ник и скин.
                // В зависимости от версии CloudburstMC.Protocol Bedrock, доступ к скину может быть через .skin или .getSkin()
                val entrySkin: SerializedSkin? = entry.skin // Предполагаем, что доступ через .skin
                val entryName: String = entry.displayName ?: entry.username ?: entry.xuid // Используем displayName, затем username, затем xuid

                if (entrySkin != null && entryName.isNotBlank()) {
                    SkinCache.putSkin(entryName, entrySkin)
                    // sendClientMessage("Debug: Added skin for ${entryName} to SkinCache") // Закомментируй для продакшена
                }
            }
        }
    }

    // Метод для отправки сообщений в чат клиента
    private fun sendClientMessage(msg: String) {
        session.displayClientMessage(msg)
    }

    // --- Интеграция с системой команд ---
    // Это пример того, как ты можешь создать команду, если WClient имеет свою систему команд.
    // Тебе нужно будет зарегистрировать этот класс команды в главном классе WClient или CommandManager.
    class SkinCommand(private val module: SkinStealerModule) : Command("skin", "Изменить свой скин на скин другого игрока.", listOf("<ник>")) {
        override fun execute(args: List<String>) {
            if (args.size != 1) {
                module.sendClientMessage("§cИспользование: .skin <ник>")
                return
            }
            module.applySkin(args[0])
        }
    }
}
