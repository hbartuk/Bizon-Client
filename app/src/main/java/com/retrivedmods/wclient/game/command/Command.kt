// File: app/src/main/java/com/retrivedmods/wclient/game/command/Command.kt
package com.retrivedmods.wclient.game.command

import com.retrivedmods.wclient.game.GameSession // Убедись, что это правильный импорт для GameSession

abstract class Command(vararg alias: String) {

    val alias: Array<String>

    init {
        if (alias.isEmpty()) {
            throw IllegalArgumentException("У команды должно быть хотя бы одно название (алиас).")
        }
        this.alias = alias.map { it.lowercase() }.toTypedArray() // Всегда храним алиасы в нижнем регистре
    }

    abstract fun exec(args: Array<String>, session: GameSession)

    // Метод для проверки, соответствует ли сообщение этой команде
    fun match(input: String): Boolean {
        // Убедимся, что input начинается с префикса команды (например, ".")
        if (!input.startsWith(".")) return false

        // Разделяем сообщение на префикс, название команды и аргументы
        val parts = input.substring(1).split(" ", limit = 2) // Убираем "." и делим на название и остальные аргументы
        if (parts.isEmpty()) return false

        val commandName = parts[0].lowercase() // Название команды в нижнем регистре

        // Проверяем, совпадает ли название команды с одним из алиасов
        return alias.contains(commandName)
    }
}
