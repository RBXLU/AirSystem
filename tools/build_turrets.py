"""Anti-aircraft gun models: Gepard, Slinger, Terrahawk Paladin and MANTIS.

Each vehicle is carried by one or two recognisable features — the Gepard's
side-mounted 35 mm guns and stern search radar, the Slinger's single 30 mm
barrel and flat panel antenna, the Paladin's container base and masted radar,
the MANTIS revolver cannon on outriggers. Check a silhouette with render3d.py."""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

import airframes as af
from geolib import Cube, Model, VanillaEmitter, emit_layer_class, _java_name

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/airsystem"
U = 16.0


def rgb(value: int):
    return ((value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF)


shade = af.shade


def build_gepard() -> Model:
    """Tracked SPAAG with twin 35 mm guns and two radars.

    Hull proportions follow the prototype: about twice as long as it is wide,
    seven road wheels, drive sprocket at the rear.
    """
    color = rgb(0x5B6148)
    model = Model("gepard")
    hull = model.bone("hull", pivot=(0, 0, 0))

    half_w = 24.0
    length = 96.0
    hull_half = half_w - 7

    af.tracks(hull, half_w, length, 15.0, 0.0, wheels=7)

    hull.add(Cube(origin=(-hull_half, 9, -length / 2 + 5),
                  size=(2 * hull_half, 12, length - 10), color=color))
    hull.add(Cube(origin=(-hull_half, 21, -length / 2 + 18),
                  size=(2 * hull_half, 5, length - 30), color=af.shade(color, 1.05)))
    af.glacis(hull, hull_half, 9, 16, -length / 2 + 3, 15, af.shade(color, 1.06), steps=3)
    af.glacis(hull, hull_half, 9, 12, length / 2 + 1, 9, af.shade(color, 0.94), steps=3,
              backwards=True)

    hull.add(Cube(origin=(-9, 26, -length / 2 + 20), size=(18, 2, 11), color=af.shade(color, 0.9)))
    hull.add(Cube(origin=(-8, 26, length / 2 - 24), size=(16, 2, 16), color=(52, 52, 50),
                  detail="metal"))
    hull.add(Cube(origin=(hull_half - 4, 22, length / 2 - 18), size=(4, 5, 12),
                  color=(48, 46, 44), detail="dark"))
    for side in (-1, 1):
        x = half_w - 6 if side > 0 else -half_w
        af.stowage(hull, x, 15, -length / 2 + 26, 6, 6, 24, color)
        af.stowage(hull, x, 15, length / 2 - 34, 6, 6, 20, color)

    turret = model.bone("turret", pivot=(0, 26, 0), parent="hull")
    t_half = 17.0
    af.faceted_box(turret, t_half, 26, 17, -22, 22, af.shade(color, 1.04), chamfer=2.0)
    af.glacis(turret, t_half - 2, 26, 15, -24, 7, af.shade(color, 1.09), steps=3)
    turret.add(Cube(origin=(-t_half + 4, 43, -20), size=(2 * (t_half - 4), 2, 40),
                    color=af.shade(color, 0.92)))
    turret.add(Cube(origin=(-11, 45, -4), size=(10, 2, 10), color=af.shade(color, 0.88)))

    turret.add(Cube(origin=(4, 45, -14), size=(8, 5, 6), color=af.shade(color, 0.85)))
    turret.add(Cube(origin=(5, 47, -15), size=(6, 2, 1), color=(52, 84, 104), detail="glass"))

    barrels = model.bone("barrels", pivot=(0, 34, -18), parent="turret")
    for side in (-1, 1):
        x = t_half if side > 0 else -t_half - 8
        barrels.add(Cube(origin=(x, 30, -14), size=(8, 11, 26), color=af.shade(color, 0.98)))
        barrels.add(Cube(origin=(x + 1, 38, -4), size=(6, 5, 16), color=af.shade(color, 0.88)))
        af.gun_barrel(barrels, x + 1, 33, -14, 44, 4)
    barrels.add(Cube(origin=(-t_half, 33, -10), size=(2 * t_half, 6, 12),
                     color=af.shade(color, 0.9)))

    radar = model.bone("radar", pivot=(0, 45, 16), parent="turret")
    radar.add(Cube(origin=(-4, 44, 13), size=(8, 6, 7), color=af.shade(color, 0.8)))
    af.dish_radar(radar, 28, 18, 50, 16, (82, 86, 76), tilt_deg=-16.0)
    turret.add(Cube(origin=(-4, 44, -19), size=(8, 5, 5), color=af.shade(color, 0.84)))
    af.radar_panel(turret, 20, 14, 49, -18, (78, 82, 72), tilt_deg=-22.0)
    return model


def build_slinger() -> Model:
    """Compact remote weapon station with a 30 mm cannon."""
    color = rgb(0x6B6F5E)
    model = Model("slinger")
    base = model.bone("hull", pivot=(0, 0, 0))

    half_w = 17.0
    length = 44.0

    base.add(Cube(origin=(-half_w, 2, -length / 2), size=(half_w * 2, 8, length),
                  color=af.shade(color, 0.82)))
    for dx in (-1, 1):
        for dz in (-1, 1):
            x = dx * (half_w - 6) - (5 if dx > 0 else 0)
            z = dz * (length / 2 - 8) - (5 if dz > 0 else 0)
            base.add(Cube(origin=(x, 0, z), size=(5, 4, 5), color=(44, 44, 46), detail="dark"))
    base.add(Cube(origin=(-half_w + 4, 10, -length / 2 + 6), size=(half_w * 2 - 8, 6, length - 12),
                  color=color))
    af.stowage(base, -half_w + 2, 10, length / 2 - 16, 6, 8, 12, color)
    base.add(Cube(origin=(-8, 16, -8), size=(16, 2, 16), color=af.shade(color, 0.78),
                  detail="metal"))

    turret = model.bone("turret", pivot=(0, 18, 0), parent="hull")
    af.faceted_box(turret, 10, 18, 12, -10, 10, af.shade(color, 1.06), chamfer=2.0)
    turret.add(Cube(origin=(-7, 30, -6), size=(14, 2, 13), color=af.shade(color, 0.9)))
    turret.add(Cube(origin=(10, 21, -11), size=(6, 8, 10), color=af.shade(color, 0.88)))
    turret.add(Cube(origin=(11, 24, -12), size=(4, 4, 1), color=(52, 84, 104), detail="glass"))
    turret.add(Cube(origin=(-16, 20, -3), size=(6, 10, 15), color=af.shade(color, 0.86)))

    barrels = model.bone("barrels", pivot=(0, 24, -10), parent="turret")
    barrels.add(Cube(origin=(-6, 20, -13), size=(12, 9, 15), color=af.shade(color, 0.98)))
    af.gun_barrel(barrels, -2.5, 22, -13, 38, 4)

    radar = model.bone("radar", pivot=(0, 31, 8), parent="turret")
    radar.add(Cube(origin=(-3, 29, 7), size=(6, 4, 4), color=af.shade(color, 0.8)))
    af.radar_panel(radar, 16, 11, 32, 8, (78, 82, 72))
    return model


def build_terrahawk() -> Model:
    """Container-based mount with a 360-degree radar and a 30 mm cannon."""
    color = rgb(0x7A7468)
    model = Model("terrahawk_paladin")
    base = model.bone("hull", pivot=(0, 0, 0))

    half_w = 21.0
    length = 52.0

    base.add(Cube(origin=(-half_w, 4, -length / 2), size=(half_w * 2, 20, length),
                  color=af.shade(color, 0.86)))
    for start, span in af._tiles(-length / 2 + 3, length - 6, 8):
        for x in (-half_w - 1, half_w):
            base.add(Cube(origin=(x, 6, start), size=(1, 16, max(2.0, span - 2)),
                          color=af.shade(color, 0.78), detail="panel"))
    base.add(Cube(origin=(-half_w + 3, 24, -length / 2 + 3), size=(half_w * 2 - 6, 5, length - 6),
                  color=color))
    for dx in (-1, 1):
        for dz in (-1, 1):
            x = dx * (half_w - 5) - (4 if dx > 0 else 0)
            z = dz * (length / 2 - 7) - (4 if dz > 0 else 0)
            base.add(Cube(origin=(x, 0, z), size=(4, 5, 4), color=(52, 52, 54), detail="dark"))
    base.add(Cube(origin=(-7, 6, length / 2 - 1), size=(14, 14, 1), color=af.shade(color, 0.74),
                  detail="panel"))

    turret = model.bone("turret", pivot=(0, 29, 0), parent="hull")
    af.faceted_box(turret, 13, 29, 15, -14, 14, af.shade(color, 1.05), chamfer=2.0)
    af.glacis(turret, 11, 29, 14, -16, 7, af.shade(color, 1.09), steps=3)
    turret.add(Cube(origin=(-10, 44, -10), size=(20, 2, 20), color=af.shade(color, 0.92)))
    turret.add(Cube(origin=(13, 32, -16), size=(6, 10, 12), color=af.shade(color, 0.88)))
    turret.add(Cube(origin=(14, 35, -17), size=(4, 5, 1), color=(52, 84, 104), detail="glass"))

    barrels = model.bone("barrels", pivot=(0, 36, -14), parent="turret")
    barrels.add(Cube(origin=(-7, 32, -18), size=(14, 10, 18), color=af.shade(color, 0.98)))
    af.gun_barrel(barrels, -3, 34, -18, 44, 4)

    radar = model.bone("radar", pivot=(0, 46, 0), parent="turret")
    radar.add(Cube(origin=(-3, 46, -3), size=(6, 9, 6), color=af.shade(color, 0.82)))
    af.dish_radar(radar, 22, 13, 54, 0, (86, 90, 80), tilt_deg=-12.0)
    return model


def build_mantis() -> Model:
    """MANTIS: unmanned mount with a 35 mm revolver cannon.

    A low faceted module on an outrigger pedestal rather than a turret with a
    big dish — search radar for the system lives on a separate post.
    """
    color = rgb(0x4A5348)
    model = Model("mantis")
    base = model.bone("hull", pivot=(0, 0, 0))

    half_w = 20.0
    length = 40.0

    base.add(Cube(origin=(-half_w, 3, -length / 2), size=(half_w * 2, 7, length),
                  color=af.shade(color, 0.84)))
    for dx in (-1, 1):
        for dz in (-1, 1):
            x = dx * (half_w + 1) - (6 if dx > 0 else 0)
            z = dz * (length / 2 - 4) - (6 if dz > 0 else 0)
            base.add(Cube(origin=(half_w - 2 if dx > 0 else -half_w + 2 - 4, 5, z + 1),
                          size=(6, 4, 4), color=af.shade(color, 0.78), detail="panel"))
            base.add(Cube(origin=(x, 0, z), size=(6, 5, 6), color=(46, 46, 48), detail="dark"))

    base.add(Cube(origin=(-12, 10, -14), size=(24, 10, 28), color=color))
    af.stowage(base, -half_w + 1, 10, length / 2 - 18, 7, 12, 16, color)
    af.stowage(base, half_w - 8, 10, length / 2 - 18, 7, 12, 16, color)
    base.add(Cube(origin=(-3, 20, 10), size=(6, 3, 6), color=af.shade(color, 0.8), detail="panel"))
    base.add(Cube(origin=(-1.5, 23, 11.5), size=(3, 3, 3), color=(168, 62, 40), detail="glass"))

    turret = model.bone("turret", pivot=(0, 20, 0), parent="hull")
    af.faceted_box(turret, 11, 20, 14, -12, 12, af.shade(color, 1.05), chamfer=2.0)
    af.glacis(turret, 9, 20, 13, -14, 6, af.shade(color, 1.09), steps=3)
    turret.add(Cube(origin=(-8, 34, -8), size=(16, 2, 16), color=af.shade(color, 0.9)))
    turret.add(Cube(origin=(11, 23, -14), size=(6, 9, 11), color=af.shade(color, 0.86)))
    turret.add(Cube(origin=(12, 26, -15), size=(4, 4, 1), color=(52, 84, 104), detail="glass"))
    turret.add(Cube(origin=(12, 31, -15), size=(3, 2, 1), color=(120, 52, 44), detail="glass"))
    turret.add(Cube(origin=(-17, 22, -4), size=(6, 11, 18), color=af.shade(color, 0.88)))
    turret.add(Cube(origin=(-10, 20, 12), size=(20, 8, 2), color=(58, 60, 58), detail="metal"))

    barrels = model.bone("barrels", pivot=(0, 27, -12), parent="turret")
    barrels.add(Cube(origin=(-7, 23, -16), size=(14, 11, 18), color=af.shade(color, 0.98)))
    af.gun_barrel(barrels, -3, 25, -16, 52, 5)
    barrels.add(Cube(origin=(-4, 24, -28), size=(8, 8, 8), color=af.shade(color, 0.82),
                     detail="metal"))

    radar = model.bone("radar", pivot=(0, 36, 0), parent="turret")
    radar.add(Cube(origin=(-4, 36, -4), size=(8, 5, 8), color=af.shade(color, 0.82)))
    radar.add(Cube(origin=(-6, 41, -3), size=(12, 6, 6), color=af.shade(color, 0.9),
                   detail="panel"))
    radar.add(Cube(origin=(-5, 43, -4), size=(4, 3, 1), color=(52, 84, 104), detail="glass"))
    af.antenna(radar, 0, 47, 7, color=(72, 76, 70))
    return model


BUILDERS = {
    "gepard": build_gepard,
    "slinger": build_slinger,
    "terrahawk_paladin": build_terrahawk,
    "mantis": build_mantis,
}

JAVA_DIR = ROOT / "src/main/java/com/rbxlu/airsystem/client/model"


def build() -> None:
    methods = []
    dispatch_cases = []

    for index, (turret_id, builder) in enumerate(BUILDERS.items()):
        model = builder()
        texture_size = model.pack_uv()
        suffix = _java_name(turret_id)

        methods.append(VanillaEmitter(model, texture_size).emit_method(suffix))
        dispatch_cases.append(f"            case {turret_id.upper()} -> create{suffix}();")

        image = model.render_texture(texture_size, seed=2000 + index)
        path = ASSETS / f"textures/entity/turret/{turret_id}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path)

        print(f"  {turret_id:<20} texture {texture_size}x{texture_size}, "
              f"cubes: {len(model.all_cubes())}")

    dispatch = "\n".join([
        "    /** Model for one turret type. */",
        "    public static LayerDefinition create(TurretKind kind) {",
        "        return switch (kind) {",
        *dispatch_cases,
        "        };",
        "    }",
    ])

    JAVA_DIR.mkdir(parents=True, exist_ok=True)
    source = emit_layer_class(
        "com.rbxlu.airsystem.client.model",
        "TurretLayers",
        "tools/build_turrets.py",
        methods,
        dispatch,
        extra_imports=["com.rbxlu.airsystem.content.turret.TurretKind"],
    )
    (JAVA_DIR / "TurretLayers.java").write_text(source, encoding="utf-8")
    print(f"  TurretLayers.java — {len(methods)} models")


if __name__ == "__main__":
    print("Building turret models:")
    build()
