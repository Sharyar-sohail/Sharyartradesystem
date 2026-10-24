"""
Asset Generator: Creates custom high-resolution application icon (.ico)
and branding assets for the Binance Pro Trading Terminal.
"""

from PIL import Image, ImageDraw
from pathlib import Path

ASSETS_DIR = Path(__file__).resolve().parent / "assets"
ASSETS_DIR.mkdir(parents=True, exist_ok=True)
ICO_PATH = ASSETS_DIR / "icon.ico"
PNG_PATH = ASSETS_DIR / "icon.png"

def create_terminal_icon():
    """Generates a sleek, high-res dark gold/emerald financial terminal icon."""
    size = (256, 256)
    image = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    # Outer rounded background
    # Background polygon / rounded rectangle
    draw.rounded_rectangle([(10, 10), (246, 246)], radius=45, fill=(11, 14, 20, 255), outline=(0, 230, 153, 220), width=6)

    # Inner glowing tech grid / lines
    draw.line([(40, 190), (216, 190)], fill=(30, 41, 59, 255), width=3)
    draw.line([(40, 140), (216, 140)], fill=(30, 41, 59, 255), width=3)
    draw.line([(40, 90), (216, 90)], fill=(30, 41, 59, 255), width=3)

    # Stylized Candlesticks (Green & Gold)
    # Candle 1 (Green)
    draw.line([(70, 70), (70, 180)], fill=(0, 230, 153, 255), width=3)
    draw.rectangle([(58, 95), (82, 160)], fill=(0, 230, 153, 255))

    # Candle 2 (Gold / Yellow - High Impulse)
    draw.line([(128, 40), (128, 210)], fill=(245, 158, 11, 255), width=3)
    draw.rectangle([(116, 65), (140, 175)], fill=(245, 158, 11, 255))

    # Candle 3 (Cyan / Breakout)
    draw.line([(186, 50), (186, 190)], fill=(56, 189, 248, 255), width=3)
    draw.rectangle([(174, 75), (198, 145)], fill=(56, 189, 248, 255))

    # Ascending Trend / Lightning Bolt Arrow
    trend_points = [(50, 180), (100, 150), (150, 110), (205, 55)]
    draw.line(trend_points, fill=(255, 255, 255, 240), width=5)

    # Arrowhead
    draw.polygon([(205, 55), (185, 60), (200, 75)], fill=(255, 255, 255, 255))

    # Save as PNG
    image.save(PNG_PATH, format="PNG")

    # Save as Multi-Resolution ICO
    image.save(
        ICO_PATH,
        format="ICO",
        sizes=[(256, 256), (128, 128), (64, 64), (48, 48), (32, 32), (16, 16)]
    )
    print(f"Icon generated successfully at: {ICO_PATH}")

if __name__ == "__main__":
    create_terminal_icon()
