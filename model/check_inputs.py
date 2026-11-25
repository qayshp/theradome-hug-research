"""Compare the compiled input model with selected PIC18 paths in the HUG HEX.

Usage: python3 check_inputs.py /path/to/inputs.so [path/to/firmware.hex]
"""

import ctypes as ct
import hashlib
import itertools
from pathlib import Path
import sys


def read_image(path):
    raw = path.read_bytes()
    digest = hashlib.sha256(raw).hexdigest()
    if digest != "40caa7fd679e70205a8acc923f5586484884060e9ef4eb0cfaa4c4d23f120c69":
        raise ValueError(f"Unexpected HEX digest {digest}")
    data = bytearray(65536)
    for line in raw.splitlines():
        record = bytes.fromhex(line[1:].decode("ascii"))
        if len(record) != 5 + record[0] or sum(record) & 255:
            raise ValueError("Invalid Intel HEX record")
        if record[3] == 0:
            start = int.from_bytes(record[1:3], "big")
            data[start:start + record[0]] = record[4:-1]
        elif record[3] != 1:
            raise ValueError("Unexpected record type")
    return data


class Pic:
    """Only the executed instructions and register side effects used by this comparison."""
    def __init__(self, image):
        self.image = image
        self.mem = bytearray(4096)
        self.bank = 0
        self.w = 0
        self.zero = False

    def run(self, pc, stops):
        for _ in range(150):
            if pc in stops:
                return pc
            addr = pc
            word = self.image[pc] | self.image[pc + 1] << 8
            pc += 2
            f = word & 255
            if word & 0x100:
                f |= self.bank << 8
            elif f >= 0x60:
                f |= 0xF00
            bit = 1 << ((word >> 9) & 7)
            op = word >> 12
            if op == 0xA or op == 0xB:  # BTFSS/BTFSC, all skipped instructions here are one word
                if bool(self.mem[f] & bit) == (op == 0xA):
                    pc += 2
            elif op == 0x8:
                self.mem[f] |= bit
            elif op == 0x9:
                self.mem[f] &= ~bit
            elif word & 0xF800 == 0xD000:
                offset = ((word & 0x7FF) ^ 0x400) - 0x400
                pc += 2 * offset
            elif word >> 8 in (0xE0, 0xE1):  # BZ/BNZ
                if self.zero == (word >> 8 == 0xE0):
                    pc += 2 * (((word & 255) ^ 128) - 128)
            elif word >> 8 == 0x0E:
                self.w = word & 255
            elif word >> 8 == 0x0A:
                self.w ^= word & 255
                self.zero = self.w == 0
            elif word & 0xFFF0 == 0x0100:
                self.bank = word & 15
            elif word & 0xFE00 == 0x6E00:
                self.mem[f] = self.w
            elif word & 0xFE00 == 0x6A00:
                self.mem[f] = 0
                self.zero = True
            elif word & 0xFE00 == 0x6600:
                if self.mem[f] == 0:
                    pc += 2
            elif word & 0xFE00 == 0x5000:  # MOVF f,W
                self.w = self.mem[f]
                self.zero = self.w == 0
            elif word & 0xFE00 == 0x0400:  # DECF f,W
                self.w = (self.mem[f] - 1) & 255
                self.zero = self.w == 0
            elif op == 0xC:  # MOVFF, only WREG source used here
                src = word & 0xFFF
                dest = self.image[pc] | (self.image[pc + 1] & 15) << 8
                self.mem[dest] = self.w if src == 0xFE8 else self.mem[src]
                pc += 2
            elif word == 0x0012:
                return addr
            else:
                raise ValueError(f"Unexpected word {word:04X} at {addr:04X}")
        raise RuntimeError("Instruction budget exhausted")

    def button_step(self, signal):
        if signal == 0:
            self.mem[0xFF0] |= 0x02
        elif signal == 1:
            self.mem[0xFF0] |= 0x04
        elif self.mem[0xFB1] & 1:
            self.mem[0xFA1] |= 0x02
        self.run(0x847A, {0x8560, 0x8768})

    def button_view(self):
        timer_active = (self.mem[0xFB1] & 1) and (self.mem[0xFA0] & 2)
        interval = 65536 - (self.mem[0xFB3] << 8 | self.mem[0xFB2]) if timer_active else 0
        return (self.mem[0xF3], bool(self.mem[0x27B] & 128), bool(self.mem[0x4F] & 2),
                bool(self.mem[0xFF0] & 16), bool(self.mem[0xFF0] & 32), interval)


