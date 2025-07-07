package com.retrivedmods.wclient.game

import com.retrivedmods.wclient.application.AppContext
import com.retrivedmods.wclient.game.entity.LocalPlayer
import com.retrivedmods.wclient.game.world.Level
import com.mucheng.mucute.relay.MuCuteRelaySession
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import com.retrivedmods.wclient.game.Module // Ensure this import is present if Module is referenced directly

@Suppress("MemberVisibilityCanBePrivate")
class GameSession(val muCuteRelaySession: MuCuteRelaySession) : ComposedPacketHandler {

    val localPlayer = LocalPlayer(this)

    val level = Level(this)

    private val versionName by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppContext.instance.packageManager.getPackageInfo(
            AppContext.instance.packageName, 0
        ).versionName
    }

    init {
        // IMPORTANT: Initialize ModuleManager with the current GameSession here.
        // This ensures all modules have access to the session from the start.
        ModuleManager.initialize(this)
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

        // --- Packet processing by modules ---
        for (module in ModuleManager.modules) {
            // The session is already set for modules during ModuleManager.initialize(this).
            // No need to set it again for each packet here.
            module.beforePacketBound(interceptablePacket)
            if (interceptablePacket.isIntercepted) {
                return true
            }
        }

        // --- Command processing logic has been moved to CommandHandlerModule.kt ---
        // This entire block, which handles chat commands, should be removed from here.
        // It's now handled by CommandHandlerModule as a regular module.

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

    // Your existing displayClientMessage method
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

    // Method to get a module by its class
    fun <T : Module> getModule(moduleClass: Class<T>): T? {
        for (module in ModuleManager.modules) {
            if (moduleClass.isInstance(module)) {
                return moduleClass.cast(module)
            }
        }
        return null
    }
}
