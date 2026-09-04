"""Airframe kit: tapered fuselages, swept wings, tails and running gear.

Four rules hold the whole library together. A wing is defined by both its
edges, because a delta needs a near-straight trailing edge or it comes out a
diamond. Wing sections thicken at the root and thin at the tip, or they read as
plywood. Detail is sparse but large: long clean sections instead of a picket
fence of one-unit steps. And every protruding part is anchored to what it
touches, with joint coordinates computed rather than eyeballed — check_models.py
enforces that."""
from __future__ import annotations

import math
from typing import Optional, Tuple

from geolib import Bone, Cube

U = 16.0


def shade(color, factor: float):
    return tuple(max(0, min(255, int(c * factor))) for c in color)


def _q(value: float) -> float:
    """Rounds to whole units; the model lives on a sixteenth-of-a-block grid."""
    return float(max(1, round(value)))


def _tiles(start: float, total: float, count: int):
    """Splits a span into adjoining integer tiles with no gaps or overlaps.

    Fractional positions with rounded sizes leave slits between the cubes.
    """
    edges = [round(start + total * i / count) for i in range(count + 1)]
    for i in range(count):
        left, right = edges[i], edges[i + 1]
        if right <= left:
            right = left + 1
            edges[i + 1] = right
        yield float(left), float(right - left)


def body_profile(t: float, nose: float, tail: float) -> float:
    """Relative fuselage section along its length: 0 at the nose, 1 at the tail."""
    if t < nose:
        return max(0.18, (t / nose) ** 0.55)
    if t > 1.0 - tail:
        k = (t - (1.0 - tail)) / tail
        return 1.0 - 0.62 * k
    return 1.0


def fuselage(bone: Bone, length: float, width: float, height: float, base_y: float,
             color, sections: int = 5, nose: float = 0.30, tail: float = 0.32,
             z_center: float = 0.0) -> list:
    """Fuselage built from a few large sections of smoothly varying cross-section.

    Returns the sections it added; the largest one later carries the serial.
    """
    added = []
    for i, (z_start, depth) in enumerate(_tiles(z_center - length / 2, length, sections)):
        tm = (i + 0.5) / sections
        factor = body_profile(tm, nose, tail)

        w = _q(width * factor)
        h = _q(height * factor)

        y = base_y + (height - h) * 0.45
        tone = 1.0 + 0.05 * math.sin(tm * math.pi)
        section = Cube(origin=(-w / 2, y, z_start), size=(w, h, depth), color=shade(color, tone))
        bone.add(section)
        added.append(section)
    return added


def nose_cone(bone: Bone, tip_z: float, width: float, height: float, center_y: float,
              color, steps: int = 3, depth: float = 4.0, cap_color=None) -> None:
    """Pointed nose, tapering and centred on the fuselage axis.

    ``cap_color`` paints the cap itself: on armed types the fuzed nose is
    noticeably darker than the skin.
    """
    if cap_color is not None:
        color = cap_color
    for i, (z, dz) in enumerate(_tiles(tip_z, depth * steps, steps)):
        k = (i + 1.2) / (steps + 1.0)
        w = _q(width * k)
        h = _q(height * k)
        bone.add(Cube(origin=(-w / 2, round(center_y - h / 2), z), size=(w, h, dz),
                      color=shade(color, 0.86 + 0.03 * i)))


def ogive_nose(bone: Bone, tip_z: float, length: float, diameter: float, center_y: float,
               color, steps: int = 3) -> None:
    """Ogive nose: the section grows as a square root, giving a bullet shape
    rather than a stepped cone.
    """
    for i, (z, dz) in enumerate(_tiles(tip_z, length, steps)):
        k = ((i + 1.0) / steps) ** 0.55
        size = _q(max(2.0, diameter * k))
        bone.add(Cube(origin=(-size / 2, round(center_y - size / 2), z),
                      size=(size, size, dz), color=shade(color, 0.88 + 0.03 * i)))


