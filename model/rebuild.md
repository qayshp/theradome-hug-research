# Rebuilding the HUG application image

Install Python 3 and gputils with `gpasm` on the shell's path. Run `sh model/rebuild.sh` from the repository root in a Unix shell, including Linux or Windows Subsystem for Linux. It assembles `model/rebuild.asm` into a new temporary directory. The verifier checks every populated address against the original Intel HEX, then compares the assembled program bytes with the included flat binary.

## What was recovered

The assembler input contains 19,367 PIC18 instructions spanning 48,048 bytes, and 1,384 literal words covering the other 2,768 bytes. All direct branch and call targets within the application have labels. The classifier follows both sides of conditions and instruction skips from the relocated reset and interrupt vectors. Indirect control flow and embedded table pointers still need hand analysis. Literal words include strings, padding and possible additional code. These counts measure representation in the source. They do not measure how much behavior is understood.

The assembly uses numeric peripheral operands. The 18F66K22 assembler profile supplies the standard PIC18 instruction encoding. Pin names, package, EEPROM state, oscillator settings and hardware identity need independent evidence. Displaced code can leave embedded table addresses stale even when an assembler resolves its branch labels. Original byte addresses appear in comments for matching the behavior notes and checking layout.

## Verification

The unedited source assembled with gpasm 1.4.0, 0 errors and 0 warnings. The independent Intel HEX reader checked the record checksums, lengths, start and end addresses, and all 50,816 bytes at 0x1900 through 0xDF7F inclusive. The byte stream in ascending address order hashed to SHA-256 `3b4eb6bdb7e535760f1ee14cd5687c8a3b20739a4ec0f2d406f5fcaeb813ef55`. The original HEX text file hashed to `40caa7fd679e70205a8acc923f5586484884060e9ef4eb0cfaa4c4d23f120c69`. Different Intel HEX record formatting can represent identical memory.

In a disposable copy, changing the hexadecimal digit formatter's literal at original address 0xDED8 from 0x37 to 0x57 assembled successfully. The verifier detected exactly one different byte, at 0xDED8, as predicted for the uppercase-to-lowercase change. This checked an actual source edit offline. The retained source is the original baseline.

## Provenance and regeneration

`python3 model/tools/rebuild.py generate --output /tmp/regenerated.asm` uses `gpdasm --strict` and the original HEX to recreate the assembly source in a temporary file. Rebuilding an edited assembly file uses the assembler directly. Supply `--gpdasm PATH` to select a specific disassembler.

The reference build used gpasm and gpdasm from gputils 1.4.0 on Ubuntu arm64. Build verification covers the HUG application address range. The bootloader, configuration words, and device-specific EEPROM contents occupy separate ranges outside this HEX. [gputils project](https://gputils.sourceforge.io/)
