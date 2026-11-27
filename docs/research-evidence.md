# Archive and firmware test evidence

The archive search recovered the vendor installers and the stock application. The second of the first two experimental builds was installed on the EVO LH40. Later updates extended the volume selection to ordinary treatment prompts, reworked warning and fault speech within the live-tested range, and applied the selected gain to the manually requested treatment count. The repository records installer fingerprints, source, and reported results. The vendor packages, raw archive exports, raw device captures, local updater bundle, and compiled experimental images are held in the local research archive. [Discovery and fingerprints](discovery.md) · [Original live test](volume-settings.md#live-evo-lh40-test-on-november-26-2026) · [Latest live update](volume-settings.md#live-count-readout-update)

## Repeating the archive search

| Search | What to compare | Saved result |
| --- | --- | --- |
| Internet Archive | Search the old Windows and Mac installer URLs individually with and without `www`. Check the host's capture index for `HugApplication.exe` and `HugApplication-V200.zip`. Use the archived `/hug` landing page to confirm the old paths. | The saved host index had 14,669 rows and no matching installer filename. The June 2022 page supplied both old links. A separate broad request timed out. |
| Arquivo.pt | Query the old installer URLs and inspect domain and subdomain results for the same filenames. | The queried installer URLs yielded no archived payload. |
| Common Crawl | Query the installer filenames and the `/hug` page separately. Then scan the domain and subdomain URL keys in each collection, retrying failures and counting each successfully read collection once. Look for the two filenames, the `/hug` path, firmware-related names, and file extensions used for installers and firmware. | The saved batches and retries covered 117 distinct collections, with successful reads from 116 and 16,688 rows total. Among those rows, the broadened path and filename screen matched one distinct URL, `https://theradome.com/hug`. Collection `CC-MAIN-2015-32` remained unavailable during the search. |
| Vendor site and firmware archives | Follow the live `/hug` redirect and compare the final download paths with the captured page. Verify the installer files against the published digests. Compare the stock firmware inside the Mac and Windows packages with the pinned upstream file and the Software Heritage content record. | The current installer paths were on the vendor's download host. The stock application matched the pinned source and archived bytes by digest. |

The Common Crawl bulk check used the public sorted URL index. For each collection it found the blocks spanning the reversed host key `com,theradome`, fetched and decompressed those blocks, and filtered matching domain and subdomain records. The row count includes repeat captures across collections. Archive indexing, query timeouts, and service errors limit the coverage of these results. [Wayback landing-page capture](https://web.archive.org/web/20220629065518/https://theradome.com/hug) · [Arquivo.pt URL API](https://dados.gov.pt/en/datasets/cdx-server-api-url-search) · [Common Crawl index](https://index.commoncrawl.org/) · [Common Crawl landing-page hit](https://index.commoncrawl.org/CC-MAIN-2022-27-index?url=theradome.com%2Fhug&output=json) · [Provider and archive comparison](discovery.md#the-search-trail)

## Distinguishing the two builds

These were the first two volume experiments. Later updates build on the second experiment. [Later live test results](volume-settings.md#live-count-readout-update)

| Evidence | Requested announcement only | Persistent setting and countdowns |
| --- | --- | --- |
| Scope | A single press in the active or paused state uses the selected gain for both the number and its suffix, then restores the prior effective gain. Automatic announcements retain the stock behavior. | The selected volume survives a simulated restart. Both requested and automatic announcements use that selection. |
| Image comparison | 48 application bytes changed, all within two original button handlers. The original address range was preserved. | 13 stock bytes changed across five hooks. The added routines occupy three more HEX records. |
| Exact HEX SHA-256 | `43cc538cf91870e9b0f18f3d5dda60d85923c1c613c95f6281a2409fda2dc967` | `5ce52a11605881b2da8c2ef1bf3a5715e2cd304eaf1b04d77c61b979a82ea1f6` |
| Host test | 1,008 cases, including every tested pairing of selected level and prior effective gain. Both speech requests used the selected level. The post-call RAM and counter matched stock. | 1,136 cases covering startup, save and restart, both countdown paths, and long-press behavior. |
| Physical test | The first build remained offline. | This image was transferred to the LH40. The device responded after reboot and the owner heard the requested announcement at the selected level. |

The first experiment has its own source patch, HEX encoder, and verifier. Its verifier compares decoded instructions, speech requests, and resulting state with the archived application. The later build's exact assembly overlay, builder, and verifier are available in the source commit used for the live test. Both local HEX files matched their recorded SHA-256 values, and both offline verifiers passed on rerun. [First experiment source and build notes](../model/experiments/requested-volume/README.md) · [First experiment results](verification.md#offline-build-and-volume-change) · [Second experiment source commit](https://github.com/qayshp/theradome-hug-research/commit/994518261dcfdc6da08d11030e52c03f6ecd04ab) · [Second experiment build and tests](volume-settings.md#offline-build-and-test)

## What the original transfer records establish

| Observation | Evidence |
| --- | --- |
| Initial attempt | The host verified the image digest and stopped after receiving zero bytes during the identification window. The log records no transfer. |
| Successful attempt | The host checked the same digest, read revision `126.6`, recorded loader readiness, then recorded the loader's `Success` reply and worker status `3`. |
| After reboot | A separate session returned revision `126.6`. That revision is also reported by the stock program. The listening test supplied the independent observation of changed behavior. |
| After calibration | The later EEPROM display supplies the second set of values in the published comparison. It is a selected parameter display. |

The expanded build also received the bootloader's `Success` reply, and the application returned `126.6` in a fresh USB session. After the 797-row warning refactor, the owner reported that ordinary use followed the selected volume away from the charger. After the count-readout update and calibration, the owner confirmed that the short-press count played quietly on laptop USB. Physical listening checks of the automatic countdowns and retention across a later physical restart remain pending. Direct flash readback, interrupted-update recovery and stock reinstallation on this helmet remain unverified. [Expanded update](volume-settings.md#expanded-treatment-prompt-update) · [797-row refactor](volume-settings.md#live-797-row-refactor) · [Count-readout update](volume-settings.md#live-count-readout-update) · [Recovery coverage](verification.md#backup-and-recovery)
