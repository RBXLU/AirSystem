"""Item icons, block models, blockstates, tags and loot tables."""
from __future__ import annotations

import json
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).parent))
from build_drones import DRONES

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/airsystem"
DATA = ROOT / "src/main/resources/data/airsystem"

TURRETS = [
    ("gepard", 0x5B6148),
    ("slinger", 0x6B6F5E),
    ("terrahawk_paladin", 0x7A7468),
    ("mantis", 0x4A5348),
]


def rgb(value: int):
    return ((value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF)


def shade(color, factor: float):
    return tuple(max(0, min(255, int(c * factor))) for c in color)


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def new_icon() -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    return image, ImageDraw.Draw(image)


def save_icon(image: Image.Image, name: str, folder: str = "item") -> None:
    path = ASSETS / f"textures/{folder}/{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def drone_icon(archetype: str, color) -> Image.Image:
    image, draw = new_icon()
    light = shade(color, 1.15) + (255,)
    body = color + (255,)
    dark = shade(color, 0.6) + (255,)

    if archetype == "delta":
        draw.polygon([(8, 1), (14, 13), (8, 11), (2, 13)], fill=body, outline=dark)
        draw.line([(8, 2), (8, 11)], fill=light)
        draw.point((2, 13), fill=dark)
        draw.point((14, 13), fill=dark)
    elif archetype == "flying_wing":
        draw.polygon([(8, 2), (15, 10), (12, 12), (8, 9), (4, 12), (1, 10)], fill=body, outline=dark)
        draw.line([(8, 3), (8, 9)], fill=light)
    elif archetype == "cruciform":
        draw.line([(2, 2), (14, 14)], fill=body, width=2)
        draw.line([(14, 2), (2, 14)], fill=body, width=2)
        draw.rectangle([7, 3, 8, 13], fill=shade(color, 0.85) + (255,), outline=dark)
        draw.point((7, 3), fill=(40, 40, 44, 255))
    elif archetype == "twin_boom":
        draw.rectangle([7, 3, 8, 11], fill=body, outline=dark)
        draw.rectangle([1, 7, 14, 8], fill=body, outline=dark)
        draw.line([(4, 8), (4, 14)], fill=shade(color, 0.9) + (255,))
        draw.line([(11, 8), (11, 14)], fill=shade(color, 0.9) + (255,))
        draw.line([(4, 13), (11, 13)], fill=dark)
    elif archetype == "quad_vtol":
        draw.rectangle([7, 2, 8, 12], fill=body, outline=dark)
        draw.rectangle([1, 6, 14, 7], fill=body, outline=dark)
        for x in (3, 12):
            for y in (3, 11):
                draw.ellipse([x - 2, y - 2, x + 1, y + 1], outline=dark, fill=shade(color, 0.75) + (255,))
    else:
        draw.rectangle([7, 1, 8, 13], fill=body, outline=dark)
        draw.rectangle([1, 6, 14, 7], fill=body, outline=dark)
        draw.rectangle([4, 12, 11, 12], fill=shade(color, 0.9) + (255,))
        draw.point((7, 1), fill=(40, 40, 44, 255))
        draw.point((8, 1), fill=(40, 40, 44, 255))
    return image


def turret_icon(color) -> Image.Image:
    image, draw = new_icon()
    body = color + (255,)
    dark = shade(color, 0.6) + (255,)
    metal = (70, 72, 68, 255)

    draw.rectangle([1, 10, 14, 14], fill=shade(color, 0.8) + (255,), outline=dark)
    for x in (3, 7, 11):
        draw.rectangle([x, 13, x + 1, 14], fill=(40, 40, 42, 255))
    draw.rectangle([4, 6, 11, 10], fill=body, outline=dark)
    draw.line([(4, 6), (1, 3)], fill=metal, width=2)
    draw.line([(5, 7), (2, 4)], fill=metal, width=1)
    draw.rectangle([10, 3, 13, 6], fill=(96, 100, 92, 255), outline=dark)
    return image


def world_map_icon() -> Image.Image:
    image, draw = new_icon()
    draw.rectangle([1, 1, 14, 14], fill=(214, 199, 162, 255), outline=(120, 104, 74, 255))
    draw.rectangle([2, 4, 6, 9], fill=(122, 152, 96, 255))
    draw.rectangle([8, 2, 13, 6], fill=(110, 142, 92, 255))
    draw.rectangle([3, 10, 12, 13], fill=(96, 128, 168, 255))
    for i in range(2, 15, 4):
        draw.line([(i, 1), (i, 14)], fill=(180, 166, 132, 255))
        draw.line([(1, i), (14, i)], fill=(180, 166, 132, 255))
    draw.line([(9, 8), (13, 12)], fill=(190, 40, 40, 255), width=1)
    draw.line([(13, 8), (9, 12)], fill=(190, 40, 40, 255), width=1)
    return image


def remote_control_icon() -> Image.Image:
    image, draw = new_icon()
    draw.rectangle([2, 4, 13, 14], fill=(58, 62, 60, 255), outline=(30, 32, 31, 255))
    draw.rectangle([4, 6, 11, 10], fill=(46, 122, 74, 255), outline=(24, 60, 38, 255))
    draw.line([(5, 8), (10, 8)], fill=(126, 226, 126, 255))
    draw.line([(5, 9), (8, 9)], fill=(96, 186, 96, 255))
    draw.rectangle([4, 12, 5, 13], fill=(180, 60, 60, 255))
    draw.rectangle([7, 12, 8, 13], fill=(160, 160, 160, 255))
    draw.line([(4, 4), (2, 1)], fill=(140, 140, 140, 255))
    draw.line([(11, 4), (13, 1)], fill=(140, 140, 140, 255))
    return image


def linking_cable_icon() -> Image.Image:
    image, draw = new_icon()
    for offset in range(0, 4):
        y = 3 + offset * 3
        draw.arc([3, y, 12, y + 4], start=0, end=360, fill=(196, 128, 40, 255), width=2)
    draw.rectangle([2, 2, 4, 4], fill=(160, 160, 168, 255))
    draw.rectangle([11, 12, 13, 14], fill=(160, 160, 168, 255))
    return image


def shell_icon(tall: bool) -> Image.Image:
    image, draw = new_icon()
    top = 2 if tall else 4
    draw.rectangle([6, top + 3, 9, 14], fill=(184, 148, 62, 255), outline=(120, 94, 36, 255))
    draw.polygon([(6, top + 3), (7, top), (8, top), (9, top + 3)], fill=(150, 62, 48, 255))
    draw.line([(6, 12), (9, 12)], fill=(120, 94, 36, 255))
    return image


def module_icon(kind: str) -> Image.Image:
    image, draw = new_icon()
    if kind == "frame":
        draw.rectangle([2, 5, 13, 10], outline=(150, 150, 154, 255))
        draw.line([(2, 5), (13, 10)], fill=(120, 120, 124, 255))
        draw.line([(13, 5), (2, 10)], fill=(120, 120, 124, 255))
    elif kind == "engine":
        draw.rectangle([4, 5, 11, 11], fill=(96, 98, 94, 255), outline=(52, 54, 50, 255))
        draw.line([(8, 2), (8, 14)], fill=(40, 40, 42, 255), width=2)
        draw.ellipse([6, 7, 9, 10], fill=(140, 142, 138, 255))
    elif kind == "warhead":
        draw.polygon([(8, 1), (12, 8), (12, 14), (4, 14), (4, 8)], fill=(80, 82, 76, 255),
                     outline=(40, 42, 38, 255))
        draw.rectangle([4, 9, 12, 10], fill=(178, 48, 40, 255))
    elif kind == "camera":
        draw.ellipse([3, 3, 12, 12], fill=(46, 48, 52, 255), outline=(24, 26, 28, 255))
        draw.ellipse([6, 6, 10, 10], fill=(72, 132, 168, 255))
        draw.point((7, 7), fill=(220, 240, 255, 255))
    else:
        draw.rectangle([2, 3, 13, 12], fill=(38, 96, 62, 255), outline=(20, 52, 34, 255))
        for x in range(4, 12, 3):
            draw.line([(x, 4), (x, 11)], fill=(196, 176, 84, 255))
        draw.rectangle([6, 6, 9, 9], fill=(24, 24, 26, 255))
    return image


def metal_texture(base, bolts: bool = True) -> Image.Image:
    image = Image.new("RGBA", (16, 16), base + (255,))
    draw = ImageDraw.Draw(image)
    for y in range(0, 16, 4):
        draw.line([(0, y), (15, y)], fill=shade(base, 0.88) + (255,))
    if bolts:
        for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
            draw.point((x, y), fill=shade(base, 0.6) + (255,))
    return image


def horn_texture() -> Image.Image:
    base = (108, 110, 104)
    image = Image.new("RGBA", (16, 16), base + (255,))
    draw = ImageDraw.Draw(image)
    for radius, factor in ((7, 0.75), (5, 0.6), (3, 0.45), (1, 0.3)):
        draw.ellipse([8 - radius, 8 - radius, 8 + radius, 8 + radius],
                     outline=shade(base, factor) + (255,))
    return image


def lamp_texture(on: bool) -> Image.Image:
    color = (232, 68, 52) if on else (96, 40, 34)
    image = Image.new("RGBA", (16, 16), shade(color, 0.5) + (255,))
    draw = ImageDraw.Draw(image)
    draw.ellipse([3, 3, 12, 12], fill=color + (255,), outline=shade(color, 0.6) + (255,))
    if on:
        draw.ellipse([6, 5, 9, 8], fill=(255, 216, 200, 255))
    return image


def panel_texture() -> Image.Image:
    base = (72, 74, 78)
    image = metal_texture(base)
    draw = ImageDraw.Draw(image)
    draw.rectangle([3, 3, 12, 8], fill=(38, 40, 44, 255), outline=(24, 26, 28, 255))
    draw.line([(4, 5), (11, 5)], fill=(120, 190, 120, 255))
    draw.line([(4, 7), (9, 7)], fill=(90, 150, 90, 255))
    return image


def button_texture(pressed: bool) -> Image.Image:
    base = (72, 74, 78)
    image = metal_texture(base, bolts=False)
    draw = ImageDraw.Draw(image)
    color = (240, 90, 70) if pressed else (168, 44, 36)
    draw.ellipse([4, 4, 11, 11], fill=color + (255,), outline=(40, 20, 18, 255))
    if pressed:
        draw.ellipse([6, 5, 8, 7], fill=(255, 210, 200, 255))
    return image


def siren_model(active: bool) -> dict:
    lamp = "airsystem:block/siren_lamp_on" if active else "airsystem:block/siren_lamp_off"
    return {
        "credit": "Project AirSystem",
        "textures": {
            "body": "airsystem:block/siren_body",
            "horn": "airsystem:block/siren_horn",
            "lamp": lamp,
            "particle": "airsystem:block/siren_body",
        },
        "elements": [
            {
                "from": [6, 0, 6], "to": [10, 7, 10],
                "faces": {
                    "north": {"uv": [0, 0, 4, 7], "texture": "#body"},
                    "east": {"uv": [0, 0, 4, 7], "texture": "#body"},
                    "south": {"uv": [0, 0, 4, 7], "texture": "#body"},
                    "west": {"uv": [0, 0, 4, 7], "texture": "#body"},
                    "up": {"uv": [0, 0, 4, 4], "texture": "#body"},
                    "down": {"uv": [0, 0, 4, 4], "texture": "#body"},
                },
            },
            {
                "from": [4, 7, 5], "to": [12, 14, 11],
                "faces": {
                    "north": {"uv": [0, 0, 16, 14], "texture": "#horn"},
                    "east": {"uv": [0, 0, 12, 14], "texture": "#body"},
                    "south": {"uv": [0, 0, 16, 14], "texture": "#body"},
                    "west": {"uv": [0, 0, 12, 14], "texture": "#body"},
                    "up": {"uv": [0, 0, 16, 12], "texture": "#body"},
                    "down": {"uv": [0, 0, 16, 12], "texture": "#body"},
                },
            },
            {
                "from": [5, 8, 1], "to": [11, 13, 5],
                "faces": {
                    "north": {"uv": [0, 0, 16, 14], "texture": "#horn"},
                    "east": {"uv": [0, 0, 10, 14], "texture": "#body"},
                    "south": {"uv": [0, 0, 16, 14], "texture": "#body"},
                    "west": {"uv": [0, 0, 10, 14], "texture": "#body"},
                    "up": {"uv": [0, 0, 16, 10], "texture": "#body"},
                    "down": {"uv": [0, 0, 16, 10], "texture": "#body"},
                },
            },
            {
                "from": [7, 14, 7], "to": [9, 16, 9],
                "faces": {
                    "north": {"uv": [0, 0, 8, 8], "texture": "#lamp"},
                    "east": {"uv": [0, 0, 8, 8], "texture": "#lamp"},
                    "south": {"uv": [0, 0, 8, 8], "texture": "#lamp"},
                    "west": {"uv": [0, 0, 8, 8], "texture": "#lamp"},
                    "up": {"uv": [0, 0, 8, 8], "texture": "#lamp"},
                },
            },
        ],
    }


def alarm_button_model(triggered: bool) -> dict:
    button = "airsystem:block/alarm_button_on" if triggered else "airsystem:block/alarm_button_off"
    return {
        "credit": "Project AirSystem",
        "textures": {
            "panel": "airsystem:block/alarm_panel",
            "button": button,
            "particle": "airsystem:block/alarm_panel",
        },
        "elements": [
            {
                "from": [2, 0, 2], "to": [14, 2, 14],
                "faces": {
                    "north": {"uv": [0, 0, 12, 2], "texture": "#panel"},
                    "east": {"uv": [0, 0, 12, 2], "texture": "#panel"},
                    "south": {"uv": [0, 0, 12, 2], "texture": "#panel"},
                    "west": {"uv": [0, 0, 12, 2], "texture": "#panel"},
                    "up": {"uv": [0, 0, 12, 12], "texture": "#panel"},
                    "down": {"uv": [0, 0, 12, 12], "texture": "#panel"},
                },
            },
            {
                "from": [3, 2, 4], "to": [13, 12, 12],
                "rotation": {"angle": -22.5, "axis": "x", "origin": [8, 2, 8]},
                "faces": {
                    "north": {"uv": [0, 0, 16, 16], "texture": "#button"},
                    "east": {"uv": [0, 0, 12, 16], "texture": "#panel"},
                    "south": {"uv": [0, 0, 16, 16], "texture": "#panel"},
                    "west": {"uv": [0, 0, 12, 16], "texture": "#panel"},
                    "up": {"uv": [0, 0, 16, 12], "texture": "#panel"},
                    "down": {"uv": [0, 0, 16, 12], "texture": "#panel"},
                },
            },
        ],
    }


FACING_ROTATION = {"north": 0, "east": 90, "south": 180, "west": 270}


def directional_blockstate(model_off: str, model_on: str, property_name: str) -> dict:
    variants = {}
    for facing, rotation in FACING_ROTATION.items():
        for state, model in ((False, model_off), (True, model_on)):
            key = f"{property_name}={'true' if state else 'false'},facing={facing}"
            variant = {"model": model}
            if rotation:
                variant["y"] = rotation
            variants[key] = variant
    return {"variants": variants}


def build_items() -> None:
    print("Item icons:")
    for drone_id, archetype, _length, _span, color, _params in DRONES:
        save_icon(drone_icon(archetype, rgb(color)), drone_id)
        write_json(ASSETS / f"models/item/{drone_id}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"airsystem:item/{drone_id}"},
        })

    for turret_id, color in TURRETS:
        save_icon(turret_icon(rgb(color)), turret_id)
        write_json(ASSETS / f"models/item/{turret_id}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"airsystem:item/{turret_id}"},
        })

    simple = {
        "world_map": world_map_icon(),
        "remote_control": remote_control_icon(),
        "linking_cable": linking_cable_icon(),
        "ammo_35mm": shell_icon(True),
        "ammo_30mm": shell_icon(False),
        "drone_frame": module_icon("frame"),
        "engine_module": module_icon("engine"),
        "warhead_module": module_icon("warhead"),
        "camera_module": module_icon("camera"),
        "guidance_module": module_icon("guidance"),
    }
    for name, image in simple.items():
        save_icon(image, name)
        write_json(ASSETS / f"models/item/{name}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"airsystem:item/{name}"},
        })
    print(f"  done: {len(DRONES) + len(TURRETS) + len(simple)} icons")


