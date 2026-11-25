# Treatment history and the live countdown

On a completed session, this application writes a dated log entry, then increments its session counter. During a session it keeps the active countdown in RAM. All addresses below are byte addresses in the HUG application unless identified as EEPROM or RAM.

## EEPROM layout

| Location in EEPROM | Meaning in this image | Firmware evidence |
|---|---|---|
| Bytes 0–1 (word slot 0) | Completed-treatment counter, little endian. The viewer interprets 0xFFFF as zero. | 0x396C–3998, 0x5EE4–5F0A |
| Slots 0x00–0x0A, 0x14–0x18 and 0x19–0x1F | The diagnostic groups these as “ERROR COUNTERS,” “CALIBRATION CONSTANTS” and “MFG FLAGS,” respectively. The first group's label also covers slot 0, which separately counts completed sessions. | Label selection at 0x3008–3058, bounds and reads at 0x3086–30EC and 0x30EE–31E4 |
| Bytes 0x40–0x43 (slots 0x20 and 0x21) | Factory install date, displayed from these two words. | Label at 0x2015, print and read calls at 0x3404–341A |
| Bytes 0x44–0x47 (slots 0x22 and 0x23) | Upgrade install date, displayed from these two words. | Label at 0x1FFD, print and read calls at 0x3440–3456 |
| Bytes 0x50–0x53 (slots 0x28 and 0x29) | Two numeric components the diagnostic combines for its calibration-temperature reading. | Label at 0x2132, reads at 0x336A and 0x33A0 |
| Bytes 0x64–0x6D (slots 0x32–0x36) | Ten bytes displayed in increasing address order as the helmet serial number. | Label at 0x21B1, print loop at 0x320E–325C |
| Bytes 0xC8–0x3E7 | 50 entries of 16 bytes, stored in a ring. For zero-based session index N the location is 0xC8 + 16 × (N modulo 50). | Writer 0x8CF4–8EDA, independent reader 0xC648–C6D2 |
| Bytes 0x3E8–0x3E9 (word slot 0x1F4) | Product code revision, displayed by the firmware diagnostic. | String at 0x1F80, printed at 0x3280, followed by the read at 0x328E |

The menu prints “TREATMENTS COMPLETED LOG” at 0x383A. Its headings recovered directly from ROM include number, date, day of week, time, battery, temperature (Celsius) and laser current (mA). The writer and reader both take the unsigned remainder after division by 50. The writer uses word addresses 0x64 through 0x6B plus the ring index times eight. The reader independently calculates the same addresses. At session 51, the first record is overwritten. The final record field is written before the counter increment, so interrupted writes can leave physical bytes beyond what the count currently claims.

| Offset within record | Stored value | Encoding |
|---|---|---|
| 0–1 | Century, year within century | Packed decimal digits in each byte |
| 2–3 | Month, day of month | Packed decimal |
| 4–7 | Weekday (Sunday = 0), hour, minute, second | Packed decimal |
| 8–9 | Battery reading | Unsigned integer byte followed by decimal fraction byte |
| 10–11 | Temperature in Celsius | Same display convention |
| 12–13 | Laser current in mA | Same display convention |
| 14–15 | Written as zero | Meaning of this reserved word is unknown |

The reader at 0x39E6 loads a record. The code at 0x39EA–3B50 converts the date and time from packed decimal. Addresses 0x3B60–3C46 print each measurement as two ordinary unsigned decimal byte values separated by a decimal point. The display pads single-digit fractions to two places. The title for the battery column omits the unit, so the offline reader preserves the displayed number. The RTC load at 0xD9BE obtains the century byte from EEPROM slot 0x2A and the other clock fields from RTC registers. Local clock and week-day settings may disagree with a wall clock.

The counter increment helper at 0xD892 reads the word, maps 0xFFFF to zero, and then increments it. A count of 0xFFFE therefore increments to 0xFFFF. On the next completed session the writer starts again at ring index 0. The treatment-counter clear routine at 0xDDB4 sets slots 0 and 0x0A to 0xFFFF and leaves ring contents in place. Other call sites also write slot 0 (e.g. 0x5790 and 0x582E). Rely on the stored count when reconstructing ordinary activity, and consider interruptions, counter clears, and those other paths when interpreting surprising dumps.