def tail_boom(bone: Bone, z_from: float, z_to: float, y: float, size: float, color,
              x: float = 0.0) -> None:
    """Tail boom: a slim tube from the body to the tail surfaces."""
    thickness = _q(size)
    start, end = round(z_from), round(z_to)
    bone.add(Cube(origin=(round(x - thickness / 2), round(y), start),
                  size=(thickness, thickness, _q(end - start)), color=shade(color, 0.94)))


def _chord_at(root_chord: float, tip_chord: float, z_leading: float, le_tan: float,
              te_tan: Optional[float], offset: float, t: float) -> float:
    """Chord at a station, from either the taper ratio or the two edges."""
    if te_tan is None:
        return root_chord + (tip_chord - root_chord) * t
    return (z_leading + root_chord + te_tan * offset) - (z_leading + le_tan * offset)


def wing_half(bone: Bone, side: int, root_chord: float, tip_chord: float, half_span: float,
              sweep_deg: float, thickness: float, y: float, z_leading: float, color,
              segments: int = 5, dihedral: float = 0.0, root_offset: float = 0.0,
              te_sweep_deg: Optional[float] = None, tip_thickness: Optional[float] = None,
              flap_span: Optional[Tuple[float, float]] = None,
              flap_frac: float = 0.28) -> None:
    """Half a wing: chord taper, sweep, thickness and control surfaces.

    ``te_sweep_deg`` sweeps the trailing edge; near zero for a delta, which is
    the only way the silhouette comes out triangular. ``flap_span`` is the
    fraction of span where the trailing section becomes its own cube.
    """
    le_tan = math.tan(math.radians(sweep_deg))
    te_tan = math.tan(math.radians(te_sweep_deg)) if te_sweep_deg is not None else None
    root_t = thickness
    tip_t = thickness if tip_thickness is None else tip_thickness

    for i, (offset, seg_span) in enumerate(_tiles(root_offset, half_span - root_offset, segments)):
        middle = offset + seg_span * 0.5
        t = (middle - root_offset) / max(1e-6, half_span - root_offset)

        chord = _q(max(2.0, _chord_at(root_chord, tip_chord, z_leading, le_tan, te_tan,
                                      middle, t)))
        thick = _q(root_t + (tip_t - root_t) * t)
        x = offset if side > 0 else -(offset + seg_span)
        z = round(z_leading + le_tan * middle)
        y_off = round(y + dihedral * middle)
        tone = 1.02 - 0.02 * i

        in_flap = flap_span is not None and flap_span[0] <= t <= flap_span[1] and chord >= 6
        if not in_flap:
            bone.add(Cube(origin=(x, y_off, z), size=(seg_span, thick, chord),
                          color=shade(color, tone)))
            continue

        flap = _q(max(2.0, chord * flap_frac))
        main = chord - flap
        bone.add(Cube(origin=(x, y_off, z), size=(seg_span, thick, main),
                      color=shade(color, tone)))
        bone.add(Cube(origin=(x, y_off, z + main), size=(seg_span, max(1.0, thick - 1.0), flap),
                      color=shade(color, tone * 0.88)))


def wing(bone: Bone, root_chord: float, tip_chord: float, half_span: float, sweep_deg: float,
         thickness: float, y: float, z_leading: float, color, segments: int = 5,
         dihedral: float = 0.0, root_offset: float = 0.0,
         te_sweep_deg: Optional[float] = None, tip_thickness: Optional[float] = None,
         flap_span: Optional[Tuple[float, float]] = None, flap_frac: float = 0.28) -> None:
    for side in (-1, 1):
        wing_half(bone, side, root_chord, tip_chord, half_span, sweep_deg, thickness, y,
                  z_leading, color, segments, dihedral, root_offset, te_sweep_deg,
                  tip_thickness, flap_span, flap_frac)


