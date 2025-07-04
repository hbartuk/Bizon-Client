package com.retrivedmods.wclient.game

import com.retrivedmods.wclient.application.AppContext
import com.retrivedmods.wclient.game.entity.LocalPlayer
import com.retrivedmods.wclient.game.world.Level
import com.mucheng.mucute.relay.MuCuteRelaySession
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket

@Suppress("MemberVisibilityCanBePrivate")
class GameSession(val muCuteRelaySession: MuCuteRelaySession) : ComposedPacketHandler {

    val localPlayer = LocalPlayer(this)

    val level = Level(this)

    private val versionName by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppContext.instance.packageManager.getPackageInfo(
            AppContext.instance.packageName, 0
        ).versionName
    }

    fun clientBound(packet: BedrockPacket) {
        muCuteRelaySession.clientBound(packet)
    }

    fun serverBound(packet: BedrockPacket) {
        muCuteRelaySession.serverBound(packet)
    }

    override fun beforePacketBound(packet: BedrockPacket): Boolean {
        localPlayer.onPacketBound(packet)
        level.onPacketBound(packet)

        val interceptablePacket = InterceptablePacket(packet)

        // --- Обработка пакетов модулями ---
        for (module in ModuleManager.modules) {
            // Установим сессию для модуля перед его использованием.
            // Это решит проблему с lateinit property session has not been initialized
            module.session = this 
            
            module.beforePacketBound(interceptablePacket)
            if (interceptablePacket.isIntercepted) {
                return true
            }
        }

        // --- Добавляем логику обработки команд здесь ---
        if (packet is TextPacket && packet.type == TextPacket.Type.CHAT) {
            val message = packet.message.trim()
            if (message.startsWith(".")) { // Проверяем, начинается ли с префикса команды
                // Разделяем сообщение на название команды и аргументы
                val parts = message.substring(1).split(" ", limit = 2) 
                val commandName = parts[0].lowercase() // Название команды в нижнем регистре
                // Аргументы: если есть только название команды, аргументов нет
                val args = if (parts.size > 1) parts[1].split(" ").toTypedArray() else emptyArray()

                val command = ModuleManager.getCommand(commandName) // Ищем команду в ModuleManager
                if (command != null) {
                    // Команда найдена, выполняем её
                    command.exec(args, this) // 'this' здесь - это текущий GameSession
                    // После выполнения команды, отменяем пакет, чтобы он не отображался в чате
                    interceptablePacket.isIntercepted = true // <-- ИСПРАВЛЕНИЕ ЗДЕСЬ!
                    return true // Прерываем дальнейшую обработку пакета, так как команда обработана
                } else {
                    // Команда не найдена - отправляем сообщение об ошибке
                    displayClientMessage("§cНеизвестная команда: §f.$commandName")
                    // Также отменяем пакет, чтобы неизвестная команда не отображалась в чате
                    interceptablePacket.isIntercepted = true // <-- ИСПРАВЛЕНИЕ ЗДЕСЬ!
                    return true
                }
            }
        }
        // --- Конец логики обработки команд ---

        return false
    }

    override fun afterPacketBound(packet: BedrockPacket) {
        for (module in ModuleManager.modules) {
            module.afterPacketBound(packet)
        }
    }

    override fun onDisconnect(reason: String) {
        localPlayer.onDisconnect()
        level.onDisconnect()

        for (module in ModuleManager.modules) {
            module.onDisconnect(reason)
        }
    }

    // Твой существующий метод displayClientMessage
    fun displayClientMessage(message: String, type: TextPacket.Type = TextPacket.Type.RAW) {
        val textPacket = TextPacket()
        textPacket.type = type
        textPacket.isNeedsTranslation = false
        textPacket.sourceName = ""
        textPacket.message = message
        textPacket.xuid = ""
        textPacket.platformChatId = ""
        textPacket.filteredMessage = ""
        clientBound(textPacket)
    }

}
