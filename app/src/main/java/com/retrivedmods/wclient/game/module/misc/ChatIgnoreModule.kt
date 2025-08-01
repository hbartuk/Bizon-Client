package com.retrivedmods.wclient.game.module.misc

import com.google.gson.Gson
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.GameSession
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import java.io.File

class ChatIgnoreModule : Module("ChatIgnore", ModuleCategory.Misc) {

    private val gson = Gson()
    private val ignoreListFile = "ignore_list.json"
    private val ignoredMessages = mutableSetOf<String>()

    private val ignoreNames by boolValue("Ignore Player Names", true)
    private val showBlockedMessage by boolValue("Show Blocked Message", true)
    private val logIgnoredMessages by boolValue("Log Ignored Messages", false)
    
    override fun onEnabled() {
        super.onEnabled()
        loadIgnoreList()
        session.displayClientMessage("§a[ChatIgnore] Модуль включен. Сообщения, содержащие игнорируемые слова, будут скрыты.")
    }

    override fun onDisabled() {
        super.onDisabled()
        session.displayClientMessage("§c[ChatIgnore] Модуль отключен.")
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet is TextPacket && packet.type == TextPacket.Type.CHAT) {
            val message = packet.message
            val sender = packet.sourceName ?: ""

            // --- ПРОВЕРЯЕМ, НЕ ЯВЛЯЕТСЯ ЛИ ОТПРАВИТЕЛЬ НАМИ ---
            val localPlayerName = session.localPlayer?.name
            if (localPlayerName != null && sender.equals(localPlayerName, ignoreCase = true)) {
                // Если отправитель - это ты, не блокируем сообщение.
                return
            }
            
            // --- ТЕПЕРЬ ФИЛЬТРУЕМ ТОЛЬКО ЧУЖИЕ СООБЩЕНИЯ ---
            val isIgnoredMessage = ignoredMessages.any { ignoredText ->
                // Проверяем, содержит ли сообщение игнорируемое слово (без учета регистра)
                message.contains(ignoredText, ignoreCase = true)
            }

            val isIgnoredSender = ignoreNames && ignoredMessages.any { ignoredName ->
                // Проверяем, содержит ли ник отправителя игнорируемое слово (без учета регистра)
                sender.contains(ignoredName, ignoreCase = true)
            }

            if (isIgnoredMessage || isIgnoredSender) {
                // Если хоть одно условие совпало, блокируем пакет
                interceptablePacket.intercept()
                if (showBlockedMessage) {
                    session.displayClientMessage("§7[ChatIgnore] Заблокировано: §8$message")
                }
                if (logIgnoredMessages) {
                    println("ChatIgnore: Заблокировано сообщение от $sender: $message")
                }
            }
        }
    }

    private fun getIgnoreFile(): File {
        val storageDir = session.context.getExternalFilesDir("wclient_data")
        val dataDir = File(storageDir, "ignore_data")
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }
        return File(dataDir, ignoreListFile)
    }

    fun addIgnoreItem(item: String) {
        if (item.isNotBlank() && item !in ignoredMessages) {
            ignoredMessages.add(item)
            saveIgnoreList()
            session.displayClientMessage("§a[ChatIgnore] Добавлено в список: §7\"$item\"")
        } else if (item in ignoredMessages) {
            session.displayClientMessage("§e[ChatIgnore] \"$item\" уже в списке.")
        }
    }

    fun removeIgnoreItem(item: String) {
        if (ignoredMessages.remove(item)) {
            saveIgnoreList()
            session.displayClientMessage("§c[ChatIgnore] Удалено из списка: §7\"$item\"")
        } else {
            session.displayClientMessage("§e[ChatIgnore] \"$item\" не найден в списке.")
        }
    }

    fun getIgnoreList(): List<String> = ignoredMessages.toList()

    private fun saveIgnoreList() {
        val file = getIgnoreFile()
        val json = gson.toJson(ignoredMessages)
        file.writeText(json)
    }

    private fun loadIgnoreList() {
        val file = getIgnoreFile()
        if (file.exists()) {
            val json = file.readText()
            try {
                val list = gson.fromJson(json, Array<String>::class.java)
                ignoredMessages.addAll(list)
                session.displayClientMessage("§a[ChatIgnore] Загружено ${ignoredMessages.size} игнорируемых слов из файла.")
            } catch (e: Exception) {
                session.displayClientMessage("§c[ChatIgnore] Ошибка загрузки списка: ${e.message}")
            }
        }
    }
}
