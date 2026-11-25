"""Check the observation table against the archived mode and indicator dispatchers."""
from itertools import product

from session_trace import HANDLERS, visit
from state_paths import dispatch, indicator, read_image


def session_outcome(mode, expired, checks_pass, flag_set):
    if expired:
        return 0x0C if mode == 7 else 0x0A
    if mode == 9 or not checks_pass:
        return 9
    if flag_set:
        return 7
    return 4 if mode == 4 else 8


def main():
    image = read_image()
    fade_modes = set()
    for mode in range(256):
        periodic = dispatch(image, mode, 0x5FD4, 0x5FD4, 0x6052, 0x2B5)
        if mode in HANDLERS:
            assert periodic == HANDLERS[mode]
        dispatch(image, mode, 0x6488, 0x6488, 0x6502, 0x2B5)
        dispatch(image, mode, 0x7478, 0x7478, 0x74DA, 0xEA)
        for flags in range(16):
            light = indicator(image, mode, flags)
            if light.selector == 3 and light.pattern == 13:
                assert (light.red, light.green) == (17, 37)
                fade_modes.add(mode)
            if mode == 1:
                assert (light.selector, light.pattern, light.red, light.green) == (2, 9, 0, 7)
            if mode in (7, 8):
                assert light.selector == (4 if flags & 0x0C else 2)
                assert light.pattern == (9 if mode == 7 else 10)
    assert fade_modes == {0x02, 0x12}
    print("Exercised 768 dispatcher lookups and 4,096 indicator cases.")

    cases = product(HANDLERS, (False, True), (False, True), (False, True),
                    (0, 1, 2, 255), (0, 1, 3, 14), (0, 1, 2, 255))
    for mode, expired, checks_pass, flag_set, variant, gain, stage in cases:
        new_mode, events = visit(image, mode, expired, checks_pass, flag_set, variant, gain, stage)
        assert new_mode == session_outcome(mode, expired, checks_pass, flag_set)
        if mode == 4 and new_mode == 7:
            assert "load countdown 0 with 600 ticks" in events
        if mode == 8 and new_mode == 7:
            assert not any(event.startswith("load countdown 0") for event in events)
        if expired and mode != 7:
            assert "increment EEPROM counter 8" in events
    print("Checked the requested next state in 2,048 session passes.")


if __name__ == "__main__":
    main()
