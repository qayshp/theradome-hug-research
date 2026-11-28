"""Open the LH40 desktop app without a console window on Windows."""

from pathlib import Path
import sys


def main():
    sys.path.insert(0, str(Path(__file__).resolve().parent / "tools"))
    try:
        from lh40_desktop import main as open_desktop
    except ImportError as error:
        message = (
            "The LH40 History app could not start. Check that the whole repository "
            "is extracted and that Python 3.10 or later includes Tcl/Tk.\n\n"
            f"{error}"
        )
        if sys.platform == "win32":
            from ctypes import windll

            windll.user32.MessageBoxW(None, message, "LH40 History", 0x10)
        else:
            print(message, file=sys.stderr)
        return 1

    return open_desktop()


if __name__ == "__main__":
    sys.exit(main())
