# -*- mode: python ; coding: utf-8 -*-
"""Build the Windows app with its Python, Tcl/Tk and charting runtime."""

from importlib.metadata import distribution
from pathlib import Path
import sys


if sys.platform != "win32":
    raise SystemExit("Build the Windows app on Windows.")

here = Path(SPECPATH).resolve()
tools = here.parent.parent / "tools"
python_root = Path(sys.base_prefix)
tcl_root = python_root / "tcl"
datas = [
    (str(tools / "lh40_usb.js"), "."),
    (str(python_root / "LICENSE.txt"), "notices/Python"),
]
for toolkit in ("Tcl", "Tk"):
    candidates = sorted(tcl_root.glob(f"{toolkit.lower()}[0-9]*/license.terms"))
    license_file = candidates[0] if candidates else here / f"{toolkit}-license.terms"
    datas.append((str(license_file), f"notices/{toolkit}"))

for name in (
    "contourpy", "cycler", "fonttools", "kiwisolver", "matplotlib", "numpy",
    "packaging", "pillow", "pyparsing", "python-dateutil", "tzdata", "PyInstaller",
):
    package = distribution(name)
    for file in package.files:
        if file.parts[0].endswith(".dist-info") and any(
            part.lower().startswith(("license", "copying", "notice")) for part in file.parts[1:]
        ):
            relative = Path(*file.parts[1:])
            datas.append((str(package.locate_file(file)), (Path("notices") / name / relative.parent).as_posix()))

analysis = Analysis(
    [str(here / "launch.py")],
    pathex=[str(tools)],
    datas=datas,
    hooksconfig={"matplotlib": {"backends": ["Agg"]}},
)
archive = PYZ(analysis.pure)
executable = EXE(
    archive, analysis.scripts, [], exclude_binaries=True,
    name="LH40 History", console=False, upx=False,
)
bundle = COLLECT(
    executable, analysis.binaries, analysis.datas,
    upx=False, name="LH40 History",
)
