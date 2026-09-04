"""Geometry library for cube models, in sixteenths of a block.

Packs the box UV, paints the faces and emits either .geo.json or vanilla
LayerDefinition code."""
from __future__ import annotations

import json
import math
import random
from dataclasses import dataclass, field
from typing import List, Optional, Tuple

from PIL import Image


@dataclass
class Cube:
    origin: Tuple[float, float, float]
    size: Tuple[float, float, float]
    color: Tuple[int, int, int]
    rotation: Optional[Tuple[float, float, float]] = None
    pivot: Optional[Tuple[float, float, float]] = None
    inflate: float = 0.0
    detail: str = "panel"
    uv: Tuple[int, int] = (0, 0)
    label: Optional[str] = None

    def int_size(self) -> Tuple[int, int, int]:
        """Integer sizes: the vanilla format derives the UV from these too."""
        return tuple(max(1, int(round(v))) for v in self.size)

    def uv_size(self) -> Tuple[int, int]:
        w, h, d = self.int_size()
        return 2 * (w + d), h + d


STENCIL_FONT = {
    "0": ("111", "101", "101", "101", "111"),
    "1": ("010", "110", "010", "010", "111"),
    "2": ("111", "001", "111", "100", "111"),
    "3": ("111", "001", "111", "001", "111"),
    "4": ("101", "101", "111", "001", "001"),
    "5": ("111", "100", "111", "001", "111"),
    "6": ("111", "100", "111", "101", "111"),
    "7": ("111", "001", "010", "010", "010"),
    "8": ("111", "101", "111", "101", "111"),
    "9": ("111", "101", "111", "001", "111"),
    "-": ("000", "000", "111", "000", "000"),
}


@dataclass
class Bone:
    name: str
    pivot: Tuple[float, float, float] = (0.0, 0.0, 0.0)
    parent: Optional[str] = None
    rotation: Optional[Tuple[float, float, float]] = None
    cubes: List[Cube] = field(default_factory=list)

    def add(self, *cubes: Cube) -> "Bone":
        self.cubes.extend(cubes)
        return self


