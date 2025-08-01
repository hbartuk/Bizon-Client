// File: com.retrivedmods.wclient.game.ModuleManager.kt
package com.retrivedmods.wclient.game

import android.content.Context
import android.net.Uri
import com.retrivedmods.wclient.application.AppContext
// Импорты модулей
import com.retrivedmods.wclient.game.module.combat.*
import com.retrivedmods.wclient.game.module.misc.*
import com.retrivedmods.wclient.game.module.player.*
import com.retrivedmods.wclient.game.module.motion.*
import com.retrivedmods.wclient.game.module.visual.*
import com.retrivedmods.wclient.game.module.world.*

// Эти явные импорты нужны, если DesyncModule и FreeCameraModule находятся в других подпакетах.
// Если они в том же пакете, что и com.retrivedmods.wclient.game.module.player.*,
// то эти строки не нужны, но и вреда от них не будет, если они дублируют.
import com.retrivedmods.wclient.game.module.player.DesyncModule
import com.retrivedmods.wclient.game.module.player.FreeCameraModule

// *** НОВЫЙ ИМПОРТ ДЛЯ SPOOFINGMODULE ***
//import com.retrivedmods.wclient.game.module.misc.SpoofingModule // Убедитесь, что путь правильный, если SpoofingModule находится в другом месте

// Импорт команд
import com.retrivedmods.wclient.game.command.impl.SledCommand
import com.retrivedmods.wclient.game.command.impl.SaveSkinCommand
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.command.impl.SkinStealerCommand
import com.retrivedmods.wclient.game.command.impl.SoundCommand

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File

object ModuleManager {

    var session: GameSession? = null

    // *** ИСПРАВЛЕНИЕ: МЕНЯЕМ НА "public val _modules" ДЛЯ НАДЕЖНОСТИ ***
    public val _modules: MutableList<Module> = ArrayList()
    val modules: List<Module> = _modules

    private val _commands: MutableList<Command> = ArrayList()
    val commands: List<Command> = _commands

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    init {
        with(_modules) {
            add(CommandHandlerModule())
            add(FlyModule())
            add(GravityControlModule())
            add(ZoomModule())
            add(AutoHvHModule())
            add(AirJumpModule())
            add(NoClipModule())
            add(GlideModule())
            add(JitterFlyModule())
            add(AdvanceCombatAuraModule())
            add(TriggerBotModule())
            add(CrystalauraModule())
            add(TrollerModule())
            add(AutoClickerModule())
            add(DamageTextModule())
            add(WAuraModule())
            add(TrackingModule())
            add(SpeedModule())
            add(JetPackModule())
            add(AdvanceDisablerModule())
            add(BlinkModule()) // Дублирование, возможно, следует удалить одну
            add(NightVisionModule())
            add(PingSpoofModule())
            add(RegenerationModule())
            add(AutoDisconnectModule())
            add(SkinStealerModule())
            add(SoundModule()) // SoundModule is registered here
            add(AntiKickModule())
            add(NoFormsModule())
            add(PlayerJoinNotifierModule())
            add(HitboxModule())
            add(InfiniteAuraModule())
            add(CriticalsModule())
            add(FakeProxyModule())
            add(ReachModule())
            add(SmartAuraModule())
            add(PlayerTPModule())
            add(HighJumpModule())
            add(SpiderModule())
            add(JesusModule())
            add(AntiKnockbackModule())
            add(FastStopModule())
            add(OpFightBotModule())
            add(FakeLagModule())
            add(FastBreakModule())
            add(BhopModule())
            add(SprintModule())
            add(NoHurtCameraModule())
            add(AutoWalkModule())
            add(AntiAFKModule())
            add(DesyncModule())
            add(PositionLoggerModule())
            add(MotionFlyModule())
            add(FreeCameraModule())
            add(KillauraModule())
            add(AntiCrystalModule())
            add(TimeShiftModule())
            add(WeatherControllerModule())
            add(MotionVarModule())
            add(PlayerTracerModule())
            add(ChestFinderModule())
            add(EnemyHunterModule())
        }

        with(_commands) {
            add(SkinStealerCommand())
            add(SoundCommand())
            add(SledCommand())
            add(SaveSkinCommand())
        }
    }

    fun initialize(session: GameSession) {
        this.session = session
        _modules.forEach { module ->
            module.session = session
            module.initialize()
        }
    }

    inline fun <reified T : Module> getModule(): T? {
        // Доступ к _modules теперь разрешен, потому что он public
        return _modules.firstOrNull { it is T } as? T
    }

    fun getCommand(name: String): Command? {
        return _commands.firstOrNull { it.alias.contains(name.lowercase()) }
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
        val modules = jsonObject["modules"]!!.jsonObject
        _modules.forEach { module ->
            (modules[module.name] as? JsonObject)?.let {
                module.fromJson(it)
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
            val modules = jsonObject["modules"]?.jsonObject ?: return

            _modules.forEach { module ->
                modules[module.name]?.let {
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
