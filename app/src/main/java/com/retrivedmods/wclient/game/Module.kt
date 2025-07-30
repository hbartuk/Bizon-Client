// Файл: /home/runner/work/Bizon-Client/Bizon-Client/app/src/main/java/com/retrivedmods/wclient/game/Module.kt

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
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket // !!! Добавь этот импорт для BedrockPacket !!!

abstract class Module(
    val name: String,
    val category: ModuleCategory,
    defaultEnabled: Boolean = false,
    val private: Boolean = false
) : InterruptiblePacketHandler, // Интерфейс, который требует реализации методов работы с пакетами
    Configurable { // Интерфейс, который требует реализации getValue и values

    open lateinit var session: GameSession // Позволяет позже инициализировать сессию

    private var _isEnabled by mutableStateOf(defaultEnabled)

    var isEnabled: Boolean
        get() = _isEnabled
        set(value) {
            if (_isEnabled != value) { // Добавил проверку, чтобы избежать лишних вызовов onEnabled/onDisabled
                _isEnabled = value
                if (value) {
                    onEnabled()
                } else {
                    onDisabled()
                    // !!! ВАЖНО: Закрываем настройки модуля при его выключении !!!
                    isSettingsOpen = false
                }
            }
        }

    val isSessionCreated: Boolean
        get() = ::session.isInitialized

    var isExpanded by mutableStateOf(false) // Для UI, чтобы показать/скрыть подробности модуля

    var isShortcutDisplayed by mutableStateOf(false) // Для кнопки ярлыка на экране

    var shortcutX = 0

    var shortcutY = 100

    val overlayShortcutButton by lazy { OverlayShortcutButton(this) }

    // !!! НОВОЕ СОСТОЯНИЕ ДЛЯ ОКНА НАСТРОЕК !!!
    // Это свойство отвечает за то, открыто ли окно настроек конкретного модуля
    var isSettingsOpen by mutableStateOf(false)
    // ----------------------------------------

    override val values: MutableList<Value<*>> = ArrayList()

    // !!! НОВЫЙ МЕТОД: Для получения Value по имени !!!
    override fun getValue(name: String): Value<*>? {
        return values.firstOrNull { it.name == name }
    }
    // ----------------------------------------

    // !!! НОВЫЙ МЕТОД: Для выполнения действий в контексте GameSession !!!
    // Он безопасно вызывает переданную лямбду, только если сессия инициализирована
    protected fun runOnSession(action: (GameSession) -> Unit) {
        if (isSessionCreated) action(session)
        else println("DEBUG: Session not initialized for module ${this.name} during runOnSession.")
    }
    // ----------------------------------------

    // !!! НОВЫЙ МЕТОД: Инициализация модуля !!!
    // Этот метод будет вызываться после добавления модуля в ModuleManager
    open fun initialize() {
        runOnSession { it.displayClientMessage("DEBUG: Модуль ${this.name} проинициализирован.") }
    }
    // ----------------------------------------

    // !!! НОВЫЙ МЕТОД: Простой переключатель состояния !!!
    fun toggle() {
        isEnabled = !isEnabled
    }
    // ----------------------------------------

    open fun onEnabled() {
        sendToggleMessage(true)
    }

    open fun onDisabled() {
        sendToggleMessage(false)
        isSettingsOpen = false // Убеждаемся, что настройки закрыты при выключении
    }

    // !!! АБСТРАКТНЫЕ МЕТОДЫ ИЗ InterruptiblePacketHandler - ДОЛЖНЫ БЫТЬ ОБЪЯВЛЕНЫ ЗДЕСЬ !!!
    // Или в SoundModule, если он сам их реализует.
    // Если они абстрактные в интерфейсе, они должны быть либо здесь open fun, либо override fun в SoundModule.
    // Если они abstract в InterruptiblePacketHandler, то здесь должны быть abstract fun,
    // а их реализация (даже пустая) должна быть в SoundModule.
    // Я предполагаю, что они open fun, если ты хочешь общую реализацию здесь.
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) { /* Пустая реализация по умолчанию */ }
    override fun afterPacketBound(packet: BedrockPacket) { /* Пустая реализация по умолчанию */ }
    override fun onDisconnect(reason: String) { /* Пустая реализация по умолчанию */ }
    // ----------------------------------------

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
        // !!! Сохраняем состояние окна настроек !!!
        put("isSettingsOpen", isSettingsOpen)
    }

    open fun fromJson(jsonElement: JsonElement) {
        if (jsonElement is JsonObject) {
            // Исправлена проблема с 'booleanOrNull' для большей безопасности
            isEnabled = (jsonElement["state"] as? JsonPrimitive)?.boolean ?: isEnabled
            (jsonElement["values"] as? JsonObject)?.let {
                it.forEach { jsonObject ->
                    val value = getValue(jsonObject.key) ?: return@forEach
                    try {
                        value.fromJson(jsonObject.value)
                    } catch (e: Throwable) {
                        value.reset() // Сброс значения при ошибке загрузки
                    }
                }
            }
            (jsonElement["shortcut"] as? JsonObject)?.let {
                shortcutX = (it["x"] as? JsonPrimitive)?.int ?: shortcutX
                shortcutY = (it["y"] as? JsonPrimitive)?.int ?: shortcutY
                isShortcutDisplayed = true
            }
            // !!! Загружаем состояние окна настроек !!!
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