class Model:
    def __init__(self, identifier: str):
        self.identifier = identifier
        self.bones: List[Bone] = []

    def bone(self, name: str, pivot=(0.0, 0.0, 0.0), parent=None, rotation=None) -> Bone:
        bone = Bone(name=name, pivot=pivot, parent=parent, rotation=rotation)
        self.bones.append(bone)
        return bone

    def all_cubes(self) -> List[Cube]:
        return [cube for bone in self.bones for cube in bone.cubes]


    def normalize(self) -> None:
        """Rounds cube sizes to whole units.

        The renderer derives the UV layout from the same numbers as the geometry,
        so texture and model have to agree on them.
        """
        for cube in self.all_cubes():
            cube.size = tuple(float(v) for v in cube.int_size())

    def pack_uv(self) -> int:
        """Shelf-packs the box UV and returns the texture side length."""
        self.normalize()
        cubes = sorted(self.all_cubes(), key=lambda c: -c.uv_size()[1])
        for texture_size in (64, 128, 256, 512):
            if self._try_pack(cubes, texture_size):
                return texture_size
        raise RuntimeError(f"could not pack the UV of model {self.identifier}")

    @staticmethod
    def _try_pack(cubes: List[Cube], size: int) -> bool:
        x = y = shelf_height = 0
        for cube in cubes:
            w, h = cube.uv_size()
            if w > size or h > size:
                return False
            if x + w > size:
                x = 0
                y += shelf_height + 1
                shelf_height = 0
            if y + h > size:
                return False
            cube.uv = (x, y)
            x += w + 1
            shelf_height = max(shelf_height, h)
        return True


    def to_geo(self, texture_size: int, bounds: Tuple[float, float, float]) -> dict:
        bones = []
        for bone in self.bones:
            entry = {"name": bone.name, "pivot": list(bone.pivot)}
            if bone.parent:
                entry["parent"] = bone.parent
            if bone.rotation:
                entry["rotation"] = list(bone.rotation)
            if bone.cubes:
                entry["cubes"] = [self._cube_json(cube) for cube in bone.cubes]
            bones.append(entry)

        return {
            "format_version": "1.12.0",
            "minecraft:geometry": [
                {
                    "description": {
                        "identifier": f"geometry.{self.identifier}",
                        "texture_width": texture_size,
                        "texture_height": texture_size,
                        "visible_bounds_width": bounds[0],
                        "visible_bounds_height": bounds[1],
                        "visible_bounds_offset": [0, bounds[2], 0],
                    },
                    "bones": bones,
                }
            ],
        }

    @staticmethod
    def _cube_json(cube: Cube) -> dict:
        entry = {
            "origin": [round(v, 2) for v in cube.origin],
            "size": [round(v, 2) for v in cube.size],
            "uv": list(cube.uv),
        }
        if cube.inflate:
            entry["inflate"] = cube.inflate
        if cube.rotation:
            entry["rotation"] = [round(v, 2) for v in cube.rotation]
            entry["pivot"] = [round(v, 2) for v in (cube.pivot or cube.origin)]
        return entry


    def render_texture(self, texture_size: int, seed: int) -> Image.Image:
        """Paints the atlas: panel lines, rivets and wear."""
        image = Image.new("RGBA", (texture_size, texture_size), (0, 0, 0, 0))
        rng = random.Random(seed)

        for index, cube in enumerate(self.all_cubes()):
            w, h, d = cube.int_size()
            u, v = cube.uv
            base = cube.color

            faces = [
                ("up", u + d, v, w, d),
                ("down", u + d + w, v, w, d),
                ("side", u, v + d, d, h),
                ("front", u + d, v + d, w, h),
                ("side", u + d + w, v + d, d, h),
                ("back", u + 2 * d + w, v + d, w, h),
            ]

            for kind, fx, fy, fw, fh in faces:
                self._paint_face(image, rng, cube, base, kind, fx, fy, fw, fh, index)

        return image

    FACE_SHADE = {"up": 1.16, "down": 0.66, "side": 0.90, "front": 1.02, "back": 0.80}

    def _paint_face(self, image, rng, cube, base, kind, fx, fy, fw, fh, index) -> None:
        shade = self.FACE_SHADE[kind]
        detail = cube.detail

        if detail == "glass":
            self._paint_glass(image, fx, fy, fw, fh, shade)
            return
        if detail == "dark":
            self._paint_flat(image, rng, (34, 34, 38), fx, fy, fw, fh, shade * 1.05, noise=4)
            return

        if kind == "down":
            base = tuple(int(channel * 0.55 + 150 * 0.45) for channel in base)

        if detail == "composite":
            self._paint_composite(image, rng, base, fx, fy, fw, fh, shade, index)
            self._paint_stencil(image, cube, base, kind, fx, fy, fw, fh, shade)
            return

        self._paint_flat(image, rng, base, fx, fy, fw, fh, shade,
                         noise=5 if detail == "panel" else 3)

        if detail == "metal":
            streak = tuple(max(0, int(c * shade * 0.86)) for c in base) + (255,)
            for y in range(fy + 1, fy + fh - 1, 3):
                for x in range(fx, fx + fw):
                    if rng.random() < 0.7:
                        image.putpixel((x, y), streak)
            return

        if detail == "marking" and fw >= 4 and fh >= 3:
            band = (156, 44, 36, 255)
            for x in range(fx + 1, fx + fw - 1):
                image.putpixel((x, fy + fh // 2), band)
            return

        if detail != "panel":
            return

        line = tuple(max(0, int(c * shade * 0.74)) for c in base) + (255,)
        rivet = tuple(max(0, int(c * shade * 0.60)) for c in base) + (255,)

        if fw >= 6:
            step = max(3, fw // (2 + index % 2))
            for x in range(fx + step, fx + fw - 1, step):
                for y in range(fy, fy + fh):
                    image.putpixel((x, y), line)
                for y in range(fy + 1, fy + fh - 1, 3):
                    image.putpixel((x, y), rivet)
        if fh >= 6:
            for y in range(fy + fh // 2, fy + fh - 1, max(4, fh // 2)):
                for x in range(fx, fx + fw):
                    image.putpixel((x, y), line)

        self._paint_edges(image, base, fx, fy, fw, fh, shade, 0.80)


    def _paint_composite(self, image, rng, base, fx, fy, fw, fh, shade, index) -> None:
        """Matte moulded skin: no rivets, no panel grid.

        A Shahed airframe is glassfibre laid over carbon and pulled from a mould
        in one piece, so panel lines and rivets on it are simply wrong. The only
        lines are the mould seam and the wing root; the mottle is gelcoat, since
        per-pixel noise reads as dirt at this scale.
        """
        seed = index * 7919
        for x in range(fx, fx + fw):
            for y in range(fy, fy + fh):
                u = (x - fx) / max(1, fw - 1)
                v = (y - fy) / max(1, fh - 1)
                mottle = (math.sin((u * 2.3 + seed) * 1.7) * math.cos((v * 2.9 + seed) * 1.3))
                gradient = 1.0 - 0.08 * v
                factor = shade * gradient * (1.0 + 0.035 * mottle)
                color = tuple(max(0, min(255, int(channel * factor))) for channel in base)
                image.putpixel((x, y), color + (255,))

        if fh >= 5:
            seam = tuple(max(0, int(c * shade * 0.86)) for c in base) + (255,)
            y = fy + fh // 2
            for x in range(fx + 1, fx + fw - 1):
                image.putpixel((x, y), seam)

        self._paint_edges(image, base, fx, fy, fw, fh, shade, 0.88)

    @staticmethod
    def _paint_stencil(image, cube, base, kind, fx, fy, fw, fh, shade) -> None:
        """Stencilled serial, side faces only: a 3x5 glyph fits a five-pixel face."""
        if not cube.label or kind != "side":
            return
        text = cube.label[:3]
        need = len(text) * 4 - 1
        if fw < need or fh < 5:
            return

        ink = tuple(max(0, int(c * shade * 0.58)) for c in base) + (255,)
        x0 = fx + (fw - need) // 2
        y0 = fy + (fh - 5) // 2
        for position, glyph in enumerate(text):
            rows = STENCIL_FONT.get(glyph)
            if not rows:
                continue
            for dy, row in enumerate(rows):
                for dx, on in enumerate(row):
                    if on == "1":
                        image.putpixel((x0 + position * 4 + dx, y0 + dy), ink)

    @staticmethod
    def _paint_edges(image, base, fx, fy, fw, fh, shade, factor: float) -> None:
        """Edge shading, which brings out the joints between cubes in game."""
        edge = tuple(max(0, int(c * shade * factor)) for c in base) + (255,)
        for x in range(fx, fx + fw):
            image.putpixel((x, fy), edge)
            image.putpixel((x, fy + fh - 1), edge)
        for y in range(fy, fy + fh):
            image.putpixel((fx, y), edge)
            image.putpixel((fx + fw - 1, y), edge)

    @staticmethod
    def _paint_flat(image, rng, base, fx, fy, fw, fh, shade, noise: int) -> None:
        for x in range(fx, fx + fw):
            for y in range(fy, fy + fh):
                gradient = 1.0 - 0.10 * (y - fy) / max(1, fh - 1)
                jitter = rng.randint(-noise, noise)
                color = tuple(
                    max(0, min(255, int(channel * shade * gradient) + jitter)) for channel in base
                )
                image.putpixel((x, y), color + (255,))

    @staticmethod
    def _paint_glass(image, fx, fy, fw, fh, shade) -> None:
        for x in range(fx, fx + fw):
            for y in range(fy, fy + fh):
                t = (x - fx) / max(1, fw - 1)
                color = (
                    int((30 + 26 * t) * shade),
                    int((44 + 34 * t) * shade),
                    int((58 + 40 * t) * shade),
                    255,
                )
                image.putpixel((x, y), color)
        if fw >= 3 and fh >= 3:
            image.putpixel((fx + 1, fy + 1), (176, 198, 212, 255))


def write_json(path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def _java_name(identifier: str) -> str:
    return "".join(part.capitalize() for part in identifier.split("_"))


def _f(value: float) -> str:
    return f"{value:.1f}F"


class VanillaEmitter:
    """Turns cubes into vanilla LayerDefinition code.

    The vanilla model space is flipped, and the renderer mirrors X and Y, so the
    coordinates become x' = -(x + width), y' = -(y + height), z unchanged. Box UV
    is identical in both formats, so the textures carry over untouched.
    """

    def __init__(self, model: Model, texture_size: int):
        self.model = model
        self.texture_size = texture_size

    @staticmethod
    def _vanilla_point(x: float, y: float, z: float) -> Tuple[float, float, float]:
        return -x, -y, z

    def _cube_min(self, cube: Cube) -> Tuple[float, float, float]:
        w, h, _d = cube.size
        return -(cube.origin[0] + w), -(cube.origin[1] + h), cube.origin[2]

    def emit_method(self, method_suffix: str) -> str:
        lines: List[str] = []
        lines.append(f"    public static LayerDefinition create{method_suffix}() {{")
        lines.append("        MeshDefinition mesh = new MeshDefinition();")
        lines.append("        PartDefinition root = mesh.getRoot();")
        lines.append("")

        pivots = {bone.name: self._vanilla_point(*bone.pivot) for bone in self.model.bones}
        variables = {}

        for bone in self.model.bones:
            parent = bone.parent
            parent_variable = variables.get(parent, "root")
            parent_pivot = pivots.get(parent, (0.0, 0.0, 0.0)) if parent else (0.0, 0.0, 0.0)
            pivot = pivots[bone.name]
            offset = tuple(pivot[i] - parent_pivot[i] for i in range(3))

            plain = [cube for cube in bone.cubes if not cube.rotation]
            rotated = [cube for cube in bone.cubes if cube.rotation]

            variable = bone.name.replace("-", "_")
            variables[bone.name] = variable

            builder = self._cube_list(plain, pivot)
            lines.append(f"        PartDefinition {variable} = {parent_variable}.addOrReplaceChild(\"{bone.name}\",")
            lines.append(f"                {builder},")
            lines.append(f"                PartPose.offset({_f(offset[0])}, {_f(offset[1])}, {_f(offset[2])}));")
            lines.append("")

            for index, cube in enumerate(rotated):
                cube_pivot = self._vanilla_point(*(cube.pivot or cube.origin))
                local_pivot = tuple(cube_pivot[i] - pivot[i] for i in range(3))
                angle_x = -math.radians(cube.rotation[0])
                angle_y = -math.radians(cube.rotation[1])
                angle_z = math.radians(cube.rotation[2])
                child = f"{variable}_r{index}"
                lines.append(f"        {parent_variable if False else variable}.addOrReplaceChild(\"{child}\",")
                lines.append(f"                {self._cube_list([cube], cube_pivot)},")
                lines.append(
                    f"                PartPose.offsetAndRotation({_f(local_pivot[0])}, {_f(local_pivot[1])}, "
                    f"{_f(local_pivot[2])}, {angle_x:.4f}F, {angle_y:.4f}F, {angle_z:.4f}F));"
                )
                lines.append("")

        lines.append(
            f"        return LayerDefinition.create(mesh, {self.texture_size}, {self.texture_size});"
        )
        lines.append("    }")
        return "\n".join(lines)

    def _cube_list(self, cubes: List[Cube], pivot: Tuple[float, float, float]) -> str:
        if not cubes:
            return "CubeListBuilder.create()"

        parts = ["CubeListBuilder.create()"]
        for cube in cubes:
            minimum = self._cube_min(cube)
            local = tuple(minimum[i] - pivot[i] for i in range(3))
            w, h, d = cube.int_size()
            parts.append(
                f"                        .texOffs({cube.uv[0]}, {cube.uv[1]})"
                f".addBox({_f(local[0])}, {_f(local[1])}, {_f(local[2])}, "
                f"{w}.0F, {h}.0F, {d}.0F)"
            )
        return "\n".join(parts)


def emit_layer_class(package: str, class_name: str, generator: str, methods: List[str],
                     dispatch: str = "", extra_imports: Optional[List[str]] = None) -> str:
    """Assembles the generated LayerDefinition source file."""
    header = [
        f"package {package};",
        "",
    ]
    for extra in extra_imports or []:
        header.append(f"import {extra};")
    if extra_imports:
        header.append("")
    header += [
        "import net.minecraft.client.model.geom.PartPose;",
        "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
        "import net.minecraft.client.model.geom.builders.LayerDefinition;",
        "import net.minecraft.client.model.geom.builders.MeshDefinition;",
        "import net.minecraft.client.model.geom.builders.PartDefinition;",
        "",
        f"// Generated by {generator} — edit the script, not this file.",
    ]
    header.extend([
        f"public final class {class_name} {{",
        "",
    ])

    body = "\n\n".join(methods)
    footer = []
    if dispatch:
        footer.append("")
        footer.append(dispatch)
    footer.extend([
        "",
        f"    private {class_name}() {{",
        "    }",
        "}",
        "",
    ])
    return "\n".join(header) + body + "\n".join(footer)
