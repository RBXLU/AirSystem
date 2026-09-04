"""Models and textures for every drone in the mod.

Proportions and layout follow the real aircraft. Check a silhouette with
render3d.py, which rasterises the model and reports projection metrics."""
from __future__ import annotations

import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

import airframes as af
from geolib import Cube, Model, VanillaEmitter, emit_layer_class, _java_name

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/airsystem"

U = 16.0
GROUND = 3.0


def rgb(value: int):
    return ((value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF)


def _i(value: float) -> float:
    """Rounds a size onto the whole-unit grid."""
    return float(max(1, round(value)))


shade = af.shade


def _sweep_for(root_le: float, root_te: float, tip_chord: float, half_span: float) -> float:
    """Leading-edge sweep that puts the tip where it belongs."""
    return math.degrees(math.atan2(root_te - tip_chord - root_le, max(1.0, half_span)))


def delta_wing(model: Model, length: float, span: float, color, jet: bool, fins: bool = True,
               serial: str = ""):
    """Blended delta: the Shahed family and KUB-BLA.

    Pointed nose, straight full-width trailing edge, fins on the tips themselves
    and a pusher propeller on the axis behind the wing.
    """
    L, S = length * U, span * U
    body = model.bone("body", pivot=(0, GROUND, 0))

    half = S / 2
    wing_y = GROUND + 1.0
    root_le = -L * 0.46
    root_te = L * 0.44
    root_chord = root_te - root_le
    tip_chord = max(3.0, L * 0.09)
    sweep_deg = _sweep_for(root_le, root_te, tip_chord, half)
    root_offset = max(2.0, S * 0.07)
    thickness = max(3.0, L * 0.05)

    af.wing(body, root_chord=root_chord, tip_chord=tip_chord, half_span=half,
            sweep_deg=sweep_deg, te_sweep_deg=2.0, thickness=thickness, tip_thickness=1.0,
            y=wing_y, z_leading=root_le, color=color, segments=7, root_offset=root_offset,
            flap_span=(0.15, 0.95), flap_frac=0.22)
    af.delta_centre(body, root_le, root_te, sweep_deg, root_offset + 1.0, thickness,
                    wing_y, color, steps=3)

    hump_h = max(3.0, L * 0.09)
    hump_w = max(4.0, S * 0.17)
    hump_top = wing_y + thickness + hump_h
    sections = af.fuselage(body, L * 0.70, hump_w, hump_h, wing_y + thickness - 1.0, color,
                           sections=4, nose=0.36, tail=0.30, z_center=L * 0.04)
    if serial and sections:
        max(sections, key=lambda cube: (cube.size[1], cube.size[2])).label = serial

    af.nose_cone(body, root_le, hump_w * 0.85, hump_h + thickness * 0.6,
                 wing_y + thickness * 0.5 + 1.0, color, steps=3, depth=L * 0.06,
                 cap_color=af.shade(color, 0.42))
    af.pitot(body, z=root_le, y=wing_y + thickness * 0.5, length=max(2.0, L * 0.04))
    if L >= 40:
        af.marking_band(body, wing_y + thickness, -L * 0.24, hump_w * 2.2, 2.0)
        af.hatch(body, hump_w / 2 - 1, wing_y + thickness, -L * 0.06, L * 0.10, 2.0, color)

    if fins:
        tip_z, tip_len = af.wing_tip(root_chord, tip_chord, half, sweep_deg, root_le,
                                     root_offset, te_sweep_deg=2.0)
        for side in (-1, 1):
            af.fin(body, x=side * (half - 1.0), z=tip_z, chord=tip_len,
                   tip_chord=tip_len * 0.6, height=max(3.0, S * 0.11), thickness=1.0,
                   y=wing_y, color=color, sweep_deg=22.0, segments=2, cant_deg=-18.0 * side)

    if jet:
        af.jet_nacelle(body, z=L * 0.10, y=wing_y + thickness - 1.0,
                       width=hump_w * 0.9, height=hump_h, length=L * 0.32)
        af.exhaust_nozzle(body, z=root_te, y=wing_y + thickness + hump_h * 0.4,
                          size=hump_h * 0.9)
        af.intake(body, wing_y + thickness + hump_h - 1.0, -L * 0.02, hump_w * 0.6, 2.0)
    else:
        hub_z = round(root_te) + 1.0
        engine_z = round(L * 0.22)
        af.engine_pod(body, z=engine_z, y=wing_y + thickness, width=hump_w * 0.8,
                      height=hump_h * 0.8, length=hub_z - engine_z, color=color)
        af.propeller(model, "propeller", "body", hub_z,
                     wing_y + thickness + hump_h * 0.4, S * 0.17)
        if L >= 40:
            af.antenna(body, z=-L * 0.02, y=hump_top - 1.0, height=3.0)
            af.intake(body, wing_y + thickness + 1.0, L * 0.30, hump_w * 0.5)


def flying_wing(model: Model, length: float, span: float, color, jet: bool, fins: bool = True):
    """Flying wing: ZALA, Eleron, S-70.

    Shorter root chord and wider tips than a delta, and wider than it is long —
    that is what reads as a wing rather than an arrowhead.
    """
    L, S = length * U, span * U
    body = model.bone("body", pivot=(0, GROUND, 0))

    half = S / 2
    wing_y = GROUND + 1.0
    root_le = -L * 0.42
    root_te = L * 0.40
    root_chord = root_te - root_le
    tip_chord = max(3.0, root_chord * 0.28)
    sweep_deg = _sweep_for(root_le, root_te * 0.72, tip_chord, half)
    root_offset = max(2.0, S * 0.06)
    thickness = max(2.0, L * 0.10)

    af.wing(body, root_chord=root_chord, tip_chord=tip_chord, half_span=half,
            sweep_deg=sweep_deg, te_sweep_deg=-6.0, thickness=thickness, tip_thickness=1.0,
            y=wing_y, z_leading=root_le, color=color, segments=6, root_offset=root_offset,
            dihedral=0.02, flap_span=(0.35, 0.95), flap_frac=0.26)
    af.delta_centre(body, root_le, root_te, sweep_deg, root_offset + 1.0, thickness,
                    wing_y, color, steps=2)

    pod_w = max(4.0, S * 0.15)
    pod_h = max(3.0, L * 0.13)
    af.fuselage(body, L * 0.66, pod_w, pod_h, wing_y + thickness - 1.0, color,
                sections=4, nose=0.38, tail=0.32, z_center=-L * 0.02)
    af.nose_cone(body, root_le, pod_w * 0.8, pod_h + thickness * 0.5,
                 wing_y + thickness * 0.5 + 1.0, color, steps=3, depth=L * 0.05)

    af.hatch(body, pod_w / 2 - 1, wing_y + thickness, -L * 0.02, L * 0.12, 2.0, color)
    if jet:
        body.add(Cube(origin=(-2.0, round(wing_y) - 1, round(-L * 0.30)), size=(4.0, 1.0, 2.0),
                      color=(58, 96, 124), detail="glass"))
        for z in (-L * 0.22, L * 0.04):
            body.add(Cube(origin=(-pod_w * 0.4, round(wing_y) - 1, round(z)),
                          size=(_i(pod_w * 0.8), 1.0, _i(L * 0.10)),
                          color=af.shade(color, 0.9), detail="panel"))
    else:
        af.gimbal_camera(body, z=-L * 0.16, y=wing_y + 1.0, size=max(3.0, L * 0.10))
        af.pitot(body, z=root_le, y=wing_y + thickness * 0.5, length=max(2.0, L * 0.05))
        af.skid(body, z=-L * 0.10, y=wing_y, width=pod_w * 0.7, length=L * 0.18)

    tip_z, tip_len = af.wing_tip(root_chord, tip_chord, half, sweep_deg, root_le,
                                 root_offset, te_sweep_deg=-6.0)
    if fins:
        for side in (-1, 1):
            af.fin(body, x=side * (half - 1.0), z=tip_z, chord=tip_len * 0.9,
                   tip_chord=tip_len * 0.55, height=max(3.0, S * 0.09), thickness=1.0,
                   y=wing_y, color=color, sweep_deg=24.0, segments=2, cant_deg=-14.0 * side)
    af.nav_light(body, -half, wing_y + 1.0, tip_z + tip_len * 0.4, (188, 62, 54))
    af.nav_light(body, half - 1, wing_y + 1.0, tip_z + tip_len * 0.4, (64, 176, 88))

    if jet:
        af.jet_nacelle(body, z=-L * 0.06, y=wing_y + thickness - 1.0,
                       width=pod_w * 0.9, height=pod_h * 0.9, length=L * 0.34)
        af.exhaust_nozzle(body, z=root_te - L * 0.04, y=wing_y + thickness + pod_h * 0.35,
                          size=pod_h * 0.8)
        af.intake(body, wing_y + thickness + pod_h - 1.0, -L * 0.10, pod_w * 0.6, 2.0)
    else:
        af.engine_pod(body, z=L * 0.16, y=wing_y + thickness, width=pod_w * 0.7,
                      height=pod_h * 0.7, length=L * 0.16, color=color)
        af.propeller(model, "propeller", "body", root_te - L * 0.02,
                     wing_y + thickness + pod_h * 0.3, S * 0.12)


def conventional(model: Model, length: float, span: float, color, prop: str, tail: str,
                 gear: str = "skid"):
    """Conventional layout: fuselage, straight wing, tail.

    A tractor design gets a short body and its tail on a slim boom; a pusher
    carries the tail on the fuselage with the propeller right at the back.
    """
    L, S = length * U, span * U
    body = model.bone("body", pivot=(0, GROUND, 0))

    tractor = prop == "front"
    body_w = max(4.0, L * 0.15)
    body_h = max(4.0, L * 0.17)
    pod_len = L * 0.58 if tractor else L * 0.90
    pod_center = -L * 0.16 if tractor else 0.0

    af.fuselage(body, pod_len, body_w, body_h, GROUND, color,
                sections=5, nose=0.30, tail=0.34, z_center=pod_center)
    nose_z = pod_center - pod_len / 2
    af.nose_cone(body, nose_z - L * 0.06, body_w * 0.8, body_h * 0.8, GROUND + body_h * 0.55,
                 color, steps=2, depth=L * 0.04)

    wing_y = GROUND + body_h * 0.74
    wing_le = pod_center - L * 0.02
    root_chord = L * 0.20
    tip_chord = L * 0.13
    af.wing(body, root_chord=root_chord, tip_chord=tip_chord, half_span=S / 2,
            sweep_deg=4.0, thickness=2.0, y=wing_y, z_leading=wing_le, color=color,
            segments=4, dihedral=0.03, root_offset=body_w * 0.45,
            flap_span=(0.45, 0.95), flap_frac=0.3)
    body.add(Cube(origin=(-body_w * 0.6, wing_y - 1, round(wing_le - 2)),
                  size=(_i(body_w * 1.2), 2.0, _i(root_chord + 4)), color=af.shade(color, 1.05)))

    af.spine_strip(body, GROUND + body_h, pod_center, L * 0.26, max(2.0, body_w * 0.4), color)
    af.hatch(body, body_w / 2 - 1, GROUND + body_h * 0.35, pod_center - L * 0.04,
             L * 0.10, body_h * 0.3, color)
    af.gimbal_camera(body, z=pod_center - L * 0.14, y=GROUND + body_h * 0.14,
                     size=max(3.0, min(L * 0.12, body_h * 0.55)))
    af.pitot(body, z=nose_z - L * 0.06, y=GROUND + body_h * 0.55, length=max(3.0, L * 0.05))

    tail_y = GROUND + body_h * 0.52
    if tractor:
        boom_size = max(2.0, body_h * 0.32)
        af.tail_boom(body, pod_center + pod_len / 2 - 2, L * 0.40, tail_y, boom_size, color)
        tail_z = L * 0.34
    else:
        tail_z = L * 0.30

    if tail == "v":
        af.v_tail(body, half_span=S * 0.20, root_chord=L * 0.16, tip_chord=L * 0.09,
                  thickness=2.0, y=tail_y, z_leading=tail_z, color=color,
                  angle_deg=40.0, segments=3)
    elif tail == "t":
        af.fin(body, x=-0.5, z=tail_z, chord=L * 0.15, tip_chord=L * 0.09,
               height=S * 0.15, thickness=1.0, y=tail_y, color=color, segments=3)
        af.stabilizer(body, half_span=S * 0.17, root_chord=L * 0.11, tip_chord=L * 0.07,
                      thickness=1.0, y=tail_y + S * 0.15, z_leading=tail_z + L * 0.04,
                      color=color)
    else:
        af.fin(body, x=-0.5, z=tail_z, chord=L * 0.16, tip_chord=L * 0.09,
               height=S * 0.14, thickness=1.0, y=tail_y, color=color, segments=3)
        af.stabilizer(body, half_span=S * 0.16, root_chord=L * 0.11, tip_chord=L * 0.07,
                      thickness=1.0, y=tail_y, z_leading=tail_z + L * 0.04, color=color)

    if gear == "tricycle":
        gear_h = wing_y - GROUND + 3.0
        for side in (-1, 1):
            af.gear_leg(body, side * (S * 0.11) - (1.0 if side > 0 else 0.0),
                        pod_center + L * 0.02, wing_y, gear_h)
        af.gear_leg(body, -0.5, nose_z + pod_len * 0.30, GROUND, 3.0)
    else:
        af.skid(body, z=pod_center - L * 0.10, y=GROUND, width=body_w * 0.8, length=L * 0.22)

    tip_z, tip_len = af.wing_tip(root_chord, tip_chord, S / 2, 4.0, wing_le, body_w * 0.45)
    af.nav_light(body, -S / 2, wing_y + 2.0, tip_z + tip_len * 0.3, (188, 62, 54))
    af.nav_light(body, S / 2 - 1, wing_y + 2.0, tip_z + tip_len * 0.3, (64, 176, 88))

    if tractor:
        af.propeller(model, "propeller", "body", nose_z - L * 0.07, GROUND + body_h * 0.55,
                     S * 0.11)
        af.intake(body, GROUND + body_h * 0.55, nose_z, body_w * 0.5)
        af.exhaust(body, body_w / 2 - 2, GROUND + body_h * 0.25, nose_z + L * 0.03, L * 0.06)
    else:
        af.propeller(model, "propeller", "body", pod_len / 2 + 1.0, GROUND + body_h * 0.5,
                     S * 0.11)
        af.intake(body, GROUND + body_h * 0.7, L * 0.26, body_w * 0.5)


def twin_boom(model: Model, length: float, span: float, color):
    """Twin-boom pusher: Leleka, SHARK, Granat."""
    L, S = length * U, span * U
    body = model.bone("body", pivot=(0, GROUND, 0))

    pod_w = max(4.0, L * 0.20)
    pod_h = max(4.0, L * 0.21)
    pod_len = L * 0.58
    pod_center = -L * 0.16

    af.fuselage(body, pod_len, pod_w, pod_h, GROUND, color,
                sections=4, nose=0.34, tail=0.30, z_center=pod_center)
    nose_z = pod_center - pod_len / 2
    af.nose_cone(body, nose_z - L * 0.05, pod_w * 0.72, pod_h * 0.72, GROUND + pod_h * 0.5,
                 color, steps=2, depth=L * 0.04)
    af.gimbal_camera(body, z=pod_center - L * 0.12, y=GROUND + pod_h * 0.16,
                     size=max(3.0, min(L * 0.13, pod_h * 0.55)))
    af.pitot(body, z=nose_z - L * 0.05, y=GROUND + pod_h * 0.5, length=max(3.0, L * 0.05))

    wing_y = GROUND + pod_h * 0.72
    wing_le = pod_center - L * 0.01
    root_chord = L * 0.20
    tip_chord = L * 0.13
    af.wing(body, root_chord=root_chord, tip_chord=tip_chord, half_span=S / 2,
            sweep_deg=4.0, thickness=2.0, y=wing_y, z_leading=wing_le, color=color,
            segments=4, dihedral=0.03, root_offset=pod_w * 0.45,
            flap_span=(0.45, 0.95), flap_frac=0.3)

    boom_x = S * 0.19
    boom_start = wing_le + 2
    boom_end = L * 0.40
    fin_h = max(5.0, S * 0.16)
    for side in (-1, 1):
        x = boom_x if side > 0 else -boom_x - 2.0
        body.add(Cube(origin=(x, wing_y, round(boom_start)), size=(2.0, 2.0,
                      _i(boom_end - boom_start)), color=af.shade(color, 0.92)))
        af.fin(body, x=x, z=boom_end - L * 0.13, chord=L * 0.12, tip_chord=L * 0.08,
               height=fin_h, thickness=2.0, y=wing_y, color=color, segments=2)

    body.add(Cube(origin=(-boom_x - 1.0, round(wing_y + fin_h * 0.60),
                          round(boom_end - L * 0.11)),
                  size=(_i(boom_x * 2 + 3), 1.0, _i(L * 0.10)), color=af.shade(color, 0.96)))

    af.spine_strip(body, GROUND + pod_h, pod_center - L * 0.06, L * 0.24,
                   max(2.0, pod_w * 0.4), color)
    af.skid(body, z=pod_center - L * 0.08, y=GROUND, width=pod_w * 0.8, length=L * 0.20)
    af.antenna(body, z=pod_center + L * 0.10, y=GROUND + pod_h, height=max(3.0, L * 0.07))

    tip_z, tip_len = af.wing_tip(root_chord, tip_chord, S / 2, 4.0, wing_le, pod_w * 0.45)
    af.nav_light(body, -S / 2, wing_y + 2.0, tip_z + tip_len * 0.3, (188, 62, 54))
    af.nav_light(body, S / 2 - 1, wing_y + 2.0, tip_z + tip_len * 0.3, (64, 176, 88))

    hub_z = pod_center + pod_len / 2 + 1.0
    af.engine_pod(body, z=pod_center + pod_len / 2 - L * 0.10, y=GROUND + pod_h * 0.3,
                  width=pod_w * 0.7, height=pod_h * 0.5, length=L * 0.10, color=color)
    af.propeller(model, "propeller", "body", hub_z, GROUND + pod_h * 0.5, S * 0.13)


def cruciform(model: Model, length: float, span: float, color):
    """Lancet: a slim tube with two X wings.

    The body really is a tube — roughly seven times longer than it is wide.
    """
    L, S = length * U, span * U
    body = model.bone("body", pivot=(0, GROUND, 0))

    tube = max(3.0, L * 0.16)
    axis_y = GROUND + tube / 2
    af.fuselage(body, L * 0.72, tube, tube, GROUND, color,
                sections=4, nose=0.10, tail=0.24, z_center=L * 0.10)
    af.ogive_nose(body, -L * 0.52, L * 0.28, tube, axis_y, color, steps=3)
    body.add(Cube(origin=(-1.0, round(axis_y) - 1, -L * 0.53), size=(2.0, 1.0, 1.0),
                  color=(58, 96, 124), detail="glass"))
    af.marking_band(body, GROUND + tube, -L * 0.20, tube, 2.0)
    af.hatch(body, tube / 2 - 1, round(axis_y) - 1, L * 0.02, L * 0.10, 2.0, color)

    for index, (z, scale) in enumerate(((-L * 0.20, 0.72), (L * 0.24, 1.0))):
        arm = (S / 2) * scale
        chord = _i(L * 0.10)
        for angle in (45.0, -45.0):
            body.add(Cube(origin=(-arm, round(axis_y) - 0.5, round(z)),
                          size=(_i(arm * 2), 1.0, chord),
                          color=af.shade(color, 0.98 - 0.03 * index),
                          rotation=(0.0, 0.0, angle),
                          pivot=(0.0, axis_y, z)))

    af.engine_pod(body, z=L * 0.32, y=round(axis_y) - tube * 0.4, width=tube * 0.9,
                  height=tube * 0.8, length=L * 0.12, color=color)
    af.propeller(model, "propeller", "body", L * 0.46, axis_y, S * 0.22)


def quad_vtol(model: Model, length: float, span: float, color):
    """PD-2: an aeroplane with four lift rotors on booms."""
    L, S = length * U, span * U
    body = model.bone("body", pivot=(0, GROUND, 0))

    body_w = max(4.0, L * 0.16)
    body_h = max(4.0, L * 0.17)

    af.fuselage(body, L * 0.86, body_w, body_h, GROUND, color,
                sections=5, nose=0.30, tail=0.32)
    af.nose_cone(body, -L * 0.49, body_w * 0.72, body_h * 0.72, GROUND + body_h * 0.5,
                 color, steps=2, depth=L * 0.04)
    af.gimbal_camera(body, z=-L * 0.26, y=GROUND + body_h * 0.14,
                     size=max(3.0, min(L * 0.12, body_h * 0.55)))
    af.pitot(body, z=-L * 0.50, y=GROUND + body_h * 0.6, length=max(3.0, L * 0.05))

    wing_y = GROUND + body_h * 0.72
    wing_le = -L * 0.12
    root_chord = L * 0.20
    tip_chord = L * 0.13
    af.wing(body, root_chord=root_chord, tip_chord=tip_chord, half_span=S / 2,
            sweep_deg=4.0, thickness=2.0, y=wing_y, z_leading=wing_le, color=color,
            segments=4, dihedral=0.03, root_offset=body_w * 0.45,
            flap_span=(0.45, 0.95), flap_frac=0.3)
    af.v_tail(body, half_span=S * 0.20, root_chord=L * 0.16, tip_chord=L * 0.09,
              thickness=2.0, y=GROUND + body_h * 0.5, z_leading=L * 0.30, color=color,
              angle_deg=40.0, segments=3)

    rotor_z = (-L * 0.30, L * 0.24)
    for side in (-1, 1):
        x = S * 0.21 if side > 0 else -S * 0.21 - 2.0
        body.add(Cube(origin=(x, wing_y - 2.0, round(-L * 0.34)), size=(2.0, 2.0, _i(L * 0.64)),
                      color=af.shade(color, 0.9)))
        for z in rotor_z:
            body.add(Cube(origin=(x - 1.0, wing_y - 2.0, round(z)), size=(4.0, 3.0, 4.0),
                          color=(46, 46, 50), detail="dark"))
        for z in (-L * 0.30, L * 0.22):
            af.gear_leg(body, x + 0.5, z, wing_y - 2.0, 4.0)

    rotor = model.bone("rotor", pivot=(0, wing_y + 1.0, 0), parent="body")
    for side in (-1, 1):
        x = S * 0.21 if side > 0 else -S * 0.21
        for z in rotor_z:
            rotor.add(Cube(origin=(x - S * 0.11, wing_y + 1.0, round(z) + 1.0),
                           size=(_i(S * 0.22), 1.0, 1.0), color=(34, 34, 36), detail="dark"))
            rotor.add(Cube(origin=(x - 0.5, wing_y + 1.0, round(z) + 1.0 - S * 0.11),
                           size=(1.0, 1.0, _i(S * 0.22)), color=(34, 34, 36), detail="dark"))

    af.spine_strip(body, GROUND + body_h, -L * 0.20, L * 0.24, max(2.0, body_w * 0.4), color)
    af.propeller(model, "propeller", "body", -L * 0.50 - L * 0.05, GROUND + body_h * 0.5,
                 S * 0.10)
    af.intake(body, GROUND + body_h * 0.55, -L * 0.42, body_w * 0.5)

    tip_z, tip_len = af.wing_tip(root_chord, tip_chord, S / 2, 4.0, wing_le, body_w * 0.45)
    af.nav_light(body, -S / 2, wing_y + 2.0, tip_z + tip_len * 0.3, (188, 62, 54))
    af.nav_light(body, S / 2 - 1, wing_y + 2.0, tip_z + tip_len * 0.3, (64, 176, 88))


DRONES = [
    ("shahed_131", "delta", 3.5, 3.0, 0xB4A67C, {"jet": False, "serial": "131"}),
    ("shahed_136", "delta", 4.2, 3.0, 0x8E8B72, {"jet": False, "serial": "136"}),
    ("shahed_238", "delta", 4.2, 3.0, 0x3C3F44, {"jet": True, "serial": "238"}),
    ("orlan_10", "conventional", 2.0, 3.1, 0xDCDCD2, {"prop": "front", "tail": "v"}),
    ("orlan_30", "conventional", 2.4, 3.4, 0xC8C8BE, {"prop": "front", "tail": "v"}),
    ("eleron_3", "flying_wing", 1.4, 2.2, 0x6E7A5A, {"jet": False}),
    ("zala_421_16e", "flying_wing", 1.8, 2.8, 0xE0E0D8, {"jet": False}),
    ("zala_421_08", "flying_wing", 1.0, 1.6, 0xD2D2C8, {"jet": False}),
    ("granat_4", "twin_boom", 2.2, 3.2, 0xA8AC96, {}),
    ("lancet_1", "cruciform", 1.1, 1.0, 0x4E5B44, {}),
    ("lancet_3", "cruciform", 1.6, 1.4, 0x44503C, {}),
    ("kub_bla", "delta", 1.3, 1.5, 0x5A5F52, {"jet": False, "fins": False}),
    ("orion", "conventional", 5.0, 8.0, 0xB0B4A4, {"prop": "rear", "tail": "v",
                                                   "gear": "tricycle"}),
    ("s_70", "flying_wing", 7.0, 9.0, 0x2E3238, {"jet": True, "fins": False}),
    ("leleka_100", "twin_boom", 1.6, 2.0, 0xC0C4B4, {}),
    ("shark", "twin_boom", 2.2, 3.0, 0xA6AA9A, {}),
    ("pd_2", "quad_vtol", 2.6, 3.6, 0xD8D8D0, {}),
    ("liutyi", "conventional", 4.4, 4.2, 0x2A2E33, {"prop": "front", "tail": "v"}),
    ("uj_22", "conventional", 3.4, 4.0, 0x5C6154, {"prop": "rear", "tail": "t"}),
    ("ram_ii", "twin_boom", 1.7, 2.0, 0x707A62, {}),
]

BUILDERS = {
    "delta": lambda m, l, s, c, p: delta_wing(m, l, s, c, p.get("jet", False), p.get("fins", True),
                                              p.get("serial", "")),
    "flying_wing": lambda m, l, s, c, p: flying_wing(m, l, s, c, p.get("jet", False),
                                                     p.get("fins", True)),
    "conventional": lambda m, l, s, c, p: conventional(m, l, s, c, p.get("prop", "front"),
                                                       p.get("tail", "v"), p.get("gear", "skid")),
    "twin_boom": lambda m, l, s, c, p: twin_boom(m, l, s, c),
    "cruciform": lambda m, l, s, c, p: cruciform(m, l, s, c),
    "quad_vtol": lambda m, l, s, c, p: quad_vtol(m, l, s, c),
}


JAVA_PACKAGE = "com.rbxlu.airsystem.client.model"
JAVA_DIR = ROOT / "src/main/java/com/rbxlu/airsystem/client/model"


def aerial_bomb_model() -> Model:
    """Guided bomb: ogive nose, cylindrical body with a driving band and cruciform
    fins inside a ring stabiliser.
    """
    model = Model("aerial_bomb")
    body = model.bone("body", pivot=(0, 0, 0))
    color = (74, 76, 70)

    af.ogive_nose(body, -11.0, 5.0, 5.0, 2.0, color, steps=3)
    body.add(Cube(origin=(-2.5, -0.5, -6.0), size=(5.0, 5.0, 12.0), color=color))
    body.add(Cube(origin=(-2.0, 0.0, 6.0), size=(4.0, 4.0, 4.0), color=af.shade(color, 0.94)))

    body.add(Cube(origin=(-3.0, -1.0, -1.0), size=(6.0, 6.0, 2.0),
                  color=af.shade(color, 0.86), detail="metal"))
    body.add(Cube(origin=(-2.5, 4.5, -5.0), size=(5.0, 1.0, 3.0),
                  color=(150, 44, 38), detail="marking"))

    for angle in (0.0, 90.0):
        body.add(Cube(origin=(-6.0, 1.0, 5.0), size=(12.0, 1.0, 5.0),
                      color=af.shade(color, 0.9), detail="metal",
                      rotation=(0.0, 0.0, angle), pivot=(0.0, 1.5, 7.0)))
    for x in (-6.0, 5.0):
        body.add(Cube(origin=(x, -2.0, 6.0), size=(1.0, 7.0, 3.0),
                      color=af.shade(color, 0.82), detail="metal"))
    for y in (-2.0, 4.0):
        body.add(Cube(origin=(-6.0, y, 6.0), size=(12.0, 1.0, 3.0),
                      color=af.shade(color, 0.82), detail="metal"))

    return model


def _composite_skin(model: Model) -> None:
    """Switches the skin to a composite finish.

    These airframes are moulded glassfibre, not riveted duralumin, so panel
    lines and rivets on them are as wrong as sheet seams on a car bumper.
    """
    for cube in model.all_cubes():
        if cube.detail == "panel":
            cube.detail = "composite"


def build() -> None:
    methods = []
    dispatch_cases = []

    for index, (drone_id, archetype, length, span, color, params) in enumerate(DRONES):
        model = Model(drone_id)
        BUILDERS[archetype](model, length, span, rgb(color), params)
        _composite_skin(model)

        texture_size = model.pack_uv()
        suffix = _java_name(drone_id)
        methods.append(VanillaEmitter(model, texture_size).emit_method(suffix))
        dispatch_cases.append(f"            case {drone_id.upper()} -> create{suffix}();")

        image = model.render_texture(texture_size, seed=1000 + index)
        texture_path = ASSETS / f"textures/entity/drone/{drone_id}.png"
        texture_path.parent.mkdir(parents=True, exist_ok=True)
        image.save(texture_path)

        print(f"  {drone_id:<14} {archetype:<13} texture {texture_size}x{texture_size}, "
              f"cubes: {len(model.all_cubes())}")

    bomb = aerial_bomb_model()
    bomb_size = bomb.pack_uv()
    methods.append(VanillaEmitter(bomb, bomb_size).emit_method("AerialBomb"))
    bomb.render_texture(bomb_size, seed=99).save(ASSETS / "textures/entity/drone/aerial_bomb.png")
    print(f"  {'aerial_bomb':<14} {'munition':<13} texture {bomb_size}x{bomb_size}, "
          f"cubes: {len(bomb.all_cubes())}")

    dispatch = "\n".join([
        "    /** Model for one drone type. */",
        "    public static LayerDefinition create(DroneKind kind) {",
        "        return switch (kind) {",
        *dispatch_cases,
        "        };",
        "    }",
    ])

    JAVA_DIR.mkdir(parents=True, exist_ok=True)
    source = emit_layer_class(
        JAVA_PACKAGE,
        "DroneLayers",
        "tools/build_drones.py",
        methods,
        dispatch,
        extra_imports=["com.rbxlu.airsystem.content.drone.DroneKind"],
    )
    (JAVA_DIR / "DroneLayers.java").write_text(source, encoding="utf-8")
    print(f"  DroneLayers.java — {len(methods)} models")


if __name__ == "__main__":
    print("Building drone models:")
    build()
