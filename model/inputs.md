# Master switch and charge-status diagnostics

The C model describes the HUG image with Intel HEX file SHA-256 `40CAA7FD679E70205A8ACC923F5586484884060E9EF4EB0CFAA4C4D23F120C69`. A physical LH40 may carry a different revision. Code addresses below are byte addresses. Register names follow the PIC18F87K22-family map. For these particular interrupts and pins, the PIC18F87K90-family map agrees. A package marking or a chip ID would settle the part number. [Microchip PIC18F87K22 family data sheet](https://ww1.microchip.com/downloads/en/DeviceDoc/39960d.pdf)

## From switch edges to event codes

The firmware makes RB2 and RB3 inputs at `0x98E8`. It programs INT2 to sense a falling edge and INT3 to sense a rising edge at `0x9A1E–0x9A24`. The diagnostic that prints “Press Button” at `0x44A4` arms this recognizer and waits until RAM byte `0x0F3` equals **2**. This ties the recognizer to the master switch. The board wiring between the contacts and the two PIC pins is unknown.

| Observed electrical interrupt sequence after arming | Firmware event in RAM | Interpretation |
|---|---|---|
| INT2 falls | 0, with `0x27B.7` set | Input activity has started. Timer3 and INT3 are armed. |
| One INT3 rise before the first timeout, then the restarted timer expires | 2 | Ordinary short press if INT3 tracks release. The diagnostic accepts this result. |
| Two INT3 rises, each before its respective short timeout | 5 | Double press candidate. The first rise restarts the short window. |
| First timer expires with no INT3 rise | 1 | Intermediate held state. A longer timer starts. |
| INT3 rises during the longer window | 2 | Also accepted as an ordinary press. |
| Longer timer expires without that rise | 3 | Sustained hold. |

The controller can read the intermediate codes. Completed events persist until a foreground call clears them. The handler masks INT2 after the first edge and masks INT3 and the timer when it produces a final result. It does not automatically rearm itself. The interval between the initial fall and the *second* rise can therefore exceed one short window if the first rise restarted it. These observations describe the input to the foreground state machine. The foreground can disarm or rearm the recognizer at any point.

The short timer preload is `0xE000`, giving 8,192 increments to overflow. The long preload is `0x4000`, giving 49,152 increments. `T3CON = 0x9E`, followed by setting its enable bit, selects the secondary oscillator with a 1:2 prescaler. With a 32,768 Hz secondary crystal the intervals would be **0.5 seconds** and **3 seconds**, or approximately **3.5 seconds** continuously held before event 3. The actual oscillator frequency on an LH40 board has not been measured. Electrical filtering and foreground response can affect observed timing.

The C implementation exposes `button_enable` (firmware `0xD9F8`) and `button_input` (three paths starting at `0x847A`, `0x84B2`, and `0x850C`). The caller supplies individual hardware edges or timer expirations in service order. `interval_ticks` represents the selected timer interval. In the original interrupt dispatcher a pending INT2 has priority over INT3, which has priority over Timer3. Handling one source may clear another pending flag. Determining which source won an actual timing tie requires hardware timing.

## What the charging diagnostic prints

The charge diagnostic checks RA4 and RB4 and consults RAM byte `0x2AD`. The table assumes the inputs remain stable throughout `0x4C6E–0x4CE6`. The input bits are read individually more than once.

| RA4 | RB4 | Mode other than 2 | Mode 2 |
|---:|---:|---|---|
| 0 | 0 | Error | Test Mode |
| 0 | 1 | Charged | Error |
| 1 | 0 | Charging | Error |
| 1 | 1 | Error | Error |

On the three recognized paths the code also writes 3 for Charged, 5 for Charging, or 0 for Test Mode to RAM word `0x36` and sets `0x344` to 1. On Error it clears `0x344` and leaves the previous word intact. The serial strings come from `0x23CF`, `0x23A5`, `0x2389`, and `0x23F4`. The separate display path controls the red and green channels. [Charging indicator waveform](indicator.md)

## How a charge-input change affects the operating state

A low-priority port-change interrupt at `0x1924–0x1940` reads PORTB twice, clears and masks the interrupt and sets pending bit `0x27B.6`. At `0x6062–0x606E` the main loop calls `0xC370` when this bit is set. The helper clears it, reads the two status pins, consults the current operating state `0x2B5`, and proposes a next state in `0x2B4`. It reenables the interrupt before returning. Later foreground code can still change the proposed state. The diagnostic mode byte `0x2AD` is a separate variable.

| Charge inputs | Prior operating state | Return or action |
|---|---|---|
| RA4 and RB4 both high | Any | Requests state 5. Calls `0xA954(0)` first when prior state is `0x1C`. All paths here also call `0xDEB6`. |
| Any other combination | 4, 7, 8 or 9 | Increments EEPROM slot 7 via `0xD892(7)` and requests `0x0A`. An erased counter word of `0xFFFF` becomes 1 on this path. |
| Any other combination | `0x0B` | Calls `0xC8DE(1)` and `0xD1B0(1)` and retains `0x0B`. |
| Any other combination | `0x1D` | Jumps to application entry at `0x1900`. |
| Any other combination | Other states | RA4 low requests 1, RA4 high requests 2. |

Thus RA4/RB4 both low can produce the diagnostic text **Error** and a foreground request for state **1** at different points in the program. A diagnostic label and an operating-state request answer different questions about the same inputs. `charge_next_state` models the proposed state and reports `-1` for the application-entry jump.

## Independent comparison

From the repository root on Linux, build the host model and compare it with the same HEX used in the investigation.

```sh
cc -std=c99 -fPIC -shared model/inputs.c -o /tmp/theradome-inputs.so
python3 model/check_inputs.py /tmp/theradome-inputs.so
```

The verifier interprets the PIC instruction words on these paths and follows the branches, including a skip that skips the next skip. It compares all four digital input combinations for each of 256 diagnostic mode bytes and each of 256 prior operating states. For the operating-state comparison it skips the peripheral/EEPROM subroutine calls and checks the returned state or application-entry jump. It also compares the event, activity latch, rising-edge latch, interrupt enables and active timer interval after each step in all 15,625 six-step traces over falling edge, rising edge, expiration, arm and disarm. This comparison uses a host-compiled copy of `inputs.c`. Firmware execution on a physical board and analog signal behavior are outside this comparison.