def build_blocks() -> None:
    print("Alarm system blocks:")
    save_icon(metal_texture((120, 122, 118)), "siren_body", folder="block")
    save_icon(horn_texture(), "siren_horn", folder="block")
    save_icon(lamp_texture(False), "siren_lamp_off", folder="block")
    save_icon(lamp_texture(True), "siren_lamp_on", folder="block")
    save_icon(panel_texture(), "alarm_panel", folder="block")
    save_icon(button_texture(False), "alarm_button_off", folder="block")
    save_icon(button_texture(True), "alarm_button_on", folder="block")

    write_json(ASSETS / "models/block/air_raid_siren.json", siren_model(False))
    write_json(ASSETS / "models/block/air_raid_siren_on.json", siren_model(True))
    write_json(ASSETS / "models/block/alarm_button.json", alarm_button_model(False))
    write_json(ASSETS / "models/block/alarm_button_on.json", alarm_button_model(True))

    write_json(ASSETS / "blockstates/air_raid_siren.json", directional_blockstate(
        "airsystem:block/air_raid_siren", "airsystem:block/air_raid_siren_on", "active"))
    write_json(ASSETS / "blockstates/alarm_button.json", directional_blockstate(
        "airsystem:block/alarm_button", "airsystem:block/alarm_button_on", "triggered"))

    write_json(ASSETS / "models/block/drone.json", {
        "textures": {"particle": "airsystem:block/siren_body"},
    })
    write_json(ASSETS / "blockstates/drone.json", {
        "variants": {
            f"facing={facing}": ({"model": "airsystem:block/drone"} if rotation == 0
                                 else {"model": "airsystem:block/drone", "y": rotation})
            for facing, rotation in FACING_ROTATION.items()
        },
    })

    write_json(ASSETS / "models/item/air_raid_siren.json", {"parent": "airsystem:block/air_raid_siren"})
    write_json(ASSETS / "models/item/alarm_button.json", {"parent": "airsystem:block/alarm_button"})
    print("  models, blockstates and textures written")


