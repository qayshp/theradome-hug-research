# Finding the HUG installers and firmware

The two installers came from Theradome's own download host. Opening the Mac package exposed a Java archive that contained both the helmet firmware and the address HUG uses to fetch a HEX file. The firmware from the package, the pinned Git revision and the Software Heritage archive has the same SHA-256. This investigation began with an EVO LH40. The image recovered through HUG contains LH80 identification strings. [Theradome HUG page](https://theradome.com/pages/hug) · [Pinned firmware](https://raw.githubusercontent.com/renuraja03/fwupdate/54d3c7583bfe1d474c832fa3fee7bb71462f8445/my.hex) · [Software Heritage content record](https://archive.softwareheritage.org/api/1/content/sha1_git:77276d2f4ef6dfce35eb8212fac5dda62ead7439/)

Dates below use Pacific time.

## The search trail

1. **Recover the filenames.** On November 24, 2026, the June 29, 2022 Wayback capture of Theradome's `/hug` page revealed Windows and Mac download links. Both pointed to `www.theradome.com/wp-content/uploads/2019/10/`. The filenames were `HugApplication.exe` and `HugApplication-V200.zip`. Those URLs returned HTTP 404 during the investigation and again on November 25. The directory date is part of the historical URL and does not establish a release date. [Archived HUG page](https://web.archive.org/web/20220629065518/https://theradome.com/hug)
2. **Check what the archives actually held.** Searches for the exact URLs, with and without `www`, and Wayback filename wildcard queries found the landing page but no archived installer payload. Arquivo.pt returned no record for the queried installer URLs. Searching web indexes for both exact filenames and likely model terms, including `LH40`, `Theragrow` and `BL4`, yielded no verified alternative build. Theradome's white paper illustrates the HUG update and USB workflow in Figure 15. It supplies no download endpoint for an LH40-specific image. [Wayback](https://web.archive.org/) · [Arquivo.pt](https://arquivo.pt/) · [Theradome white paper, Figure 15](https://m.media-amazon.com/images/I/918RN9b8aCL.pdf#page=15)
3. **Follow the live landing page as well as the old file links.** An early conclusion that the installers had disappeared was premature. Requesting the original `https://theradome.com/hug` landing-page route followed a redirect to `/pages/hug`. Its HTML contained the same two filenames with a different host and path, `apps.theradome.com/downloads/`. Both downloads succeeded. Repeating the requests on November 25, 2026 returned HTTP 200 and reproduced the lengths and hashes below. [Live redirect](https://theradome.com/hug) · [Current HUG landing page](https://theradome.com/pages/hug)
4. **Inspect the packages without starting HUG.** The Mac ZIP contains `Resources/myapp.jar`. Opening this JAR as a ZIP reveals the root resource `myTheradome.hex`. The Windows download is an Inno Setup installer containing a Launch4j launcher, `HugUpgrade.exe`, with an appended JAR. All 629 archive entries were compared by name and uncompressed content with the Mac JAR and matched. The Windows distribution also includes a Java 8u202 runtime. [Windows installer](https://apps.theradome.com/downloads/HugApplication.exe) · [Mac ZIP](https://apps.theradome.com/downloads/HugApplication-V200.zip)
5. **Read the updater's own URL strings.** In the decompiled Java, `com.screens.FirmwareUpdateFetch.getHexFile()` names `raw.githubusercontent.com/renuraja03/fwupdate/master/my.hex`. The firmware menu names `fwversion.txt` on the same branch. A separate class, `DownloadHexGithub`, still refers to `version.txt`, which returned HTTP 404 on November 25. Git history records a rename of that text file. `FirmwareUpdateProgram` also passes the resource name `/myTheradome.hex` to the communication code. In the traced programming path, `SerialCommunication.Communication` opens that argument with `getResourceAsStream`. It therefore reads the bundled JAR resource. The separate fetch step downloads a file to disk. Both files have identical bytes in the distribution examined. [Java trace](java.md) [Firmware endpoint](https://raw.githubusercontent.com/renuraja03/fwupdate/master/my.hex) · [Version endpoint](https://raw.githubusercontent.com/renuraja03/fwupdate/master/fwversion.txt) · [Version-file rename](https://github.com/renuraja03/fwupdate/commit/8bf509f)
6. **Find an archive of the firmware itself.** The Git blob ID leads to a Software Heritage content record. One request during the original search saved a 508-byte HTML error page with HTTP 403. On November 25, a fresh request for the raw content returned HTTP 200, 111,967 bytes of Intel HEX and the exact expected SHA-256. The independently retrieved metadata agreed on both length and digest. [Archived bytes](https://archive.softwareheritage.org/api/1/content/sha1_git:77276d2f4ef6dfce35eb8212fac5dda62ead7439/raw/) · [Archive metadata](https://archive.softwareheritage.org/api/1/content/sha1_git:77276d2f4ef6dfce35eb8212fac5dda62ead7439/)

## Artifact fingerprints

The following are SHA-256 digests of the complete files as downloaded or extracted. The ZIP and EXE are distinct distribution packages. The HEX digest includes its original text formatting and CRLF line endings.

| File | Bytes | SHA-256 |
|---|---:|---|
| [HugApplication.exe](https://apps.theradome.com/downloads/HugApplication.exe) | 28,481,268 | `238634453318259dbd0fb07993a2c41c4f27ed11dc5102ab35f061dd2a2b3d25` |
| [HugApplication-V200.zip](https://apps.theradome.com/downloads/HugApplication-V200.zip) | 45,691,094 | `fb6d297db37e1c89431c874d2f2bb34805717bb22159a022cc945afa2e682e3d` |
| `Resources/myapp.jar` inside the Mac ZIP | 2,033,121 | `27ddcc2f7fb6f590e9ef0977327d1150ffb20d792a3f5b59ee7a8647a37c37b9` |
| `myTheradome.hex` inside the JAR, also published upstream as [my.hex](https://raw.githubusercontent.com/renuraja03/fwupdate/54d3c7583bfe1d474c832fa3fee7bb71462f8445/my.hex) | 111,967 | `40caa7fd679e70205a8acc923f5586484884060e9ef4eb0cfaa4c4d23f120c69` |

Decoding the Intel HEX records gives 50,816 consecutive application bytes at addresses `0x1900–0xDF7F`. Hashing these bytes in ascending address order gives `3b4eb6bdb7e535760f1ee14cd5687c8a3b20739a4ec0f2d406f5fcaeb813ef55`. The strings at byte addresses `0x1C4D` and `0x1C78` identify the command menu and board test as LH80. A printable-string search did not find an LH40 or EVO identifier. [Rebuild notes](../model/rebuild.md)

## What the Git history tells us about versions

The upstream repository has nine commits on the inspected branch, all from August 2019. In the first commit its author described the addition as “sample files.” The second commit renamed `myTheradome.hex` to `my.hex`. Examining the tree at **every commit** found the same firmware Git blob, `77276d2f4ef6dfce35eb8212fac5dda62ead7439`, despite several changes to the version file. The upstream branch was rechecked on November 25, 2026. [First commit](https://github.com/renuraja03/fwupdate/commit/b8b654085d3baafbe9108cc399067d438ef027f7) · [Commit history](https://github.com/renuraja03/fwupdate/commits/master/)

| Commit (oldest first) | Firmware filename | Value in the version file |
|---|---|---:|
| `b8b6540` | `myTheradome.hex` | 129 |
| `ad6e055` | `my.hex` | 129 |
| `4926588` | `my.hex` | 126 |
| `8bf509f` | `my.hex` | 126 |
| `235673c` | `my.hex` | 129 |
| `250d254` | `my.hex` | 126 |
| `7a2367f` | `my.hex` | 125 |
| `7662bb0` | `my.hex` | 127 |
| `54d3c75` | `my.hex` | 126 |

At the pinned revision the file reads `version: 126`. These records establish that HUG refers to this repository and that one set of HEX bytes was served with multiple version values. They leave the installed revision of any individual helmet, the release status of the image and its compatibility with an LH40 unresolved. [Pinned version file](https://raw.githubusercontent.com/renuraja03/fwupdate/54d3c7583bfe1d474c832fa3fee7bb71462f8445/fwversion.txt)

## Archive and community search coverage

* **Wayback.** A saved Theradome domain and subdomain CDX listing contained 14,669 rows. The June 2022 landing-page capture included the old links. The check found no matching capture of either installer under the searched filenames. An earlier broad request timed out. Together these results explain why the first attempt recovered useful HTML but no executable. [Archived landing page](https://web.archive.org/web/20220629065518/https://theradome.com/hug)
* **Common Crawl.** An initial exact Windows-file lookup returned no match and a ZIP lookup failed. A later domain and subdomain index search checked raw index records across multiple crawl collections. The newer batch returned results for 61 of 69 collections. Combining it with the older batch and retries by unique collection identifier gives **116 successful collections out of 117 queried**, spanning `CC-MAIN-2015-06` through `CC-MAIN-2026-47`. `CC-MAIN-2015-32` still returned HTTP 503. `CC-MAIN-2022-27` indexed the HUG landing page. The saved records contained neither of the two installer filenames nor a separate LH40 firmware image. Coverage and server errors limit any claim about what other captures might exist. [Common Crawl index catalog](https://index.commoncrawl.org/collinfo.json) · [2022-27 HUG index lookup](https://index.commoncrawl.org/CC-MAIN-2022-27-index?url=theradome.com%2Fhug&output=json)
* **First-hand reports.** On November 24, 2026, searches combined “Theradome HUG,” “Helmet UpGrade,” the installer filenames, “USB,” “firmware” and “diagnostics” with Reddit and hair-loss forum terms. An owner on HairLossTalk mentioned PC connectivity for downloads. The post gave no HUG session, diagnostic result, or before-and-after version number. The search did not find a first-hand report establishing an LH40 update or a change in volume behavior. [HairLossTalk owner discussion](https://www.hairlosstalk.com/interact/threads/theradome-lazer-helmet.75469/page-3)