def wing_centre(bone: Bone, root_offset: float, root_chord: float, thickness: float,
                y: float, z_leading: float, color) -> None:
    """Centre section between the wing halves.

    The halves start at a root offset, so without this the nose and sensor ball
    would be attached to nothing.
    """
    width = _q(root_offset * 2 + 2)
    bone.add(Cube(origin=(-width / 2, round(y), round(z_leading)),
                  size=(width, _q(thickness), _q(root_chord)), color=shade(color, 1.02)))


def delta_centre(bone: Bone, root_le: float, root_te: float, sweep_deg: float,
                 half_width: float, thickness: float, y: float, color, steps: int = 3) -> None:
    """Delta centre section, narrowing to a point along the leading edge.

    A rectangular filler here grows a tongue ahead of the apex in plan view.
    """
    le_tan = math.tan(math.radians(sweep_deg))
    z_full = root_le + le_tan * half_width

    for _i, (z, dz) in enumerate(_tiles(root_le, z_full - root_le, steps)):
        width = _q(max(2.0, 2.0 * (z + dz - root_le) / max(1e-6, le_tan)))
        width = min(width, _q(half_width * 2))
        bone.add(Cube(origin=(-width / 2, round(y), z), size=(width, _q(thickness), dz),
                      color=shade(color, 1.03)))

    full = _q(half_width * 2)
    bone.add(Cube(origin=(-full / 2, round(y), round(z_full)),
                  size=(full, _q(thickness), _q(root_te - z_full)), color=shade(color, 1.02)))


def wing_tip(root_chord: float, tip_chord: float, half_span: float, sweep_deg: float,
             z_leading: float, root_offset: float,
             te_sweep_deg: Optional[float] = None) -> Tuple[float, float]:
    """Where a wing ends: the tip leading edge and its chord."""
    le_tan = math.tan(math.radians(sweep_deg))
    te_tan = math.tan(math.radians(te_sweep_deg)) if te_sweep_deg is not None else None
    offset = half_span - 1.0
    t = (offset - root_offset) / max(1e-6, half_span - root_offset)
    chord = _chord_at(root_chord, tip_chord, z_leading, le_tan, te_tan, offset,
                      min(1.0, max(0.0, t)))
    return round(z_leading + le_tan * offset), _q(max(2.0, chord))


def fin(bone: Bone, x: float, z: float, chord: float, tip_chord: float, height: float,
        thickness: float, y: float, color, sweep_deg: float = 25.0, segments: int = 3,
        cant_deg: float = 0.0) -> None:
    """Vertical surface: a fin or a wingtip plate."""
    sweep = math.tan(math.radians(sweep_deg))
    thick = _q(thickness)
    for i, (y_start, seg_h) in enumerate(_tiles(y, height, segments)):
        t = (i + 0.5) / segments
        local_chord = _q(chord + (tip_chord - chord) * t)
        cube = Cube(
            origin=(round(x), y_start, round(z + sweep * (y_start - y))),
            size=(thick, seg_h, local_chord),
            color=shade(color, 0.93 - 0.015 * i),
        )
        if cant_deg:
            cube.rotation = (0.0, 0.0, cant_deg)
            cube.pivot = (x, y, z)
        bone.add(cube)


def v_tail(bone: Bone, half_span: float, root_chord: float, tip_chord: float, thickness: float,
           y: float, z_leading: float, color, angle_deg: float = 42.0, segments: int = 3,
           root_x: float = 0.0) -> None:
    """V tail: two surfaces rotated about a shared point on the boom, so their
    roots meet where the tail ends.
    """
    sweep = math.tan(math.radians(18.0))
    for side in (-1, 1):
        pivot = (root_x, y, z_leading)
        for i, (offset, seg_span) in enumerate(_tiles(0.0, half_span, segments)):
            t = (i + 0.5) / segments
            chord = _q(root_chord + (tip_chord - root_chord) * t)
            x = root_x + offset if side > 0 else root_x - (offset + seg_span)
            cube = Cube(
                origin=(x, round(y), round(z_leading + sweep * offset)),
                size=(seg_span, _q(thickness), chord),
                color=shade(color, 0.95 - 0.02 * i),
                rotation=(0.0, 0.0, angle_deg * side),
                pivot=pivot,
            )
            bone.add(cube)


