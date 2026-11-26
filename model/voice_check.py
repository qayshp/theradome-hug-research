"""Compare voice.c to selected instructions in the archived image on the host.

python3 model/voice_check.py /absolute/path/to/voice.so
"""
import ctypes as ct
import itertools
from pathlib import Path
import sys

from check_inputs import read_image


class Voice(ct.Structure):
    _fields_ = [("index", ct.c_uint8), ("shift", ct.c_uint8),
                ("requested_state", ct.c_uint8), ("slot7", ct.c_uint16)]


class Request(ct.Structure):
    _fields_ = [("number", ct.c_uint16), ("clip", ct.c_uint8),
                ("shift_at_request", ct.c_uint8)]


def values(obj):
    return tuple(getattr(obj, name) for name, _ in obj._fields_)


class Runner:
    """PIC18 subset for the foreground event handlers, timer calculation and PWM.

    At the number, audio, and EEPROM peripheral boundaries, capture the real
    arguments. The foreground ignores the peripheral timing and return values
    on the traced routes. Hardware playback is abstracted as enabled.
    """
    def __init__(self, image, state):
        self.image = image
        self.ram = bytearray(4096)
        self.w = self.bank = self.status = 0
        self.ram[0x44], self.ram[0x2B9], self.ram[0x2B4], self.slot7 = values(state)
        self.number = self.clip = self.at_request = 0

    def word(self, pc):
        return int.from_bytes(self.image[pc:pc+2], "little")

    def read(self, f):
        return self.status if f == 0xFD8 else self.w if f == 0xFE8 else self.ram[f]

    def store(self, f, value):
        if f == 0xFD8:
            self.status = value & 255
        elif f == 0xFE8:
            self.w = value & 255
        else:
            self.ram[f] = value & 255

    def z(self, result):
        self.status = (self.status & ~4) | ((result & 255 == 0) << 2)

    def carry(self, flag):
        self.status = (self.status & ~1) | bool(flag)

    def pair(self, f):
        return self.ram[f] + (self.ram[f+1] << 8)

    def putpair(self, f, value):
        self.ram[f:f+2] = value.to_bytes(2, "little")

    def peripheral(self, pc):
        if pc == 0xA4C0:
            self.number = self.pair(0xC8)
        elif pc == 0x94F2:
            self.clip, self.at_request = self.pair(0x81), self.ram[0x2B9]
            self.ram[0xF3] = 0  # playback calls D9F8 with 0
        elif pc == 0xD3F8:
            assert self.pair(0x66) == 7
            self.putpair(0x66, self.slot7)
            self.bank = 2
        elif pc == 0xD6CE:
            assert self.pair(0xC0) == 7
            self.slot7 = self.pair(0xC2)
            self.bank = 0
        elif pc != 0xD9F8:  # final rearm is outside the audio model
            return False
        return True

    def run(self, pc, stop=None):
        stack = []
        for _ in range(800):
            if pc == stop:
                return
            if self.peripheral(pc):
                if not stack:
                    return
                pc = stack.pop()
                continue
            addr, op = pc, self.word(pc)
            pc += 2
            bit = 1 << ((op >> 9) & 7)
            f = op & 255
            if op & 0x100:
                f += self.bank << 8
            elif f >= 0x60:
                f += 0xF00
            v = self.read(f)
            byte_op = op & 0xFC00
            result = None
            skip = False
            if op == 0x00:  # NOP
                continue
            if op == 0x12:
                if not stack:
                    return
                pc = stack.pop()
            elif op >> 8 in (0xEC, 0xEF):
                target = ((self.word(pc) & 4095) << 9) | ((op & 255) << 1)
                if op >> 8 == 0xEC:
                    stack.append(pc + 2)
                pc = target
            elif op & 0xF800 == 0xD000:
                pc += 2 * (((op & 2047) ^ 1024) - 1024)
            elif op & 0xF800 == 0xD800:
                stack.append(pc)
                pc += 2 * (((op & 2047) ^ 1024) - 1024)
            elif op >> 8 in (0xE0, 0xE1, 0xE2, 0xE3):
                flag = (self.status >> (2 if op >> 8 < 0xE2 else 0)) & 1
                if flag != (op >> 8 & 1):
                    pc += 2 * (((op & 255) ^ 128) - 128)
            elif op >> 12 == 0xC:
                self.store(self.word(pc) & 4095, self.read(op & 4095))
                pc += 2
            elif op >> 12 in (8, 9):
                self.store(f, (v | bit) if op >> 12 == 8 else (v & ~bit))
            elif op >> 12 in (10, 11):
                skip = bool(v & bit) == (op >> 12 == 10)
            elif op >> 8 == 1:
                self.bank = op & 15
            elif op >> 8 == 14:
                self.w = op & 255
            elif op >> 8 == 10:
                self.w ^= op & 255
                self.z(self.w)
            elif op >> 8 == 11:
                self.w &= op & 255
                self.z(self.w)
            elif op & 0xFE00 == 0x6E00:
                self.store(f, self.w)
            elif op & 0xFE00 == 0x6A00:
                self.store(f, 0)
                self.z(0)
            elif op & 0xFE00 == 0x6400:
                skip = v > self.w
            elif byte_op == 0x3800:
                swapped = ((v & 15) << 4) | (v >> 4)
                if op & 0x200:
                    self.store(f, swapped)
                else:
                    self.w = swapped
            elif byte_op in (0x2C00, 0x4800):
                result = (v + (1 if byte_op == 0x4800 else -1)) & 255
                skip = (result != 0) if byte_op == 0x4800 else (result == 0)
                self.store(f, result)
                result = None
            elif byte_op in (0x5000, 0x1800, 0x1000, 0x1400):
                result = {0x5000: v, 0x1800: v ^ self.w,
                          0x1000: v | self.w, 0x1400: v & self.w}[byte_op]
            elif byte_op in (0x0400, 0x2800, 0x2000):
                change = {0x0400: -1, 0x2800: 1, 0x2000: self.w + (self.status & 1)}[byte_op]
                result = v + change
                self.carry(result >= 0 if byte_op == 0x0400 else result > 255)
            elif byte_op in (0x5C00, 0x5800):
                result = v - self.w - (int(not (self.status & 1)) if byte_op == 0x5800 else 0)
                self.carry(result >= 0)
            elif byte_op in (0x3400, 0x3000):
                if byte_op == 0x3400:
                    result = (v << 1) | (self.status & 1)
                    self.carry(v & 128)
                else:
                    result = (v >> 1) | ((self.status & 1) << 7)
                    self.carry(v & 1)
            else:
                raise ValueError(f"Unknown opcode {op:04x} at {addr:04x}")
            if result is not None:
                self.z(result)
                if op & 0x200:
                    self.store(f, result)
                else:
                    self.w = result & 255
            if skip:
                second_word = self.word(pc) >> 12 == 12 or self.word(pc) >> 8 in (0xEC, 0xEF)
                pc += 4 if second_word else 2
        raise RuntimeError("Instruction budget exhausted")


