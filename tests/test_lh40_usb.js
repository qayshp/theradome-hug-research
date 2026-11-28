// Run from the repository root with Node or the HUG bundle's Nashorn jjs.
var LH40_USB_LIBRARY = true;
var usb;
if (typeof load === "function") {
    load("tools/lh40_usb.js");
    usb = {runSession: runSession};
} else {
    var fs = require("fs");
    var vm = require("vm");
    usb = vm.createContext({LH40_USB_LIBRARY: true});
    vm.runInContext(fs.readFileSync("tools/lh40_usb.js", "utf8"), usb);
}

var BANNER = "\u001b[H\r\n********** THERADOME LH80  -  BOARD TEST *********\n\r" +
    "Press [?] for Menu...[ESC] to Exit\n\r";
var MENU = "********** THERADOME LH80  -  COMMAND MENU *********\r\n" +
    "[V] View Treatment Log\n\r[t] Display Date and Time\n\r[T] Set Date and Time\n\r" +
    "[D] Display EEPROM Parameters\n\r[ESC] Exit COM Mode\n\r";
var TREATMENT_HEAD = "\r\n****** TREATMENTS COMPLETED LOG ******\n\r" +
    "\t#\tDate\tDOW\tTime\tBattery\tTemp(C)\tLaser Current(mA)\n\r--------\n\r";
var ROW = "\t1\t05/08/2031\tTHU\t09:21:00\t4.00\t22.07\t10.48\n\r";
var EXPECTED_DATE_PROMPT = "Enter Date as YYYY-MM-DD-WW  WW - 00=Sunday..06=Saturday  - ";
var EXPECTED_TIME_PROMPT = "Enter Time as HH:MM:SS - ";
var START = new Date(2031, 4, 8, 14, 0, 0).getTime();

function parameters(total) {
    return "\r\nERROR COUNTERS\n\r0  " + total + "\n\r1  0\n\rSERIAL NUMBER - \xff\n\r" +
        "PRODUCT CODE REVISION - 126\n\rBOOT LOADER REVISION - 6\n\rUPGRADE INSTALL DATE - 00/00/0000\n\r";
}

function clock(epoch) {
    var date = new Date(epoch);
    function two(num) { return ("0" + num).slice(-2); }
    var weekday = ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"][date.getDay()];
    return "Date Read - " + weekday + "   " + date.getFullYear() + "-" + two(date.getMonth() + 1) + "-" +
        two(date.getDate()) + "\n\rTime Read - " + two(date.getHours()) + ":" + two(date.getMinutes()) + ":" +
        two(date.getSeconds()) + "\n\r";
}

function fake(responses, start) {
    var now = start === undefined ? START : start;
    var readIndex = 0;
    var io = {
        sent: [],
        captures: {},
        logins: 0,
        messages: [],
        progress: function (message) { io.messages.push(message); },
        now: function () { return now; },
        monotonic: function () { return now - START; },
        sleep: function (ms) { now += ms; },
        login: function () { io.logins++; return true; },
        write: function (command) { io.sent.push(command); },
        save: function (filename, raw) { io.captures[filename] = raw; },
        receive: function (condition, timeout, quiet) {
            if (readIndex >= responses.length) throw new Error("Unexpected serial receive");
            var response = responses[readIndex++];
            var raw = typeof response === "function" ? response(io) : response;
            var complete = condition(raw);
            now += complete ? quiet + 20 : timeout;
            return {raw: raw, complete: complete};
        }
    };
    return io;
}

function assert(condition, message) {
    if (!condition) throw new Error(message);
}

function sent(io, expected) {
    assert(JSON.stringify(io.sent) === JSON.stringify(expected), "Unexpected outgoing USB commands " + JSON.stringify(io.sent));
}

function fails(io, mode, expected) {
    try {
        usb.runSession(io, mode, 10);
    } catch (error) {
        assert(expected.test(error.message), "Unexpected failure " + error.message);
        return;
    }
    throw new Error("Expected USB operation to fail");
}

var validClock = clock(START);
var sessionStart = [BANNER, MENU, parameters(1), validClock];
var afterSet = function (io) { return clock(io.now()); };

var direct = fake(sessionStart.concat(TREATMENT_HEAD + ROW));
usb.runSession(direct, "import", 10);
sent(direct, ["?", "D", "t", "V", "\u001b"]);
assert(direct.logins === 0, "An existing Board Test session does not need another login");
assert(direct.captures["parameters.txt"] === parameters(1), "D raw bytes should be preserved");
assert(direct.captures["clock-before.txt"] === validClock, "The raw t response should be kept");
assert(/^\d+\n$/.test(direct.captures["clock-before-host-ms.txt"]), "Missing laptop time midpoint");
assert(direct.captures["treatment-count.txt"] === "1\n", "Missing EEPROM treatment count");
assert(direct.captures["treatments.txt"] === TREATMENT_HEAD + ROW, "Missing complete raw treatment table");
assert(!direct.messages.some(function (message) { return /Hold the master switch/.test(message); }),
    "An existing session should not ask for the switch hold");