class ButtonState(ct.Structure):
    _fields_ = [("event", ct.c_uint8), ("started", ct.c_bool), ("saw_rising_edge", ct.c_bool),
                ("int2_enabled", ct.c_bool), ("int3_enabled", ct.c_bool),
                ("interval_ticks", ct.c_uint16)]

    def view(self):
        return tuple(getattr(self, field) for field, _ in self._fields_)


def verify(image, library):
    library.button_enable.argtypes = [ct.POINTER(ButtonState), ct.c_bool]
    library.button_input.argtypes = [ct.POINTER(ButtonState), ct.c_int]
    library.charge_text.argtypes = [ct.c_bool, ct.c_bool, ct.c_uint8]
    library.charge_next_state.argtypes = [ct.c_bool, ct.c_bool, ct.c_uint8]
    charge_labels = ["Error", "Test Mode", "Charging", "Charged"]
    for mode, a, b in itertools.product(range(256), range(2), range(2)):
        pic = Pic(image)
        pic.bank = 2  # The preceding 0xDC1E call returns with BSR=2.
        pic.mem[0x2AD], pic.mem[0xF80], pic.mem[0xF81] = mode, a << 4, b << 4
        pic.run(0x4C6E, {0x4C84, 0x4CAC, 0x4CCC, 0x4CE0})
        ptr = pic.mem[0x7E] | pic.mem[0x7F] << 8
        original = image[ptr:image.index(0, ptr)].decode("ascii")
        modeled = charge_labels[library.charge_text(a, b, mode)]
        assert original == modeled, (mode, a, b, original, modeled)

    # End at the result assignment/return in each block. Stub the peripheral
    # and EEPROM calls by stepping past CALL. Their side effects fall outside
    # the state-number comparison. 0xC394 reloads the current state from RAM.
    calls = {0xC38A, 0xC390, 0xC39C, 0xC3AA, 0xC3B2, 0xC3BA, 0xC3EC, 0xC3F6}
    for mode, a, b in itertools.product(range(256), range(2), range(2)):
        pic = Pic(image)
        pic.bank = 2
        pic.mem[0x2B5], pic.mem[0xF80], pic.mem[0xF81] = mode, a << 4, b << 4
        pc = 0xC370
        while True:
            pc = pic.run(pc, calls | {0xC3C0})
            if pc not in calls:
                break
            assert image[pc + 1] == 0xEC, hex(pc)
            pc += 4
        if pc == 0xC3C0:
            assert image[pc:pc + 4] == bytes.fromhex("80 EF 0C F0")
            result = -1
        else:
            assert pc == 0xC402
            result = pic.w
        assert result == library.charge_next_state(a, b, mode), (mode, a, b, result)

    # Explore interleavings, including repeated edges, disabled inputs and rearming.
    # Each trace starts disabled and quiescent. Values 3 and 4 are enable/disable calls.
    for signals in itertools.product(range(5), repeat=6):
        pic, state = Pic(image), ButtonState()
        for signal in signals:
            if signal >= 3:
                pic.w = int(signal == 3)
                pic.run(0xD9F8, set())
                library.button_enable(ct.byref(state), signal == 3)
            else:
                pic.button_step(signal)
                library.button_input(ct.byref(state), signal)
            assert pic.button_view() == state.view(), (signals, signal, pic.button_view(), state.view())
    print("Matched 1,024 diagnostic cases, 1,024 charge transitions and 15,625 six-step button traces against the HEX.")


if __name__ == "__main__":
    default = Path(__file__).resolve().parents[1] / "outputs/myTheradome-LH80-labeled.hex"
    verify(read_image(Path(sys.argv[2]) if len(sys.argv) > 2 else default),
           ct.CDLL(str(Path(sys.argv[1]).resolve())))
