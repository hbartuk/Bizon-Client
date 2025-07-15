// File: app/src/main/java/com/retrivedmods/wclient/game/module/delegates/ModuleDelegates.kt
package com.retrivedmods.wclient.game.module.delegates

import com.retrivedmods.wclient.game.Module // Импортируем Module
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull

// --- Классы VALUE ---
// Базовый класс для всех типов значений
abstract class Value<T>(val name: String, var value: T, private val defaultValue: T) {
    abstract fun toJson(): JsonElement
    abstract fun fromJson(jsonElement: JsonElement)
    open fun reset() { this.value = defaultValue }
}

// Реализация для Boolean значений
class BooleanValue(name: String, defaultValue: Boolean) : Value<Boolean>(name, defaultValue) {
    override fun toJson() = JsonPrimitive(value)
    override fun fromJson(jsonElement: JsonElement) {
        if (jsonElement is JsonPrimitive && jsonElement.isBoolean) {
            value = jsonElement.boolean
        } else {
            reset()
        }
    }
}

// Реализация для Int значений
class IntValue(name: String, defaultValue: Int) : Value<Int>(name, defaultValue) {
    override fun toJson() = JsonPrimitive(value)
    override fun fromJson(jsonElement: JsonElement) {
        if (jsonElement is JsonPrimitive) {
            value = jsonElement.intOrNull ?: defaultValue
        } else {
            reset()
        }
    }
}

// Реализация для Enum значений
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

// --- ФУНКЦИИ-ДЕЛЕГАТЫ ДЛЯ МОДУЛЕЙ ---

// Функция-делегат для Boolean значений
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

// Функция-делегат для Int значений
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

// Функция-делегат для Enum значений
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
