package com.retrivedmods.wclient.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.retrivedmods.wclient.overlay.OverlayShortcutButton
import com.retrivedmods.wclient.util.translatedSelf
import kotlinx.serialization.json.*
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

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
                // Когда isEnabled меняется через сеттер, вызываем соответствующие методы
                if (_isEnabledState) {
                    onEnabled()
                } else {
                    onDisabled()
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
        // Если модуль включен по умолчанию, он вызовет onEnabled() через сеттер isEnabled
        // if (isEnabled) onEnabled() // Эта строка теперь может быть лишней, так как сеттер isEnabled это делает
    }

    // --- Вот этот метод нужно добавить! ---
    fun toggle() {
        isEnabled = !isEnabled // Это вызовет сеттер isEnabled, который в свою очередь вызовет onEnabled/onDisabled
    }
    // ------------------------------------

    open fun onEnabled() { sendToggleMessage(true) }
    open fun onDisabled() { sendToggleMessage(false) }
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
    }

    open fun fromJson(jsonElement: JsonElement) {
        if (jsonElement is JsonObject) {
            // При загрузке из JSON, напрямую обновляем _isEnabledState, чтобы не вызывать onEnabled/onDisabled
            // во время инициализации, а только когда пользователь реально его переключает.
            // Если тебе нужно вызывать onEnabled/onDisabled при загрузке,
            // тогда вызывай isEnabled = ... вместо _isEnabledState = ...
            _isEnabledState = (jsonElement["state"] as? JsonPrimitive)?.booleanOrNull ?: _isEnabledState
            (jsonElement["values"] as? JsonObject)?.forEach { (key, elem) ->
                getValue(key)?.runCatching { fromJson(elem) }?.onFailure { getValue(key)?.reset() }
            }
            (jsonElement["shortcut"] as? JsonObject)?.let {
                shortcutX = (it["x"] as? JsonPrimitive)?.intOrNull ?: shortcutX
                shortcutY = (it["y"] as? JsonPrimitive)?.intOrNull ?: shortcutY
                isShortcutDisplayed = true
            }
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
