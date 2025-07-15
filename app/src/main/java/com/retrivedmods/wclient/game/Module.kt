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
import kotlinx.serialization.json.booleanOrNull // Исправлено: используем booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull // Исправлено: используем intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.contentOrNull
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// --- КЛАССЫ VALUE И ФУНКЦИИ-ДЕЛЕГАТЫ ТЕПЕРЬ НАХОДЯТСЯ ЗДЕСЬ ---

// Базовый класс для всех типов значений
abstract class Value<T>(val name: String, var value: T, protected val defaultValue: T) { // Исправлено: defaultValue теперь protected
    abstract fun toJson(): JsonElement
    abstract fun fromJson(jsonElement: JsonElement)
    open fun reset() { this.value = defaultValue }
}

// Реализация для Boolean значений
class BooleanValue(name: String, defaultValue: Boolean) : Value<Boolean>(name, defaultValue) {
    override fun toJson() = JsonPrimitive(value)
    override fun fromJson(jsonElement: JsonElement) {
        if (jsonElement is JsonPrimitive) { // Проверяем только тип примитива
            value = jsonElement.booleanOrNull ?: defaultValue // Исправлено: используем booleanOrNull и сброс
        } else {
            reset()
        }
    }
}

// Реализация для Int значений
class IntValue(name: String, defaultValue: Int) : Value<Int>(name, defaultValue) {
    override fun toJson() = JsonPrimitive(value)
    override fun fromJson(jsonElement: JsonElement) {
        if (jsonElement is JsonPrimitive) { // Проверяем только тип примитива
            value = jsonElement.intOrNull ?: defaultValue // Исправлено: используем intOrNull и сброс
        } else {
            reset()
        }
    }
}

// Реализация для Enum значений
class EnumValue<E : Enum<E>>(name: String, defaultValue: E, private val enumClass: Class<E>) : Value<E>(name, defaultValue) { // Исправлено: передан defaultValue
    override fun toJson() = JsonPrimitive(value.name)
    override fun fromJson(jsonElement: JsonElement) {
        if (jsonElement is JsonPrimitive) {
            val enumName = jsonElement.contentOrNull
            if (enumName != null) {
                try {
                    value = java.lang.Enum.valueOf(enumClass, enumName)
                } catch (e: IllegalArgumentException) {
                    value = defaultValue // Если имя enum не найдено, используем значение по умолчанию
                }
            } else {
                reset()
            }
        } else {
            reset()
        }
    }
}

// --- ФУНКЦИИ-ДЕЛЕГАТЫ ДЛЯ МОДУЛЕЙ ---
// Эти функции теперь могут быть здесь, так как Value классы находятся в том же пакете

fun Module.boolValue(name: String, defaultValue: Boolean): ReadWriteProperty<Module, Boolean> {
    val valueInstance = BooleanValue(name, defaultValue) // Передаем defaultValue
    this.values.add(valueInstance)
    return object : ReadWriteProperty<Module, Boolean> {
        override fun getValue(thisRef: Module, property: KProperty<*>) = valueInstance.value
        override fun setValue(thisRef: Module, property: KProperty<*>, value: Boolean) {
            valueInstance.value = value
        }
    }
}

fun Module.intValue(name: String, defaultValue: Int): ReadWriteProperty<Module, Int> {
    val valueInstance = IntValue(name, defaultValue) // Передаем defaultValue
    this.values.add(valueInstance)
    return object : ReadWriteProperty<Module, Int> {
        override fun getValue(thisRef: Module, property: KProperty<*>) = valueInstance.value
        override fun setValue(thisRef: Module, property: KProperty<*>, value: Int) {
            valueInstance.value = value
        }
    }
}

fun <T : Enum<T>> Module.enumValue(name: String, defaultValue: T): ReadWriteProperty<Module, T> {
    val valueInstance = EnumValue(name, defaultValue, defaultValue.javaClass) // Передаем defaultValue
    this.values.add(valueInstance)
    return object : ReadWriteProperty<Module, T> {
        override fun getValue(thisRef: Module, property: KProperty<*>) = valueInstance.value
        override fun setValue(thisRef: Module, property: KProperty<*>, value: T) {
            valueInstance.value = value
        }
    }
}

// --- КОНЕЦ НОВЫХ КЛАССОВ И ФУНКЦИЙ ---


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

    // Убедитесь, что Value здесь ссылается на новые классы Value выше
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
            _isEnabledState = (jsonElement["state"] as? JsonPrimitive)?.booleanOrNull ?: _isEnabledState // Исправлено

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
                shortcutX = (it["x"] as? JsonPrimitive)?.intOrNull ?: shortcutX // Исправлено
                shortcutY = (it["y"] as? JsonPrimitive)?.intOrNull ?: shortcutY // Исправлено
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
