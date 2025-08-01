package com.retrivedmods.wclient.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.retrivedmods.wclient.overlay.OverlayShortcutButton
import com.retrivedmods.wclient.util.translatedSelf
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.put
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket

abstract class Module(
    val name: String,
    val category: ModuleCategory,
    defaultEnabled: Boolean = false,
    val private: Boolean = false
) : InterruptiblePacketHandler,
    Configurable {

    open lateinit var session: GameSession

    private var _isEnabled by mutableStateOf(defaultEnabled)

    var isEnabled: Boolean
        get() = _isEnabled
        set(value) {
            if (_isEnabled != value) {
                _isEnabled = value
                if (value) {
                    onEnabled()
                } else {
                    onDisabled()
                    isSettingsOpen = false
                }
            }
        }

    val isSessionCreated: Boolean
        get() = ::session.isInitialized

    var isExpanded by mutableStateOf(false)

    var isShortcutDisplayed by mutableStateOf(false)

    var shortcutX = 0

    var shortcutY = 100

    val overlayShortcutButton by lazy { OverlayShortcutButton(this) }

    var isSettingsOpen by mutableStateOf(false)

    override val values: MutableList<Value<*>> = ArrayList()

    override fun getValue(name: String): Value<*>? {
        return values.firstOrNull { it.name == name }
    }

    protected fun runOnSession(action: (GameSession) -> Unit) {
        if (isSessionCreated) action(session)
        else println("DEBUG: Session not initialized for module ${this.name} during runOnSession.")
    }

    open fun initialize() {
        runOnSession { it.displayClientMessage("DEBUG: Модуль ${this.name} проинициализирован.") }
    }

    fun toggle() {
        isEnabled = !isEnabled
    }

    open fun onEnabled() {
        sendToggleMessage(true)
    }

    open fun onDisabled() {
        sendToggleMessage(false)
        isSettingsOpen = false
    }
    
    // ДОБАВЛЕН НОВЫЙ МЕТОД onTick()
    open fun onTick() {}

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) { /* Пустая реализация по умолчанию */ }
    override fun afterPacketBound(packet: BedrockPacket) { /* Пустая реализация по умолчанию */ }
    override fun onDisconnect(reason: String) { /* Пустая реализация по умолчанию */ }

    open fun toJson() = buildJsonObject {
        put("state", isEnabled)
        put("values", buildJsonObject {
            values.forEach { value ->
                put(value.name, value.toJson())
            }
        })
        if (isShortcutDisplayed) {
            put("shortcut", buildJsonObject {
                put("x", shortcutX)
                put("y", shortcutY)
            })
        }
        put("isSettingsOpen", isSettingsOpen)
    }

    open fun fromJson(jsonElement: JsonElement) {
        if (jsonElement is JsonObject) {
            isEnabled = (jsonElement["state"] as? JsonPrimitive)?.boolean ?: isEnabled
            (jsonElement["values"] as? JsonObject)?.let {
                it.forEach { jsonObject ->
                    val value = getValue(jsonObject.key) ?: return@forEach
                    try {
                        value.fromJson(jsonObject.value)
                    } catch (e: Throwable) {
                        value.reset()
                    }
                }
            }
            (jsonElement["shortcut"] as? JsonObject)?.let {
                shortcutX = (it["x"] as? JsonPrimitive)?.int ?: shortcutX
                shortcutY = (it["y"] as? JsonPrimitive)?.int ?: shortcutY
                isShortcutDisplayed = true
            }
            isSettingsOpen = (jsonElement["isSettingsOpen"] as? JsonPrimitive)?.boolean ?: isSettingsOpen
        }
    }

    private fun sendToggleMessage(enabled: Boolean) {
        if (!isSessionCreated) {
            return
        }

        val stateText = if (enabled) "enabled".translatedSelf else "disabled".translatedSelf
        val status = (if (enabled) "§a" else "§c") + stateText
        val moduleName = name.translatedSelf
        val message = "§l§c[WClient] §r§7${moduleName} §8» $status"

        session.displayClientMessage(message)
    }
}
