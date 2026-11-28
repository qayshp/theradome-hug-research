# Save LH40 treatment history and check the clock

LH40 History reads the helmet’s treatment log into a local CSV and redraws the battery, temperature and recorded laser-current chart. It has a desktop window and a command-line interface. The USB path targets the **EVO LH40** with application revision **126** and bootloader revision **6**.

## Desktop app

### Windows download

1. Download the LH40 History Windows ZIP from GitHub Releases. Choose **x64** for a 64-bit Intel or AMD PC or **ARM64** for Windows 11 on Arm. **Settings > System > About** shows your system type. [GitHub Releases](https://github.com/qayshp/theradome-hug-research/releases)
2. In File Explorer, use **Extract All** and open `LH40 History.exe` inside the extracted folder. Keep the whole folder together. The ZIP includes Python, the charting library and a `START HERE.txt` guide.
3. For USB, install Theradome’s original Windows HUG package if needed. Its Java runtime is included with HUG. LH40 History checks its standard install locations, including `C:\Program Files (x86)\HugApplication`. For a custom installation, use **Settings > Original HUG folder > Browse** and select the folder containing `HugUpgrade.exe` and `jre\bin\jjs.exe`. Close HUG before importing. [Windows HUG download](https://apps.theradome.com/downloads/HugApplication.exe) · [Installer fingerprint](discovery.md)
4. Connect the helmet with a USB data cable and choose its COM number. If it does not appear as a USB Serial Port in Device Manager, check the FTDI driver for your Windows architecture. [USB driver setup](usb-clock-details.md#windows-arm64-driver-setup) · [FTDI drivers](https://ftdichip.com/drivers/vcp-drivers/)

### Run from source

Use Python 3.10 or newer with Tk and install Matplotlib once with `python -m pip install "matplotlib>=3.6"`. Open the complete repository folder so the launcher can find its supporting files. [Python for Windows](https://www.python.org/downloads/windows/) · [Tk setup](https://docs.python.org/3/library/tkinter.html)

- **Windows.** Open `Open LH40 History.pyw` from File Explorer using the `pythonw.exe` from your Python installation or environment. Configure HUG and the COM port as described above.
- **macOS and Linux.** Run `python3 tools/lh40_desktop.py`. In **Settings**, choose `Resources/myapp.jar` from the original Mac HUG archive and choose `bin/jjs` from a Java 8 JDK if it is outside your PATH. On macOS, select the helmet’s `/dev/cu.usbserial…` port. On Linux, select its `/dev/ttyUSB…` or `/dev/serial/by-id/…` port and check that your account can access it. The archived HUG library has Intel-only macOS serial code. An Apple Silicon Mac would need an Intel Java runtime under Rosetta where available. Physical USB testing has been done on Windows. [Mac HUG download](https://apps.theradome.com/downloads/HugApplication-V200.zip) · [Java 8 jjs](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/jjs.html) · [Apple’s Rosetta notes](https://support.apple.com/en-us/102527)

In **Settings**, choose the folder that already contains `treatments.csv` to continue an existing archive. The CSV, chart, and raw captures stay in that folder.

1. Connect the charged helmet with a USB data cable, keep it off your head, and choose **Import treatments**. If prompted, hold the master switch until the helmet speaks its revision, then release it. The window reports when the import is complete.
2. Choose **Open chart** to view the updated chart in your usual image viewer. This also works with the helmet disconnected.
3. Choose **Sync clock…** only when you want to set the helmet’s clock. The window shows the laptop’s local time and asks you to confirm. The tool reads the helmet clock back after setting it.

## Command-line setup

The examples below use Windows PowerShell. Close HUG and other programs using the helmet’s port. Set the HUG folder and archive path as described above. The tool checks that the launcher matches the archived HUG build. [Package fingerprints](discovery.md)

```powershell
$hugDir = 'C:\path\to\installed\HUG'
$historyDir = Join-Path $env:LOCALAPPDATA 'Theradome\LH40-history'
$port = 'COM7'
```

On macOS or Linux, use `--hug-jar /path/to/Resources/myapp.jar` in place of `--hug-dir` and pass the USB device path to `--port`. Add `--jjs /path/to/jdk/bin/jjs` if the Java 8 executable is outside your PATH.

## Import over USB

Connect the charged helmet to the computer with a USB data cable. Keep it off your head and pointed away from eyes. Start the command. If it asks for the master switch, hold the switch until the helmet speaks its revision, then release it. Keep USB connected while the tool reads the results and closes the session. [How the switch opens the USB receive window](usb-clock-details.md#what-the-com7-sessions-showed)

```powershell
python tools/lh40_history.py import --archive $historyDir --hug-dir $hugDir --port $port
```

The import reads the diagnostic parameters, clock and completed-treatment display. It saves the raw replies in a uniquely named folder under `captures` and reports the clock offset. It checks the completed-treatment counter against the full display before merging any rows. Every run reports the count of new and archived treatments.

| Local file | Contents |
| --- | --- |
| `treatments.csv` | The accumulated history, including the helmet’s original local timestamps. |
| `treatment-history.png` | Battery voltage, temperature and recorded current plotted by treatment number. |
| `captures` | Raw diagnostic replies, including dates and potentially the device identifier. |

Keep the archive folder outside your repository. Use one folder for each helmet. If the clock has been corrected, earlier treatment timestamps retain the values stored at the time. The archive is sorted by treatment number so a clock correction does not move records out of sequence.

The helmet retains the latest **50 completed treatments**. Import periodically to build a longer history. Overlapping reads merge by treatment number, and repeated imports add zero duplicates. The tool reports missing ranges, such as older records from before your first import. If the same number has different data, the tool stops and keeps the raw capture and existing CSV. [Treatment log format and retention](../model/eeprom.md) · [How to interpret the measurements](treatment-analysis.md)

## Replay a saved capture

Use `import-log` to reprocess a complete treatment display without connecting the helmet. From a tool-created capture folder, use its `treatments.txt` and the value in `treatment-count.txt`. For a saved display of all 50 rows, you can omit `--completed-count`. If a capture ended early, collect it again over USB.

```powershell
python tools/lh40_history.py import-log --archive $historyDir --completed-count 244 'C:\path\to\treatments.txt'
```

Replaying a complete capture also rebuilds the chart. If the CSV or chart is open in another program and the update fails, close it and replay the saved capture.

## Set the clock explicitly

Run `sync-clock` when you want to copy the laptop’s local date and time to the helmet. Check the laptop’s date, time and time zone first. Use the same USB and switch sequence as for an import.

```powershell
python tools/lh40_history.py sync-clock --archive $historyDir --hug-dir $hugDir --port $port
```

The command saves the before and after replies locally. It requires the helmet’s `Date Set` acknowledgement and verifies the result with independent clock reads. If a response is lost after the time is sent, use an import to check the actual clock before deciding whether to sync again. [Clock read, write and verification details](usb-clock-details.md#notes-for-a-clock-sync-tool)

## Verification

Run the archive and simulated USB checks from the repository root. The archive checks cover merging overlapping reads, counter conflicts and gaps, incomplete reports, and preserving the CSV if chart rendering fails. The USB checks cover the exact commands sent during import and explicit sync, as well as missing responses, unsupported revisions, and clock readbacks.

```powershell
python -B -m unittest discover -s tests -p 'test_lh40_*.py'
node tests/test_lh40_usb.js
```

On a desktop with Tk and Matplotlib, `python -B tests/desktop_smoke.py` exercises the window using a temporary archive and a simulated helmet. It checks that importing generates the chart and that declining the separate clock confirmation sends no sync request.

The Windows Start menu launch was verified with the existing archive. A Linux ARM64 test used a simulated serial port with the original HUG JAR and Java 8. It verified import through capture, CSV and chart generation, plus a separate clock sync through two advancing readbacks.

In a command-line test on the physical LH40, the tool read the clock and treatment log over USB, added one new treatment to the existing CSV, and regenerated the chart. The simulated cases cover the terminal-clear sequences that preceded the live clock report.

A separate command-line run verified `sync-clock`. The helmet was about 11 seconds ahead, acknowledged the write with `Date Set`, and then returned two readings that advanced by two seconds and matched the laptop’s displayed second. [Clock protocol and earlier hardware check](verification.md#live-evo-lh40-tests)

### Live Windows app tests

On November 28, 2026, both tests ran from the LH40 History Start menu shortcut on Windows ARM64 with an EVO LH40 at application revision 126 and bootloader revision 6 on COM7.

| App action | Observed result |
| --- | --- |
| **Import treatments** and **Open chart** | The app reused the existing diagnostics session and read all 50 retained treatments. The helmet’s completed-treatment count matched the newest archived record, and the CSV still contained 53 unique treatments after import. The app saved the raw replies locally, regenerated `treatment-history.png`, and displayed it in Windows Photos when **Open chart** was selected. The USB import was read-only on the helmet. |
| **Sync clock…** | The app showed the laptop’s local time, asked for confirmation, and then prompted for a master-switch hold. The helmet acknowledged the write with `Date Set`. Two separate USB readbacks advanced by two seconds and each matched the laptop’s displayed second. The app displayed “Clock synced and verified against the laptop.” |

The raw replies and personal treatment timestamps are stored in the local archive.

## Build the Windows download

The packaging workflow builds the portable ZIPs on separate x64 and ARM64 Windows runners using Python 3.13.17. It runs the history and desktop tests, launches the executable extracted from each ZIP in self-test mode, and uploads each ZIP with its SHA-256. The self-test exercises the bundled desktop, chart renderer and USB script without opening a serial port. [Windows packaging workflow](../.github/workflows/desktop-windows.yml)
