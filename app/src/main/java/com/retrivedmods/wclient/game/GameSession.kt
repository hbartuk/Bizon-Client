// File: app/src/main/java/com/retrivedmods/wclient/game/GameSession.kt
package com.retrivedmods.wclient.game

import com.retrivedmods.wclient.application.AppContext
import com.retrivedmods.wclient.game.entity.LocalPlayer
import com.retrivedmods.wclient.game.world.Level
import com.mucheng.mucute.relay.MuCuteRelaySession
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import com.retrivedmods.wclient.game.Module // <-- ИСПРАВЛЕНО: Правильный путь для Module.kt
import com.retrivedmods.wclient.game.ModuleManager // <-- ИСПРАВЛЕНО: Предполагаю, что ModuleManager тоже здесь

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

        for (module in ModuleManager.modules) {
            // module.session = this // Эта строка не нужна, если сессия передаётся в конструктор Module
            module.beforePacketBound(interceptablePacket)
            if (interceptablePacket.isIntercepted) {
                return true
            }
        }

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

    // НОВЫЙ МЕТОД: для получения модуля по его классу
    fun <T : Module> getModule(moduleClass: Class<T>): T? {
        for (module in ModuleManager.modules) {
            if (moduleClass.isInstance(module)) {
                return moduleClass.cast(module)
            }
        }
        return null
    }
}
