"""Language files: Russian and English."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANG = ROOT / "src/main/resources/assets/airsystem/lang"

DRONE_NAMES = {
    "shahed_131": ("Шахед-131", "Shahed-131"),
    "shahed_136": ("Шахед-136", "Shahed-136"),
    "shahed_238": ("Шахед-238 «Реактивный»", "Shahed-238 (Jet)"),
    "orlan_10": ("Орлан-10", "Orlan-10"),
    "orlan_30": ("Орлан-30", "Orlan-30"),
    "eleron_3": ("Элерон-3", "Eleron-3"),
    "zala_421_16e": ("ZALA 421-16Е", "ZALA 421-16E"),
    "zala_421_08": ("ZALA 421-08", "ZALA 421-08"),
    "granat_4": ("Гранат-4", "Granat-4"),
    "lancet_1": ("Ланцет-1", "Lancet-1"),
    "lancet_3": ("Ланцет-3", "Lancet-3"),
    "kub_bla": ("КУБ-БЛА", "KUB-BLA"),
    "orion": ("Иноходец (Орион)", "Inokhodets (Orion)"),
    "s_70": ("С-70 «Охотник»", "S-70 Okhotnik"),
    "leleka_100": ("Лелека-100", "Leleka-100"),
    "shark": ("SHARK", "SHARK"),
    "pd_2": ("PD-2", "PD-2"),
    "liutyi": ("Лютый (Ан-196)", "Liutyi (An-196)"),
    "uj_22": ("UJ-22 Airborne", "UJ-22 Airborne"),
    "ram_ii": ("RAM II UAV", "RAM II UAV"),
}

TURRET_NAMES = {
    "gepard": ("ЗСУ Gepard", "Flakpanzer Gepard"),
    "slinger": ("ЗУ Slinger", "Slinger"),
    "terrahawk_paladin": ("Terrahawk Paladin", "Terrahawk Paladin"),
    "mantis": ("Авто-ПВО MANTIS", "MANTIS Auto AAA"),
}

RU = {
    "itemGroup.airsystem.main": "Project AirSystem",

    "block.airsystem.drone": "Беспилотник на стоянке",

    "tooltip.airsystem.remote.linked_at": "Аппарат на стоянке: %s %s %s",
    "tooltip.airsystem.remote.in_flight": "Борт в воздухе — откройте трансляцию",

    "message.airsystem.remote.error_too_many": "Ошибка: в воздухе слишком много аппаратов",

    "screen.airsystem.remote.drone_in_flight": "Борт в воздухе",
    "screen.airsystem.feed.look_free": "Обзор: свободный",
    "screen.airsystem.feed.look_locked": "Обзор: по курсу",
    "screen.airsystem.feed.no_signal": "НЕТ СИГНАЛА",
    "screen.airsystem.feed.munitions": "Боеприпасы",

    "command.airsystem.flights.empty": "В воздухе никого",
    "command.airsystem.flights.header": "Бортов в воздухе: %s",
    "command.airsystem.abort": "Снято с маршрута: %s",
    "command.airsystem.launched": "Поднят %s на цель %s %s %s",

    "block.airsystem.air_raid_siren": "Громкоговоритель оповещения",
    "block.airsystem.alarm_button": "Кнопка воздушной тревоги",

    "item.airsystem.world_map": "Карта мира",
    "item.airsystem.remote_control": "Пульт управления",
    "item.airsystem.linking_cable": "Кабель связи",
    "item.airsystem.ammo_35mm": "35-мм снаряды",
    "item.airsystem.ammo_30mm": "30-мм снаряды",
    "item.airsystem.drone_frame": "Планер БПЛА",
    "item.airsystem.engine_module": "Двигательный модуль",
    "item.airsystem.warhead_module": "Боевая часть",
    "item.airsystem.camera_module": "Оптико-электронный модуль",
    "item.airsystem.guidance_module": "Модуль наведения",

    "tooltip.airsystem.drone.role_recon": "Назначение: разведка и наблюдение",
    "tooltip.airsystem.drone.role_kamikaze": "Назначение: барражирующий боеприпас",
    "tooltip.airsystem.drone.role_strike": "Назначение: ударный комплекс",
    "tooltip.airsystem.drone.speed": "Крейсерская скорость: %s блоков/с",
    "tooltip.airsystem.drone.ceiling": "Практический потолок: %s",
    "tooltip.airsystem.drone.warhead": "Боевая часть: мощность %s",
    "tooltip.airsystem.drone.incendiary": "Зажигательная боевая часть",
    "tooltip.airsystem.drone.recon": "Боевой части нет — только разведка",
    "tooltip.airsystem.drone.hint": "Поставьте на землю и подключите пультом",

    "tooltip.airsystem.turret.caliber": "Калибр: %s",
    "tooltip.airsystem.turret.range": "Дальность огня: %s блоков",
    "tooltip.airsystem.turret.magazine": "Боекомплект: %s выстрелов",
    "tooltip.airsystem.turret.hint": "ПКМ — сесть за прицел, Shift+ПКМ — автоматический режим",
    "tooltip.airsystem.turret.auto_hint":
        "Работает сама: ищет цели РЛС и бьёт по чужим. Свои — стартовавшие в радиусе %s блоков",

    "tooltip.airsystem.remote.linked": "Аппарат подключён",
    "tooltip.airsystem.remote.not_linked": "Аппарат не подключён",
    "tooltip.airsystem.remote.target": "Цель: %s",
    "tooltip.airsystem.remote.no_target": "Цель не задана",
    "tooltip.airsystem.remote.hint": "ПКМ по дрону — подключить, ПКМ в воздух — открыть пульт",

    "tooltip.airsystem.map.mark": "Отметка: %s",
    "tooltip.airsystem.map.hint": "ПКМ — открыть карту, клик по карте — скопировать координаты",

    "tooltip.airsystem.cable.selected": "Выбран громкоговоритель: %s %s %s",
    "tooltip.airsystem.cable.hint": "Кликните по громкоговорителю, затем по кнопке тревоги",

    "message.airsystem.turret.no_ammo": "Боекомплект пуст",
    "message.airsystem.turret.full": "Боекомплект полон",
    "message.airsystem.turret.loaded": "Заряжено: %s выстрелов",
    "message.airsystem.turret.auto_on": "Автоматический режим включён — РЛС ведёт поиск целей",
    "message.airsystem.turret.auto_off": "Автоматический режим выключен",
    "message.airsystem.turret.always_auto":
        "Установка работает только автоматически. Свои борта — стартующие в радиусе %s блоков",

    "message.airsystem.cable.siren_selected": "Громкоговоритель выбран: %s %s %s",
    "message.airsystem.cable.no_siren": "Сначала кликните по громкоговорителю",
    "message.airsystem.cable.siren_gone": "Выбранный громкоговоритель не найден",
    "message.airsystem.cable.linked": "Привязано. Всего громкоговорителей: %s",
    "message.airsystem.cable.unlinked": "Привязка снята. Осталось: %s",

    "message.airsystem.alarm.on": "Тревога объявлена",
    "message.airsystem.alarm.off": "Дан отбой тревоги",
    "message.airsystem.alarm.no_sirens": "Нет привязанных громкоговорителей поблизости",
    "message.airsystem.alarm.raised": "ВОЗДУШНАЯ ТРЕВОГА — сирен задействовано: %s",
    "message.airsystem.alarm.cleared": "Отбой тревоги — сирен задействовано: %s",

    "message.airsystem.drone.manual_on": "Ручное управление включено: WASD, пробел — газ, Shift — сброс",
    "message.airsystem.drone.landing": "Возврат на стоянку: автопилот ведёт аппарат домой",
    "message.airsystem.drone.landing_impossible": "Посадка невозможна: нет связи со стоянкой или отказал двигатель",
    "message.airsystem.drone.landing_one_way": "Барражирующий боеприпас не садится",
    "message.airsystem.drone.landed": "Аппарат сел на стоянку %s %s %s",
    "message.airsystem.drone.manual_off": "Ручное управление выключено — аппарат идёт по маршруту",
    "message.airsystem.drone.linked": "Аппарат подключён к пульту: %s",

    "message.airsystem.remote.target_set": "Цель задана: %s %s %s",
    "message.airsystem.remote.error_no_drone": "Ошибка: аппарат не подключён к пульту",
    "message.airsystem.remote.error_no_target": "Ошибка: не заданы координаты цели",
    "message.airsystem.remote.error_lost": "Ошибка: связь с аппаратом потеряна",
    "message.airsystem.remote.error_range": "Ошибка: аппарат вне зоны действия пульта",
    "message.airsystem.remote.error_not_grounded": "Ошибка: аппарат должен стоять на земле",
    "message.airsystem.remote.launched": "Пуск выполнен. Цель: %s %s %s",
    "message.airsystem.remote.launched_free": "Пуск без задания. Ведите борт вручную",

    "screen.airsystem.drone_feed": "Трансляция с борта",
    "screen.airsystem.feed.manual_on": "Ручное управление: ВКЛ",
    "screen.airsystem.feed.manual_off": "Ручное управление",
    "screen.airsystem.feed.manual_wait": "Переключение…",
    "screen.airsystem.feed.strike": "Удар",
    "screen.airsystem.feed.land": "Посадка",
    "screen.airsystem.feed.returning": "ВОЗВРАТ НА СТОЯНКУ",
    "screen.airsystem.feed.disconnect": "Отключиться",
    "screen.airsystem.feed.state": "Режим",
    "screen.airsystem.feed.altitude": "Высота",
    "screen.airsystem.feed.speed": "Скорость",
    "screen.airsystem.feed.position": "Координаты",
    "screen.airsystem.feed.target": "Цель",
    "screen.airsystem.feed.distance": "До цели",
    "screen.airsystem.feed.fuel": "Топливо",
    "screen.airsystem.feed.unit.metre": "м",
    "screen.airsystem.feed.unit.speed": "км/ч",
    "screen.airsystem.feed.engine_lost": "ОТКАЗ ДВИГАТЕЛЯ",
    "screen.airsystem.feed.damage": "Повреждения корпуса, запас: %s",
    "screen.airsystem.feed.controls": "W/S — тангаж, A/D — курс, пробел — газ, Shift — сброс газа",

    "screen.airsystem.remote": "Пульт управления БПЛА",
    "screen.airsystem.remote.coordinates": "Координаты цели",
    "screen.airsystem.remote.paste": "Вставить",
    "screen.airsystem.remote.save": "Сохранить",
    "screen.airsystem.remote.start": "СТАРТ",
    "screen.airsystem.remote.clipboard_empty": "Буфер обмена пуст",
    "screen.airsystem.remote.pasted": "Координаты вставлены",
    "screen.airsystem.remote.bad_coordinates": "Неверный формат координат",
    "screen.airsystem.remote.saved": "Цель сохранена",
    "screen.airsystem.remote.drone_connected": "Аппарат подключён",
    "screen.airsystem.remote.drone_missing": "Аппарат не подключён",
    "screen.airsystem.remote.current_target": "Текущая цель: %s",
    "screen.airsystem.remote.no_target": "Цель не задана",
    "screen.airsystem.remote.mode_target": "Точка: нужна",
    "screen.airsystem.remote.mode_free": "Точка: не нужна",
    "screen.airsystem.remote.target_not_needed": "Вылет без задания — ручное управление",
    "screen.airsystem.remote.manual_hint": "Координаты не требуются",
    "screen.airsystem.remote.hint": "Скопируйте координаты с карты мира и вставьте сюда",

    "screen.airsystem.world_map": "Карта мира",
    "screen.airsystem.map.copy": "Копировать",
    "screen.airsystem.map.center": "К игроку",
    "screen.airsystem.map.scale": "Масштаб: %s блоков в пикселе",
    "screen.airsystem.map.marked": "Отметка: %s",
    "screen.airsystem.map.copied": "Координаты скопированы в буфер обмена",
    "screen.airsystem.map.hint": "ЛКМ — отметить цель, ПКМ — перетащить карту, колесо — масштаб",

    "hud.airsystem.air_raid": "ВОЗДУШНАЯ ТРЕВОГА",
    "hud.airsystem.air_raid.sub": "Пройдите в укрытие",
    "hud.airsystem.all_clear": "ОТБОЙ ТРЕВОГИ",
    "hud.airsystem.all_clear.sub": "Угроза миновала",

    "state.airsystem.idle": "На земле",
    "state.airsystem.launch": "Набор высоты",
    "state.airsystem.cruise": "Маршрут",
    "state.airsystem.orbit": "Барражирование",
    "state.airsystem.dive": "Боевой курс",
    "state.airsystem.falling": "Падение",
    "entity.airsystem.aerial_bomb": "Управляемая авиабомба",
    "message.airsystem.drone.no_munitions": "Ошибка: боеприпасы израсходованы",
    "state.airsystem.destroyed": "Уничтожен",
    "state.airsystem.rtb": "Возврат",
    "state.airsystem.landing": "Заход на посадку",
    "state.airsystem.landed": "Сел",
}

EN = {
    "itemGroup.airsystem.main": "Project AirSystem",

    "block.airsystem.drone": "Parked UAV",

    "tooltip.airsystem.remote.linked_at": "UAV parked at %s %s %s",
    "tooltip.airsystem.remote.in_flight": "Airborne — open the feed",

    "message.airsystem.remote.error_too_many": "Error: too many UAVs airborne",

    "screen.airsystem.remote.drone_in_flight": "Airborne",
    "screen.airsystem.feed.look_free": "View: free",
    "screen.airsystem.feed.look_locked": "View: heading",
    "screen.airsystem.feed.no_signal": "NO SIGNAL",
    "screen.airsystem.feed.munitions": "Munitions",

    "command.airsystem.flights.empty": "Nothing airborne",
    "command.airsystem.flights.header": "UAVs airborne: %s",
    "command.airsystem.abort": "Recalled: %s",
    "command.airsystem.launched": "Launched %s at %s %s %s",

    "block.airsystem.air_raid_siren": "Air Raid Loudspeaker",
    "block.airsystem.alarm_button": "Air Raid Alarm Button",

    "item.airsystem.world_map": "World Map",
    "item.airsystem.remote_control": "Remote Control",
    "item.airsystem.linking_cable": "Linking Cable",
    "item.airsystem.ammo_35mm": "35mm Shells",
    "item.airsystem.ammo_30mm": "30mm Shells",
    "item.airsystem.drone_frame": "UAV Airframe",
    "item.airsystem.engine_module": "Engine Module",
    "item.airsystem.warhead_module": "Warhead Module",
    "item.airsystem.camera_module": "Optical Module",
    "item.airsystem.guidance_module": "Guidance Module",

    "tooltip.airsystem.drone.role_recon": "Role: reconnaissance and observation",
    "tooltip.airsystem.drone.role_kamikaze": "Role: loitering munition",
    "tooltip.airsystem.drone.role_strike": "Role: strike platform",
    "tooltip.airsystem.drone.speed": "Cruise speed: %s blocks/s",
    "tooltip.airsystem.drone.ceiling": "Service ceiling: %s",
    "tooltip.airsystem.drone.warhead": "Warhead power: %s",
    "tooltip.airsystem.drone.incendiary": "Incendiary warhead",
    "tooltip.airsystem.drone.recon": "No warhead — reconnaissance only",
    "tooltip.airsystem.drone.hint": "Place on the ground, then link it with the remote",

    "tooltip.airsystem.turret.caliber": "Caliber: %s",
    "tooltip.airsystem.turret.range": "Engagement range: %s blocks",
    "tooltip.airsystem.turret.magazine": "Magazine: %s rounds",
    "tooltip.airsystem.turret.hint": "Right-click to man the sight, Shift+Right-click for auto mode",
    "tooltip.airsystem.turret.auto_hint":
        "Fully automatic: the radar finds targets and engages hostiles. Drones launched within %s blocks are friendly",

    "tooltip.airsystem.remote.linked": "Drone linked",
    "tooltip.airsystem.remote.not_linked": "No drone linked",
    "tooltip.airsystem.remote.target": "Target: %s",
    "tooltip.airsystem.remote.no_target": "No target set",
    "tooltip.airsystem.remote.hint": "Right-click a drone to link, right-click air to open the remote",

    "tooltip.airsystem.map.mark": "Marker: %s",
    "tooltip.airsystem.map.hint": "Right-click to open, click the map to copy coordinates",

    "tooltip.airsystem.cable.selected": "Loudspeaker selected: %s %s %s",
    "tooltip.airsystem.cable.hint": "Click a loudspeaker, then the alarm button",

    "message.airsystem.turret.no_ammo": "Out of ammunition",
    "message.airsystem.turret.full": "Magazine is full",
    "message.airsystem.turret.loaded": "Loaded: %s rounds",
    "message.airsystem.turret.auto_on": "Auto mode on — radar is scanning for targets",
    "message.airsystem.turret.auto_off": "Auto mode off",
    "message.airsystem.turret.always_auto":
        "This installation runs fully automatically. Friendly drones are those launched within %s blocks",

    "message.airsystem.cable.siren_selected": "Loudspeaker selected: %s %s %s",
    "message.airsystem.cable.no_siren": "Click a loudspeaker first",
    "message.airsystem.cable.siren_gone": "The selected loudspeaker is gone",
    "message.airsystem.cable.linked": "Linked. Loudspeakers total: %s",
    "message.airsystem.cable.unlinked": "Unlinked. Remaining: %s",

    "message.airsystem.alarm.on": "Alarm raised",
    "message.airsystem.alarm.off": "All clear",
    "message.airsystem.alarm.no_sirens": "No linked loudspeakers nearby",
    "message.airsystem.alarm.raised": "AIR RAID ALERT — loudspeakers active: %s",
    "message.airsystem.alarm.cleared": "All clear — loudspeakers active: %s",

    "message.airsystem.drone.manual_on": "Manual control on: WASD, Space — throttle up, Shift — throttle down",
    "message.airsystem.drone.landing": "Returning to base: the autopilot is bringing the aircraft home",
    "message.airsystem.drone.landing_impossible": "Cannot land: no pad on record or the engine is out",
    "message.airsystem.drone.landing_one_way": "A loitering munition does not land",
    "message.airsystem.drone.landed": "Aircraft landed at %s %s %s",
    "message.airsystem.drone.manual_off": "Manual control off — the drone follows its route",
    "message.airsystem.drone.linked": "Drone linked to the remote: %s",

    "message.airsystem.remote.target_set": "Target set: %s %s %s",
    "message.airsystem.remote.error_no_drone": "Error: no drone linked to this remote",
    "message.airsystem.remote.error_no_target": "Error: target coordinates are not set",
    "message.airsystem.remote.error_lost": "Error: lost contact with the drone",
    "message.airsystem.remote.error_range": "Error: the drone is out of control range",
    "message.airsystem.remote.error_not_grounded": "Error: the drone must be on the ground",
    "message.airsystem.remote.launched": "Launched. Target: %s %s %s",
    "message.airsystem.remote.launched_free": "Launched without a task. Fly it manually",

    "screen.airsystem.drone_feed": "Onboard Feed",
    "screen.airsystem.feed.manual_on": "Manual control: ON",
    "screen.airsystem.feed.manual_off": "Manual control",
    "screen.airsystem.feed.manual_wait": "Switching…",
    "screen.airsystem.feed.strike": "Strike",
    "screen.airsystem.feed.land": "Land",
    "screen.airsystem.feed.returning": "RETURNING TO BASE",
    "screen.airsystem.feed.disconnect": "Disconnect",
    "screen.airsystem.feed.state": "Mode",
    "screen.airsystem.feed.altitude": "Altitude",
    "screen.airsystem.feed.speed": "Speed",
    "screen.airsystem.feed.position": "Position",
    "screen.airsystem.feed.target": "Target",
    "screen.airsystem.feed.distance": "Distance",
    "screen.airsystem.feed.fuel": "Fuel",
    "screen.airsystem.feed.unit.metre": "m",
    "screen.airsystem.feed.unit.speed": "km/h",
    "screen.airsystem.feed.engine_lost": "ENGINE FAILURE",
    "screen.airsystem.feed.damage": "Airframe damage, hits left: %s",
    "screen.airsystem.feed.controls": "W/S — pitch, A/D — heading, Space — throttle up, Shift — throttle down",

    "screen.airsystem.remote": "UAV Remote Control",
    "screen.airsystem.remote.coordinates": "Target coordinates",
    "screen.airsystem.remote.paste": "Paste",
    "screen.airsystem.remote.save": "Save",
    "screen.airsystem.remote.start": "START",
    "screen.airsystem.remote.clipboard_empty": "Clipboard is empty",
    "screen.airsystem.remote.pasted": "Coordinates pasted",
    "screen.airsystem.remote.bad_coordinates": "Invalid coordinate format",
    "screen.airsystem.remote.saved": "Target saved",
    "screen.airsystem.remote.drone_connected": "Drone connected",
    "screen.airsystem.remote.drone_missing": "No drone connected",
    "screen.airsystem.remote.current_target": "Current target: %s",
    "screen.airsystem.remote.no_target": "No target set",
    "screen.airsystem.remote.mode_target": "Target point: required",
    "screen.airsystem.remote.mode_free": "Target point: not required",
    "screen.airsystem.remote.target_not_needed": "Free flight — manual control",
    "screen.airsystem.remote.manual_hint": "Coordinates are not required",
    "screen.airsystem.remote.hint": "Copy coordinates on the world map and paste them here",

    "screen.airsystem.world_map": "World Map",
    "screen.airsystem.map.copy": "Copy",
    "screen.airsystem.map.center": "Center",
    "screen.airsystem.map.scale": "Scale: %s blocks per pixel",
    "screen.airsystem.map.marked": "Marker: %s",
    "screen.airsystem.map.copied": "Coordinates copied to clipboard",
    "screen.airsystem.map.hint": "LMB — mark target, RMB — drag map, wheel — zoom",

    "hud.airsystem.air_raid": "AIR RAID ALERT",
    "hud.airsystem.air_raid.sub": "Take shelter",
    "hud.airsystem.all_clear": "ALL CLEAR",
    "hud.airsystem.all_clear.sub": "The threat has passed",

    "state.airsystem.idle": "On the ground",
    "state.airsystem.launch": "Climbing",
    "state.airsystem.cruise": "En route",
    "state.airsystem.orbit": "Loitering",
    "state.airsystem.dive": "Attack run",
    "state.airsystem.falling": "Falling",
    "entity.airsystem.aerial_bomb": "Guided Aerial Bomb",
    "message.airsystem.drone.no_munitions": "Error: no munitions left",
    "state.airsystem.destroyed": "Destroyed",
    "state.airsystem.rtb": "Returning",
    "state.airsystem.landing": "On approach",
    "state.airsystem.landed": "Landed",
}


def build() -> None:
    for drone_id, (ru, en) in DRONE_NAMES.items():
        RU[f"item.airsystem.{drone_id}"] = ru
        RU[f"entity.airsystem.{drone_id}"] = ru
        RU[f"entity.airsystem.drone_{drone_id}"] = ru
        EN[f"item.airsystem.{drone_id}"] = en
        EN[f"entity.airsystem.{drone_id}"] = en
        EN[f"entity.airsystem.drone_{drone_id}"] = en

    for turret_id, (ru, en) in TURRET_NAMES.items():
        RU[f"item.airsystem.{turret_id}"] = ru
        RU[f"entity.airsystem.{turret_id}"] = ru
        RU[f"entity.airsystem.turret_{turret_id}"] = ru
        EN[f"item.airsystem.{turret_id}"] = en
        EN[f"entity.airsystem.{turret_id}"] = en
        EN[f"entity.airsystem.turret_{turret_id}"] = en

    LANG.mkdir(parents=True, exist_ok=True)
    for name, table in (("ru_ru", RU), ("en_us", EN)):
        with open(LANG / f"{name}.json", "w", encoding="utf-8") as handle:
            json.dump(dict(sorted(table.items())), handle, indent=2, ensure_ascii=False)
            handle.write("\n")
        print(f"  {name}.json — {len(table)} keys")


if __name__ == "__main__":
    print("Language files:")
    build()
