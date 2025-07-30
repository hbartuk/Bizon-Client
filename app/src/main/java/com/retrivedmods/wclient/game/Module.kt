// /home/runner/work/Bizon-Client/Bizon-Client/app/src/main/java/com/retrivedmods/wclient/game/Module.kt

package com.retrivedmods.wclient.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.retrivedmods.wclient.overlay.OverlayShortcutButton
import com.retrivedmods.wclient.util.translatedSelf
import kotlinx.serialization.json.*
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import kotlin.properties.ReadWriteProperty // This import might not be needed if not used elsewhere
import kotlin.reflect.KProperty // This import might not be needed if not used elsewhere

abstract class Module(
    val name: String,
    val category: ModuleCategory,
    defaultEnabled: Boolean = false,
    val private: Boolean = false
) : InterruptiblePacketHandler, Configurable {

    open lateinit var session: GameSession

    private var _isEnabledState by mutableStateOf(defaultEnabled)
    open var isEnabled: Boolean
        get() = _isEnabledState
        set(value) {
            if (_isEnabledState != value) {
                _isEnabledState = value
                if (_isEnabledState) {
                    onEnabled()
                } else {
                    onDisabled()
                    // При выключении модуля, закрываем его окно настроек
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

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {}
    override fun afterPacketBound(packet: BedrockPacket) {}
    override fun onDisconnect(reason: String) {}

    open fun toJson() = buildJsonObject {
        put("state", isEnabled)
        put("values", buildJsonObject {
            values.forEach { value -> put(value.name, value.toJson()) }
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
            _isEnabledState = (jsonElement["state"] as? JsonPrimitive)?.booleanOrNull ?: _isEnabledState
            (jsonElement["values"] as? JsonObject)?.forEach { (key, elem) ->
                getValue(key)?.runCatching { fromJson(elem) }?.onFailure { getValue(key)?.reset() }
            }
            (jsonElement["shortcut"] as? JsonObject)?.let {
                shortcutX = (it["x"] as? JsonPrimitive)?.intOrNull ?: shortcutX
                shortcutY = (it["y"] as? JsonPrimitive)?.intOrNull ?: shortcutY
                isShortcutDisplayed = true
            }
            isSettingsOpen = (jsonElement["isSettingsOpen"] as? JsonPrimitive)?.booleanOrNull ?: isSettingsOpen
        }
    }

    private fun sendToggleMessage(enabled: Boolean) {
        runOnSession { currentSession ->
            val stateText = if (enabled) "enabled".translatedSelf else "disabled".translatedSelf
            val status = (if (enabled) "§a" else "§c") + stateText
            val moduleName = name.translatedSelf
            val message = "§l§c[WClient] §r§7${moduleName} §8» $status"
            currentSession.displayClientMessage(message)
        }
    }
}

// --- ADD THESE CLASSES TO YOUR Module.kt FILE ---

// Make sure ModuleCategory is also defined. Example:
enum class ModuleCategory {
    Combat, Movement, Player, Render, World, Misc
}

abstract class Value<T>(
    val name: String,
    val description: String = ""
) {
    abstract var value: T // Current value
    abstract fun toJson(): JsonElement
    abstract fun fromJson(jsonElement: JsonElement)
    open fun reset() {} // For resetting the value
}

class BooleanValue(name: String, default: Boolean) : Value<Boolean>(name) {
    override var value by mutableStateOf(default)
    override fun toJson() = JsonPrimitive(value)
    override fun fromJson(jsonElement: JsonElement) {
        if (jsonElement is JsonPrimitive) value = jsonElement.booleanOrNull ?: value
    }
}

class NumberValue(name: String, default: Number, val min: Number, val max: Number) : Value<Number>(name) {
    override var value by mutableStateOf(default)
    override fun toJson() = JsonPrimitive(value)
    override fun fromJson(jsonElement: JsonElement) {
        if (jsonElement is JsonPrimitive) value = jsonElement.intOrNull ?: jsonElement.doubleOrNull ?: value
    }
}

class ModeValue(name: String, val modes: List<String>, default: String) : Value<String>(name) {
    override var value by mutableStateOf(default)
    override fun toJson() = JsonPrimitive(value)
    override fun fromJson(jsonElement: JsonElement) {
        if (jsonElement is JsonPrimitive) value = jsonElement.contentOrNull ?: value
    }
}
