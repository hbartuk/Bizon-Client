package com.retrivedmods.wclient.game

import android.content.Context
import android.util.Log
import com.retrivedmods.wclient.game.entity.LocalPlayer
import com.retrivedmods.wclient.game.world.Level
import com.mucheng.mucute.relay.MuCuteRelaySession // ПУТЬ К ВАШЕЙ РЕАЛИЗАЦИИ ПРОКСИ-СЕССИИ!
import org.cloudburstmc.protocol.bedrock.data.PlayerListEntry
import org.cloudburstmc.protocol.bedrock.data.PlayerListEntry.Skin
import org.cloudburstmc.protocol.bedrock.data.PlayerListEntry.Skin.TrustedSkinFlag
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket
import com.google.gson.JsonParser
import com.retrivedmods.wclient.utils.base64Decode // Предполагаем, что у вас есть такая утилита в WClient
import java.io.ByteArrayOutputStream

// Если у вас есть другой интерфейс для обработчиков пакетов,
// адаптируйте его. Для простоты, оставим эту заглушку.
interface ComposedPacketHandler {
    fun beforePacketBound(packet: BedrockPacket): Boolean
    fun afterPacketBound(packet: BedrockPacket)
    fun onDisconnect(reason: String)
}

// ЭТОТ ИНТЕРФЕЙС НУЖЕН ВАШЕМУ ПРОКСИ (MuCuteRelaySession), ЧТОБЫ ВЫЗЫВАТЬ МЕТОДЫ GameSession
// Вам НУЖНО убедиться, что ваша MuCuteRelaySession (или как у вас там назван ваш прокси-сессия)
// вызывает эти методы (onPacketInbound, onPacketOutbound) для ваших слушателей.
interface IWClientPacketListener { // Изменил название, чтобы не путать с dev.sora.relay
    fun onPacketOutbound(packet: BedrockPacket): Boolean
    fun onPacketPostOutbound(packet: BedrockPacket)
    fun onPacketInbound(packet: BedrockPacket): Boolean
    fun onPacketPostInbound(packet: BedrockPacket)
    fun onDisconnect(isClient: Boolean, reason: String)
}


