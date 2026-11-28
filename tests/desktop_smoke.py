"""Exercise the desktop buttons with a temporary archive and simulated USB."""

import os
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
from threading import Event
from time import monotonic, sleep
import tkinter as tk
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))

import lh40_desktop as desktop
import lh40_history as history


PORT = "COM7" if os.name == "nt" else "/dev/ttyUSB0"


class DesktopSmokeTest(unittest.TestCase):
    def test_import_chart_and_explicit_sync(self):
        temporary = TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        directory = Path(temporary.name)
        archive = directory / "archive"
        settings = directory / "settings.json"
        desktop.save_settings(settings, desktop.Settings(str(archive), "example HUG", port=PORT))
        root = tk.Tk()
        root.withdraw()
        self.addCleanup(root.destroy)
        app = desktop.DesktopApp(root, settings)
        sent = []
        release = Event()
        self.addCleanup(release.set)

        def await_status(condition):
            deadline = monotonic() + 10
            while not condition() and monotonic() < deadline:
                root.update()
                sleep(0.02)
            self.assertTrue(condition(), app.status.get())

        def helmet(mode, runtime, port, capture, wake_seconds, *, progress):
            sent.append(mode)
            progress("Hold the master switch until the helmet speaks its revision, then release.")
            if mode == "sync-clock":
                return
            if not release.wait(10):
                raise TimeoutError("The simulated button hold timed out.")
            (capture / "clock-before.txt").write_text(
                "Date Read - MON   2026-04-06\nTime Read - 07:13:00\n", encoding="ascii")
            (capture / "clock-before-host-ms.txt").write_text("1775459580000\n", encoding="ascii")
            (capture / "treatment-count.txt").write_text("1\n", encoding="ascii")
            (capture / "treatments.txt").write_text(
                "***** TREATMENTS COMPLETED LOG *****\n"
                "# Date DOW Time Battery Temp(C) Laser Current(mA)\n"
                "------------------------------------------------\n"
                "1 04/06/2026 MON 07:13:00 4.05 23.00 10.49\n",
                encoding="ascii",
            )

        with patch.object(history, "run_usb", side_effect=helmet), patch.object(history, "get_hug_runtime"):
            self.assertTrue(app.chart_button.instate(["disabled"]))
            app.import_button.invoke()
            self.assertTrue(app.busy)
            self.assertTrue(app.sync_button.instate(["disabled"]))
            await_status(lambda: "hold the master switch" in app.status.get())
            release.set()
            await_status(lambda: not app.busy)

            self.assertEqual(sent, ["import"])
            self.assertEqual(app.archive_text.get(), "1 treatment archived")
            self.assertEqual(app.status.get(), "Imported 1 new treatment. Chart updated.")
            csv, png = archive / history.CSV_NAME, archive / history.CHART_NAME
            saved = (csv.read_bytes(), png.read_bytes())
            self.assertTrue(saved[1].startswith(b"\x89PNG"))
            self.assertEqual(len(list((archive / "captures").glob("*/treatments.txt"))), 1)
            with patch.object(desktop, "open_chart_file") as opener:
                app.chart_button.invoke()
                opener.assert_called_once_with(png)

            with patch.object(desktop.messagebox, "askyesno", return_value=False):
                app.sync_button.invoke()
                self.assertEqual(sent, ["import"])
            with patch.object(desktop.messagebox, "askyesno", return_value=True):
                app.sync_button.invoke()
                await_status(lambda: not app.busy)
            self.assertEqual(sent, ["import", "sync-clock"])
            self.assertEqual(app.status.get(), "Clock synced and verified against the laptop.")
            self.assertEqual((csv.read_bytes(), png.read_bytes()), saved)


if __name__ == "__main__":
    unittest.main()
