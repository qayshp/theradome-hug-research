# Requested remaining-time volume

This experimental offline build is based on the archived HUG application revision 126, labeled LH80. In active and paused treatment, a single press requests the remaining time at the selected volume. The spoken number and its suffix both use the selected gain. After the report, the handler restores the previous effective gain.

| Selected level | Digital shift during the report |
| --- | ---: |
| Loud | 0 |
| Medium | 1 |
| Soft | 3 |
| Silent | 14 |

The scope is the button-requested report. Automatic countdown announcements follow the stock gain and timing rules. The build retains the stock handling of volume persistence, double presses, long presses and other announcements. The patch consolidates the duplicated button cases in the existing space. It changes 48 bytes, all within the original state 7 and state 8 button handlers at `0x7360–0x7397`.

## Image and verification

- `LH80-126-requested-volume-experimental.hex` contains 50,816 application bytes at `0x1900–0xDF7F`. It uses the stock 794 data records of 64 bytes, one end record, CRLF and valid Intel HEX checksums.
- HEX SHA-256 is `43cc538cf91870e9b0f18f3d5dda60d85923c1c613c95f6281a2409fda2dc967`.
- Decoded application SHA-256 is `5bd5c7ecf16cc5851b1a90652883b9104a592aaa45a05dac259057a1def92114`.
- The verifier checks 1,008 instruction-level cases. The 416 requested reports cover all four selected levels paired with all four saved effective gains, including mismatches. It captures both speech requests and checks post-call RAM. It also compares 416 automatic paths, 32 double presses, 128 long presses and 16 out-of-range selection cases against stock.

The test runs the assembled instruction words on the host and captures speech calls. This first experiment remained offline. A later build tested on the physical LH40 also added persistence and automatic countdown behavior. The installed application was never read back for comparison. [Later LH40 test](../../../docs/volume-settings.md#live-evo-lh40-test-on-november-26-2026)

## Reproduce offline

From a disposable checkout of the research repository with Python 3 and `gpasm` installed:

```sh
package=model/experiments/requested-volume
build=$(mktemp -d)
git apply --check "$package/requested-volume.patch"
git apply "$package/requested-volume.patch"
gpasm -o "$build/assembled.hex" model/rebuild.asm
python3 -B "$package/pack-hex.py" . "$build/assembled.hex" "$build/repacked.hex"
python3 -B "$package/verify-volume.py" . "$build/repacked.hex"
```

The stock HEX in the research checkout is the verifier's comparison source. The bootloader, chip configuration words, device EEPROM and voice recordings are separate from this application image.
