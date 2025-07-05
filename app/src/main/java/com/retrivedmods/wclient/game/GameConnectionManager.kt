package com.retrivedmods.wclient.game

import android.util.Log
import com.retrivedmods.wclient.application.AppContext
import com.retrivedmods.wclient.game.entity.Account
import com.mucheng.mucute.relay.MuCuteRelay
import com.mucheng.mucute.relay.MuCuteRelaySession
import com.mucheng.mucute.relay.address.MuCuteAddress
import kotlin.concurrent.thread

@Suppress("MemberVisibilityCanBePrivate")
object GameConnectionManager {

    var minecraftRelay: MuCuteRelay? = null // Теперь это основной объект прокси MuCuteRelay
    private var gameSession: GameSession? = null // Сессия для подключившегося клиента

    @Suppress("UnstableApiUsage")
    fun connect(hostname: String, port: Int, account: Account, callback: (Boolean) -> Unit) {
        if (this.minecraftRelay != null && this.minecraftRelay!!.isRunning) {
            this.disconnect()
        }
        thread {
            try {
                // Определи локальный порт, на котором твой прокси будет слушать входящие подключения от клиентов
                val localProxyPort = 19132 // Можешь изменить на любой свободный порт
                val remoteServerPort = port // Порт целевого Minecraft-сервера

                val localAddress = MuCuteAddress("0.0.0.0", localProxyPort)
                val remoteAddress = MuCuteAddress(hostname, remoteServerPort)

                // Инициализация MuCuteRelay.
                // Важно: поскольку мы не можем изменять библиотеку,
                // мы не можем передать 'account' напрямую в конструктор MuCuteRelay.
                // Логин прокси к целевому серверу будет зависеть от поведения библиотеки по умолчанию.
                minecraftRelay = MuCuteRelay(
                    localAddress = localAddress,
                    remoteAddress = remoteAddress // Устанавливаем целевой адрес сервера для прокси
                )

                // Запускаем прокси-сервер. Метод capture() является блокирующим
                // (до тех пор, пока сервер не будет отключен через disconnect()).
                // Лямбда (onSessionCreated) внутри capture() выполняется, когда
                // КЛИЕНТ подключается К ТВОЕМУ ПРОКСИ.
                minecraftRelay?.capture(
                    remoteAddress = remoteAddress // Убедимся, что capture использует правильный удаленный сервер
                ) {
                    // 'this' внутри этой лямбды относится к экземпляру MuCuteRelaySession,
                    // который управляет соединением между клиентом (подключившимся к прокси)
                    // и целевым Minecraft-сервером.
                    val muCuteRelaySessionInstance = this

                    // Создаем твой GameSession для этого конкретного подключения клиента.
                    // Передаем MuCuteRelaySession и объект Account.
                    // Объект Account здесь — это аккаунт ТВОЕГО прокси, который ты хочешь использовать для скина.
                    gameSession = GameSession(muCuteRelaySessionInstance, account)

                    Log.d("GameConnectionManager", "MuCuteRelaySession создан для клиента. GameSession инициализирован.")
                    // Фактическое подключение прокси к целевому серверу происходит внутренне в MuCuteRelay,
                    // часто при первом подключении клиента к прокси или неявно.
                    // Мы полагаемся на метод beforePacketBound в GameSession.kt для отправки PlayerSkinPacket.
                }

                // Вызываем коллбэк, чтобы сообщить, что настройка прокси инициирована.
                // Обрати внимание: capture() является блокирующим, поэтому этот коллбэк
                // будет вызван после того, как сервер начнет слушать, но до того,
                // как он будет отключен.
                callback(true)

            } catch (t: Throwable) {
                Log.e("GameConnectionManager", "Connect error: ${t.stackTraceToString()}")
                callback(false)
            }
        }
    }

    fun disconnect() {
        if (minecraftRelay?.isRunning == true) {
            minecraftRelay?.disconnect()
        }
        minecraftRelay = null
        gameSession = null // Очищаем GameSession тоже
        Log.d("GameConnectionManager", "Прокси отключен.")
    }
}
