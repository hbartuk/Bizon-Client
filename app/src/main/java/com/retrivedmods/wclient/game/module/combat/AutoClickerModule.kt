package com.retrivedmods.wclient.game.module.combat

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.entity.* // Импортируем все сущности из твоего пакета
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket // Нам нужен этот пакет для обработки тиков

// В этом файле НЕТ импорта InteractAction, InteractPacket и т.д.

class AutoClickerModule : Module("AutoClicker", ModuleCategory.Combat) {

    // Настройка КПС: от 1 до 5000 кликов в секунду
    private var clicksPerSecond by intValue("КПС", 10, 1..500000)
    // Максимальная дистанция, на которой будет работать кликер
    private var attackRange by floatValue("Дистанция атаки", 4.0f, 1.0f..800.0f)

    private var lastAttackTime: Long = 0L

    fun onEnable() {
        session.displayClientMessage("§a[WClient] Авто-кликер включен. КПС: ${clicksPerSecond}")
    }

    fun onDisable() {
        session.displayClientMessage("§c[WClient] Авто-кликер выключен.")
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        // Мы будем запускать логику атаки каждый раз, когда получаем пакет ввода игрока,
        // но с учетом задержки по КПС.
        val packet = interceptablePacket.packet
        if (packet !is PlayerAuthInputPacket) {
            return // Нам нужен именно PlayerAuthInputPacket для синхронизации
        }

        val minDelay = 1000L / clicksPerSecond
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastAttackTime < minDelay) {
            return // Ещё не время для следующего удара
        }

        val localPlayerPos = session.localPlayer.vec3Position
        val localPlayerEntityId = session.localPlayer.runtimeEntityId

        if (localPlayerPos == null || localPlayerEntityId == 0L) {
            return
        }

        var targetEntity: Entity? = null
        val maxAttackRangeSq = attackRange * attackRange

        // Поиск ближайшей цели. Используем entityMap из Killaura.
        for (entity in session.level.entityMap.values) {
            // Исключаем себя и сущности без позиции
            if (entity.runtimeEntityId == localPlayerEntityId || entity.vec3Position == null) {
                continue
            }

            // Проверяем дистанцию
            val distanceSq = localPlayerPos.distanceSquared(entity.vec3Position)
            if (distanceSq < maxAttackRangeSq) {
                // Здесь ты можешь добавить логику isTarget() из Killaura,
                // чтобы фильтровать только игроков или только мобов.
                // Например:
                // if (entity is Player && playersOnly.value) { ... }
                // if (entity is EntityUnknown && mobsOnly.value) { ... }
                targetEntity = entity
                break // Нашли ближайшего, выходим из цикла
            }
        }

        // Если нашли цель в радиусе действия
        if (targetEntity != null) {
            // Используем метод атаки из LocalPlayer, как в твоей Killaura
            session.localPlayer.attack(targetEntity!!)
            lastAttackTime = currentTime // Обновляем время последнего удара
        }
    }
}
