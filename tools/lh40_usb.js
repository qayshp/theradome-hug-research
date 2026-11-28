// Nashorn bridge for the Windows HUG serial driver and normal vendor login.
// The caller gives each session its own private, existing capture directory.
var BOARD_PROMPT = /THERADOME LH(?:40|80)[ \t]+-[ \t]+BOARD TEST[^\r\n]*[\r\n]+Press \[\?\] for Menu\.\.\.\[ESC\] to Exit/;
var LOGIN_PROMPT = /Enter Password[ \t]*-/;
var MENU_END = /\[ESC\] Exit COM Mode/;
var PARAMETERS_END = /(?:^|[\r\n])UPGRADE INSTALL DATE[ \t]*-[^\r\n]*[\r\n]/;
var CLOCK_REPORT = /(?:^|[\r\n])Date Read[ \t]*-[ \t]*([A-Z]{3})[ \t]+(\d{4})-(\d{2})-(\d{2})[ \t]*[\r\n]+Time Read[ \t]*-[ \t]*(\d{2}):(\d{2}):(\d{2})[ \t]*[\r\n]/;
var DATE_PROMPT = /Enter Date as YYYY-MM-DD-WW[\s]*WW - 00=Sunday\.\.06=Saturday[ \t]*-[ \t]+$/;
var TIME_PROMPT = /Enter Time as HH:MM:SS[ \t]*-[ \t]+$/;
var CLOCK_ACK = /(?:^|[\r\n])Date Set[ \t]*[\r\n]/;
var CLOCK_TOLERANCE_MS = 2000;

function countFromParameters(raw) {
    var match = /(?:^|[\r\n])ERROR COUNTERS[\s]+0[ \t]+(-?\d+)[ \t]*[\r\n]/.exec(raw);
    var count = match ? Number(match[1]) : NaN;
    if (!isFinite(count) || count < -32768 || count > 65535) {
        throw new Error("The diagnostic treatment counter is missing or invalid.");
    }
    if (count === -1 || count === 65535) return 0;
    return count < 0 ? count + 65536 : count;
}

function treatmentCount(raw) {
    var lines = raw.split(/[\r\n]/);
    lines.pop(); // A final record without its line terminator is incomplete.
    return lines.filter(function (line) {
        return /^[ \t]*\d+[ \t]+\d{2}\/\d{2}\/\d{4}[ \t]+[A-Z]{3}[ \t]+\d{2}:\d{2}:\d{2}(?:[ \t]+-?\d+\.\d+){3}[ \t]*$/.test(line);
    }).length;
}

function localDay(date) {
    return date.getFullYear() + "-" + two(date.getMonth() + 1) + "-" + two(date.getDate());
}

function two(value) {
    return ("0" + value).slice(-2);
}

function civilHostMillis(date) {
    // Compare wall-clock readings even during an ambiguous daylight-saving hour.
    return Date.UTC(date.getFullYear(), date.getMonth(), date.getDate(),
        date.getHours(), date.getMinutes(), date.getSeconds(), date.getMilliseconds());
}

