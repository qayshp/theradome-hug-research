import contextlib
from io import StringIO
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

TOOLS = Path(__file__).resolve().parents[1] / "tools"
sys.path.insert(0, str(TOOLS))

import lh40_history as cli


DISPLAY = (
    b"***** TREATMENTS COMPLETED LOG *****\r\n"
    b"# Date DOW Time Battery Temp(C) Laser Current(mA)\r\n"
    b"------------------------------------------------\r\n"
    b"1 04/06/2026 MON 07:13:00 4.05 23.00 10.49\r\n"
)


def fake_chart(rows, path):
    path.write_text(f"Chart from {len(rows)} rows", encoding="utf-8")


class HistoryCommandTests(unittest.TestCase):
    def setUp(self):
        temporary = TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.archive = Path(temporary.name)
        self.log = self.archive / "log.txt"
        self.log.write_bytes(DISPLAY)
        self.output = StringIO()
        redirects = contextlib.ExitStack()
        self.addCleanup(redirects.close)
        redirects.enter_context(contextlib.redirect_stdout(self.output))
        redirects.enter_context(contextlib.redirect_stderr(self.output))

    def import_log(self):
        return cli.main(["import-log", "--archive", str(self.archive),
                         "--completed-count", "1", str(self.log)])

    def run_usb_command(self, mode):
        return cli.main([mode, "--archive", str(self.archive),
                         "--hug-dir", str(self.archive), "--port", "COM7"])

    def test_replay_preserves_raw_bytes_and_updates_archive_once(self):
        with patch.object(cli, "get_chart_renderer", return_value=fake_chart):
            self.assertEqual(self.import_log(), 0)
            original = (self.archive / cli.CSV_NAME).read_bytes()
            self.assertEqual(self.import_log(), 0)

        captures = list((self.archive / "captures").glob("*/treatments.txt"))
        self.assertEqual([path.read_bytes() for path in captures], [DISPLAY, DISPLAY])
        self.assertEqual((self.archive / cli.CSV_NAME).read_bytes(), original)
        self.assertEqual(len(original.splitlines()), 2)
        self.assertEqual((self.archive / cli.CHART_NAME).read_text(), "Chart from 1 rows")
        self.assertIn("0 new treatments, 1 archived", self.output.getvalue())

    def test_chart_failure_preserves_existing_csv_and_new_raw_capture(self):
        with patch.object(cli, "get_chart_renderer", return_value=fake_chart):
            self.assertEqual(self.import_log(), 0)
        before = (self.archive / cli.CSV_NAME).read_bytes()
        new_log = DISPLAY + b"2 04/07/2026 TUE 07:13:00 4.03 23.00 10.49\r\n"
        self.log.write_bytes(new_log)

        def fail_chart(rows, path):
            raise OSError("chart write failed")

        with patch.object(cli, "get_chart_renderer", return_value=fail_chart):
            self.assertEqual(cli.main(["import-log", "--archive", str(self.archive),
                                       "--completed-count", "2", str(self.log)]), 1)
        self.assertEqual((self.archive / cli.CSV_NAME).read_bytes(), before)
        captures = (self.archive / "captures").glob("*/treatments.txt")
        self.assertIn(new_log, [path.read_bytes() for path in captures])

    def test_usb_import_routes_only_to_the_read_mode(self):
        def fake_usb(mode, runtime, port, capture, wake_seconds):
            self.assertEqual((mode, port, wake_seconds), ("import", "COM7", 60))
            (capture / "treatments.txt").write_bytes(DISPLAY)
            (capture / "treatment-count.txt").write_text("1\n", encoding="ascii")
            (capture / "clock-before.txt").write_bytes(
                b"Date Read - MON   2026-04-06\r\nTime Read - 07:13:00\r\n")
            (capture / "clock-before-host-ms.txt").write_text("1775459580000\n")
        with (
            patch.object(cli, "get_chart_renderer", return_value=fake_chart),
            patch.object(cli, "get_hug_runtime"),
            patch.object(cli, "run_usb", side_effect=fake_usb),
        ):
            self.assertEqual(self.run_usb_command("import"), 0)
        self.assertTrue((self.archive / cli.CSV_NAME).exists())
        self.assertIn("Approximate clock offset", self.output.getvalue())

    def test_sync_requires_separate_command_and_leaves_history_files_alone(self):
        csv_path = self.archive / cli.CSV_NAME
        chart_path = self.archive / cli.CHART_NAME
        csv_path.write_bytes(b"original CSV")
        chart_path.write_bytes(b"original chart")
        with (
            patch.object(cli, "get_chart_renderer") as chart,
            patch.object(cli, "get_hug_runtime"),
            patch.object(cli, "run_usb") as usb,
        ):
            self.assertEqual(self.run_usb_command("sync-clock"), 0)
        chart.assert_not_called()
        usb.assert_called_once()
        self.assertEqual(usb.call_args.args[0], "sync-clock")
        self.assertEqual(csv_path.read_bytes(), b"original CSV")
        self.assertEqual(chart_path.read_bytes(), b"original chart")