def stabilizer(bone: Bone, half_span: float, root_chord: float, tip_chord: float,
               thickness: float, y: float, z_leading: float, color, segments: int = 3) -> None:
    """Horizontal stabiliser."""
    wing(bone, root_chord, tip_chord, half_span, 10.0, thickness, y, z_leading, color, segments)


def propeller(model, bone_name: str, parent: str, hub_z: float, hub_y: float, radius: float,
              blades: int = 2, spinner: bool = True):
    """Propeller as its own bone, which the renderer spins about the shaft."""
    y = round(hub_y)
    z = round(hub_z)
    prop = model.bone(bone_name, pivot=(0.0, y, z), parent=parent)

    span = _q(radius)
    inner = _q(max(1.0, span * 0.45))
    dark = (36, 36, 40)

    prop.add(Cube(origin=(-inner, y - 1, z), size=(inner * 2, 2.0, 1.0), color=dark, detail="dark"))
    prop.add(Cube(origin=(-span, y - 1, z), size=(span * 2, 1.0, 1.0),
                  color=shade(dark, 1.15), detail="dark"))
    if blades >= 3:
        prop.add(Cube(origin=(-1.0, y - span, z), size=(1.0, span * 2, 1.0),
                      color=shade(dark, 1.15), detail="dark"))

    if spinner:
        prop.add(Cube(origin=(-1.5, y - 1.5, z - 1), size=(3.0, 3.0, 2.0),
                      color=(62, 62, 66), detail="metal"))
    return prop


def engine_pod(bone: Bone, z: float, y: float, width: float, height: float, length: float,
               color) -> None:
    """Piston engine pod with cylinders and exhaust."""
    w, h, d = _q(width), _q(height), _q(length)
    bone.add(Cube(origin=(-w / 2, round(y), round(z)), size=(w, h, d),
                  color=shade(color, 0.88), detail="panel"))
    for side in (-1, 1):
        x = w / 2 - 1 if side > 0 else -w / 2
        bone.add(Cube(origin=(x, round(y) + 1, round(z) + 1), size=(1.0, max(1.0, h - 2), 3.0),
                      color=(58, 58, 60), detail="metal"))
    bone.add(Cube(origin=(-1.0, round(y) - 1, round(z) + d - 3), size=(2.0, 1.0, 3.0),
                  color=(40, 38, 36), detail="dark"))


def spinner(bone: Bone, z: float, y: float, size: float, color=(52, 52, 56)) -> None:
    bone.add(Cube(origin=(-size / 2, y - size / 2, z), size=(_q(size), _q(size), 2.0),
                  color=color, detail="metal"))


def sensor_ball(bone: Bone, z: float, y: float, size: float) -> None:
    """Electro-optical turret under the nose."""
    body = _q(min(size, 4.0))
    bone.add(Cube(origin=(-body / 2, y - body, z), size=(body, body, body),
                  color=(46, 48, 52), detail="metal"))
    if body >= 3:
        bone.add(Cube(origin=(-body / 2 + 1, y - body + 1, z - 1),
                      size=(body - 2, body - 2, 1.0), color=(60, 92, 118), detail="glass"))


def jet_nacelle(bone: Bone, z: float, y: float, width: float, height: float, length: float,
                color=(58, 60, 64)) -> None:
    """Jet engine pod."""
    bone.add(Cube(origin=(-width / 2, y, z), size=(_q(width), _q(height), _q(length)),
                  color=color, detail="metal"))
    bone.add(Cube(origin=(-width / 2 + 1, y + 1, z + length - 1),
                  size=(_q(width - 2), _q(height - 2), 2.0), color=(26, 26, 28), detail="dark"))
    bone.add(Cube(origin=(-width / 2 + 1, y + 1, z - 1),
                  size=(_q(width - 2), _q(height - 2), 1.0), color=(30, 30, 32), detail="dark"))


