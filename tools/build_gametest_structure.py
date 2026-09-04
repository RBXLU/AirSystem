"""Empty arena structure for the game tests.

GameTest demands a structure template even for tests that need no blocks, so this
writes the minimal valid NBT: a size, an empty palette and empty block lists."""
from __future__ import annotations

import gzip
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/data/airsystem/structure/empty.nbt"

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


def name(text: str) -> bytes:
    raw = text.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def tag_int(key: str, value: int) -> bytes:
    return bytes([TAG_INT]) + name(key) + struct.pack(">i", value)


def int_list(key: str, values) -> bytes:
    payload = bytes([TAG_LIST]) + name(key) + bytes([TAG_INT]) + struct.pack(">i", len(values))
    for value in values:
        payload += struct.pack(">i", value)
    return payload


def empty_list(key: str) -> bytes:
    return bytes([TAG_LIST]) + name(key) + bytes([TAG_END]) + struct.pack(">i", 0)


def compound_list(key: str, entries) -> bytes:
    payload = bytes([TAG_LIST]) + name(key) + bytes([TAG_COMPOUND]) + struct.pack(">i", len(entries))
    for entry in entries:
        payload += entry + bytes([TAG_END])
    return payload


def build() -> None:
    palette_entry = bytes([TAG_STRING]) + name("Name") + name("minecraft:air")

    body = b""
    body += tag_int("DataVersion", 3955)
    body += int_list("size", [5, 5, 5])
    body += compound_list("palette", [palette_entry])
    body += empty_list("blocks")
    body += empty_list("entities")

    root = bytes([TAG_COMPOUND]) + name("") + body + bytes([TAG_END])

    OUT.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(OUT, "wb") as handle:
        handle.write(root)
    print(f"  {OUT.relative_to(ROOT)} — {OUT.stat().st_size} bytes")


if __name__ == "__main__":
    print("Game test arena:")
    build()
