"""Render a local chart from the complete LH40 archive."""

from pathlib import Path
from typing import Sequence

from matplotlib.backends.backend_agg import FigureCanvasAgg
from matplotlib.figure import Figure
from matplotlib.ticker import MaxNLocator

from lh40_archive import Treatment


def render_chart(rows: Sequence[Treatment], path: Path) -> None:
    figure = Figure(figsize=(11, 7.2), dpi=135, layout="constrained")
    FigureCanvasAgg(figure)
    figure.suptitle("LH40 treatment history", fontsize=16, fontweight="bold")
    if not rows:
        figure.text(0.5, 0.5, "No completed treatments recorded", ha="center")
        figure.savefig(path)
        return

    axes = figure.subplots(3, 1, sharex=True)
    numbers = [row.session_number for row in rows]
    panels = (
        ("Battery voltage (V)", "battery_voltage_v", "#095b79"),
        ("Temperature (°C)", "temperature_c", "#a54824"),
        ("Recorded laser current (mA)", "laser_current_ma", "#4c3a90"),
    )
    for axis, (label, field, color) in zip(axes, panels):
        # Leave a break in the line where the helmet's rolling log lost records.
        x, y = [], []
        for number, row in zip(numbers, rows):
            if x and number > x[-1] + 1:
                x.append(number - 1)
                y.append(float("nan"))
            x.append(number)
            y.append(float(getattr(row, field)))
        axis.plot(x, y, color=color, linewidth=1.5, marker="o", markersize=3)
        axis.set_ylabel(label)
        axis.grid(axis="y", alpha=0.25)
        axis.spines[["top", "right"]].set_visible(False)

    axes[-1].set_xlabel(f"Helmet treatment number · {len(rows)} archived")
    axes[-1].xaxis.set_major_locator(MaxNLocator(integer=True))
    axes[-1].set_xlim(numbers[0] - 0.6, numbers[-1] + 0.6)
    figure.savefig(path)
