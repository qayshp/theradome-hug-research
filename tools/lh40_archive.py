"""Parse the LH40 treatment display and merge it into a local CSV history."""

import csv
import io
import re
from dataclasses import astuple, dataclass, fields
from datetime import datetime
from decimal import Decimal
from pathlib import Path
from typing import Iterable


LOG_WINDOW = 50
MAX_SESSION = 0xFFFE  # The EEPROM viewer treats 0xFFFF as zero.
WEEKDAYS = frozenset(("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"))
CONSOLE_ESCAPES = re.compile(r"\x1b(?:\[[0-?]*[ -/]*[@-~]|H)")
TITLE = re.compile(r"\*+ TREATMENTS COMPLETED LOG \*+")
DISPLAY_COLUMNS = "# Date DOW Time Battery Temp(C) Laser Current(mA)".split()
MEASUREMENT = r"[0-9]{1,3}\.[0-9]{2}"
VALUE = re.compile(MEASUREMENT)
DISPLAY_ROW = re.compile(
    r"([0-9]{1,5})\s+([0-9]{2}/[0-9]{2}/[0-9]{4})\s+([A-Z]{3})\s+"
    r"([0-9]{2}:[0-9]{2}:[0-9]{2})\s+"
    rf"({MEASUREMENT})\s+({MEASUREMENT})\s+({MEASUREMENT})"
)
CSV_TIMESTAMP = re.compile(r"[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}")


class ArchiveError(ValueError):
    """A malformed capture, invalid archive, or conflicting treatment counter."""


@dataclass(frozen=True)
class Treatment:
    session_number: int
    finished_at_helmet_local: datetime
    helmet_weekday: str
    battery_voltage_v: Decimal
    temperature_c: Decimal
    laser_current_ma: Decimal


CSV_COLUMNS = tuple(field.name for field in fields(Treatment))


@dataclass(frozen=True)
class MergeResult:
    rows: tuple[Treatment, ...]
    added_count: int
    gaps: tuple[tuple[int, int], ...]


def _record(number, stamp, stamp_format, weekday, measurements, where):
    if not re.fullmatch(r"[0-9]{1,5}", number) or not 1 <= int(number) <= MAX_SESSION:
        raise ArchiveError(f"Invalid session number in {where}.")
    if weekday not in WEEKDAYS:
        raise ArchiveError(f"Invalid weekday in {where}.")
    if any(not VALUE.fullmatch(value) or Decimal(value) >= 256 for value in measurements):
        raise ArchiveError(f"Invalid measurement in {where}.")
    try:
        local_time = datetime.strptime(stamp, stamp_format)
    except ValueError as error:
        raise ArchiveError(f"Invalid helmet date or time in {where}.") from error
    # Preserve the RTC weekday as printed, even when it differs from the date.
    return Treatment(int(number), local_time, weekday, *(Decimal(value) for value in measurements))


def parse_treatments(text: str, *, expected_count: int | None = None) -> list[Treatment]:
    """Read a complete V display, using the D counter to verify a partial ring."""
    cleaned = CONSOLE_ESCAPES.sub("", text)
    lines = [line.strip() for line in cleaned.splitlines() if line.strip()]
    titles = [index for index, line in enumerate(lines) if TITLE.fullmatch(line)]
    if len(titles) != 1:
        raise ArchiveError("Expected one complete treatment log title.")
    report = lines[titles[0] + 1:]
    if len(report) < 2 or report[0].split() != DISPLAY_COLUMNS or not re.fullmatch(r"-{2,}", report[1]):
        raise ArchiveError("The treatment log column headings or separator are missing.")
    # V has no footer. Its final line break and a fully populated expected window
    # are the evidence that transmission finished.
    if not cleaned.rstrip(" \t").endswith(("\r", "\n")):
        raise ArchiveError("The treatment log ends in an unterminated line.")
    rows = []
    for offset, line in enumerate(report[2:], 1):
        match = DISPLAY_ROW.fullmatch(line)
        if match is None:
            raise ArchiveError(f"Incomplete or unexpected treatment log row {offset}.")
        number, date, weekday, time, *measurements = match.groups()
        rows.append(_record(number, f"{date} {time}", "%m/%d/%Y %H:%M:%S", weekday,
                            measurements, f"treatment log row {offset}"))

    if expected_count is None:
        if len(rows) != LOG_WINDOW:
            raise ArchiveError(f"Expected a full {LOG_WINDOW}-row capture without a separate treatment count.")
        expected_count = rows[-1].session_number
    if not 0 <= expected_count <= MAX_SESSION:
        raise ArchiveError("The reported treatment count is outside the supported range.")
    first = max(1, expected_count - LOG_WINDOW + 1)
    expected_numbers = list(range(first, expected_count + 1))
    if [row.session_number for row in rows] != expected_numbers:
        raise ArchiveError("Treatment rows do not match the reported counter and 50-entry window.")
    return rows


