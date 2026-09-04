"""Renders the models with numpy/scipy, to see a silhouette without the game.

A real rasteriser rather than painter's algorithm: a Z buffer so faces cannot
bleed through each other, scipy rotations for cubes and camera, supersampled
edges, and outlines drawn from depth discontinuities.

It also reports silhouette metrics — connectivity, span, elongation and fill —
which say whether the outline reads like the real aircraft."""
from __future__ import annotations

import math
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import List, Sequence, Tuple

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage
from scipy.spatial.transform import Rotation

sys.path.insert(0, str(Path(__file__).parent))
from geolib import Cube, Model

BACKGROUND = np.array([24, 26, 30], dtype=np.float64)
KEY_LIGHT = np.array([-0.42, 0.80, 0.43])
FILL_LIGHT = np.array([0.55, 0.25, -0.60])
SUPERSAMPLE = 3

_CORNERS = np.array([
    [0, 0, 0], [1, 0, 0], [1, 1, 0], [0, 1, 0],
    [0, 0, 1], [1, 0, 1], [1, 1, 1], [0, 1, 1],
], dtype=np.float64)

_FACES = (
    ((0, 1, 2, 3), (0.0, 0.0, -1.0), "front"),
    ((5, 4, 7, 6), (0.0, 0.0, 1.0), "back"),
    ((4, 0, 3, 7), (-1.0, 0.0, 0.0), "side_w"),
    ((1, 5, 6, 2), (1.0, 0.0, 0.0), "side_e"),
    ((3, 2, 6, 7), (0.0, 1.0, 0.0), "up"),
    ((4, 5, 1, 0), (0.0, -1.0, 0.0), "down"),
)


def _cube_rotation(cube: Cube) -> Rotation:
    """Cube rotation: X, then Y, then Z, as the geo format defines it."""
    return Rotation.from_euler("XYZ", cube.rotation, degrees=True)


def _camera_rotation(azimuth: float, elevation: float) -> Rotation:
    """Camera: yaw about the vertical, then elevation above the horizon.

    The negative sign matters: a positive elevation has to bring the top of the
    model towards the viewer, or the Z buffer shows the underside — which looks
    almost, but not quite, like a plan view.
    """
    return Rotation.from_euler("XY", [-elevation, azimuth], degrees=True)


@dataclass
class Geometry:
    """Scene triangles: vertices, normals, colours and owning cube."""

    vertices: np.ndarray
    normals: np.ndarray
    colors: np.ndarray
    cube_ids: np.ndarray

    @property
    def count(self) -> int:
        return len(self.vertices)


def build_geometry(model: Model, texture: Image.Image | None = None) -> Geometry:
    vertices: List[np.ndarray] = []
    normals: List[np.ndarray] = []
    colors: List[np.ndarray] = []
    cube_ids: List[int] = []

    for cube_id, cube in enumerate(model.all_cubes()):
        origin = np.asarray(cube.origin, dtype=np.float64)
        size = np.asarray(cube.size, dtype=np.float64)
        corners = _CORNERS * size + origin

        if cube.rotation:
            rotation = _cube_rotation(cube)
            pivot = np.asarray(cube.pivot or cube.origin, dtype=np.float64)
            corners = rotation.apply(corners - pivot) + pivot

        for indices, normal, kind in _FACES:
            normal_vec = np.asarray(normal, dtype=np.float64)
            if cube.rotation:
                normal_vec = _cube_rotation(cube).apply(normal_vec)

            color = np.asarray(cube.color, dtype=np.float64)
            if texture is not None:
                sampled = _face_color(texture, cube, kind)
                if sampled is not None:
                    color = sampled

            quad = corners[list(indices)]
            for triangle in ((0, 1, 2), (0, 2, 3)):
                vertices.append(quad[list(triangle)])
                normals.append(normal_vec)
                colors.append(color)
                cube_ids.append(cube_id)

    if not vertices:
        empty = np.zeros((0, 3, 3))
        return Geometry(empty, np.zeros((0, 3)), np.zeros((0, 3)), np.zeros(0, dtype=int))

    return Geometry(np.array(vertices), np.array(normals), np.array(colors),
                    np.array(cube_ids, dtype=int))


