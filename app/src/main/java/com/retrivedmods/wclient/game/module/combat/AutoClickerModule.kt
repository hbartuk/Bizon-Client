package com.retrivedmods.wclient.game.module.combat // Убедитесь, что это combat

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.entity.* // Импортируем все сущности из твоего пакета
import org.cloudburstmc.math.vector.Vector3f
// УДАЛЕНО: import org.cloudburstmc.protocol.bedrock.data.entity.InteractAction (больше не нужен)
// УДАЛЕНО: import org.cloudburstmc.protocol.bedrock.packet.InteractPacket (больше не нужен)
// УДАЛЕНО: import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket (если не используется напрямую)
// УДАЛЕНО: import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket (если не используется)
// УДАЛЕНО: import org.cloudburstmc.protocol.bedrock.data.entity.EntityData (если не используется)

class AutoClickerModule : Module("AutoClicker", ModuleCategory.Combat) {

    // Настройка КПС: от 1 до 5000 кликов в секунду
    private var clicksPerSecond by intValue("КПС", 10, 1..5000)
    // Максимальная дистанция, на которой будет работать кликер (обычно 3-4 блока для удара)
    private var attackRange by floatValue("Дистанция атаки", 4.0f, 1.0f..8.0f)

    private var lastAttackTime: Long = 0L

    // onEnable и onDisable без 'override', как в твоем KillauraModule
    fun onEnable() {
        session.displayClientMessage("§a[WClient] Авто-кликер включен. КПС: ${clicksPerSecond}")
    }

    fun onDisable() {
        session.displayClientMessage("§c[WClient] Авто-кликер выключен.")
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        // Вычисляем необходимую задержку между ударами
        val minDelay = 1000L / clicksPerSecond

        val currentTime = System.currentTimeMillis()

        // Если с момента последнего удара прошло недостаточно времени, ждем
        if (currentTime - lastAttackTime < minDelay) {
            return
        }

        // Получаем позицию нашего игрока
        val localPlayerPos = session.localPlayer.vec3Position
        val localPlayerEntityId = session.localPlayer.runtimeEntityId

        if (localPlayerPos == null || localPlayerEntityId == 0L) {
            return
        }

        var targetEntity: Entity? = null
        val maxAttackRangeSq = attackRange * attackRange

        // Ищем ближайшую цель (игрока или моба, в зависимости от твоих настроек и фильтров)
        // Используем логику Killaura, предполагая, что session.level.entityMap доступна
        // и Entity.distance работает
        for (entity in session.level.entityMap.values) {
            // Исключаем себя и сущности без позиции
            if (entity.runtimeEntityId == localPlayerEntityId || entity.vec3Position == null) {
                continue
            }

            // Проверяем дистанцию до цели
            val distanceSq = localPlayerPos.distanceSquared(entity.vec3Position)
            if (distanceSq < maxAttackRangeSq) {
                // Здесь ты можешь добавить дополнительные фильтры, как в Killaura.isTarget()
                // Например, только игроки, только мобы и т.д.
                // Для простоты, пока бьем любую сущность в радиусе.
                // Если нужна именно логика Killaura.isTarget(), добавь её сюда:
                // if (entity.isTarget()) { // Если у тебя есть такой метод расширения для Entity
                    targetEntity = entity
                    break // Нашли ближайшую цель, выходим из цикла
                // }
            }
        }

        // Если нашли цель в радиусе действия
        if (targetEntity != null) {
            // Используем метод attack из LocalPlayer, как в твоей Killaura
            session.localPlayer.attack(targetEntity!!)
            lastAttackTime = currentTime // Обновляем время последнего удара
        }
    }
}
