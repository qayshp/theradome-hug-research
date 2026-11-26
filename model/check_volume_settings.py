"""Run the assembled volume settings overlay against modeled PIC18 peripherals."""
import argparse
from itertools import product
from pathlib import Path

from tools.build_volume_settings import APPLICATION_START, EXTENSION_START, HOOKS, archive_layout
from tools.rebuild import ORIGINAL, load_hex
from voice_check import Runner, Voice


PREFERENCE = 0xC0
CODES = (0xC3, 0xC6, 0xC9, 0xCC)
GAINS = (0, 1, 3, 14)
REPORT_TICKS = (0, 1, 29, 30, 148, 149, 150, 298, 299, 300, 448, 449, 450, 599, 600, 65535)
STOCK_AUTOMATIC_TICKS = {149, 299, 449}


class Trace(Runner):
    """Model EEPROM at the archived helper boundaries and capture speech requests."""

    def __init__(self, image, eeprom, mode=7, index=0, gain=0):
        super().__init__(image, Voice(index, gain, mode, 123))
        self.eeprom = eeprom
        self.writes = []
        self.reads = []
        self.speech = []
        self.eeprom_busy_reads = 0
        self.ram[0xFF2] = 0x80
        self.ram[0x2A0] = gain  # set by the subsystem check before the automatic call

    def read(self, address):
        if address == 0xF7F and self.eeprom_busy_reads:
            self.eeprom_busy_reads -= 1
            if not self.eeprom_busy_reads:
                self.ram[address] &= ~0x02
        return super().read(address)

    def peripheral(self, pc):
        if pc == 0xD3F8 and self.pair(0x66) == PREFERENCE // 2:
            self.reads.append(PREFERENCE)
            low = self.eeprom.get(PREFERENCE, 0xFF)
            high = self.eeprom.get(PREFERENCE + 1, 0xFF)
            self.putpair(0x66, low | high << 8)
            self.w, self.bank = high, 2
            return True
        if pc == 0xD6CE and self.pair(0xC0) == 0x1F4:
            self.write_word(0x3E8, self.pair(0xC2))
            self.w, self.bank = self.ram[0xC3], 0
            self.ram[0xFF2] |= 0x80  # original startup revision writer
            return True
        if pc == 0xD80A:
            assert self.pair(0xF62) == PREFERENCE
            assert self.ram[0xF7F] & 0xC6 == 0x04
            assert self.ram[0xFF2] & 0x80 == 0
            value = self.ram[0xF61]
            self.eeprom[PREFERENCE] = value
            self.writes.append((PREFERENCE, value))
            self.ram[0xF7F] |= 0x02
            self.eeprom_busy_reads = 2
            self.w = 0xAA
            return True
        if pc == 0xC8DE:  # idle-hold peripheral boundary
            return True
        if pc == 0xA4C0:
            self.speech.append(("number", self.pair(0xC8), self.ram[0x2B9]))
        if pc == 0x94F2:
            self.speech.append(("clip", self.pair(0x81), self.ram[0x2B9]))
        return super().peripheral(pc)

    def write_word(self, start, word):
        for offset, value in enumerate(word.to_bytes(2, "little")):
            self.eeprom[start + offset] = value
            self.writes.append((start + offset, value))

    def restart(self):
        # Stock startup has cleared selection/gain and prepared this write.
        self.putpair(0xC0, 0x1F4)
        self.putpair(0xC2, 126)
        self.run(0x5358, 0x535C)

    def press(self, mode, event):
        self.ram[0xF3], self.w = event, mode
        self.run(0x7082)


def linear(image):
    program = bytearray(65536)
    for address, value in image.items():
        program[address] = value
    return program


def check_restart(candidate):
    for code in range(256):
        eeprom = {PREFERENCE: code, PREFERENCE + 1: 0x57}
        pic = Trace(candidate, eeprom)
        pic.restart()
        index = CODES.index(code) if code in CODES else 0
        assert (pic.ram[0x44], pic.ram[0x2B9]) == (index, GAINS[index]), code
        assert pic.reads == [PREFERENCE]
        assert pic.writes == [(0x3E8, 126), (0x3E9, 0)]
        assert (eeprom[PREFERENCE], eeprom[PREFERENCE + 1]) == (code, 0x57)
    return 256