def _face_uv_rect(cube: Cube, kind: str) -> Tuple[int, int, int, int]:
    w, h, d = cube.int_size()
    u, v = cube.uv
    return {
        "up": (u + d, v, w, d),
        "down": (u + d + w, v, w, d),
        "side_w": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "side_e": (u + d + w, v + d, d, h),
        "back": (u + 2 * d + w, v + d, w, h),
    }[kind]


def _face_color(texture: Image.Image, cube: Cube, kind: str) -> np.ndarray | None:
    """Mean face colour from the atlas, approximating the real texture."""
    x, y, w, h = _face_uv_rect(cube, kind)
    pixels = np.asarray(texture.convert("RGBA"), dtype=np.float64)
    patch = pixels[y:y + h, x:x + w]
    if patch.size == 0:
        return None
    mask = patch[..., 3] > 0
    if not mask.any():
        return None
    return patch[..., :3][mask].mean(axis=0)


def _project(geometry: Geometry, azimuth: float, elevation: float) -> np.ndarray:
    """Orthographic projection: screen x, screen y and depth per vertex."""
    rotation = _camera_rotation(azimuth, elevation)
    flat = geometry.vertices.reshape(-1, 3)
    view = rotation.apply(flat).reshape(geometry.vertices.shape)
    return np.stack([view[..., 0], -view[..., 1], view[..., 2]], axis=-1)


def _rasterize(points: np.ndarray, shade: np.ndarray, colors: np.ndarray,
               cube_ids: np.ndarray, width: int, height: int):
    """Z-buffered rasteriser, returning colour, depth, cube id and coverage."""
    frame = np.tile(BACKGROUND, (height, width, 1))
    depth = np.full((height, width), np.inf)
    ident = np.full((height, width), -1, dtype=np.int32)

    order = np.argsort(points[:, :, 2].mean(axis=1))
    for index in order:
        tri = points[index]
        x0 = max(int(np.floor(tri[:, 0].min())), 0)
        x1 = min(int(np.ceil(tri[:, 0].max())) + 1, width)
        y0 = max(int(np.floor(tri[:, 1].min())), 0)
        y1 = min(int(np.ceil(tri[:, 1].max())) + 1, height)
        if x1 <= x0 or y1 <= y0:
            continue

        ax, ay = tri[0, 0], tri[0, 1]
        bx, by = tri[1, 0], tri[1, 1]
        cx, cy = tri[2, 0], tri[2, 1]
        area = (bx - ax) * (cy - ay) - (cx - ax) * (by - ay)
        if abs(area) < 1e-9:
            continue

        ys, xs = np.mgrid[y0:y1, x0:x1]
        px = xs + 0.5
        py = ys + 0.5
        w0 = ((bx - px) * (cy - py) - (cx - px) * (by - py)) / area
        w1 = ((cx - px) * (ay - py) - (ax - px) * (cy - py)) / area
        w2 = 1.0 - w0 - w1
        inside = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
        if not inside.any():
            continue

        z = w0 * tri[0, 2] + w1 * tri[1, 2] + w2 * tri[2, 2]
        window = depth[y0:y1, x0:x1]
        visible = inside & (z < window)
        if not visible.any():
            continue

        window[visible] = z[visible]
        frame[y0:y1, x0:x1][visible] = colors[index] * shade[index]
        ident[y0:y1, x0:x1][visible] = cube_ids[index]

    return frame, depth, ident, np.isfinite(depth)


def _shade_factors(geometry: Geometry, azimuth: float, elevation: float,
                   textured: bool) -> np.ndarray:
    """Lambert shading from a key light and a weak fill."""
    rotation = _camera_rotation(azimuth, elevation)
    normals = rotation.apply(geometry.normals)
    key = np.clip(normals @ KEY_LIGHT, 0.0, 1.0)
    fill = np.clip(normals @ FILL_LIGHT, 0.0, 1.0)
    if textured:
        return (0.70 + 0.24 * key + 0.10 * fill)[:, None]
    return (0.34 + 0.56 * key + 0.16 * fill)[:, None]


