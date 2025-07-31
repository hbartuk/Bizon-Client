package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module        // Your base Module class
import com.retrivedmods.wclient.game.ModuleCategory // Your ModuleCategory enum
import com.retrivedmods.wclient.util.AssetManager   // Correct import for AssetManager
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.coerceAtLeast // Correct import for coerceAtLeast

// PingSpoofModule now correctly passes name and category to the base Module constructor,
// just like your AdvanceDisablerModule example.
class PingSpoofModule : Module("FakePing", ModuleCategory.Misc) {

    // User-configurable settings for ping spoofing
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
                // Since 'fromServer' is a private field in CloudburstMC's NetworkStackLatencyPacket,
                // we cannot access it directly. This block will handle all NetworkStackLatencyPacket types.
                // For a more precise ping spoof, your `InterceptablePacket` might need to provide
                // a property like `isIncoming` or `isOutgoing`.
                handleLatencyPacket(interceptablePacket, packet)
            }
            is PlayerAuthInputPacket -> {
                // We use PlayerAuthInputPacket as a "heartbeat" to process delayed responses.
                // We check 'packet.tick' to control the processing frequency.
                if (packet.tick % tickInterval == 0L) {
                    processPendingResponses()
                }
            }
        }
    }

    private fun handleLatencyPacket(interceptablePacket: InterceptablePacket, packet: NetworkStackLatencyPacket) {
        // Intercept the packet to prevent it from reaching the client immediately.
        interceptablePacket.intercept()

        val delay = calculateDelay()
        // Store the original packet's timestamp and the future time when we should "respond" to it.
        pendingResponses[packet.timestamp] = System.currentTimeMillis() + delay

        // Clear the cache if it gets too large to prevent memory overflow.
        if (pendingResponses.size > 1000) {
            pendingResponses.clear()
            session?.displayClientMessage("§e[FakePing] Очищен буфер отложенных пинг-пакетов (переполнение).")
        }
    }

    private fun processPendingResponses() {
        val currentTime = System.currentTimeMillis()
        // Filter out all packets whose response time has arrived.
        val readyPackets = pendingResponses.entries.filter { it.value <= currentTime }

        readyPackets.forEach { (serverTimestamp, _) ->
            // Send a new NetworkStackLatencyPacket to the server, simulating a client response.
            // The 'fromServer' field does not exist for packets sent TO the server, so it's removed.
            session?.serverBound(NetworkStackLatencyPacket().apply {
                timestamp = serverTimestamp
            })
            pendingResponses.remove(serverTimestamp)
        }
    }

    private fun calculateDelay(): Long {
        val baseDelay = pingValue.toLong()
        // Add a random offset (jitter) for more realistic simulation.
        val jitterOffset = if (jitter > 0) random.nextInt(jitter * 2) - jitter else 0
        // Return the calculated delay, ensuring it's not negative.
        return (baseDelay + jitterOffset).coerceAtLeast(0)
    }
}
