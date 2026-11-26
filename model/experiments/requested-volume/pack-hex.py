"""Write an offline rebuild in the archived HUG Intel HEX record layout."""
import argparse
import hashlib
from pathlib import Path
import runpy


def encode_record(address, kind, data=b""):
    body = bytes([len(data)]) + address.to_bytes(2, "big") + bytes([kind]) + data
    return ":" + (body + bytes([-sum(body) % 256])).hex().upper()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("repo", type=Path, help="path to the research checkout")
    parser.add_argument("assembled", type=Path, help="gpasm output")
    parser.add_argument("output", type=Path, help="new output HEX")
    args = parser.parse_args()
    load_hex = runpy.run_path(str(args.repo / "model/tools/rebuild.py"))["load_hex"]
    stock = load_hex(args.repo / "outputs/myTheradome-LH80-labeled.hex")
    candidate = load_hex(args.assembled)
    if candidate.keys() != stock.keys() or len(stock) != 50816:
        parser.error("expected all archived application addresses at 0x1900–0xDF7F")
    start, end = min(stock), max(stock) + 1
    lines = [encode_record(a, 0, bytes(candidate[i] for i in range(a, a + 64)))
             for a in range(start, end, 64)]
    payload = ("\r\n".join(lines + [encode_record(0, 1)]) + "\r\n").encode("ascii")
    with args.output.open("xb") as target:
        target.write(payload)
    print(f"{len(payload)} bytes, SHA-256 {hashlib.sha256(payload).hexdigest()}")


if __name__ == "__main__":
    main()
