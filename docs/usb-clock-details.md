# LH40 USB diagnostics and clock details

These observations come from one EVO LH40 and the archived HUG application image. Windows identified the bridge with USB ID `0403:6015` and the description `LH 80 - Laser Helmet BL4`. The helmet’s diagnostics also displayed an LH80 banner. The physical unit, USB descriptor and firmware banner identify different parts of the setup. [Device and controller evidence](device.md)

## Windows ARM64 driver setup

1. Device Manager initially reported Code 28. After the FTDI driver was first installed, the USB Serial Converter showed **OK**, but its USB Serial Port child still showed Code 28. The other listed COM ports belonged to Bluetooth.
2. The serial-port child needed its own driver association. In Device Manager, the owner updated the child from the extracted FTDI Windows ARM64 driver package, selecting the folder containing `ftdiport.inf`.
3. Windows then showed **USB Serial Port (COM7)** with status **OK**. The COM number is assigned by Windows on that computer. It establishes that the USB bridge is available. The later serial exchange established that the helmet controller responded.

[FTDI VCP driver downloads](https://ftdichip.com/drivers/vcp-drivers/) · [FTDI Windows installation guide](https://ftdichip.com/wp-content/uploads/2022/05/AN_396-FTDI-Drivers-Installation-Guide-for-Windows-10_11.pdf) · [Live verification](verification.md#live-evo-lh40-tests)

The same data cable initially showed charging on a wall adapter and no apparent charging on the PC. Later, after a disconnect and reconnect to the laptop, the switch faded between orange and yellow and then held green. That later sequence supports charging and a charge-complete indication on this laptop. USB current was unmeasured in both observations. [Later laptop charging observation](volume-settings.md#what-the-switch-light-indicated)

## What the COM7 sessions showed

The diagnostic sessions used 57,600 baud, eight data bits, no parity and one stop bit, with no hardware flow control and DTR and RTS off. This condensed transcript leaves out the unit identifier and the HUG login exchange.

| Stage | Observation |
| --- | --- |
| First successful connection | With the port open, the owner held the master switch until it said “Theradome Revision 126.6.” HUG-compatible diagnostics reached the `BOARD TEST` menu. The EEPROM parameter display reported `PRODUCT CODE REVISION - 126` and `BOOT LOADER REVISION - 6`. |
| Reopen with the cable still attached | Opening the port again produced zero bytes during a 2.2-second listen. One carriage return brought back the `BOARD TEST` banner and the menu prompt. The same version fields were readable without another button hold or login. This establishes session continuity in that instance. |
| Later check after unplugging and reconnecting | Windows still showed a healthy COM7 when the switch was steady green. A passive five-second listen produced no bytes. A separate carriage return also produced no bytes in four seconds. |

[Published USB verification](verification.md#live-evo-lh40-tests) · [After-update observation](volume-settings.md#live-evo-lh40-test-on-november-26-2026)

On the archived charged-state path, the long-hold handler calls the revision speech routine first. **The spoken revision sends zero bytes over the serial port on this path.** After the audio finishes, the firmware opens a timed receive window. It waits for a host byte before starting the diagnostic handshake. If the window expires, the receive path shuts down again. Windows can continue to show a healthy USB bridge throughout. The silence on reconnect and the reply when resuming an existing board session describe two different application states. [Hold handler](../model/rebuild.asm#L10176-L10198) · [Revision speech](../model/rebuild.asm#L20779-L20822) · [Timed receive](../model/rebuild.asm#L20021-L20044) · [Handshake entry](../model/rebuild.asm#L21015-L21031)

## Diagnostic reads

These five displays were read on the unit. Their contents are bounded by the display routines.

| Display | What it returned |
| --- | --- |
| EEPROM parameters (`D`) | Selected counters, calibration constants, flags, revision fields and install dates. |
| Date and time (`t`) | Local calendar date, weekday and clock time at one-second resolution. |
| Alarm settings (`a`) | The status of the treatment reminder, the charging timeout and the currently armed alarm. |
| Treatment log (`V`) | The latest 50 completed-treatment records. |
| First wave file (`W`) | The clip directory and the decoded waveform of the first clip. |

The display menu offered no identified general RAM read, complete EEPROM or flash export, USB setter for the selected volume, or per-field calibration restore. Some other menu entries run active hardware diagnostics or write persistent state. In particular, the proximity and battery/temperature tests can change outputs. They were outside the read-only run. [Menu dispatcher](../model/rebuild.asm#L6063) · [Readback and recovery limits](verification.md#backup-and-recovery)

The selected volume index lives at RAM `0x044`. The effective right-shift count lives at RAM `0x2B9`. The waveform routine applies the shift before printing each sample. The first printed value was **2,054**, and the trace reached **4,080**. A single right shift from the archived decoder’s maximum would cap the result at 2,040, so the capture supports a zero shift while the waveform was read. The long-hold entry can itself force full digital gain. The capture therefore establishes effective gain in diagnostics and leaves the selected index and ordinary playback gain unobserved. [Volume state](../model/volume.md#selection-and-playback) · [Waveform scaling](../model/rebuild.asm#L6367) · [Decoder limit](../model/rebuild.asm#L16996)

## Why the clock had a plausible date

The archived startup code enables the microcontroller’s real-time clock and calendar without setting a new date. The hardware can keep time while the CPU sleeps if its clock source and supply remain available. The archived Windows and Mac HUG applications yielded no automatic time-sync path in the portions traced. Factory or service setup is a plausible source for the initial setting. The time of that setup, the installed oscillator and retention during complete removal of power remain unverified on this unit. [Startup](../model/rebuild.asm#L6422) · [Clock enable](../model/rebuild.asm#L22514-L22523) · [Microchip RTCC documentation](https://ww1.microchip.com/downloads/en/DeviceDoc/PIC18F87K22-Family-Data-Sheet-30009960F.pdf#page=219)

In an earlier pair of read-only checks, the helmet and laptop each advanced **4 minutes 49 seconds** between readings. The helmet was about **28 minutes 8 seconds ahead** at both endpoints. This shows that the clock was running before the sync. A wrong initial setting and accumulated drift both fit that observation.

## Notes for a clock-sync tool

The later sync began with a roughly **28-minute 9-second** offset. The setter acknowledged the write with `Date Set`. Two subsequent reads were within one displayed second of the laptop clock and advanced by two seconds. Both alarms and the current alarm still reported disabled. Existing treatment timestamps retain the clock values stored when those sessions finished. [Live clock check](verification.md#live-evo-lh40-tests) · [Clock write](../model/rebuild.asm#L21202-L21233) · [Treatment record format](../model/eeprom.md)

1. In an established Board Test session, use lowercase `t` for the time read and lowercase `a` for the optional alarm baseline. Single-byte menu commands are sent without terminators. A read-only mode can stop here.
2. For a requested clock write, send uppercase `T` once. Wait for the date prompt and send local `YYYY-MM-DD-WW` as ASCII with one carriage return. The two-digit weekday runs from `00` for Sunday to `06` for Saturday. Validate the date and weekday together.
3. Wait for the time prompt and send local `HH:MM:SS` with one carriage return and no line feed. Use a consistent host time zone and avoid splitting the exchange across local midnight. The date line stages the values in RAM. Completing the time line writes the century to EEPROM word `0x2A` and sets the hardware calendar.
4. Wait for `Date Set`. The setter can wait for the seconds register to roll over if it reaches the write while the register is at 59. Treat a missing acknowledgement as uncertain. Confirm the normal menu before making an independent time read. The archived parser can reset on cancellation, so a timeout alone is insufficient evidence for replaying the write.
5. Read the clock twice to check local date, weekday, offset and continued progression. The host used a two-second tolerance in this test. If an alarm baseline was captured, compare it with a fresh alarm read.

## Alarm state and partial writes

The archived firmware has two software schedules backed by one hardware alarm. The treatment reminder uses an interval in days. The charging timeout uses an interval in hours and has a separate alert and state transition. Its effect on actual charging current remains unverified. The alarm display was read on the tested unit. The alarm setter and enable controls were traced in the archived image only. [Reminder scheduling](../model/rebuild.asm#L8152) · [Charging-timeout path](../model/rebuild.asm#L7872)

The menu assigns `a` to display the alarm settings, `A` to change the schedules, `B` to enable alarms and `b` to disable them. The device separates saved schedule values from the running alarm-enable flag. The startup path clears that flag and reloads stored settings. If alarms are enabled, the two-prompt setter can save the reminder interval before asking for the charge timeout. Canceling or losing the session at the second prompt can therefore leave the first update in place. A host tool should read back both fields after any interrupted write. [Enable and disable handlers](../model/rebuild.asm#L4109-L4151) · [Setter](../model/rebuild.asm#L4152-L4317) · [First write gate](../model/rebuild.asm#L16078) · [Startup alarm state](../model/rebuild.asm#L6566)
