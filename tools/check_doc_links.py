"""Check inline relative file, heading, and source-line links in the project guides."""

from pathlib import Path
import re
from urllib.parse import unquote, urlsplit


ROOT = Path(__file__).resolve().parents[1]
LINK = re.compile(r"\[[^\]\n]+\]\(([^)\s]+)\)")
HEADING = re.compile(r"^#{1,6}[ \t]+(.+?)\s*#*$", re.MULTILINE)
SOURCE_LINES = re.compile(r"L(\d+)(?:-L(\d+))?")


def heading_ids(markdown):
    ids = set()
    for title in HEADING.findall(markdown):
        base = re.sub(r"[^\w\s-]", "", title.lower()).replace(" ", "-")
        anchor = base
        suffix = 0
        while anchor in ids:
            suffix += 1
            anchor = f"{base}-{suffix}"
        ids.add(anchor)
    return ids


def check_link(page, link):
    url = urlsplit(link)
    if url.scheme or url.netloc:
        return None
    target = page.parent / unquote(url.path) if url.path else page
    if not target.exists():
        return "target does not exist"
    if not url.fragment:
        return None

    anchor = unquote(url.fragment)
    if not target.is_file():
        return "fragment points to a directory"
    source = target.read_text(encoding="utf-8")
    line_range = SOURCE_LINES.fullmatch(anchor)
    if line_range:
        start = int(line_range[1])
        end = int(line_range[2] or line_range[1])
        if not 1 <= start <= end <= len(source.splitlines()):
            return "source line is out of range"
    elif anchor not in heading_ids(source):
        return "heading does not exist"
    return None


def main():
    guides = [ROOT / "README.md", *sorted((ROOT / "docs").glob("*.md"))]
    failures = []
    for page in guides:
        for link in LINK.findall(page.read_text(encoding="utf-8")):
            problem = check_link(page, link)
            if problem:
                failures.append(f"{page.relative_to(ROOT)}: {link}: {problem}")
    for failure in failures:
        print(failure)
    if failures:
        raise SystemExit(1)
    print(f"Relative links passed across {len(guides)} guides.")


if __name__ == "__main__":
    main()
