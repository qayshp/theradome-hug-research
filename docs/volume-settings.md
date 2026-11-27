# Persistent volume and treatment announcements

This experimental build uses HUG's archived revision-126 application as its base. An accepted volume double press saves the new choice. Startup restores Loud, Medium, Soft or Silent from that preference. An erased or invalid value selects the original Loud default.

Both a requested countdown and the automatic 15-, 10- and 5-minute announcements use the selected level for the spoken number and following phrase. The existing countdown schedule determines when the automatic report runs. Each report restores the previous effective gain afterward. The patch also keeps the selected level through the archived idle-hold path that used to clear it.

The ready prompt, off-head reminder, resume, numbered start and successful completion announcements also use the selected level. In the archived application, some states can set the live gain to full while the selected index remains available. Ordinary treatment speech uses that live gain, so it can sound loud between countdowns that apply the selection. The overlay applies selected gain around the stock clip or numbered announcement and restores the incoming gain on return. The stock calls supply clip identifiers and their ordering. The words attributed to individual clips are inferred from the call sites and listening observations because the recordings are in separate voice flash. [Ready and start calls](../model/rebuild.asm#L7496-L7553) · [Pause and resume calls](../model/rebuild.asm#L7616-L7651) · [Completion calls](../model/rebuild.asm#L7695-L7703)

## Battery warnings, manual abort and blocking faults

The follow-up uses the selected level for a low-battery warning that lets treatment continue and for the announcement after a manual abort. Silent therefore mutes both. A fault that prevents or stops treatment uses at least Soft. It uses the selected level when that level is Soft, Medium or Loud. Only Silent gets raised to Soft for a fault. This is a digital playback setting. Audibility depends on the recording, speaker and surroundings.

The battery status check can warn once and allow treatment to continue, or reject the current operation. The same low-voltage band can select either path. The controller also checks a separate internal supply, temperature and laser-current feedback. Applying volume by branch allows the nonblocking warning to use Silent and the blocking alert to use a nonzero gain. The same ordinary abort notice follows a manual stop and other exits through the stop state, such as a paused-treatment timeout. The notice uses the selection on each of those paths. If a check fails, its preceding fault alert still uses the Soft minimum. Nonblocking temperature advisories in the shared check use the selection as well. [Battery warning branches](../model/rebuild.asm#L11615-L11668) · [Controller and sensor checks](../model/firmware-io-map.md#analog-acquisition-and-charger-feedback) · [Stock stop state](../model/rebuild.asm#L7705-L7720)

## Button-requested treatment count

With the refactored 797-row image installed, a short press while the helmet was connected to a charger still played “Treatment [number] complete” at full volume. This is the stored completion-count readout. The button dispatcher temporarily sets full gain before calling the count routine at `0x7428`. Automatic treatment completion has its own call at `0x5F2A`. The follow-up uses the existing counted-speech wrapper at `0x7428` so the report uses the selected level for the prefix, number and suffix. The wrapper restores the incoming gain on return, and the button dispatcher then restores the gain that preceded the press. [Button gain override](../model/rebuild.asm#L9725-L9737) · [Count readout call](../model/rebuild.asm#L10172-L10175) · [Counted speech](../model/rebuild.asm#L20310-L20360)

## Assembly and storage

[The PIC18 assembly overlay](../model/volume-settings.asm) uses the same `18f66k22` assembler profile as the stock reconstruction. It assembles short hooks, a shared volume conversion in the stock button-volume area, and a separate block of routines. The original source and its address map remain available for comparison.

The overlay, build script and verifier used for the earlier live test are in commit [`9945182`](https://github.com/qayshp/theradome-hug-research/commit/994518261dcfdc6da08d11030e52c03f6ecd04ab). Commit [`35f7ac3`](https://github.com/qayshp/theradome-hug-research/commit/35f7ac3f0999c10adb69a0c218f79165478e496c) extended that build to ordinary treatment prompts. The following table lists the hooks in the count-readout build. The overlay is ordinary hand-editable PIC18 assembly. Microchip's XC8 compiler can also generate PIC18 code from C. A C routine here would need to fit the existing application's memory layout, register and calling conventions, and bootloader image layout. The C files under `model/` are host-side behavior models. [Microchip XC8 compiler](https://www.microchip.com/en-us/tools-resources/develop/mplab-xc-compilers/xc8) · [Stock assembly reconstruction](../model/rebuild.md)

| Stock address | Purpose |
| --- | --- |
| `0x5358` | Retain the original startup revision write, then read the saved volume. |
| `0x5D86`, `0x5D9A` | Apply selected gain to the numbered treatment-start announcement and its optional clip. |
| `0x5DBE`, `0x5DCA`, `0x5DE2` | Apply the selected gain for each of the three routine ready clips, then restore the incoming gain. |
| `0x5E26` | Save and restore the effective gain around the automatic report. |
| `0x5E96`, `0x5EA2`, `0x5EB4` | Apply selected gain to the two resume clips and the off-head reminder while paused. |
| `0x5F24`, `0x5F2A` | Apply selected gain to the optional successful-completion clip and the numbered completion announcement. |
| `0x5F46`, `0x5F52` | Apply selected gain to the two ordinary abort clips. |
| `0x6306` | Apply the Soft floor to the optional clip in the failed-check waiting state. |
| `0x72AC` | Advance the selected level, persist it, then run the original feedback handler. |
| `0x7330` | Preserve the selected index on the idle hold. |
| `0x7428` | Apply selected gain to the button-requested completed-treatment count. |
| `0x74E8`, `0x74EC–0x750D` | Route the button confirmation through the same volume conversion as the speech paths. Resume the stock clip setup at `0x7512`. |
| `0x80C4` | Use selected gain after the shared check has saved the incoming gain. |
| `0x80CE`, `0x8122`, `0x81C4`, `0x81DE`, `0x8240`, `0x8274`, `0x8316`, `0x838C` | Apply the Soft floor before the first clip of each blocking battery, controller-supply, temperature or laser-current sequence. The shared check restores the incoming gain at its original exit. |
| `0xCAC8` | Apply the selected gain when a countdown report reaches its speech path. |

The preference uses **data EEPROM byte `0xC0`**, the low byte of word slot `0x60`. The four one-byte codes are `C3`, `C6`, `C9` and `CC`. The reader accepts only those codes. Saving uses one byte write and keeps the adjacent byte intact. Only the volume-cycle path writes the preference. The archived application has no identified ordinary use of that slot. The treatment ring starts at word `0x64`, and a whole-EEPROM erase would clear the preference. The physical unit and the separate bootloader have not been checked for their use of this EEPROM range. [Archived EEPROM map](../model/eeprom.md)

## Offline build and test

From the repository root with Python 3 and gputils installed, run these commands in Linux or WSL. The build here used gpasm 1.4.0.

```sh
build_dir=$(mktemp -d)
python3 -B -m model.tools.build_volume_settings "$build_dir/volume-settings.hex"
python3 -B model/check_volume_settings.py "$build_dir/volume-settings.hex"
```

The [build script](../model/tools/build_volume_settings.py) invokes gpasm for the sparse overlay, checks the stock HEX digest and address range, and restricts changes to declared hooks and a contiguous extension. The script re-encodes the merged image using the vendor's 64-byte Intel HEX rows, per-record checksums and CRLF line endings. The stock image also round-trips through the encoder byte for byte. [Intel HEX format](https://onlinedocs.microchip.com/oxy/GUID-C3F66E96-7CDD-47A0-9AB7-9068BADB46C0-en-US-4/GUID-DF9E479D-6BA8-49E3-A2A5-997BBA49D34D.html)

### Button-requested count follow-up

Two independent builds of the count-readout follow-up produced the same 112,390-byte HEX with SHA-256 `f8b3a0e4828560ea2ca38e48c1a8ebf2de6105dc551f8d9a9d3378a58bf03396`. It adds one hook using existing routines and keeps the 797 data records across `0x1900–0xE03F`. A binary comparison with the installed `50b1474` image found differences at `0x7428` and `0x742A`, the two modified bytes of the call target. [Count readout](#button-requested-treatment-count) · [Assembly overlay](../model/volume-settings.asm)

The verifier passed 5,376 new short-press cases across 14 dispatch states, including 768 cases for ordinary charging and charged states. The checks compare the speech sequence, selected and restored gain, caller and playback registers, and treatment-counter EEPROM operations. The 4,608 existing comparisons against `a99c969` also passed, for 9,984 cases across the full run. The same image was used for the live transfer. [Volume verifier](../model/check_volume_settings.py) · [Live count-readout update](#live-count-readout-update)

### 797-row volume refactor

The refactor brings the warning and fault policy from `a99c969` into the 797-row layout used in the physical treatment-prompt update. The shared volume conversion takes 30 bytes at `0x74EC–0x7509`. The four-byte adapter at `0x750A` calls the conversion and branches to the original confirmation clip setup at `0x7512`. Announcements use the same conversion through a jump from the overlay. Reusing a clip-call adapter, returning directly from the startup conversion, and shortening the Soft floor check reduce the space needed after the stock image. [798-row baseline commit](https://github.com/qayshp/theradome-hug-research/commit/a99c9692e2d5de9cc328974091f2274d9d3085fb) · [Assembly overlay](../model/volume-settings.asm) · [Archived button feedback](../model/rebuild.asm#L10277-L10317)

The 112,390-byte refactored HEX has SHA-256 `d006463504583b23788555442f06b24cd940467e496b07ccbd4ff62d41c6ac98`. It carries 51,008 bytes in 797 data records over `0x1900–0xE03F`. Its extension after the stock image is 192 bytes, down from 230 bytes in `a99c969`. The refactor changes 94 stock bytes across 29 hooks. Compared directly with `a99c969`, the refactor changes 224 bytes in the common range and drops the final 64-byte record. [Earlier warning and fault build](#results-for-the-offline-warning-and-fault-build)

The direct verifier matched the two builds in **4,608 cases**. It reran all 4,352 existing scenarios and added 256 double presses with audio enabled and disabled. Both images made the same ordered clip and number requests at the same gain, with matching playback-entry registers and enable flag, final modeled RAM and registers, EEPROM reads and writes, and peripheral calls. These checks model the sensors and audio player at their call boundaries. The checked Windows bundle preparer accepted the refactored HEX and the vendor's Java 8 runtime found the embedded resource at the expected SHA-256. The loader accepted the refactored image through `0xE03F` on the tested LH40. [Comparison verifier](../model/check_volume_settings.py) · [Refactored image live transfer](#live-797-row-refactor)

#### Reproduce the direct comparison

After building the image above, build the baseline in a separate working tree and pass both HEX files to the verifier. The current checkout also runs the new count-readout checks against the stock button handler.

```sh
baseline_dir=$(mktemp -d)
git worktree add --detach "$baseline_dir/source" a99c9692e2d5de9cc328974091f2274d9d3085fb
(
  cd "$baseline_dir/source"
  python3 -B -m model.tools.build_volume_settings "$baseline_dir/a99c969.hex"
)
python3 -B model/check_volume_settings.py "$build_dir/volume-settings.hex" \
  --compare-to "$baseline_dir/a99c969.hex"
git worktree remove "$baseline_dir/source"
```

The verifier checks the baseline digest before comparing ordered clip and number requests, gain, audio enable, registers, RAM, and modeled EEPROM and peripheral activity. [Comparison verifier](../model/check_volume_settings.py)

### Results for the offline warning and fault build

Two builds produced byte-identical 112,531-byte HEX files with SHA-256 `aef971a11b97101bdeee421517cab60e516df8bf0b1ff65c81ff054c8e2f2cfd`. That overlay changes 61 stock bytes across 27 hooks and adds 230 bytes of code after the stock application's last row. The code ends at `0xE065`. Encoding the last row with its padding yields 798 data records across `0x1900–0xE07F`. Compared with the installed treatment-prompt build, there are 123 changed bytes in the common range and one additional 64-byte row.

The verifier passed **4,352 instruction-level cases** with modeled sensors, EEPROM and audio. The stock comparisons check which clips are requested, whether a check passes, error and abort counters, and the restored gain. The new cases include a battery advisory followed by a different blocking fault in the same scan, previously issued advisories, and the player's audio-disabled return.

| Checks for this build | Cases passed |
| --- | ---: |
| Existing startup, preference, countdown, button and ordinary speech cases | 2,096 |
| Shared advisory and fault checks | 1,824 |
| Stop announcement after a manual hold or a timeout | 336 |
| Supplemental failed-check prompt | 96 |
| **Total** | **4,352** |

The checked HUG bundle preparer accepts the previously tested range through `0xE03F` and rejects the additional row in the `a99c969` image. That build was exercised on the host. Its extra flash row and physical alert audibility remain unverified. [Earlier live transfer](#expanded-treatment-prompt-update) · [Bundle range check](../tools/prepare_hug_update.py)

### Results for the installed treatment-prompt build

Two independent builds at `35f7ac3` produced the same 112,390-byte HEX with SHA-256 `20acb2e12643db82b9bfc48df288261f0b7bfa2d5bb985470416b00c4cf9d68c`. It changes 33 bytes within the stock application. The single-clip helper occupies 22 bytes, and the 20-byte numbered-announcement wrapper fits in the remaining space through `0xE03F`. That keeps the 797 data rows and address range of the earlier installed image, with 60 changed byte values across the equal-sized images. The earlier ready-only image ended its code at `0xE02B` and carried 20 bytes of padding in the last row.

The HUG bundle preparer accepted the expanded image. A separate Java 8 check verified the HEX digest through the same classpath lookup used by the HUG programming worker. Two earlier preparations of the ready-only image produced byte-identical JARs. [Bundle preparation](custom-firmware.md#prepare-hug-on-windows)

The [offline verifier at `35f7ac3`](https://github.com/qayshp/theradome-hug-research/blob/35f7ac3f0999c10adb69a0c218f79165478e496c/model/check_volume_settings.py) executed the assembled instructions through the startup, button, countdown and treatment-prompt hooks with modeled EEPROM and audio.

| Check | Cases passed |
| --- | ---: |
| Startup across all possible EEPROM byte values | 256 |
| Double presses followed by fresh simulated restarts | 64 |
| Requested and automatic countdowns across levels, gains and timer values | 768 |
| Long-press behavior | 48 |
| Both ready-prompt stages across selected and effective gains, invalid selections and audio-enable states | 96 |
| Off-head reminder and resume across the same gain and audio states | 96 |
| Numbered start and completion across the same gain and audio states, with and without optional clips and varying stored treatment counts | 768 |
| **Total** | **2,096** |

The countdown tests check both parts of the announcement, automatic timing, and gain after the report. The treatment-prompt tests compare the speech requests, player inputs, RAM, registers and EEPROM operations. The start and completion calls reuse the stock numbered-announcement helper with its original distinct arguments, `0x24` and `0x23`. The tests confirm its treatment-count read and its stock behavior when the count is uninitialized, including a write of zero. The volume wrapper applies the selected gain for playback and restores the incoming setting afterward.

## Live count-readout update

On November 27, 2026, the checked Windows runner transferred the image built from `cbb99db` to the same EVO LH40. The owner heard “Theradome Revision 126.6” before the handshake. The pre-transfer USB session independently reported `126.6` and saved the diagnostic parameter display locally. After the loader reported readiness, the runner streamed 797 data records. The transfer log recorded 797 row acknowledgments, zero NAKs, one final `Success`, and HUG worker status `3`. Those results establish acceptance by the loader. A post-update playback check is separate. [Tested image and digest](#button-requested-count-follow-up)

| Step | Light and speech reported by the owner |
| --- | --- |
| USB connected before the handshake | Steady green at rest. A short press spoke the treatment count, and a long press spoke the revision. During each announcement the light briefly showed yellow and returned to green. |
| Handshake accepted | Steady orange appeared when the handshake completed. The owner saw no further change when transfer began or while it was in progress. |
| Loader reported `Success` | The light went out briefly, showed several colors, and returned to steady orange. The helmet stayed silent. No new diagnostic session had started. |
| USB unplugged once, with the switch untouched | The light blinked red at about two flashes per second. |
| Brief switch press while unplugged and off-head | The blink appeared to restart with the press and continued at about two flashes per second. The helmet stayed silent. |
| Reconnected to laptop USB using the same data cable, with the switch untouched | The light returned to steady orange. No new diagnostic session was running. |
| One brief switch press with laptop USB connected | The light stayed steady orange. The owner heard no speech and saw no laser emission. |

Neither charged-state button handler explicitly requests yellow for speech. A change in the charging indication could account for the brief yellow. Charge-status inputs and charging current were unmeasured during playback. Board Test requests steady red and green together, which fits the first orange seen at the handshake. The origin of the steady orange during and after transfer remains unverified. The LH40/LH80 manual lists a firmware update as one possible reason for a blinking red reset indication. For a system error it describes one brief switch press with the helmet unplugged to check for “System resumed.” [Charged-state button handlers](../model/rebuild.asm#L10170) · [Indicator model](../model/indicator.md) · [Board Test indicator](../model/rebuild.asm#L1489) · [Theradome owner’s manual, pp. 16 and 30](https://device.report/m/f7d6c2daf50f49bd67078d4ed2da62448f70583a10c274b357377daf5ef15b9a) · [Theradome system-error instructions](https://theradome.com/a/docs/troubleshooting/system-error-message)

The unplugged press did not produce the documented acknowledgment. The archived firmware has several red-only patterns and distinguishes electrical switch activity from a completed button event. A change in the blink phase is insufficient to identify the state or gesture. The plugged-in press yielded no observable response, although a press can launch diagnostics if Board Test is active. No serial traffic was captured during that press. The earlier wait on laptop USB is an observed interval. The archived calibration paths check saved markers and charger inputs, and HUG's guidance for repeated calibration speech refers to a steady green light. Those sources give no 24-minute recovery interval. [Button-event recognition](../model/inputs.md#from-switch-edges-to-event-codes) · [Calibration prerequisites](../model/rebuild.asm#L6791-L6822) · [HUG help](../java/com/screens/FirmwareUpdateMenu.java#L432)

### Calibration snapshots across updates

The first capture predates experimental firmware. The second followed the first flash, `9945182`, and its observed calibration. There was an intervening transfer of `35f7ac3` with a revision read and no captured calibration parameters. The runner captured the third column after the owner reported completing calibration on `50b1474` and before transferring `cbb99db`. The snapshots show stored values and span updates and use between captures. [First update parameter comparison](#eeprom-display-before-the-update-and-after-calibration) · [Intervening transfer](#expanded-treatment-prompt-update)

| Field | Original read | After `9945182` calibration | After `50b1474` calibration, before `cbb99db` |
| --- | ---: | ---: | ---: |
| 20 · Battery-voltage offset | −4 | −13 | 1 |
| 21 · Internal-voltage measurement offset | −30 | −13 | −86 |
| 22 · Temperature offset | −1 | −1 | −1 |
| 23 · Laser-current feedback offset | −25 | −25 | −22 |
| 24 · Proximity-sensor offset | −5 | −6 | −4 |
| Calibration temperature | 23.50 °C | 22.82 °C | 20.88 °C |

The manufacturing flags in fields 25–31 matched across all three reads, including `A5A5` in the calibration-complete field 29. Both install-date fields in the latest two reads reported November 26, 2026. The raw captures are retained locally. [Calibration sequence and saved values](../model/firmware-io-map.md#how-the-built-in-calibration-runs)

## Live 797-row refactor

The checked Windows runner installed the image built from commit `50b1474` on the same EVO LH40 on November 26, 2026. The transfer log recorded 797 row acknowledgments and zero NAKs. The loader returned `Success`, and HUG's worker reported status `3`. A fresh read-only COM7 session returned application and loader revision `126.6`. [Refactored image and digest](#797-row-volume-refactor)

After unplugging and reconnecting the helmet, the owner saw the switch blinking red. The helmet remained connected to laptop USB for roughly 24 minutes. On returning, one brief press appeared to elicit “Ready for calibration.” Calibration started while the helmet was connected. The owner reported seeing brief laser emission before calibration finished. In the archived ready state, the switch handler can repeat the announcement, and an existing charger connection can allow the next phase to begin immediately. The exact trigger and individual emitter output remain unverified. [Calibration-ready and charger checks](../model/rebuild.asm#L6791-L6822) · [Ready-state switch handler](../model/rebuild.asm#L10005-L10015) · [Calibration-start path](../model/rebuild.asm#L6989-L7026)

The owner reported that ordinary use away from the charger followed the selected volume. The remaining louder prompt they noticed was “Treatment [number] complete” after a short press while connected to a charger. [Button-requested count readout](#button-requested-treatment-count)

## Expanded treatment-prompt update

The expanded image was installed on the same physical EVO LH40 on November 26, 2026. The checked Windows runner used HUG's programming worker and the bundled HEX with SHA-256 `20acb2e12643db82b9bfc48df288261f0b7bfa2d5bb985470416b00c4cf9d68c`.

| Check | Result |
| --- | --- |
| First read-only connection | COM7 opened and received no bytes in the connection window. |
| Updater connection | With COM7 open, the owner held the master switch. The host read application revision 126 and bootloader revision 6 and waited for bootloader readiness before sending the image. |
| Transfer | The bootloader returned `Success`, HUG's worker reported status `3`, and the checked runner exited with status `0`. |
| Fresh application read | A separate read-only COM7 session returned `126.6` and exited with status `0`. No additional button hold was needed for this read. |

The fresh revision reply confirms the application was responsive after the transfer. Direct program-flash readback, physical playback of the newly changed prompts and retention across a physical restart of this build remain unverified. The compiled HEX, repackaged JAR and raw serial logs stay in the local archive. [Offline checks and row coverage](#offline-build-and-test) · [Backup and recovery](verification.md#backup-and-recovery)

## Live EVO LH40 test on November 26, 2026

This test covered one physical EVO LH40. Its USB diagnostics identified it as LH80. The test used HUG's existing Windows update worker and the earlier volume image from commit `9945182`, with SHA-256 `5ce52a11605881b2da8c2ef1bf3a5715e2cd304eaf1b04d77c61b979a82ea1f6`. That image changed 13 stock bytes and used three additional HEX rows, as did the later treatment-prompt build. The host checked the bundled image's digest before transfer. The compiled patched HEX and raw device logs were kept local. The comparison omits the unit identifier.

| Check | Result |
| --- | --- |
| First connection attempt | COM7 opened but returned no bytes during the identification window. The updater stopped before sending any HEX records. |
| Retry with the port open | A sustained master-switch hold brought the application into contact. The host read product revision 126 and bootloader revision 6, then waited for loader readiness before starting the transfer. |
| Transfer | The bootloader returned `Success` after the upload. HUG's worker reported success status `3`. |
| Application after reboot | A fresh COM7 session again returned revision 126.6. The overlay preserves that version, so the revision establishes that the application responded after reboot. |
| Calibration sequence | The owner reported an orange LED during the update, followed by flashing red. The helmet later said “Ready for calibration” and flashed yellow. Reconnecting the USB cable to the laptop produced “Calibration started,” then “Calibration complete.” |
| Requested remaining time | After calibration, the owner reported normal operation and confirmed that the requested remaining-time announcement used the selected volume. This was a listening check. |
| Later charging on laptop USB | The light stayed orange after the parameter readout. It turned off when USB was disconnected. A button press produced a blinking green light and the spoken request to place the helmet on the head. After USB was reconnected, the light briefly faded between yellow and orange, then held solid green. |
| USB after the laptop charge indication | Windows still showed a healthy COM7. A five-second passive listen received zero bytes. One carriage return sent with the switch untouched produced zero bytes in four seconds. |
| Automatic countdowns and retention | The offline tests passed. Playback at the automatic countdowns and retention of a new selection across a later physical restart still await a listening check. |

This unit booted and responded after the transfer. Direct program-flash readback, interrupted-update recovery and stock reinstallation were outside this test. [Earlier USB and recovery results](verification.md)

### EEPROM display before the update and after calibration

The before and after readings are separate diagnostic snapshots. The fields changed between those snapshots, which cover the update, reboot, calibration and subsequent operation. The display exposes selected parameters rather than the entire EEPROM.

| Field | Before | After |
| --- | ---: | ---: |
| 20 · Battery-voltage offset | −4 | −13 |
| 21 · Internal-voltage measurement offset | −30 | −13 |
| 22 · Temperature offset | −1 | −1 |
| 23 · Laser-current feedback offset | −25 | −25 |
| 24 · Proximity-sensor offset | −5 | −6 |
| Calibration temperature | 23.50 °C | 22.82 °C |
| Factory install date | February 10, 2026 | November 26, 2026 |
| Upgrade install date | Erased, displayed as `165/165/165165` | November 26, 2026 |
| Displayed counter 0 | 242 | 243 |
| Displayed counter 7 | 21 | 22 |
| Displayed counter 10 | 8823 | 8824 |

The other displayed counters matched. Manufacturing flags 25–31 were `A5A5`, `A5A5`, `FFFF`, `A5A5`, `A5A5`, `A5A5`, `A5A5` in both reads. The application, bootloader and voice revisions matched (`126`, `6`, `V1`). The invalid old upgrade date comes from interpreting erased bytes as packed decimal without checking the digits. The archive has separate date writes for service setup and post-update processing. The service path can rewrite the field labeled “Factory install date.” HUG's help describes calibration after an upgrade, and the exact trigger on this unit remains unverified. [Date formatter](../model/rebuild.asm#L18889) · [Service setup date write](../model/rebuild.asm#L9170) · [Upgrade date write](../model/rebuild.asm#L6706) · [HUG troubleshooting text](../java/com/screens/FirmwareUpdateMenu.java#L432)

### Battery reference and calibration status

The archived calibration path waits for the same charger-input pattern that ordinary diagnostics report as **Charged**. It compares the sampled battery reading with a fixed reference equivalent to about **4.20 V** and saves a signed correction. The change in field 20 makes the normal reported battery voltage about **10 mV lower** for the same raw input. Field 21 changed by 17 ADC counts, equivalent to about 15 mV at a 3.7 V battery estimate with the other inputs held fixed. The field 21 calculation depends on both the battery estimate and the internal ADC measurement. A physical change in the supply rail remains unverified. [Charger inputs](../model/inputs.md#what-the-charging-diagnostic-prints) · [Calibration gate](../model/rebuild.asm#L7023) · [Battery correction](../model/rebuild.asm#L13645) · [Internal-voltage correction](../model/rebuild.asm#L16789)

The calibration routine can store `0xA5A5` in word 29 to mark calibration complete. That word had the same value in both snapshots. The traced routine saves the voltage correction and completion flag. It has no separate save of a learned full-charge percentage or capacity. Completing calibration on laptop USB shows that the firmware saw the expected charger signal. The later fade-to-green transition on the same laptop is further evidence that the helmet saw charging and then charge complete. No inline measurement of USB current or independent measurement of cell voltage was made. The test covered one calibration cycle. [Completion flag](../model/rebuild.asm#L7407) · [Startup flag check](../model/rebuild.asm#L6791)

### What the switch light indicated

| Observation | Interpretation |
| --- | --- |
| Orange during transfer | The observed timing places this during the update. The archived application's accepted handoff resets into the loader without an explicit orange request. The bootloader falls outside the archived HEX, so the origin of that light remains unverified. |
| Flashing red before “Ready for calibration” | Several archived states request a red-only pulse. The color alone leaves the exact state uncertain. |
| Pulsing yellow with “Ready for calibration” | The ready path requests a pulse using both red and green LED channels. After “Calibration started,” the application requests the same fade used during ordinary charging. |
| Steady orange after the settings readout | Entering Board Test requests steady red and green together. The host closed its serial port without sending the helmet's menu-exit command. That makes the diagnostic indicator a plausible explanation for the steady orange. |
| Orange-to-yellow or yellow-to-orange fade, then solid green after reconnect | Theradome documents a red-to-orange or orange-to-yellow fade while charging, a yellow-to-green fade near completion and steady green when charged. The plugged-in transition distinguishes the charge indication from the green treatment-ready light. |

[Application handoff](../model/rebuild.asm#L4623) · [Board Test indicator request](../model/rebuild.asm#L1489) · [Calibration-ready path](../model/rebuild.asm#L6791) · [Calibration-start path](../model/rebuild.asm#L6989) · [Indicator model](../model/indicator.md) · [Theradome charging colors](https://theradome.com/a/docs/guide/how-to-use-theradome)

### What the long hold changes over USB

Windows enumerates the USB bridge independently of the helmet application's serial state. On the archived charged-state path, a sustained hold first plays the revision, then enables the controller's serial receiver and waits for input or a timer. On receive it proceeds to the Board Test handshake. If the receive window expires it calls the serial cleanup routine. Successfully entering Board Test sets the two-color indicator. This fits the quiet charged-state check and the earlier successful connection timed to a hold. Different application states can give the same button gesture a different meaning. [Switch-event timing](../model/inputs.md#from-switch-edges-to-event-codes) · [Charged-state hold handler](../model/rebuild.asm#L10176) · [Receive window](../model/rebuild.asm#L19975) · [Handshake](../model/rebuild.asm#L21015)

### Why the revision announcement can still be loud

“Theradome Revision 126.6” is one application announcement. The application supplies `126` and reads `6` from the bootloader region at `0x18FE`. The shared button dispatcher temporarily clears attenuation, which gives the long-press announcement full digital gain. The ordinary returning path restores the previous effective gain when its state check permits it. The original idle-hold handler also reset the selected index. The overlay prevents that index reset and changes the two countdown paths. The long-press gain override still applies. The application could apply the selected gain around the revision announcement in a future change. No revision audio change was made for this test. [Button gain override](../model/rebuild.asm#L9734) · [Revision call site](../model/rebuild.asm#L10177) · [Gain restore](../model/rebuild.asm#L10319) · [Spoken revision routine](../model/rebuild.asm#L20779)

Other stock state entries and status routines can also clear the effective attenuation while leaving the stored preference intact. A loud prompt by itself gives limited evidence about the selected setting. [State entry at `0x62DA`](../model/rebuild.asm#L8135) · [Status routine](../model/rebuild.asm#L11545) · [Volume state trace](../model/volume.md#ways-a-prompt-can-sound-loud)

The speech routine passes the application revision to its general spoken-number renderer. An application deliberately renumbered to `127` could therefore say `127.6` with the same bootloader and recordings. The startup value used by the serial parameter display and the Board Test Audio text have separate copies of the application revision. Changing only HUG's remote version metadata would leave those values and the speech unchanged. The tested overlay keeps revision `126`. [Spoken revision argument](../model/rebuild.asm#L20804) · [Startup revision](../model/rebuild.asm#L6429) · [Audio-test text](../model/rebuild.asm#L5739)

### Where the voice recordings are stored

The archived application contains playback code and tables. Its playback routine looks up a clip in a four-byte directory entry, reads compressed bytes from a separate SPI flash address space, decodes two 4-bit ADPCM samples per byte, applies the effective attenuation, and drives PWM audio. The number routine builds spoken numbers from clips. The archived application HEX and the HUG packages examined contain no identified library of the recordings. [Playback routine](../model/rebuild.asm#L13882) · [Clip directory lookup](../model/rebuild.asm#L17818) · [SPI read](../model/rebuild.asm#L19011) · [Number routine](../model/rebuild.asm#L15674) · [Archive coverage](firmware.md)

The earlier LH40 diagnostics returned 255 clip-directory slots. The first 50 entries describe a contiguous run of clip pages totaling 507,336 bytes of compressed payload. The first clip's 8,617 compressed bytes correspond to the 17,234 decoded samples seen in two matching captures. The part number, complete phrase inventory and original recording format remain unverified. [Earlier live voice readout](verification.md#live-evo-lh40-tests) · [Clip IDs, banks and words](voice-clips.md)
