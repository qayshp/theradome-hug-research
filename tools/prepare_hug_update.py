"""Prepare a local JAR for the known Windows HUG release without opening a serial port."""

import argparse
from binascii import unhexlify
import hashlib
import io
from pathlib import Path
import re
from zipfile import BadZipFile, ZIP_STORED, ZipFile, ZipInfo

from verify_packages import FIRMWARE, WINDOWS_LAUNCHER, members, verify_digest


APPLICATION_START = 0x1900
STOCK_END = 0xDF80  # Exclusive
TESTED_END = 0xE040  # Exclusive, including Intel HEX row padding
ROW_SIZE = 64
RESOURCE = "myTheradome.hex"
MANIFEST = "META-INF/MANIFEST.MF"
VENDOR_MEMBER_COUNT = 629
DATA_ROW = re.compile(rb":[0-9A-F]{138}")
DIGEST_HEADER = re.compile(rb"(?im)^(?:signature-version|[\w-]+-digest(?:-[\w-]+)?):")


def validate_image(firmware):
    rows = firmware.split(b"\r\n")
    if rows[-2:] != [b":00000001FF", b""]:
        raise ValueError("Use HUG's CRLF record format with one end-of-file record")
    end = APPLICATION_START + ROW_SIZE * (len(rows) - 2)
    if not STOCK_END <= end <= TESTED_END:
        raise ValueError("Expected a complete application at 0x1900 through 0xDF7F, with up to three extra rows")

    for number, row in enumerate(rows[:-2]):
        address = APPLICATION_START + number * ROW_SIZE
        if not DATA_ROW.fullmatch(row):
            raise ValueError(f"Expected an uppercase 64-byte Intel HEX row at 0x{address:04X}")
        record = unhexlify(row[1:])
        header = bytes([ROW_SIZE]) + address.to_bytes(2, "big") + b"\x00"
        if record[:4] != header or sum(record) & 0xFF:
            raise ValueError(f"Invalid address, type, or checksum in the row for 0x{address:04X}")
    return end


def check_manifest(archive):
    manifest = archive.read(MANIFEST)
    if b"Main-Class: com.screens.Splash" not in manifest.splitlines():
        raise ValueError("Unexpected HUG main class")
    for name in archive.namelist():
        path = name.upper()
        if path.startswith("META-INF/"):
            leaf = path.rsplit("/", 1)[-1]
            if leaf.startswith("SIG-") or leaf.endswith((".SF", ".RSA", ".DSA", ".EC")):
                raise ValueError("Changing a signed JAR would invalidate its signature")
    if DIGEST_HEADER.search(manifest):
        raise ValueError("Changing this JAR could invalidate its manifest digests")


def prepare_bundle(launcher, firmware):
    verify_digest("Windows launcher", launcher, WINDOWS_LAUNCHER)
    buffer = io.BytesIO()
    with ZipFile(io.BytesIO(launcher)) as source:
        original_members = members(source)
        if len(original_members) != VENDOR_MEMBER_COUNT:
            raise ValueError("Unexpected number of HUG archive members")
        verify_digest("Original HUG firmware resource", source.read(RESOURCE), FIRMWARE)
        check_manifest(source)

        # Preserve vendor member order and attributes. Stored entries avoid
        # byte differences between releases of the compressor.
        with ZipFile(buffer, "w", ZIP_STORED) as target:
            for entry in source.infolist():
                member = ZipInfo(entry.filename)
                member.create_system = entry.create_system
                member.external_attr = entry.external_attr
                data = firmware if entry.filename == RESOURCE else source.read(entry)
                target.writestr(member, data)

    data = buffer.getvalue()
    original_members[RESOURCE] = hashlib.sha256(firmware).digest()
    with ZipFile(io.BytesIO(data)) as prepared:
        if members(prepared) != original_members:
            raise ValueError("Prepared bundle did not preserve the expected HUG members")
    return data


def write_new_bundle(output, bundle):
    stream = output.open("xb")
    try:
        with stream:
            stream.write(bundle)
    except BaseException:
        output.unlink(missing_ok=True)
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("launcher", type=Path, help="the original installed HugUpgrade.exe")
    parser.add_argument("hex", type=Path, help="a local Intel HEX in the archived HUG record format")
    parser.add_argument("output", type=Path, help="a new JAR path")
    args = parser.parse_args()
    if args.output.suffix.lower() != ".jar":
        parser.error("the output must be a .jar file")
    try:
        firmware = args.hex.read_bytes()
        end = validate_image(firmware)
        bundle = prepare_bundle(args.launcher.read_bytes(), firmware)
        write_new_bundle(args.output, bundle)
    except (BadZipFile, KeyError, OSError, ValueError) as error:
        parser.exit(1, f"Bundle preparation failed: {error}\n")
    print(f"Application records: 0x{APPLICATION_START:04X} through 0x{end - 1:04X}")
    print(f"Input HEX SHA-256: {hashlib.sha256(firmware).hexdigest()}")
    print(f"Prepared {args.output} (JAR SHA-256 {hashlib.sha256(bundle).hexdigest()})")


if __name__ == "__main__":
    main()
