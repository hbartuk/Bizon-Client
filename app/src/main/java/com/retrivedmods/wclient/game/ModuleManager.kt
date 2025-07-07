// File: app/src/main/java/com/retrivedmods/wclient/game/ModuleManager.kt
package com.retrivedmods.wclient.game

import android.content.Context
import android.net.Uri
import com.retrivedmods.wclient.application.AppContext

// Импорты модулей
import com.retrivedmods.wclient.game.module.combat.* // Использование wildcard для удобства, если все модули в пакетах с одинаковыми именами
import com.retrivedmods.wclient.game.module.misc.*
import com.retrivedmods.wclient.game.module.motion.*
import com.retrivedmods.wclient.game.module.player.*
import com.retrivedmods.wclient.game.module.visual.*
import com.retrivedmods.wclient.game.module.world.*

// Импорты для системы команд
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.command.impl.SkinStealerCommand
import com.retrivedmods.wclient.game.command.impl.SoundCommand // <-- ИСПРАВЛЕНО: Добавлен импорт SoundCommand

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File

object ModuleManager {

    private val _modules: MutableList<Module> = ArrayList()
    val modules: List<Module> = _modules // Публичный неизменяемый список

    private val _commands: MutableList<Command> = ArrayList()
    val commands: List<Command> = _commands // Публичный неизменяемый список

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // Блок init теперь только для настройки JSON и, возможно, загрузки конфигов при старте приложения
    // Регистрация модулей и команд перенесена в initialize(session)
    init {
        // Здесь можно было бы загрузить конфиги, но пока оставим это в initialize,
        // так как для fromJson() нужна инициализированная session для некоторых модулей.
    }

    /**
     * Инициализирует ModuleManager, регистрируя все модули и команды.
     * Этот метод должен быть вызван один раз после создания GameSession.
     */
    fun initialize(session: GameSession) {
        // --- Регистрация модулей ---
        // Инициализируем сессию для каждого модуля здесь.
        // ModuleManager.modules должен быть MutableList<Module>
        // А Module.session должен быть lateinit var open var session: GameSession
        _modules.clear() // Очищаем на случай повторной инициализации
        with(_modules) {
            // CommandHandlerModule должен быть зарегистрирован, чтобы обрабатывать команды
            // Ему также понадобится ссылка на ModuleManager для доступа к командам
            add(CommandHandlerModule().apply { this.session = session }) // Инициализация session для CommandHandlerModule

            // Все остальные модули. Инициализируем их session сразу.
            add(FlyModule().apply { this.session = session })
            add(GravityControlModule().apply { this.session = session })
            add(ZoomModule().apply { this.session = session })
            add(AutoHvHModule().apply { this.session = session })
            add(AirJumpModule().apply { this.session = session })
            add(NoClipModule().apply { this.session = session })
            add(GlideModule().apply { this.session = session })
            add(JitterFlyModule().apply { this.session = session })
            add(AdvanceCombatAuraModule().apply { this.session = session })
            add(TriggerBotModule().apply { this.session = session })
            add(CrystalauraModule().apply { this.session = session })
            add(TrollerModule().apply { this.session = session })
            add(AutoClickerModule().apply { this.session = session })
            add(DamageTextModule().apply { this.session = session })
            add(WAuraModule().apply { this.session = session })
            add(SpeedModule().apply { this.session = session })
            add(JetPackModule().apply { this.session = session })
            add(BlinkModule().apply { this.session = session })
            add(AdvanceDisablerModule().apply { this.session = session })
            // add(BlinkModule().apply { this.session = session }) // Дубликат BlinkModule
            add(NightVisionModule().apply { this.session = session })
            add(RegenerationModule().apply { this.session = session })
            add(AutoDisconnectModule().apply { this.session = session })
            add(SkinStealerModule().apply { this.session = session })
            add(PlayerJoinNotifierModule().apply { this.session = session })
            add(HitboxModule().apply { this.session = session })
            add(InfiniteAuraModule().apply { this.session = session })
            add(CriticalsModule().apply { this.session = session })
            add(FakeProxyModule().apply { this.session = session })
            add(ReachModule().apply { this.session = session })
            add(SmartAuraModule().apply { this.session = session })
            add(PlayerTPModule().apply { this.session = session })
            add(HighJumpModule().apply { this.session = session })
            add(SpiderModule().apply { this.session = session })
            add(JesusModule().apply { this.session = session })
            add(AntiKnockbackModule().apply { this.session = session })
            add(FastStopModule().apply { this.session = session })
            add(OpFightBotModule().apply { this.session = session })
            add(FakeLagModule().apply { this.session = session })
            add(FastBreakModule().apply { this.session = session })
            add(BhopModule().apply { this.session = session })
            add(SprintModule().apply { this.session = session })
            add(NoHurtCameraModule().apply { this.session = session })
            add(AutoWalkModule().apply { this.session = session })
            add(AntiAFKModule().apply { this.session = session })
            add(DesyncModule().apply { this.session = session })
            add(PositionLoggerModule().apply { this.session = session })
            add(SoundModule().apply { this.session = session }) // <-- Инициализация session для SoundModule
            add(MotionFlyModule().apply { this.session = session })
            add(FreeCameraModule().apply { this.session = session })
            add(KillauraModule().apply { this.session = session })
            add(AntiCrystalModule().apply { this.session = session })
            add(TimeShiftModule().apply { this.session = session })
            add(WeatherControllerModule().apply { this.session = session })
            add(MotionVarModule().apply { this.session = session })
            add(PlayerTracerModule().apply { this.session = session })
            add(EnemyHunterModule().apply { this.session = session })
        }

        // --- Регистрация команд ---
        _commands.clear() // Очищаем на случай повторной инициализации
        with(_commands) {
            add(SkinStealerCommand())
            add(SoundCommand())
            // Добавляй другие команды здесь
        }

        // Загружаем конфиг после того, как все модули инициализированы session
        loadConfig()

        // Включаем модули, которые должны быть включены по умолчанию
        _modules.forEach {
            if (it.isEnabled) { // Если модуль был включен в конфиге или по defaultEnabled
                it.onEnabled() // Вызываем onEnabled, чтобы отправить сообщение и запустить логику
            }
        }
    }


