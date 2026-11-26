# Archived application address index

This map describes the revision-126 application bundled with HUG. Its Intel HEX contains 50,816 consecutive program bytes at `0x1900–0xDF7F`. Use the hexadecimal **byte address** at the end of an [assembly line](rebuild.asm) to find the corresponding location in the HEX. The raw binary starts at `0x1900`, so its file offset is the byte address minus `0x1900`. RAM registers and data EEPROM have separate address spaces. [Image inventory](../docs/firmware.md) · [EEPROM layout](eeprom.md)

The source preserves the original instruction operands, branch labels and addresses. New comments identify behavior established by traces, call sites, tables, or peripheral accesses. An annotation on a label describes that entry point. It may be a shared tail or an interior branch rather than a subroutine. [Controller family evidence](controller.md)

## Programming ranges

| Byte range | Contents in this image |
| --- | --- |
| Below `0x1900` | Outside the archived application. The revision announcer reads the loader revision from immediately below the relocated reset vector. The loader code itself remains unavailable. [Revision read](rebuild.asm#L20815) |
| `0x1900–0x1A47` | Relocated reset and interrupt vectors, low-priority interrupt code, and the reset trampoline. [Vectors](rebuild.asm#L7) · [Control-flow map](firmware-control-map.md) |
| `0x1A48–0x2439` | Main preserved data block. It begins with the voice decoder's step and index tables and also contains constants and diagnostic strings. [Step table](rebuild.asm#L217) · [Index table](rebuild.asm#L266) · [Recovered strings](../outputs/firmware-strings.txt) |
| `0x243A–0x530D` | Board Test entry, text and diagnostic handlers, programming handoff, command loop, and related support paths. [Board Test entry](rebuild.asm#L1490) · [Command dispatcher](rebuild.asm#L6063) |
| `0x530E–0x6FFF` | Application startup, operating-state dispatcher, treatment and calibration state handlers, and shared support routines. [Startup](rebuild.asm#L6407) · [Foreground dispatcher](rebuild.asm#L6618) · [Control-flow map](firmware-control-map.md) |
| `0x7000–0x8417` | Button actions, indicator settings and waveforms, alarm processing, and status announcements interleaved with shared routines. [Button dispatcher](rebuild.asm#L9714) · [Indicator request](rebuild.asm#L10335) · [Status audio path](rebuild.asm#L11541) |
| `0x8418–0x9301` | High-priority interrupt entry, switch interrupts and timers, and mixed control and measurement code. `0x860A–0x86A7` is the largest preserved word run outside the main data block. Its full control flow remains unresolved. [High-priority entry](rebuild.asm#L11951) · [Unresolved run](rebuild.asm#L12260) |
| `0x9302–0xB075` | Battery acquisition, voice playback and decoding, indicator lookup, spoken numbers, and peripheral support. [Battery acquisition](rebuild.asm#L13645) · [Clip player](rebuild.asm#L13824) · [ADPCM decoder](rebuild.asm#L16916) |
| `0xB076–0xCD41` | ADC selection, serial flash access, charging events, application UART, remaining-time reporting, and assorted numeric routines. [ADC selector](rebuild.asm#L17077) · [SPI directory lookup](rebuild.asm#L17818) · [Serial receive window](rebuild.asm#L19975) |
| `0xCD42–0xDF7F` | Compiler RAM initialization and additional serial, RTC, EEPROM and numeric helpers, with trailing initialization data. [Compiler startup](rebuild.asm#L20416) · [EEPROM read](rebuild.asm#L21234) · [EEPROM write](rebuild.asm#L21582) · [RTC read](rebuild.asm#L21895) |
| At and above `0xDF80` | Outside the stock HEX. The independently built volume overlay added code here and completed its last HEX row at `0xE03F` on one tested LH40. The device's supported program limit and the use of other addresses remain unverified. [Volume overlay layout](../docs/volume-settings.md#offline-build-and-test) |

The ranges summarize nearby code. The compiler lays out related routines throughout the image, so a path can move between ranges. The two maps below follow those calls across the image.

## Trace by subsystem

| Area | Documentation |
| --- | --- |
| Reset, interrupts, modes, treatment and power | [Firmware control-flow map](firmware-control-map.md) and the [executable state model](README.md) |
| Voice, diagnostics, sensors, clock and nonvolatile storage | [Firmware I/O and storage map](firmware-io-map.md) and the [EEPROM layout](eeprom.md) |
| Switch timing, charging and indicator outputs | [Input and button timing](inputs.md) and the [charging waveform](indicator.md) |
| Effective speech gain versus the selected level | [Voice control model](volume.md) and the [volume overlay](../docs/volume-settings.md) |
| Board and MCU identification | [Controller register evidence](controller.md) and [USB and physical-model evidence](../docs/device.md) |

## How to extend the annotations

The listing covers every byte of the archived application. Its instruction lines represent **48,048 bytes**, including operands of two-word instructions. `dw` directives preserve the other **2,768 bytes**. `dw` can mean tables, strings or filler. It can also mean instruction words the conservative disassembly did not resolve. A branch target, an indexed program-memory read, or a tested execution trace can establish how a word is used. The assembler's ability to decode an opcode alone does not establish that the firmware executes it.

Add a source comment where the evidence identifies the behavior. Use the byte address in notes so the same location can be found in the stock HEX. Keep an observed register access distinct from an inferred circuit role. The `18f66k22` profile fixes the assembler encoding, and the hardware register evidence narrows the controller family independently. [Microchip PIC18F87K22 family data sheet](https://ww1.microchip.com/downloads/en/DeviceDoc/PIC18F87K22-Family-Data-Sheet-30009960F.pdf)

After annotation-only edits, run the [stock verification](rebuild.md) to check every byte. The disassembly generator produces the undecorated baseline in a scratch file. Manual annotations remain in the checked-in listing. Bootloader code, physical wiring, voice clip payloads, and device-specific EEPROM contents require evidence outside this HEX. [Build and installation guide](../docs/custom-firmware.md)
