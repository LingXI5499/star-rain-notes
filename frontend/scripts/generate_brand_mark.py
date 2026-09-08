"""Emit the horizontal wordmark from the uploaded logo source.

Icon / favicon PNGs are rasterized from public/brand/mark.svg (see the
sharp-based one-off in this folder). Do not crop the tiny source photo
into mark.png — it goes muddy at header size.
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image

SRC = Path(__file__).with_name("brand-logo-source.png")
OUT = Path(__file__).resolve().parents[1] / "public" / "brand"


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    wordmark = Image.open(SRC).convert("RGB")
    tw = 352
    th = max(1, round(tw * wordmark.size[1] / wordmark.size[0]))
    wordmark.resize((tw, th), Image.Resampling.LANCZOS).save(OUT / "logo-wordmark.png", optimize=True)
    print("saved logo-wordmark.png", tw, th)


if __name__ == "__main__":
    main()
