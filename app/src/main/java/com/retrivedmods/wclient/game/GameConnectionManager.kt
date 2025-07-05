package com.retrivedmods.wclient.game

import android.util.Log
import com.google.gson.JsonParser
import com.retrivedmods.wclient.application.AppContext
import com.retrivedmods.wclient.game.entity.Account
import com.retrivedmods.wclient.game.entity.identityToken
import com.mucheng.mucute.relay.MuCuteRelaySession
import org.cloudburstmc.protocol.bedrock.Bedrock
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket
import java.util.Base64
import kotlin.concurrent.thread

@Suppress("MemberVisibilityCanBePrivate")
object GameConnectionManager {

    var minecraftRelay: MuCuteRelaySession? = null

    private fun fetchIdentityToken(accessToken: String, deviceInfo: String): String {
        // Здесь должна быть логика получения identityToken.
        // Я предполагаю, что у тебя уже есть эта функция или она находится в другом месте.
        // Это заглушка, чтобы код был компилируемым.
        // Убедись, что твоя реальная функция fetchIdentityToken возвращает нужный JSON-токен.
        return AppContext.instance.accountManager.activeAccount?.identityToken?.toString() ?: "" // Пример: замени на твою реальную реализацию
    }

    private fun base64Encode(input: ByteArray): ByteArray {
        return Base64.getEncoder().encode(input)
    }

    @Suppress("UnstableApiUsage")
    fun connect(hostname: String, port: Int, account: Account, callback: (Boolean) -> Unit) {
        if (this.minecraftRelay != null) {
            this.disconnect()
        }
        thread {
            try {
                minecraftRelay = MuCuteRelaySession(Bedrock.getBedrockPeer().connect(hostname, port), 0, /* Здесь нужен объект MuCuteRelay */) // Предполагаем, что MuCuteRelaySession принимает MuCuteRelay
                minecraftRelay?.start()?.whenComplete { _, throwable ->
                    if (throwable == null) {
                        minecraftRelay?.let { relay ->
                            val identityToken = fetchIdentityToken(account.accessToken, account.deviceInfo)

                            val loginPacket = LoginPacket()
                            loginPacket.setProtocol(Bedrock.PROTOCOL_VERSION)
                            loginPacket.setChain(JsonParser.parseString(identityToken).asJsonArray.map { it.asString })
                            loginPacket.setExtra("") // identityToken.extra может быть здесь, если он не объединен с main identityToken.chain

                            relay.client?.sendPacket(loginPacket) // <-- Отправка LoginPacket

                            // --- ДОБАВЛЕНЫЙ БЛОК: Отправка PlayerSkinPacket после LoginPacket ---
                            val playerSkinManager = PlayerSkinManager(account.uuid)

                            // Здесь нужно получить реальный SerializedSkin игрока.
                            // Используй твою логику для загрузки скина, например, из AppContext.instance.accountManager
                            val playerSkin: SerializedSkin = AppContext.instance.accountManager.activeAccount?.skin // Пример: получить скин из активного аккаунта
                                ?: playerSkinManager.defaultSkin() // Если скин не найден, используй дефолтный

                            val playerSkinPacket = PlayerSkinPacket()
                            playerSkinPacket.uuid = account.uuid // UUID игрока из твоего объекта Account
                            playerSkinPacket.skin = playerSkin
                            playerSkinPacket.newSkinName = playerSkin.skinResourcePatch ?: "Default Skin" // Имя скина
                            playerSkinPacket.oldSkinName = "" // Старое имя скина, если меняется
                            playerSkinPacket.trustedSkin = true // Установи true, если скин из надежного источника (из игры/MS)

                            relay.client?.sendPacket(playerSkinPacket) // <-- Отправка PlayerSkinPacket
                            Log.d("GameConnectionManager", "PlayerSkinPacket отправлен после логина.")
                            // --- КОНЕЦ ДОБАВЛЯЕМОГО БЛОКА ---

                            callback(true)
                        } ?: callback(false)
                    } else {
                        Log.e("GameConnectionManager", "Relay start error: ${throwable.stackTraceToString()}")
                        callback(false)
                    }
                }
            } catch (t: Throwable) {
                Log.e("GameConnectionManager", "Connect error: ${t.stackTraceToString()}")
                callback(false)
            }
        }
    }

    fun disconnect() {
        minecraftRelay?.disconnect()
        minecraftRelay = null
    }
}
