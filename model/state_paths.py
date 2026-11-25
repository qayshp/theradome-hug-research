"""Inspect the mode dispatchers and indicator requests in the archived HUG image.

Implements the instructions used by the dispatch, indicator and session probes.
Unknown instructions stop execution. Addresses are byte addresses.
"""
import argparse
import hashlib
import struct
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path

HEX = Path(__file__).resolve().parents[1] / "outputs/myTheradome-LH80-labeled.hex"
CHANNEL = ("off", "red", "green", "both", "both")
PATTERN = {0: "off", 8: "level-dependent", 9: "steady", 10: "toggle",
           11: "pulse", 13: "fade", 14: "alternate"}


def read_image():
    data = {}
    records = HEX.read_bytes()
    for line in records.splitlines():
        row = bytes.fromhex(line[1:].decode())
        if line[:1] != b":" or len(row) != row[0] + 5 or sum(row) % 256:
            raise ValueError("Invalid Intel HEX record")
        if row[3] == 1:
            break
        if row[3] != 0:
            raise ValueError("Unsupported record type")
        address = int.from_bytes(row[1:3], "big")
        for offset, value in enumerate(row[4:-1]):
            if address + offset in data:
                raise ValueError("Overlapping records")
            data[address + offset] = value
    if hashlib.sha256(records).hexdigest() != "40caa7fd679e70205a8acc923f5586484884060e9ef4eb0cfaa4c4d23f120c69":
        raise ValueError("This model uses addresses from the archived HUG image")
    return data


def signed(number, bits):
    sign = 1 << (bits - 1)
    return (number ^ sign) - sign


def run(image, pc, ram, w, stop, bank=0, calls=None):
    zero = False
    carry = False
    stack = []

    def skip(address):
        high = image[address + 1]
        return address + (4 if high >> 4 == 0xC or high in (0xEC, 0xED, 0xEF, 0xEE) else 2)

    for _ in range(250):
        if stop(pc):
            return pc, w
        op = image[pc] | image[pc + 1] << 8
        start = pc
        pc += 2
        byte = op & 255
        addr = (bank << 8 | byte) if op & 256 else (byte if byte < 0x60 else 0xF00 | byte)
        # Bit tests can read these flags directly, as well as branch on them.
        ram[0xFD8] = (ram[0xFD8] & ~5) | int(carry) | (int(zero) << 2)
        if op & 0xFFF0 == 0x0100:  # MOVLB
            bank = op & 15
        elif op & 0xFF00 == 0x0E00:
            w = byte
        elif op & 0xFE00 == 0x5000:  # MOVF f,W
            w = ram[addr]
            zero = w == 0
        elif op & 0xFE00 == 0x6E00:
            ram[addr] = w
        elif op & 0xFE00 == 0x6A00:
            ram[addr] = 0
            zero = True
        elif op & 0xFE00 == 0x6000:  # CPFSLT
            if ram[addr] < w:
                pc = skip(pc)
        elif op & 0xFE00 == 0x6600:  # TSTFSZ
            if ram[addr] == 0:
                pc = skip(pc)
        elif op & 0xFE00 == 0x0400:  # DECF f,W
            carry = ram[addr] != 0
            w = (ram[addr] - 1) & 255
            zero = w == 0
        elif op & 0xFE00 == 0x2A00:  # INCF f,F
            carry = ram[addr] == 255
            ram[addr] = (ram[addr] + 1) & 255
            zero = ram[addr] == 0
        elif op >> 12 == 8:
            ram[addr] |= 1 << ((op >> 9) & 7)
        elif op & 0xFF00 == 0x0A00:
            w ^= byte
            zero = w == 0
        elif op & 0xF000 in (0xA000, 0xB000):
            bit = bool(ram[addr] & 1 << ((op >> 9) & 7))
            if bit == (op & 0xF000 == 0xA000):
                pc = skip(pc)
        elif op & 0xFF00 in (0xE000, 0xE100):
            if zero == (op & 0xFF00 == 0xE000):
                pc += 2 * signed(byte, 8)
        elif op & 0xF800 == 0xD000:
            pc += 2 * signed(op & 0x7FF, 11)
        elif op >> 8 in (0xE2, 0xE3):  # BC/BNC
            if carry == (op >> 8 == 0xE2):
                pc += 2 * signed(byte, 8)
        elif op & 0xF800 == 0xD800:  # RCALL
            stack.append(pc)
            pc += 2 * signed(op & 0x7FF, 11)
        elif op & 0xFF00 in (0xEF00, 0xEC00):
            target = 2 * (byte | ((image[pc] | (image[pc + 1] & 15) << 8) << 8))
            if op & 0xFF00 == 0xEC00:
                if calls is not None and target in calls:
                    # Probes supply return values only for explicitly listed callees.
                    w, bank, carry = calls[target](w, ram)
                    pc += 2
                    continue
                stack.append(pc + 2)
            pc = target
        elif op & 0xFF00 == 0x0C00 and stack:
            w = byte
            pc = stack.pop()
        else:
            raise ValueError(f"Unhandled instruction {op:04X} at {start:04X}")
    raise ValueError("Probe exceeded its instruction limit")


def dispatch(image, mode, location, first, last, register):
    ram = defaultdict(int, {register: mode})
    return run(image, location, ram, 0, lambda pc: pc < first or pc > last)[0]


@dataclass(frozen=True)
class Indicator:
    selector: int
    pattern: int
    red: float
    green: float

    def describe(self):
        if self.selector == 0 and self.pattern == 0:
            return "off"
        return f"{CHANNEL[self.selector]} {PATTERN[self.pattern]} (R={self.red:g}, G={self.green:g})"


def indicator(image, mode, flags=0):
    # Entry at the dispatch after MOVFF WREG,C8. Mode 25 is decimal here.
    ram = defaultdict(int, {0xC8: mode, 0x27B: flags})
    _, selector = run(image, 0xA0F0, ram, 0, lambda pc: pc == 0x7540)
    # 24-bit floating point has its lowest mantissa byte omitted.
    def level(start):
        return struct.unpack("<f", bytes([0, ram[start], ram[start+1], ram[start+2]]))[0]
    return Indicator(selector, ram[0xC0], level(0xC1), level(0xC4))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mode", type=lambda x: int(x, 0), choices=range(256), metavar="BYTE")
    parser.add_argument("--channel", choices=CHANNEL[:4])
    parser.add_argument("--pattern", choices=PATTERN.values())
    parser.add_argument("--all-bytes", action="store_true",
                        help="include undefined byte values that route to fallback handlers")
    args = parser.parse_args()
    image = read_image()
    print("Mode   Periodic  On entry  On button   Indicator requests (union over flag bits 0,2,3)")
    modes = (args.mode,) if args.mode is not None else range(256 if args.all_bytes else 32)
    for mode in modes:
        profiles = {indicator(image, mode, flags) for flags in range(16)}
        matches = [v for v in profiles if (args.channel is None or CHANNEL[v.selector] == args.channel)
                   and (args.pattern is None or PATTERN[v.pattern] == args.pattern)]
        if matches:
            periodic = dispatch(image, mode, 0x5FD4, 0x5FD4, 0x6052, 0x2B5)
            entry = dispatch(image, mode, 0x6488, 0x6488, 0x6502, 0x2B5)
            button = dispatch(image, mode, 0x7478, 0x7478, 0x74DA, 0xEA)
            print(f"0x{mode:02X}   {periodic:04X}      {entry:04X}      {button:04X}        " +
                  " / ".join(sorted(v.describe() for v in matches)))


if __name__ == "__main__":
    main()