@Suppress("MemberVisibilityCanBePrivate")
class GameSession(
    // Здесь должна быть ссылка на ваш класс, который управляет перехватом пакетов
    val muCuteRelaySession: MuCuteRelaySession,
    val context: Context
) : ComposedPacketHandler, IWClientPacketListener { // Используем ваш интерфейс

    val localPlayer = LocalPlayer(this)
    val level = Level(this)

    private val versionName by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }

    // --- Ваша кастомная текстура скина для Bizon Client ---
    // ПОМЕСТИТЕ ВАШ PNG-ФАЙЛ СКИНА В app/src/main/res/raw/my_bizon_skin.png
    private val customBizonSkinData: ByteArray by lazy {
        try {
            context.resources.openRawResource(com.retrivedmods.wclient.R.raw.my_bizon_skin).use { inputStream ->
                val buffer = ByteArrayOutputStream()
                var nRead: Int
                val data = ByteArray(16384)
                while (inputStream.read(data, 0, data.size).also { nRead = it } != -1) {
                    buffer.write(data, 0, nRead)
                }
                Log.d("GameSession", "Загружены данные кастомного скина, размер: ${buffer.size()} байт")
                buffer.toByteArray()
            }
        } catch (e: Exception) {
            Log.e("GameSession", "Ошибка при загрузке кастомного скина Bizon: ${e.message}", e)
            ByteArray(0) // Возвращаем пустой массив, если ошибка
        }
    }

    private val CUSTOM_SKIN_GEOMETRY_CLASSIC = """
        {"geometry":{"default":"geometry.humanoid.custom"}}
    """.trimIndent()

    private val CUSTOM_SKIN_GEOMETRY_SLIM = """
        {"geometry":{"default":"geometry.humanoid.customSlim"}}
    """.trimIndent()


    // --- Реализация методов IWClientPacketListener ---

    override fun onPacketOutbound(packet: BedrockPacket): Boolean {
        // Логика обработки исходящих пакетов (клиент -> прокси -> сервер)
        // Здесь можно подменить скин, который отправляется на сервер,
        // если это необходимо (например, для других игроков).
        // Но для _вашего_ клиента нужно менять входящие пакеты.

        val interceptablePacket = InterceptablePacketImpl(packet) // Предполагается, что InterceptablePacketImpl существует
        for (module in ModuleManager.modules) { // Предполагается, что ModuleManager существует
            module.beforePacketBound(interceptablePacket)
            if (interceptablePacket.isIntercepted) {
                return false // Пакет перехвачен модулем
            }
        }
        return true // Разрешить отправку пакета
    }

    override fun onPacketPostOutbound(packet: BedrockPacket) {
        for (module in ModuleManager.modules) {
            module.afterPacketBound(packet)
        }
    }

    // ЭТО КЛЮЧЕВОЙ МЕТОД ДЛЯ ПОДМЕНЫ СКИНА ДЛЯ ВАШЕГО КЛИЕНТА
    override fun onPacketInbound(packet: BedrockPacket): Boolean {
        // Логика обработки входящих пакетов (сервер -> прокси -> клиент)
        when (packet) {
            is PlayerListPacket -> handlePlayerListPacket(packet) // МОДИФИКАЦИЯ СКИНА ЗДЕСЬ
            is LoginPacket -> handleLoginPacket(packet)         // ПОЛУЧЕНИЕ XUID ЗДЕСЬ
            // Добавьте другие обработки пакетов, если есть
        }
        return true // Всегда возвращаем true, чтобы МОДИФИЦИРОВАННЫЙ пакет ДОШЁЛ до клиента
    }

    override fun onPacketPostInbound(packet: BedrockPacket) {
        // Логика после обработки входящего пакета
    }

    override fun onDisconnect(isClient: Boolean, reason: String) {
        localPlayer.onDisconnect()
        level.onDisconnect()

        // Вызываем onDisconnect для модулей
        for (module in ModuleManager.modules) {
            module.onDisconnect(reason)
        }
        Log.i("GameSession", "Отключено. Клиент: $isClient, Причина: $reason")
    }

    // --- Вспомогательные методы GameSession ---

    fun displayClientMessage(message: String, type: TextPacket.Type = TextPacket.Type.RAW) {
        val textPacket = TextPacket()
        textPacket.type = type
        textPacket.isNeedsTranslation = false
        textPacket.sourceName = ""
        textPacket.message = message
        textPacket.xuid = ""
        textPacket.platformChatId = ""
        textPacket.filteredMessage = ""
        muCuteRelaySession.clientBound(textPacket) // Отправляем пакет клиенту через сессию прокси
    }

    // --- Метод для обработки PlayerListPacket и подмены скина ---
    private fun handlePlayerListPacket(packet: PlayerListPacket) {
        if (localPlayer.xuid == null) {
            Log.w("GameSession", "XUID локального игрока равен null, не могу изменить собственный скин в PlayerListPacket. Убедитесь, что LoginPacket был обработан.")
            return
        }

        val newEntries = mutableListOf<PlayerListEntry>()

        for (entry in packet.entries) {
            if (entry.xuid == localPlayer.xuid) {
                Log.d("GameSession", "Найдена запись локального игрока (${entry.displayName}, XUID: ${entry.xuid}) в PlayerListPacket. Модифицирую скин.")

                val newSkin = Skin(
                    "BizonClient_Skin_${localPlayer.xuid}",
                    Skin.SkinData(customBizonSkinData),
                    Skin.SkinData(customBizonSkinData), // fullSkinData часто то же самое
                    null, // capedata, если у вас есть плащ
                    CUSTOM_SKIN_GEOMETRY_CLASSIC, // Выберите: CLASSIC или SLIM
                    "", // animationData
                    null, // personaPieces
                    null, // pieceTintColors
                    false, // isPremium
                    false, // isPersona
                    true, // isPrimaryUser (ВАЖНО! Указывает, что это основной пользователь)
                    false, // isCapeOnClassicSkin
                    false, // isOverride
                    TrustedSkinFlag.ALL_EMISSIVE // Можно попробовать TrustedSkinFlag.EMPTY или TrustedSkinFlag.NONE
                )

                val modifiedEntry = PlayerListEntry(
                    entry.uuid,
                    entry.xuid,
                    entry.displayName,
                    entry.entityId,
                    entry.buildPlatform,
                    entry.devNetId,
                    newSkin, // <-- Подставляем наш новый скин
                    entry.isTeacher,
                    entry.isHost,
                    entry.currentSequenceNumber
                )
                newEntries.add(modifiedEntry)
            } else {
                newEntries.add(entry)
            }
        }

        packet.entries.clear()
        packet.entries.addAll(newEntries)

        Log.d("GameSession", "PlayerListPacket модифицирован для скина локального игрока. Всего записей: ${newEntries.size}")
    }

    // --- Метод для извлечения XUID из LoginPacket ---
    private fun handleLoginPacket(packet: LoginPacket) {
        try {
            val chainData = JsonParser.parseString(packet.chainData.toString(Charsets.UTF_8)).asJsonObject
            val chain = chainData.getAsJsonArray("chain")
            for (element in chain) {
                val token = element.asString
                val jwtSplit = token.split(".")
                if (jwtSplit.size < 2) continue
                
                val payloadObject = JsonParser.parseString(base64Decode(jwtSplit[1]).toString(Charsets.UTF_8)).asJsonObject
                
                if (payloadObject.has("xuid")) {
                    val xuid = payloadObject.get("xuid").asString
                    localPlayer.xuid = xuid // Устанавливаем XUID для localPlayer
                    Log.i("GameSession", "XUID локального игрока установлен на: $xuid")
                    break
                }
            }
        } catch (e: Exception) {
            Log.e("GameSession", "Ошибка при парсинге LoginPacket для XUID: ${e.message}", e)
        }
    }

    // --- Методы для управления звуками (как вы их предоставили) ---
    fun stopAllSounds() {
        println("GameSession: Останавливаю все звуки.")
    }

    fun playSound(soundName: String, volume: Float = 1.0f, pitch: Float = 1.0f) {
        println("GameSession: Воспроизвожу звук: $soundName (Громкость: $volume, Высота тона: $pitch)")
    }

    fun toggleSounds(enable: Boolean) {
        println("GameSession: Переключаю звуки на: $enable")
    }

    fun soundList(soundSet: Any) { // Замените 'Any' на ваш фактический тип
        println("GameSession: Устанавливаю список звуков на: $soundSet")
    }
}

