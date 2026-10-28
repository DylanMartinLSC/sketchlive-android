"""Render a gallery from actual Android Fast-mode PNGs. Requires Pillow and FFmpeg."""
from pathlib import Path
import subprocess
import tempfile
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1]

def font(size):
    for path in ["C:/Windows/Fonts/segoeui.ttf","/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"]:
        if Path(path).exists():return ImageFont.truetype(path,size)
    return ImageFont.load_default(size=size)

def main():
    frames=[]
    for name,title in [("cottage","Mushroom cottage"),("robot","Friendly robot"),("lighthouse","Stormy lighthouse")]:
        canvas=Image.new("RGB",(840,548),"#10141c");d=ImageDraw.Draw(canvas)
        d.text((24,16),"Fast mode · "+title,font=font(27),fill="#eaf0fa")
        d.text((24,70),"YOUR SKETCH",font=font(13),fill="#aab8cd")
        d.text((432,70),"SDXS SKETCH · 256 PX · ONE STEP",font=font(13),fill="#8ea4ff")
        for kind,x in [("sketch",24),("preview",432)]:
            image=Image.open(ROOT/f"docs/fast-examples/{name}-{kind}.png").convert("RGB")
            if kind=="preview":assert image.size==(256,256)
            canvas.paste(image.resize((384,384),Image.Resampling.LANCZOS),(x,98))
        d.text((24,502),"Real Android app output · SDXS + Sketch ControlNet · 1.26 GB model pack",font=font(14),fill="#eaf0fa")
        d.text((24,525),"Android 15 emulator, CPU, offline · gallery playback is not inference timing",font=font(12),fill="#aab8cd")
        frames.append(canvas)
    scratch=ROOT/"build";scratch.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="fast-gif-",dir=scratch) as directory:
        directory=Path(directory);timeline=[]
        for i,frame in enumerate(frames):
            frame.save(directory/f"{i}.png");timeline.extend([f"file '{i}.png'","duration 2.5"])
        timeline.append("file '2.png'");(directory/"frames.txt").write_text("\n".join(timeline)+"\n")
        subprocess.run(["ffmpeg","-hide_banner","-loglevel","error","-y","-f","concat","-safe","0","-i","frames.txt","-filter_complex","split[a][b];[a]palettegen[p];[b][p]paletteuse=dither=sierra2_4a","-fps_mode","vfr","-loop","0",str(ROOT/"docs/fast-mode.gif")],cwd=directory,check=True)

if __name__=="__main__":main()
