"""Desktop interface for the local LH40 history and clock tool."""

from argparse import ArgumentTypeError
from dataclasses import asdict, dataclass, fields, replace
from datetime import datetime
import json
import os
from pathlib import Path
from queue import Empty, Queue
import subprocess
import sys
from tempfile import NamedTemporaryFile
from threading import Thread
import tkinter as tk
from tkinter import filedialog, font, messagebox, ttk

from lh40_archive import load_history
from lh40_history import CHART_NAME, CSV_NAME, archive_directory, port_name, run_operation


APP_NAME = "LH40 History"
WAKE_SECONDS = 90


def local_app_directory():
    if os.name == "nt":
        return Path(os.environ.get("LOCALAPPDATA", Path.home() / "AppData/Local")) / "Theradome/LH40-history"
    if sys.platform == "darwin":
        return Path.home() / "Library/Application Support/Theradome/LH40-history"
    return Path(os.environ.get("XDG_DATA_HOME", Path.home() / ".local/share")) / "lh40-history"


@dataclass(frozen=True)
class Settings:
    archive: str
    hug: str = ""
    jjs: str = ""
    port: str = ""


def load_settings(path):
    values = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    if not isinstance(values, dict):
        raise ValueError("Settings must contain app preferences.")
    defaults = Settings(str(local_app_directory()))
    return Settings(**{
        field.name: values[field.name]
        if isinstance(values.get(field.name), str) else getattr(defaults, field.name)
        for field in fields(Settings)
    })


def save_settings(path, settings):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary_path = None
    try:
        with NamedTemporaryFile(mode="w", encoding="utf-8", dir=path.parent,
                                prefix=".settings-", delete=False) as temporary:
            temporary_path = Path(temporary.name)
            json.dump(asdict(settings), temporary, indent=2)
            temporary.write("\n")
        os.replace(temporary_path, path)
    finally:
        if temporary_path is not None:
            temporary_path.unlink(missing_ok=True)


def available_ports():
    if os.name == "nt":
        import winreg

        try:
            key = winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, r"HARDWARE\DEVICEMAP\SERIALCOMM")
        except FileNotFoundError:
            return []
        with key:
            count = winreg.QueryInfoKey(key)[1]
            ports = [winreg.EnumValue(key, index) for index in range(count)]
        return sorted({value for name, value, _ in ports if "bthmodem" not in name.lower()})
    patterns = ("cu.usbserial*", "cu.usbmodem*") if sys.platform == "darwin" else (
        "serial/by-id/*", "ttyUSB*", "ttyACM*",
    )
    return [str(path) for pattern in patterns for path in sorted(Path("/dev").glob(pattern)) if path.exists()]


