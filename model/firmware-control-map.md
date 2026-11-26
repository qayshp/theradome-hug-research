# Firmware control flow

This map follows the archived revision-126 application from its relocated entry points through startup, interrupt service and the treatment state machine. Addresses in code links are **program byte addresses**. RAM and EEPROM references name their address spaces explicitly. RAM operands use the PIC18 access bank or the current bank-select register, so a file-register operand such as `0xB5` must be interpreted in context. State labels in this document describe observed branches and outputs. The physical controller model, board wiring and oscillator frequencies remain unverified. [Controller register evidence](controller.md) · [Microchip PIC18F87K22 family data sheet](https://ww1.microchip.com/downloads/en/DeviceDoc/39960d.pdf)

## Application entry and startup

| Address | Role in the archived bytes |
| --- | --- |
| [`0x1900`](rebuild.asm#L7) | Relocated application reset entry. It jumps through `0x1A44` into RAM initialization at `0xCD42`. |
| [`0x1908`](rebuild.asm#L11) | Relocated high-priority entry. It jumps to `0x8418`. |
| [`0x1918`](rebuild.asm#L20) | Relocated low-priority entry. The handler begins at the entry itself. |
| [`0xCD42–0xCDB5`](rebuild.asm#L20416-L20469) | Zeroes RAM ranges `0x300–0x32F`, `0x20E–0x2EC` and `0x033–0x04C`, along with selected single-byte flags. Then it copies two 12-byte groups from program flash. Program bytes `0xDF4E–0xDF59` initialize RAM `0x2ED–0x2F8`, and program bytes `0xDF42–0xDF4D` initialize RAM `0x050–0x05B`. The branch at the end enters `0x530E`. |
| [`0x530E–0x535B`](rebuild.asm#L6407-L6436) | Calls hardware initialization at `0x98A6`, sets initial operating and timer conditions, enables the real-time clock, clears event latches and closes the serial receive path. Writes application revision `126` to EEPROM word `0x1F4`. |
| [`0x535C–0x5427`](rebuild.asm#L6437-L6525) | Checks saved words and conditionally initializes EEPROM settings. The later branch consults manufacturing flags and the external audio directory, then sets variant byte `0x2B8`. The mapping from all its values to physical board revisions remains open. |
| [`0x5428–0x54B1`](rebuild.asm#L6526-L6572) | Loads saved settings, clears the runtime alarm-enable flag, closes the serial receive path and assigns the initial state `0x0D` before dispatch. |
| [`0x54B2–0x5557`](rebuild.asm#L6573-L6650) | The initial periodic pass checks EEPROM flags and branches to setup, calibration or the ordinary operating path. An initial state value therefore describes a firmware phase before idle or charging is selected. |
| [`0x98A6–0x9A7D`](rebuild.asm#L14254-L14482) | Initializes oscillator and port registers, sets up peripherals and timers, configures switch edge polarity, and enables the two interrupt priority levels. The bytes show register operations. A matching board layout would require hardware evidence. |

The addresses below `0x1900`, including the loader and its interrupt forwarding, fall outside the archived application. The archive also omits device configuration words and per-unit EEPROM contents. The two RAM bytes used for stock volume selection and attenuation lie in the zeroed ranges, which is why a stock application reset begins at full digital gain. [Firmware image scope](../docs/firmware.md) · [Volume state](volume.md#selection-and-playback)

## Interrupt dispatch and countdowns

The low handler saves `STATUS`, `WREG` and `BSR` in RAM `0x002–0x004`, then restores them and returns at `0x1A36–0x1A43`. The high handler at `0x8418–0x8769` ends with `RETFIE FAST`. Each handler tests both the pending and enable bits for the serviced source where its dispatch does both checks. The low handler reaches a transmit-flag check at its shared exit. The high handler tests Timer2, Timer0, INT1 and receive activity before it reaches the switch recognizer. Timer8 has a separate branch after the switch sources. [Low-priority dispatch](rebuild.asm#L20-L214) · [High-priority dispatch](rebuild.asm#L11951-L12020) · [High-priority return](rebuild.asm#L12396-L12397)

| Source | Work found in the handler |
| --- | --- |
| PORTB change | Reads PORTB twice, clears the mismatch interrupt, masks it and marks a charge-input event in RAM `0x27B.6`. Foreground code reexamines the charge pins and rearms the interrupt. [ISR](rebuild.asm#L20-L40) · [Foreground service](rebuild.asm#L19211) |
| Timer1 | Decrements the two nonzero RAM words at `0x26F–0x270` and `0x263–0x264`. The first is the active-treatment countdown. [ISR](rebuild.asm#L41-L70) |
| RTCC | Clears the clock interrupt flag, sets a pending alarm bit at RAM `0x04E.5` and calls `0xCAE0`. The foreground later selects the alert according to the operating state. [ISR](rebuild.asm#L75-L100) · [Foreground alarm dispatch](rebuild.asm#L7868-L7941) |
| Timer5 | Reloads the hardware counter. States `0x02` and `0x12` use the shorter interval. Other states in this branch use the longer interval. It decrements nonzero words at RAM `0x279–0x27A` and `0x26D–0x26E`. [ISR](rebuild.asm#L101-L166) |
| Timer0 and serial receive | The active handler paths clear their corresponding timer or receive enable and latch a timeout or receive event for the serial-wait logic. [Dispatch](rebuild.asm#L11966-L12019) · [Timed receive](rebuild.asm#L20021-L20044) |
| INT2, INT3 and Timer3 | The master-switch recognizer consumes these sources in that order. It posts activity in RAM `0x27B.7` and a gesture code in RAM `0x0F3`. [Interrupt branches](rebuild.asm#L12020-L12163) · [Gesture and timing traces](inputs.md#from-switch-edges-to-event-codes) |

The helper at `0xC5B8` routes countdown selectors `0`, `1`, `2` and `3` to RAM words `0x26F–0x270`, `0x279–0x27A`, `0x263–0x264` and `0x26D–0x26E`, respectively. It also clears other countdowns on several paths. Selector `4` copies its input to all four words. The expiry test at `0xD358` reports zero through the carry flag. The shared timer configuration at `0xD1B0` selects which interrupt-driven hardware timer is running. With a 32,768 Hz secondary oscillator at a 1:1 prescale, Timer1 would overflow from zero in two seconds, consistent with 600 ticks for a 20-minute session. Timer5's regular `0xC000` reload would give half-second ticks. Its short charging reload is described in the indicator trace. These durations are derived from register settings and the assumed oscillator. [Countdown writes](rebuild.asm#L19491-L19552) · [Expiry test](rebuild.asm#L21151-L21200) · [Timer setup](rebuild.asm#L14420-L14434) · [Timer selection](rebuild.asm#L20966-L21014) · [Charging indicator timing](indicator.md)

## Foreground ordering

The stock main loop moves between four distinct activities. Keeping their order visible explains how a button press or cable event can affect a transition requested during the same pass.

1. **Periodic handler.** The dispatcher at `0x5FD4` reads the current state at RAM `0x2B5`. A handler can propose a next state by writing RAM `0x2B4`. [Dispatcher](rebuild.asm#L7781-L7854)
2. **Pending events.** The loop services master-switch activity, then charge-input changes, then a pending RTCC alarm. A charge-input handler's returned state is written to `0x2B4`, so it can supersede an earlier proposal in that pass. The button dispatcher and charge service both receive the *current* state. [Event order](rebuild.asm#L7855-L7871) · [Alarm state selection](rebuild.asm#L7872-L7941)
3. **State commit.** It compares `0x2B4` with `0x2B5`. On a change, it copies the proposed state into the current-state byte, requests that state's light profile, then dispatches its entry routine at `0x6488`. Entry routines set up timers, wake conditions and outputs for the new mode. [Commit and first entry block](rebuild.asm#L7942-L7964) · [Entry dispatcher](rebuild.asm#L8290-L8372)
4. **Indicator and wait.** The tail of the loop refreshes the light profile and checks pending events before choosing to run another pass or execute `SLEEP`. In the idle and standby paths the oscillator configuration and wake conditions differ from the active state. A sleeping CPU can resume this loop under the configured wake conditions. Reset and sleep therefore reach different paths. [Refresh and sleep](rebuild.asm#L8373-L8421) · [Clock and idle settings](rebuild.asm#L19842-L19918)

## State groups and landmarks

The offline dispatcher enumerates all byte values. This table groups the recognized paths by the evidence available for their role. Some values share default handlers while selecting different light profiles. Those profiles alone give limited support for a state name. [Executable state table](state_paths.py) · [Visible behavior and session trace](README.md)

| State or group | Role and evidence |
| --- | --- |
| `0x01`, `0x02` | Ordinary charge completion and charging. Charge status can select these states through `0xC370`. `0x01` gives steady green and `0x02` gives the red-plus-green fade. The entry routines program different timer intervals and alarm behavior. [Charge transition](rebuild.asm#L19211) · [Charging and charged entries](rebuild.asm#L8217-L8243) |
| `0x04` | Waiting to start. The state entry arms the switch and a secondary countdown. The periodic handler consults the subsystem check and its status flag before loading 600 active-treatment ticks and requesting `0x07`. [Entry](rebuild.asm#L8075-L8099) · [Periodic handler](rebuild.asm#L7484-L7522) |
| `0x05` | Ordinary idle state. Completion requests it, and the charge service can request it when both status inputs are high. Its entry clears latches, selects the low-power clock settings and programs wake behavior. [Idle entry](rebuild.asm#L8051-L8074) · [Wake and idle handling](rebuild.asm#L7478-L7483) |
| `0x07`, `0x08` | Active treatment and pause or resume wait. Active expiry requests `0x0C`. Loss of the required status flag during active treatment requests `0x08` and loads a separate countdown. Resuming requests `0x07` without loading a new 600-tick timer. [Active and pause handlers](rebuild.asm#L7555-L7650) |
| `0x09`, `0x0A` | A separate wait follows a check failure. Expiry proceeds to the stop path. The ordinary stop handler calls output and device services, plays its clips and follows the charge reevaluation path. It does not call the treatment-history writer. [Check-failure and stop handlers](rebuild.asm#L7651-L7670) · [Stop routine](rebuild.asm#L7705-L7720) |
| `0x0B`, `0x1D` | The idle sustained-hold path requests `0x0B`. Its entry handles standby and announcement actions, then its periodic branch tests a port input before progressing. State `0x1D` has its own switch and charge behavior. The charge helper can branch back to the relocated application entry from this state. [Idle hold](rebuild.asm#L10040) · [Standby entry](rebuild.asm#L8244-L8284) · [Wait branch](rebuild.asm#L7721-L7768) · [Charge service](rebuild.asm#L19211) |
| `0x0C` | Normal completion. Reads the completed count, calls the history writer at `0x8CF4`, increments EEPROM word `0`, then uses completion announcement and transition logic. This is the path that records a finished session. [Completion handler](rebuild.asm#L7669-L7704) · [History and timestamp interpretation](eeprom.md) |
| `0x0D`, `0x0E` | Startup flag checks and a separate entry that records a diagnostic counter and plays status clips. The clip sequence and visible red pulse provide state evidence. The precise hardware condition for every route into `0x0E` is outside the session model. [Startup checks](rebuild.asm#L6573-L6650) · [Status periodic handler](rebuild.asm#L7447-L7477) |
| `0x11`–`0x13`, `0x16`–`0x19` | Initialization, service and calibration phases. Boot checks and EEPROM markers select these handlers. `0x11` is calibration-ready, `0x12` uses the charging fade during the calibration sequence, and `0x18` checks the completion marker. Startup and calibration phases can share indicator profiles with other states. [Startup flag dispatch](rebuild.asm#L6573-L6650) · [Calibration-ready and active paths](rebuild.asm#L6791-L7033) · [Observed calibration sequence](../docs/volume-settings.md#what-the-switch-light-indicated) |
| `0x1B`, `0x1C` | RTCC alert paths. The noncharging alert requests the green pulse state `0x1B`; the charging branch requests the red pulse state `0x1C`. Both refollow the ordinary charge-state handling after their own timeout. Other callers can reach the same states, so pulse color alone leaves the trigger uncertain. [Alarm foreground branches](rebuild.asm#L7872-L7941) · [Alert periodic handlers](rebuild.asm#L7769-L7780) |
| `0x00`, `0x03`, `0x06`, `0x0F`, `0x10`, `0x14`, `0x15`, `0x1A`, `0x1E`, `0x1F` | Dispatch probes find different indicator requests with the shared periodic handler at `0x5550`. The shared handler requests charge reevaluation. `0x0F` also has a direct indicator call in the serial diagnostic entry. The reachable contexts of every other value in this group remain unclassified. [Shared periodic handler](rebuild.asm#L6647-L6650) · [Serial diagnostic entry](rebuild.asm#L1490) · [Indicator dispatcher](rebuild.asm#L15093) |

## Switch, charge and treatment interpretation

The switch IRQ distinguishes activity, the intermediate hold and the completed gesture. The foreground handler at `0x7082` reads the event, checks its completion and dispatches by the state it was handed. Button-initiated audio can temporarily force full digital gain without changing the chosen index. In active and paused treatment, a short press reaches the remaining-time routine, a double press reaches the volume selector, and a completed long hold reaches the stop path. A long hold when ordinarily charged uses a different branch that plays the revision and then opens a timed serial receive window. [Button foreground dispatcher](rebuild.asm#L9714) · [Active button paths](rebuild.asm#L10059-L10110) · [Loudness and button traces](volume.md) · [Charged-state serial behavior](../docs/usb-clock-details.md#what-the-com7-sessions-showed)

Charging status, indicator rendering and treatment eligibility are separate operations. The charge interrupt queues an event. Foreground code reads the two status inputs and chooses a proposed application state. The indicator dispatcher at `0x9FD2` separately chooses how to drive the two indicator channels. `0x5220` initializes their request, and the charging fade calculation uses the sampled battery value and a phase counter. A fading light also appears on a calibration path. [Charge inputs and mode table](inputs.md#how-a-charge-input-change-affects-the-operating-state) · [Indicator initialization](rebuild.asm#L6286) · [Fade arithmetic](rebuild.asm#L10657) · [Indicator interpretation](indicator.md)

The treatment periodic handlers use the result of their subsystem check and an additional status flag to decide when to start, continue, pause or stop. Manual stop, expiry of a waiting or paused session, and normal treatment completion take different paths. Only the normal completion path described here calls the history writer. It stores the record with the real-time clock value before incrementing the completed count. The host-side traces exercise the state decisions by supplying the check result. They leave the analog sensing, emitter drive, optical output and physical safeguards outside the model. [Treatment branches](rebuild.asm#L7484-L7720) · [Scope of host trace](session_trace.py) · [Treatment log evidence](../docs/treatment-analysis.md#log-format-and-timestamps)

## Recheck the control map

Run these commands from the repository root. They read the stock image and exercise it offline.

```sh
python3 -B model/state_paths.py
python3 -B model/session_trace.py 7 --expired
python3 -B model/session_trace.py 8
python3 -B model/check_states.py
```

The last command compares 768 dispatcher lookups, 4,096 indicator cases and 2,048 session passes. These checks describe reachable instruction paths and proposed state changes. They leave actual interrupt timing and electrical behavior unmeasured. [State checks](check_states.py)
