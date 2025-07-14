package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.Base64
import java.util.UUID
import java.nio.charset.StandardCharsets

// *** ВНИМАНИЕ: Если ошибка 'Unresolved reference: enumValue' останется,
// *** то обязательно пришлите содержимое файла com.retrivedmods.wclient.game.Module.kt!

class SpoofingModule : Module("Spoofing", ModuleCategory.Misc) {

    enum class DeviceOS(val id: Int, val displayName: String) {
        // Мобильные платформы
        ANDROID(1, "Android"),
        IOS(2, "iOS"),
        AMAZON_FIRE_OS(4, "Amazon FireOS"),

        // ПК-платформы
        WINDOWS_10(7, "Windows 10 (PC)"),

        // Консоли
        XBOX(9, "Xbox (One/Series)"),
        PLAYSTATION(10, "PlayStation"),
        NINTENDO_SWITCH(8, "Nintendo Switch"),

        // Виртуальная/Смешанная реальность
        GEAR_VR(5, "Gear VR (Вирт. реальность)"),
        HOLOLENS(6, "HoloLens (Смеш. реальность)"),

        // Другие/Менее распространенные
        LINUX(11, "Linux (Неоф./Кастомная)"),

        // Опция для использования оригинальной ОС без подделки
        ORIGINAL_OS(-1, "Оригинальная ОС")
    }

    private var spoofDeviceOS by enumValue("Подделать ОС устройства", DeviceOS.ORIGINAL_OS)
    private var changeDeviceIdOnSpoof by boolValue("Сменить Device ID при подделке", true)

    private var spoofedDeviceId: String? = null

    private val objectMapper = ObjectMapper()

    override fun onEnabled() {
        super.onEnabled()
        session?.displayClientMessage("§8[Spoofing] §aМодуль активирован.")

        if (spoofDeviceOS != DeviceOS.ORIGINAL_OS && changeDeviceIdOnSpoof) {
            spoofedDeviceId = UUID.randomUUID().toString()
            session?.displayClientMessage("§8[Spoofing] §aСгенерирован новый Device ID: §f$spoofedDeviceId")
        } else {
            spoofedDeviceId = null
        }
        session?.displayClientMessage("§8[Spoofing] §eИзменения применяются при следующем входе на сервер.")
    }

    override fun onDisabled() {
        super.onDisabled()
        session?.displayClientMessage("§8[Spoofing] §cМодуль деактивирован.")
        spoofedDeviceId = null
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            return
        }

        val packet = interceptablePacket.packet

        if (packet is LoginPacket) {
            if (spoofDeviceOS != DeviceOS.ORIGINAL_OS) {
                try {
                    // *** ИСПРАВЛЕНО: Используем getClientJwt() ***
                    val originalClientDataJwt = packet.getClientJwt()

                    val decodedJwt: DecodedJWT = JWT.decode(originalClientDataJwt)

                    val payloadJsonString = String(Base64.getUrlDecoder().decode(decodedJwt.payload), StandardCharsets.UTF_8)
                    val clientDataNode = objectMapper.readTree(payloadJsonString) as ObjectNode

                    clientDataNode.put("DeviceOS", spoofDeviceOS.id)

                    if (changeDeviceIdOnSpoof && spoofedDeviceId != null) {
                        clientDataNode.put("DeviceId", spoofedDeviceId)
                    }

                    val headerJsonString = String(Base64.getUrlDecoder().decode(decodedJwt.header), StandardCharsets.UTF_8)
                    
                    // Создаем новый JWT с измененным payload
                    val newClientDataJwt = JWT.create()
                        .withHeader(objectMapper.readValue(headerJsonString, Map::class.java) as Map<String, Any>)
                        .withPayload(objectMapper.convertValue(clientDataNode, Map::class.java) as Map<String, Any>)
                        .sign(Algorithm.none()) // ВНИМАНИЕ: Подписываем "никаким" алгоритмом.
                                                // Это может не работать на серверах, строго проверяющих подпись.

                    // *** ИСПРАВЛЕНО: Создаем новый LoginPacket с помощью его конструктора
                    //                  и правильных геттеров (getAuthPayload()) ***
                    val newLoginPacket = LoginPacket(packet.getProtocolVersion(), packet.getAuthPayload(), newClientDataJwt)
                    interceptablePacket.setPacket(newLoginPacket) // Заменяем оригинальный пакет модифицированным

                    session?.displayClientMessage("§8[Spoofing] §aОС подделана на: §f${spoofDeviceOS.displayName}")
                    if (changeDeviceIdOnSpoof && spoofedDeviceId != null) {
                        session?.displayClientMessage("§8[Spoofing] §aDevice ID изменен на: §f$spoofedDeviceId")
                    }
                    session?.displayClientMessage("§8[Spoofing] §eДля применения изменений требуется переподключение к серверу.")

                } catch (e: Exception) {
                    session?.displayClientMessage("§8[Spoofing] §cОшибка при подделке данных: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
}
