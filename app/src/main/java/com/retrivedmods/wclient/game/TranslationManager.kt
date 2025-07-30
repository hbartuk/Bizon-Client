package com.retrivedmods.wclient.game

object TranslationManager {

    private val map = HashMap<String, Map<String, String>>()

    init {
        map["en"] = en()
        map["zh"] = zh()
    }

    private fun en() = buildMap {
        put("fly", "Fly")
        put("no_clip", "No Clip")
        put("zoom", "Zoom")
        put("air_jump", "Air Jump")
        put("speed", "Speed")
        put("full_bright", "Full Bright")
        put("haste", "Haste")
        put("jetpack", "Jetpack")
        put("levitation", "Levitation")
        put("high_jump", "High Jump")
        put("slow_falling", "Slow Falling")
        put("anti_knockback", "Velocity")
        put("poseidon", "Poseidon")
        put("regeneration", "regen")
        put("bhop", "BHOP")
        put("sprint", "Sprint")
        put("no_hurt_camera", "No Hurt Camera")
        put("anti_afk", "Anti AFK")
        put("auto_walk", "Auto Walk")
        put("desync", "Desync")
        put("position_logger", "Entity Tracer")
        put("killaura", "Killaura")
        put("motion_fly", "Motion Fly")
        put("free_camera", "Free Camera")
        put("player_tracer", "Player Tracker")
        put("critic", "Criticals")
        put("nausea", "Nausea")
        put("health_boost", "Health Boost")
        put("jump_boost", "Jump Boost")
        put("resistance", "Resistance")
        put("fire_resist", "Fire Resistance")
        put("swiftness", "Swiftness")
        put("instant_health", "Instant Health")
        put("strength", "Strength")
        put("instant_damage", "Instant Damage")
        put("anti_crystal", "Anti Crystal")
        put("bad_omen", "Bad Omen")
        put("conduit_power", "Conduit Power")
        put("darkness", "Darkness")
        put("fatal_poison", "Fatal Poison")
        put("hunger", "Hunger")
        put("poison", "Poison")
        put("village_omen", "Village Hero")
        put("weakness", "Weakness")
        put("wither", "Wither")
        put("night_vision", "Night Vision")
        put("invisibility", "Invisibility")
        put("saturation", "Saturation")
        put("absorption", "Absorption")
        put("blindness", "Blindness")
        put("hunger", "Hunger")
        put("time_shift", "Time Changer")
        put("weather_controller", "Weather Controller")
        put("crash", "Crash")


        // Below for module options
        put("times", "Times")
        put("flySpeed", "Fly Speed")
        put("range", "Range")
        put("cps", "CPS")
        put("amplifier", "Amplifier")
        put("nightVision", "Night Vision")
        put("scanRadius", "Scan Radius")
        put("jumpHeight", "Jump Height")
        put("verticalUpSpeed", "Vertical Up Speed")
        put("verticalDownSpeed", "Vertical Down Speed")
        put("motionInterval", "Motion Interval")
        put("glideSpeed", "Glide Speed")
        put("vanillaFly", "Vanilla Fly")
        put("repeat", "Repeat")
        put("delay", "Delay")
        put("enabled", "Enabled")
        put("disabled", "Disabled")
        put("players_only", "Players Only")
        put("mobs_only", "Mob Aura")
        put("time", "Time")
        put("keep_distance", "Distance")
        put("tp_speed", "Teleport Speed")
        put("packets", "Packets")
        put("strafe", "Strafe")
        put("tp_aura", "TP Aura")
        put("teleport_behind", "TP Behind")
        put("strafe_angle", "Strafe Angle")
        put("strafe_speed", "Strafe Speed")
        put("strafe_radius", "Strafe Radius")
        put("clear", "Clear")
        put("rain", "Rain")
        put("thunderstorm", "Thunderstorm")
        put("intensity", "Intensity")
        put("interval", "Interval")
    }

