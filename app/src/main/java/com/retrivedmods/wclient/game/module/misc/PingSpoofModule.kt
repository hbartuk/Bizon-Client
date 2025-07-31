package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module // Базовый класс Module
import com.retrivedmods.wclient.game.ModuleCategory // Категория модуля
// Исправляем импорт для IntValue, он, вероятно, находится в game.Module или constructors
// Исходя из ваших примеров, boolValue, intValue и т.д. являются частью базового класса Module
// Поэтому отдельный импорт для 'constructors' или 'IntValue' может быть не нужен, если они объявлены в Module.
// Если ошибки 'intValue' сохранятся, добавьте 'import com.retrivedmods.wclient.constructors.IntValue'
// и аналогично для BoolValue, FloatValue, если они у вас в таком пакете.

// Проверим AssetManager:
import com.retrivedmods.wclient.util.AssetManager // Убедитесь, что это правильный путь к AssetManager

import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.coerceAtLeast // Импорт для coerceAtLeast

// Меняем название класса на PingSpoofModule для соответствия файлу
class PingSpoofModule : Module(
    name = "FakePing", // Передаем имя
    category = ModuleCategory.Misc // Передаем категорию
    // iconResId и displayNameResId не принимаются конструктором Module
    // Если вам нужны эти поля, их нужно будет объявить внутри класса PingSpoofModule
    // и, возможно, обновить логику отображения GUI, чтобы она брала их из экземпляра модуля.
    // Пока что, следуя вашим примерам, убираем их из конструктора Module().
) {
    // Внутри класса, если хотите иметь ссылку на эти ресурсы
    // private val moduleIconResId: Int = AssetManager.getAsset("ic_timer_sand_black_24dp")
    // private val moduleDisplayNameResId: Int = AssetManager.getString("module_fakeping_display_name")


    // Пользовательские настройки пинга (используем как в ваших примерах)
    private val pingValue by intValue("Пинг (мс)", 300, 50..1000)
    private val jitter by intValue("Джиттер (мс)", 50, 0..200)
    private val tickInterval by intValue("Интервал тиков", 1, 1..20)

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
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        when (packet) {
            is NetworkStackLatencyPacket -> {
                // ИСХОДЯЩИЙ пакет (от клиента к серверу) имеет timestamp = System.currentTimeMillis()
                // ВХОДЯЩИЙ пакет (от сервера к клиенту) имеет timestamp = временная метка сервера
                // Чтобы определить, пришел ли пакет от сервера, нужно проверить его тип
                // или контекст, в котором он был получен.
                // CloudburstMC NetworkStackLatencyPacket не имеет публичного поля fromServer.
                // Мы должны предполагать, что packet в beforePacketBound для входящих пакетов - это NetworkStackLatencyPacket от сервера.
                // Или, если beforePacketBound - это для всех пакетов, нам нужно другое условие.
                // Исходя из вашего кода, beforePacketBound, вероятно, обрабатывает как входящие, так и исходящие пакеты.
                // В таком случае, нам нужно явно проверять, предназначен ли пакет для клиента.

                // Предполагается, что 'interceptablePacket.packet' - это пакет, который вот-вот будет обработан.
                // Если NetworkStackLatencyPacket НЕ имеет публичного поля fromServer,
                // тогда мы не можем использовать 'packet.fromServer'.
                // Вместо этого, нам нужно будет фильтровать пакеты по их направлению,
                // если InterceptablePacket предоставляет такую информацию.
                // Поскольку в вашем коде 'packet.fromServer' используется,
                // возможно, у вас есть своя модифицированная версия CloudburstMC,
                // или InterceptablePacket каким-то образом предоставляет это.
                // Если нет, то мы не сможем различать пакеты пинга по fromServer.

                // Временно используем проверку на timestamp = 0L,
                // как это иногда используется в некоторых реализациях для идентификации пакетов от сервера.
                // Это ОЧЕНЬ непрочное решение и требует точного понимания протокола
                // или наличия более надежного поля/метода.
                // Если 'fromServer' действительно приватное, то лучшее, что мы можем сделать,
                // это отфильтровать по типу пакета, а затем управлять задержкой.
                // Если packet.fromServer вызывает ошибку, его надо убрать.
                // Тогда этот блок будет срабатывать на все NetworkStackLatencyPacket.

                // Если InterceptablePacket - это ваш обертчик, он МОЖЕТ иметь свойство isIncoming
                // if (packet is NetworkStackLatencyPacket && interceptablePacket.isIncoming) { // Если у InterceptablePacket есть свойство isIncoming
                //     handleLatencyPacket(interceptablePacket, packet)
                // }

                // Если у InterceptablePacket нет isIncoming, и fromServer приватное, то этот модуль
                // будет задерживать как входящие, так и исходящие пакеты пинга, что не идеально, но будет работать как FakePing.
                // Убираем problematic 'packet.fromServer' если оно приватное:
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
                // ИСХОДЯЩИЙ пакет от клиента К СЕРВЕРУ
                // Если 'fromServer' приватное, здесь тоже будет проблема.
                // Если это ответ клиента, то fromServer должно быть false или не устанавливаться.
                // CloudburstMC Protocol Bedrock 567 (1.20.70)
                // NetworkStackLatencyPacket не имеет поля fromServer для исходящих пакетов.
                // Возможно, вы хотите имитировать пакет NetworkStackLatencyPacket, который сервер отправляет обратно клиенту?
                // Тогда fromServer = true было бы корректно, но отправлять его нужно клиенту.
                // Здесь мы отправляем на СЕРВЕР (serverBound).
                // ПОЭТОМУ fromServer = true; здесь, вероятно, некорректно для пакета, отправляемого НА СЕРВЕР.
                // Убираем fromServer = true; если оно для пакетов, отправляемых от сервера к клиенту.
                // Если поле fromServer есть только для входящих пакетов и его нельзя установить для исходящих,
                // то его здесь быть не должно.
                // Поле 'fromServer' отсутствует в NetworkStackLatencyPacket для отправки на сервер.
                // В NetworkStackLatencyPacket есть только 'timestamp' и базовый класс BedrockPacket.
                // Просто убираем эту строку, если она вызывает ошибку.
                // fromServer = true
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
