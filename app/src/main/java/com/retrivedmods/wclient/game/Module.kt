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

// --- БАЗОВЫЕ КЛАССЫ VALUE И ДЕЛЕГАТЫ ---

// Абстрактный базовый класс для всех Value
abstract class Value<T>(val name: String, var value: T, protected val defaultValue: T) {
    abstract fun toJson(): JsonElement
    abstract fun fromJson(jsonElement: JsonElement)
    open fun reset() { this.value = defaultValue }
}

// Boolean значение
class BooleanValue(name: String, defaultValue: Boolean) : Value<Boolean>(name, defaultValue) {
    override fun toJson() = JsonPrimitive(value)
    override fun fromJson(jsonElement: JsonElement) {
        value = (jsonElement as? JsonPrimitive)?.booleanOrNull ?: defaultValue
    }
}

// Int значение
class IntValue(name: String, defaultValue: Int) : Value<Int>(name, defaultValue) {
    override fun toJson() = JsonPrimitive(value)
    override fun fromJson(jsonElement: JsonElement) {
        value = (jsonElement as? JsonPrimitive)?.intOrNull ?: defaultValue
    }
}

// Enum значение
class EnumValue<E : Enum<E>>(name: String, defaultValue: E, private val enumClass: Class<E>) : Value<E>(name, defaultValue) {
    override fun toJson() = JsonPrimitive(value.name)
    override fun fromJson(jsonElement: JsonElement) {
        val enumName = (jsonElement as? JsonPrimitive)?.contentOrNull
        value = enumName?.let {
            try { java.lang.Enum.valueOf(enumClass, it) } catch (_: Exception) { defaultValue }
        } ?: defaultValue
    }
}

// Делегаты для Value
fun Module.boolValue(name: String, defaultValue: Boolean): ReadWriteProperty<Module, Boolean> {
    val v = BooleanValue(name, defaultValue)
    values.add(v)
    return object : ReadWriteProperty<Module, Boolean> {
        override fun getValue(thisRef: Module, property: KProperty<*>) = v.value
        override fun setValue(thisRef: Module, property: KProperty<*>, value: Boolean) { v.value = value }
    }
}
fun Module.intValue(name: String, defaultValue: Int): ReadWriteProperty<Module, Int> {
    val v = IntValue(name, defaultValue)
    values.add(v)
    return object : ReadWriteProperty<Module, Int> {
        override fun getValue(thisRef: Module, property: KProperty<*>) = v.value
        override fun setValue(thisRef: Module, property: KProperty<*>, value: Int) { v.value = value }
    }
}
fun <T : Enum<T>> Module.enumValue(name: String, defaultValue: T): ReadWriteProperty<Module, T> {
    val v = EnumValue(name, defaultValue, defaultValue.javaClass)
    values.add(v)
    return object : ReadWriteProperty<Module, T> {
        override fun getValue(thisRef: Module, property: KProperty<*>) = v.value
        override fun setValue(thisRef: Module, property: KProperty<*>, value: T) { v.value = value }
    }
}

// --- КОНЕЦ КЛАССОВ VALUE И ДЕЛЕГАТОВ ---

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
        set(value) { if (_isEnabledState != value) _isEnabledState = value }

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
        if (isEnabled) onEnabled()
    }

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
            _isEnabledState = (jsonElement["state"] as? JsonPrimitive)?.booleanOrNull ?: _isEnabledState
            (jsonElement["values"] as? JsonObject)?.forEach { (key, elem) ->
                getValue(key)?.runCatching { fromJson(elem) } ?: reset()
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
