"""Compare known HUG packages and their bundled firmware without launching them."""

import argparse
import hashlib
import io
from pathlib import Path
from zipfile import BadZipFile, ZipFile


MAC_PACKAGE = "fb6d297db37e1c89431c874d2f2bb34805717bb22159a022cc945afa2e682e3d"
MAC_JAR = "27ddcc2f7fb6f590e9ef0977327d1150ffb20d792a3f5b59ee7a8647a37c37b9"
WINDOWS_INSTALLER = "238634453318259dbd0fb07993a2c41c4f27ed11dc5102ab35f061dd2a2b3d25"
WINDOWS_LAUNCHER = "890d6a2960c06677f7988a6a474ec923f8770bd32fcb888660c89215b7cc9b1d"
FIRMWARE = "40caa7fd679e70205a8acc923f5586484884060e9ef4eb0cfaa4c4d23f120c69"
HEX_PATH = Path(__file__).resolve().parents[1] / "outputs/myTheradome-LH80-labeled.hex"


def verify_digest(label, contents, expected):
    actual = hashlib.sha256(contents).hexdigest()
    if actual != expected:
        raise ValueError(f"{label} differs from the archived build (SHA-256 {actual})")
    print(f"{label}: {len(contents):,} bytes  {actual}")


def members(archive):
    result = {}
    for entry in archive.infolist():
        if entry.filename in result:
            raise ValueError(f"Duplicate member {entry.filename}")
        result[entry.filename] = hashlib.sha256(archive.read(entry)).digest()
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mac_zip", type=Path, help="HugApplication-V200.zip")
    parser.add_argument("--windows-installer", type=Path, help="HugApplication.exe")
    parser.add_argument("--windows-launcher", type=Path, help="HugUpgrade.exe extracted from the installer")
    args = parser.parse_args()
    try:
        mac_zip = args.mac_zip.read_bytes()
        verify_digest("Mac package", mac_zip, MAC_PACKAGE)
        with ZipFile(io.BytesIO(mac_zip)) as package:
            jar_data = package.read("Resources/myapp.jar")
        verify_digest("Mac application JAR", jar_data, MAC_JAR)
        with ZipFile(io.BytesIO(jar_data)) as jar:
            bundled_hex = jar.read("myTheradome.hex")
            verify_digest("Bundled firmware HEX", bundled_hex, FIRMWARE)
            if bundled_hex != HEX_PATH.read_bytes():
                raise ValueError("Repository HEX differs from the bundled firmware")
            print("Repository HEX matches the Mac application resource.")
            if args.windows_launcher:
                launcher = args.windows_launcher.read_bytes()
                verify_digest("Windows launcher", launcher, WINDOWS_LAUNCHER)
                with ZipFile(io.BytesIO(launcher)) as embedded:
                    if members(jar) != members(embedded):
                        raise ValueError("Windows and Mac JAR member names or contents differ")
                    print(f"All {len(jar.infolist())} Windows and Mac JAR members match.")
        if args.windows_installer:
            verify_digest("Windows installer", args.windows_installer.read_bytes(), WINDOWS_INSTALLER)
    except (OSError, ValueError, BadZipFile, KeyError) as error:
        parser.exit(1, f"Verification failed: {error}\n")


if __name__ == "__main__":
    main()
