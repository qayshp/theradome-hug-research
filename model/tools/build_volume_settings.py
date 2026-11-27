"""Assemble the volume overlay and merge it with the verified stock HUG image."""
import argparse
import hashlib
from pathlib import Path
import subprocess
import tempfile

from .rebuild import ORIGINAL, ROOT, load_hex


SOURCE = ROOT / "model/volume-settings.asm"
STOCK_SHA256 = "40caa7fd679e70205a8acc923f5586484884060e9ef4eb0cfaa4c4d23f120c69"
APPLICATION_START = 0x1900
EXTENSION_START = 0xDF80
# Exclusive end of the offline candidate, including its additional flash row.
APPLICATION_END = 0xE080
RECORD_SIZE = 64
HOOKS = (
    (0x5358, 4),
    (0x5D86, 4), (0x5D9A, 4),
    (0x5DBE, 4), (0x5DCA, 4), (0x5DE2, 4),
    (0x5E26, 4), (0x5E96, 4), (0x5EA2, 4), (0x5EB4, 4),
    (0x5F24, 4), (0x5F2A, 4), (0x5F46, 4), (0x5F52, 4),
    (0x6306, 4), (0x72AC, 4), (0x7330, 2),
    (0x80C4, 4), (0x80CE, 4), (0x8122, 4), (0x81C4, 4), (0x81DE, 4),
    (0x8240, 4), (0x8274, 4), (0x8316, 4), (0x838C, 4),
    (0xCAC8, 4),
)


def encode_record(address, kind, payload=b""):
    body = bytes([len(payload)]) + address.to_bytes(2, "big") + bytes([kind]) + payload
    return ":" + (body + bytes([-sum(body) % 256])).hex().upper()


def merge(original, overlay):
    if original.keys() != set(range(APPLICATION_START, EXTENSION_START)):
        raise ValueError("unexpected stock application address map")
    hooks = {a for start, size in HOOKS for a in range(start, start + size)}
    extension = {a for a in overlay if a >= EXTENSION_START}
    if not extension or overlay.keys() != hooks | extension:
        raise ValueError("overlay must cover the declared hooks and its own extension")
    end = max(extension) + 1
    if end % 2 or end > APPLICATION_END or extension != set(range(EXTENSION_START, end)):
        raise ValueError("extension must be contiguous, word-aligned and within the declared application rows")
    return original | overlay


def archive_layout(image):
    end = (max(image) // RECORD_SIZE + 1) * RECORD_SIZE
    records = []
    for address in range(APPLICATION_START, end, RECORD_SIZE):
        row = bytes(image.get(i, 0xFF) for i in range(address, address + RECORD_SIZE))
        records.append(encode_record(address, 0, row))
    records.append(encode_record(0, 1))
    return ("\r\n".join(records) + "\r\n").encode("ascii")


def build(gpasm, output):
    stock_bytes = ORIGINAL.read_bytes()
    if hashlib.sha256(stock_bytes).hexdigest() != STOCK_SHA256:
        raise ValueError("the firmware does not match the archived HUG base")
    stock = load_hex(ORIGINAL)
    with tempfile.TemporaryDirectory(prefix="theradome-volume-build-") as temp:
        overlay_path = Path(temp) / "overlay.hex"
        subprocess.run([gpasm, "-o", str(overlay_path), str(SOURCE)], check=True, cwd=temp)
        image = merge(stock, load_hex(overlay_path))
    if archive_layout(stock) != stock_bytes:
        raise ValueError("the encoder failed to reproduce the stock image")
    payload = archive_layout(image)
    with output.open("xb") as stream:
        stream.write(payload)
    print(f"Wrote {output} ({len(payload):,} bytes)")
    print(f"Application range 0x{min(image):04X} through 0x{max(image):04X} before row padding")
    print(f"HEX SHA-256 {hashlib.sha256(payload).hexdigest()}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path, help="new experimental Intel HEX, for host testing")
    parser.add_argument("--gpasm", default="gpasm", help="path to the gputils assembler")
    args = parser.parse_args()
    build(args.gpasm, args.output)


if __name__ == "__main__":
    main()
