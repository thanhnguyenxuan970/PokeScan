"""
Build perceptual hash database for set symbol disambiguation.

Usage:
    pip install imagehash requests Pillow
    python build_phash_db.py

Output:
    set_phashes.json  -- copy to android/app/src/main/assets/ and PokeScan/Resources/

Each entry: {"setCode": "hex64bitHash", ...}
Hash computed from bottom-right 15% crop of first card image per set.
Hamming distance <= 10 = same set; > 15 = different set.
"""

import json
import time
from io import BytesIO
from pathlib import Path

import imagehash
import requests
from PIL import Image

SET_DB_PATH = Path(__file__).parent.parent.parent / "android/app/src/main/assets/set_database.json"
OUTPUT_PATH = Path(__file__).parent / "set_phashes.json"
ANDROID_ASSETS = Path(__file__).parent.parent.parent / "android/app/src/main/assets/set_phashes.json"
IOS_RESOURCES = Path(__file__).parent.parent.parent / "PokeScan/Resources/set_phashes.json"

API_BASE = "https://api.pokemontcg.io/v2"
RATE_LIMIT_DELAY = 0.5  # seconds between requests (avoid 429)


def compute_phash(image_url: str) -> str | None:
    try:
        resp = requests.get(image_url, timeout=15)
        resp.raise_for_status()
        img = Image.open(BytesIO(resp.content)).convert("RGB")

        # Crop bottom-right 15% = set symbol region
        w, h = img.size
        crop_x = int(w * 0.85)
        crop_y = int(h * 0.85)
        cropped = img.crop((crop_x, crop_y, w, h))

        phash = imagehash.phash(cropped)
        return str(phash)
    except Exception as e:
        print(f"  ERROR computing hash from {image_url}: {e}")
        return None


def get_first_card_image(set_code: str) -> str | None:
    url = f"{API_BASE}/cards"
    params = {"q": f"set.id:{set_code}", "pageSize": 1}
    try:
        resp = requests.get(url, params=params, timeout=15)
        resp.raise_for_status()
        data = resp.json()
        cards = data.get("data", [])
        if not cards:
            print(f"  No cards found for set {set_code}")
            return None
        images = cards[0].get("images", {})
        return images.get("large") or images.get("small")
    except Exception as e:
        print(f"  ERROR fetching cards for {set_code}: {e}")
        return None


def main():
    if not SET_DB_PATH.exists():
        print(f"set_database.json not found at {SET_DB_PATH}")
        return

    with open(SET_DB_PATH) as f:
        sets = json.load(f)

    set_codes = [s["setCode"] for s in sets if "setCode" in s]
    print(f"Processing {len(set_codes)} sets...")

    hashes: dict[str, str] = {}
    failed: list[str] = []

    for i, code in enumerate(set_codes):
        print(f"[{i+1}/{len(set_codes)}] {code}")
        image_url = get_first_card_image(code)
        time.sleep(RATE_LIMIT_DELAY)

        if image_url:
            ph = compute_phash(image_url)
            if ph:
                hashes[code] = ph
                print(f"  {ph}")
            else:
                failed.append(code)
        else:
            failed.append(code)

    # Write output
    OUTPUT_PATH.write_text(json.dumps(hashes, indent=2))
    print(f"\nWrote {len(hashes)} hashes to {OUTPUT_PATH}")

    if failed:
        print(f"Failed ({len(failed)}): {', '.join(failed)}")

    # Copy to asset dirs
    for dest in [ANDROID_ASSETS, IOS_RESOURCES]:
        if dest.parent.exists():
            dest.write_text(json.dumps(hashes, indent=2))
            print(f"Copied to {dest}")
        else:
            print(f"Skipped (dir not found): {dest}")

    print("\nDone. Verify collision pairs:")
    for pair in [("base1", "ex5"), ("base2-jp", "base3-jp")]:
        a, b = pair
        if a in hashes and b in hashes:
            ha = imagehash.hex_to_hash(hashes[a])
            hb = imagehash.hex_to_hash(hashes[b])
            dist = ha - hb
            print(f"  {a} vs {b}: Hamming distance = {dist} ({'DISTINCT ✓' if dist > 10 else 'COLLISION WARNING'})")


if __name__ == "__main__":
    main()
