# Theradome LH40 tools and firmware research

This repository contains the LH40 History app, a firmware volume fix, and research on Theradome’s HUG updater and helmet firmware.

<a name="save-treatment-history-and-check-the-clock"></a>

## LH40 History app

Import completed treatments over USB, save the raw replies locally, and update a CSV and chart. **Sync clock** asks for separate confirmation. LH40 History works with the helmet’s existing firmware. The Windows download includes Python.

- [Download LH40 History for Windows](https://github.com/qayshp/theradome-hug-research/releases/tag/lh40-history-v1.0.0) for x64 or ARM64.
- [Set up HUG and use the app](docs/laptop-history.md#windows-download) with the Windows download, or use the source and command-line instructions in the same guide.

<a name="build-flash-and-return-to-normal-use"></a>

## Firmware volume fix

The fix saves the chosen volume and uses it for ordinary announcements, including the treatment count. Alerts that block or stop treatment still play at least at Soft. [Volume behavior and test results](docs/volume-settings.md)

One **EVO LH40** reporting application revision **126** and bootloader revision **6** completed calibration and a full treatment with the tested volume image. Compatibility with other helmets remains unverified. [Live results](docs/volume-settings.md#live-count-readout-update) · [How to identify the device](docs/device.md)

- [Download the tested volume fix](https://github.com/qayshp/theradome-hug-research/releases/tag/lh40-volume-v1.0.0) with its HEX and checksum.
- [Install the volume fix on Windows](docs/custom-firmware.md) covers the model check, HUG preparation, and verification after updating.
- [Recover after a successful transfer](docs/board-test-recovery.md#recover-after-a-successful-transfer) covers calibration prompts and what to do if the helmet stays in Board Test.
- [Build or edit the firmware from source](docs/custom-firmware.md#rebuild-a-reference-image) covers the stock rebuild, volume checks, and a small assembly example.

## GitHub Actions builds

- **LH40 History.** Pull requests that affect the app or its packaging build and test the x64 and ARM64 Windows packages before merge. [Windows workflow](.github/workflows/desktop-windows.yml) · [Build instructions](docs/laptop-history.md#build-the-windows-download)
- **Firmware.** Successful builds on `main` attach `volume-settings.hex` and `SHA256SUMS`. A manual run can also verify bundle preparation using Theradome’s installer. [Firmware workflow](.github/workflows/firmware.yml) · [Tagged firmware release instructions](docs/custom-firmware.md#prepare-a-tagged-release)

## Follow the investigation

The models and annotated assembly use the LH80-labeled application distributed with HUG. The live EVO LH40 also reported an LH80 diagnostic label. [USB and hardware evidence](docs/device.md) · [Application image](docs/firmware.md)

| Question | Evidence |
| --- | --- |
| Where did the installers and firmware come from? | [Discovery log](docs/discovery.md) with vendor download links, the archived landing page, unsuccessful searches, the URL recovered from the Java app, and independent copies of the firmware. |
| What does HUG actually run and which firmware file does it use? | [Java application analysis and source index](docs/java.md). The app downloads one HEX to disk and its programming routine opens a bundled resource. The copies examined had identical contents. |
| What exactly is in the HEX? | [Firmware inventory](docs/firmware.md), [original Intel HEX](outputs/myTheradome-LH80-labeled.hex), [decoded application binary](outputs/myTheradome-LH80-labeled.bin), and [rebuildable PIC18 assembly](model/rebuild.asm). |
| Where are the firmware subsystems and unresolved regions? | [Address index and subsystem maps](model/firmware-index.md) and [annotated PIC18 assembly](model/rebuild.asm). |
| What does a press, light change, or voice announcement mean? | [Observation-to-state guide](model/README.md), [button and charger inputs](model/inputs.md), [charging-light waveform](model/indicator.md), and [voice behavior](model/volume.md). |
| How does the firmware assemble spoken announcements? | [Voice clip directory, bank addresses, and spoken numbers](docs/voice-clips.md). |
| What survives power loss? | [EEPROM storage map](model/eeprom.md) with the completed-session counter, 50-entry history, and bytewise write order. The stock volume selection lives in RAM. The [volume overlay](docs/volume-settings.md) saves it in EEPROM. |
| How closely does the reconstruction match? | [Build and verification](model/rebuild.md). The recovered assembly reproduces all 50,816 populated bytes. Host-side models also run individual paths against instructions read from the original HEX. |
| What worked on a physical LH40, and what would recovery require? | [USB, clock, offline patch and recovery results](docs/verification.md). |
| How did COM7, the clock and the alarms behave? | [Windows ARM64 setup, serial observations, and clock and alarm details](docs/usb-clock-details.md). |
| What did the 50 treatment records show? | [Treatment record format, battery and current plots, and feedback analysis](docs/treatment-analysis.md). |
| How does the volume patch handle prompts, low-battery warnings and faults? | [Experimental PIC18 volume settings overlay, build recipe and LH40 test results](docs/volume-settings.md). |
| How do I enter or exit diagnostics and finish a successful update? | [Board Test and post-flash recovery](docs/board-test-recovery.md). |
| What evidence supports the archive search and firmware tests? | [Search reproducibility and firmware test evidence](docs/research-evidence.md). |

## Try it offline

From the repository root with Python 3, list the states that request the charging fade, trace what happens when the active-session timer expires, or run the state checks.

```sh
python model/state_paths.py --pattern fade --channel both
python model/session_trace.py 7 --expired
python model/check_states.py
```

The first command returns states `0x02` and `0x12`. Both keep green steady while varying red. Going from that fade to sustained green on the charger fits the normal charge-complete transition. The second command follows state `0x07` to `0x0C`, where the next periodic pass logs the completed session. These commands read the archived image and run on a computer without a connected helmet. [Indicator trace](model/indicator.md) · [Session and EEPROM trace](model/eeprom.md)

If you have the original Mac ZIP, run `python tools/verify_packages.py HugApplication-V200.zip` to compare its embedded firmware with this repository. Supply `--windows-installer` with the downloaded EXE to check its hash, or `--windows-launcher` with the extracted `HugUpgrade.exe` to compare every JAR member across platforms. [Package verifier](tools/verify_packages.py)

## Attribution

Theradome’s HUG updater and the archived firmware originate from the HUG distribution. The Java text was recovered with a decompiler. The LH40 History app, assembly, behavioral models, tests, and research notes were prepared for this project. The upstream firmware repository lists no license for the image. No reuse license is asserted here on behalf of the original authors. [Upstream firmware repository](https://github.com/renuraja03/fwupdate)
