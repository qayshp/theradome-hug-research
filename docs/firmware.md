# The firmware image that HUG distributed

The Mac HUG ZIP contains `Resources/myapp.jar`. The JAR contains a file named `myTheradome.hex`. Its 111,967 bytes match the firmware HUG was written to fetch as `my.hex` from GitHub. Each contains the strings `THERADOME LH80` in the command-menu and board-test headings, at PIC program addresses `0x1C4D` and `0x1C78`. The stock archive, the updater URL in the Java code and the SHA-256 comparison establish the link between them. The investigation started with an EVO LH40. Whether that unit runs this exact image remains unknown. [Discovery record](discovery.md) · [Firmware at a pinned upstream revision](https://raw.githubusercontent.com/renuraja03/fwupdate/ad6e0558454a12b60f1da2ef65a3cfba5dc5bb26/my.hex)

## Two representations of the same application

| File | Size | SHA-256 |
|---|---:|---|
| [Original Intel HEX](../outputs/myTheradome-LH80-labeled.hex) | 111,967 bytes | `40caa7fd679e70205a8acc923f5586484884060e9ef4eb0cfaa4c4d23f120c69` |
| [Decoded application binary](../outputs/myTheradome-LH80-labeled.bin) | 50,816 bytes | `3b4eb6bdb7e535760f1ee14cd5687c8a3b20739a4ec0f2d406f5fcaeb813ef55` |

The original HEX preserves the original CRLF line endings, 794 records of 64 data bytes and one end-of-file record. The raw binary concatenates the program data in ascending address order. Its first byte belongs at `0x1900` and its last at `0xDF7F`. All addresses in that interval are populated. To locate a firmware address in the binary, subtract `0x1900`. The first instruction is the application's reset vector. The bootloader, configuration words, audio recordings and per-unit data EEPROM are outside this image. [Reconstruction notes](../model/rebuild.md) · [Selected firmware strings](../outputs/firmware-strings.txt)

## Inspecting and reproducing the files

From the repository root, use Python 3 to check the HEX and the supplied binary. The second command writes a new raw binary from the HEX. Both work offline.

```sh
python3 model/tools/rebuild.py verify outputs/myTheradome-LH80-labeled.hex --binary outputs/myTheradome-LH80-labeled.bin
python3 model/tools/rebuild.py extract --output /tmp/theradome-application.bin
```

To verify the assembly independently, run `sh model/rebuild.sh` with Python 3 and gputils on the path. It assembles the recovered source, checks all addresses and bytes against the archived HEX, then checks the decoded bytes against the supplied binary. The 18F66K22 assembler profile reproduces the instruction encoding. The register map and initialized ports favor a large-flash PIC18 K22-family controller. A physical part number or board revision would give stronger attribution. [Assembly](../model/rebuild.asm) · [Controller evidence](../model/controller.md) · [gputils](https://gputils.sourceforge.io/)

## What the host models cover

The observation guide starts with visible behavior, including the slowly changing charging light and a loud requested time announcement after selecting Soft or Silent. The Python probes walk instructions from the archived image. The C models independently describe button recognition, the charging diagnostics and voice handling. The comparisons exercise the boundaries between those paths, including the difference between automatic and requested announcements. Sensor readings, real-time scheduling and speaker output still require observations from the hardware. [Behavior guide and executable examples](../model/README.md)