def exhaust_nozzle(bone: Bone, z: float, y: float, size: float) -> None:
    """Nozzle: a shroud ring with a dark opening inside."""
    ring = _q(size)
    bone.add(Cube(origin=(-ring / 2, round(y - ring / 2), round(z)), size=(ring, ring, 2.0),
                  color=(64, 62, 60), detail="metal"))
    if ring >= 3:
        bone.add(Cube(origin=(-ring / 2 + 1, round(y - ring / 2) + 1, round(z) + 1),
                      size=(ring - 2, ring - 2, 1.0), color=(22, 22, 24), detail="dark"))


def antenna(bone: Bone, z: float, y: float, height: float, color=(70, 72, 76)) -> None:
    bone.add(Cube(origin=(-0.5, y, z), size=(1.0, _q(height), 1.0), color=color, detail="metal"))


def skid(bone: Bone, z: float, y: float, width: float, length: float, color=(48, 48, 50)) -> None:
    """Landing skid."""
    bone.add(Cube(origin=(-width / 2, y - 2, z), size=(_q(width), 1.0, _q(length)),
                  color=color, detail="dark"))
    for x in (-width / 2 + 0.5, width / 2 - 1.5):
        bone.add(Cube(origin=(x, y - 2, z + length / 2 - 0.5), size=(1.0, 2.0, 1.0),
                      color=color, detail="dark"))


def gear_leg(bone: Bone, x: float, z: float, y: float, height: float,
             wheel: float = 2.0, color=(52, 52, 56)) -> None:
    """Landing gear leg and wheel.

    The top runs a unit above the attachment point so that grid rounding cannot
    leave it hanging below the skin.
    """
    top = round(y) + 1
    bottom = round(y - height)
    h = max(1.0, top - bottom)
    bone.add(Cube(origin=(round(x), bottom, round(z)), size=(1.0, h, 1.0),
                  color=color, detail="metal"))
    w = _q(wheel)
    bone.add(Cube(origin=(round(x) - 0.5, bottom - w + 1, round(z) - (w - 1) / 2),
                  size=(2.0, w, w), color=(34, 34, 36), detail="dark"))


def landing_gear(bone: Bone, half_track: float, main_z: float, nose_z: float, y: float,
                 height: float, color=(52, 52, 56)) -> None:
    """Tricycle gear: two mains and a nose leg."""
    for side in (-1, 1):
        gear_leg(bone, side * half_track - (1.0 if side > 0 else 0.0), main_z, y, height, 2.0, color)
    gear_leg(bone, -0.5, nose_z, y, height * 0.85, 2.0, color)


def spine_strip(bone: Bone, y_top: float, z: float, length: float, width: float, color) -> None:
    """Dorsal spine: antenna raceway and mould split line."""
    bone.add(Cube(origin=(-width / 2, round(y_top), round(z)),
                  size=(_q(width), 1.0, _q(length)), color=shade(color, 0.93), detail="panel"))


def pitot(bone: Bone, z: float, y: float, length: float = 4.0,
          color=(58, 60, 64)) -> None:
    """Pitot boom ahead of the nose, sunk two units into the cap so it stays
    attached whatever the rounding does.
    """
    start = round(z - length)
    bone.add(Cube(origin=(-0.5, round(y), start), size=(1.0, 1.0, _q(round(z) + 2 - start)),
                  color=color, detail="metal"))


def nav_light(bone: Bone, x: float, y: float, z: float, color) -> None:
    """Navigation light on the wingtip."""
    bone.add(Cube(origin=(round(x), round(y), round(z)), size=(1.0, 1.0, 1.0),
                  color=color, detail="glass"))


def exhaust(bone: Bone, x: float, y: float, z: float, length: float = 4.0) -> None:
    """Exhaust stub of a piston engine."""
    bone.add(Cube(origin=(round(x), round(y), round(z)), size=(2.0, 2.0, _q(length)),
                  color=(40, 38, 36), detail="dark"))


