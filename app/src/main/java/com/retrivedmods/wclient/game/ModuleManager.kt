// File: app/src/main/java/com/retrivedmods/wclient/game/ModuleManager.kt
package com.retrivedmods.wclient.game

import android.content.Context
import android.net.Uri
import com.retrivedmods.wclient.application.AppContext

// --- Явные импорты для проблемных модулей ---
import com.retrivedmods.wclient.game.module.player.DesyncModule
import com.retrivedmods.wclient.game.module.player.FreeCameraModule
import com.retrivedmods.wclient.game.module.misc.SoundModule

// Wildcard imports (оставить для остальных модулей)
import com.retrivedmods.wclient.game.module.combat.*
import com.retrivedmods.wclient.game.module.misc.*
import com.retrivedmods.wclient.game.module.motion.*
import com.retrivedmods.wclient.game.module.player.*
import com.retrivedmods.wclient.game.module.visual.*
import com.retrivedmods.wclient.game.module.world.*

// Импорты для системы команд
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.command.impl.SkinStealerCommand
import com.retrivedmods.wclient.game.command.impl.SoundCommand

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.encodeToString // <-- Убедиться, что этот импорт есть
import kotlinx.serialization.decodeFromString // <-- Убедиться, что этот импорт есть
import java.io.File

object ModuleManager {

    private val _modules: MutableList<Module> = ArrayList()
    val modules: List<Module> = _modules

    private val _commands: MutableList<Command> = ArrayList()
    val commands: List<Command> = _commands

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun initialize(session: GameSession) {
        _modules.clear()

        fun <T : Module> addAndInitModule(module: T) {
            module.session = session
            _modules.add(module)
        }

        // --- Регистрация модулей ---
        addAndInitModule(CommandHandlerModule())
        addAndInitModule(FlyModule())
        addAndInitModule(GravityControlModule())
        addAndInitModule(ZoomModule())
        addAndInitModule(AutoHvHModule())
        addAndInitModule(AirJumpModule())
        addAndInitModule(NoClipModule())
        addAndInitModule(GlideModule())
        addAndInitModule(JitterFlyModule())
        addAndInitModule(AdvanceCombatAuraModule())
        addAndInitModule(TriggerBotModule())
        addAndInitModule(CrystalauraModule())
        addAndInitModule(TrollerModule())
        addAndInitModule(AutoClickerModule())
        addAndInitModule(DamageTextModule())
        addAndInitModule(WAuraModule())
        addAndInitModule(SpeedModule())
        addAndInitModule(JetPackModule())
        addAndInitModule(BlinkModule())
        addAndInitModule(AdvanceDisablerModule())
        addAndInitModule(NightVisionModule())
        addAndInitModule(RegenerationModule())
        addAndInitModule(AutoDisconnectModule())
        addAndInitModule(SkinStealerModule())
        addAndInitModule(PlayerJoinNotifierModule())
        addAndInitModule(HitboxModule())
        addAndInitModule(InfiniteAuraModule())
        addAndInitModule(CriticalsModule())
        addAndInitModule(FakeProxyModule())
        addAndInitModule(ReachModule())
        addAndInitModule(SmartAuraModule())
        addAndInitModule(PlayerTPModule())
        addAndInitModule(HighJumpModule())
        addAndInitModule(SpiderModule())
        addAndInitModule(JesusModule())
        addAndInitModule(AntiKnockbackModule())
        addAndInitModule(FastStopModule())
        addAndInitModule(OpFightBotModule())
        addAndInitModule(FakeLagModule())
        addAndInitModule(FastBreakModule())
        addAndInitModule(BhopModule())
        addAndInitModule(SprintModule())
        addAndInitModule(NoHurtCameraModule())
        addAndInitModule(AutoWalkModule())
        addAndInitModule(AntiAFKModule())
        addAndInitModule(DesyncModule())
        addAndInitModule(PositionLoggerModule())
        addAndInitModule(SoundModule())
        addAndInitModule(MotionFlyModule())
        addAndInitModule(FreeCameraModule())
        addAndInitModule(KillauraModule())
        addAndInitModule(AntiCrystalModule())
        addAndInitModule(TimeShiftModule())
        addAndInitModule(WeatherControllerModule())
        addAndInitModule(MotionVarModule())
        addAndInitModule(PlayerTracerModule())
        addAndInitModule(EnemyHunterModule())

        // --- Регистрация команд ---
        _commands.clear()
        _commands.add(SkinStealerCommand())
        _commands.add(SoundCommand())

        loadConfig()

        _modules.forEach {
            if (it.isEnabled) {
                it.onEnabled()
            }
        }
    }

    fun getCommand(name: String): Command? {
        // ИСПРАВЛЕНИЕ: Используем метод toLowerCase() из Java String
        return _commands.firstOrNull { it.alias.toLowerCase() == name.toLowerCase() }
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

        // ИСПРАВЛЕНИЕ: Используем более современный синтаксис encodeToString
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

        // ИСПРАВЛЕНИЕ: Используем более современный синтаксис decodeFromString
        val jsonObject = json.decodeFromString<JsonObject>(jsonString)
        val modulesConfig = jsonObject["modules"]?.jsonObject
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
            val jsonObject = json.decodeFromString<JsonObject>(configStr)
            val modulesConfig = jsonObject["modules"]?.jsonObject ?: return

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
