"""Keep a local history of LH40 treatments and optionally set the helmet clock."""

import argparse
from contextlib import contextmanager
from datetime import datetime
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
from tempfile import TemporaryDirectory, mkdtemp

from lh40_archive import ArchiveError, load_history, merge_history, parse_treatments, serialize_history
from verify_packages import WINDOWS_LAUNCHER


TOOLS = Path(__file__).resolve().parent
CSV_NAME = "treatments.csv"
CHART_NAME = "treatment-history.png"


def archive_directory(path):
    archive = path.expanduser().resolve()
    if archive.is_relative_to(TOOLS.parent):
        raise ArchiveError("Choose an archive folder outside this repository.")
    archive.mkdir(parents=True, exist_ok=True)
    return archive


def capture_directory(archive):
    captures = archive / "captures"
    captures.mkdir(exist_ok=True)
    prefix = datetime.now().strftime("%Y%m%d-%H%M%S-")
    return Path(mkdtemp(prefix=prefix, dir=captures))


@contextmanager
def archive_lock(archive):
    with (archive / ".history.lock").open("a+b") as lock:
        try:
            if os.name == "nt":
                import msvcrt

                if lock.tell() == 0:
                    lock.write(b"\0")
                    lock.flush()
                lock.seek(0)
                msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
            else:
                import fcntl

                fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except OSError as error:
            raise ArchiveError("The history archive is in use. Retry after the other import finishes.") from error
        yield


def update_history(archive, source, completed_count, render_chart):
    capture = parse_treatments(source.read_bytes().decode("latin-1"), expected_count=completed_count)
    csv_path = archive / CSV_NAME

    with archive_lock(archive):
        result = merge_history(load_history(csv_path), capture)
        with TemporaryDirectory(prefix=".history-", dir=archive) as stage:
            staging = Path(stage)
            staged_chart = staging / CHART_NAME
            render_chart(result.rows, staged_chart)
            if result.added_count or not csv_path.exists():
                staged_csv = staging / CSV_NAME
                staged_csv.write_bytes(serialize_history(result.rows).encode("utf-8"))
                os.replace(staged_csv, csv_path)
            # Both outputs are ready before replacing either. If the PNG is
            # open in another program, replaying the raw capture repairs it.
            os.replace(staged_chart, archive / CHART_NAME)

    print(f"{result.added_count} new treatments, {len(result.rows)} archived.")
    print(f"CSV: {csv_path}")
    print(f"Chart: {archive / CHART_NAME}")
    if result.gaps:
        ranges = ", ".join(str(start) if start == end else f"{start}-{end}"
                           for start, end in result.gaps)
        print(f"Treatment numbers missing from the archive: {ranges}")


def get_chart_renderer():
    try:
        from lh40_chart import render_chart
    except ImportError as error:
        raise ArchiveError("Install Matplotlib with python -m pip install 'matplotlib>=3.6'.") from error
    return render_chart


def get_hug_runtime(path):
    hug = path.expanduser().resolve()
    launcher = hug / "HugUpgrade.exe"
    jjs = hug / "jre" / "bin" / "jjs.exe"
    if not launcher.is_file() or not jjs.is_file():
        raise ArchiveError("Choose the original HUG folder containing HugUpgrade.exe and jre/bin/jjs.exe.")
    if hashlib.sha256(launcher.read_bytes()).hexdigest() != WINDOWS_LAUNCHER:
        raise ArchiveError("HugUpgrade.exe differs from the archived Windows build. Use the original HUG launcher.")
    return jjs, launcher


def run_usb(mode, runtime, port, capture, wake_seconds):
    jjs, launcher = runtime
    command = [str(jjs), "-scripting", "-cp", str(launcher), str(TOOLS / "lh40_usb.js"),
               "--", mode, port, str(capture), str(wake_seconds)]
    result = subprocess.run(command, check=False)
    if result.returncode:
        raise ArchiveError(f"USB {mode} did not complete. Review the output and saved responses.")


def show_clock_offset(capture):
    report = (capture / "clock-before.txt").read_bytes().decode("latin-1")
    date = re.search(r"Date Read - [A-Z]{3} +([0-9]{4}-[0-9]{2}-[0-9]{2})", report)
    time = re.search(r"Time Read - ([0-9]{2}:[0-9]{2}:[0-9]{2})", report)
    if not date or not time:
        raise ArchiveError("The capture does not contain a recognizable clock display.")
    try:
        helmet_time = datetime.fromisoformat(f"{date[1]} {time[1]}")
        host_ms = int((capture / "clock-before-host-ms.txt").read_text(encoding="ascii"))
        host_time = datetime.fromtimestamp(host_ms / 1000)
    except (ValueError, OverflowError):
        print("The helmet clock was recorded but could not be compared with the laptop clock.")
        return
    offset = round((helmet_time - host_time).total_seconds())
    print(f"Helmet clock: {helmet_time:%Y-%m-%d %H:%M:%S} (helmet local time).")
    print(f"Approximate clock offset (helmet minus laptop): {offset:+d} seconds.")


def port_name(value):
    if not re.fullmatch(r"COM[1-9][0-9]*", value.upper()):
        raise argparse.ArgumentTypeError("Use a Windows port name such as COM7.")
    return value.upper()


def wake_seconds(value):
    seconds = int(value)
    if not 1 <= seconds <= 300:
        raise argparse.ArgumentTypeError("Choose a wait from 1 to 300 seconds.")
    return seconds


def parser():
    result = argparse.ArgumentParser(description=__doc__)
    commands = result.add_subparsers(dest="mode", required=True)
    for mode, help_text in (
        ("import", "Read the treatment history and clock over USB, then update the local archive."),
        ("import-log", "Update the local archive from a saved treatment display."),
        ("sync-clock", "Write the laptop's local time to the helmet and verify it by reading back."),
    ):
        command = commands.add_parser(mode, help=help_text)
        command.add_argument("--archive", required=True, type=Path, help="Local archive folder outside the repository.")
        if mode == "import-log":
            command.add_argument("log", type=Path, help="Saved raw treatment display.")
            command.add_argument("--completed-count", type=int, help="Lifetime count reported at capture time.")
        else:
            command.add_argument("--hug-dir", required=True, type=Path, help="Original Windows HUG application folder.")
            command.add_argument("--port", required=True, type=port_name, help="Windows port, for example COM7.")
            command.add_argument("--wake-seconds", type=wake_seconds, default=60, help="Time to wait for the switch hold.")
    return result


def main(argv=None):
    args = parser().parse_args(argv)
    try:
        archive = archive_directory(args.archive)
        if args.mode != "sync-clock":
            render_chart = get_chart_renderer()
        if args.mode != "import-log":
            runtime = get_hug_runtime(args.hug_dir)

        capture = capture_directory(archive)
        print(f"Local capture: {capture}", flush=True)
        if args.mode == "import-log":
            source = capture / "treatments.txt"
            shutil.copyfile(args.log, source)
            if args.completed_count is not None:
                (capture / "treatment-count.txt").write_text(f"{args.completed_count}\n", encoding="ascii")
            update_history(archive, source, args.completed_count, render_chart)
        else:
            run_usb(args.mode, runtime, args.port, capture, args.wake_seconds)
            if args.mode == "import":
                completed_count = int((capture / "treatment-count.txt").read_text(encoding="ascii"))
                show_clock_offset(capture)
                update_history(archive, capture / "treatments.txt", completed_count, render_chart)
    except (OSError, ValueError) as error:
        print(f"LH40 history: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
