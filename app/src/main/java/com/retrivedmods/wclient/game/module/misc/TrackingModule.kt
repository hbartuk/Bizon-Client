// File: com.retrivedmods.wclient.game.module.visual.TrackingModule.kt (или в другом подходящем месте)
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
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap // Возможно, потребуется для метаданных
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes // Для типов метаданных
import java.util.UUID

class TrackingModule : Module("tracking", ModuleCategory.Visual) {

    private var targetPlayerName: String? = null
    private var targetPlayerRuntimeId: Long = -1L
    private var originalPlayerPos: Vector3f? = null

    // Метод для установки цели из команды .sled <ник>
    fun setTarget(session: GameSession, playerName: String) {
        // Находим игрока по нику. Вам нужно будет добавить метод в GameSession.level
        // для поиска игрока по имени, например: session.level.findPlayerByName(playerName)
        val foundPlayer = session.level.entityMap.values.firstOrNull { it is Player && it.username.equals(playerName, ignoreCase = true) } as? Player

        if (foundPlayer == null) {
            session.displayClientMessage("§c[Слежка] Игрок '$playerName' не найден.")
            isEnabled = false // Отключаем модуль, если цель не найдена
            return
        }

        targetPlayerName = playerName
        targetPlayerRuntimeId = foundPlayer.runtimeEntityId
        originalPlayerPos = session.localPlayer.vec3Position // Сохраняем позицию нашего игрока

        session.displayClientMessage("§a[Слежка] Начата слежка за игроком: §b$playerName")
        isEnabled = true // Включаем модуль
    }

    override fun onDisabled() {
        super.onDisabled()
        // Возвращаем нашего игрока на исходную позицию
        if (session.isSessionCreated && originalPlayerPos != null) {
            val movePlayerPacket = MovePlayerPacket().apply {
                runtimeEntityId = session.localPlayer.runtimeEntityId
                position = originalPlayerPos!!
                rotation = session.localPlayer.vec3Rotation // Или сохраните исходный поворот
                mode = MovePlayerPacket.Mode.NORMAL
                isOnGround = false
                tick = session.localPlayer.tickExists
            }
            session.clientBound(movePlayerPacket)
            originalPlayerPos = null
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
        if (packet is MovePlayerPacket && packet.runtimeEntityId == targetPlayerRuntimeId) {
            // Мы перехватили пакет движения целевого игрока
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
        if (packet is AddPlayerPacket && packet.runtimeEntityId == targetPlayerRuntimeId) {
            modifyPlayerAppearanceMetadata(packet.metadata)
            // Важно: не перехватываем пакет, чтобы игрок всё равно добавился в мир, но с измененным внешним видом
        } else if (packet is SetEntityDataPacket && packet.runtimeEntityId == targetPlayerRuntimeId) {
            modifyPlayerAppearanceMetadata(packet.entityData)
            // Важно: не перехватываем пакет
        }
    }

    private fun modifyPlayerAppearanceMetadata(metadata: EntityDataMap) {
        // --- Логика для скрытия брони ---
        // Эти ключи могут меняться между версиями Bedrock Protocol.
        // Нужно найти точные EntityDataTypes для слотов брони.
        // Например, EntityDataTypes.ARMOR_STAND_POSE_INDEX (если используется для игроков)
        // или прямые ID для слотов. Предположим, что они есть и могут быть обнулены.
        // Возможно, есть EntityDataTypes.ARMOR_CONTENTS или похожие.
        // Пример (псевдокод, нужно найти актуальные типы):
        // metadata.remove(EntityDataTypes.HELMET)
        // metadata.remove(EntityDataTypes.CHESTPLATE)
        // metadata.remove(EntityDataTypes.LEGGINGS)
        // metadata.remove(EntityDataTypes.BOOTS)
        // Или установка их в Item.EMPTY_ITEM

        // --- Логика для скрытия скина ---
        // Это более сложная часть. Нет прямого "скрыть скин" флага.
        // Возможные варианты (требуется эксперимент и знание протокола):
        // 1. Установка флага невидимости (если применимо к игрокам через метаданные, что редко).
        //    Например: metadata.setByte(EntityDataTypes.FLAGS, (metadata.getByte(EntityDataTypes.FLAGS) ?: 0).or(EntityFlag.INVISIBLE.ordinal.toByte()))
        // 2. Изменение флагов отображения частей скина (рубашка, рукава и т.д.):
        //    metadata.setByte(EntityDataTypes.PLAYER_FLAGS, 0) // Отключить все флаги кастомизации
        // 3. Более радикальный вариант - манипуляция с UUID или именем, чтобы клиент не ассоциировал его со скином,
        //    но это может вызвать проблемы с отображением имени или другие баги.
        // 4. Наиболее вероятный способ: изменение флага видимости сущности или её компонента рендеринга.
        //    В некоторых версиях протокола может быть что-то вроде EntityDataTypes.SCALE для уменьшения до нуля,
        //    или специфический флаг для "невидимости" сущности.
        // Пример:
        // metadata.setByte(EntityDataTypes.AFFECTED_BY_GRAVITY, 0) // Может помочь с некоторыми визуальными багами, но не скрывает
        // metadata.setBoolean(EntityDataTypes.NO_AI, true) // Невидимсоть, но может быть не применимо к игрокам

        // Поскольку у меня нет точных EntityDataTypes и их эффектов для скрытия скина/брони в Bedrock,
        // вам потребуется обратиться к документации CloudburstMC Protocol Bedrock
        // или к реверс-инжинирингу для нахождения точных полей и их значений.
        // Например, ищите флаги в EntityDataTypes.FLAGS или EntityDataTypes.PLAYER_FLAGS.
    }
}
