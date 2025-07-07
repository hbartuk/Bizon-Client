package com.retrivedmods.wclient.game.command.impl // Убедись, что это правильный пакет для твоих команд

import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.skin.AnimationData
import org.cloudburstmc.protocol.bedrock.data.skin.ImageData
import org.cloudburstmc.protocol.bedrock.data.skin.PersonaPieceData
import org.cloudburstmc.protocol.bedrock.data.skin.PersonaPieceTintData
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket
import tech.wcs.client.network.ClientSession // Предполагается, что это твой класс ClientSession
import java.util.UUID

/**
 * Команда для "кражи" скина другого игрока.
 *
 * Использование: .steal <никнейм>
 *
 * Важно: этот пример предполагает, что у вас есть доступ к данным скина другого игрока.
 * В реальном клиенте вам нужно будет перехватывать пакеты AddPlayerPacket или SetEntityDataPacket
 * чтобы получить данные скина других игроков.
 */
class SkinStealerCommand(session: ClientSession) : Command(session, "steal", "Украсть скин другого игрока", ".steal <никнейм>") {

    override fun exec(args: List<String>): Boolean {
        if (args.isEmpty()) {
            session.displayClientMessage("§cИспользование: §7.steal <никнейм>")
            return false
        }

        val targetUsername = args[0]

        // --- ВАЖНО: Здесь вам нужно получить объект SerializedSkin целевого игрока ---
        // Это самое сложное место. В реальном клиенте вы должны перехватывать
        // пакеты, которые содержат данные скина других игроков (например, AddPlayerPacket
        // или SetEntityDataPacket). Для примера, используем заглушку.
        val targetSkin: SerializedSkin? = getSkinFromCache(targetUsername) // Ваша функция для получения скина

        if (targetSkin == null) {
            session.displayClientMessage("§cНе удалось найти скин игрока §7$targetUsername§c. Возможно, он не в зоне видимости.")
            return false
        }

        // Генерируем новый уникальный ID для нашего "украденного" скина
        val newSkinId = UUID.randomUUID().toString()

        // --- Формируем новый SerializedSkin для отправки ---
        // Копируем данные скина, но с новым ID и сброшенными премиум/персона флагами
        val stolenSkin = SerializedSkin.builder()
            .skinId(newSkinId)
            .playFabId(session.localPlayer.getPlayFabId()) // ИСПРАВЛЕНО
            .skinData(targetSkin.getSkinData()) // ИСПРАВЛЕНО
            .capeData(targetSkin.getCapeData() ?: ImageData.EMPTY) // ИСПРАВЛЕНО
            // !!! ВАЖНО: GeometryData - это ОЧЕНЬ частая причина проблем !!!
            // Для скинов 64x64/64x32 нужно использовать стандартную геометрию Minecraft.
            // Если скин имеет кастомную модель, вам нужно получить её JSON-строку.
            // Попробуйте начать с "geometry.humanoid.custom" или полного JSON-а.
            .geometryData(targetSkin.getGeometryData() ?: DEFAULT_GEOMETRY_DATA) // ИСПРАВЛЕНО
            .geometryName(targetSkin.getGeometryName()) // ИСПРАВЛЕНО
            .skinResourcePatch(targetSkin.getSkinResourcePatch() ?: DEFAULT_SKIN_RESOURCE_PATCH) // ИСПРАВЛЕНО
            .animations(targetSkin.getAnimations() ?: emptyList()) // ИСПРАВЛЕНО
            .animationData(targetSkin.getAnimationData() ?: "") // ИСПРАВЛЕНО
            .premium(false) // Обязательно false для "украденных" скинов
            .persona(false) // Обязательно false
            .capeOnClassic(false) // Если не уверены, лучше false
            .primaryUser(true) // По идее, true, если это ваш основной скин
            .capeId(targetSkin.getCapeId() ?: "") // ИСПРАВЛЕНО
            .fullSkinId(newSkinId + (targetSkin.getCapeId() ?: "")) // ИСПРАВЛЕНО
            .armSize(targetSkin.getArmSize() ?: "wide") // ИСПРАВЛЕНО
            .skinColor(targetSkin.getSkinColor() ?: "#0") // ИСПРАВЛЕНО
            .personaPieces(targetSkin.getPersonaPieces() ?: emptyList()) // ИСПРАВЛЕНО
            .tintColors(targetSkin.getTintColors() ?: emptyList()) // ИСПРАВЛЕНО
            .overridingPlayerAppearance(true) // Обычно true для кастомных скинов
            .build()

        // --- Детальные логи для отладки ---
        session.displayClientMessage("§e--- Логи SerializedSkin для отправки ---")
        session.displayClientMessage("§eSkinId: §b${stolenSkin.getSkinId()}") // ИСПРАВЛЕНО, хотя и до этого работало. Лучше использовать геттер
        session.displayClientMessage("§ePlayFabId: §b${stolenSkin.getPlayFabId()}")
        session.displayClientMessage("§eSkinData Width: §b${stolenSkin.getSkinData().getWidth()}")
        session.displayClientMessage("§eSkinData Height: §b${stolenSkin.getSkinData().getHeight()}")
        session.displayClientMessage("§eSkinData Length: §b${stolenSkin.getSkinData().getImage().size} §7(Ожидаемые: ${SerializedSkin.SINGLE_SKIN_SIZE}, ${SerializedSkin.DOUBLE_SKIN_SIZE}, ${SerializedSkin.SKIN_128_64_SIZE}, ${SerializedSkin.SKIN_128_128_SIZE})")
        session.displayClientMessage("§eGeometryData Length: §b${stolenSkin.getGeometryData()?.length}")
        session.displayClientMessage("§eGeometryData (первые 500 симв.): §b${stolenSkin.getGeometryData()?.take(500)}")
        session.displayClientMessage("§eGeometryName: §b${stolenSkin.getGeometryName()}")
        session.displayClientMessage("§eSkinResourcePatch: §b${stolenSkin.getSkinResourcePatch()}")
        session.displayClientMessage("§eIsPremium: §b${stolenSkin.isPremium()}")
        session.displayClientMessage("§eIsPersona: §b${stolenSkin.isPersona()}")
        session.displayClientMessage("§eIsCapeOnClassic: §b${stolenSkin.isCapeOnClassic()}")
        session.displayClientMessage("§eAnimations size: §b${stolenSkin.getAnimations().size}")
        session.displayClientMessage("§eCapeData Length: §b${stolenSkin.getCapeData().getImage().size}")
        session.displayClientMessage("§eArmSize: §b${stolenSkin.getArmSize()}")
        session.displayClientMessage("§eFullSkinId: §b${stolenSkin.getFullSkinId()}")
        session.displayClientMessage("§e--- Конец логов SerializedSkin ---")

        // Создаем и отправляем PlayerSkinPacket
        val playerSkinPacket = PlayerSkinPacket().apply {
            uuid = session.localPlayer.getUuid() // ИСПРАВЛЕНО
            skin = stolenSkin
            newSkinName = newSkinId
            oldSkinName = session.localPlayer.getSkin().getSkinId() // ИСПРАВЛЕНО
            premium = false // Это поле в NukkitX может игнорироваться, как мы видели
        }

        session.serverBound(playerSkinPacket)
        session.displayClientMessage("§a[WClient] Попытка установить скин игрока §b$targetUsername§a. Проверьте результат.")

        return true
    }

