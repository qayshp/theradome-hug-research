# Build, flash and check a local application image with HUG

The editable [PIC18 assembly](../model/rebuild.asm) reconstructs the application shipped with HUG. This guide starts with a byte-identical rebuild, then builds either the published volume change or a small edit of your own. A Windows preparation tool takes a local HEX and the owner's original installed HUG launcher and writes a separate JAR for HUG's normal update interface.

The volume images were tested on one EVO LH40 reporting application revision **126** and bootloader revision **6**. A checked local runner used the prepared bundle, the vendor's Java 8 runtime, and HUG's Windows programming worker. The loader accepted all 797 records of the short-press count update at `cbb99db`. The helmet completed calibration and the owner confirmed that the plugged-in count played quietly. The GUI steps below follow the archived application code. A connected-device update through the prepared JAR's graphical interface remains untested. [Live count-readout update](volume-settings.md#live-count-readout-update) · [Post-flash recovery](board-test-recovery.md#recover-after-a-successful-transfer)

The LH80 diagnostic label can appear on an LH40, and a revision match alone cannot establish that another helmet has the same board. [Expanded update](volume-settings.md#expanded-treatment-prompt-update) · [Original physical test](volume-settings.md#live-evo-lh40-test-on-november-26-2026) · [Controller evidence](device.md)

## Rebuild a reference image

Use Python 3 and gputils in Linux or WSL. The reference tests used `gpasm` 1.4.0. In a checkout of this repository, run the stock verification from the root.

```sh
sh model/rebuild.sh
```

A passing result reports all **50,816** application bytes matching the archived HUG image. The source uses the `18f66k22` assembler profile. See the [firmware address index](../model/firmware-index.md) for the instruction layout, annotated labels, and hardware assumptions. [Stock rebuild and digests](../model/rebuild.md)

## Build the volume change or make a small edit

Keep your output in a separate directory that Windows can read if you are building under WSL. For a checkout on a Windows-mounted drive, this example creates it beside the checkout.

```sh
build_dir=$(mktemp -d "$PWD/../theradome-build.XXXXXX")
python3 -B -m model.tools.build_volume_settings "$build_dir/volume-settings.hex"
python3 -B model/check_volume_settings.py "$build_dir/volume-settings.hex"
```

These commands build and verify the volume overlay, including the short-press count change. The tested build at `cbb99db` has **797 data rows** covering `0x1900–0xE03F` and SHA-256 `f8b3a0e4828560ea2ca38e48c1a8ebf2de6105dc551f8d9a9d3378a58bf03396`. The full verification run passed 9,984 modeled cases, including a direct comparison with the earlier warning build. Commit `a99c969` reproduces that 798-row build, which extends one row past the preparer's supported range. Commit `35f7ac3` reproduces the earlier 797-row expanded treatment-prompt image. Commit `9945182` reproduces the first installed volume image. [Count build and checks](volume-settings.md#button-requested-count-follow-up) · [Reproduce the comparison](volume-settings.md#reproduce-the-direct-comparison)

For a small same-size rewrite, edit `model/rebuild.asm` in a separate checkout. An offline example is the immediate operand in the hexadecimal-digit formatter at application byte address `0xDED8`. Changing `0x37` to `0x57` there changes the alphabetic digits in that formatter from uppercase to lowercase. This example changes display formatting and preserves the application address layout. From that checkout, assemble the edit and encode the result using the existing HUG-layout packer.

```sh
build_dir=$(mktemp -d "$PWD/../theradome-build.XXXXXX")
gpasm -o "$build_dir/edited.hex" model/rebuild.asm
python3 -B model/experiments/requested-volume/pack-hex.py . \
  "$build_dir/edited.hex" "$build_dir/custom.hex"
python3 -B model/tools/rebuild.py diff "$build_dir/custom.hex"
```

The difference report lists changed, added and missing byte ranges. For the display-format example the expected set is one changed byte, at `0xDED8`, with no added or missing bytes. Review the address and instruction differences for your own change and test the affected behavior separately. The same-range packer requires the original populated address map. The example and the volume tests do not exercise laser output, charging behavior, or proximity sensing on hardware. [First experiment and packer](../model/experiments/requested-volume/README.md) · [Annotated display helper](../model/rebuild.asm#L22490)

## Prepare HUG on Windows

Install the known Windows HUG release from the vendor link in the [discovery log](discovery.md). Find the installed `HugUpgrade.exe` and the adjacent `jre` directory. The preparation tool takes **the installed launcher**, which contains HUG's JAR, and verifies its digest and original firmware resource. The tool prepares a new local JAR and prints the application range and the SHA-256 digests of the HEX and the result.

Use the actual paths for your copy of HUG and the directory containing the 797-row build you intend to prepare. The preparer accepts application images within the tested range through `0xE03F`. From the repository root in PowerShell:

```powershell
$hugDir = 'C:\path\to\installed\HUG'
$buildDir = 'C:\path\to\theradome-build'
$hex = Join-Path $buildDir 'volume-settings.hex'
$bundle = Join-Path $buildDir 'HugLocal.jar'
python tools/prepare_hug_update.py (Join-Path $hugDir 'HugUpgrade.exe') $hex $bundle
```

The preparer requires contiguous 64-byte HUG records with valid checksums and an end-of-file record. It accepts the full stock application from `0x1900` through `0xDF7F` and up to the three additional rows accepted on the tested unit. It checks that the resulting JAR preserves the manifest and all other vendor member contents. It rejects unexpected or signed inputs and refuses to overwrite an existing output. It opens no serial port. Two preparations of the same inputs produced byte-identical JARs. [Preparation tool](../tools/prepare_hug_update.py)

## Flash from the prepared HUG bundle

Identify the physical helmet and its application and bootloader revisions before starting. Use a charged helmet, a data-capable USB cable, and a USB Serial Port entry that is working in Device Manager. Keep the helmet off your head and pointed away from eyes during the connection, transfer and startup checks. Keep the original HUG installation and stock image available separately. Save the `D` parameter display if you have an active diagnostics session, then close other programs using the port before starting HUG. An unfamiliar board or loader is outside the tested configuration. [Serial setup](usb-clock-details.md#windows-arm64-driver-setup) · [Board Test and parameter display](board-test-recovery.md#recognize-and-enter-board-test)

Launch the new JAR with the Java runtime shipped beside the vendor launcher:

```powershell
& (Join-Path $hugDir 'jre\bin\java.exe') -jar $bundle
```

The Java 8 runtime was checked offline for the normal HUG entry point and the bundled application resource. Leave the PowerShell window open so you can see the worker result. HUG's online prompt uses Theradome's version metadata. The programmer reads `myTheradome.hex` from the running JAR, so the prepared HEX digest identifies the image to be transferred. [HUG programming and image selection](java.md)

1. **Connect in HUG.** Choose the physical model and click **Next** on the cable screen. On the switch screen, hold the connected helmet's master switch until it speaks the revision. Release it and promptly click **Next** to start the search. When HUG says **Theradome connected**, click **Next** again so it reads the device version and opens the upgrade menu. [Model selection](../java/com/screens/Choose.java#L43-L111) · [Switch screen](../java/com/screens/ConnectPressButton.java#L123-L140) · [Version read](../java/com/screens/Connected.java#L46-L63) · [Button and serial sequence](board-test-recovery.md#recognize-and-enter-board-test)
2. **Confirm the upgrade.** Open **Helmet Upgrade**. Check that you are using the prepared JAR for the intended unit before choosing **Yes**. The GUI requires its online version response and can offer **Yes** even when it says the firmware is up to date. The example builds preserve the application revision. [Version comparison and prompt](../java/com/screens/FirmwareUpdateMenu.java#L196-L262) · [Embedded resource selection](../java/com/screens/FirmwareUpdateProgram.java#L116-L128)
3. **Wait for the loader result.** Leave USB connected during programming. The verified result was `resp rcvdSuccess` followed by `completed write 3` in the console. The GUI completion screen also opens for status `5`, an end-of-transfer fallback based on progress. Check the console for the loader's actual reply. A missing success reply leaves the result uncertain. See the [failed-transfer limits](verification.md#backup-and-recovery) in that case. [Worker fallback](../java/SerialCommunication/Communication.java#L873-L881) · [Completion screen conditions](../java/com/screens/FirmwareUpdateProgram.java#L78-L89)
4. **Finish startup after confirmed success.** In HUG, click **Close** on the completion screen and let its follow-up routine finish. If using the programming worker directly, check the application at 57,600 after the worker releases the port. Follow the [post-flash recovery steps](board-test-recovery.md#recover-after-a-successful-transfer) for Board Test, Escape and the calibration prompts. Verify the behavior you changed after the helmet has returned to normal operation. [Stock completion workflow](board-test-recovery.md#what-stock-hug-does)

The stock image remains available for an application-level reinstall if the helmet can still enter HUG's normal update flow. There is no verified flash readback, stock rollback, or interrupted-update recovery procedure for the tested helmet. The archived application also omits the bootloader, device-specific EEPROM, and voice data. [Backup and recovery](verification.md#backup-and-recovery)
