# Build a local application image for HUG

The editable [PIC18 assembly](../model/rebuild.asm) reconstructs the application shipped with HUG. This guide starts with a byte-identical rebuild, then builds either the published volume change or a small edit of your own. A Windows preparation tool takes a local HEX and the owner's original installed HUG launcher and writes a separate JAR for HUG's normal update interface.

The earlier volume image was installed successfully on one EVO LH40 reporting application revision **126** and bootloader revision **6**, using HUG's Windows programming worker. That transfer used the earlier local runner. The preparation tool has been tested offline with the deployed image and with the vendor's Java 8 runtime. The current overlay adds the ready-prompt change and is offline-tested. A connected-device update through the newly prepared GUI bundle remains untested. The LH80 diagnostic label can appear on an LH40, and a revision match alone cannot establish that another helmet has the same board. [Physical test](volume-settings.md#live-evo-lh40-test-on-november-26-2026) · [Controller evidence](device.md)

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

The current volume overlay yields the SHA-256 digest `3c0a575a8aa3cc1676d0601f0290ce592e09f588068228d419d60cd01a9ac6ef` and passes 1,232 offline cases. To reproduce the earlier live-tested image, use commit `9945182` in a separate checkout. The builder checks the original application digest and allows the declared hook ranges and a contiguous extension. The verifier checks the EEPROM preference, both countdown paths, and the routine ready prompt. [Overlay and verifier](volume-settings.md#offline-build-and-test)

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

Use the actual paths for your copy of HUG and your build directory. From the repository root in PowerShell:

```powershell
$hugDir = 'C:\path\to\installed\HUG'
$buildDir = 'C:\path\to\theradome-build'
$hex = Join-Path $buildDir 'volume-settings.hex'
$bundle = Join-Path $buildDir 'HugLocal.jar'
python tools/prepare_hug_update.py (Join-Path $hugDir 'HugUpgrade.exe') $hex $bundle
```

The preparer requires contiguous 64-byte HUG records with valid checksums and an end-of-file record. It accepts the full stock application from `0x1900` through `0xDF7F` and up to the three additional rows accepted on the tested unit. It checks that the resulting JAR preserves the manifest and all other vendor member contents. It rejects unexpected or signed inputs and refuses to overwrite an existing output. It opens no serial port. Two preparations of the same inputs produced byte-identical JARs. [Preparation tool](../tools/prepare_hug_update.py)

## Use the prepared bundle with the normal HUG interface

Identify the physical helmet and its application and bootloader revisions before starting. The existing [USB notes](usb-clock-details.md) explain why a Windows COM port and the spoken revision answer different questions. Treat an unrecognized board or loader as an untested configuration. Keep the original HUG installation and the stock image available separately.

Launch the new JAR with the Java runtime shipped beside the vendor launcher:

```powershell
& (Join-Path $hugDir 'jre\bin\java.exe') -jar $bundle
```

The Java 8 runtime was checked offline for the normal HUG entry point and the bundled application resource. The interface and device handoff are still HUG's. Its online version prompt describes Theradome's version metadata. The programming worker reads `myTheradome.hex` from the running JAR, so the printed local HEX digest is the reference for the build it will transfer. The stock GUI can still depend on the vendor version endpoint. [HUG programming and image selection](java.md)

When using the update interface, confirm the intended unit and wait for HUG's programming result before disconnecting. After a successful update, reconnect and verify that the application responds and the edited behavior works. An unchanged revision string is expected for both examples. The observed LH40 rebooted, completed its calibration routine, and played the remaining-time announcement at the selected volume. [Live post-update checks](volume-settings.md#live-evo-lh40-test-on-november-26-2026)

The stock image remains available for an application-level reinstall if the helmet can still enter HUG's normal update flow. There is no verified flash readback, stock rollback, or interrupted-update recovery procedure for the tested helmet. The archived application also omits the bootloader, device-specific EEPROM, and voice data. [Backup and recovery](verification.md#backup-and-recovery)