// --- Классы-заглушки для компиляции ---
// Если у вас УЖЕ есть эти классы в проекте, УДАЛИТЕ ЭТИ ЗАГЛУШКИ.
// Они здесь только для того, чтобы весь GameSession.kt мог компилироваться.

// Эта заглушка для класса вашей прокси-сессии. Замените на реальную реализацию.
// Она должна иметь метод clientBound(packet: BedrockPacket)
/*
package com.mucheng.mucute.relay

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket

interface MuCuteRelaySession {
    fun clientBound(packet: BedrockPacket) // Отправляет пакет от прокси к клиенту
    fun serverBound(packet: BedrockPacket) // Отправляет пакет от прокси к серверу
}
*/

// Пример заглушки для InterceptablePacketImpl
/*
class InterceptablePacketImpl(val packet: BedrockPacket) {
    var isIntercepted: Boolean = false
    // Можете добавить методы для модификации пакета, если они используются модулями
}
*/

// Пример заглушки для ModuleManager (если он есть в вашем проекте)
/*
object ModuleManager {
    val modules: List<Any> = emptyList() // Замените Any на ваш фактический тип модуля
    fun beforePacketBound(packet: InterceptablePacketImpl) { }
    fun afterPacketBound(packet: BedrockPacket) { }
    fun onDisconnect(reason: String) { }
}
*/

// Пример заглушки для com.retrivedmods.wclient.utils.base64Decode
// Убедитесь, что у вас есть реальная функция декодирования Base64
/*
package com.retrivedmods.wclient.utils

fun base64Decode(str: String): ByteArray {
    // Реализуйте здесь декодирование Base64
    // Например: android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
    return str.toByteArray() // ЗАГЛУШКА!
}
*/
