package com.retrivedmods.wclient.game.module.combat // Изменено на combat

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.InteractPacket
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket // Нужен для получения позиции игрока
import org.cloudburstmc.protocol.bedrock.data.entity.InteractAction // ИСПРАВЛЕНО: Добавлен импорт для InteractAction

// УДАЛЕНО: import org.cloudburstmc.protocol.bedrock.data.entity.EntityData (не используется)
// УДАЛЕНО: import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket (не используется)

class AutoClickerModule : Module("AutoClicker", ModuleCategory.Combat) {

    // Настройка КПС: от 1 до 5000 кликов в секунду
    private var clicksPerSecond by intValue("КПС", 10, 1..5000)
    // Максимальная дистанция, на которой будет работать кликер (обычно 3-4 блока для удара)
    private var attackRange by floatValue("Дистанция атаки", 4.0f, 1.0f..8.0f)

    private var lastAttackTime: Long = 0L

    // ИСПРАВЛЕНО: Убрано 'override', т.к. ваш базовый класс Module не помечает их как open/abstract
    fun onEnable() {
        session.displayClientMessage("§a[Bizon client] Авто-кликер включен. КПС: ${clicksPerSecond}") // ИСПРАВЛЕНО: убрано .value
    }

    // ИСПРАВЛЕНО: Убрано 'override'
    fun onDisable() {
        session.displayClientMessage("§c[Bizon client] Авто-кликер выключен.")
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        // Вычисляем необходимую задержку между ударами
        val minDelay = 1000L / clicksPerSecond // ИСПРАВЛЕНО: убрано .value

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

        // --- ВАЖНОЕ ЗАМЕЧАНИЕ: ЭТА ЧАСТЬ ТРЕБУЕТ АДАПТАЦИИ ПОД ТВОЙ КЛИЕНТ ---
        // Ищем ближайшую сущность (пока не ограничиваемся игроками, это можно добавить)
        // В реальном клиенте нужно итерироваться по списку сущностей, видимых клиенту.
        // Здесь предполагается, что session.entities содержит мапу сущностей (ID к объекту сущности)
        var targetEntityId: Long? = null
        val maxAttackRangeSq = attackRange * attackRange // ИСПРАВЛЕНО: убрано .value

        // **!!! ТУТ ТЕБЕ НУЖНО ЗАМЕНИТЬ session.entities НА РЕАЛЬНЫЙ СПОСОБ ПОЛУЧЕНИЯ СУЩНОСТЕЙ !!!**
        // Если у тебя есть Map<Long, YourEntityClass> entities в session:
        // session.entities.forEach { (entityId, entity) ->
        //     if (entityId != localPlayerEntityId && entity.vec3Position != null) { // Не бьем себя
        //         val distanceSq = localPlayerPos.distanceSquared(entity.vec3Position)
        //         if (distanceSq < maxAttackRangeSq) {
        //             // Можно добавить проверку, что это именно игрок, а не моб
        //             // if (entity.isPlayer) { // Если у тебя есть такой метод/флаг
        //                 targetEntityId = entityId
        //                 return@forEach // Нашли ближайшего, выходим из цикла
        //             // }
        //         }
        //     }
        // }
        // --- КОНЕЦ ВАЖНОГО ЗАМЕЧАНИЯ ---


        // ВРЕМЕННЫЙ ПЛЕЙСХОЛДЕР ДЛЯ ТЕСТИРОВАНИЯ (УДАЛИ ПОСЛЕ РЕАЛИЗАЦИИ ПОИСКА СУЩНОСТЕЙ):
        // Если у тебя нет списка сущностей, ты не сможешь автоматически выбирать цель.
        // Этот модуль *требует* способа найти runtimeEntityId цели.
        // Возможно, тебе нужно будет получить entityId из MovePlayerPacket, который ты обрабатывал ранее?
        // Но для PvP-кликера нужна активная цель.
        // Если у тебя есть способ получить ID сущности, на которую ты смотришь, используй его.
        // Например, если ты хочешь просто бить "что-нибудь", ты можешь выбрать ID какого-то известного моба/игрока для теста.
        // targetEntityId = 12345L // ЗАМЕНИ ЭТО НА РЕАЛЬНЫЙ ID ЦЕЛИ ИЛИ РЕАЛИЗУЙ ПОИСК ВЫШЕ


        // Если нашли цель в радиусе действия
        if (targetEntityId != null) {
            // Создаем пакет удара InteractPacket
            val attackPacket = InteractPacket().apply {
                this.runtimeEntityId = targetEntityId!! // ID цели
                this.action = InteractAction.ATTACK // ИСПРАВЛЕНО: Использование InteractAction
                // Другие поля могут понадобиться в зависимости от версии протокола
                // this.targetPosition = Vector3f.ZERO; // Не всегда нужно для атаки
                // this.hitPosition = Vector3f.ZERO; // Не всегда нужно для атаки
            }

            // Отправляем пакет на сервер
            session.muCuteRelaySession?.server?.sendPacket(attackPacket)
            lastAttackTime = currentTime // Обновляем время последнего удара
        }
    }
}