    fun getCommand(name: String): Command? {
        return _commands.firstOrNull { it.alias.lowercase() == name.lowercase() } // Сравниваем точно, а не contains
    }

    fun saveConfig() {
        val configsDir = AppContext.instance.filesDir.resolve("configs")
        configsDir.mkdirs()

        val config = configsDir.resolve("UserConfig.json")
        val jsonObject = buildJsonObject {
            put("modules", buildJsonObject {
                _modules.forEach {
                    if (it.private) {
                        return@forEach
                    }
                    put(it.name, it.toJson())
                }
            })
        }

        config.writeText(json.encodeToString(jsonObject))
    }

    fun loadConfig() {
        val configsDir = AppContext.instance.filesDir.resolve("configs")
        configsDir.mkdirs()

        val config = configsDir.resolve("UserConfig.json")
        if (!config.exists()) {
            return
        }

        val jsonString = config.readText()
        if (jsonString.isEmpty()) {
            return
        }

        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val modulesConfig = jsonObject["modules"]?.jsonObject // Переименовал, чтобы не конфликтовать с `modules`
        modulesConfig?.let {
            _modules.forEach { module ->
                (it[module.name] as? JsonObject)?.let { moduleJson ->
                    module.fromJson(moduleJson)
                }
            }
        }
    }

    fun exportConfig(): String {
        val jsonObject = buildJsonObject {
            put("modules", buildJsonObject {
                _modules.forEach {
                    if (it.private) {
                        return@forEach
                    }
                    put(it.name, it.toJson())
                }
            })
        }
        return json.encodeToString(jsonObject)
    }

    fun importConfig(configStr: String) {
        try {
            val jsonObject = json.parseToJsonElement(configStr).jsonObject
            val modulesConfig = jsonObject["modules"]?.jsonObject ?: return // Переименовал

            _modules.forEach { module ->
                modulesConfig[module.name]?.let {
                    if (it is JsonObject) {
                        module.fromJson(it)
                    }
                }
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid config format")
        }
    }

    fun exportConfigToFile(context: Context, fileName: String): Boolean {
        return try {
            val configsDir = context.getExternalFilesDir("configs")
            configsDir?.mkdirs()

            val configFile = File(configsDir, "$fileName.json")
            configFile.writeText(exportConfig())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importConfigFromFile(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val configStr = input.bufferedReader().readText()
                importConfig(configStr)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
