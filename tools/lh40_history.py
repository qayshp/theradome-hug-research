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
from verify_packages import MAC_JAR, WINDOWS_LAUNCHER


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


def update_history(archive, source, completed_count, render_chart, *, progress=print):
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

    progress(f"{result.added_count} new treatments, {len(result.rows)} archived.")
    progress(f"CSV: {csv_path}")
    progress(f"Chart: {archive / CHART_NAME}")
    if result.gaps:
        ranges = ", ".join(str(start) if start == end else f"{start}-{end}"
                           for start, end in result.gaps)
        progress(f"Treatment numbers missing from the archive: {ranges}")
    return result


def get_chart_renderer():
    try:
        from lh40_chart import render_chart
    except ImportError as error:
        raise ArchiveError("Install Matplotlib with python -m pip install 'matplotlib>=3.6'.") from error
    return render_chart


def get_hug_runtime(path, jjs_path=None):
    hug = path.expanduser().resolve()
    if path.suffix.lower() != ".jar":
        launcher = hug / "HugUpgrade.exe"
        jjs = hug / "jre" / "bin" / "jjs.exe"
        if not launcher.is_file() or not jjs.is_file():
            raise ArchiveError("Choose the original HUG folder containing HugUpgrade.exe and jre/bin/jjs.exe.")
        expected = WINDOWS_LAUNCHER
    else:
        launcher = hug
        expected = MAC_JAR
        if not launcher.is_file():
            raise ArchiveError("Choose the original HUG myapp.jar.")
        jjs = jjs_path.expanduser().resolve() if jjs_path else shutil.which("jjs")
        if not jjs or not Path(jjs).is_file():
            raise ArchiveError("Choose the Java 8 JDK's bin/jjs executable.")
        jjs = Path(jjs).resolve()
    if hashlib.sha256(launcher.read_bytes()).hexdigest() != expected:
        raise ArchiveError(f"{launcher.name} differs from the archived HUG build. Choose the original application.")
    return jjs, launcher


def run_usb(mode, runtime, port, capture, wake_seconds, *, progress=None):
    jjs, launcher = runtime
    command = [str(jjs), "-scripting", "-cp", str(launcher), str(TOOLS / "lh40_usb.js"),
               "--", mode, port, str(capture), str(wake_seconds)]
    if progress is None:
        code = subprocess.run(command, check=False).returncode
    else:
        failure = ""
        with subprocess.Popen(
            command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            text=True, encoding="utf-8", errors="replace",
            creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0,
        ) as process:
            for line in process.stdout:
                message = line.strip()
                if message.startswith("USB session failed."):
                    failure = message
                if message:
                    progress(message)
            code = process.wait()
        if code and failure:
            raise ArchiveError(failure)
    if code:
        raise ArchiveError(f"USB {mode} did not complete. Review the output and saved responses.")


def show_clock_offset(capture, *, progress=print):
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
        progress("The helmet clock was recorded but could not be compared with the laptop clock.")
        return
    offset = round((helmet_time - host_time).total_seconds())
    progress(f"Helmet clock: {helmet_time:%Y-%m-%d %H:%M:%S} (helmet local time).")
    progress(f"Approximate clock offset (helmet minus laptop): {offset:+d} seconds.")


def run_operation(mode, archive, hug_path, port, wait, *, jjs_path=None, progress=None):
    if mode not in ("import", "sync-clock"):
        raise ValueError("Unknown USB operation.")
    archive = archive_directory(archive)
    render_chart = get_chart_renderer() if mode == "import" else None
    runtime = get_hug_runtime(hug_path, jjs_path)
    capture = capture_directory(archive)
    report = progress or (lambda message: print(message, flush=True))
    report(f"Local capture: {capture}")
    run_usb(mode, runtime, port, capture, wait, progress=progress)
    if mode == "import":
        count = int((capture / "treatment-count.txt").read_text(encoding="ascii"))
        show_clock_offset(capture, progress=report)
        return update_history(archive, capture / "treatments.txt", count, render_chart, progress=report)
    return None


def port_name(value):
    if os.name == "nt":
        if re.fullmatch(r"COM[1-9][0-9]*", value.upper()):
            return value.upper()
        raise argparse.ArgumentTypeError("Choose a Windows port such as COM7.")
    if re.fullmatch(r"/dev/(?:cu\.[A-Za-z0-9][A-Za-z0-9._:-]*|tty(?:USB|ACM)[0-9]+|"
                    r"serial/by-id/[A-Za-z0-9][A-Za-z0-9._:-]*)", value):
        return value
    raise argparse.ArgumentTypeError("Choose a USB serial port such as /dev/cu.usbserial or /dev/ttyUSB0.")


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
            source = command.add_mutually_exclusive_group(required=True)
            source.add_argument("--hug-dir", type=Path, help="Original Windows HUG application folder.")
            source.add_argument("--hug-jar", type=Path, help="Original myapp.jar from Mac HUG.")
            command.add_argument("--jjs", type=Path, help="Java 8 bin/jjs for --hug-jar. Defaults to jjs on PATH.")
            command.add_argument("--port", required=True, type=port_name, help="USB serial port.")
            command.add_argument("--wake-seconds", type=wake_seconds, default=60, help="Time to wait for the switch hold.")
    return result


def main(argv=None):
    args = parser().parse_args(argv)
    try:
        if args.mode == "import-log":
            archive = archive_directory(args.archive)
            render_chart = get_chart_renderer()
            capture = capture_directory(archive)
            print(f"Local capture: {capture}", flush=True)
            source = capture / "treatments.txt"
            shutil.copyfile(args.log, source)
            if args.completed_count is not None:
                (capture / "treatment-count.txt").write_text(f"{args.completed_count}\n", encoding="ascii")
            update_history(archive, source, args.completed_count, render_chart)
        else:
            run_operation(args.mode, args.archive, args.hug_dir or args.hug_jar,
                          args.port, args.wake_seconds, jjs_path=args.jjs)
    except (OSError, ValueError) as error:
        print(f"LH40 history: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