def verify(library, firmware):
    library.voice_button.argtypes = [ct.POINTER(Voice), ct.c_uint8, ct.c_uint8,
                                     ct.c_uint16, ct.POINTER(Request)]
    library.voice_button.restype = ct.c_bool
    library.voice_automatic_report.argtypes = [ct.POINTER(Voice), ct.c_uint16]
    library.voice_automatic_report.restype = Request
    counts = []
    for event in (2, 3, 5, 0):
        count = 0
        ticks_set = (0, 1, 29, 30, 149, 150, 299, 300, 449, 450, 599, 600, 65535)
        for mode, index, shift, ticks, slot in itertools.product(
            [7, 8], range(4), [0, 1, 3, 14], ticks_set if event in (0, 2) else [450],
            [0, 36, 65534, 65535] if event == 3 else [123],
        ):
            state = Voice(index, shift, mode, slot)
            pic = Runner(firmware, state)
            pic.putpair(0x26F, ticks)
            if event == 0:
                request = library.voice_automatic_report(ct.byref(state), ticks)
                pic.run(0xCA60)
            else:
                request = Request()
                pic.w, pic.ram[0xF3] = mode, event
                pic.run(0x7082)
                assert library.voice_button(ct.byref(state), mode, event, ticks, ct.byref(request))
            result = (pic.ram[0x44], pic.ram[0x2B9], pic.ram[0x2B4], pic.slot7,
                      pic.number, pic.clip, pic.at_request)
            assert values(state) + values(request) == result, (mode, event, ticks, result, values(state), values(request))
            count += 1
        counts.append(count)
    print(f"Matched PIC and C for {sum(counts)} cases (short, held, double, automatic: {counts}).")
    library.voice_duty.argtypes = [ct.c_uint16, ct.c_uint8]
    library.voice_duty.restype = ct.c_uint16
    samples = [0, 1, 3, 4, 15, 16, 0x247, 0x80F, 0xFF0, 0xFFFF]
    for sample, shift in itertools.product(samples, [0, 1, 3, 14]):
        pic = Runner(firmware, Voice())
        pic.ram[0x83] = shift
        pic.putpair(0x66, sample)
        pic.run(0x95AA, 0x95BE)  # captured decoder output to timer wait
        pic.run(0x9672)          # PWM update after the timer signals
        expected = (pic.ram[0xF4A] << 2) | ((pic.ram[0xF49] >> 4) & 3)
        assert expected == library.voice_duty(sample, shift), (sample, shift, expected)
    print(f"Matched {len(samples) * 4} audio samples and shift settings against the PWM writer.")


if __name__ == "__main__":
    root = Path(__file__).resolve().parents[1]
    verify(ct.CDLL(str(Path(sys.argv[1]).resolve())),
           read_image(root / "outputs/myTheradome-LH80-labeled.hex"))
