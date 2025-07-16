// File: app/src/main/java/com/retrivedmods/wclient/game/Module.kt
package com.retrivedmods.wclient.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.retrivedmods.wclient.overlay.OverlayShortcutButton
import com.retrivedmods.wclient.util.translatedSelf
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean // Убедитесь, что этот импорт есть
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int // Убедитесь, что этот импорт есть
import kotlinx.serialization.json.put
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket

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
            if (_isEnabledState == value) return
            _isEnabledState = value
        }

    val isSessionCreated: Boolean
        get() = ::session.isInitialized

    var isExpanded by mutableStateOf(false)

    var isShortcutDisplayed by mutableStateOf(false)

    var shortcutX = 0

    var shortcutY = 100

    val overlayShortcutButton by lazy { OverlayShortcutButton(this) }

    // Предполагается, что класс Value<*> определен где-то еще в вашем проекте
    // (если он был до попыток внедрения спуфинга), или вам нужно будет его создать.
    // Если у вас нет класса Value, это вызовет ошибку.
    override val values: MutableList<Value<*>> = ArrayList()

    protected fun runOnSession(action: (GameSession) -> Unit) {
        if (isSessionCreated) {
            action(session)
        } else {
            println("DEBUG: Session not initialized for module ${this.name} during runOnSession.")
        }
    }

    open fun initialize() {
        runOnSession { it.displayClientMessage("DEBUG: Модуль ${this.name} проинициализирован.") }
        if (isEnabled) {
            onEnabled()
        }
    }

    open fun onEnabled() {
        sendToggleMessage(true)
    }

    open fun onDisabled() {
        sendToggleMessage(false)
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) { /* реализация */ }
    override fun afterPacketBound(packet: BedrockPacket) { /* реализация */ }
    override fun onDisconnect(reason: String) { /* реализация */ }

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
    }

    open fun fromJson(jsonElement: JsonElement) {
        if (jsonElement is JsonObject) {
            // Используем .boolean, если уверены, что это всегда boolean.
            // Если может быть null, используйте .booleanOrNull ?: defaultValue.
            _isEnabledState = (jsonElement["state"] as? JsonPrimitive)?.boolean ?: _isEnabledState

            (jsonElement["values"] as? JsonObject)?.let {
                it.forEach { jsonObject ->
                    val value = getValue(jsonObject.key) ?: return@forEach
                    try {
                        // Здесь предполагается, что Value.fromJson существует
                        value.fromJson(jsonObject.value)
                    } catch (e: Throwable) {
                        value.reset()
                    }
                }
            }
            (jsonElement["shortcut"] as? JsonObject)?.let {
                // Используем .int, если уверены, что это всегда int.
                // Если может быть null, используйте .intOrNull ?: defaultValue.
                shortcutX = (it["x"] as? JsonPrimitive)?.int ?: shortcutX
                shortcutY = (it["y"] as? JsonPrimitive)?.int ?: shortcutY
                isShortcutDisplayed = true
            }
        }
    }

    private fun getValue(name: String): Value<*>? {
        return values.firstOrNull { it.name == name }
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
