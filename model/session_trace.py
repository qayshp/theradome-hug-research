"""Run one pass of an archived session handler, given the results of its input checks.

The PIC instructions decide the next state. Peripheral service calls end at
named boundaries so the trace also shows which observations support an inference.
"""
import argparse
from collections import defaultdict
from state_paths import read_image, run

HANDLERS = {4: 0x5D60, 7: 0x5DF0, 8: 0x5E6C, 9: 0x5ECC}


def visit(image, mode, expired=False, checks_pass=True, flag_set=True,
          variant=2, attenuation=0, prompt_stage=0):
    """Return the requested mode and ordered service calls for one periodic pass."""
    ram = defaultdict(int, {0x2B4: mode, 0x2B5: mode, 0x2B8: variant,
                            0x27B: int(flag_set), 0x2B9: attenuation, 0x2B3: prompt_stage})
    calls = {}
    events = []

    def hook(address, describe, bank, carry=False):
        def action(w, mem):
            events.append(describe(w, mem))
            return 0, bank, carry
        calls[address] = action

    hook(0xD358, lambda w, m: f"countdown {w} {'expired' if expired else 'running'}", 0, expired)
    hook(0x80BA, lambda w, m: f"subsystem checks {'passed' if checks_pass else 'failed'}", 0, checks_pass)
    hook(0xCA60, lambda w, m: f"check automatic time announcement (shift {m[0x2B9]})", 0)
    hook(0xC5B8, lambda w, m: f"load countdown {w} with {m[0x66] | m[0x67] << 8} ticks", 2)
    hook(0xD892, lambda w, m: f"increment EEPROM counter {w}", 0)
    hook(0x94F2, lambda w, m: f"request audio clip 0x{m[0x81]:02X}", 0)
    hook(0xCC56, lambda w, m: f"request count announcement (clip argument 0x{w:02X})", 0)
    hook(0x9C4C, lambda w, m: f"call device service 0x9C4C({w})", 0)
    hook(0xBF0E, lambda w, m: "update output configuration", 0)
    hook(0x9FD2, lambda w, m: f"request indicator for mode 0x{w:02X}", 0)
    hook(0x9A7E, lambda w, m: f"call service 0x9A7E with 0x{m[0xC8]:02X}", 0)
    run(image, HANDLERS[mode], ram, 0, lambda pc: pc == 0x6056, bank=2, calls=calls)
    return ram[0x2B4], events


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", type=lambda x: int(x, 0), choices=HANDLERS)
    parser.add_argument("--expired", action="store_true", help="the selected countdown reached zero")
    parser.add_argument("--check-failed", action="store_true", help="0x80BA returned carry clear")
    parser.add_argument("--flag-clear", action="store_true", help="clear RAM 0x27B bit 0 after the check")
    parser.add_argument("--shift", choices=(0, 1, 3, 14), type=int, default=0)
    args = parser.parse_args()
    new, events = visit(read_image(), args.mode, args.expired, not args.check_failed,
                        not args.flag_clear, attenuation=args.shift)
    for event in events:
        print(event)
    print(f"Mode 0x{args.mode:02X} requests 0x{new:02X} before asynchronous events are serviced.")


if __name__ == "__main__":
    main()
