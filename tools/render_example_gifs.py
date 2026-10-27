"""Animate comparisons of untouched Android-generated PNGs; this never generates model output.

Install Pillow and FFmpeg, then run: python tools/render_example_gifs.py
The source PNGs and settings live in docs/examples/. Phone screen recordings are captured separately.
"""
from pathlib import Path
import math
import subprocess
import tempfile
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
EXAMPLES = ROOT / "docs/examples"
BG = "#10141c"


def font(size):
    for name in ["C:/Windows/Fonts/segoeui.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"]:
        if Path(name).is_file():
            return ImageFont.truetype(name, size)
    return ImageFont.load_default(size=size)


def render(name, title):
    sketch = Image.open(EXAMPLES / f"{name}-sketch.png").convert("RGB").resize((384, 384), Image.Resampling.LANCZOS)
    preview = Image.open(EXAMPLES / f"{name}-preview.png").convert("RGB")
    enhanced = Image.open(EXAMPLES / f"{name}-enhanced.png").convert("RGB")
    assert preview.size == enhanced.size == (384, 384)
    blank = Image.new("RGB", (384, 384), "#1c2330")
    d = ImageDraw.Draw(blank)
    d.text((84, 175), "A few shapes. A prompt.", font=font(18), fill="#aab8cd")
    frames, durations = [], []

    def add(right, phase, duration=100):
        frame = Image.new("RGB", (840, 548), BG)
        draw = ImageDraw.Draw(frame)
        draw.text((24, 15), title, font=font(27), fill="#eaf0fa")
        draw.text((624, 26), "SKETCHLIVE / ANDROID", font=font(13), fill="#8ea4ff")
        draw.text((24, 70), "YOUR SKETCH", font=font(13), fill="#aab8cd")
        draw.text((432, 70), phase, font=font(13), fill="#8ea4ff")
        frame.paste(sketch, (24, 98))
        frame.paste(right, (432, 98))
        draw.text((24, 502), "Real SD-Turbo output · 384 px · Android 15 emulator · CPU", font=font(15), fill="#eaf0fa")
        draw.text((24, 525), "Comparison animation — transitions do not represent inference time", font=font(12), fill="#aab8cd")
        frames.append(frame)
        durations.append(duration)

    add(blank, "READY TO GENERATE", 800)
    for i in range(1, 13):
        right = blank.copy()
        x = round(384 * (1 - math.cos(i / 12 * math.pi)) / 2)
        right.paste(preview.crop((0, 0, x, 384)), (0, 0))
        add(right, "PREVIEW · 1 DENOISING STEP", 80)
    add(preview, "PREVIEW · 1 DENOISING STEP", 1800)
    for i in range(1, 9):
        add(Image.blend(preview, enhanced, i / 8), "ENHANCE · 2 DENOISING STEPS", 80)
    add(enhanced, "ENHANCE · 2 DENOISING STEPS", 2400)
    output = ROOT / f"docs/{name}.gif"
    scratch = ROOT / "build"
    scratch.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="demo-gif-", dir=scratch) as temporary:
        temporary = Path(temporary)
        timeline = []
        for index, (frame, duration) in enumerate(zip(frames, durations)):
            filename = f"{index:04d}.png"
            frame.save(temporary / filename)
            timeline.extend([f"file '{filename}'", f"duration {duration / 1000:.3f}"])
        timeline.append(f"file '{len(frames) - 1:04d}.png'")
        (temporary / "frames.txt").write_text("\n".join(timeline) + "\n")
        subprocess.run([
            "ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-f", "concat", "-safe", "0", "-i", "frames.txt",
            "-filter_complex", "split[a][b];[a]palettegen=max_colors=256:stats_mode=full[p];[b][p]paletteuse=dither=sierra2_4a",
            "-fps_mode", "vfr", "-loop", "0", str(output),
        ], cwd=temporary, check=True)
    print(output.name, output.stat().st_size, "bytes")


if __name__ == "__main__":
    for name, title in [("cottage", "A home in the enchanted forest"), ("robot", "A little robot with character"), ("lighthouse", "A lighthouse in the storm")]:
        render(name, title)