def build_data() -> None:
    print("Data (tags, loot, recipes):")
    write_json(DATA / "tags/block/shockwave_fragile.json", {
        "replace": False,
        "values": [
            "#minecraft:impermeable",
            "minecraft:glass_pane",
            "minecraft:tinted_glass",
            "minecraft:flower_pot",
            {"id": "minecraft:white_stained_glass_pane", "required": False},
            {"id": "minecraft:light_gray_stained_glass_pane", "required": False},
            {"id": "minecraft:gray_stained_glass_pane", "required": False},
            {"id": "minecraft:black_stained_glass_pane", "required": False},
            {"id": "minecraft:brown_stained_glass_pane", "required": False},
            {"id": "minecraft:red_stained_glass_pane", "required": False},
            {"id": "minecraft:orange_stained_glass_pane", "required": False},
            {"id": "minecraft:yellow_stained_glass_pane", "required": False},
            {"id": "minecraft:lime_stained_glass_pane", "required": False},
            {"id": "minecraft:green_stained_glass_pane", "required": False},
            {"id": "minecraft:cyan_stained_glass_pane", "required": False},
            {"id": "minecraft:light_blue_stained_glass_pane", "required": False},
            {"id": "minecraft:blue_stained_glass_pane", "required": False},
            {"id": "minecraft:purple_stained_glass_pane", "required": False},
            {"id": "minecraft:magenta_stained_glass_pane", "required": False},
            {"id": "minecraft:pink_stained_glass_pane", "required": False},
        ],
    })

    write_json(DATA / "loot_table/blocks/drone.json", {
        "type": "minecraft:block",
        "pools": [],
    })

    for block in ("air_raid_siren", "alarm_button"):
        write_json(DATA / f"loot_table/blocks/{block}.json", {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "bonus_rolls": 0,
                "entries": [{"type": "minecraft:item", "name": f"airsystem:{block}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }],
        })

    write_json(DATA / "recipe/remote_control.json", {
        "type": "minecraft:crafting_shaped",
        "category": "equipment",
        "pattern": ["I I", "RGR", "III"],
        "key": {
            "I": {"item": "minecraft:iron_ingot"},
            "R": {"item": "minecraft:redstone"},
            "G": {"item": "minecraft:glass_pane"},
        },
        "result": {"id": "airsystem:remote_control", "count": 1},
    })
    write_json(DATA / "recipe/world_map.json", {
        "type": "minecraft:crafting_shaped",
        "category": "equipment",
        "pattern": [" C ", "PPP", " C "],
        "key": {
            "C": {"item": "minecraft:compass"},
            "P": {"item": "minecraft:paper"},
        },
        "result": {"id": "airsystem:world_map", "count": 1},
    })
    write_json(DATA / "recipe/linking_cable.json", {
        "type": "minecraft:crafting_shaped",
        "category": "equipment",
        "pattern": ["CCC", "RRR", "CCC"],
        "key": {
            "C": {"item": "minecraft:copper_ingot"},
            "R": {"item": "minecraft:redstone"},
        },
        "result": {"id": "airsystem:linking_cable", "count": 4},
    })
    write_json(DATA / "recipe/air_raid_siren.json", {
        "type": "minecraft:crafting_shaped",
        "category": "building",
        "pattern": ["INI", "IRI", "III"],
        "key": {
            "I": {"item": "minecraft:iron_ingot"},
            "N": {"item": "minecraft:note_block"},
            "R": {"item": "minecraft:redstone_block"},
        },
        "result": {"id": "airsystem:air_raid_siren", "count": 1},
    })
    write_json(DATA / "recipe/alarm_button.json", {
        "type": "minecraft:crafting_shaped",
        "category": "building",
        "pattern": ["IBI", "IRI", "III"],
        "key": {
            "I": {"item": "minecraft:iron_ingot"},
            "B": {"item": "minecraft:red_dye"},
            "R": {"item": "minecraft:redstone"},
        },
        "result": {"id": "airsystem:alarm_button", "count": 1},
    })
    write_json(DATA / "recipe/ammo_35mm.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": [" G ", "GIG", " I "],
        "key": {
            "G": {"item": "minecraft:gunpowder"},
            "I": {"item": "minecraft:copper_ingot"},
        },
        "result": {"id": "airsystem:ammo_35mm", "count": 8},
    })
    write_json(DATA / "recipe/ammo_30mm.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": [" G ", "GIG"],
        "key": {
            "G": {"item": "minecraft:gunpowder"},
            "I": {"item": "minecraft:copper_ingot"},
        },
        "result": {"id": "airsystem:ammo_30mm", "count": 8},
    })

    modules = {
        "drone_frame": (["III", "I I", "III"], {"I": {"item": "minecraft:iron_ingot"}}),
        "engine_module": (["ICI", "CRC", "ICI"], {
            "I": {"item": "minecraft:iron_ingot"},
            "C": {"item": "minecraft:copper_ingot"},
            "R": {"item": "minecraft:redstone"},
        }),
        "warhead_module": (["TTT", "TGT", "TTT"], {
            "T": {"item": "minecraft:tnt"},
            "G": {"item": "minecraft:gunpowder"},
        }),
        "camera_module": ([" G ", "GEG", " I "], {
            "G": {"item": "minecraft:glass"},
            "E": {"item": "minecraft:ender_eye"},
            "I": {"item": "minecraft:iron_ingot"},
        }),
        "guidance_module": (["RGR", "GDG", "RGR"], {
            "R": {"item": "minecraft:redstone"},
            "G": {"item": "minecraft:gold_ingot"},
            "D": {"item": "minecraft:compass"},
        }),
    }
    for name, (pattern, key) in modules.items():
        write_json(DATA / f"recipe/{name}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "misc",
            "pattern": pattern,
            "key": key,
            "result": {"id": f"airsystem:{name}", "count": 1},
        })

    from build_drones import DRONES as DRONE_TABLE
    strike = {"shahed_131", "shahed_136", "shahed_238", "lancet_1", "lancet_3", "kub_bla",
              "liutyi", "uj_22", "ram_ii"}
    for drone_id, _archetype, _length, _span, _color, _params in DRONE_TABLE:
        payload = "airsystem:warhead_module" if drone_id in strike else "airsystem:camera_module"
        write_json(DATA / f"recipe/{drone_id}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "equipment",
            "pattern": ["FEF", "FPF", "FGF"],
            "key": {
                "F": {"item": "airsystem:drone_frame"},
                "E": {"item": "airsystem:engine_module"},
                "P": {"item": payload},
                "G": {"item": "airsystem:guidance_module"},
            },
            "result": {"id": f"airsystem:{drone_id}", "count": 1},
        })

    for turret_id, _color in TURRETS:
        write_json(DATA / f"recipe/{turret_id}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "equipment",
            "pattern": ["NGN", "IFI", "III"],
            "key": {
                "N": {"item": "minecraft:netherite_scrap"},
                "G": {"item": "airsystem:guidance_module"},
                "I": {"item": "minecraft:iron_block"},
                "F": {"item": "airsystem:drone_frame"},
            },
            "result": {"id": f"airsystem:{turret_id}", "count": 1},
        })
    print("  tags, loot tables and recipes written")


if __name__ == "__main__":
    build_items()
    build_blocks()
    build_data()
