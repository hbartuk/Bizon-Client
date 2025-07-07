package com.retrivedmods.wclient.game.module.combat // Изменено на combat

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag
import org.cloudburstmc.protocol.bedrock.packet.InteractPacket
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket // Нужен для получения позиции игрока
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket // Может понадобиться для работы с сущностями
import org.cloudburstmc.protocol.bedrock.data.entity.EntityData // Может понадобиться для работы с сущностями

// Дополнительный импорт для InteractPacket.Action (если его нет)
import org.cloudburstmc.protocol.bedrock.data.entity.InteractAction

class AutoClickerModule : Module("AutoClicker", ModuleCategory.Combat) {

    // Настройка КПС: от 1 до 5000 кликов в секунду
    private var clicksPerSecond by intValue("КПС", 10, 1..5000)

    // Максимальная дистанция, на которой будет работать кликер (обычно 3-4 блока для удара)
    private var attackRange by floatValue("Дистанция атаки", 4.0f, 1.0f..8.0f)

    private var lastAttackTime: Long = 0L

    override fun onEnable() {
        session.displayClientMessage("§a[WClient] Авто-кликер включен. КПС: ${clicksPerSecond.value}")
    }

    override fun onDisable() {
        session.displayClientMessage("§c[WClient] Авто-кликер выключен.")
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        // Вычисляем необходимую задержку между ударами
        val minDelay = 1000L / clicksPerSecond.value // Задержка в миллисекундах

        val currentTime = System.currentTimeMillis()

        // Если с момента последнего удара прошло недостаточно времени, ждем
        if (currentTime - lastAttackTime < minDelay) {
            return
        }

        // Получаем позицию и ID нашего игрока
        val localPlayerPos = session.localPlayer.vec3Position
        val localPlayerEntityId = session.localPlayer.runtimeEntityId

        if (localPlayerPos == null || localPlayerEntityId == 0L) {
            return
        }

        // Ищем ближайшую сущность (пока не ограничиваемся игроками, это можно добавить)
        // В реальном клиенте нужно итерироваться по списку сущностей, видимых клиенту.
        // Здесь предполагается, что session.entities содержит мапу сущностей (ID к объекту сущности)
        var targetEntityId: Long? = null
        var minDistanceSq = attackRange * attackRange

        // Примерный поиск ближайшего игрока/сущности.
        // Тебе нужно будет заменить это на реальный способ получения списка сущностей
        // и их позиций из твоей `session`.
        // Например, если у тебя есть map<Long, Entity> session.entities
        session.entities.forEach { (entityId, entity) ->
            if (entityId != localPlayerEntityId && entity.vec3Position != null) { // Не бьем себя
                val distanceSq = localPlayerPos.distanceSquared(entity.vec3Position)
                if (distanceSq < minDistanceSq) {
                    // Можно добавить проверку, что это именно игрок, а не моб
                    // if (entity.isPlayer) { // Если у тебя есть такой метод/флаг
                        minDistanceSq = distanceSq
                        targetEntityId = entityId
                    // }
                }
            }
        }


        // Если нашли цель в радиусе действия
        if (targetEntityId != null) {
            // Создаем пакет удара InteractPacket
            val attackPacket = InteractPacket().apply {
                this.runtimeEntityId = targetEntityId!! // ID цели
                this.action = InteractAction.ATTACK // Действие - атака
                // Other fields might be needed depending on your protocol version
                // this.targetPosition = Vector3f.ZERO; // Not always needed for attack action
                // this.hitPosition = Vector3f.ZERO; // Not always needed for attack action
            }

            // Отправляем пакет на сервер
            session.muCuteRelaySession?.server?.sendPacket(attackPacket)
            lastAttackTime = currentTime // Обновляем время последнего удара
        }
    }
}