def check_cycle(candidate):
    eeprom = {PREFERENCE + 1: 0x57}
    count = 0
    for mode in (0, 4, 5, 7, 8, 9, 0x12, 0x19):
        for step in range(8):
            pic = Trace(candidate, eeprom, mode=mode)
            pic.restart()
            assert pic.ram[0x44] == step % 4
            pic.writes.clear()
            pic.press(mode, 5)
            index = (step + 1) % 4
            assert (pic.ram[0x44], pic.ram[0x2B9]) == (index, GAINS[index])
            assert pic.speech == [("clip", 0x31, GAINS[index])]
            assert pic.writes == [(PREFERENCE, CODES[index])]
            assert pic.ram[0xFF2] & 0x80
            assert not pic.ram[0xF7F] & 0x06
            count += 1
    assert eeprom[PREFERENCE + 1] == 0x57
    return count


def check_announcements(original, candidate):
    count = 0
    for mode, index, gain, ticks in product((7, 8), range(4), GAINS, REPORT_TICKS):
        for automatic in (False, True):
            if automatic and mode != 7:
                continue
            stock = Trace(original, {}, mode=mode, index=index, gain=gain)
            patched = Trace(candidate, {}, mode=mode, index=index, gain=gain)
            for pic in (stock, patched):
                pic.putpair(0x26F, ticks)
                if automatic:
                    pic.run(0x5E24, 0x5E2A)
                else:
                    pic.press(mode, 2)
            if not automatic or ticks in STOCK_AUTOMATIC_TICKS:
                number = ticks // 30 + 1
                stock_gain = gain if automatic else 0
                assert stock.speech == [("number", number, stock_gain), ("clip", 0x2D, stock_gain)]
                assert patched.speech == [("number", number, GAINS[index]), ("clip", 0x2D, GAINS[index])]
            else:
                assert stock.speech == patched.speech == []
            assert patched.ram == stock.ram, (mode, index, gain, ticks, automatic)
            assert not patched.reads and not patched.writes
            count += 1
    return count


def check_holds(original, candidate):
    count = 0
    for mode, index, gain in product((5, 7, 8), range(4), GAINS):
        stock = Trace(original, {}, mode=mode, index=index, gain=gain)
        patched = Trace(candidate, {}, mode=mode, index=index, gain=gain)
        stock.press(mode, 3)
        patched.press(mode, 3)
        assert patched.ram[0x44] == index
        if mode == 5:
            assert stock.ram[0x44] == 0
            stock.ram[0x44] = index
        assert patched.ram == stock.ram and patched.speech == stock.speech
        assert patched.slot7 == stock.slot7
        assert not patched.reads and not patched.writes
        count += 1
    return count


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("image", type=Path)
    args = parser.parse_args()
    original, candidate = load_hex(ORIGINAL), load_hex(args.image)
    assert candidate.keys() == set(range(APPLICATION_START, max(candidate) + 1))
    hooks = {a for start, size in HOOKS for a in range(start, start + size)}
    changed = {a for a in original if original[a] != candidate[a]}
    assert changed <= hooks
    assert all(changed.intersection(range(start, start + size)) for start, size in HOOKS)
    assert max(candidate) > EXTENSION_START
    assert archive_layout(candidate) == args.image.read_bytes()
    before, after = linear(original), linear(candidate)
    checks = [
        ("restart value", check_restart(after)),
        ("double press and restart", check_cycle(after)),
        ("countdown and timing", check_announcements(before, after)),
        ("long press", check_holds(before, after)),
    ]
    for name, count in checks:
        print(f"{name}: {count} passed")
    print(f"{sum(count for _, count in checks)} instruction-level cases passed")
    print(f"{len(changed)} changed stock bytes across {len(HOOKS)} hooks")


if __name__ == "__main__":
    main()