def render_view(model: Model, azimuth: float = 35.0, elevation: float = 22.0,
                size: Tuple[int, int] = (420, 320), title: str = "",
                texture: Image.Image | None = None,
                scale: float | None = None) -> Image.Image:
    """One view of a model."""
    geometry = build_geometry(model, texture)
    width, height = size
    if geometry.count == 0:
        return Image.fromarray(np.tile(BACKGROUND, (height, width, 1)).astype(np.uint8))

    points = _project(geometry, azimuth, elevation)
    shade = _shade_factors(geometry, azimuth, elevation, texture is not None)

    ss = SUPERSAMPLE
    hi_w, hi_h = width * ss, height * ss

    xs, ys = points[..., 0], points[..., 1]
    span = max(xs.max() - xs.min(), ys.max() - ys.min()) or 1.0
    view_scale = (scale if scale is not None else min(width, height) * 0.84 / span) * ss
    offset_x = hi_w / 2 - (xs.min() + xs.max()) / 2 * view_scale
    offset_y = hi_h / 2 - (ys.min() + ys.max()) / 2 * view_scale

    screen = points.copy()
    screen[..., 0] = screen[..., 0] * view_scale + offset_x
    screen[..., 1] = screen[..., 1] * view_scale + offset_y

    frame, depth, ident, mask = _rasterize(screen, shade, geometry.colors,
                                           geometry.cube_ids, hi_w, hi_h)
    frame = _outline(frame, depth, ident, mask)

    frame = frame.reshape(height, ss, width, ss, 3).mean(axis=(1, 3))
    image = Image.fromarray(np.clip(frame, 0, 255).astype(np.uint8))

    if title:
        ImageDraw.Draw(image).text((8, 6), title, fill=(198, 204, 212))
    return image


def _outline(frame: np.ndarray, depth: np.ndarray, ident: np.ndarray,
             mask: np.ndarray) -> np.ndarray:
    """Dark line along cube joints and the outer silhouette.

    A joint is where depth jumps or the cube id changes; the ndimage minimum and
    maximum filters find those pixels in one pass.
    """
    filled = np.where(mask, depth, 0.0)
    high = ndimage.maximum_filter(filled, size=3)
    low = ndimage.minimum_filter(np.where(mask, depth, np.inf), size=3)
    step = (high - low) > 0.55

    id_max = ndimage.maximum_filter(ident, size=3)
    id_min = ndimage.minimum_filter(ident, size=3)
    seam = (id_max != id_min) & mask & step

    border = mask & ~ndimage.binary_erosion(mask, np.ones((3, 3), bool))

    edges = (step & mask) | seam | border
    out = frame.copy()
    out[edges] *= 0.62
    return out