    private fun zh() = buildMap {
        put("fly", "飞行")
        put("no_clip", "穿墙")
        put("zoom", "缩放")
        put("air_jump", "空中跳跃")
        put("speed", "速度")
        put("full_bright", "夜视")
        put("haste", "急速")
        put("jetpack", "喷气背包")
        put("levitation", "飘浮")
        put("high_jump", "高跳")
        put("slow_falling", "缓降")
        put("anti_knockback", "防击退")
        put("poseidon", "海神")
        put("regeneration", "生命恢复")
        put("bhop", "连跳")
        put("sprint", "疾跑")
        put("no_hurt_camera", "无伤害抖动")
        put("anti_afk", "防挂机")
        put("auto_walk", "自动行走")
        put("desync", "异步发包")
        put("position_logger", "实体追踪器")
        put("killaura", "杀戮光环")
        put("motion_fly", "动量飞行")
        put("free_camera", "自由视角")
        put("player_tracer", "玩家追踪器")
        put("critic", "批评家")
        put("nausea", "反胃")
        put("health_boost", "生命提升")
        put("jump_boost", "跳跃增强")
        put("resistance", "抗性")
        put("fire_resist", "抗火")
        put("swiftness", "极速")
        put("instant_health", "瞬间治疗")
        put("strength", "力量")
        put("instant_damage", "瞬间伤害")
        put("anti_crystal", "反水晶")
        put("bad_omen", "凶兆")
        put("conduit_power", "潮涌能量")
        put("darkness", "黑暗")
        put("fatal_poison", "剧毒")
        put("hunger", "饥饿")
        put("poison", "中毒")
        put("village_omen", "村庄英雄")
        put("weakness", "虚弱")
        put("wither", "凋零")
        put("night_vision", "夜视")
        put("invisibility", "隐身")
        put("saturation", "饱和")
        put("absorption", "伤害吸收")
        put("blindness", "失明")
        put("hunger", "饥饿")
        put("time_shift", "时间修改器")
        put("crash", "崩溃")
        put("weather_controller", "天气控制器")

        // Below for module options
        put("times", "次数")
        put("flySpeed", "飞行速度")
        put("range", "范围")
        put("cps", "CPS")
        put("amplifier", "等级")
        put("nightVision", "夜视")
        put("scanRadius", "搜索半径")
        put("jumpHeight", "跳跃高度")
        put("verticalUpSpeed", "垂直上升速度")
        put("verticalDownSpeed", "垂直下降速度")
        put("motionInterval", "运动间隔")
        put("glideSpeed", "滑行速度")
        put("vanillaFly", "香草飞行")
        put("repeat", "重复")
        put("delay", "延迟")
        put("enabled", "启用")
        put("disabled", "禁用")
        put("players_only", "仅限玩家")
        put("mobs_only", "生物光环")
        put("time", "时间")
        put("keep_distance", "距离")
        put("tp_speed", "传送速度")
        put("packets", "发包次数")
        put("strafe", "环绕")
        put("tp_aura", "传送光环")
        put("teleport_behind", "传送到身后")
        put("strafe_angle", "环绕角")
        put("strafe_speed", "环绕速度")
        put("strafe_radius", "环绕半径")
        put("clear", "晴朗")
        put("rain", "雨")
        put("thunderstorm", "雷雨")
        put("intensity", "强度")
        put("interval", "间隔")
    }

    fun getTranslationMap(language: String): Map<String, String> {
        val translationMap = map[language]
        if (translationMap != null) {
            return translationMap
        }

        map.forEach { (key, value) ->
            if (key.startsWith(language)) {
                return value
            }
        }

        return map["en"]!!
    }

