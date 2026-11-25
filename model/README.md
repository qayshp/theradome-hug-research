# Theradome HUG firmware behavior model

This directory contains an editable assembly reconstruction of the 50,816-byte application shipped with HUG and host-side models of its controls. The archived firmware identifies itself as LH80. Matching an observation on an LH40 increases confidence in the particular behavior. Establishing that the two units use identical firmware would take a device-specific comparison.

## Working backward from a helmet observation

| Observation | What the archived code predicts | What distinguishes the alternatives |
|---|---|---|
| The master switch changes color slowly while plugged into a wall adapter. | The ordinary charging state, `0x02`, selects a two-color fade. State `0x12` selects exactly the same indicator. | A fade observed immediately after plugging in an idle helmet fits `0x02`. A fade by itself does not distinguish these states. |
| The switch goes from that fade to sustained green while it stays plugged in. | The ordinary charge-complete input requests state `0x01`, which selects steady green at level 7. | The transition and the attached charger matter. An active session also has a steady-green profile, with a green level of 37 in this image. These levels are firmware values. |
| During a session, one press produces a loud remaining-time announcement. The next ordinary announcement is soft again. | States `0x07` and `0x08` both save the effective gain, play the requested time at full level and restore the gain. | A steady indicator fits `0x07`. A toggling indicator fits `0x08`. Both can include a red component depending on an internal flag. |
| A double press makes the confirmation progressively quieter, then silent, then loud. | The selected index advances Loud → Medium → Soft → Silent → Loud. Digital sample gain is full, one half, one eighth, or effectively zero. | A spoken time after an intended double press is consistent with recognition as a single press. |
| The helmet was set to Soft. A prompt sounds loud, and the next accepted double press produces silence. | This fits a retained selection of Soft. A temporary override or a state change that clears the effective attenuation can make a prompt loud. | Hearing another ordinary prompt at Soft before that double press favors a temporary override. If a double press instead confirms Medium, the sequence fits a selection that had returned to Loud. |
| The timer reaches its end, and the session count advances. | State `0x07` requests `0x0C`. Its periodic handler writes a dated entry to EEPROM, increments the count, then requests `0x05`. | The ordinary completion record includes battery, temperature and laser-current readings. The table of the last 50 entries wraps while the count keeps advancing. |

Theradome's charge guide uses ten seconds of solid green on the charger as its full-charge indication. Its volume guide describes the four-level cycle. External observations can also vary with firmware revision, whether playback is enabled, and whether the switch event was accepted. [Charging guide](https://theradome.com/a/docs/troubleshooting/use-and-care-theradome-charger) · [Volume guide](https://theradome.com/a/docs/troubleshooting/adjusting-sound-volume-on-theradome)

## Session states

The main loop dispatches the periodic handler using the current state in RAM `0x2B5`. The handler can propose a next state in `0x2B4`. The main loop then services pending switch and charge events before committing the next state and requesting the indicator at `0x6116–0x6120`. This order matters when a cable change and a press occur together.

| Current state | Event or condition | Requested state |
|---|---|---|
| `0x04` waiting to start | Checks pass and RAM `0x27B` bit 0 is set | `0x07`, with session countdown loaded to 600 |
| `0x07` active | Checks pass, countdown running, bit 0 clears | `0x08`, with a separate countdown loaded |
| `0x08` paused or waiting to resume | Checks pass, secondary countdown running, bit 0 sets | `0x07`, without the initial 600-tick session reload |
| `0x04`, `0x07`, `0x08` | Checks fail while the applicable countdown is running | `0x09`, with a separate 120-tick countdown |
| `0x04`, `0x08`, `0x09` | Secondary countdown expires | `0x0A`, with EEPROM word counter 8 incremented |
| `0x07` | Session countdown expires | `0x0C`, then completion logging and `0x05` |
| `0x07`, `0x08` | Long press is accepted | `0x0A`, with EEPROM word counter 7 incremented |

The numbers, branches, loads and writes are direct firmware evidence. “Waiting to start,” “active,” and “paused” interpret those operations in context. The trace accepts the result of the checks at `0x80BA` and the value of the flag after that call. It does not infer which sensor or physical condition produced them. The two countdowns use distinct timer paths.

## Running the model

Run these commands from the repository root with Python 3.

```sh
python3 model/state_paths.py --pattern fade --channel both
python3 model/state_paths.py --mode 0x07
python3 model/session_trace.py 7 --flag-clear
python3 model/session_trace.py 8
python3 model/session_trace.py 7 --expired
python3 model/check_states.py
```

`state_paths.py` executes the PIC instructions for the main dispatchers and the requested indicator. Its table includes the union of indicator profiles over relevant flag values. Direct calls elsewhere can also temporarily request a particular display. `session_trace.py` executes one periodic pass in states 4, 7, 8 or 9 and shows the service calls on that path. Other services, interrupts and analog hardware are outside this trace.

From the same directory, `sh model/rebuild.sh` builds the PIC assembly with gputils and verifies every populated byte against both archived formats. The host C models in this directory compile separately for comparison with the archived instructions. The build notes cover the successful one-byte source-edit test and the limits on relocating code.

| Area | Source and evidence |
|---|---|
| PIC assembly and rebuild | [Assembly](rebuild.asm) · [Build and byte comparison](rebuild.md) · [Controller identification](controller.md) |
| Button timing and charge inputs | [Recognition and charge-state tables](inputs.md) · [C model](inputs.c) |
| Charge indicator | [Color channels and fade](indicator.md) |
| Volume and time announcements | [Playback and reset paths](volume.md) · [C model](voice.c) |
| EEPROM and countdown | [Storage map and write ordering](eeprom.md) · [Offline dump reader](eeprom.py) |