def _by_number(rows: Iterable[Treatment]) -> dict[int, Treatment]:
    indexed = {}
    previous = 0
    for row in rows:
        number = row.session_number
        if not 1 <= number <= MAX_SESSION or number <= previous:
            raise ArchiveError("Treatment session numbers must be unique and in increasing order.")
        indexed[number] = row
        previous = number
    return indexed


def load_history(path: Path) -> list[Treatment]:
    """Read the existing six-column CSV. A missing file starts an empty history."""
    try:
        with Path(path).open(encoding="utf-8-sig", newline="") as file:
            reader = csv.reader(file, strict=True)
            if tuple(next(reader, ())) != CSV_COLUMNS:
                raise ArchiveError("The history CSV has unrecognized or missing columns.")
            rows = []
            for line, row in enumerate(reader, 2):
                where = f"history CSV row {line}"
                if len(row) != len(CSV_COLUMNS) or not CSV_TIMESTAMP.fullmatch(row[1]):
                    raise ArchiveError(f"Invalid format in {where}.")
                number, stamp, weekday, *measurements = row
                rows.append(_record(number, stamp, "%Y-%m-%d %H:%M:%S", weekday, measurements, where))
    except FileNotFoundError:
        return []
    except (OSError, UnicodeError, csv.Error) as error:
        raise ArchiveError(f"Cannot read the history CSV: {error}") from error
    _by_number(rows)
    return rows


def merge_history(history: Iterable[Treatment], capture: Iterable[Treatment]) -> MergeResult:
    """Keep all known records and report the inclusive ranges missing from the CSV."""
    archived = _by_number(history)
    incoming = _by_number(capture)
    overlap = archived.keys() & incoming.keys()
    for number in sorted(overlap):
        if archived[number] != incoming[number]:
            raise ArchiveError(f"Treatment {number} disagrees with the archive. The counter may have reset.")
    new_numbers = incoming.keys() - archived.keys()
    if archived and not overlap and (not incoming or min(incoming) <= max(archived)):
        raise ArchiveError("The capture has no matching records and the counter may have reset.")
    combined = archived | incoming
    rows = tuple(combined[number] for number in sorted(combined))
    gaps = []
    previous = 0
    for row in rows:
        if row.session_number > previous + 1:
            gaps.append((previous + 1, row.session_number - 1))
        previous = row.session_number
    return MergeResult(rows, len(new_numbers), tuple(gaps))


def serialize_history(rows: Iterable[Treatment]) -> str:
    """Write the established CSV schema with naive helmet-local timestamps."""
    output = io.StringIO(newline="")
    writer = csv.writer(output)
    writer.writerow(CSV_COLUMNS)
    for row in _by_number(rows).values():
        number, stamp, weekday, *measurements = astuple(row)
        writer.writerow((number, stamp.isoformat(sep=" ", timespec="seconds"), weekday,
                         *(format(value, ".2f") for value in measurements)))
    return output.getvalue()
