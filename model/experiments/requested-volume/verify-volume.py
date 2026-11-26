"""Verify the experimental image against the archived program on the host."""
import argparse
from collections import Counter
from itertools import product
from pathlib import Path
import runpy
import sys


class Case:
    def __init__(self, mode, event, index, gain, ticks, slot):
        self.mode, self.event = mode, event
        self.index, self.gain = index, gain
        self.ticks, self.slot = ticks, slot

    def run(self, runner, voice, image):
        pic = runner(image, voice(self.index, self.gain, self.mode, self.slot))
        pic.putpair(0x26F, self.ticks)
        if self.event == 0:
            pic.run(0xCA60)
        else:
            pic.w, pic.ram[0xF3] = self.mode, self.event
            pic.run(0x7082)
        return pic


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("repo", type=Path, help="path to the stock research checkout")
    parser.add_argument("hex", type=Path, help="experimental Intel HEX")
    args = parser.parse_args()
    model = args.repo / "model"
    sys.path.insert(0, str(model.resolve()))
    from voice_check import Runner, Voice

    class Trace(Runner):
        def __init__(self, image, state):
            super().__init__(image, state)
            self.speech = []

        def peripheral(self, pc):
            if pc == 0xA4C0:
                self.speech.append(("number", self.pair(0xC8), self.ram[0x2B9]))
            if pc == 0x94F2:
                self.speech.append(("clip", self.pair(0x81), self.ram[0x2B9]))
            return super().peripheral(pc)

    load_hex = runpy.run_path(str(model / "tools/rebuild.py"))["load_hex"]
    original = load_hex(args.repo / "outputs/myTheradome-LH80-labeled.hex")
    candidate = load_hex(args.hex)
    assert candidate.keys() == original.keys() == set(range(0x1900, 0xDF80))
    changed = [address for address in sorted(original) if original[address] != candidate[address]]
    assert len(changed) == 48 and all(0x7360 <= address < 0x7398 for address in changed)
    records = [bytes.fromhex(line[1:].decode("ascii")) for line in args.hex.read_bytes().splitlines()]
    assert len(records) == 795 and all(row[0] == 64 and row[3] == 0 for row in records[:-1])
    assert records[-1] == bytes.fromhex("00000001FF")

    def linear(image):
        code = bytearray(65536)
        for address, value in image.items():
            code[address] = value
        return code

    before, after = linear(original), linear(candidate)
    gains = (0, 1, 3, 14)
    ticks = (0, 1, 29, 30, 149, 150, 299, 300, 449, 450, 599, 600, 65535)
    totals = Counter()

    def compare(case):
        stock = case.run(Trace, Voice, before)
        patched = case.run(Trace, Voice, after)
        expected = stock.speech
        if case.event == 2 and case.index < 4:
            expected = [("number", case.ticks // 30 + 1, gains[case.index]),
                        ("clip", 0x2D, gains[case.index])]
            assert stock.speech == [(kind, value, 0) for kind, value, _ in expected]
        assert patched.speech == expected, vars(case)
        assert patched.ram == stock.ram, vars(case)
        assert patched.slot7 == stock.slot7, vars(case)

    for mode, index, gain, tick in product((7, 8), range(4), gains, ticks):
        compare(Case(mode, 2, index, gain, tick, 123))
        compare(Case(mode, 0, index, gain, tick, 123))
        totals.update(("requested", "automatic"))

    for mode, index, gain in product((7, 8), range(4), gains):
        compare(Case(mode, 5, index, gain, 450, 123))
        totals.update(("double press",))
        for slot in (0, 36, 65534, 65535):
            compare(Case(mode, 3, index, gain, 450, slot))
            totals.update(("long press",))

    for mode, index, gain in product((7, 8), (4, 255), gains):
        compare(Case(mode, 2, index, gain, 450, 123))
        totals.update(("invalid selection",))

    print(f"{sum(totals.values())} instruction-level cases passed.")
    for name, count in totals.items():
        print(f"{name}: {count}")
    print("Both parts of a requested announcement used selected gain. Post-call RAM and counter matched stock.")
    print(f"All {len(changed)} changed bytes are in the two time-request button cases.")


if __name__ == "__main__":
    main()

