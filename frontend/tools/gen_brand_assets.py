"""Genera assets de marca BUCARATRANSIT desde new_logo.png (azul rey #01265A + amarillo #FCBB01).

El logo es arte flotante azul+amarillo (sin badge sólido): sobre azul rey su propio arte
azul desaparece, por eso los iconos y la versión de UI van sobre BLANCO.

Idempotente. Corre con:
    python frontend/tools/gen_brand_assets.py
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "new_logo.png"
RES = ROOT / "frontend/composeApp/src"

WHITE = (255, 255, 255, 255)

# fracción del ANCHO del canvas que debe ocupar el arte del logo.
# El arte real del logo ocupa ~81% de su cuadrado, de ahí el factor 1/0.81.
ART_FRACTION = 0.81

# 108dp (icono adaptativo) y 48dp (icono legacy) por densidad
DENSITY = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}


def bus_glyph(px: int) -> Image.Image:
    """Silueta BLANCA de bus (para ic_notification: Android la tinta, debe ser plana)."""
    from PIL import ImageDraw

    s = px * 8  # supersample para bordes suaves
    mask = Image.new("L", (s, s), 0)
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle([0.16 * s, 0.12 * s, 0.84 * s, 0.80 * s], radius=0.11 * s, fill=255)
    for i in range(3):  # ventanas (huecos)
        x0 = (0.27 + i * 0.17) * s
        d.rounded_rectangle([x0, 0.24 * s, x0 + 0.11 * s, 0.44 * s], radius=0.03 * s, fill=0)
    d.ellipse([0.24 * s, 0.78 * s, 0.42 * s, 0.94 * s], fill=255)  # ruedas
    d.ellipse([0.58 * s, 0.78 * s, 0.76 * s, 0.94 * s], fill=255)
    out = Image.new("RGBA", (px, px), (255, 255, 255, 0))
    out.putalpha(mask.resize((px, px), Image.LANCZOS))
    return out


def place(logo: Image.Image, size: int, art_w: float, bg) -> Image.Image:
    """Logo centrado en un canvas `size`² con el arte ocupando `art_w` del ancho."""
    canvas = Image.new("RGBA", (size, size), bg)
    side = max(1, round(size * art_w / ART_FRACTION))
    art = logo.resize((side, side), Image.LANCZOS)
    off = ((size - side) // 2, (size - side) // 2)
    canvas.alpha_composite(art, off)
    return canvas


def main() -> None:
    logo = Image.open(SRC).convert("RGBA")

    # 1) Icono adaptativo (Android 8+): fondo por @color (blanco) + foreground en zona segura
    # 2) Icono legacy (pre-Android 8): fondo blanco + logo
    for d, scale in DENSITY.items():
        dpi = round(108 * scale)  # canvas 108dp
        fg = place(logo, dpi, art_w=0.70, bg=(0, 0, 0, 0))
        fg.save(RES / f"main/res/mipmap-{d}/ic_launcher_foreground.webp", "WEBP", lossless=True)
        legacy = round(48 * scale)
        for name in ("ic_launcher", "ic_launcher_round"):
            place(logo, legacy, art_w=0.84, bg=WHITE).save(
                RES / f"main/res/mipmap-{d}/{name}.webp", "WEBP", lossless=True
            )

    # 3) Play Store 512×512 PNG (sin transparencia, exige fondo sólido)
    place(logo, 512, art_w=0.84, bg=WHITE).save(
        RES / "androidMain/ic_launcher-playstore.png", "PNG"
    )

    # 4) favicon.ico multi-tamaño
    place(logo, 64, art_w=0.90, bg=WHITE).save(
        ROOT / "bus_unab/public/favicon.ico",
        sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64)],
    )

    # 5) Logo para la UI (Compose): recortado a su arte, sobre blanco para que lea
    #    en el gradiente del login y en dark mode.
    bbox = logo.getchannel("A").getbbox()
    pad = 24
    l, t, r, b = bbox
    cropped = logo.crop(
        (max(0, l - pad), max(0, t - pad), min(logo.width, r + pad), min(logo.height, b + pad))
    )
    ui = Image.new("RGBA", cropped.size, WHITE)
    ui.alpha_composite(cropped)
    ui.convert("RGB").save(
        RES / "commonMain/composeResources/drawable/logo_bucaratransit.webp", "WEBP", quality=92
    )

    # 6) ic_notification: silueta BLANCA de bus (Android la tinta; multicolor = manchon)
    for d, scale in DENSITY.items():
        bus_glyph(round(24 * scale)).save(
            RES / f"main/res/mipmap-{d}/ic_notification.webp", "WEBP", lossless=True
        )

    print("assets de marca OK (iconos y logo de UI sobre blanco)")


if __name__ == "__main__":
    main()