## How a save completes

Both EEPROM helpers take a 16-bit **word slot**. Each doubles that number to get the physical byte address. The reader at 0xD3F8–D442 reads the low byte at twice the slot number and the high byte at the next address. The writer at 0xD6CE–D710 selects data EEPROM, enables writing and clears interrupt-enable bit INTCON.7. It starts the low-byte write, waits until the hardware clears the write-in-progress bit, advances one address, starts the high-byte write and waits again. It then disables writing and sets INTCON.7. The helper at 0xD80A–D814 performs the chip's 0x55, 0xAA unlock sequence for each byte.

This is a blocking save with two separate hardware write cycles. The function checks completion after each byte. It returns without testing the write-error flag, reading the value back or retrying. The routine's instructions give no fixed elapsed duration, and the time on a helmet was not measured. If power fails after the first byte is committed and before the second is committed, a word can contain one new byte and one old byte. A completed session requires eight such word writes for the record and a ninth for the counter. The identified completion path has no transaction marker or checksum covering those nine writes.

A separate board diagnostic prints “Write/Read/Verify 0x6336.” That routine writes the pattern across the entire treatment-log ring, slots 0x64–0x1F3, and then checks each word against it (0x4712–4760, loop bound at 0x52E4). It would overwrite prior log entries. Ordinary saves do not call this diagnostic.

The volume gesture changes RAM at 0x044, and a separate RAM byte controls the effective playback attenuation. A reset clears both. The identified volume-change routine contains no EEPROM write. A sustained hold in states 7 and 8 does increment EEPROM slot 7, but tracing it to the counter helper identifies it as a count, not the volume level. See [the volume trace](volume.md).

## Offline reader

`python model/eeprom.py path/to/data-eeprom.bin` produces JSON containing the count and up to the last 50 sessions in chronological counter order, oldest first. The input must contain data EEPROM beginning at byte zero. The HUG program HEX occupies a different memory space. The reader keeps raw record bytes along with each interpreted timestamp and measurement. Invalid dates yield a null timestamp. Entries selected by a suspect count remain visible. The original menu tests the sign bit of a 16-bit subtraction when choosing its starting row at 0x399A. The offline tool bounds output to 50 even for abnormally large counts that trip that sign test.

The reader was exercised with synthetic empty, one-entry, full-ring and wraparound dumps including counts 0, 1, 50, 51, 65534 and 65535. At 51, it returned sessions 2–51 starting at byte 216 and ending at byte 200. A packed timestamp and a battery pair of 0x03, 0x48 decoded as expected. These checks used synthetic EEPROM data.

## Countdown and spoken status

The relevant load at 0x5D9E–5DAA sets timer 0 to 600. At 0xC5BE the 16-bit value is copied into RAM 0x26F–0x270. The interrupt at 0x1942–1966 decrements it if nonzero. A button path can read the timer at 0xCA64. At expiry, the state-7 handler takes the branch at 0x5DF6, which requests state 0x0C at 0x5E04. The state-0x0C handler at 0x5EDE logs and increments the count, then requests state 5. The normal ring entry records the completion timestamp. It contains no countdown value. Reset initialization at 0xCD4E–CD58 clears this RAM. A separate power-loss resume mechanism has not been established.

The automatic time reporting path (call at 0x5E26) reaches the number/voice routine for 449, 299 or 149 timer ticks. A manual request (event 2 at 0x7360 or 0x737C) passes a nonzero argument, which also reaches this routine at other tick values. The numeric argument is the unsigned quotient of the remaining tick count divided by 30, plus one. For the three automatic cases the arguments are 15, 10 and 5, each followed by clip 0x2D. Those values, together with the 600-tick initial value, fit a 20-minute session clock with 2 seconds per tick. This timing is inferred. Audio clip words and elapsed wall time have not been measured on the helmet.
