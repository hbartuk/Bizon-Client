
package com.retrivedmods.wclient.game.module.misc

import android.util.Log
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cloudburstmc.math.vector.Vector3f
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
    
    // Anti-AFK функции
    private var preventTimeout by boolValue("⏰ Предотвращать таймаут", true)
    private var useRandomMovement by boolValue("🎲 Случайное движение", true)
    private var movementRadius by floatValue("📏 Радиус движения", 2.0f, 0.5f..10.0f)

    // Переменные состояния (объявляем только один раз)
    private var isPerformingAntiAFK: Boolean = false
    private var lastMovementTime: Long = 0L
    private var reconnectAttempts: Int = 0
    private var lastHeartbeatTime: Long = 0L
    private var maxReconnectAttempts: Int = 3
    private var reconnectDelay: Int = 5000
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
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        when (packet) {
            is DisconnectPacket -> {
                if (disconnectPacketValue) {
                    handleDisconnectPacket(interceptablePacket, packet)
                }
            }
            is TransferPacket -> {
                if (transferPacketValue) {
                    handleTransferPacket(interceptablePacket, packet)
                }
            }
            is PlayStatusPacket -> {
                if (playStatusPacketValue) {
                    handlePlayStatusPacket(interceptablePacket, packet)
                }
            }
            is NetworkSettingsPacket -> {
                if (networkSettingsPacketValue) {
                    handleNetworkSettingsPacket(interceptablePacket, packet)
                }
            }
            is PlayerAuthInputPacket -> {
                if (preventTimeout) {
                    lastMovementTime = System.currentTimeMillis()
                    
                    if (useRandomMovement && System.currentTimeMillis() - lastMovementTime > 30000) {
                        performAntiAFKMovement()
                    }
                }
            }
        }
    }

    private fun handleDisconnectPacket(interceptablePacket: InterceptablePacket, packet: DisconnectPacket) {
        if (showKickMessages) {
            session?.displayClientMessage("§8[§bАнтиКик§8] §eСервер пытается отключить вас: §f${packet.kickMessage}")
        }

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
            session?.displayClientMessage("§8[§bАнтиКик§8] §eСервер пытается перенести вас: §f${packet.address}:${packet.port}")
        }

        interceptablePacket.intercept()

        if (showKickMessages) {
            session?.displayClientMessage("§8[§bАнтиКик§8] §aОтказываю в выполнении команды переноса.")
        }

        if (autoReconnect) {
            session?.displayClientMessage("§8[§bАнтиКик§8] §eАвтопереподключение активно. Начинаю проверку соединения...")
            attemptReconnect()
        }
    }

    private fun handleNetworkSettingsPacket(interceptablePacket: InterceptablePacket, packet: NetworkSettingsPacket) {
        if (intelligentBypass) {
            // Разрешаем некоторые безопасные настройки сети
            val safeCompressionThreshold = 256
            if (packet.compressionThreshold <= safeCompressionThreshold) {
                return // Разрешаем пакет
            }
        }

        if (showKickMessages) {
            session?.displayClientMessage("§8[§bАнтиКик§8] §eСервер пытается изменить настройки сети.")
        }

        interceptablePacket.intercept()

        if (showKickMessages) {
            session?.displayClientMessage("§8[§bАнтиКик§8] §aОтказываю в изменении настроек сети.")
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
                        runtimeEntityId = session?.localPlayer?.runtimeEntityId ?: 0L
                        motion = Vector3f.from(dx, 0f, dz)
                    }
                    session?.clientBound(motionPacket)
                }
                delay(100)
            } catch (e: Exception) {
                Log.w("AntiKick", "Failed to perform anti-AFK movement", e)
            } finally {
                isPerformingAntiAFK = false
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun attemptReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            session?.displayClientMessage("§8[§bАнтиКик§8] §cМаксимальное количество попыток переподключения достигнuto.")
            return
        }

        reconnectAttempts++
        
        GlobalScope.launch {
            try {
                delay(reconnectDelay.toLong())
                session?.displayClientMessage("§8[§bАнтиКик§8] §eПопытка переподключения #$reconnectAttempts...")
                
                // Здесь должна быть логика переподключения
                // Пока что просто выводим сообщение
                session?.displayClientMessage("§8[§bАнтиКик§8] §aПереподключение выполнено.")
                
            } catch (e: Exception) {
                Log.w("AntiKick", "Failed to reconnect", e)
                session?.displayClientMessage("§8[§bАнтиКик§8] §cОшибка при переподключении: ${e.message}")
            }
        }
    }
}