def open_chart_file(path):
    if os.name == "nt":
        os.startfile(path)
    else:
        program = "open" if sys.platform == "darwin" else "xdg-open"
        subprocess.Popen([program, str(path)], stdin=subprocess.DEVNULL,
                         stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


class DesktopApp:
    def __init__(self, root, settings_path=None):
        self.root = root
        self.settings_path = settings_path or (local_app_directory() / "desktop-settings.json")
        self.events = Queue()
        self.busy = False

        root.title(APP_NAME)
        root.minsize(560, 420)
        root.protocol("WM_DELETE_WINDOW", self.close)
        try:
            self.settings = load_settings(self.settings_path)
        except (OSError, ValueError) as error:
            self.settings = Settings(str(local_app_directory()))
            settings_error = f"Could not read saved settings. Review Settings. {error}"
        else:
            settings_error = ""

        self.port = tk.StringVar(value=self.settings.port)
        self.archive_text = tk.StringVar()
        self.latest_text = tk.StringVar()
        self.status = tk.StringVar(value=settings_error)
        self.build()
        self.refresh_ports()
        self.refresh_archive()
        if not self.status.get():
            self.status.set("Ready to import." if self.settings.hug else
                            "Choose your HUG installation in Settings to connect over USB.")

    def build(self):
        root = self.root
        style = ttk.Style(root)
        family = font.nametofont("TkDefaultFont").actual("family")
        style.configure("Title.TLabel", font=(family, 22, "bold"))
        style.configure("Archive.TLabel", font=(family, 16, "bold"))
        style.configure("Section.TLabel", font=(family, 10, "bold"))
        style.configure("Action.TButton", padding=(12, 8))
        page = ttk.Frame(root, padding=(24, 20))
        page.pack(fill="both", expand=True)
        page.columnconfigure(0, weight=1)

        header = ttk.Frame(page)
        header.grid(row=0, column=0, sticky="ew", pady=(0, 18))
        header.columnconfigure(0, weight=1)
        ttk.Label(header, text="Helmet history", style="Title.TLabel").grid(row=0, column=0, sticky="w")
        ttk.Label(header, text="Theradome EVO LH40").grid(row=1, column=0, sticky="w", pady=(2, 0))
        self.settings_button = ttk.Button(header, text="Settings…", command=self.edit_settings)
        self.settings_button.grid(row=0, column=1, sticky="e")

        connection = ttk.Frame(page)
        connection.grid(row=1, column=0, sticky="ew", pady=(0, 16))
        connection.columnconfigure(1, weight=1)
        ttk.Label(connection, text="USB port").grid(row=0, column=0, padx=(0, 10))
        self.port_box = ttk.Combobox(connection, textvariable=self.port, width=24)
        self.port_box.grid(row=0, column=1, sticky="ew")
        self.refresh_button = ttk.Button(connection, text="Refresh", command=self.refresh_ports)
        self.refresh_button.grid(row=0, column=2, padx=(8, 0))

        archive = ttk.Labelframe(page, text="Treatment archive", padding=(14, 10))
        archive.grid(row=2, column=0, sticky="ew")
        ttk.Label(archive, textvariable=self.archive_text, style="Archive.TLabel").grid(row=0, column=0, sticky="w")
        ttk.Label(archive, textvariable=self.latest_text).grid(row=1, column=0, sticky="w", pady=(2, 10))
        actions = ttk.Frame(archive)
        actions.grid(row=2, column=0, sticky="w")
        self.import_button = ttk.Button(actions, text="Import treatments", command=self.import_treatments,
                                        style="Action.TButton")
        self.import_button.pack(side="left")
        self.chart_button = ttk.Button(actions, text="Open chart", command=self.open_chart, style="Action.TButton")
        self.chart_button.pack(side="left", padx=(8, 0))

        clock = ttk.Frame(page)
        clock.grid(row=3, column=0, sticky="ew", pady=(16, 12))
        clock.columnconfigure(0, weight=1)
        ttk.Label(clock, text="Helmet clock", style="Section.TLabel").grid(row=0, column=0, sticky="w")
        ttk.Label(clock, text="Set the helmet to this laptop’s local time.").grid(row=1, column=0, sticky="w")
        self.sync_button = ttk.Button(clock, text="Sync clock…", command=self.sync_clock)
        self.sync_button.grid(row=0, column=1, rowspan=2, sticky="e")

        ttk.Separator(page).grid(row=4, column=0, sticky="ew", pady=(0, 8))
        self.status_label = ttk.Label(page, textvariable=self.status, justify="left", wraplength=490)
        self.status_label.grid(row=5, column=0, sticky="w")
        page.bind("<Configure>", lambda event: self.status_label.configure(wraplength=max(250, event.width - 52)))
        self.activity = ttk.Progressbar(page, mode="indeterminate")
        self.activity.grid(row=6, column=0, sticky="ew", pady=(8, 0))
        self.activity.grid_remove()

    def refresh_ports(self):
        try:
            choices = available_ports()
        except OSError as error:
            self.status.set(f"Could not list USB ports. {error}")
            return
        self.port_box.configure(values=choices)
        if choices and (not self.port.get() or (self.port.get() not in choices and len(choices) == 1)):
            self.port.set(choices[0])

    def refresh_archive(self):
        archive = Path(self.settings.archive).expanduser()
        try:
            rows = load_history(archive / CSV_NAME)
        except (OSError, ValueError) as error:
            self.archive_text.set("Archive unavailable")
            self.latest_text.set("")
            self.status.set(str(error))
        else:
            count = len(rows)
            self.archive_text.set(f"{count} treatment{'s' if count != 1 else ''} archived")
            self.latest_text.set(f"Last completed {rows[-1].finished_at_helmet_local:%b %d, %Y at %H:%M}"
                                 if rows else "Import treatments to start your archive.")
        self.chart_button.state(["!disabled" if (archive / CHART_NAME).is_file() else "disabled"])

    def edit_settings(self):
        if self.busy:
            return
        dialog = tk.Toplevel(self.root)
        dialog.title("Settings")
        dialog.transient(self.root)
        dialog.resizable(True, False)
        frame = ttk.Frame(dialog, padding=16)
        frame.pack(fill="both", expand=True)
        frame.columnconfigure(1, weight=1)
        archive = tk.StringVar(dialog, value=self.settings.archive)
        hug = tk.StringVar(dialog, value=self.settings.hug)
        jjs = tk.StringVar(dialog, value=self.settings.jjs)

        def add_path(row, label, variable, select):
            ttk.Label(frame, text=label).grid(row=row, column=0, sticky="w", pady=5, padx=(0, 10))
            ttk.Entry(frame, textvariable=variable, width=37).grid(row=row, column=1, sticky="ew", padx=(0, 8))

            def browse():
                path = select(parent=dialog, title=label)
                if path:
                    variable.set(path)
            ttk.Button(frame, text="Browse…", command=browse).grid(row=row, column=2)

        add_path(0, "Archive folder", archive, filedialog.askdirectory)
        if os.name == "nt":
            add_path(1, "Original HUG folder", hug, filedialog.askdirectory)
        else:
            add_path(1, "Original HUG myapp.jar", hug, filedialog.askopenfilename)
            add_path(2, "Java 8 jjs (optional)", jjs, filedialog.askopenfilename)
        ttk.Label(frame, text="The CSV, chart, and raw USB replies are saved in the archive folder.",
                  wraplength=490).grid(row=3, column=0, columnspan=3, sticky="w", pady=(10, 14))

        buttons = ttk.Frame(frame)
        buttons.grid(row=4, column=0, columnspan=3, sticky="e")

        def save():
            if not archive.get().strip():
                messagebox.showerror(APP_NAME, "Choose an archive folder.", parent=dialog)
                return
            try:
                folder = archive_directory(Path(archive.get().strip()))
                settings = Settings(str(folder), hug.get().strip(), jjs.get().strip(), self.port.get().strip())
                save_settings(self.settings_path, settings)
            except (OSError, ValueError) as error:
                messagebox.showerror(APP_NAME, str(error), parent=dialog)
                return
            self.settings = settings
            dialog.destroy()
            self.status.set("Settings saved.")
            self.refresh_archive()

        ttk.Button(buttons, text="Cancel", command=dialog.destroy).pack(side="left", padx=(0, 8))
        ttk.Button(buttons, text="Save", command=save).pack(side="left")
        dialog.protocol("WM_DELETE_WINDOW", dialog.destroy)
        dialog.bind("<Escape>", lambda _event: dialog.destroy())
        dialog.grab_set()
        dialog.focus_set()

    def open_chart(self):
        path = Path(self.settings.archive).expanduser().resolve() / CHART_NAME
        try:
            if not path.is_file():
                raise FileNotFoundError("The chart is missing. Import treatments to regenerate it.")
            open_chart_file(path)
        except OSError as error:
            messagebox.showerror(APP_NAME, str(error), parent=self.root)

    def import_treatments(self):
        self.start_operation("import")

    def sync_clock(self):
        if self.busy:
            return
        now = datetime.now().astimezone()
        if messagebox.askyesno(
            "Sync helmet clock?",
            f"Set the helmet to this laptop’s clock?\n\n{now:%b %d, %Y  %H:%M:%S %Z}",
            parent=self.root, default="no",
        ):
            self.start_operation("sync-clock")

    def start_operation(self, mode):
        if self.busy:
            return
        if not self.settings.hug:
            self.status.set("Choose the original HUG application in Settings.")
            return
        try:
            port = port_name(self.port.get().strip())
            settings = replace(self.settings, port=port)
            save_settings(self.settings_path, settings)
        except (OSError, ValueError, ArgumentTypeError) as error:
            self.status.set(str(error))
            return

        self.settings = settings
        self.set_busy(True)
        self.status.set("Connecting to the helmet. Keep it off your head during USB operations.")
        Thread(target=self.run_in_background, args=(mode, settings)).start()
        self.root.after(50, self.poll_operation)

    def run_in_background(self, mode, settings):
        try:
            result = run_operation(
                mode, Path(settings.archive), Path(settings.hug), settings.port, WAKE_SECONDS,
                jjs_path=Path(settings.jjs) if settings.jjs else None,
                progress=lambda message: self.events.put(("progress", message)),
            )
        except Exception as error:
            self.events.put(("error", str(error)))
        else:
            self.events.put(("done", result))

    def set_busy(self, busy):
        self.busy = busy
        state = "disabled" if busy else "!disabled"
        for control in (self.import_button, self.sync_button, self.settings_button, self.refresh_button, self.port_box):
            control.state([state])
        if busy:
            self.chart_button.state(["disabled"])
            self.activity.grid()
            self.activity.start(12)
        else:
            self.activity.stop()
            self.activity.grid_remove()

    def poll_operation(self):
        while True:
            try:
                kind, payload = self.events.get_nowait()
            except Empty:
                break
            if kind == "progress":
                if payload.startswith("Hold the master switch"):
                    self.status.set("With the helmet off your head, " + payload[0].lower() + payload[1:])
                elif payload.startswith("Board Test connected"):
                    self.status.set("Helmet connected. Reading the response…")
            else:
                self.set_busy(False)
                self.refresh_archive()
                if kind == "error":
                    self.status.set(payload)
                elif payload is None:
                    self.status.set("Clock synced and verified against the laptop.")
                elif payload.added_count:
                    count = payload.added_count
                    self.status.set(f"Imported {count} new treatment{'s' if count != 1 else ''}. Chart updated.")
                else:
                    self.status.set("Archive is up to date. Chart updated.")
                return
        self.root.after(50, self.poll_operation)

    def close(self):
        if self.busy:
            self.status.set("Wait for the current USB operation to finish before closing.")
        else:
            self.root.destroy()


def main():
    root = tk.Tk()
    DesktopApp(root)
    root.mainloop()
    return 0


if __name__ == "__main__":
    sys.exit(main())
