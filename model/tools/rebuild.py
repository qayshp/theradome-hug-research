"""Generate a labelled PIC18 assembly baseline or compare two Intel HEX images."""

import argparse
import hashlib
from pathlib import Path
import re
import subprocess


ROOT = Path(__file__).resolve().parents[2]
ORIGINAL = ROOT / "outputs/myTheradome-LH80-labeled.hex"
SOURCE = ROOT / "model/rebuild.asm"
VECTORS = {0x1900: "app_reset", 0x1908: "app_high_interrupt", 0x1918: "app_low_interrupt"}
SYMBOLS = {
    **VECTORS,
    0xD3F8: "read_eeprom_word",
    0xD6CE: "write_eeprom_word",
    0xD80A: "start_memory_write",
    0xDECE: "format_hex_digit",
}
CONDITIONAL = set("bc bn bnc bnn bnov bnz bov bz".split())
SKIPS = set("btfsc btfss cpfseq cpfsgt cpfslt dcfsnz decfsz incfsz infsnz tstfsz".split())
TARGETS = CONDITIONAL | {"bra", "call", "goto", "rcall"}
STOPS = {"return", "retlw", "retfie", "reset", "dw"}


def load_hex(path):
    image = {}
    base = 0
    ended = False
    for line in path.read_text().splitlines():
        if not line.startswith(":") or ended:
            raise ValueError(f"Invalid record in {path}")
        record = bytes.fromhex(line[1:])
        count = record[0]
        if len(record) != count + 5 or sum(record) & 255:
            raise ValueError(f"Invalid record sizing or checksum in {path}")
        address = int.from_bytes(record[1:3], "big")
        kind = record[3]
        payload = record[4:-1]
        if kind == 0:
            for offset, value in enumerate(payload):
                key = base + address + offset
                if key in image:
                    raise ValueError(f"Overlapping record at {key:#x}")
                image[key] = value
        elif kind == 1 and address == 0 and count == 0:
            ended = True
        elif kind in (2, 4) and address == 0 and count == 2:
            base = int.from_bytes(payload, "big") << (4 if kind == 2 else 16)
        else:
            raise ValueError(f"Unsupported record type or header in {path}")
    if not ended or not image:
        raise ValueError(f"Missing program data or end record in {path}")
    return image


def contiguous_bytes(image):
    first, last = min(image), max(image)
    if len(image) != last - first + 1:
        raise ValueError("The addressed image contains gaps and cannot be exported as a contiguous payload")
    return bytes(image[address] for address in range(first, last + 1))


def compare(reference, output, binary):
    original, candidate = load_hex(reference), load_hex(output)
    changed = sorted(a for a in original.keys() | candidate.keys() if original.get(a) != candidate.get(a))
    if changed:
        print(f"{len(changed)} missing, extra or changed addresses. First: {changed[:8]}")
        return 1
    ordered = contiguous_bytes(candidate)
    if binary is not None and binary.read_bytes() != ordered:
        print(f"Raw application bytes differ from {binary}")
        return 1
    print(f"All {len(ordered)} addresses and bytes match. Payload SHA-256 {hashlib.sha256(ordered).hexdigest()}")
    return 0


def generate(disassembler, reference, output):
    # Chip selection fixes the standard instruction vocabulary. Keep hardware
    # registers numeric so assigning a peripheral name requires separate evidence.
    run = subprocess.run([disassembler, "--strict", "-p18f66k22", str(reference)],
                         check=True, capture_output=True, text=True)
    words, decoded = {}, {}
    for line in run.stdout.splitlines():
        match = re.match(r"^([0-9a-f]+):\s+([0-9a-f]{4})(?:\s+(\S+)(?:\s+(.*))?)?$", line)
        if match:
            address, word, mnemonic, operands = match.groups()
            pc = int(address, 16)
            words[pc] = int(word, 16)
            if mnemonic:
                decoded[pc] = (mnemonic, (operands or "").split(";")[0].strip())
    original = load_hex(reference)
    if {p+i: (w >> (8*i)) & 255 for p, w in words.items() for i in (0, 1)} != original:
        raise ValueError("Disassembler listing and input HEX disagree")

    def size(pc):
        return 4 if pc+2 in words and pc+2 not in decoded else 2

    live, labels, pending = set(), set(VECTORS), list(VECTORS)
    while pending:
        pc = pending.pop()
        if pc in live or pc not in decoded:
            continue
        op, arg = decoded[pc]
        if op == "dw":
            continue
        live.add(pc)
        after = pc + size(pc)
        if op in TARGETS:
            target = int(arg.split(",")[0], 0)
            if target in words:
                labels.add(target)
                pending.append(target)
        if op in SKIPS:
            pending.append(after + size(after))
        if op not in STOPS | {"bra", "goto"}:
            # Direct writes to PCL transfer control to computed destinations.
            if (op == "movwf" and arg == "0xf9, 0x0") or (op == "movff" and arg.endswith(", 0xff9")):
                continue
            pending.append(after)

    def label(pc):
        return SYMBOLS.get(pc, f"loc_{pc:04X}")

    lines = ["; HUG application image. Byte addresses in comments refer to the original image.",
             "; Standard PIC18 ISA. Processor directive selects an assembler profile.",
             "; Peripheral addresses are numeric. Unresolved words are preserved verbatim.",
             "        processor 18f66k22", "        radix hex"]
    consumed = set()
    for pc in sorted(words):
        if pc in consumed:
            continue
        if pc-2 not in words:
            lines.append(f"        org 0x{pc:04x}")
        if pc in labels:
            lines.append(label(pc) + ":")
        if pc in live:
            op, args = decoded[pc]
            if op in TARGETS:
                dest, sep, rest = args.partition(",")
                if int(dest, 0) in labels:
                    args = label(int(dest, 0)) + sep + rest
            lines.append(f"        {op:<8}{args:<27} ; {pc:04X}")
            if size(pc) == 4:
                if pc+2 in labels:
                    raise ValueError(f"Unexpected branch into operand at {pc+2:#x}")
                consumed.add(pc+2)
        else:
            lines.append(f"        dw      0x{words[pc]:04x}                      ; {pc:04X}")
    lines.append("        end")
    output.write_text("\n".join(lines) + "\n")
    print(f"{len(live)} instruction mnemonics, {2*(len(live)+len(consumed))} bytes. "
          f"{len(words)-len(live)-len(consumed)} remaining literal words.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(required=True, dest="command")
    generator = commands.add_parser("generate")
    generator.add_argument("--gpdasm", default="gpdasm")
    generator.add_argument("--output", type=Path, default=SOURCE)
    verifier = commands.add_parser("verify")
    verifier.add_argument("image", type=Path)
    verifier.add_argument("--binary", type=Path, help="also compare the raw bytes in address order")
    extractor = commands.add_parser("extract", help="write the raw contiguous application bytes")
    extractor.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.command == "generate":
        generate(args.gpdasm, ORIGINAL, args.output)
        return 0
    if args.command == "extract":
        args.output.write_bytes(contiguous_bytes(load_hex(ORIGINAL)))
        return 0
    return compare(ORIGINAL, args.image, args.binary)


if __name__ == "__main__":
    raise SystemExit(main())
