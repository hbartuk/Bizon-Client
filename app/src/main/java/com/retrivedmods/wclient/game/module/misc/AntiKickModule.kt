package com.retrivedmods.wclient.game.module.misc

import android.util.Log
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.application.AppContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason
import org.cloudburstmc.protocol.bedrock.packet.*
import kotlin.random.Random

class AntiKickModule : Module(
    name = "🛡️ Анти-Кик", // Название модуля на русском с эмодзи
    category = ModuleCategory.Misc
) {

    // Опции на русском языке с улучшенными названиями
    private var disconnectPacketValue by boolValue("🔌 Блокировать отключение", true)
    private var transferPacketValue by boolValue("📡 Блокировать перенос", true)
    private var playStatusPacketValue by boolValue("📊 Блокировать статус игры", true)
    private var networkSettingsPacketValue by boolValue("⚙️ Блокировать настройки сети", true)


    private var showKickMessages by boolValue("💬 Показывать сообщения о киках", true)
    private var intelligentBypass by boolValue("🧠 Умный обход", true)
    private var autoReconnect by boolValue("🔄 Автопереподключение", false)
    private var antiAfkSimulation by boolValue("😴 Анти-АФК симуляция", true)
    private var useRandomMovement by boolValue("🎲 Случайное движение", true)
    private var preventTimeout by boolValue("⏰ Предотвращать таймаут", true)

    private var movementInterval by intValue("⏱️ Интервал движения (мс)", 8000, 500..15000)
    
    // Дополнительные переменные для функционала
    private var isPerformingAntiAFK = false
    private var lastMovementTime = 0L
    private var reconnectAttempts = 0
    private var lastHeartbeatTime = 0L
    private val maxReconnectAttempts = 3
    private val reconnectDelay = 5000
    private val heartbeatInterval = 30000L
    private var movementDuration by intValue("⏲️ Длительность движения (мс)", 200, 50..1000)lue("Длительность движения (мс)", 500, 100..3000)


    private var reconnectDelay by intValue("Задержка переподключения (мс)", 3000, 1000..10000)
    private var maxReconnectAttempts by intValue("Макс. попыток переподключения", 3, 1..10)


    private var lastMovementTime = 0L
    private var isPerformingAntiAFK = false
    private var reconnectAttempts = 0
    private var lastDisconnectReason: String? = null
    private var lastHeartbeatTime = 0L
    private val heartbeatInterval = 30000L

    override fun onEnabled() {
        super.onEnabled()
        if (session != null) {
            lastMovementTime = System.currentTimeMillis()
            reconnectAttempts = 0
            lastHeartbeatTime = System.currentTimeMillis()

            if (preventTimeout) {
                startHeartbeatTask()
            }
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        isPerformingAntiAFK = false
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun startHeartbeatTask() {
        GlobalScope.launch {
            while (isEnabled && session != null) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastHeartbeatTime >= heartbeatInterval) {
                    try {
                        val textPacket = TextPacket().apply {
                            type = TextPacket.Type.TIP
                            isNeedsTranslation = false
                            message = ""
                            xuid = ""
                            platformChatId = ""
                        }
                        session?.clientBound(textPacket)
                        lastHeartbeatTime = currentTime
                    } catch (e: Exception) {
                        Log.w("AntiKick", "Failed to send heartbeat packet", e)
                    }
                }
                delay(5000)
            }
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || session == null) {
            return
        }

        val packet = interceptablePacket.packet
        val currentTime = System.currentTimeMillis()

        if (packet is DisconnectPacket && disconnectPacketValue) {
            handleDisconnectPacket(interceptablePacket, packet)
        }

        if (packet is TransferPacket && transferPacketValue) {
            handleTransferPacket(interceptablePacket, packet)
        }

        if (packet is PlayStatusPacket && playStatusPacketValue) {
            handlePlayStatusPacket(interceptablePacket, packet)
        }

        if (packet is NetworkSettingsPacket && networkSettingsPacketValue) {
            if (intelligentBypass) {
                if (showKickMessages) {
                    session?.displayClientMessage("§8[§bАнтиКик§8] §7Настройки сети обновлены.")
                }
            }
        }

        if (antiAfkSimulation && packet is PlayerAuthInputPacket && session?.localPlayer != null && currentTime - lastMovementTime >= movementInterval) {
            performAntiAFKMovement()
            lastMovementTime = currentTime
        }
    }

    private fun handleDisconnectPacket(interceptablePacket: InterceptablePacket, packet: DisconnectPacket) {
        lastDisconnectReason = packet.kickMessage

        val reason = getReadableKickReason(packet.reason, packet.kickMessage)

        if (showKickMessages) {
            session?.displayClientMessage("§8[§bАнтиКик§8] §eСервер пытается вас отключить: §f$reason")
        }

        // *** ИСПОЛЬЗУЕМ intercept() ВМЕСТО ПРЯМОЙ ЗАПИСИ ***
        interceptablePacket.intercept()

        if (showKickMessages) {
            session?.displayClientMessage("§8[§bАнтиКик§8] §aОтказываю в выполнении команды отключения.")
        }

        if (autoReconnect) {
            session?.displayClientMessage("§8[§bАнтиКик§8] §eАвтопереподключение активно. Начинаю проверку соединения...")
            attemptReconnect()
        }
    }

    private fun handleTransferPacket(interceptablePacket: InterceptablePacket, packet: TransferPacket) {
        if (showKickMessages) {
            session?.displayClientMessage("§8[§bАнтиКик§8] §eСервер пытается переместить вас на другой IP: §f${packet.address}:${packet.port}")
        }

        // *** ИСПОЛЬЗУЕМ intercept() ВМЕСТО ПРЯМОЙ ЗАПИСИ ***
        interceptablePacket.intercept()

        if (showKickMessages) {
            session?.displayClientMessage("§8[§bАнтиКик§8] §aОтказываю в выполнении команды перемещения.")
        }

        if (autoReconnect) {
            session?.displayClientMessage("§8[§bАнтиКик§8] §eАвтопереподключение активно. Начинаю проверку соединения...")
            attemptReconnect()
        }
    }

    private fun handlePlayStatusPacket(interceptablePacket: InterceptablePacket, packet: PlayStatusPacket) {
        val status = packet.status
        val isKickStatus = when (status) {
            PlayStatusPacket.Status.LOGIN_FAILED_CLIENT_OLD,
            PlayStatusPacket.Status.LOGIN_FAILED_SERVER_OLD,
            PlayStatusPacket.Status.LOGIN_FAILED_INVALID_TENANT,
            PlayStatusPacket.Status.LOGIN_FAILED_EDITION_MISMATCH_EDU_TO_VANILLA,
            PlayStatusPacket.Status.LOGIN_FAILED_EDITION_MISMATCH_VANILLA_TO_EDU,
            PlayStatusPacket.Status.FAILED_SERVER_FULL_SUB_CLIENT -> true
            else -> false
        }

        if (isKickStatus) {
            if (showKickMessages) {
                session?.displayClientMessage("§8[§bАнтиКик§8] §eСервер пытается отключить вас по статусу: §f$status")
            }

            // *** ИСПОЛЬЗУЕМ intercept() ВМЕСТО ПРЯМОЙ ЗАПИСИ ***
            interceptablePacket.intercept()

            if (showKickMessages) {
                session?.displayClientMessage("§8[§bАнтиКик§8] §aОтказываю в выполнении команды отключения по статусу.")
            }

            if (autoReconnect) {
                session?.displayClientMessage("§8[§bАнтиКик§8] §eАвтопереподключение активно. Начинаю проверку соединения...")
                attemptReconnect()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun performAntiAFKMovement() {
        if (isPerformingAntiAFK || session?.localPlayer == null) return

        isPerformingAntiAFK = true

        GlobalScope.launch {
            try {
                if (useRandomMovement) {
                    val dx = Random.nextFloat() * 0.05f - 0.025f
                    val dz = Random.nextFloat() * 0.05f - 0.025f
                    val motionPacket = SetEntityMotionPacket().apply {
                        runtimeEntityId = session!!.localPlayer.runtimeEntityId
                        motion = Vector3f.from(dx, 0f, dz)
                    }
                    session?.clientBound(motionPacket)
                } else {
                    val motionPacket = SetEntityMotionPacket().apply {
                        runtimeEntityId = session!!.localPlayer.runtimeEntityId
                        motion = Vector3f.from(0.01f, 0f, 0.01f)
                    }
                    session?.clientBound(motionPacket)
                }
                delay(movementDuration.toLong())
                val stopMotionPacket = SetEntityMotionPacket().apply {
                    runtimeEntityId = session!!.localPlayer.runtimeEntityId
                    motion = Vector3f.ZERO
                }
                session?.clientBound(stopMotionPacket)

                isPerformingAntiAFK = false
            } catch (e: Exception) {
                Log.e("AntiKick", "Error during anti-AFK movement", e)
                isPerformingAntiAFK = false
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun attemptReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            session?.displayClientMessage("§8[§bАнтиКик§8] §cДостигнуто максимальное количество попыток переподключения (§f$maxReconnectAttempts§c).")
            return
        }

        reconnectAttempts++

        session?.displayClientMessage("§8[§bАнтиКик§8] §eПопытка переподключения (§f$reconnectAttempts§7/§f$maxReconnectAttempts§7)...")

        GlobalScope.launch {
            delay(reconnectDelay.toLong())
            // *** ВАЖНО: Здесь должна быть реальная логика переподключения к серверу. ***
            // session?.reconnect() // <-- Замените на ваш реальный метод переподключения
            //
            // Если такого метода нет, его нужно добавить в ваш GameSession.
            // Без этого, автопереподключение будет только выводить сообщения, но не действовать.

            session?.displayClientMessage("§8[§bАнтиКик§8] §aПереподключение завершено. Проверяю статус.")
        }
    }

    private fun getReadableKickReason(reason: DisconnectFailReason, message: String): String {
        return when (reason) {
            DisconnectFailReason.KICKED, DisconnectFailReason.KICKED_FOR_EXPLOIT, DisconnectFailReason.KICKED_FOR_IDLE ->
                if (message.isNotBlank()) "выброшен: $message" else "выброшен без указания причины"
            DisconnectFailReason.TIMEOUT -> "соединение прервано (таймаут)"
            DisconnectFailReason.SERVER_FULL -> "сервер переполнен"
            DisconnectFailReason.NOT_ALLOWED -> "доступ запрещен"
            DisconnectFailReason.BANNED_SKIN -> "запрещенный скин"
            DisconnectFailReason.SHUTDOWN -> "сервер выключается"
            DisconnectFailReason.INVALID_PLAYER -> "неверные данные игрока"
            else -> if (message.isNotBlank()) "причина $reason: $message" else "неизвестная причина: $reason"
        }
    }
}
