# Persistent volume and countdown announcements

This experimental build uses HUG's archived revision-126 application as its base. An accepted volume double press saves the new choice. Startup restores Loud, Medium, Soft or Silent from that preference. An erased or invalid value selects the original Loud default.

Both a requested countdown and the automatic 15-, 10- and 5-minute announcements use the selected level for the spoken number and following phrase. The existing countdown schedule determines when the automatic report runs. Each report restores the previous effective gain afterward. The patch also keeps the selected level through the archived idle-hold path that used to clear it.

## Assembly and storage

[The PIC18 assembly overlay](../model/volume-settings.asm) uses the same `18f66k22` assembler profile as the stock reconstruction. It assembles five short hooks and a separate block of new routines. The original source and its address map remain available for comparison.

| Stock address | Purpose |
| --- | --- |
| `0x5358` | Retain the original startup revision write, then read the saved volume. |
| `0x5E26` | Save and restore the effective gain around the automatic report. |
| `0x72AC` | Advance the selected level, persist it, then run the original feedback handler. |
| `0x7330` | Preserve the selected index on the idle hold. |
| `0xCAC8` | Apply the selected gain when a countdown report reaches its speech path. |

The preference uses **data EEPROM byte `0xC0`**, the low byte of word slot `0x60`. The four one-byte codes are `C3`, `C6`, `C9` and `CC`. The reader accepts only those codes. Saving uses one byte write and keeps the adjacent byte intact. Only the volume-cycle path writes the preference. The archived application has no identified ordinary use of that slot. The treatment ring starts at word `0x64`, and a whole-EEPROM erase would clear the preference. The physical unit and the separate bootloader have not been checked for their use of this EEPROM range. [Archived EEPROM map](../model/eeprom.md)

## Offline build and test

From the repository root with Python 3 and gputils installed, run these commands in Linux or WSL. The build here used gpasm 1.4.0.

```sh
build_dir=$(mktemp -d)
python3 -B -m model.tools.build_volume_settings "$build_dir/volume-settings.hex"
python3 -B model/check_volume_settings.py "$build_dir/volume-settings.hex"
```

The [build script](../model/tools/build_volume_settings.py) invokes gpasm for the sparse overlay, checks the stock HEX digest and address range, and restricts changes to the five hooks and a contiguous extension. The script re-encodes the merged image using the vendor's 64-byte Intel HEX rows, per-record checksums and CRLF line endings. The stock image also round-trips through the encoder byte for byte. [Intel HEX format](https://onlinedocs.microchip.com/oxy/GUID-C3F66E96-7CDD-47A0-9AB7-9068BADB46C0-en-US-4/GUID-DF9E479D-6BA8-49E3-A2A5-997BBA49D34D.html)

The resulting HEX is 112,390 bytes with SHA-256 `5ce52a11605881b2da8c2ef1bf3a5715e2cd304eaf1b04d77c61b979a82ea1f6`. It changes 13 stock bytes. The appended routines run from `0xDF80` through `0xE015`. Padding completes the last of three extra rows through `0xE03F`. The archived image stops at `0xDF7F`, so the physical bootloader's acceptance of those extra rows remains unverified.

The [offline verifier](../model/check_volume_settings.py) executes the assembled instructions through the startup, button and countdown hooks with modeled EEPROM and audio.

| Check | Cases passed |
| --- | ---: |
| Startup across all possible EEPROM byte values | 256 |
| Double presses followed by fresh simulated restarts | 64 |
| Requested and automatic countdowns across levels, gains and timer values | 768 |
| Long-press behavior | 48 |
| **Total** | **1,136** |

The tests check both parts of the announcement, the original automatic timing, and the gain after the report. They also check that a save writes the one expected byte and that startup and countdowns make no preference writes. Physical EEPROM retention, audio playback and update recovery require separate verification. This image was built and tested offline and has never been sent to a helmet.
