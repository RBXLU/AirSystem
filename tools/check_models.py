"""Finds detached cubes: parts that touch nothing else.

Cubes are grouped by bounding-box overlap (half a cube of tolerance). More than
one group means a piece floats free of the airframe."""
from __future__ import annotations

import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from geolib import Cube, Model


def _rotated_bounds(cube: Cube):
    """Bounding box of a rotated cube, taken over its eight corners."""
    x, y, z = cube.origin
    w, h, d = cube.size
    corners = [
        (x, y, z), (x + w, y, z), (x + w, y + h, z), (x, y + h, z),
        (x, y, z + d), (x + w, y, z + d), (x + w, y + h, z + d), (x, y + h, z + d),
    ]
    if cube.rotation:
        pivot = cube.pivot or cube.origin
        rx, ry, rz = (math.radians(a) for a in cube.rotation)
        rotated = []
        for corner in corners:
            px, py, pz = (corner[i] - pivot[i] for i in range(3))
            if rx:
                py, pz = py * math.cos(rx) - pz * math.sin(rx), py * math.sin(rx) + pz * math.cos(rx)
            if ry:
                px, pz = px * math.cos(ry) + pz * math.sin(ry), -px * math.sin(ry) + pz * math.cos(ry)
            if rz:
                px, py = px * math.cos(rz) - py * math.sin(rz), px * math.sin(rz) + py * math.cos(rz)
            rotated.append((px + pivot[0], py + pivot[1], pz + pivot[2]))
        corners = rotated

    return (
        min(c[0] for c in corners), min(c[1] for c in corners), min(c[2] for c in corners),
        max(c[0] for c in corners), max(c[1] for c in corners), max(c[2] for c in corners),
    )


def _touching(a, b, tolerance: float = 0.6) -> bool:
    return (a[0] - tolerance <= b[3] and b[0] - tolerance <= a[3]
            and a[1] - tolerance <= b[4] and b[1] - tolerance <= a[4]
            and a[2] - tolerance <= b[5] and b[2] - tolerance <= a[5])


def components(model: Model):
    """Connected groups of cubes."""
    cubes = model.all_cubes()
    bounds = [_rotated_bounds(cube) for cube in cubes]

    parent = list(range(len(cubes)))

    def find(i):
        while parent[i] != i:
            parent[i] = parent[parent[i]]
            i = parent[i]
        return i

    def union(i, j):
        ri, rj = find(i), find(j)
        if ri != rj:
            parent[ri] = rj

    for i in range(len(cubes)):
        for j in range(i + 1, len(cubes)):
            if _touching(bounds[i], bounds[j]):
                union(i, j)

    groups = {}
    for index in range(len(cubes)):
        groups.setdefault(find(index), []).append(index)
    return cubes, bounds, list(groups.values())


def report(name: str, model: Model) -> int:
    cubes, bounds, groups = components(model)
    if len(groups) <= 1:
        return 0

    groups.sort(key=len, reverse=True)
    detached = groups[1:]
    total = sum(len(group) for group in detached)
    print(f"  {name}: {total} detached cubes in {len(detached)} groups")
    for group in detached:
        for index in group:
            box = bounds[index]
            print(f"      cube origin={tuple(round(v, 1) for v in cubes[index].origin)} "
                  f"size={tuple(round(v, 1) for v in cubes[index].size)} "
                  f"bounds=({box[0]:.1f}..{box[3]:.1f}, {box[1]:.1f}..{box[4]:.1f}, "
                  f"{box[2]:.1f}..{box[5]:.1f})")
    return total


def main() -> int:
    from build_drones import DRONES, BUILDERS, rgb, aerial_bomb_model
    from build_turrets import BUILDERS as TURRETS

    print("Checking models for detached parts:")
    problems = 0

    for drone_id, archetype, length, span, color, params in DRONES:
        model = Model(drone_id)
        BUILDERS[archetype](model, length, span, rgb(color), params)
        model.normalize()
        problems += report(drone_id, model)

    bomb = aerial_bomb_model()
    bomb.normalize()
    problems += report("aerial_bomb", bomb)

    for turret_id, builder in TURRETS.items():
        model = builder()
        model.normalize()
        problems += report(turret_id, model)

    if problems == 0:
        print("  all connected")
        return 0
    print(f"  TOTAL detached cubes: {problems}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
