"""Asset integrity: every reference resolves and every key is translated."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/rbxlu/airsystem"
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/airsystem"
DATA = RES / "data/airsystem"

problems: list[str] = []


def fail(message: str) -> None:
    problems.append(message)


def load(path: Path):
    try:
        with open(path, encoding="utf-8") as handle:
            return json.load(handle)
    except Exception as error:
        fail(f"unreadable JSON {path.relative_to(ROOT)}: {error}")
        return None


def resource_path(identifier: str, folder: str, extension: str) -> Path | None:
    if ":" in identifier:
        namespace, path = identifier.split(":", 1)
    else:
        namespace, path = "minecraft", identifier
    if namespace != "airsystem":
        return None
    return ASSETS / folder / f"{path}{extension}"


def check_json_files() -> None:
    for path in list(RES.rglob("*.json")):
        load(path)


def check_entity_assets() -> None:
    drone_ids = re.findall(r'\("([a-z0-9_]+)", "(?:delta|flying_wing|conventional|twin_boom|cruciform|quad_vtol)"',
                           (ROOT / "tools/build_drones.py").read_text(encoding="utf-8"))
    turret_ids = ["gepard", "slinger", "terrahawk_paladin"]

    drone_layers = (SRC / "client/model/DroneLayers.java").read_text(encoding="utf-8")
    turret_layers = (SRC / "client/model/TurretLayers.java").read_text(encoding="utf-8")

    def java_name(identifier: str) -> str:
        return "".join(part.capitalize() for part in identifier.split("_"))

    for drone_id in drone_ids:
        for path in (
            ASSETS / f"textures/entity/drone/{drone_id}.png",
            ASSETS / f"models/item/{drone_id}.json",
            ASSETS / f"textures/item/{drone_id}.png",
        ):
            if not path.exists():
                fail(f"missing file {path.relative_to(ROOT)}")
        if f"createLayer" not in drone_layers and f"create{java_name(drone_id)}()" not in drone_layers:
            fail(f"{drone_id}: no model in DroneLayers.java")
        if f"case {drone_id.upper()} ->" not in drone_layers:
            fail(f"{drone_id}: not handled by DroneLayers.create(DroneKind)")

    if "createAerialBomb()" not in drone_layers:
        fail("no aerial bomb model in DroneLayers.java")
    if not (ASSETS / "textures/entity/drone/aerial_bomb.png").exists():
        fail("no aerial bomb texture")

    for turret_id in turret_ids:
        for path in (
            ASSETS / f"textures/entity/turret/{turret_id}.png",
            ASSETS / f"models/item/{turret_id}.json",
            ASSETS / f"textures/item/{turret_id}.png",
        ):
            if not path.exists():
                fail(f"missing file {path.relative_to(ROOT)}")
        if f"create{java_name(turret_id)}()" not in turret_layers:
            fail(f"{turret_id}: no model in TurretLayers.java")
        for bone in ('"hull"', '"turret"', '"barrels"'):
            if bone not in turret_layers:
                fail(f"turret models are missing bone {bone}")


def check_layer_textures() -> None:
    """The texture size in the LayerDefinition must match the PNG itself."""
    from PIL import Image

    for java_file, folder, prefix in (
        (SRC / "client/model/DroneLayers.java", "textures/entity/drone", ""),
        (SRC / "client/model/TurretLayers.java", "textures/entity/turret", ""),
    ):
        source = java_file.read_text(encoding="utf-8")
        methods = re.findall(r"public static LayerDefinition create(\w+)\(\).*?"
                             r"LayerDefinition\.create\(mesh, (\d+), (\d+)\)",
                             source, re.S)
        names = re.findall(r"case ([A-Z0-9_]+) -> create(\w+)\(\);", source)
        by_method = {method: (int(w), int(h)) for method, w, h in methods}
        for enum_name, method in names:
            texture = ASSETS / folder / f"{enum_name.lower()}{prefix}.png"
            if not texture.exists():
                continue
            with Image.open(texture) as image:
                if image.size != by_method.get(method, image.size):
                    fail(f"{method}: texture {image.size} does not match model {by_method[method]}")


def check_models() -> None:
    for path in (ASSETS / "models").rglob("*.json"):
        model = load(path)
        if not model:
            continue
        for key, value in (model.get("textures") or {}).items():
            if not isinstance(value, str) or value.startswith("#"):
                continue
            texture = resource_path(value, "textures", ".png")
            if texture is not None and not texture.exists():
                fail(f"{path.relative_to(ROOT)}: missing texture {value}")
        parent = model.get("parent")
        if parent and parent.startswith("airsystem:"):
            parent_path = ASSETS / "models" / f"{parent.split(':', 1)[1]}.json"
            if not parent_path.exists():
                fail(f"{path.relative_to(ROOT)}: missing parent model {parent}")

    for path in (ASSETS / "blockstates").glob("*.json"):
        blockstate = load(path)
        if not blockstate:
            continue
        for key, variant in blockstate.get("variants", {}).items():
            model = variant["model"] if isinstance(variant, dict) else variant[0]["model"]
            model_path = ASSETS / "models" / f"{model.split(':', 1)[1]}.json"
            if not model_path.exists():
                fail(f"{path.name}: variant {key} references missing model {model}")


def check_sounds() -> None:
    sounds = load(ASSETS / "sounds.json") or {}
    registered = set(re.findall(r'register\("([a-z0-9_]+)"\)',
                                (SRC / "registry/ModSounds.java").read_text(encoding="utf-8")))
    for name in registered:
        if name not in sounds:
            fail(f"sound {name} is registered in Java but missing from sounds.json")
    for name, entry in sounds.items():
        for sound in entry["sounds"]:
            ogg = ASSETS / "sounds" / f"{sound['name'].split(':', 1)[1]}.ogg"
            if not ogg.exists():
                fail(f"missing sound file {ogg.relative_to(ROOT)}")


def check_lang() -> None:
    ru = load(ASSETS / "lang/ru_ru.json") or {}
    en = load(ASSETS / "lang/en_us.json") or {}

    used: set[str] = set()
    pattern = re.compile(r'translatable\(\s*"([a-zA-Z0-9_.]+)"')
    for path in SRC.rglob("*.java"):
        used.update(pattern.findall(path.read_text(encoding="utf-8")))

    dynamic_prefixes = ("state.airsystem.", "tooltip.airsystem.drone.role_", "entity.airsystem.")
    for key in sorted(used):
        if key.startswith(dynamic_prefixes):
            continue
        if key not in ru:
            fail(f"no Russian translation for key {key}")
        if key not in en:
            fail(f"no English translation for key {key}")

    missing_in_en = set(ru) - set(en)
    if missing_in_en:
        fail(f"keys present only in ru_ru: {sorted(missing_in_en)[:5]}")


def check_data() -> None:
    for path in (DATA / "recipe").glob("*.json"):
        recipe = load(path)
        if not recipe:
            continue
        result = recipe.get("result", {}).get("id", "")
        if result.startswith("airsystem:"):
            item = result.split(":", 1)[1]
            if not (ASSETS / f"models/item/{item}.json").exists():
                fail(f"{path.name}: result {result} has no item model")


def check_detached_parts() -> None:
    """Detached geometry is the usual regression when the airframes change."""
    import io
    import contextlib
    import check_models

    buffer = io.StringIO()
    with contextlib.redirect_stdout(buffer):
        code = check_models.main()
    if code != 0:
        for line in buffer.getvalue().splitlines():
            if "detached" in line:
                fail(line.strip())


def main() -> int:
    check_json_files()
    check_entity_assets()
    check_detached_parts()
    check_layer_textures()
    check_models()
    check_sounds()
    check_lang()
    check_data()

    if problems:
        print(f"Problems found: {len(problems)}")
        for problem in problems:
            print(f"  - {problem}")
        return 1
    print("Assets validated: all references and translations resolve")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
