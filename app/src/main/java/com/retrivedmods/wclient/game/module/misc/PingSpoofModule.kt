package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module        // Ваш базовый класс Module
import com.retrivedmods.wclient.game.ModuleCategory // Ваша категория ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.* // Импорт функции coerceAtLeast

// PingSpoofModule теперь имеет пустой конструктор.
// Если ваш базовый класс Module требует параметры в конструкторе,
// то этот код не скомпилируется. Но судя по вашей ошибке, это именно то,
// что нужно для ModuleManager.
class PingSpoofModule : Module("FakePing", ModuleCategory.Misc) {

    private val pingValue by intValue("Пинг (мс)", 300, 50..1000)
    private val jitter by intValue("Джиттер (мс)", 50, 0..200)
    private val tickInterval by intValue("Интервал тиков", 1, 1..20)

    private val pendingResponses = HashMap<Long, Long>()
    private val random = Random()

    override fun onEnabled() {
        super.onEnabled()
        pendingResponses.clear()
        session?.displayClientMessage("§a[FakePing] Включен. Целевой пинг: §b${pingValue}мс")
    }

    override fun onDisabled() {
        super.onDisabled()
        pendingResponses.clear()
        session?.displayClientMessage("§c[FakePing] Выключен.")
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        when (packet) {
            is NetworkStackLatencyPacket -> {
                handleLatencyPacket(interceptablePacket, packet)
            }
            is PlayerAuthInputPacket -> {
                if (packet.tick % tickInterval == 0L) {
                    processPendingResponses()
                }
            }
        }
    }

    private fun handleLatencyPacket(interceptablePacket: InterceptablePacket, packet: NetworkStackLatencyPacket) {
        interceptablePacket.intercept()

        val delay = calculateDelay()
        pendingResponses[packet.timestamp] = System.currentTimeMillis() + delay

        if (pendingResponses.size > 1000) {
            pendingResponses.clear()
            session?.displayClientMessage("§e[FakePing] Очищен буфер отложенных пинг-пакетов (переполнение).")
        }
    }

    private fun processPendingResponses() {
        val currentTime = System.currentTimeMillis()
        val readyPackets = pendingResponses.entries.filter { it.value <= currentTime }

        readyPackets.forEach { (serverTimestamp, _) ->
            session?.serverBound(NetworkStackLatencyPacket().apply {
                timestamp = serverTimestamp
            })
            pendingResponses.remove(serverTimestamp)
        }
    }

    private fun calculateDelay(): Long {
        val baseDelay = pingValue.toLong()
        val jitterOffset = if (jitter > 0) random.nextInt(jitter * 2) - jitter else 0
        return (baseDelay + jitterOffset).coerceAtLeast(0)
    }
}