def sheet(model: Model, name: str, texture: Image.Image | None = None,
          tile: Tuple[int, int] = (420, 320)) -> Image.Image:
    """Four views of one model on a single sheet."""
    views = (
        (34.0, 24.0, "3/4"),
        (90.0, 0.0, "side"),
        (0.0, 0.0, "front"),
        (180.0, 89.9, "plan"),
    )
    tiles = [render_view(model, az, el, size=tile, title=f"{name} — {label}", texture=texture)
             for az, el, label in views]
    canvas = Image.new("RGB", (tile[0] * 2, tile[1] * 2), tuple(BACKGROUND.astype(int)))
    for index, image in enumerate(tiles):
        canvas.paste(image, ((index % 2) * tile[0], (index // 2) * tile[1]))
    return canvas


def contact_sheet(models: Sequence, columns: int = 5,
                  tile: Tuple[int, int] = (300, 220)) -> Image.Image:
    """models: a sequence of (name, model) or (name, model, texture)."""
    rows = (len(models) + columns - 1) // columns
    canvas = Image.new("RGB", (tile[0] * columns, tile[1] * rows),
                       tuple(BACKGROUND.astype(int)))
    for index, entry in enumerate(models):
        name, model = entry[0], entry[1]
        texture = entry[2] if len(entry) > 2 else None
        image = render_view(model, size=tile, title=name, texture=texture)
        canvas.paste(image, ((index % columns) * tile[0], (index // columns) * tile[1]))
    return canvas


@dataclass
class Silhouette:
    """Numbers that say whether the outline reads as the real aircraft."""

    blobs: int
    holes: int
    fill: float
    aspect: float
    elongation: float


def silhouette(model: Model, azimuth: float, elevation: float,
               size: Tuple[int, int] = (320, 240)) -> Silhouette:
    """Projection metrics: connectivity and proportions."""
    geometry = build_geometry(model)
    width, height = size
    points = _project(geometry, azimuth, elevation)

    xs, ys = points[..., 0], points[..., 1]
    span = max(xs.max() - xs.min(), ys.max() - ys.min()) or 1.0
    scale = min(width, height) * 0.86 / span
    screen = points.copy()
    screen[..., 0] = screen[..., 0] * scale + width / 2 - (xs.min() + xs.max()) / 2 * scale
    screen[..., 1] = screen[..., 1] * scale + height / 2 - (ys.min() + ys.max()) / 2 * scale

    shade = np.ones((geometry.count, 1))
    _frame, _depth, _ident, mask = _rasterize(screen, shade, geometry.colors,
                                              geometry.cube_ids, width, height)

    _labels, blobs = ndimage.label(mask)
    holes = ndimage.label(ndimage.binary_fill_holes(mask) & ~mask)[1]

    rows = np.where(mask.any(axis=1))[0]
    cols = np.where(mask.any(axis=0))[0]
    box_h = rows[-1] - rows[0] + 1 if rows.size else 1
    box_w = cols[-1] - cols[0] + 1 if cols.size else 1
    fill = mask.sum() / float(box_w * box_h)

    ys_i, xs_i = np.nonzero(mask)
    if ys_i.size > 2:
        coords = np.stack([xs_i - xs_i.mean(), ys_i - ys_i.mean()])
        eigenvalues = np.linalg.eigvalsh(np.cov(coords))
        elongation = float(math.sqrt(max(eigenvalues) / max(1e-9, min(eigenvalues))))
    else:
        elongation = 1.0

    return Silhouette(blobs=blobs, holes=holes, fill=float(fill),
                      aspect=float(box_w) / float(box_h), elongation=elongation)


def describe(name: str, model: Model) -> str:
    """Report line: plan view and side profile."""
    plan = silhouette(model, 0.0, 89.9)
    side = silhouette(model, 90.0, 0.0)
    return (f"  {name:<18} plan: blobs {plan.blobs}, holes {plan.holes}, "
            f"fill {plan.fill:.2f}, span/length {plan.aspect:.2f}  |  "
            f"profile: fill {side.fill:.2f}, elongation {side.elongation:.1f}")


def _load_models(names: Sequence[str]):
    """Builds the named models, or every drone and turret when given no names."""
    from build_drones import DRONES, BUILDERS, rgb, aerial_bomb_model
    from build_turrets import BUILDERS as TURRETS

    wanted = set(names)
    models = []

    for drone_id, archetype, length, span, color, params in DRONES:
        if wanted and drone_id not in wanted:
            continue
        model = Model(drone_id)
        BUILDERS[archetype](model, length, span, rgb(color), params)
        model.normalize()
        models.append((drone_id, model))

    if not wanted or "aerial_bomb" in wanted:
        bomb = aerial_bomb_model()
        bomb.normalize()
        models.append(("aerial_bomb", bomb))

    for turret_id, builder in TURRETS.items():
        if wanted and turret_id not in wanted:
            continue
        model = builder()
        model.normalize()
        models.append((turret_id, model))

    return models


def main(argv: Sequence[str]) -> int:
    import argparse

    parser = argparse.ArgumentParser(description="Render the mod models to PNG.")
    parser.add_argument("names", nargs="*", help="model ids (default: all of them)")
    parser.add_argument("-o", "--out", default="build/preview", help="output directory")
    parser.add_argument("--textured", action="store_true",
                        help="take face colours from the built textures")
    args = parser.parse_args(argv)

    models = _load_models(args.names)
    if not models:
        print("no model matches those names")
        return 1

    assets = Path(__file__).resolve().parents[1] / "src/main/resources/assets/airsystem"
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)

    entries = []
    for name, model in models:
        texture = None
        if args.textured:
            for folder in ("drone", "turret"):
                candidate = assets / f"textures/entity/{folder}/{name}.png"
                if candidate.exists():
                    texture = Image.open(candidate)
                    break
        sheet(model, name, texture).save(out / f"{name}.png")
        entries.append((name, model, texture))
        print(describe(name, model))

    contact_sheet(entries).save(out / "_all.png")
    print(f"  images written to {out}/")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
