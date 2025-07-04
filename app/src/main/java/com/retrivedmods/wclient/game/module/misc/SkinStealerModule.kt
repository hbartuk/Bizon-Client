package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.data.skin.SkinCache
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket
import org.cloudburstmc.protocol.bedrock.packet.ModalFormRequestPacket
import org.cloudburstmc.protocol.bedrock.packet.ModalFormResponsePacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import org.cloudburstmc.protocol.bedrock.data.SerializedSkin
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class SkinStealerModule : Module("skinstealer", ModuleCategory.Misc) {
    private val FORM_ID = 12345 // Уникальный ID формы

    init {
        sendClientMessage("Debug: SkinStealerModule initialized, enabled: $isEnabled")
    }

    // Открыть форму для ввода ника
    fun openSkinForm() {
        val form = JsonObject().apply {
            addProperty("type", "form")
            addProperty("title", "Skin Stealer")
            addProperty("content", "Enter the player name to steal their skin:")
            val elements = JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("type", "input")
                    addProperty("text", "Player Name")
                    addProperty("placeholder", "e.g., Icha728")
                    addProperty("default", "")
                })
            }
            add("buttons", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("text", "Apply Skin")
                })
                add(JsonObject().apply {
                    addProperty("text", "Cancel")
                })
            })
        }
        val packet = ModalFormRequestPacket().apply {
            formId = FORM_ID
            formData = Gson().toJson(form)
        }
        session.clientBound(packet)
        sendClientMessage("Debug: Skin form sent to player")
    }

    // Обработка команды .skin
    fun handleChatCommand(message: String) {
        sendClientMessage("Debug: Received message: $message")
        if (!isEnabled) {
            sendClientMessage("Debug: SkinStealerModule is disabled")
            return
        }
        val args = message.trim().split("\\s+".toRegex())
        if (args.isEmpty() || args[0] != ".skin") {
            sendClientMessage("Debug: Not a .skin command")
            return
        }
        if (args.size == 1) {
            openSkinForm() // Открываем форму, если введена просто .skin
            return
        }
        if (args.size != 2) {
            sendClientMessage("§cUsage: .skin <ник> or .skin")
            return
        }
        applySkin(args[1])
    }

    // Обработка ответа формы
    fun handleFormResponse(packet: ModalFormResponsePacket) {
        if (packet.formId != FORM_ID) return
        if (packet.responseData == null) {
            sendClientMessage("§cForm cancelled")
            return
        }
        try {
            val response = Gson().fromJson(packet.responseData, JsonArray::class.java)
            if (response.size() > 0) {
                val playerName = response[0].asString
                if (playerName.isNotBlank()) {
                    applySkin(playerName)
                } else {
                    sendClientMessage("§cPlayer name cannot be empty")
                }
            }
        } catch (e: Exception) {
            sendClientMessage("§cError processing form: ${e.message}")
        }
    }

    // Логика применения скина
    private fun applySkin(targetNick: String) {
        sendClientMessage("Debug: Attempting to apply skin for $targetNick")
        val skin: SerializedSkin? = SkinCache.getSkin(targetNick)
        if (skin == null) {
            sendClientMessage("§cСкин игрока '$targetNick' не найден в кэше! Он должен зайти через прокси или быть на сервере.")
            return
        }
        try {
            val packet = PlayerSkinPacket().apply {
                uuid = session.localPlayer.uuid
                this.skin = skin
            }
            sendClientMessage("Debug: Sending skin packet for $targetNick, UUID: ${session.localPlayer.uuid}")
            session.serverBound(packet)
            session.clientBound(packet)
            sendClientMessage("§aСкин успешно изменён на скин $targetNick!")
        } catch (e: Exception) {
            sendClientMessage("§cОшибка смены скина: ${e.message}")
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return
        val packet = interceptablePacket.packet
        // Заполнение SkinCache из PlayerListPacket
        if (packet is PlayerListPacket) {
            packet.entries.forEach { entry ->
                SkinCache.putSkin(entry.name, entry.skin)
                sendClientMessage("Debug: Added skin for ${entry.name} to SkinCache")
            }
        }
        // Перехват чат-команд
        if (packet is TextPacket && packet.type == TextPacket.Type.CHAT && packet.sourceName == session.localPlayer.name) {
            handleChatCommand(packet.message)
            interceptablePacket.isCancelled = true // Отменяем отправку команды в чат
        }
        // Обработка ответа формы
        if (packet is ModalFormResponsePacket) {
            handleFormResponse(packet)
        }
    }

    private fun sendClientMessage(msg: String) {
        session.displayClientMessage(msg)
    }
}
