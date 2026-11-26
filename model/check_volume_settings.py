"""Run the assembled volume settings overlay against modeled PIC18 peripherals."""
import argparse
from itertools import product
from pathlib import Path

from tools.build_volume_settings import APPLICATION_END, APPLICATION_START, HOOKS, archive_layout
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
        self.ram[0x2A0] = gain  # set by the subsystem check before ordinary speech

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


class PromptTrace(Trace):
    """Capture speech arguments while running the stock counted-message logic."""

    def __init__(self, image, mode, index, gain, audio_enabled, completed=0):
        super().__init__(image, dict(enumerate(completed.to_bytes(2, "little"))),
                         mode=mode, index=index, gain=gain)
        self.ram[0x27C] = 4 if audio_enabled else 0
        self.player_registers = []

    def peripheral(self, pc):
        if pc == 0xD3F8 and self.pair(0x66) == 0:
            self.reads.append(0)
            low, high = self.eeprom[0], self.eeprom[1]
            self.putpair(0x66, low | high << 8)
            self.w, self.bank = high, 2
            return True
        if pc == 0xD6CE and self.pair(0xC0) == 0:
            self.write_word(0, self.pair(0xC2))
            self.w, self.bank = self.ram[0xC3], 0
            return True
        if pc != 0x94F2:
            return super().peripheral(pc)
        self.speech.append(("clip", self.pair(0x81), self.ram[0x2B9]))
        self.player_registers.append((self.w, self.bank, self.status))
        if not self.ram[0x27C] & 4:
            return False  # execute the player's disabled-audio return
        Runner.peripheral(self, pc)
        # Exercise preservation of the result registers at the playback boundary.
        self.w, self.bank, self.status = 0xA6, 1, 5
        return True


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


def check_prompt_result(stock, patched, sequence, gain, expected):
    assert stock.speech == [(kind, value, gain) for kind, value in sequence]
    assert patched.speech == [(kind, value, expected) for kind, value in sequence]
    assert patched.player_registers == stock.player_registers
    assert patched.ram == stock.ram
    assert (patched.w, patched.bank, patched.status) == (stock.w, stock.bank, stock.status)
    assert patched.reads == stock.reads and patched.writes == stock.writes
    assert patched.eeprom == stock.eeprom


def check_ready_prompts(original, candidate):
    # The three clips are split between the first two waiting-for-treatment passes.
    paths = ((0x5DB6, (0x2E, 0x27)), (0x5DDA, (0x20,)))
    count = 0
    for (address, clips), index, gain, enabled in product(paths, (0, 1, 2, 3, 4, 255), GAINS, (False, True)):
        stock = PromptTrace(original, 4, index, gain, enabled)
        patched = PromptTrace(candidate, 4, index, gain, enabled)
        for pic in (stock, patched):
            pic.run(address, 0x5DCE)
        expected = GAINS[index] if index < len(GAINS) else gain
        check_prompt_result(stock, patched, [("clip", clip) for clip in clips], gain, expected)
        assert not patched.reads and not patched.writes
        count += 1
    return count


def check_pause_prompts(original, candidate):
    paths = ((0x5EAC, 0x5D4C, (0x20,)), (0x5E8E, 0x5DAE, (0x27, 0x2A)))
    count = 0
    for (address, stop, clips), index, gain, enabled in product(
            paths, (0, 1, 2, 3, 4, 255), GAINS, (False, True)):
        stock = PromptTrace(original, 8, index, gain, enabled)
        patched = PromptTrace(candidate, 8, index, gain, enabled)
        for pic in (stock, patched):
            pic.bank = 2
            pic.run(address, stop)
        expected = GAINS[index] if index < len(GAINS) else gain
        check_prompt_result(stock, patched, [("clip", clip) for clip in clips], gain, expected)
        assert not patched.reads and not patched.writes
        count += 1
    return count


def check_count_prompts(original, candidate):
    # Enter after the readiness check or after completion logging and shutdown.
    paths = ((0x5D84, 0x5D9E, 4, 0x24), (0x5F14, 0x5F2E, 0x0C, 0x23))
    count = 0
    for (address, stop, mode, phrase), variant, index, gain, enabled, completed in product(
            paths, (0, 2), (0, 1, 2, 3, 4, 255), GAINS, (False, True), (0, 12, 255, 65535)):
        stock = PromptTrace(original, mode, index, gain, enabled, completed)
        patched = PromptTrace(candidate, mode, index, gain, enabled, completed)
        for pic in (stock, patched):
            pic.ram[0x2B8] = variant
            pic.bank = 2
            pic.run(address, stop)
        number = 0 if completed == 65535 else completed
        if phrase == 0x24:
            number += 1
        sequence = [("clip", 0x27), ("number", number), ("clip", phrase)]
        if variant == 2:
            if phrase == 0x24:
                sequence.append(("clip", 0x35))
            else:
                sequence.insert(0, ("clip", 0x34))
        expected = GAINS[index] if index < len(GAINS) else gain
        check_prompt_result(stock, patched, sequence, gain, expected)
        assert patched.reads == [0]
        assert patched.writes == ([(0, 0), (1, 0)] if completed == 65535 else [])
        count += 1
    return count


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("image", type=Path)
    args = parser.parse_args()
    original, candidate = load_hex(ORIGINAL), load_hex(args.image)
    assert candidate.keys() == set(range(APPLICATION_START, APPLICATION_END))
    hooks = {a for start, size in HOOKS for a in range(start, start + size)}
    changed = {a for a in original if original[a] != candidate[a]}
    assert changed <= hooks
    assert all(changed.intersection(range(start, start + size)) for start, size in HOOKS)
    assert archive_layout(candidate) == args.image.read_bytes()
    before, after = linear(original), linear(candidate)
    checks = [
        ("restart value", check_restart(after)),
        ("double press and restart", check_cycle(after)),
        ("countdown and timing", check_announcements(before, after)),
        ("long press", check_holds(before, after)),
        ("ready prompt", check_ready_prompts(before, after)),
        ("pause reminder and resume prompts", check_pause_prompts(before, after)),
        ("numbered treatment start and completion prompts", check_count_prompts(before, after)),
    ]
    for name, count in checks:
        print(f"{name}: {count} passed")
    print(f"{sum(count for _, count in checks)} instruction-level cases passed")
    print(f"{len(changed)} changed stock bytes across {len(HOOKS)} hooks")


if __name__ == "__main__":
    main()
