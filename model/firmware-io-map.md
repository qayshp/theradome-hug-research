# Firmware I/O and persistent data

This map follows the archived HUG revision-126 application. The addresses without another qualifier are hexadecimal byte addresses in PIC18 program memory, matching the comments in [the rebuildable listing](rebuild.asm). EEPROM and RAM have their own address spaces. Peripheral names use the PIC18F87K22 family register map. The firmware evidence supports that family, and the exact controller part and board wiring remain unverified on the examined LH40. [Controller identification](controller.md) · [Microchip peripheral reference](https://ww1.microchip.com/downloads/en/DeviceDoc/PIC18F87K22-Family-Data-Sheet-30009960F.pdf)

## Voice storage and playback

| Program address | Role and evidence |
| --- | --- |
| [`0xA4C0`](rebuild.asm#L15674) | Converts a number to decimal and chooses speech clips for its components. The revision and remaining-time routines call the same number renderer. |
| [`0xD038`](rebuild.asm#L20779) | Composes the spoken revision from several clips, the application revision constant, and a bootloader value. The bootloader read uses program memory immediately below the archived application. |
| [`0xCA60`](rebuild.asm#L20049) | Reads the treatment countdown and selects the scheduled or requested announcement. It calls the number renderer and plays the trailing phrase. |
| [`0x94F2`](rebuild.asm#L13824) | Plays one numbered clip. It returns if audio is disabled. The active path prepares playback, brings the serial memory out of low power, reads the clip and ends by returning the serial memory to low power. |
| [`0xB6DA`](rebuild.asm#L17818) | Reads a four-byte directory entry with two big-endian fields. The first field is the clip's starting page and the second is its compressed byte length. The routine multiplies the clip index by four. Both this routine and the clip reader incorporate RAM `0x045` in the SPI address, so the full contents and purpose of any alternate address banks remain unverified. |
| [`0xC104`](rebuild.asm#L18960) | Converts a directory page number into a byte address and starts a sequential read. The shift by eight makes a page 256 bytes. The controller transfers through MSSP2 and polls the SPI transfer flag. |
| [`0x9596`](rebuild.asm#L13901), [`0xAF22`](rebuild.asm#L16916) | Processes the upper nibble before the lower nibble of each compressed byte. The decoder uses a 49-entry step table at `0x1A48` and a 16-entry index adjustment table at `0x1AAA`, which support an adaptive differential PCM interpretation. The decoded sample and step index stay in RAM. |
| [`0x96C4`](rebuild.asm#L14051), [`0x9672`](rebuild.asm#L14010) | Shifts the sample according to RAM `0x2B9` and then maps ten sample bits into CCP3 duty registers. The playback loop polls Timer2 match before each sample update. CCP8 and CCP9 instead drive the two switch LED channels. |

The application contains clip numbers, decoding logic and tables. The recordings are read from a separate SPI address space. Diagnostics on the tested LH40 returned 255 directory slots and two matching reads of the first waveform. Its reported length of 8,617 compressed bytes matches the 17,234 decoded samples expected from two nibbles per byte. A directory slot count does not establish how many slots are populated. The precise flash part, original encoder and complete phrase inventory remain unidentified. [Live voice readout](../docs/volume-settings.md#where-the-voice-recordings-are-stored) · [Clip directory and spoken words](../docs/voice-clips.md) · [Voice control and gain](volume.md) · [Switch LED waveform](indicator.md)

The stock voice selection at RAM `0x044` and the shift count at `0x2B9` have different lifetimes. Some paths temporarily use zero attenuation, including the path that speaks the revision on a long hold. The diagnostic waveform shows samples after the shift, so it can report effective gain during that read without revealing the selected level. [Gain behavior](volume.md#ways-a-prompt-can-sound-loud) · [Physical waveform checks](../docs/usb-clock-details.md#diagnostic-reads)

## USB bridge, application UART and diagnostics

The USB device presented to the computer is the FTDI bridge. The archived application exchanges bytes via EUSART1. It uses the PIC UART registers `RCSTA1`, `TXSTA1`, `TXREG1` and `RCREG1` in the same pathways that drive the observed COM-port dialogue. USB enumeration establishes that the bridge is available. The application's ability to answer depends on its current state. [Hardware and USB evidence](../docs/device.md) · [Observed COM7 sessions](../docs/usb-clock-details.md#what-the-com7-sessions-showed)

| Program address | Role and evidence |
| --- | --- |
| [`0x742E`](rebuild.asm#L10176), [`0xC9E0`](rebuild.asm#L19975) | In the traced long-hold state, the application speaks the revision and then configures the UART receive window. The timing branch waits for either a software receive flag or a timer flag. The timeout path shuts the UART down. |
| [`0xD206`](rebuild.asm#L21015), [`0x2456`](rebuild.asm#L1501) | The application starts the Board Test handshake only after input arrives. The board-test entry builds the banner from the firmware's string table and enters the dispatcher. Speaking the revision alone sends no serial version bytes on this path. |
| [`0x5054`](rebuild.asm#L6063), [`0x2FEA`](rebuild.asm#L2639), [`0x3782`](rebuild.asm#L3441) | The dispatcher interprets the session command and routes it to a specific view or action. These two views produce the selected EEPROM parameter display and completed-treatment log. Each formats a defined set of fields. |
| [`0x3764`](rebuild.asm#L3430), [`0x96CC`](rebuild.asm#L14056) | The voice diagnostic branches into the directory and first-wave reader, which emits decoded sample values instead of updating PWM for each sample. |
| [`0xCDB6`](rebuild.asm#L20470), [`0xDC1E`](rebuild.asm#L22159) | Common string and byte transmit helpers. String output selects either program reads through `TBLPTR` or indirect data reads through FSR0 according to the address. It stops at the string terminator. The byte helper polls the transmit flag. |
| [`0x42BE`](rebuild.asm#L4624) | The archived application acknowledges its accepted firmware-update handoff and resets. The bootloader has a separate address range. The archived application provides no direct evidence about the implementation of its write or recovery routines. |

The live diagnostics read selected parameters, timestamps, alarms, completed sessions, and waveform data. The command trace has no identified general RAM, flash, or EEPROM export in that menu. Diagnostics can also change board state. The published capture covers the named reads rather than arbitrary access to the controller. [Diagnostic reads](../docs/usb-clock-details.md#diagnostic-reads) · [Archive boundaries](../docs/firmware.md) · [Recovery evidence](../docs/verification.md#backup-and-recovery)

## Real-time clock and alarms

The application enables the PIC's RTCC peripheral during startup. It reads packed decimal fields through the RTCC value register window and obtains the century from the low byte of EEPROM slot `0x2A`. The code at `0xD9BE` sets the RTCC window pointer and reads the full set of fields. In the K22 register map, accessing the high byte advances the read through the next pair. [RTCC enable](rebuild.asm#L22514) · [Clock read and RAM copies](rebuild.asm#L21895-L21916) · [Microchip clock register windows](https://ww1.microchip.com/downloads/en/DeviceDoc/PIC18F87K22-Family-Data-Sheet-30009960F.pdf#page=231)

| Clock field | RAM copy after the read |
| --- | --- |
| Century | `0x2AE` |
| Year within century | `0x2BA` |
| Month, day | `0x2B2`, `0x2B0` |
| Weekday, hour | `0x04A`, `0x04B` |
| Minute, second | `0x04C`, `0x2B7` |

The write routine at [`0xD3A8`](rebuild.asm#L21201) saves the century first, then writes the clock fields through the address window. The archive shows neither a hardware century field nor a time-zone conversion at this boundary. The live test established that the clock ran before sync and that the setter and readback worked on the tested unit. Retention through complete power removal remains untested. [Clock and alarm observations](../docs/usb-clock-details.md#why-the-clock-had-a-plausible-date) · [Clock-sync check](../docs/usb-clock-details.md#notes-for-a-clock-sync-tool)

The firmware has two schedule concepts and one hardware alarm. Startup loads saved settings from EEPROM slots `0x2B` through `0x2E` at [`0xC7DA`](rebuild.asm#L19731) and clears the software enable flag. The shared hardware programming helper at [`0xCF02`](rebuild.asm#L20644) checks that flag, copies values through the alarm register window, and enables the RTCC interrupt. Separate foreground paths interpret a reminder interval in days and a charging timeout in hours. The observed time read and set did not exercise those alarm events on the physical unit. The behavior was traced in the archived application. [Alarm checks and persistence](../docs/usb-clock-details.md#alarm-state-and-partial-writes) · [Reminder path](rebuild.asm#L8152) · [Charging timeout](rebuild.asm#L7872)

## EEPROM, calibration and recorded measurements

| Program address | Role and evidence |
| --- | --- |
| [`0xD3F8`](rebuild.asm#L21234), [`0xD6CE`](rebuild.asm#L21582) | Read and write a little-endian data EEPROM word. The adapter converts a word slot into two successive byte addresses. A write waits after each byte. The helper has no readback, retry, or two-byte transaction marker. |
| [`0xD892`](rebuild.asm#L21804) | Increments a persistent counter. Its erased-word convention (`0xFFFF` means zero before incrementing) belongs to the counter caller, rather than to the EEPROM read itself. |
| [`0xCF6A`](rebuild.asm#L20696) | Loads five saved calibration words from EEPROM to RAM for the board measurement paths. Other service and startup code reads settings and flags separately. |
| [`0x5EDE`](rebuild.asm#L7669), [`0x8CF4`](rebuild.asm#L13014) | Normal completion writes a timestamp and finish measurements to a 50-record ring. The record consists of eight word writes, followed by a separate count update. A log entry represents completion on the device clock. |
| [`0xC648`](rebuild.asm#L19553) | Reads a ring record using the corresponding modulo-50 address calculation. Formatting the record for the serial menu is a separate operation. |

The stock data map includes factory and upgrade dates, calibration words, flags, revision and per-device identity, as well as treatment history. This storage sits outside the application HEX. Stock volume selection is RAM state. The published experimental overlay uses a data EEPROM byte for volume and has a different startup and save path from the original. [EEPROM layout and write behavior](eeprom.md) · [Treatment timestamps and readings](../docs/treatment-analysis.md#log-format-and-timestamps) · [Experimental preference](../docs/volume-settings.md#assembly-and-storage)

## Analog acquisition and charger feedback

[`0xB076`](rebuild.asm#L17077) is the shared ADC front end. It loads the channel selector, chooses reference and acquisition settings according to the selector, and starts the conversions. At [`0xB118`](rebuild.asm#L17164), the routine polls each conversion, accumulates the sign-extended result, and divides the sum by 20. The callers convert the averaged result for their own use and cache the three-byte values below. These addresses refer to values stored in RAM, often reused by diagnostic displays and treatment logging. [Microchip ADC selection and reference fields](https://ww1.microchip.com/downloads/en/DeviceDoc/PIC18F87K22-Family-Data-Sheet-30009960F.pdf#page=343)

| Measurement | Firmware evidence |
| --- | --- |
| Battery, RAM `0x2D0–0x2D2` | [`0x9302`](rebuild.asm#L13645) reads AN4 in both ordinary measurement and calibration. The alternate calibration selector is remapped to AN4 by the ADC front end and changes the configured positive reference. Calibration word `0x14` adjusts reported pack voltage. The battery fade consumes that same value. |
| Internal core voltage, RAM `0x2D3–0x2D5` | [`0xADB6`](rebuild.asm#L16791) selects internal ADC input 30, which the K22 data sheet labels `VDDCORE`. Its conversion also uses the battery estimate and calibration word `0x15`. The board's supply topology and absolute measurement error remain unknown. |
| Laser-current feedback, RAM `0x2D6–0x2D8` | [`0x8F22`](rebuild.asm#L13218) selects AN8. The treatment measurement uses the internal 2.048 V reference and a signed correction from calibration word `0x17`. The application compares the converted result with its target band. The channel establishes one feedback measurement, with physical current-sense wiring still unidentified. |
| Proximity-related reading, RAM `0x2D9–0x2DB` | [`0xA32A`](rebuild.asm#L15524) takes a reading after a timed setup and selects one of two ADC routes. Both resolve to AN5 once the ADC helper's remapping is accounted for. The calibration path saves correction word `0x18`. The code associates this quantity with the proximity diagnostic. The sensor circuitry and transfer function remain unidentified. |
| Temperature estimate, RAM `0x2DC–0x2DE` | [`0x7D4C`](rebuild.asm#L11234) makes a timed acquisition on external channel AN7. Its conversion writes the quantity later printed as temperature and saved at treatment finish. The assembly does not establish the physical sensor's part or location. |

The charger state logic separately reads two digital inputs. Their combinations feed diagnostic messages and state changes. The battery measurement helps determine the fade's color, while the charge-complete indication depends on the charge-state path. A before-and-after calibration readout shows offsets saved by the firmware, with neither a measurement of incoming USB current nor proof of battery capacity. [Charger input cases](inputs.md#what-the-charging-diagnostic-prints) · [Light sequence and calibration comparison](../docs/volume-settings.md#battery-reference-and-calibration-status) · [Treatment voltage and current analysis](../docs/treatment-analysis.md#what-the-current-reading-represents)

### How the built-in calibration runs

The routine is in the archived PIC18 application. HUG transfers the application. The controller reads the charger's two status inputs and the ADC, runs the checks below, and writes its data EEPROM. The charger supplies the status signals that let the application advance. The archived image covers the routines and their constants. The sensors, charger circuitry and unit's stored values are separate. [Application coverage](../docs/firmware.md) · [Calibration start and charger gate](rebuild.asm#L6989-L7033)

The calibration-ready state can announce “Calibration started” when it detects charger power. Before it takes the first calibration sample, it waits for the input combination displayed as “Charged.” At that point it fills the five calibration words with `FFFF`, reloads them into RAM, and proceeds with the following checks. [Charger status cases](inputs.md#what-the-charging-diagnostic-prints) · [Clearing the five words](rebuild.asm#L22230-L22247)

| Step | What the application measures and saves |
| --- | --- |
| Battery voltage | Samples AN4 under the calibration reference setup. It computes field 20 against a fixed reference equivalent to about 4.20 V and checks the result. The saved value corrects future voltage readings. [Battery acquisition and offset](rebuild.asm#L13645) |
| Internal core voltage | Samples the controller's internal `VDDCORE` channel. The calculation uses the battery estimate to express a nominal 3.3 V reference in ADC counts, then compares that reference with the core-channel sample to compute field 21. [Core-voltage acquisition and offset](rebuild.asm#L16791) |
| Temperature | Samples and checks AN7. Separately, it writes the cached calibration-temperature reference to words `0x28` and `0x29`, the pair shown in diagnostics. The normal sequence calls the helper with a mode that skips recomputing field 22, so the earlier `FFFF` remains and shows as signed `−1`. The normal measurement uses it as a signed raw correction. [Temperature check and record](rebuild.asm#L7162-L7253) · [Temperature helper](rebuild.asm#L11234-L11250) |
| Proximity | Samples the proximity-related ADC input, subtracts the average from a target of 530 raw counts, saves the signed correction in field 24 and checks the result. The physical sensor and its transfer function remain unidentified. [Proximity check](rebuild.asm#L7254-L7308) · [Proximity measurement](rebuild.asm#L15524) |
| Laser-current feedback | Takes one reading with the drive disabled, then takes a separate driven reading. In the active feedback branch it saves field 23 by comparing the averaged raw reading with the 10.50 mA reference in the conversion's units, then checks the result. This measures one electrical feedback channel. Optical output and individual emitters were unmeasured. [Off and driven checks](rebuild.asm#L7310-L7405) · [Feedback offset calculation](rebuild.asm#L13218-L13263) |

The routine saves offsets during individual steps. A failed later check can leave earlier writes in place. On the successful path it requests “Calibration complete,” writes `A5A5` to field 29, reloads the saved offsets and returns through startup. This trace explains the completion marker and measurement corrections visible in the diagnostic display. [Completion handler](rebuild.asm#L7406-L7434) · [Live snapshots across updates](../docs/volume-settings.md#calibration-snapshots-across-updates)