    private fun ru(): Map<String, String> {
        val map = HashMap<String, String>()

        // Категории модулей с эмодзи
        map["combat"] = "⚔️ Бой"
        map["motion"] = "🏃 Движение"
        map["visual"] = "👁️ Визуал"
        map["world"] = "🌍 Мир"
        map["player"] = "👤 Игрок"
        map["misc"] = "🔧 Разное"

        // Модули боя
        map["killaura"] = "Килл Аура"
        map["trigger_bot"] = "Триггер Бот"
        map["reach"] = "Досягаемость"
        map["auto_armor"] = "Авто Броня"
        map["crystal_aura"] = "Кристал Аура"
        map["velocity"] = "Антиотброс"
        map["criticals"] = "Критические Удары"
        map["auto_totem"] = "Авто Тотем"

        // Модули движения
        map["fly"] = "Полёт"
        map["speed"] = "Скорость"
        map["jesus"] = "Хождение по Воде"
        map["longjump"] = "Длинный Прыжок"
        map["step"] = "Ступенька"
        map["spider"] = "Паук"
        map["glide"] = "Планирование"
        map["highjump"] = "Высокий Прыжок"
        map["no_fall"] = "Без Урона от Падения"
        map["phase"] = "Фаза"
        map["elytra_fly"] = "Полёт на Элитрах"
        map["auto_walk"] = "Авто Ходьба"
        map["parkour"] = "Паркур"
        map["strafe"] = "Страйф"
        map["boat_fly"] = "Полёт на Лодке"
        map["freeze"] = "Заморозка"
        map["free_camera"] = "Свободная Камера"

        // Визуальные модули
        map["esp"] = "ESP"
        map["fullbright"] = "Полная Яркость"
        map["nametags"] = "Имена"
        map["tracers"] = "Трассировка"
        map["chest_esp"] = "ESP Сундуков"
        map["item_esp"] = "ESP Предметов"
        map["mob_esp"] = "ESP Мобов"
        map["xray"] = "Рентген"
        map["freecam"] = "Свободная Камера"
        map["no_hurt_cam"] = "Без Тряски Камеры"
        map["block_overlay"] = "Контур Блоков"
        map["arraylist"] = "Список Модулей"
        map["hud"] = "HUD"
        map["custom_sky"] = "Пользовательское Небо"
        map["anti_blind"] = "Анти Слепота"
        map["chams"] = "Чамы"

        // Модули мира
        map["nuker"] = "Разрушитель"
        map["scaffold"] = "Мостик"
        map["auto_mine"] = "Авто Копание"
        map["chest_stealer"] = "Воровство из Сундуков"
        map["auto_farm"] = "Авто Ферма"
        map["build_assist"] = "Помощь в Строительстве"
        map["block_reach"] = "Досягаемость Блоков"
        map["fast_break"] = "Быстрая Ломка"
        map["auto_tool"] = "Авто Инструмент"
        map["liquid_walk"] = "Хождение по Жидкости"

        // Модули игрока
        map["auto_eat"] = "Авто Еда"
        map["fast_use"] = "Быстрое Использование"
        map["inventory_move"] = "Движение в Инвентаре"
        map["no_rotate"] = "Без Поворота"
        map["auto_respawn"] = "Авто Возрождение"
        map["auto_reconnect"] = "Авто Переподключение"
        map["middle_click_pearl"] = "Эндер Жемчуг на СКМ"
        map["auto_fish"] = "Авто Рыбалка"
        map["fake_player"] = "Фальшивый Игрок"
        map["blink"] = "Блинк"

        // Разные модули
        map["auto_clicker"] = "Авто Кликер"
        map["spam"] = "Спам"
        map["fake_lag"] = "Фальшивый Лаг"
        map["packet_fly"] = "Пакетный Полёт"
        map["disabler"] = "Дизейблер"
        map["timer"] = "Таймер"
        map["ping_spoof"] = "Подмена Пинга"
        map["auto_register"] = "Авто Регистрация"
        map["chat_translate"] = "Перевод Чата"
        map["name_protect"] = "Защита Имени"
        map["tracking"] = "Отслеживание"

        // Эффекты
        map["speed_effect"] = "Эффект Скорости"
        map["jump_boost"] = "Эффект Прыгучести"
        map["haste"] = "Спешка"
        map["strength"] = "Сила"
        map["instant_health"] = "Мгновенное Лечение"
        map["instant_damage"] = "Мгновенный Урон"
        map["poison"] = "Яд"
        map["regeneration"] = "Регенерация"
        map["resistance"] = "Сопротивление"
        map["fire_resistance"] = "Огнестойкость"
        map["water_breathing"] = "Подводное Дыхание"
        map["invisibility"] = "Невидимость"
        map["saturation"] = "Насыщение"
        map["absorption"] = "Поглощение"
        map["blindness"] = "Слепота"
        map["hunger"] = "Голод"
        map["time_shift"] = "Сдвиг Времени"
        map["crash"] = "Краш"
        map["weather_controller"] = "Контроль Погоды"

        // Опции модулей
        map["times"] = "Раз"
        map["flySpeed"] = "Скорость Полёта"
        map["range"] = "Дальность"
        map["cps"] = "КПС"
        map["amplifier"] = "Уровень"
        map["nightVision"] = "Ночное Зрение"
        map["scanRadius"] = "Радиус Сканирования"
        map["jumpHeight"] = "Высота Прыжка"
        map["verticalUpSpeed"] = "Скорость Подъёма"
        map["verticalDownSpeed"] = "Скорость Спуска"
        map["motionInterval"] = "Интервал Движения"
        map["glideSpeed"] = "Скорость Планирования"
        map["vanillaFly"] = "Ванильный Полёт"
        map["repeat"] = "Повтор"
        map["delay"] = "Задержка"
        map["enabled"] = "Включено"
        map["disabled"] = "Выключено"
        map["players_only"] = "Только Игроки"
        map["mobs_only"] = "Только Мобы"
        map["time"] = "Время"
        map["keep_distance"] = "Держать Дистанцию"
        map["tp_speed"] = "Скорость ТП"
        map["packets"] = "Пакеты"
        map["strafe"] = "Страйф"
        map["tp_aura"] = "ТП Аура"
        map["teleport_behind"] = "Телепорт Назад"
        map["strafe_angle"] = "Угол Страйфа"
        map["strafe_speed"] = "Скорость Страйфа"
        map["strafe_radius"] = "Радиус Страйфа"
        map["clear"] = "Очистить"
        map["critical_hit"] = "Критический Удар"
        map["anti_kb"] = "Анти Отброс"
        map["only_when_holding_item"] = "Только с Предметом"
        map["click_delay"] = "Задержка Клика"
        map["auto_block"] = "Авто Блок"
        map["target_players"] = "Цель: Игроки"
        map["target_mobs"] = "Цель: Мобы"
        map["through_walls"] = "Сквозь Стены"
        map["swing_arm"] = "Махать Рукой"
        map["rotation_speed"] = "Скорость Поворота"
        map["fov"] = "Поле Зрения"
        map["predict_movement"] = "Предсказание Движения"
        map["multi_aura"] = "Мульти Аура"
        map["auto_disable"] = "Авто Отключение"
        map["silent"] = "Скрытый"
        map["legit"] = "Легитный"
        map["bypass"] = "Обход"
        map["smart"] = "Умный"
        map["fast"] = "Быстрый"
        map["instant"] = "Мгновенный"
        map["smooth"] = "Плавный"
        map["random"] = "Случайный"
        map["dynamic"] = "Динамический"
        map["adaptive"] = "Адаптивный"

        return map
    }
}