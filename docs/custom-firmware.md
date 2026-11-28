# Install or build the Theradome volume fix

The Windows steps below start with a downloaded firmware HEX. To assemble or edit it yourself, use the [source build](#rebuild-a-reference-image). Get a data-capable USB cable and [Python 3 for Windows](https://www.python.org/downloads/windows/). If HUG is missing, install it from [Theradome's Windows package](https://apps.theradome.com/downloads/HugApplication.exe) before starting.

## Check your helmet

The tested unit was one **Theradome EVO LH40** with application revision **126** and bootloader revision **6**, spoken together as **126.6**. It completed calibration, played the short-press treatment count quietly on USB, and later completed a full treatment without issues reported by its owner. The LH80 diagnostic label can appear on an LH40. Use the physical model label, and treat other hardware or revisions as untested. Even a matching revision is only one compatibility check. [Live results](volume-settings.md#live-count-readout-update) · [Device evidence](device.md)

Charge the helmet and connect it to Windows with the USB data cable. Look for its **USB Serial Port** in Device Manager. With the charged helmet connected, off your head, and pointed away from eyes, hold the switch until it speaks the revision, then release it. If you already have Board Test open, you can read both revisions and save a settings baseline with `D` before [exiting the menu](board-test-recovery.md#exit-board-test). Keep the stock HUG installation for a possible application reinstall. [USB driver setup](usb-clock-details.md#windows-arm64-driver-setup) · [Backup and recovery limits](verification.md#backup-and-recovery)

## Download and verify the HEX

1. Open the [LH40 volume fix release](https://github.com/qayshp/theradome-hug-research/releases/tag/lh40-volume-v1.0.0). Under **Assets**, download `volume-settings.hex` and `SHA256SUMS` into the same folder. The public release downloads are available without a GitHub account.
2. Download **Source code (zip)** from the same release and extract it into a separate folder. That folder contains the Windows preparation tool. It also contains the [source to rebuild the HEX](#rebuild-a-reference-image). [Build that produced the release HEX](https://github.com/qayshp/theradome-hug-research/actions/runs/46120466532)
3. In PowerShell, set `$buildDir` to the folder holding the HEX and checksum. This checks both the downloaded checksum and the hash of the exact HEX tested on the helmet. Keep the window open for the next step.

```powershell
$buildDir = 'C:\path\to\downloaded\firmware'
$hex = Join-Path $buildDir 'volume-settings.hex'
$expected = ((Get-Content -LiteralPath (Join-Path $buildDir 'SHA256SUMS') -TotalCount 1) -split '\s+')[0]
$actual = (Get-FileHash -LiteralPath $hex -Algorithm SHA256).Hash
if ($actual -ne $expected) { throw 'The HEX does not match SHA256SUMS.' }
$tested = 'f8b3a0e4828560ea2ca38e48c1a8ebf2de6105dc551f8d9a9d3378a58bf03396'
if ($actual -ne $tested) { throw 'This HEX differs from the image tested on the LH40.' }
$actual
```

For a local rebuild, compare the hash printed by the builder directly with `$tested` above, then continue to HUG preparation. The hardware results apply to that exact image. [Tested build and digest](volume-settings.md#button-requested-count-follow-up)

## Prepare HUG on Windows

Find the folder containing the installed **`HugUpgrade.exe`** and its adjacent **`jre`** folder. The preparation tool takes that launcher as input. Replace the example paths with your HUG and extracted repository folders. For a locally built image, set `$buildDir` to that build folder and `$hex` to its verified HEX. Choose a fresh JAR filename each time. [Installer fingerprints](discovery.md) · [Preparation tool](../tools/prepare_hug_update.py)

```powershell
$hugDir = 'C:\path\to\installed\HUG'
Set-Location -LiteralPath 'C:\path\to\extracted\repository'
$bundle = Join-Path $buildDir 'HugLocal.jar'
python tools/prepare_hug_update.py (Join-Path $hugDir 'HugUpgrade.exe') $hex $bundle
```

The tool verifies the known launcher and HUG record layout, then writes a separate local JAR. Its output includes the HEX digest and the application address range. For the tested volume image, expect `0x1900–0xE03F` and the digest above. If your system runs Python 3 with `py -3`, use that in place of `python`. [Preparation tool](../tools/prepare_hug_update.py) · [Record coverage](volume-settings.md#button-requested-count-follow-up)

## Flash from the prepared HUG bundle

Close other programs using the serial port. Use the bundled Java runtime to launch the prepared JAR from PowerShell. Leave the window open for the loader's response.

```powershell
& (Join-Path $hugDir 'jre\bin\java.exe') -jar $bundle
```

The live transfer used HUG's Windows programming worker from a checked runner. The prepared JAR and its resource were verified offline. The following GUI flow comes from the archived code, and a connected-device transfer through that GUI remains untested. [HUG analysis](java.md) · [Physical transfer results](volume-settings.md#live-count-readout-update)

1. **Connect in HUG.** Follow the [cable and button sequence](board-test-recovery.md#recognize-and-enter-board-test). When HUG says **Theradome connected**, click **Next** again to read the device version and open the upgrade menu. [Version read](../java/com/screens/Connected.java#L46-L63)
2. **Confirm the upgrade.** Open **Helmet Upgrade**. Check that you are using the prepared JAR for the intended unit before choosing **Yes**. The GUI needs the vendor's online version response and can offer **Yes** even when it says the firmware is up to date. The writer uses the HEX inside the prepared JAR. The patch keeps the spoken revision at **126.6**. [Version prompt](../java/com/screens/FirmwareUpdateMenu.java#L196-L262) · [Image selection](../java/com/screens/FirmwareUpdateProgram.java#L116-L128)
3. **Wait for the loader result.** Leave USB connected during programming. The verified result was `resp rcvdSuccess` followed by `completed write 3` in the console. The GUI completion screen also opens for status `5`, an end-of-transfer fallback based on progress. Check the console for the loader's actual reply. A missing success reply leaves the result uncertain. See the [failed-transfer limits](verification.md#backup-and-recovery) in that case. [Worker fallback](../java/SerialCommunication/Communication.java#L873-L881) · [Completion screen conditions](../java/com/screens/FirmwareUpdateProgram.java#L78-L89)
4. **Finish startup after confirmed success.** In HUG, click **Close** on the completion screen and let its follow-up finish. Then use the [post-flash checks](board-test-recovery.md#recover-after-a-successful-transfer) to distinguish normal operation, calibration, and Board Test. Once the helmet is operating normally, a short press while plugged in can check the treatment-count volume. [Stock completion workflow](board-test-recovery.md#what-stock-hug-does)

The stock image remains available for an application-level reinstall if the helmet can still enter HUG's normal update flow. There is no verified flash readback, stock rollback, or interrupted-update recovery procedure for the tested helmet. The archived application also omits the bootloader, device-specific EEPROM, and voice data. [Backup and recovery](verification.md#backup-and-recovery)

## Rebuild a reference image

For a source build, use Python 3 and gputils in Linux or WSL. The reference tests used `gpasm` 1.4.0. From the repository root, run the stock check first.

```sh
sh model/rebuild.sh
```

A passing result reports all **50,816** application bytes matching the archived HUG image. The source uses the `18f66k22` assembler profile. [Stock rebuild and digests](../model/rebuild.md) · [Address index and hardware assumptions](../model/firmware-index.md)

## Build the volume change or make a small edit

Build the volume overlay in a separate directory. For a checkout on a Windows-mounted drive under WSL, this example produces a HEX that the Windows tools can access.

```sh
build_dir=$(mktemp -d "$PWD/../theradome-build.XXXXXX")
python3 -B -m model.tools.build_volume_settings "$build_dir/volume-settings.hex"
python3 -B model/check_volume_settings.py "$build_dir/volume-settings.hex"
```

The tested volume image was built from `cbb99db` and contains **797 data rows** through `0xE03F`. A full comparison with the earlier warning build passed 9,984 modeled cases. Commit `a99c969` reproduces the earlier 798-row image, which extends one row past the preparer's supported range. Commit `35f7ac3` reproduces the expanded treatment-prompt image. Commit `9945182` reproduces the first installed volume image. [Count build and checks](volume-settings.md#button-requested-count-follow-up) · [Reproduce the comparison](volume-settings.md#reproduce-the-direct-comparison)

For a same-size edit, work in a separate checkout. One offline example changes `0x37` to `0x57` in the hexadecimal-digit formatter at `0xDED8`, so it uses lowercase letters. From that checkout, assemble, pack, and inspect the edit.

```sh
build_dir=$(mktemp -d "$PWD/../theradome-build.XXXXXX")
gpasm -o "$build_dir/edited.hex" model/rebuild.asm
python3 -B model/experiments/requested-volume/pack-hex.py . \
  "$build_dir/edited.hex" "$build_dir/custom.hex"
python3 -B model/tools/rebuild.py diff "$build_dir/custom.hex"
```

The expected difference in this example is a single byte at `0xDED8`. The packer requires the original populated address map. Review the instruction and address differences for your edit and test the changed behavior separately. The hardware results above cover the specified volume image. [Example packer](../model/experiments/requested-volume/README.md) · [Annotated formatter](../model/rebuild.asm#L22490)
