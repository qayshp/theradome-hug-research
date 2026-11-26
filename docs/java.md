# Inside the HUG desktop application

HUG's Windows download is an Inno Setup installer. It installs a **Launch4j 3.12** executable, `HugUpgrade.exe`, alongside a private **AdoptOpenJDK 8u202** runtime. The installer contains the marker `Inno Setup Setup Data (6.0.0)`. The installed launcher is a native 32-bit x86 executable with a JAR archive starting at byte 51,712. Its Windows resource table names `jre` as the runtime directory. The bundled `jre/bin/javaw.exe` reports product version `8.0.2020.8`. [Windows installer](https://apps.theradome.com/downloads/HugApplication.exe) · [Launch4j documentation](https://launch4j.sourceforge.net/)

The Mac ZIP contains `Resources/myapp.jar` and its own `Resources/jre/bin/java`. Opening the Windows launcher as a ZIP and comparing its entries with the Mac JAR gave the same 629 names, including directory entries, and identical decompressed bytes for every name. One set of Java listings covers both applications. Both manifests select `com.screens.Splash` as the main class. [Mac archive](https://apps.theradome.com/downloads/HugApplication-V200.zip) · [Acquisition history](discovery.md)

## Reading the Java

| Operation | Where to look | Finding |
|---|---|---|
| Start and select a helmet | [Splash](../java/com/screens/Splash.java) · [Choose](../java/com/screens/Choose.java) | Swing starts the interface and offers PRO LH80 and EVO LH40 selections. |
| Find a device | [Connect](../java/com/screens/Connect.java) · [Communication](../java/SerialCommunication/Communication.java) | The desktop code enumerates serial ports with jSerialComm. Connection detection, version reads and the transfer worker live in a separate Java package. |
| Check a version | [FirmwareUpdateMenu](../java/com/screens/FirmwareUpdateMenu.java) · [DownloadVersion](../java/com/screens/DownloadVersion.java) | The menu fetches `fwversion.txt`. Its reader interprets three ASCII digits at byte offsets 9–11. The menu compares the resulting number with the version obtained from the device and offers an upgrade prompt on either comparison outcome. |
| Fetch the HEX | [FirmwareUpdateFetch](../java/com/screens/FirmwareUpdateFetch.java) · [DownloadUtil](../java/com/screens/DownloadUtil.java) | The fetch screen downloads `my.hex` to the user's home directory. The screens carry the model selection, but this method uses the same fixed URL for both models. |
| Select the programming image | [FirmwareUpdateProgram](../java/com/screens/FirmwareUpdateProgram.java) · [Communication](../java/SerialCommunication/Communication.java) | The programming screen passes `/myTheradome.hex` to `startCommThread`. That method opens the named resource twice with `Class.getResourceAsStream`. The worker reads one stream to count the file length and the other to consume the HEX. |
| Contact support | [Contact](../java/com/screens/Contact.java) · [SMTPConfig](../java/com/screens/SMTPConfig.java) | The contact feature constructs an email using JavaMail. The distributed source listing redacts contact and authentication data. |

The normal update menu requires the online version response. The subsequent HEX-fetch method returns the same status after a successful download and after displaying its ordinary HTTP failure screen. Its caller can attempt to enter programming mode in either case. The programming worker reads the bundled classpath resource. The online HEX and the bundled resource matched byte for byte in this investigation. [Fetch and return handling](../java/com/screens/FirmwareUpdateFetch.java#L105) · [Firmware acquisition and hashes](discovery.md) · [Recovery limitations](verification.md#backup-and-recovery)

One extra class, `DownloadHexGithub`, contains a standalone `main` that requests `version.txt`. The JAR manifest launches `Splash`, and the menu code calls `DownloadVersion` with `fwversion.txt`. The bundled `version.txt` says `version: 129` and the recovered server `fwversion.txt` says `version: 126`. A number in either text file is insufficient to identify the firmware running on an individual helmet. [Standalone helper](../java/com/screens/DownloadHexGithub.java) · [Menu code](../java/com/screens/FirmwareUpdateMenu.java)

## Coverage and checks

The JAR contains **79 application class files** targeting Java 8 bytecode (major version 52). There are 77 in `com/screens` and two in `SerialCommunication`. Inner and anonymous classes account for the difference between 79 class files and the **22 top-level Java listings** in this repository. The bundled JavaMail, jSerialComm, native libraries, artwork and runtime are separate dependencies and resources. The archive's `module-info.class` describes the jSerialComm dependency.

CFR 0.152 produced all 22 listings. An independent rerun reproduced every line of the original decompiler output before the publication edits below. CFR reported that it could not fully reconstruct structured control flow in two methods of `Communication`. Its generated `** GOTO` markers remain visible in those methods. The listings serve as a readable record of the bytecode. A Java application build from these listings has not been demonstrated. The PIC assembly rebuild documented elsewhere applies only to the helmet firmware. [CFR](https://www.benf.org/other/cfr/) · [PIC firmware rebuild](../model/rebuild.md)

### Editorial redactions in the Java listings

Each `/* [EDITORIAL REDACTION] … */` comment marks the exact position of an expression the repository editors withheld. The space in the Java statement is deliberately incomplete. The comment describes what the original application supplied there. There are five such omissions across four files.

| Listing | Expression omitted |
| --- | --- |
| [Contact.java](../java/com/screens/Contact.java#L81) | The contact form’s recipient email address. |
| [FirmwareUpdateMenu.java](../java/com/screens/FirmwareUpdateMenu.java#L113) | The support form’s recipient email address. |
| [SMTPConfig.java](../java/com/screens/SMTPConfig.java#L36) | The sending account address and its SMTP password, each marked separately. The account address also served as the support form’s recipient. |
| [Communication.java](../java/SerialCommunication/Communication.java#L471) | The byte-array initializer containing the helmet login credential. The surrounding method shows how HUG sends the request, checks the reply, and returns success or failure. |

The first public revision used quoted placeholder strings for the mail fields and showed the login method as a stub that always returned `false`. Those were publication edits. This transcription preserves the original decompiler output around each omission, including both login outcomes. A line-by-line comparison found no other differences across the 22 Java listings apart from line endings. The firmware HEX and binary are preserved byte for byte and are outside the scope of these Java redactions.

## Reproducing the static inspection

With Python 3 and a JDK, extract the Mac JAR into a local scratch directory, then use `javap` to inspect the actual instructions. These commands read the archive and class data without launching the application.

```sh
python -c "from pathlib import Path; from zipfile import ZipFile; Path('myapp.jar').write_bytes(ZipFile('HugApplication-V200.zip').read('Resources/myapp.jar'))"
javap -classpath myapp.jar -p -c com.screens.FirmwareUpdateFetch
javap -classpath myapp.jar -p -c com.screens.FirmwareUpdateProgram
```

For a Java transcription of the fetch screen, extract that class and provide its archive as the decompiler's dependency classpath. The two `javap` results above independently show the download URL and the bundled resource passed to the programming worker. A separate `javap` check confirmed the classpath read inside the worker.

```sh
jar xf myapp.jar com/screens/FirmwareUpdateFetch.class
java -jar cfr-0.152.jar com/screens/FirmwareUpdateFetch.class --extraclasspath myapp.jar --outputdir inspection
```

The Windows package can be extracted with innoextract. The application archive inside `app/HugUpgrade.exe` can then be read using Python's `zipfile.ZipFile`. ZIP readers that support an executable prefix locate its central directory without running the native code. [innoextract](https://constexpr.org/innoextract/)

| Compared artifact | Bytes | SHA-256 |
|---|---:|---|
| Mac `Resources/myapp.jar` | 2,033,121 | `27ddcc2f7fb6f590e9ef0977327d1150ffb20d792a3f5b59ee7a8647a37c37b9` |
| Windows `app/HugUpgrade.exe` | 2,061,135 | `890d6a2960c06677f7988a6a474ec923f8770bd32fcb888660c89215b7cc9b1d` |
| CFR 0.152 used here | 2,162,315 | `f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2` |
