package com.retrivedmods.wclient.game.module.combat

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.entity.Player
import com.retrivedmods.wclient.game.entity.LocalPlayer
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemUseTransaction
import org.cloudburstmc.math.vector.Vector3f
import kotlin.math.*

class AutoClickerModule : Module("AutoClicker", ModuleCategory.Combat) {

    private var clicksPerSecond by intValue("КПС", 10, 1..50)
    private var attackRange by floatValue("Дистанция атаки", 4.0f, 1.0f..10.0f)
    private var fovRange by floatValue("FOV угол", 90.0f, 30.0f..180.0f)

    // Настройки целей
    private var attackPlayers by boolValue("Атаковать игроков", true)
    private var attackMobs by boolValue("Атаковать мобов", true)
    private var ignoreBots by boolValue("Игнорировать ботов", true)
    private var requireCrosshair by boolValue("Только по прицелу", true)

    private var lastAttackTime: Long = 0L

    override fun onEnabled() {
        session?.displayClientMessage("§a[AutoClicker] Включен. КПС: $clicksPerSecond")
    }

    override fun onDisabled() {
        session?.displayClientMessage("§c[AutoClicker] Выключен")
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet !is PlayerAuthInputPacket) return

        val minDelay = 1000L / clicksPerSecond
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastAttackTime < minDelay) return

        session?.let { session ->
            val localPlayer = session.localPlayer
            val localPlayerPos = localPlayer.vec3Position
            val localPlayerRotation = localPlayer.rotation

            // Поиск цели
            val target = findBestTarget(localPlayerPos, localPlayerRotation, session) ?: return

            // Выполняем атаку
            performAttack(target, session)
            lastAttackTime = currentTime
        }
    }

    private fun findBestTarget(playerPos: Vector3f, playerRotation: Vector3f, session: com.retrivedmods.wclient.game.GameSession): Player? {
        val players = session.players.values

        return players
            .filter { player ->
                if (!attackPlayers && isPlayer(player)) return@filter false
                if (!attackMobs && isMob(player)) return@filter false
                if (ignoreBots && isBot(player)) return@filter false

                val distance = playerPos.distance(player.vec3Position)
                if (distance > attackRange) return@filter false

                if (requireCrosshair && !isInCrosshair(playerPos, playerRotation, player.vec3Position)) return@filter false

                true
            }
            .minByOrNull { playerPos.distance(it.vec3Position) }
    }

    private fun isPlayer(player: Player): Boolean {
        // Проверяем, является ли сущность игроком
        return player.nameTag.matches(Regex("[a-zA-Z0-9_]{3,16}"))
    }

    private fun isMob(player: Player): Boolean {
        // Проверяем, является ли сущность мобом (упрощенная логика)
        val mobKeywords = setOf("zombie", "skeleton", "spider", "creeper", "enderman", "witch", "pillager")
        return mobKeywords.any { player.nameTag.lowercase().contains(it) }
    }

    private fun isBot(player: Player): Boolean {
        // Простая проверка на бота
        val botPatterns = listOf(
            Regex("bot_.*", RegexOption.IGNORE_CASE),
            Regex(".*bot.*", RegexOption.IGNORE_CASE),
            Regex("npc_.*", RegexOption.IGNORE_CASE)
        )
        return botPatterns.any { it.matches(player.nameTag) }
    }

    private fun isInCrosshair(playerPos: Vector3f, playerRotation: Vector3f, targetPos: Vector3f): Boolean {
        val direction = targetPos.sub(playerPos).normalize()
        val playerDirection = Vector3f.from(
            -sin(Math.toRadians(playerRotation.y.toDouble())).toFloat() * cos(Math.toRadians(playerRotation.x.toDouble())).toFloat(),
            -sin(Math.toRadians(playerRotation.x.toDouble())).toFloat(),
            cos(Math.toRadians(playerRotation.y.toDouble())).toFloat() * cos(Math.toRadians(playerRotation.x.toDouble())).toFloat()
        )

        val dot = direction.dot(playerDirection)
        val angle = Math.toDegrees(acos(dot.toDouble())).toFloat()

        return angle <= fovRange / 2
    }

    private fun performAttack(target: Player, session: com.retrivedmods.wclient.game.GameSession) {
        // Создаем пакет для атаки
        val attackPacket = InventoryTransactionPacket().apply {
            transactionType = InventoryTransactionType.ITEM_USE_ON_ENTITY
            // Здесь нужно настроить данные транзакции для атаки
        }

        session.serverBound(attackPacket)
        session.displayClientMessage("§7[AutoClicker] Атакован: §f${target.nameTag}")
    }
}