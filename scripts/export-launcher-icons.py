#!/usr/bin/env python3
"""Export the approved PowerSync artwork to Android resources (requires Pillow)."""

from pathlib import Path
import shutil

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "design/icons/powersync-aragonite-sync-v2.png"
RES = ROOT / "client/src/main/res"


def main():
    with Image.open(SOURCE) as source:
        if source.mode != "RGBA" or source.width != source.height:
            raise ValueError("Launcher source must be a square RGBA image")
        if source.getchannel("A").getextrema()[0] != 0:
            raise ValueError("Launcher source must have a transparent background")

        shutil.copyfile(SOURCE, RES / "drawable-nodpi/powersync_aragonite.png")
        # Adaptive/themed launchers use the full source through the existing inset.
        # Legacy launchers need a white background and density-specific dimensions.
        for density, size in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96),
                              ("xxhdpi", 144), ("xxxhdpi", 192)):
            icon = source.resize((size, size), Image.Resampling.LANCZOS)
            canvas = Image.new("RGBA", icon.size, "white")
            canvas.alpha_composite(icon)
            canvas.convert("RGB").save(RES / f"mipmap-{density}/ic_launcher.png")


if __name__ == "__main__":
    main()
