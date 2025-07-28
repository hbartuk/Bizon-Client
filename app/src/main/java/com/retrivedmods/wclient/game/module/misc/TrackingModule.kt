// File: com.retrivedmods.wclient.game.module.visual.TrackingModule.kt (или другое подходящее расположение)
package com.retrivedmods.wclient.game.module.visual

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.entity.LocalPlayer
import com.retrivedmods.wclient.game.entity.Player // Для доступа к другим игрокам
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket // Для скрытия скина при появлении
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket // Для скрытия скина/брони при обновлении
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap // <--- ИСПРАВЛЕНИЕ: Убедитесь, что этот импорт есть
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes // Для типов метаданных
import java.util.UUID // Может быть, уже импортировано

class TrackingModule : Module("tracking", ModuleCategory.Visual) {

    private var targetPlayerName: String? = null
    private var targetPlayerRuntimeId: Long = -1L
    private var originalPlayerPos: Vector3f? = null
    private var originalPlayerRotation: Vector3f? = null // Для сохранения исходного поворота

    // Метод для установки цели из команды .sled <ник>
    fun setTarget(session: GameSession, playerName: String) {
        val foundPlayer = session.level.entityMap.values.firstOrNull { it is Player && it.username.equals(playerName, ignoreCase = true) } as? Player

        if (foundPlayer == null) {
            session.displayClientMessage("§c[Слежка] Игрок '$playerName' не найден.")
            isEnabled = false // Отключаем модуль, если цель не найдена
            return
        }

        targetPlayerName = playerName
        targetPlayerRuntimeId = foundPlayer.runtimeEntityId
        originalPlayerPos = session.localPlayer.vec3Position // Сохраняем позицию нашего игрока
        originalPlayerRotation = session.localPlayer.vec3Rotation // Сохраняем поворот нашего игрока

        session.displayClientMessage("§a[Слежка] Начата слежка за игроком: §b$playerName")
        isEnabled = true // Включаем модуль
    }

    override fun onDisabled() {
        super.onDisabled()
        // ИСПРАВЛЕНИЕ: Удалили 'isSessionCreated' и полагаемся на originalPlayerPos != null
        if (originalPlayerPos != null) {
            val movePlayerPacket = MovePlayerPacket().apply {
                runtimeEntityId = session.localPlayer.runtimeEntityId
                position = originalPlayerPos!!
                rotation = originalPlayerRotation ?: Vector3f.ZERO // Восстанавливаем исходный поворот
                mode = MovePlayerPacket.Mode.NORMAL
                isOnGround = false
                tick = session.localPlayer.tickExists
            }
            session.clientBound(movePlayerPacket)
            originalPlayerPos = null
            originalPlayerRotation = null
        }
        targetPlayerName = null
        targetPlayerRuntimeId = -1L
        session.displayClientMessage("§a[Слежка] Слежка остановлена.")
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || targetPlayerRuntimeId == -1L) return

        val packet = interceptablePacket.packet

        // 1. Делаем наш аккаунт AFK (перехватываем наш собственный ввод)
        if (packet is PlayerAuthInputPacket) {
            interceptablePacket.intercept() // Останавливаем наш ввод от отправки на сервер
            return // Важно: после перехвата, дальше пакет не обрабатываем
        }

        // 2. Синхронизация камеры (движение и поворот)
        // Перехватываем MovePlayerPacket, предназначенный для ЦЕЛЕВОГО игрока
        if (packet is MovePlayerPacket && packet.runtimeEntityId == targetPlayerRuntimeId) {
            // Теперь создаем точно такой же пакет для НАШЕГО LocalPlayer
            val newMovePacketForLocalPlayer = MovePlayerPacket().apply {
                runtimeEntityId = session.localPlayer.runtimeEntityId // ID нашего игрока
                position = packet.position // Позиция целевого игрока
                rotation = packet.rotation // Поворот целевого игрока
                mode = MovePlayerPacket.Mode.NORMAL
                isOnGround = packet.isOnGround // Сохраняем статус земли
                tick = session.localPlayer.tickExists // Используем наш текущий тик
            }
            session.clientBound(newMovePacketForLocalPlayer) // Отправляем его нашему клиенту
            // Важно: не перехватываем оригинальный пакет, чтобы он дошел до других обработчиков и обновил позицию целевого игрока
            return // Пакет обработан, далее не идем.
        }

