"""
Build perceptual hash database for set symbol disambiguation.

Usage:
    pip install requests Pillow
    python build_phash_db.py

Output:
    set_phashes.json  -- copy to android/app/src/main/assets/ and PokeScan/Resources/

Each entry: {"setCode": "hex64bitHash", ...}
Hash algorithm mirrors PHashService.kt/PHashService.swift exactly:
  BT.601 luminance -> 32x32 -> normalized 8x8 DCT-II (cu/cv factors, /4) ->
  mean of 63 AC coefficients (exclude DC[0,0]) -> 64-bit hash.
Hamming distance <= 10 = same set; > 15 = different set.
"""

import json
import math
import time
from io import BytesIO
from pathlib import Path

import requests
from PIL import Image

SET_DB_PATH = Path(__file__).parent.parent.parent / "android/app/src/main/assets/set_database.json"
OUTPUT_PATH = Path(__file__).parent / "set_phashes.json"
ANDROID_ASSETS = Path(__file__).parent.parent.parent / "android/app/src/main/assets/set_phashes.json"
IOS_RESOURCES = Path(__file__).parent.parent.parent / "PokeScan/Resources/set_phashes.json"

API_BASE = "https://api.pokemontcg.io/v2"
RATE_LIMIT_DELAY = 0.5  # seconds between requests (avoid 429)

_INV_SQRT2 = 1.0 / math.sqrt(2.0)
_PI_OVER_64 = math.pi / 64.0


def _compute_phash_compat(image: Image.Image) -> str:
    """
    Mirrors PHashService.kt computeHash() and PHashService.swift computeHash(from:UIImage) exactly.
    BT.601 luminance -> 32x32 -> normalized 8x8 DCT-II (cu/cv, /4) ->
    mean of 63 AC values (exclude DC[0,0]) -> 64-bit hash as 16-char hex string.
    """
    resized = image.convert("RGB").resize((32, 32), Image.LANCZOS)

    # BT.601 luminance, normalized to [0, 1] — matches Kotlin LUMINANCE_BT601 constants
    pixels: list[float] = []
    for y in range(32):
        for x in range(32):
            r, g, b = resized.getpixel((x, y))
            pixels.append((0.299 * r + 0.587 * g + 0.114 * b) / 255.0)

    # Normalized 8x8 DCT-II over the 32x32 block — same formula as app
    dct = [[0.0] * 8 for _ in range(8)]
    for u in range(8):
        for v in range(8):
            s = 0.0
            for x in range(32):
                for y in range(32):
                    s += (pixels[y * 32 + x]
                          * math.cos((2 * x + 1) * u * _PI_OVER_64)
                          * math.cos((2 * y + 1) * v * _PI_OVER_64))
            cu = _INV_SQRT2 if u == 0 else 1.0
            cv = _INV_SQRT2 if v == 0 else 1.0
            dct[u][v] = s * cu * cv * 0.25

    # Mean of 63 AC coefficients (exclude [0,0]) — same exclusion as app
    ac_sum = sum(dct[u][v] for u in range(8) for v in range(8) if u != 0 or v != 0)
    mean = ac_sum / 63.0

    # 64-bit hash — same bit ordering as app
    hash_val = 0
    bit = 0
    for u in range(8):
        for v in range(8):
            if dct[u][v] > mean:
                hash_val |= 1 << bit
            bit += 1

    return format(hash_val, "016x")


def _hamming(hex_a: str, hex_b: str) -> int:
    return bin(int(hex_a, 16) ^ int(hex_b, 16)).count("1")


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

        return _compute_phash_compat(cropped)
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
            dist = _hamming(hashes[a], hashes[b])
            print(f"  {a} vs {b}: Hamming distance = {dist} ({'DISTINCT ✓' if dist > 10 else 'COLLISION WARNING'})")


if __name__ == "__main__":
    main()
