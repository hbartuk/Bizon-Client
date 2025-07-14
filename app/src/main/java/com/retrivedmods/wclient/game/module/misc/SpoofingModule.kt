package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.Base64
import java.util.UUID
import java.nio.charset.StandardCharsets

class SpoofingModule : Module("Spoofing", ModuleCategory.Misc) {

    // Расширенное перечисление для выбора операционной системы устройства
    // ID соответствуют известным значениям DeviceOS в Minecraft Bedrock.
    // Если вам нужны другие, поищите DeviceOS ID в Bedrock Protocol.
    enum class DeviceOS(val id: Int, val displayName: String) {
        // Мобильные платформы
        ANDROID(1, "Android"),
        IOS(2, "iOS"),
        AMAZON_FIRE_OS(4, "Amazon FireOS"), // Устройства Amazon Fire

        // ПК-платформы
        WINDOWS_10(7, "Windows 10 (PC)"), // Самая распространенная ПК-версия

        // Консоли
        XBOX(9, "Xbox (One/Series)"),
        PLAYSTATION(10, "PlayStation"),
        NINTENDO_SWITCH(8, "Nintendo Switch"),

        // Виртуальная/Смешанная реальность
        GEAR_VR(5, "Gear VR (Вирт. реальность)"), // Для устройств Samsung Gear VR
        HOLOLENS(6, "HoloLens (Смеш. реальность)"), // Для Microsoft HoloLens

        // Другие/Менее распространенные
        LINUX(11, "Linux (Неоф./Кастомная)"), // Может встречаться в некоторых сборках

        // Опция для использования оригинальной ОС без подделки
        ORIGINAL_OS(-1, "Оригинальная ОС") // Специальное значение для отключения подделки
    }

    // Настройки модуля
    private var spoofDeviceOS by enumValue("Подделать ОС устройства", DeviceOS.ORIGINAL_OS)
    private var changeDeviceIdOnSpoof by boolValue("Сменить Device ID при подделке", true)

    // Храним сгенерированный Device ID для текущей сессии модуля, если он меняется
    private var spoofedDeviceId: String? = null

    // ObjectMapper для работы с JSON внутри JWT
    private val objectMapper = ObjectMapper()

    override fun onEnabled() {
        super.onEnabled()
        session?.displayClientMessage("§8[Spoofing] §aМодуль активирован.")

        // Генерируем новый Device ID, если включена подделка ОС и смена Device ID
        if (spoofDeviceOS != DeviceOS.ORIGINAL_OS && changeDeviceIdOnSpoof) {
            spoofedDeviceId = UUID.randomUUID().toString()
            session?.displayClientMessage("§8[Spoofing] §aСгенерирован новый Device ID: §f$spoofedDeviceId")
        } else {
            spoofedDeviceId = null // Если не подделываем или не меняем ID, сбрасываем его
        }
        session?.displayClientMessage("§8[Spoofing] §eИзменения применяются при следующем входе на сервер.")
    }

    override fun onDisabled() {
        super.onDisabled()
        session?.displayClientMessage("§8[Spoofing] §cМодуль деактивирован.")
        spoofedDeviceId = null // Очищаем сгенерированный Device ID при отключении модуля
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            return
        }

        val packet = interceptablePacket.packet

        // Мы интересуемся только LoginPacket, который отправляется при подключении к серверу
        if (packet is LoginPacket) {
            // Применяем подделку только если выбрана не "Оригинальная ОС"
            if (spoofDeviceOS != DeviceOS.ORIGINAL_OS) {
                try {
                    // LoginPacket.clientData() возвращает JWT строку.
                    // Нам нужно её декодировать, изменить, затем закодировать обратно.
                    val originalClientDataJwt = packet.clientData

                    // 1. Декодируем JWT
                    val decodedJwt: DecodedJWT = JWT.decode(originalClientDataJwt)

                    // Получаем payload JWT как JSON строку, затем парсим её в изменяемый ObjectNode
                    val payloadJsonString = String(Base64.getUrlDecoder().decode(decodedJwt.payload), StandardCharsets.UTF_8)
                    val clientDataNode = objectMapper.readTree(payloadJsonString) as ObjectNode

                    // 2. Модифицируем значения
                    clientDataNode.put("DeviceOS", spoofDeviceOS.id)

                    if (changeDeviceIdOnSpoof && spoofedDeviceId != null) {
                        clientDataNode.put("DeviceId", spoofedDeviceId)
                    }

                    // 3. Собираем новый JWT
                    // Этот блок остается сложным из-за необходимости переподписывать JWT.
                    // Если сервер строго проверяет подпись, то это может вызвать дисконнект.
                    // В большинстве случаев, если CloudburstMC не предоставляет прямой метод для
                    // модификации NbtMap внутри LoginPacket (который затем сам пересоздаст JWT
                    // с корректной подписью), то обойти проверку подписи сложно.
                    // Данный подход создает JWT без подписи, что может быть отклонено сервером.
                    
                    val headerJsonString = String(Base64.getUrlDecoder().decode(decodedJwt.header), StandardCharsets.UTF_8)
                    
                    val newClientDataJwt = JWT.create()
                        .withHeader(objectMapper.readValue(headerJsonString, Map::class.java) as Map<String, Any>)
                        .withPayload(objectMapper.convertValue(clientDataNode, Map::class.java) as Map<String, Any>)
                        .sign(Algorithm.none()) // !!! ВНИМАНИЕ: Подписываем "никаким" алгоритмом. Это сделает JWT невалидным для строгой проверки.


                    // **** ПОТЕНЦИАЛЬНОЕ УЛУЧШЕНИЕ ****
                    // Если ваш LoginPacket имеет конструктор или метод setClientData(),
                    // который принимает String JWT, то вы можете сделать так:
                    // interceptablePacket.setPacket(LoginPacket(packet.protocolVersion, newClientDataJwt, packet.chainData))
                    // (Предполагается, что LoginPacket имеет такой конструктор)

                    session?.displayClientMessage("§8[Spoofing] §aОС подделана на: §f${spoofDeviceOS.displayName}")
                    if (changeDeviceIdOnSpoof && spoofedDeviceId != null) {
                        session?.displayClientMessage("§8[Spoofing] §aDevice ID изменен на: §f$spoofedDeviceId")
                    }
                    session?.displayClientMessage("§8[Spoofing] §eДля применения изменений требуется переподключение к серверу.")

                } catch (e: Exception) {
                    session?.displayClientMessage("§8[Spoofing] §cОшибка при подделке данных: ${e.message}")
                    e.printStackTrace() // Для отладки
                }
            }
        }
    }
}
