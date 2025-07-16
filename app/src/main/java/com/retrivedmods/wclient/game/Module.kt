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
import kotlinx.serialization.json.contentOrNull // Для EnumValue
import kotlinx.serialization.json.booleanOrNull // Для безопасного парсинга
import kotlinx.serialization.json.intOrNull // Для безопасного парсинга
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// --- БАЗОВЫЕ КЛАССЫ VALUE И ФУНКЦИИ-ДЕЛЕГАТЫ (упрощенные, без Lombok-специфики) ---
// Эти классы нужны, так как они используются в Module.kt (override val values)
// и в ваших модулях, если они конфигурируются через такие значения.

// Базовый класс для всех типов значений
abstract class Value<T>(val name: String, var value: T, protected val defaultValue: T) {
    abstract fun toJson(): JsonElement
    abstract fun fromJson(jsonElement: JsonElement)
    open fun reset() { this.value = defaultValue }
}

// Пример Boolean значения (для BoolValue из старых уроков)
class BooleanValue(name: String, defaultValue: Boolean) : Value<Boolean>(name, defaultValue) {
    override fun toJson() = JsonPrimitive(value)
    override fun fromJson(jsonElement: JsonElement) {
        if (jsonElement is JsonPrimitive) {
            value = jsonElement.booleanOrNull ?: defaultValue // Используем booleanOrNull для безопасности
        } else {
            reset()
        }
    }
}

// Пример Int значения
class IntValue(name: String, defaultValue: Int) : Value<Int>(name, defaultValue) {
    override fun toJson() = JsonPrimitive(value)
    override fun fromJson(jsonElement: JsonElement) {
        if (jsonElement is JsonPrimitive) {
            value = jsonElement.intOrNull ?: defaultValue // Используем intOrNull для безопасности
        } else {
            reset()
        }
    }
}

// Пример Enum значения
class EnumValue<E : Enum<E>>(name: String, defaultValue: E, private val enumClass: Class<E>) : Value<E>(name, defaultValue) {
    override fun toJson() = JsonPrimitive(value.name)
    override fun fromJson(jsonElement: JsonElement) {
        if (jsonElement is JsonPrimitive) {
            val enumName = jsonElement.contentOrNull
            if (enumName != null) {
                try {
                    value = java.lang.Enum.valueOf(enumClass, enumName)
                } catch (e: IllegalArgumentException) {
                    value = defaultValue
                }
            } else {
                reset()
            }
        } else {
            reset()
        }
    }
}

// Функции-делегаты для удобства создания значений в модулях
fun Module.boolValue(name: String, defaultValue: Boolean): ReadWriteProperty<Module, Boolean> {
    val valueInstance = BooleanValue(name, defaultValue)
    this.values.add(valueInstance)
    return object : ReadWriteProperty<Module, Boolean> {
        override fun getValue(thisRef: Module, property: KProperty<*>) = valueInstance.value
        override fun setValue(thisRef: Module, property: KProperty<*>, value: Boolean) {
            valueInstance.value = value
        }
    }
}

fun Module.intValue(name: String, defaultValue: Int): ReadWriteProperty<Module, Int> {
    val valueInstance = IntValue(name, defaultValue)
    this.values.add(valueInstance)
    return object : ReadWriteProperty<Module, Int> {
        override fun getValue(thisRef: Module, property: KProperty<*>) = valueInstance.value
        override fun setValue(thisRef: Module, property: KProperty<*>, value: Int) {
            valueInstance.value = value
        }
    }
}

fun <T : Enum<T>> Module.enumValue(name: String, defaultValue: T): ReadWriteProperty<Module, T> {
    val valueInstance = EnumValue(name, defaultValue, defaultValue.javaClass)
    this.values.add(valueInstance)
    return object : ReadWriteProperty<Module, T> {
        override fun getValue(thisRef: Module, property: KProperty<*>) = valueInstance.value
        override fun setValue(thisRef: Module, property: KProperty<*>, value: T) {
            valueInstance.value = value
        }
    }
}

// --- КОНЕЦ БАЗОВЫХ КЛАССОВ VALUE И ДЕЛЕГАТОВ ---


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

    // Убедитесь, что Value здесь ссылается на классы Value выше в этом файле
    override val values: MutableList<Value<*>> = ArrayList()

    // ИСПРАВЛЕНИЕ: Добавлен override к getValue.
    // Этот метод теперь должен быть здесь, если Configurable имеет getValue
    override fun getValue(name: String): Value<*>? { // <<<<<<<<<< ИСПРАВЛЕНО ЗДЕСЬ
        return values.firstOrNull { it.name == name }
    }


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
            _isEnabledState = (jsonElement["state"] as? JsonPrimitive)?.booleanOrNull ?: _isEnabledState

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