    // --- Заглушка для получения скина из кэша ---
    // В реальном клиенте это должен быть ваш механизм для хранения скинов других игроков.
    // Например, вы парсите AddPlayerPacket и сохраняете SerializedSkin в HashMap.
    private fun getSkinFromCache(username: String): SerializedSkin? {
        // Здесь должен быть код, который ищет скин по никнейму в вашем хранилище
        // Например:
        // return session.entityManager.getPlayerSkin(username)
        session.displayClientMessage("§6[DEBUG] Вызов заглушки getSkinFromCache. Вам нужно реализовать её!")
        // Возвращаем тестовый скин для демонстрации, если нет реального
        return createTestSkin() // В реальном коде заменить на реальное получение скина
    }

    // --- Заглушка для создания тестового скина (только для демонстрации!) ---
    // Это просто пример, чтобы код скомпилировался.
    // Вам нужно будет заменить это на реальные данные скина, полученные от сервера.
    private fun createTestSkin(): SerializedSkin {
        val testSkinData = ByteArray(SerializedSkin.DOUBLE_SKIN_SIZE) { 0xFF.toByte() } // Белый скин 64x64
        val testImageData = ImageData(64, 64, testSkinData)

        return SerializedSkin.builder()
            .skinId(UUID.randomUUID().toString())
            .playFabId("")
            .skinData(testImageData)
            .geometryData(DEFAULT_GEOMETRY_DATA) // Стандартная геометрия для 64x64
            .skinResourcePatch(DEFAULT_SKIN_RESOURCE_PATCH)
            .premium(false)
            .persona(false)
            .capeOnClassic(false)
            .armSize("wide")
            .build()
    }

    // --- Константы для стандартной геометрии ---
    // ЭТО ОЧЕНЬ ВАЖНО! Для большинства скинов вам понадобится полная JSON-модель.
    // Пример для стандартного скина 64x64 ("Стив"):
    // Этот JSON нужно получить из ресурсов игры или из какой-либо базы данных Minecraft Bedrock геометрий.
    // Если вы крадете скин с тонкими руками, вам нужна другая геометрия.
    private val DEFAULT_GEOMETRY_DATA = """
        {
          "format_version": "1.8.0",
          "minecraft:geometry": [
            {
              "description": {
                "identifier": "geometry.humanoid.custom",
                "texture_width": 64,
                "texture_height": 64
              },
              "bones": [
                {"name": "body", "pivot": [0, 24, 0], "cubes": [ {"origin": [-4, 12, -2], "size": [8, 12, 4], "uv": [16, 16]} ]},
                {"name": "head", "pivot": [0, 24, 0], "cubes": [ {"origin": [-4, 24, -4], "size": [8, 8, 8], "uv": [0, 0]}, {"origin": [-4, 24, -4], "size": [9, 9, 9], "inflate": 0.5, "uv": [32, 0]} ]},
                {"name": "rightArm", "pivot": [-5, 22, 0], "cubes": [ {"origin": [-8, 12, -2], "size": [4, 12, 4], "uv": [40, 16]} ]},
                {"name": "leftArm", "pivot": [5, 22, 0], "cubes": [ {"origin": [4, 12, -2], "size": [4, 12, 4], "uv": [32, 48]} ]},
                {"name": "rightLeg", "pivot": [-1.9, 12, 0], "cubes": [ {"origin": [-4, 0, -2], "size": [4, 12, 4], "uv": [0, 16]} ]},
                {"name": "leftLeg", "pivot": [1.9, 12, 0], "cubes": [ {"origin": [0, 0, -2], "size": [4, 12, 4], "uv": [16, 48]} ]}
              ]
            }
          ]
        }
    """.trimIndent()

    // Это стандартный патч ресурсов для кастомной геометрии.
    private val DEFAULT_SKIN_RESOURCE_PATCH = """
        {"geometry":{"default":"geometry.humanoid.custom"}}
    """.trimIndent()
}
