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
) : InterruptiblePacketHandler, Configurable {

    open lateinit var session: GameSession

    // Используем приватное поле для хранения фактического состояния.
    // 'by mutableStateOf' предназначен для интеграции с Compose UI,
    // он не должен напрямую вызывать onEnabled/onDisabled.
    private var _isEnabledState by mutableStateOf(defaultEnabled) // Переименовано для ясности

    // Публичное свойство isEnabled
    open var isEnabled: Boolean
        get() = _isEnabledState
        set(value) {
            // Если значение не изменилось, просто выходим.
            if (_isEnabledState == value) return

            // Сохраняем новое значение.
            _isEnabledState = value

            // ВАЖНО: onEnabled()/onDisabled() НЕ ВЫЗЫВАЮТСЯ ЗДЕСЬ НАПРЯМУЮ!
            // Их вызов должен быть явным и происходить только тогда,
            // когда session гарантированно доступна.
            // Например, после инициализации модуля или при ручном переключении в UI/команде.
        }

    val isSessionCreated: Boolean
        get() = ::session.isInitialized

    var isExpanded by mutableStateOf(false)

    var isShortcutDisplayed by mutableStateOf(false)

    var shortcutX = 0

    var shortcutY = 100

    val overlayShortcutButton by lazy { OverlayShortcutButton(this) }

    override val values: MutableList<Value<*>> = ArrayList()

    /**
     * Вспомогательный метод для безопасного доступа к сессии.
     * Используется для выполнения действий, требующих инициализированной сессии.
     */
    protected fun runOnSession(action: (GameSession) -> Unit) {
        if (isSessionCreated) {
            action(session)
        } else {
            // Это сообщение для отладки, не для вывода пользователю.
            // Указывает, что попытка доступа к сессии была до её инициализации.
            println("DEBUG: Session not initialized for module ${this.name} during runOnSession.")
        }
    }

    /**
     * Вызывается один раз при инициализации ModuleManager,
     * после того как сессия становится доступной.
     * Подклассы могут переопределять этот метод для своей специфической инициализации.
     */
    open fun initialize() {
        // Здесь session гарантированно инициализирована.
        // Теперь, если модуль должен быть включен по умолчанию или был включен в конфиге,
        // мы можем безопасно вызвать onEnabled().
        // Важно: вызывайте onEnabled/onDisabled только когда session готова.
        // Это предотвратит ошибку "Backing field".
        runOnSession { it.displayClientMessage("DEBUG: Модуль ${this.name} проинициализирован.") }

        // Если модуль был defaultEnabled (или загружен как enabled из конфига),
        // вызываем onEnabled() сейчас, когда session готова.
        if (isEnabled) { // isEnabled здесь обращается к _isEnabledState
            onEnabled()
        }
    }

    // Эти методы теперь будут просто выполнять свою логику,
    // а сообщения в чат будут отправляться через runOnSession.
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
        put("state", isEnabled) // Сохраняем текущее состояние isEnabled
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
            // При загрузке из JSON, устанавливаем приватное поле _isEnabledState напрямую,
            // чтобы избежать вызова onEnabled/onDisabled во время загрузки конфига.
            // Фактический вызов onEnabled/onDisabled (если состояние enabled)
            // произойдет позже в initialize().
            _isEnabledState = (jsonElement["state"] as? JsonPrimitive)?.boolean ?: _isEnabledState

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
        }
    }

    private fun sendToggleMessage(enabled: Boolean) {
        // Используем runOnSession для безопасной отправки сообщения.
        // runOnSession уже проверит isSessionCreated.
        runOnSession { currentSession ->
            val stateText = if (enabled) "enabled".translatedSelf else "disabled".translatedSelf
            val status = (if (enabled) "§a" else "§c") + stateText
            val moduleName = name.translatedSelf
            val message = "§l§c[WClient] §r§7${moduleName} §8» $status"
            currentSession.displayClientMessage(message)
        }
    }
}
