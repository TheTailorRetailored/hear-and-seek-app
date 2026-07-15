#!/usr/bin/env python3
"""Build README/social-preview images from a real emulator screenshot."""

from __future__ import annotations

import shutil
import sys
from pathlib import Path

from PIL import Image, ImageColor, ImageDraw, ImageFilter, ImageFont

WIDTH = 1280
HEIGHT = 640
SCALE = 2

NAVY = "#06111F"
DEEP = "#0E2030"
LIME = "#B6F36B"
CYAN = "#5CE1E6"
WHITE = "#F4F7FB"
MUTED = "#98AABD"


def font(name: str, size: int) -> ImageFont.FreeTypeFont:
    candidates = [
        Path("/usr/share/fonts/truetype/dejavu") / name,
        Path("/usr/share/fonts/dejavu") / name,
    ]
    for candidate in candidates:
        if candidate.exists():
            return ImageFont.truetype(str(candidate), size * SCALE)
    raise FileNotFoundError(f"Could not find {name}")


def rounded_rectangle(
    draw: ImageDraw.ImageDraw,
    box: tuple[float, float, float, float],
    radius: float,
    *,
    fill: str | None = None,
    outline: str | None = None,
    width: int = 1,
) -> None:
    draw.rounded_rectangle(
        tuple(int(value * SCALE) for value in box),
        radius=int(radius * SCALE),
        fill=fill,
        outline=outline,
        width=width * SCALE,
    )


def build_card(screenshot: Image.Image) -> Image.Image:
    canvas = Image.new("RGB", (WIDTH * SCALE, HEIGHT * SCALE), NAVY)
    draw = ImageDraw.Draw(canvas)

    # Subtle sound-wave rings behind the copy.
    waves = ImageDraw.Draw(canvas, "RGBA")
    cx, cy = 170 * SCALE, 310 * SCALE
    for radius, alpha, colour in [
        (190, 18, CYAN),
        (260, 14, LIME),
        (330, 10, CYAN),
    ]:
        rgba = (*ImageColor.getrgb(colour), alpha)
        bounds = (
            cx - radius * SCALE,
            cy - radius * SCALE,
            cx + radius * SCALE,
            cy + radius * SCALE,
        )
        waves.arc(bounds, -65, 65, fill=rgba, width=3 * SCALE)
        waves.arc(bounds, 115, 245, fill=rgba, width=3 * SCALE)

    title_font = font("DejaVuSans-Bold.ttf", 70)
    tagline_font = font("DejaVuSans.ttf", 32)
    body_font = font("DejaVuSans.ttf", 23)
    pill_font = font("DejaVuSans-Bold.ttf", 19)

    # App icon, recreated from the Android vector drawable.
    icon_x, icon_y, icon_size = 88, 78, 140
    rounded_rectangle(
        draw,
        (icon_x, icon_y, icon_x + icon_size, icon_y + icon_size),
        34,
        fill=DEEP,
    )
    icon_draw = ImageDraw.Draw(canvas)
    center_x = (icon_x + icon_size / 2) * SCALE
    center_y = (icon_y + icon_size / 2) * SCALE
    icon_draw.ellipse(
        (
            center_x - 16 * SCALE,
            center_y - 16 * SCALE,
            center_x + 16 * SCALE,
            center_y + 16 * SCALE,
        ),
        fill=LIME,
    )

    def arc(rx: int, ry: int, start: int, end: int, colour: str) -> None:
        icon_draw.arc(
            (
                center_x - rx * SCALE,
                center_y - ry * SCALE,
                center_x + rx * SCALE,
                center_y + ry * SCALE,
            ),
            start,
            end,
            fill=colour,
            width=6 * SCALE,
        )

    arc(33, 37, 135, 225, CYAN)
    arc(33, 37, -45, 45, CYAN)
    arc(52, 58, 125, 235, LIME)
    arc(52, 58, -55, 55, LIME)

    copy_x = 88
    draw.text((copy_x * SCALE, 247 * SCALE), "HEAR & SEEK", font=title_font, fill=WHITE)
    draw.text(
        (copy_x * SCALE, 340 * SCALE),
        "Hide the phone. Follow the sound.",
        font=tagline_font,
        fill=MUTED,
    )
    draw.text(
        (copy_x * SCALE, 393 * SCALE),
        "A sound-powered hide-and-seek game for Android.",
        font=body_font,
        fill=WHITE,
    )

    def pill(x: float, y: float, text: str, colour: str) -> float:
        bounds = draw.textbbox((0, 0), text, font=pill_font)
        text_width = (bounds[2] - bounds[0]) / SCALE
        pill_width = text_width + 42
        rounded_rectangle(
            draw,
            (x, y, x + pill_width, y + 48),
            24,
            fill=DEEP,
            outline=colour,
            width=2,
        )
        draw.text(
            ((x + 21) * SCALE, (y + 11) * SCALE),
            text,
            font=pill_font,
            fill=colour,
        )
        return x + pill_width

    next_x = pill(88, 478, "20 kHz → 800 Hz", CYAN) + 16
    pill(next_x, 478, "4 game modes", LIME)

    # Crop the real screenshot to the useful upper part of the home screen.
    crop = screenshot.crop((0, 0, screenshot.width, min(1380, screenshot.height)))
    target_width, target_height = 430, 560
    crop_ratio = crop.width / crop.height
    target_ratio = target_width / target_height
    if crop_ratio > target_ratio:
        resized_height = target_height
        resized_width = int(resized_height * crop_ratio)
    else:
        resized_width = target_width
        resized_height = int(resized_width / crop_ratio)
    crop = crop.resize(
        (resized_width * SCALE, resized_height * SCALE),
        Image.Resampling.LANCZOS,
    )
    left = max(0, (crop.width - target_width * SCALE) // 2)
    top = max(0, (crop.height - target_height * SCALE) // 2)
    crop = crop.crop(
        (
            left,
            top,
            left + target_width * SCALE,
            top + target_height * SCALE,
        )
    )

    phone_x, phone_y = 790, 40
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    shadow_draw = ImageDraw.Draw(shadow)
    shadow_draw.rounded_rectangle(
        (
            (phone_x + 12) * SCALE,
            (phone_y + 16) * SCALE,
            (phone_x + target_width + 12) * SCALE,
            (phone_y + target_height + 16) * SCALE,
        ),
        radius=36 * SCALE,
        fill=(0, 0, 0, 110),
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(18 * SCALE))
    canvas = Image.alpha_composite(canvas.convert("RGBA"), shadow)

    mask = Image.new("L", (target_width * SCALE, target_height * SCALE), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle(
        (0, 0, target_width * SCALE, target_height * SCALE),
        radius=34 * SCALE,
        fill=255,
    )
    canvas.paste(crop, (phone_x * SCALE, phone_y * SCALE), mask)

    border = ImageDraw.Draw(canvas)
    border.rounded_rectangle(
        (
            phone_x * SCALE,
            phone_y * SCALE,
            (phone_x + target_width) * SCALE,
            (phone_y + target_height) * SCALE,
        ),
        radius=34 * SCALE,
        outline=LIME,
        width=3 * SCALE,
    )

    return canvas.convert("RGB").resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS)


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: make_social_preview.py SCREENSHOT")

    screenshot_path = Path(sys.argv[1])
    output_dir = Path("docs/images")
    output_dir.mkdir(parents=True, exist_ok=True)

    screenshot = Image.open(screenshot_path).convert("RGB")
    shutil.copyfile(screenshot_path, output_dir / "home-screen.png")
    build_card(screenshot).save(output_dir / "social-preview.png", optimize=True)


if __name__ == "__main__":
    main()