def intake(bone: Bone, y: float, z: float, width: float, height: float = 2.0) -> None:
    """Cooling intake."""
    bone.add(Cube(origin=(-width / 2, round(y), round(z)), size=(_q(width), _q(height), 1.0),
                  color=(30, 30, 32), detail="dark"))


def hatch(bone: Bone, x: float, y: float, z: float, width: float, height: float, color) -> None:
    """Access panel on the side."""
    bone.add(Cube(origin=(round(x), round(y), round(z)), size=(1.0, _q(height), _q(width)),
                  color=shade(color, 0.9), detail="panel"))


def marking_band(bone: Bone, y: float, z: float, width: float, depth: float = 2.0) -> None:
    """Warning stripe by the warhead."""
    bone.add(Cube(origin=(-width / 2, round(y), round(z)), size=(_q(width), 1.0, _q(depth)),
                  color=(150, 44, 38), detail="marking"))


def gimbal_camera(bone: Bone, z: float, y: float, size: float) -> None:
    """Gimballed sensor: frame, ball and lens window."""
    ball = _q(min(size, 5.0))
    top = round(y)
    bone.add(Cube(origin=(-ball / 2 - 1, top - 1, round(z) - 1),
                  size=(ball + 2, 2.0, ball + 2), color=(56, 58, 62), detail="metal"))
    bone.add(Cube(origin=(-ball / 2, top - ball - 1, round(z)),
                  size=(ball, ball, ball), color=(42, 44, 48), detail="metal"))
    if ball >= 3:
        bone.add(Cube(origin=(-ball / 2 + 1, top - ball, round(z) - 1),
                      size=(max(1.0, ball - 3), max(1.0, ball - 2), 1.0),
                      color=(58, 96, 124), detail="glass"))
        bone.add(Cube(origin=(ball / 2 - 2, top - ball, round(z) - 1),
                      size=(1.0, 1.0, 1.0), color=(112, 60, 48), detail="glass"))


def gun_barrel(bone: Bone, x: float, y: float, z_breech: float, length: float, caliber: float,
               color=(58, 60, 58)) -> None:
    """Barrel with breech and muzzle brake."""
    c = _q(caliber)
    bone.add(Cube(origin=(x, y, z_breech - length * 0.22), size=(c + 2, c + 2, _q(length * 0.22)),
                  color=color, detail="metal"))
    bone.add(Cube(origin=(x + 1, y + 1, z_breech - length), size=(c, c, _q(length * 0.78)),
                  color=(44, 44, 46), detail="dark"))
    bone.add(Cube(origin=(x + 0.5, y + 0.5, z_breech - length - 3), size=(c + 1, c + 1, 3.0),
                  color=(34, 34, 36), detail="dark"))


def tracks(bone: Bone, half_width: float, length: float, height: float, y: float,
           wheels: int = 6, color=(40, 40, 42)) -> None:
    """Running gear: track, drive sprocket at the rear, idler at the front and
    road wheels showing below the fender. Without them it reads as a black bar.
    """
    band = 6.0
    for side in (-1, 1):
        x = half_width - band if side > 0 else -half_width

        bone.add(Cube(origin=(x, y + height - 3, -length / 2), size=(band, 3.0, _q(length)),
                      color=shade(color, 1.25), detail="panel"))
        bone.add(Cube(origin=(x + 1, y + 2, -length / 2 + 1), size=(band - 2, _q(height - 5),
                      _q(length - 2)), color=color, detail="dark"))

        for start, span in _tiles(-length / 2 + 8, length - 16, wheels):
            bone.add(Cube(origin=(x - 1, y, start), size=(band + 2, _q(height - 4),
                          max(3.0, span - 1)), color=(58, 58, 60), detail="metal"))

        for z, tone in ((-length / 2 + 1, 0.92), (length / 2 - 8, 1.0)):
            bone.add(Cube(origin=(x - 1, y + 1, z), size=(band + 2, _q(height - 3), 7.0),
                          color=shade((70, 70, 72), tone), detail="metal"))