        // 3. Скрытие скина и брони целевого игрока для нашего клиента
        // Мы модифицируем пакет, чтобы наш клиент видел его измененным.
        // Не перехватываем пакет, чтобы он все равно обновил данные о сущности.
        if (packet is AddPlayerPacket && packet.runtimeEntityId == targetPlayerRuntimeId) {
            modifyPlayerAppearanceMetadata(packet.metadata)
        } else if (packet is SetEntityDataPacket && packet.runtimeEntityId == targetPlayerRuntimeId) {
            modifyPlayerAppearanceMetadata(packet.entityData) // <--- ИСПРАВЛЕНИЕ: Теперь 'entityData' должно быть доступно
        }
    }

    /**
     * Эта функция модифицирует метаданные сущности, чтобы скрыть её броню и скин.
     * ЭТО НАИБОЛЕЕ СЛОЖНАЯ ЧАСТЬ, ТРЕБУЮЩАЯ ТОЧНОГО ЗНАНИЯ ПРОТОКОЛА BEDROCK.
     * Вам нужно будет найти точные EntityDataTypes и их значения для вашей версии протокола.
     */
    private fun modifyPlayerAppearanceMetadata(metadata: EntityDataMap) {
        // --- Логика для скрытия брони ---
        // Пример (псевдокод, точные EntityDataTypes и их использование могут отличаться):
        // Протокол Bedrock использует EntityDataTypes.ARMOR для хранения массива данных о броне.
        // Или отдельные EntityDataTypes для каждого слота.
        // Нужно обнулить или удалить значения, связанные с броней.
        // Например:
        // metadata.put(EntityDataTypes.ARMOR, EntityDataMap.EMPTY_ARRAY_BYTE); // Если это массив байтов
        // Или если это отдельные слоты (примерно):
        // metadata.put(EntityDataTypes.HELMET, null); // Установить в null или Item.EMPTY_ITEM
        // metadata.put(EntityDataTypes.CHESTPLATE, null);
        // metadata.put(EntityDataTypes.LEGGINGS, null);
        // metadata.put(EntityDataTypes.BOOTS, null);

        // --- Логика для скрытия скина ---
        // Это более сложная часть. Нет прямого "скрыть скин" флага.
        // Возможные варианты (требуется эксперимент и знание протокола):
        // 1. Манипуляция EntityDataTypes.FLAGS: некоторые флаги могут влиять на видимость.
        //    Например, EntityFlag.INVISIBLE.ordinal.toByte() может быть флагом, но обычно это для эффектов.
        //    metadata.putByte(EntityDataTypes.FLAGS, (metadata.getByte(EntityDataTypes.FLAGS) ?: 0).or( /* ваш бит невидимости */ ))
        // 2. Изменение EntityDataTypes.PLAYER_FLAGS: управляет отображением частей скина (рукавов, плащей и т.д.).
        //    Установка этого флага в 0x00 может скрыть все части кастомизации.
        //    metadata.setByte(EntityDataTypes.PLAYER_FLAGS, 0x00)
        // 3. Изменение EntityDataTypes.SCALE: установка scale в 0.0f может сделать сущность невидимой.
        //    metadata.putFloat(EntityDataTypes.SCALE, 0.0f) // Это может сработать, но игрок будет как бы "существовать"
        // 4. Если есть, удаление или изменение EntityDataTypes.SKIN_ID / EntityDataTypes.TEXTURE_ARRAY / EntityDataTypes.TEXTURE_DATA
        //    на пустые или дефолтные значения.

        // Без точной документации для вашей версии Bedrock Protocol и EntityDataTypes,
        // эта часть остается местом для ваших экспериментов и исследований.
        // Начните с EntityDataTypes.PLAYER_FLAGS (попробуйте установить в 0)
        // и поиска флагов, связанных с броней, в EntityDataTypes.ARMOR или отдельных слотах.
    }
}
