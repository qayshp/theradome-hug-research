"""Check numbered speech clip selection in the original PIC18 application."""

from check_inputs import read_image
from tools.rebuild import ORIGINAL
from voice_check import Runner, Voice


class NumberTrace(Runner):
    """Start at the clip composer with packed digits and capture playback calls."""

    def __init__(self, image, value):
        super().__init__(image, Voice())
        packed_digits = bytes.fromhex(f"{value:06d}")
        self.ram[0xD0:0xD3] = packed_digits
        self.clips = []

    def peripheral(self, pc):
        if pc != 0x94F2:
            return False
        self.clips.append(self.pair(0x81))
        return True


def two_digit_clips(value):
    if value < 20:
        return [value] if value else []
    tens, ones = divmod(value, 10)
    clips = [0x12 + tens]
    if ones:
        clips.append(ones)
    return clips


def expected_clips(value):
    if value == 0:
        return [0]
    thousands, remainder = divmod(value, 1000)
    hundreds, remainder = divmod(remainder, 100)
    clips = []
    if thousands:
        clips.extend(two_digit_clips(thousands))
        clips.append(0x1D)
    if hundreds:
        clips.extend([hundreds, 0x1C])
    clips.extend(two_digit_clips(remainder))
    return clips


def main():
    image = read_image(ORIGINAL)
    case_count = 1 << 16
    for value in range(case_count):
        trace = NumberTrace(image, value)
        trace.run(0xA562)
        expected = expected_clips(value)
        if trace.clips != expected:
            raise AssertionError(f"{value}: requested {trace.clips}, expected {expected}")
    print(f"Matched clip selection for all {case_count:,} unsigned 16-bit values.")


if __name__ == "__main__":
    main()