function clockReport(raw) {
    // The helmet clears the terminal before Date Read, without a line break.
    return CLOCK_REPORT.exec(raw.replace(/\x1b\[[0-?]*[ -/]*[@-~]/g, ""));
}

function civilHelmetMillis(raw) {
    var match = clockReport(raw);
    if (!match) throw new Error("Helmet clock readback format was unexpected.");
    var year = Number(match[2]), month = Number(match[3]), day = Number(match[4]);
    var hour = Number(match[5]), minute = Number(match[6]), second = Number(match[7]);
    var millis = Date.UTC(year, month - 1, day, hour, minute, second);
    var date = new Date(millis);
    var weekdays = ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"];
    if (date.getUTCFullYear() !== year || date.getUTCMonth() + 1 !== month || date.getUTCDate() !== day ||
            hour > 23 || minute > 59 || second > 59 || weekdays[date.getUTCDay()] !== match[1]) {
        throw new Error("Helmet clock readback date, weekday or time was invalid.");
    }
    return millis;
}

function enterBoardTest(io, wakeSeconds) {
    var until = io.monotonic() + wakeSeconds * 1000;
    var prompt = function (raw) { return BOARD_PROMPT.test(raw) || LOGIN_PROMPT.test(raw); };
    var response = io.receive(prompt, Math.min(700, wakeSeconds * 1000), 30);
    function canWake() {
        return !response.complete && !response.failed && !/\S/.test(response.raw) && io.monotonic() < until;
    }
    function wake() {
        io.write("\r");
        response = io.receive(prompt, Math.min(2000, until - io.monotonic()), 30);
    }
    if (canWake()) wake();
    if (canWake()) {
        io.progress("Hold the master switch until the helmet speaks its revision, then release.");
        while (canWake()) wake();
    }
    if (!response.complete) throw new Error("No recognized Board Test banner or HUG login prompt was received.");
    if (!BOARD_PROMPT.test(response.raw) && !io.login()) {
        throw new Error("The original HUG login did not reach Board Test.");
    }
}

function runSession(io, mode, wakeSeconds) {
    if (mode !== "import" && mode !== "sync-clock") throw new Error("Unknown USB operation.");
    var safeToExit = false;

    function exchange(command, complete, timeout, capture, returnsToBoard, message) {
        safeToExit = false;
        io.write(command);
        var result = io.receive(complete, timeout, 120);
        safeToExit = returnsToBoard && result.complete && !result.failed;
        if (capture) io.save(capture, result.raw);
        if (!result.complete || result.failed) throw new Error(message);
        return result.raw;
    }

    function readClock(filename) {
        var started = io.now();
        var raw = exchange("t", function (text) { return clockReport(text) !== null; }, 3500,
            filename, true, "The helmet clock response was incomplete.");
        return {raw: raw, midpoint: Math.round((started + io.now()) / 2)};
    }

    function syncClock() {
        var now = io.now();
        if (localDay(new Date(now)) !== localDay(new Date(now + 16000))) {
            throw new Error("The clock is too close to local midnight. No clock-setting command was sent.");
        }
        try {
            exchange("T", function (text) { return DATE_PROMPT.test(text); }, 4000,
                "clock-set-date-prompt.txt", false, "Expected date and weekday prompt was absent.");
            var date = new Date(io.now());
            var day = localDay(date);
            exchange(day + "-" + two(date.getDay()) + "\r", function (text) { return TIME_PROMPT.test(text); }, 4000,
                "clock-set-time-prompt.txt", false, "Expected time prompt was absent.");
            var partialSecond = io.now() % 1000;
            if (partialSecond > 200) io.sleep(1025 - partialSecond);
            var time = new Date(io.now());
            if (day !== localDay(time)) throw new Error("The laptop date changed while the setter was open.");
            var timeText = two(time.getHours()) + ":" + two(time.getMinutes()) + ":" + two(time.getSeconds());
            exchange(timeText + "\r", function (text) { return CLOCK_ACK.test(text); }, 8000,
                "clock-set-ack.txt", true, "The helmet did not acknowledge with Date Set.");
        } catch (error) {
            throw new Error("Clock setter outcome is uncertain. " + error.message);
        }

        try {
            var first = readClock("clock-after.txt");
            io.sleep(2000);
            var second = readClock("clock-verify.txt");
            var firstTime = civilHelmetMillis(first.raw);
            var secondTime = civilHelmetMillis(second.raw);
            if (secondTime <= firstTime ||
                    Math.abs(firstTime - civilHostMillis(new Date(first.midpoint))) > CLOCK_TOLERANCE_MS ||
                    Math.abs(secondTime - civilHostMillis(new Date(second.midpoint))) > CLOCK_TOLERANCE_MS) {
                throw new Error("Both readbacks must advance and be within two seconds of the laptop.");
            }
        } catch (error) {
            throw new Error("The helmet acknowledged the clock write, but verification failed. " + error.message);
        }
        io.progress("Clock sync verified by two advancing readbacks.");
    }

    try {
        enterBoardTest(io, wakeSeconds);
        var menu = exchange("?", function (text) { return MENU_END.test(text); }, 3500,
            null, true, "The expected diagnostics menu did not arrive.");
        if (!/THERADOME LH(?:40|80)[ \t]+-[ \t]+COMMAND MENU/.test(menu) ||
                !/\[D\][ \t]+Display EEPROM Parameters/.test(menu) ||
                !/\[t\][ \t]+Display Date and Time/.test(menu) ||
                (mode === "import" && !/\[V\][ \t]+View Treatment Log/.test(menu)) ||
                (mode === "sync-clock" && !/\[T\][ \t]+Set Date and Time/.test(menu))) {
            safeToExit = false;
            throw new Error("The diagnostics menu does not match the supported commands.");
        }
        var parameters = exchange("D", function (text) { return PARAMETERS_END.test(text); }, 4500,
            "parameters.txt", true, "The EEPROM parameter response was incomplete.");
        if (!/PRODUCT CODE REVISION[ \t]*-[ \t]*126[ \t]*[\r\n]/.test(parameters) ||
                !/BOOT LOADER REVISION[ \t]*-[ \t]*6[ \t]*[\r\n]/.test(parameters)) {
            throw new Error("This tool supports the tested application and bootloader revision 126.6.");
        }
        io.progress("Board Test connected and revision 126.6 confirmed.");
        var baseline = readClock("clock-before.txt");
        io.save("clock-before-host-ms.txt", baseline.midpoint + "\n");

        if (mode === "sync-clock") {
            syncClock();
        } else {
            var total = countFromParameters(parameters);
            io.save("treatment-count.txt", total + "\n");
            var rows = Math.min(total, 50);
            exchange("V", function (text) {
                return /TREATMENTS COMPLETED LOG/.test(text) &&
                    /Temp\(C\)[ \t]+Laser Current\(mA\)/.test(text) &&
                    /(?:^|[\r\n])-{5,}[ \t]*[\r\n]/.test(text) && treatmentCount(text) === rows;
            }, 12000, "treatments.txt", true,
            "The treatment capture was incomplete. The partial response was saved locally.");
            io.progress("Treatment history and clock response saved locally.");
        }
    } finally {
        if (safeToExit) {
            try {
                io.write("\u001b");
                io.sleep(600);
            } catch (error) {
                io.progress("Board Test exit could not be confirmed at the port.");
            }
        }
    }
}

function main(args) {
    var Communication = Java.type("SerialCommunication.Communication");
    var SerialPort = Java.type("com.fazecast.jSerialComm.SerialPort");
    var System = Java.type("java.lang.System");
    var Thread = Java.type("java.lang.Thread");
    var JString = Java.type("java.lang.String");
    var ByteArray = Java.type("byte[]");
    var Files = Java.type("java.nio.file.Files");
    var Paths = Java.type("java.nio.file.Paths");
    var Options = Java.type("java.nio.file.StandardOpenOption");
    var ByteArrayOutputStream = Java.type("java.io.ByteArrayOutputStream");
    var PrintStream = Java.type("java.io.PrintStream");
    var mode = String(args[0]);
    var portName = String(args[1]).toUpperCase();
    var wakeSeconds = Number(args[3]);
    if (args.length !== 4 || (mode !== "import" && mode !== "sync-clock") ||
            !/^COM[1-9]\d*$/.test(portName) || !/^[1-9]\d*$/.test(String(args[3])) || wakeSeconds > 300) {
        throw new Error("Usage: import|sync-clock COMn CAPTURE_DIR WAKE_SECONDS (1–300)");
    }
    var path = Paths.get(String(args[2]));
    if (!Files.isDirectory(path)) throw new Error("The local capture directory must already exist.");
    var fileList = Files.list(path);
    try {
        if (fileList.findAny().isPresent()) throw new Error("Use an empty local capture directory.");
    } finally {
        fileList.close();
    }

    var client = new Communication();
    var passwordMethod = client.getClass().getDeclaredMethod("sendPassword", SerialPort["class"]);
    passwordMethod.setAccessible(true);
    var port = SerialPort.getCommPort(portName);
    var io = {
        now: function () { return Number(System.currentTimeMillis()); },
        monotonic: function () { return Number(System.nanoTime()) / 1000000; },
        sleep: function (millis) { Thread.sleep(Math.ceil(millis)); },
        progress: function (message) { System.out.println(message); System.out.flush(); },
        save: function (filename, raw) {
            Files.write(path.resolve(filename), new JString(raw).getBytes("ISO-8859-1"), Options.CREATE_NEW);
        },
        write: function (command) {
            var bytes = new JString(command).getBytes("US-ASCII");
            if (port.writeBytes(bytes, bytes.length) !== bytes.length) throw new Error("The serial write did not complete.");
        },
        login: function () {
            // Suppress the vendor method's verbose serial output while preserving its regular login.
            var standardOut = System.out;
            var discarded = new PrintStream(new ByteArrayOutputStream());
            try {
                System.setOut(discarded);
                return !!passwordMethod.invoke(client, port);
            } finally {
                System.setOut(standardOut);
                discarded.close();
            }
        },
        receive: function (complete, timeout, quiet) {
            var until = io.monotonic() + timeout;
            var lastByte = io.monotonic();
            var raw = "";
            try {
                while (io.monotonic() < until) {
                    var available = port.bytesAvailable();
                    if (available < 0) return {raw: raw, failed: true};
                    if (available > 0) {
                        var bytes = new ByteArray(Math.min(available, 4096));
                        var length = port.readBytes(bytes, bytes.length);
                        if (length < 0) return {raw: raw, failed: true};
                        if (length > 0) {
                            raw += String(new JString(bytes, 0, length, "ISO-8859-1"));
                            lastByte = io.monotonic();
                            if (raw.length > 65536) return {raw: raw, failed: true};
                        }
                    }
                    if (io.monotonic() - lastByte >= quiet && complete(raw)) return {raw: raw, complete: true};
                    io.sleep(20);
                }
            } catch (error) {
                return {raw: raw, failed: true};
            }
            return {raw: raw, complete: false};
        }
    };

    port.setComPortParameters(57600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
    port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
    if (!port.openPort()) throw new Error("Could not open " + portName + ". Close HUG or any other serial tool and check the port.");
    try {
        port.clearDTR();
        port.clearRTS();
        io.progress(portName + " open. Checking for an existing diagnostics session.");
        runSession(io, mode, wakeSeconds);
    } finally {
        port.closePort();
    }
}

if (typeof LH40_USB_LIBRARY === "undefined") {
    try {
        main(arguments);
    } catch (error) {
        var system = Java.type("java.lang.System");
        system.err.println("USB session failed. " + error.message);
        system.exit(1);
    }
}