def glacis(bone: Bone, half_width: float, y_base: float, height: float, z_front: float,
           run: float, color, steps: int = 3, backwards: bool = False) -> None:
    """Sloped glacis built from steps, which mates with the hull exactly and
    reads as armour slope in a cube style.
    """
    for i, (y, dy) in enumerate(_tiles(y_base, height, steps)):
        offset = run * (i / steps)
        if backwards:
            z = z_front - run + offset
            depth = run - offset + 2.0
        else:
            z = z_front + offset
            depth = run - offset + 2.0
        bone.add(Cube(origin=(-half_width, y, round(z)), size=(_q(half_width * 2), dy, _q(depth)),
                      color=shade(color, 1.02 - 0.02 * i)))


def faceted_box(bone: Bone, half_width: float, y: float, height: float, z_front: float,
                z_back: float, color, chamfer: float = 2.0, layers: int = 3) -> None:
    """Faceted volume: a turret in layers with the top and bottom drawn in, so it
    reads as facets rather than a brick.
    """
    depth = z_back - z_front
    for i, (level, dy) in enumerate(_tiles(y, height, layers)):
        inset = 0.0 if 0 < i < layers - 1 else chamfer
        hw = _q(max(2.0, half_width - inset))
        bone.add(Cube(origin=(-hw, level, round(z_front + inset)),
                      size=(hw * 2, dy, _q(depth - 2 * inset)),
                      color=shade(color, 1.06 - 0.05 * i)))


def radar_panel(bone: Bone, width: float, height: float, y: float, z: float, color,
                tilt_deg: float = -18.0) -> None:
    """Flat radar panel on a tilted frame."""
    w, h = _q(width), _q(height)
    bone.add(Cube(origin=(-w / 2, y, z), size=(w, h, 2.0), color=color,
                  rotation=(tilt_deg, 0.0, 0.0), pivot=(0.0, y, z), detail="metal"))
    bone.add(Cube(origin=(-w / 2 + 2, y + 2, z - 1), size=(w - 4, h - 4, 1.0),
                  color=shade(color, 0.72), rotation=(tilt_deg, 0.0, 0.0),
                  pivot=(0.0, y, z), detail="dark"))


def dish_radar(bone: Bone, width: float, height: float, y: float, z: float, color,
               tilt_deg: float = -20.0) -> None:
    """Search radar dish: frame, recessed reflector and feed horn — the Gepard's
    single most recognisable feature, so it is not one flat slab.
    """
    w, h = _q(width), _q(height)
    pivot = (0.0, y, z)
    rot = (tilt_deg, 0.0, 0.0)

    for x in (-w / 2, w / 2 - 2):
        bone.add(Cube(origin=(x, y, z), size=(2.0, h, 3.0), color=shade(color, 1.05),
                      rotation=rot, pivot=pivot, detail="metal"))
    for y_edge in (y, y + h - 2):
        bone.add(Cube(origin=(-w / 2, y_edge, z), size=(w, 2.0, 3.0), color=shade(color, 1.05),
                      rotation=rot, pivot=pivot, detail="metal"))
    bone.add(Cube(origin=(-w / 2 + 2, y + 2, z + 1), size=(w - 4, h - 4, 2.0),
                  color=shade(color, 0.70), rotation=rot, pivot=pivot, detail="dark"))
    bone.add(Cube(origin=(-1.0, round(y + h / 2) - 1, z - 5), size=(2.0, 2.0, 6.0),
                  color=shade(color, 0.85), rotation=rot, pivot=pivot, detail="metal"))


def stowage(bone: Bone, x: float, y: float, z: float, width: float, height: float, depth: float,
            color) -> None:
    """Stowage box on the side."""
    bone.add(Cube(origin=(x, y, z), size=(_q(width), _q(height), _q(depth)),
                  color=shade(color, 0.88), detail="panel"))
