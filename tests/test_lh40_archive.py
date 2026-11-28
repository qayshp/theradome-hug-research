"""Exercise archived readings with synthetic treatment data."""

from datetime import datetime
from decimal import Decimal
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tools.lh40_archive import (
    ArchiveError,
    Treatment,
    load_history,
    merge_history,
    parse_treatments,
    serialize_history,
)


HEADER = (
    "\x1b[H\x1b[J\x1b[3g ****************************** TREATMENTS COMPLETED LOG "
    "******************************\r\n"
    " #\t Date\tDOW\t Time\tBattery\tTemp(C)\tLaser Current(mA)\r\n"
    "-------------------------------------------------------------------\r\n\r"
)


def display_row(number):
    return f"\t{number}\t01/03/2026   FRI\t23:15:10\t4.08\t22.07\t10.48\r\n\r"


def treatment(number):
    return Treatment(number, datetime(2026, 1, 3, 23, 15, 10), "FRI",
                     Decimal("4.08"), Decimal("22.07"), Decimal("10.48"))


class TreatmentDisplayTests(unittest.TestCase):
    def test_full_ring_without_counter(self):
        text = "*** BOARD TEST ***\r\n" + HEADER
        text += "".join(display_row(number) for number in range(11, 61))
        self.assertEqual(parse_treatments(text), [treatment(number) for number in range(11, 61)])

    def test_small_or_empty_ring_requires_counter(self):
        self.assertEqual(parse_treatments(HEADER, expected_count=0), [])
        self.assertEqual(parse_treatments(HEADER, expected_count=65535), [])
        self.assertEqual(parse_treatments(HEADER + display_row(1), expected_count=1), [treatment(1)])
        with self.assertRaises(ArchiveError):
            parse_treatments(HEADER + display_row(1))

    def test_rejects_incomplete_or_inconsistent_capture(self):
        for broken in (
            display_row(1) + display_row(2),
            HEADER + display_row(1),
            HEADER + display_row(1) + display_row(3),
            HEADER + display_row(1) + display_row(2).rstrip(),
            HEADER + display_row(1) + display_row(2).replace("10.48", "10.4"),
            HEADER + display_row(1) + display_row(2).replace("01/03/2026", "02/30/2026"),
            HEADER + display_row(1) + display_row(2) + "3\t01/\r\n",
        ):
            with self.subTest(capture=broken), self.assertRaises(ArchiveError):
                parse_treatments(broken, expected_count=2)


class HistoryMergeTests(unittest.TestCase):
    def test_overlap_updates_once_and_keeps_counter_order_when_clock_goes_back(self):
        older_clock = Treatment(3, datetime(2025, 12, 31), "WED", Decimal("4.00"),
                                Decimal("20.00"), Decimal("10.48"))
        history = [treatment(1), treatment(2)]
        capture = [treatment(2), older_clock]
        result = merge_history(history, capture)
        self.assertEqual(result.rows, (*history, older_clock))
        self.assertEqual(result.added_count, 1)
        self.assertEqual(result.gaps, ())
        self.assertEqual(merge_history(result.rows, capture).added_count, 0)

    def test_reports_gaps_and_can_fill_one_with_matching_overlap(self):
        result = merge_history([treatment(3)], [treatment(5), treatment(6)])
        self.assertEqual(result.gaps, ((1, 2), (4, 4)))
        filled = merge_history(result.rows, [treatment(4), treatment(5)])
        self.assertEqual(filled.gaps, ((1, 2),))
        self.assertEqual(filled.added_count, 1)

    def test_rejects_conflicting_counters_without_discarding_records(self):
        conflict = Treatment(5, datetime(2025, 12, 31), "WED", Decimal("4.00"),
                             Decimal("20.00"), Decimal("10.48"))
        history = [treatment(5), treatment(6)]
        for capture in ([conflict], [treatment(1)], []):
            with self.subTest(capture=capture), self.assertRaises(ArchiveError):
                merge_history(history, capture)
        self.assertEqual(history, [treatment(5), treatment(6)])

    def test_existing_csv_schema_round_trips_and_rejects_duplicate_rows(self):
        history = [treatment(1), treatment(2)]
        serialized = serialize_history(history)
        self.assertTrue(serialized.startswith(
            "session_number,finished_at_helmet_local,helmet_weekday,"
            "battery_voltage_v,temperature_c,laser_current_ma\r\n"
        ))
        with TemporaryDirectory() as directory:
            path = Path(directory) / "treatments.csv"
            self.assertEqual(load_history(path), [])
            path.write_bytes(serialized.encode("utf-8"))
            self.assertEqual(load_history(path), history)
            path.write_bytes((serialized + serialized.splitlines(keepends=True)[-1]).encode("utf-8"))
            with self.assertRaises(ArchiveError):
                load_history(path)


if __name__ == "__main__":
    unittest.main()