var wake = fake(["", "", "Enter Password - ", MENU, parameters(1), validClock, TREATMENT_HEAD + ROW]);
usb.runSession(wake, "import", 10);
sent(wake, ["\r", "\r", "?", "D", "t", "V", "\u001b"]);
assert(wake.messages.some(function (message) { return /Hold the master switch/.test(message); }),
    "A quiet helmet should ask for the switch hold");
assert(wake.logins === 1, "The recognized prompt should use the vendor login once");

var incomplete = fake(["", "Enter Password - ", MENU, parameters(2), validClock, TREATMENT_HEAD + ROW]);
fails(incomplete, "import", /capture was incomplete/);
sent(incomplete, ["\r", "?", "D", "t", "V"]);
assert(incomplete.logins === 1, "Only the documented prompt should invoke vendor login");
assert(incomplete.captures["treatments.txt"] === TREATMENT_HEAD + ROW, "A partial table must be kept");

var erased = fake([BANNER, MENU, parameters(-1), validClock, TREATMENT_HEAD]);
usb.runSession(erased, "import", 10);
sent(erased, ["?", "D", "t", "V", "\u001b"]);
assert(erased.captures["treatment-count.txt"] === "0\n", "Erased counter should be zero");

var unknown = fake(["Random serial traffic\n\r"]);
fails(unknown, "import", /No recognized Board Test/);
sent(unknown, []);
assert(unknown.logins === 0, "Unrecognized port traffic must not invoke vendor login");

var differentRevision = fake([BANNER, MENU, parameters(1).replace("REVISION - 126", "REVISION - 125")]);
fails(differentRevision, "sync-clock", /revision 126\.6/);
sent(differentRevision, ["?", "D", "\u001b"]);

var done = fake(sessionStart.concat(EXPECTED_DATE_PROMPT, EXPECTED_TIME_PROMPT, "\n\rDate Set\n\r", afterSet, afterSet));
usb.runSession(done, "sync-clock", 10);
assert(/^14:00:\d{2}\r$/.test(done.sent[5]), "Expected one local time value and one CR");
sent(done, ["?", "D", "t", "T", "2031-05-08-04\r", done.sent[5], "t", "t", "\u001b"]);
assert(done.captures["clock-after.txt"] !== done.captures["clock-verify.txt"], "Both verification reads must be collected");

var midnight = fake(sessionStart, new Date(2031, 4, 8, 23, 59, 55).getTime());
fails(midnight, "sync-clock", /too close to local midnight/);
sent(midnight, ["?", "D", "t", "\u001b"]);

var unrecognized = fake(sessionStart.concat(EXPECTED_DATE_PROMPT, "Wrong time prompt"));
fails(unrecognized, "sync-clock", /uncertain.*Expected time prompt/);
sent(unrecognized, ["?", "D", "t", "T", "2031-05-08-04\r"]);

var midnightInSetter = fake(sessionStart.concat(EXPECTED_DATE_PROMPT, function (io) {
    io.sleep(24 * 60 * 60 * 1000);
    return EXPECTED_TIME_PROMPT;
}));
fails(midnightInSetter, "sync-clock", /uncertain.*date changed/);
sent(midnightInSetter, ["?", "D", "t", "T", "2031-05-08-04\r"]);

var noAck = fake(sessionStart.concat(EXPECTED_DATE_PROMPT, EXPECTED_TIME_PROMPT, "\n\rDate S"));
fails(noAck, "sync-clock", /uncertain.*did not acknowledge/);
sent(noAck, ["?", "D", "t", "T", "2031-05-08-04\r", noAck.sent[5]]);
assert(noAck.captures["clock-set-ack.txt"] === "\n\rDate S", "Incomplete acknowledgement should be preserved");

var invalidWeekday = function (io) { return clock(io.now()).replace("THU", "FRI"); };
var badClock = fake(sessionStart.concat(EXPECTED_DATE_PROMPT, EXPECTED_TIME_PROMPT, "\n\rDate Set\n\r", invalidWeekday, afterSet));
fails(badClock, "sync-clock", /acknowledged.*verification failed.*weekday/);
sent(badClock, ["?", "D", "t", "T", "2031-05-08-04\r", badClock.sent[5], "t", "t", "\u001b"]);

var stuck = fake(sessionStart.concat(EXPECTED_DATE_PROMPT, EXPECTED_TIME_PROMPT, "\n\rDate Set\n\r", validClock, validClock));
fails(stuck, "sync-clock", /verification failed.*advance/);
sent(stuck, ["?", "D", "t", "T", "2031-05-08-04\r", stuck.sent[5], "t", "t", "\u001b"]);

var message = "LH40 USB protocol tests passed (13 synthetic sessions; no USB port opened).";
if (typeof print === "function") print(message);
else console.log(message);
