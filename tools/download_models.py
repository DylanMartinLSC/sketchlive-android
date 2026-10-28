"""Download and verify the exact Android model pack for JVM inference tests (stdlib only)."""
import hashlib
import json
from pathlib import Path
import sys
import urllib.request
import argparse

REVISION = "30015d1ff375a67d2ade66f9a43339adf5cd5639"
BASE = f"https://huggingface.co/xay2001/sd-turbo-android/resolve/{REVISION}/sd-turbo-v1/"


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("destination")
    parser.add_argument("--mode",choices=["quality","fast"],default="quality")
    args=parser.parse_args()
    destination = Path(args.destination)
    destination.mkdir(parents=True, exist_ok=True)
    asset="sdxs-manifest.json" if args.mode=="fast" else "model-manifest.json"
    manifest = json.loads((Path(__file__).parents[1] / "app/src/main/assets" / asset).read_text())
    base=manifest.get("base_url",BASE)
    for item in manifest["files"]:
        if "_512" in item["name"]:
            continue
        target = destination / item["name"]
        def verified(path):
            if not path.exists() or path.stat().st_size != item["size"]:
                return False
            with path.open("rb") as source:
                digest = hashlib.sha256()
                for block in iter(lambda: source.read(1024 * 1024), b""):
                    digest.update(block)
            return digest.hexdigest() == item["sha256"]
        if not verified(target):
            partial = target.with_suffix(target.suffix + ".part")
            print("Downloading", item["name"], flush=True)
            urllib.request.urlretrieve(base + item["name"], partial)
            if not verified(partial):
                raise RuntimeError(f"Checksum mismatch: {partial}")
            partial.replace(target)
        print("Verified", item["name"], flush=True)
    print("Models ready at", destination.resolve())


if __name__ == "__main__":
    main()
