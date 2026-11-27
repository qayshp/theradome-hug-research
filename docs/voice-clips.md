# Voice clip directory and spoken words

The revision-126 application assembles announcements from numbered recordings in separate SPI flash. The application HEX contains the playback routines, clip IDs, and number-rendering code. The LH40 directory capture and the call sites establish which clips the firmware requests. The table below marks inferred wording where the individual recordings have yet to be heard in isolation. [Voice storage and playback](../model/firmware-io-map.md#voice-storage-and-playback)

## Banks, layout, and clip addresses

| Item | Address or representation |
| --- | --- |
| Voice bank | The firmware reads RAM byte `0x045` and shifts it left by 20 bits to form the bank base, a stride of 1 MiB. It sends three address bytes to the SPI memory. The number of physical banks and their recordings beyond the inspected layout are unverified. |
| Directory entry | The four bytes at `bank_base + 4 × clip_id` contain two big-endian 16-bit values, the starting page and compressed length in bytes. |
| Recording | The player reads from `bank_base + 256 × starting_page`. Each compressed byte supplies two 4-bit ADPCM samples. |
| Diagnostic display | The clip index is printed in decimal. The page and size are hexadecimal, with negative values displayed for words interpreted as signed. |

[Directory address and field reads](../model/rebuild.asm#L17818-L17935) · [Playback address calculation](../model/rebuild.asm#L18960-L19037) · [Sample decoder](../model/rebuild.asm#L13901-L13934) · [Diagnostic formatting](../model/rebuild.asm#L3182-L3240)

The startup clear includes the bank selector, so the default voice bank is 0. The diagnostic prints page offsets within the selected bank. [Startup RAM clear](../model/rebuild.asm#L20434-L20439)

The tested LH40's captured layout has 50 coherent entries at decimal indices 0–49 (`0x00–0x31`). Each next entry begins at the first page boundary after the previous recording. Their compressed lengths sum to **507,336 bytes**. The scan prints 255 rows in total. Rows 50–63 return irregular values, and rows 64–254 all return `-1` for both fields, corresponding to erased `0xFFFF` words. The capture supports 50 recordings in this layout. [Live voice readout](volume-settings.md#where-the-voice-recordings-are-stored)

For example, clip `0x00` has directory bytes `00 10 21 A9`. It starts on page `0x0010`, at offset `0x1000` within the bank. Its length `0x21A9` is 8,617 bytes, which produces 17,234 decoded samples. Two reads of that waveform matched. [Live waveform check](verification.md#live-evo-lh40-tests)

The firmware also checks for another layout. When clip `0x00` starts on page `0x0010` and clip `0x32` starts on page `0x0800`, the startup code can select audio variant 2, and the first-wave display then uses `0x32`. Both checks read the same directory through the same bank selector. The tested unit reported page `0x6929` for `0x32`, so this capture matches the layout without the variant-2 extension. The variant byte at `0x2B8` controls which call sites run, and the bank byte at `0x045` controls the SPI address calculation. [Layout detection](../model/rebuild.asm#L6491-L6516) · [Diagnostic waveform selection](../model/rebuild.asm#L3267-L3280)

## Spoken numbers

The routine at `0xA4C0` takes an unsigned 16-bit number from RAM `0x0C8–0x0C9`. It obtains the decimal digits, packs them into RAM `0x0D0–0x0D2`, then selects clips at `0xA562`. The number map follows directly from the clip arithmetic. [Input and decimal conversion](../model/rebuild.asm#L15674-L15758) · [Clip composition](../model/rebuild.asm#L15759-L15871)

| Clip ID | Spoken value or role |
| --- | --- |
| `0x00–0x09` | Zero through nine. The ID equals the digit. |
| `0x0A–0x13` | Ten through nineteen. The ID equals the number. |
| `0x14–0x1B` | Twenty, thirty, forty, fifty, sixty, seventy, eighty, ninety. The ID is `0x12 + tens_digit`. |
| `0x1C` | Hundred, following the nonzero hundreds digit. |
| `0x1D` | Thousand, following the thousands group. |

The renderer speaks the thousands group, then the hundreds digit if present, then the last two digits. A number below 20 uses one clip. A larger two-digit group uses its tens clip and a ones clip when needed. Empty groups are skipped, and the value zero has its own playback path. An offline check ran the original clip-selection instructions for all **65,536** unsigned input values, supplying the packed digits and capturing the player calls. Run the [check](../model/check_voice_numbers.py) from the repository root.

```sh
python3 -B model/check_voice_numbers.py
```

| Number | Clip sequence in hex |
| ---: | --- |
| 0 | `00` |
| 15 | `0F` |
| 20 | `14` |
| 42 | `16 02` |
| 105 | `01 1C 05` |
| 126 | `01 1C 14 06` |
| 1,001 | `01 1D 01` |
| 12,000 | `0C 1D` |
| 65,535 | `18 05 1D 05 1C 15 05` |

The revision routine provides other clips around the numbers. The observed “Theradome Revision 126.6” corresponds to `31 2F 01 1C 14 06 29 06`. [Revision composition](../model/rebuild.asm#L20779-L20814)

## Other clips on the tested layout

The call sites establish the numeric sequence and purpose. Words in quotation marks are inferred from heard sentences, the surrounding sequence, and reuse of the same fragments. A description without a quotation describes the call context and leaves the wording open.

| ID | Word or role | Evidence |
| --- | --- | --- |
| `0x1E` | Temperature status fragment | Played in the temperature alert before `0x22`. [Temperature calls](../model/rebuild.asm#L11792-L11807) |
| `0x1F` | Error fragment, likely “error” | Follows the system fragment in the core-voltage fault path. Also appears in battery and calibration fault sequences. [Core-voltage fault](../model/rebuild.asm#L11708-L11733) |
| `0x20` | “Place on head” | The second-stage ready prompt and the off-head reminder request the same clip. [Ready prompt](../model/rebuild.asm#L7540-L7549) · [Off-head reminder](../model/rebuild.asm#L7642-L7649) |
| `0x21` | Battery status fragment | Used by the low-battery warning and by battery faults in the captured layout. [Battery warning and fault](../model/rebuild.asm#L11592-L11668) |
| `0x22` | High-condition status fragment, likely “high” | Used in the battery upper-limit and one temperature-alert sequence. [Battery upper limit](../model/rebuild.asm#L11548-L11578) · [Temperature calls](../model/rebuild.asm#L11792-L11807) |
| `0x23` | “Completed” | Follows the treatment count in the completion and count-read paths. [Counted announcement](../model/rebuild.asm#L20332-L20360) |
| `0x24` | “Started” | Follows the number in the treatment-start announcement. Also follows the calibration fragment at calibration start. [Numbered start](../model/rebuild.asm#L20332-L20350) · [Calibration start](../model/rebuild.asm#L6989-L7004) |
| `0x25` | Laser status fragment, likely “laser” | Appears in the laser-current fault path and a laser calibration path. [Laser-current fault](../model/rebuild.asm#L11872-L11882) · [Calibration calls](../model/rebuild.asm#L6880-L6894) |
| `0x26` | “System” | Opens system standby speech and the internal supply fault. [Standby entry](../model/rebuild.asm#L8261-L8269) · [Core-voltage fault](../model/rebuild.asm#L11717-L11732) |
| `0x27` | “Treatment” | Opens the counted treatment, resume, and stop phrases. [Counted announcement](../model/rebuild.asm#L20332-L20337) · [Stop speech](../model/rebuild.asm#L7710-L7719) |
| `0x28` | “Aborted” | Follows `0x27` in treatment stop. [Stop speech](../model/rebuild.asm#L7710-L7719) |
| `0x29` | Decimal separator, “point” | Separates the application and bootloader numbers in the observed revision speech. [Revision sequence](../model/rebuild.asm#L20804-L20814) |
| `0x2A` | “Resumed” | Follows `0x27` for treatment resume and `0x26` for system resume. [Treatment resume](../model/rebuild.asm#L7631-L7641) · [System resume](../model/rebuild.asm#L7725-L7751) |
| `0x2B` | “Paused” | Follows `0x26` in system standby speech. [Standby entry](../model/rebuild.asm#L8261-L8269) |
| `0x2C` | Warning fragment | Appears in continuing battery and temperature alerts. [Battery warning](../model/rebuild.asm#L11630-L11657) · [Temperature warning](../model/rebuild.asm#L11792-L11807) |
| `0x2D` | Remaining-time suffix | Follows the spoken number in both remaining-time paths. The exact suffix wording is still open. [Countdown call](../model/rebuild.asm#L20098-L20106) |
| `0x2E` | “Ready for” fragment | Precedes the treatment fragment at startup and the calibration fragment when calibration is ready. [Ready treatment](../model/rebuild.asm#L7523-L7535) · [Ready calibration](../model/rebuild.asm#L6811-L6820) |
| `0x2F` | “Revision” | Follows the brand before the first number in observed revision speech. [Revision sequence](../model/rebuild.asm#L20792-L20806) |
| `0x30` | “Calibration” | Opens the calibration-start announcement before the started fragment. [Calibration start](../model/rebuild.asm#L6989-L7004) |
| `0x31` | “Theradome” | The volume confirmation uses this single clip, and the observed revision speech opens with it. [Volume feedback](../model/rebuild.asm#L10312-L10317) · [Revision sequence](../model/rebuild.asm#L20792-L20796) |

The alternate variant has additional call sites. `0x32` and `0x33` occur in battery alerts. `0x34` supplements treatment completion and `0x35` supplements treatment start. `0x36` appears in variant warning sequences. `0x37` appears in a variant failed-check and charge-timeout path. The variant-2 active path can call a separate play routine with `0x38`. The speech in those additional recordings remains unverified. [Variant battery warning](../model/rebuild.asm#L11615-L11657) · [Completion](../model/rebuild.asm#L7691-L7702) · [Start](../model/rebuild.asm#L7501-L7511) · [Charge timeout](../model/rebuild.asm#L7872-L7888) · [Variant active call](../model/rebuild.asm#L7578-L7600)

An exceptional battery branch also requests ID `0xFF`. The live directory scan ended at `0xFE`, so the recording or special behavior behind that request remains open. [Exceptional battery path](../model/rebuild.asm#L11669-L11687)
