"""Entry point for the frozen Windows app and its packaging smoke check."""

import os
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import tkinter
from traceback import print_exc

from lh40_archive import ArchiveError
from lh40_desktop import DesktopApp, Settings, main, save_settings
from lh40_history import CHART_NAME, TOOLS, get_chart_renderer, get_hug_runtime, run_usb, update_history


def check_hug_launch(hug_path, archive):
    """Verify the vendor child process reaches the bridge's argument check."""
    messages = []
    try:
        run_usb("self-test", get_hug_runtime(Path(hug_path)), "", archive, 1, progress=messages.append)
    except ArchiveError:
        if any(message.startswith("USB session failed. Usage:") for message in messages):
            return
        raise
    raise RuntimeError("The bundled USB bridge accepted invalid self-test arguments.")


def self_test():
    """Check bundled Tcl/Tk, chart rendering and USB resources without a helmet."""
    bridge = TOOLS / "lh40_usb.js"
    if not bridge.is_file():
        raise FileNotFoundError(bridge)

    with TemporaryDirectory(prefix="lh40-package-test-") as folder:
        archive = Path(folder)
        fixture = archive / "fixture.txt"
        fixture.write_text(
            "***** TREATMENTS COMPLETED LOG *****\n"
            "# Date DOW Time Battery Temp(C) Laser Current(mA)\n"
            "------------------------------------------------\n"
            "1 04/06/2026 MON 07:13:00 4.05 23.00 10.49\n",
            encoding="ascii",
        )
        result = update_history(archive, fixture, 1, get_chart_renderer(), progress=lambda _: None)
        if result.added_count != 1 or not (archive / CHART_NAME).read_bytes().startswith(b"\x89PNG\r\n\x1a\n"):
            raise RuntimeError("The bundled chart renderer failed.")

        settings_file = archive / "desktop-settings.json"
        save_settings(settings_file, Settings(str(archive)))
        root = tkinter.Tk()
        try:
            root.withdraw()
            app = DesktopApp(root, settings_file)
            root.update_idletasks()
            if app.archive_text.get() != "1 treatment archived" or not app.chart_button.instate(["!disabled"]):
                raise RuntimeError("The desktop app could not load the test archive.")
        finally:
            root.destroy()

        hug_path = os.environ.get("LH40_SMOKE_HUG_DIR")
        if hug_path:
            check_hug_launch(hug_path, archive)


if __name__ == "__main__":
    if sys.argv[1:] == ["--self-test"]:
        try:
            self_test()
        except Exception:
            if sys.stderr is not None:
                print_exc()
            sys.exit(1)
    else:
        sys.exit(main())
