package com.retrivedmods.wclient.game.command.impl

import android.graphics.Bitmap
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.data.skin.SkinCache
import org.cloudburstmc.protocol.bedrock.data.skin.ImageData
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

// !!! ВАЖНО !!!
// Убедись, что твой GameSession имеет доступ к Context.
// Например, если ты инициализируешь GameSession из Activity или Application,
// передай ему контекст:
// class GameSession(val context: android.content.Context) { ... }
// И при создании GameSession:
// GameSession(applicationContext) или GameSession(this) из Activity.

class SaveSkinCommand : Command("saveskin") {

    // Вспомогательная функция для сохранения ImageData как PNG
    private fun saveImageDataAsPng(imageData: ImageData, filePath: String) {
        if (imageData.getImage().isEmpty()) {
            return // Нет данных для сохранения
        }

        // Создаем Bitmap из сырых пиксельных данных (предполагаем формат ARGB_8888)
        val bitmap = Bitmap.createBitmap(imageData.getWidth(), imageData.getHeight(), Bitmap.Config.ARGB_8888)
        val buffer = ByteBuffer.wrap(imageData.getImage()) // Оборачиваем байтовый массив в ByteBuffer
        bitmap.copyPixelsFromBuffer(buffer) // Копируем пиксели в Bitmap

        FileOutputStream(filePath).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) // Сжимаем в PNG и записываем в файл
        }
    }

    // Вспомогательная функция для сохранения строки в файл
    private fun saveStringToFile(content: String, filePath: String) {
        File(filePath).writeText(content)
    }

    override fun exec(args: Array<String>, session: GameSession) {
        if (args.isEmpty()) {
            session.displayClientMessage("§cИспользование: §7.saveskin <никнейм>")
            return
        }

        val targetUsername = args[0]
        val targetSkin: SerializedSkin? = SkinCache.getSkin(targetUsername)

        if (targetSkin == null) {
            session.displayClientMessage("§cНе удалось найти скин игрока §7$targetUsername§c. Возможно, он не в зоне видимости или его скин не был кэширован.")
            return
        }

        try {
            // Получаем директорию для сохранения файлов в приватном хранилище приложения.
            // Эти файлы будут доступны только твоему приложению и не требуют разрешений.
            val context: android.content.Context = session.context // <-- Убедись, что 'session.context' доступен
            val skinsSaveDir = File(context.getExternalFilesDir(null), "WClient/saved_skins")

            val playerSkinFolder = File(skinsSaveDir, targetUsername)
            if (!playerSkinFolder.exists()) {
                playerSkinFolder.mkdirs() // Создаем папку для игрока, если ее нет
            }

            // 1. Сохраняем текстуру скина (PNG)
            val skinTextureFile = File(playerSkinFolder, "${targetUsername}_skin.png")
            saveImageDataAsPng(targetSkin.getSkinData(), skinTextureFile.absolutePath)
            session.displayClientMessage("§a[SkinSaver] Текстура скина сохранена: §7${skinTextureFile.name}")

            // 2. Сохраняем текстуру плаща (PNG), если есть
            if (targetSkin.getCapeData() != null && targetSkin.getCapeData() != ImageData.EMPTY) {
                val capeTextureFile = File(playerSkinFolder, "${targetUsername}_cape.png")
                saveImageDataAsPng(targetSkin.getCapeData(), capeTextureFile.absolutePath)
                session.displayClientMessage("§a[SkinSaver] Текстура плаща сохранена: §7${capeTextureFile.name}")
            }

            // 3. Сохраняем данные геометрии (JSON)
            targetSkin.getGeometryData()?.let {
                val geometryFile = File(playerSkinFolder, "${targetUsername}_geometry.json")
                saveStringToFile(it, geometryFile.absolutePath)
                session.displayClientMessage("§a[SkinSaver] Геометрия скина сохранена: §7${geometryFile.name}")
            } ?: session.displayClientMessage("§c[SkinSaver] Геометрия скина не найдена для сохранения.")

            // 4. Сохраняем патч ресурсов скина (JSON)
            targetSkin.getSkinResourcePatch()?.let {
                val resourcePatchFile = File(playerSkinFolder, "${targetUsername}_resource_patch.json")
                saveStringToFile(it, resourcePatchFile.absolutePath)
                session.displayClientMessage("§a[SkinSaver] Патч ресурсов скина сохранен: §7${resourcePatchFile.name}")
            } ?: session.displayClientMessage("§c[SkinSaver] Патч ресурсов скина не найден для сохранения.")


            // 5. Сохраняем метаданные скина (как JSON) для полной информации
            val metadata = JsonObject().apply {
                addProperty("skinId", targetSkin.getSkinId())
                addProperty("playFabId", targetSkin.getPlayFabId())
                addProperty("geometryName", targetSkin.getGeometryName())
                addProperty("armSize", targetSkin.getArmSize())
                addProperty("skinColor", targetSkin.getSkinColor())
                addProperty("premium", targetSkin.isPremium())
                addProperty("persona", targetSkin.isPersona())
                addProperty("capeOnClassic", targetSkin.isCapeOnClassic())
                addProperty("primaryUser", targetSkin.isPrimaryUser())
                addProperty("capeId", targetSkin.getCapeId())
                addProperty("fullSkinId", targetSkin.getFullSkinId())

                // Сериализация списков (animations, personaPieces, tintColors)
                val gson = GsonBuilder().setPrettyPrinting().create() // Используем PrettyPrinting для удобства чтения JSON
                if (!targetSkin.getAnimations().isNullOrEmpty()) {
                    add("animations", gson.toJsonTree(targetSkin.getAnimations()))
                }
                if (!targetSkin.getPersonaPieces().isNullOrEmpty()) {
                    add("personaPieces", gson.toJsonTree(targetSkin.getPersonaPieces()))
                }
                if (!targetSkin.getTintColors().isNullOrEmpty()) {
                    add("tintColors", gson.toJsonTree(targetSkin.getTintColors()))
                }
            }
            val metadataFile = File(playerSkinFolder, "${targetUsername}_metadata.json")
            saveStringToFile(GsonBuilder().setPrettyPrinting().create().toJson(metadata), metadataFile.absolutePath)
            session.displayClientMessage("§a[SkinSaver] Метаданные скина сохранены: §7${metadataFile.name}")

            session.displayClientMessage("§a[SkinSaver] Скин игрока §b$targetUsername§a успешно сохранен в §7${playerSkinFolder.absolutePath}")

        } catch (e: Exception) {
            session.displayClientMessage("§c[SkinSaver] Ошибка при сохранении скина: ${e.message}")
            e.printStackTrace()
        }
    }
}
