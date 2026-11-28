# Save LH40 treatment history and check the clock

The Windows tool reads the helmet’s treatment log into a local CSV and redraws the battery, temperature and recorded laser-current chart. When you explicitly run **`sync-clock`**, it sets the helmet to the laptop’s local date and time and reads the result back.

## Set up once

1. Use Python 3.10 or newer and install the chart library with `python -m pip install "matplotlib>=3.6"`. [Python for Windows](https://www.python.org/downloads/windows/)
2. Use the original [Theradome Windows HUG package](https://apps.theradome.com/downloads/HugApplication.exe). Locate the folder containing `HugUpgrade.exe` and `jre\bin\jjs.exe`. The tool checks that the launcher matches the [archived HUG build](discovery.md).
3. In PowerShell, open this repository folder. Set these paths for your computer. Use the folder that already contains `treatments.csv` if you have an existing archive. Close HUG and any other program using the helmet’s port.

```powershell
$hugDir = 'C:\path\to\installed\HUG'
$historyDir = Join-Path $env:LOCALAPPDATA 'Theradome\LH40-history'
$port = 'COM7'
```

The USB path targets the **EVO LH40** with application revision **126** and bootloader revision **6**. In Device Manager, use the COM number next to the helmet’s **USB Serial Port**. Bluetooth ports are separate. [Driver setup and USB observations](usb-clock-details.md)

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

The saved USB display and existing CSV were also replayed with the actual chart renderer. The clock protocol was previously exercised on the [physical LH40](verification.md#live-evo-lh40-tests). The new wrapper’s serial sequence is tested offline.
