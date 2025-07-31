// File: com.retrivedmods.wclient.game.module.misc.PingSpoofModule.kt
package com.retrivedmods.wclient.game.module.misc // Обратите внимание на путь

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module // <-- Изменено на Module
import com.retrivedmods.wclient.game.ModuleCategory // <-- Изменено на ModuleCategory
import com.retrivedmods.wclient.constructors.IntValue // Убедитесь, что этот импорт есть и корректен
import com.retrivedmods.wclient.util.AssetManager
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.coerceAtLeast

// PingSpoofModule теперь наследуется от Module
class PingSpoofModule(iconResId: Int = AssetManager.getAsset("ic_timer_sand_black_24dp")) : Module(
    name = "FakePing", // Передаем имя в конструктор Module
    category = ModuleCategory.Misc, // Передаем категорию в конструктор Module
    iconResId = iconResId,
    displayNameResId = AssetManager.getString("module_fakeping_display_name") // Убедитесь, что эта строка существует в ваших ресурсах
) {
    // Пользовательские настройки пинга
    private val pingValue by intValue("Пинг (мс)", 300, 50..1000)
    private val jitter by intValue("Джиттер (мс)", 50, 0..200)
    private val tickInterval by intValue("Интервал тиков", 1, 1..20)

    // Временные метки пакетов, ожидающих ответа
    private val pendingResponses = HashMap<Long, Long>()
    private val random = Random()

    override fun onEnabled() {
        super.onEnabled()
        pendingResponses.clear()
        // session не null, т.к. инициализируется в ModuleManager.initialize
        session?.displayClientMessage("§a[FakePing] Включен. Целевой пинг: §b${pingValue}мс")
    }

    override fun onDisabled() {
        super.onDisabled()
        pendingResponses.clear()
        session?.displayClientMessage("§c[FakePing] Выключен.")
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return // Проверяем, включен ли модуль

        val packet = interceptablePacket.packet

        when {
            // Если это пакет NetworkStackLatencyPacket от сервера
            packet is NetworkStackLatencyPacket && packet.fromServer -> {
                handleLatencyPacket(interceptablePacket, packet)
            }
            // Используем PlayerAuthInputPacket как "пульс" для обработки отложенных ответов
            // Проверяем packet.tick, чтобы не обрабатывать слишком часто
            packet is PlayerAuthInputPacket && packet.tick % tickInterval == 0L -> {
                processPendingResponses()
            }
        }
    }

    private fun handleLatencyPacket(interceptablePacket: InterceptablePacket, packet: NetworkStackLatencyPacket) {
        // Останавливаем пакет, чтобы он не дошел до клиента сразу
        interceptablePacket.intercept()

        val delay = calculateDelay()
        // Сохраняем временную метку пакета и будущее время, когда мы должны "ответить" на него
        pendingResponses[packet.timestamp] = System.currentTimeMillis() + delay

        // Очищаем кэш, если он становится слишком большим, чтобы избежать утечек памяти
        if (pendingResponses.size > 1000) { // Можно настроить это значение
            pendingResponses.clear()
            session?.displayClientMessage("§e[FakePing] Очищен буфер отложенных пинг-пакетов (переполнение).")
        }
    }

    private fun processPendingResponses() {
        val currentTime = System.currentTimeMillis()
        // Отбираем все пакеты, которые "созрели" для ответа
        val readyPackets = pendingResponses.entries.filter { it.value <= currentTime }

        readyPackets.forEach { (serverTimestamp, _) ->
            // Отправляем новый NetworkStackLatencyPacket на сервер, имитируя ответ клиента
            // timestamp должен быть в миллисекундах для CloudburstMC Protocol Bedrock
            session?.serverBound(NetworkStackLatencyPacket().apply {
                timestamp = serverTimestamp
                fromServer = true // Важно: это ответ на пакет ОТ СЕРВЕРА
            })
            // Удаляем обработанный пакет из списка
            pendingResponses.remove(serverTimestamp)
        }
    }

    private fun calculateDelay(): Long {
        val baseDelay = pingValue.toLong()
        // Добавляем случайное смещение (джиттер) для реалистичности
        val jitterOffset = if (jitter > 0) random.nextInt(jitter * 2) - jitter else 0
        // Возвращаем вычисленную задержку, гарантируя, что она не отрицательная
        return (baseDelay + jitterOffset).coerceAtLeast(0)
    }
}
