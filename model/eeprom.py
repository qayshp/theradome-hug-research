"""Read the completed-treatment log from an offline raw PIC data EEPROM dump."""

import argparse
from datetime import datetime
import json
from pathlib import Path


RING_START = 0xC8
RING_ENTRIES = 50
ENTRY_BYTES = 16


def bcd(value):
    hi, lo = divmod(value, 16)
    if hi > 9 or lo > 9:
        raise ValueError("invalid packed BCD")
    return hi * 10 + lo


def decode_record(record):
    try:
        century, year, month, day, dow, hour, minute, second = map(bcd, record[:8])
        if dow > 6:
            raise ValueError("invalid weekday")
        timestamp = datetime(century * 100 + year, month, day, hour, minute, second)
    except ValueError:
        timestamp, dow = None, None

    def display_pair(start):
        # The menu prints the two unsigned bytes as separate decimal fields.
        return f"{record[start]}.{record[start + 1]:02d}"

    return {
        "raw": record.hex(" "),
        "clock_local": timestamp.isoformat() if timestamp is not None else None,
        "weekday_sunday_zero": dow,
        "battery_display": display_pair(8),
        "temperature_C_display": display_pair(10),
        "laser_current_mA_display": display_pair(12),
        "trailing_word": int.from_bytes(record[14:16], "little"),
    }


def interpret(data):
    end = RING_START + RING_ENTRIES * ENTRY_BYTES
    if len(data) < end or len(data) > 1024 or len(data) % 2:
        raise ValueError("Expected 1000 to 1024 even-length raw EEPROM bytes starting at address zero")
    raw_count = int.from_bytes(data[:2], "little")
    count = 0 if raw_count == 0xFFFF else raw_count
    records = []
    for index in range(max(0, count - RING_ENTRIES), count):
        address = RING_START + (index % RING_ENTRIES) * ENTRY_BYTES
        records.append({
            "session": index + 1,
            "eeprom_byte_offset": address,
            **decode_record(data[address:address + ENTRY_BYTES]),
        })
    serial = data[0x64:0x6E]
    result = {
        "counter_word_raw": raw_count,
        "completed_count": count,
        "serial_number_raw": serial.hex(" "),
        "serial_number_ascii": serial.decode("ascii") if all(32 <= c <= 126 for c in serial) else None,
        "records": records,
    }
    if len(data) >= 0x3EA:
        result["product_code_revision_word_raw"] = int.from_bytes(data[0x3E8:0x3EA], "little")
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("dump", type=Path, help="raw EEPROM binary beginning at data address zero")
    args = parser.parse_args()
    try:
        print(json.dumps(interpret(args.dump.read_bytes()), indent=2))
    except (OSError, ValueError) as error:
        parser.exit(1, str(error) + "\n")
