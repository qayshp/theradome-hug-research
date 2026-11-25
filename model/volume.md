# Voice volume and button events

These addresses refer to the HUG image in `outputs/myTheradome-LH80-labeled.hex`. Addresses in the left column of the disassembly are byte addresses in program memory. Addresses described as RAM belong to the separate data address space.

## Selection and playback

| RAM | Meaning in this image | Evidence |
| --- | --- | --- |
| `0x0F3` | Button event code. Code 5 reaches the four-level volume control in several operating states. | `0x84D8–0x84E6`, `0x7320–0x7324` |
| `0x044` | Selected volume index. The update increments the byte and wraps values greater than 3 to 0. | `0x72AC–0x72B6` |
| `0x2B9` | Effective playback attenuation. The audio loop uses this byte as the number of right shifts applied to a decoded sample. | `0x74EC–0x751A`, `0x9596–0x95EE`, `0x96C4–0x96CA` |
| `0x2B5` | Current operating-state number passed to the button handler. The transition code copies the requested state from `0x2B4`. | `0x6056–0x6062`, `0x610C–0x6120` |

| Index | Shift count | Effect on the digital sample |
| --- | --- | --- |
| 0 | 0 | Full level |
| 1 | 1 | Half scale, rounded down |
| 2 | 3 | One eighth scale, rounded down |
| 3 | 14 | Silent. The shift leaves at most two low bits in a 16-bit sample. The output routine uses bits 2 through 11, so its duty command is zero. |

The routine at `0x94F2` checks an audio-enable flag before playing. The path at `0x7502–0x751A` requests clip `0x31` as feedback after applying the shift count. Theradome's guide identifies the usual confirmation as the spoken word “Theradome” and describes the four levels in this order. Matching that word to the numeric clip is an inference from the guide. The instruction trace identifies the numeric request directly. Sound pressure at the ear depends on the speaker and drive circuit. [Theradome volume guide](https://theradome.com/a/docs/troubleshooting/adjusting-sound-volume-on-theradome)

The high and low nibbles of each audio byte are decoded separately at `0x95A2` and `0x95CC`. Routine `0xAF22` uses a 49-entry step table at `0x1A48` and a 16-entry index adjustment table at `0x1AAA` for adaptive differential PCM. After each nibble the playback loop shifts the decoded sample and sends it to the output routine at `0x9672`. Silent can therefore consume playback time without producing the spoken confirmation.

For a check independent of the listing's register names, an instruction interpreter executed the original words from `0x7082` through the call site at `0x751A`. With event 5 it exercised four starting indices in each of states 0, 4, 5, 7, 8, 9, 0x12 and 0x19. All 32 runs produced the expected next index, effective shift, and clip number. This check ended at the playback call and did not simulate the speaker or interrupts.

## Ways a prompt can sound loud

* **A temporary override.** Button events 2 and 3 save the previous effective attenuation to RAM `0x0E9` and clear the live value at `0x70A4–0x70AC`. The applicable state handler can play clips at full digital level. On the ordinary returning path the previous attenuation is copied back at `0x7520–0x752A` when the saved state number differs from `0x11`. Paths that restart the program do not reach this return. Routine `0x80BA` separately saves the effective setting in RAM `0x2A0`, clears it, and restores it at `0x83F8`. Both operations can make one prompt louder while the index survives.
* **A change of state.** Some state entry routines explicitly clear the effective attenuation without writing the volume index. For example, entering state `0x09` reaches `0x62DA`. At `0x62F4` it sets full level and, when RAM `0x2B8` equals 2, requests clip `0x37`. This allows a previously chosen index and the actual playback level to disagree.
* **A hold in state 5.** Event 3 clears both the live attenuation and index at `0x732C–0x7330` and requests state `0x0B`. The common exit restores the previous attenuation on the ordinary returning path, but the index stays at 0. This is another route to a selection reset and a mismatch between the two bytes.
* **A controller restart.** The application reset vector reaches startup code `0xCD42`. Its zero-fill of `0x20E–0x2EC` includes the attenuation byte. Its zero-fill of `0x033–0x04C` includes the selection. The identified volume-change branch has no EEPROM save. No matching EEPROM restore of the index was identified. Starting at the reset vector therefore initializes both to loud. Whether the helmet's apparent “off” state caused a true reset or a sleep/wake transition requires another observation.

## Interpreting a report

State `0x05` has another way to lose the selection. A long press there clears the index at `0x7330` and requests state `0x0B`. The common button return restores the saved effective attenuation at `0x7526`, leaving the index reset. A later double press can therefore advance to Medium even if the immediately preceding ordinary prompt sounded soft. The same long press during a session takes a different path in states 7 and 8.

If a double press produced the soft confirmation, one loud status prompt followed, and subsequent ordinary prompts were soft again, the temporary override is sufficient to explain that sequence. If ordinary prompts stay loud but the next double press yields silence, the index likely remained at Soft while effective attenuation had been cleared. If the next double press yields Medium, an index reset to Loud would explain it. These checks assume the button gesture was accepted in a state that advances the index. Failure to hear a confirmation alone leaves both Silent and disabled playback possible.

The state dispatcher at `0x7478` has explicit event-5 branches to the index increment for states `0x04, 0x05, 0x07, 0x08, 0x09`. Other entries, including `0x12` and `0x19`, also reach it by fall-through when the event differs from 2 and 3. The default branch at `0x7466` handles 5 the same way. State `0x11` skips ordinary volume feedback at `0x74E4–0x74EA`. The image supports several distinct state-specific outcomes for the same physical gesture. Assigning a numeric state to a real helmet still requires the preceding events or another indication.

## Time announcements in states 7 and 8

An ordinary button press, event 2, explicitly requests an announcement in both states at `0x7368` and `0x7384`. The firmware plays it with zero attenuation, even when the user selected Silent. On return it restores the previous shift. The automatic call to the same reporting routine at `0x5E26` retains the effective level that is in force there.

`0xCA60` reads the countdown at RAM `0x26F–0x270` through `0xD2B2(0)`. It computes `floor(ticks / 30) + 1` by unsigned integer division. On a requested report it sends that number to `0xA4C0` for conversion to speech and follows it with clip `0x2D`. On an automatic check it issues the request only for counter values 449, 299 and 149. These give 15, 10 and 5. A timer interrupt decrements the counter while nonzero (`0x1942–0x1968`). One entry into state 7 loads 600 at `0x5D9E–0x5DB0`. Together with a 20-minute session this supports **minutes remaining** as the meaning. The spoken wording of clip `0x2D` has not been recovered. At the exact 600-tick boundary the calculation gives 21. Whether an actual press can be recognized at that point depends on timer and state scheduling.

In states 7 and 8 a sustained hold (event 3) reaches the counter helper `0xD892(7)`, then requests state `0x0A`. That helper reads EEPROM slot 7, treats the erased value `0xFFFF` as zero, increments and writes it back. This is a persistent counter change on the hold path. The index, shift and slot are distinct. An EEPROM change after a hold alone would give no evidence that the volume selection has been saved.

The manufacturer independently describes spoken time remaining at 15 and 5 minutes. This agrees with the numeric requests in the firmware. [Theradome first session guide, steps 2 and 3](https://theradome.com/a/docs/guide/get-prepared-for-first-theradome-session)

### Compiling and comparing the C model

The public functions are in `model/voice.h`. `voice_button` takes a completed event and the current mode. The returned request describes the audio the firmware asks to play, including its effective gain. It ends at the foreground dispatch return, before the main loop acts on a requested mode change. `voice_automatic_report` models an invocation once the state and presence checks at its call site have succeeded. Audio flags, clip contents, edge timing and real-time scheduling are external inputs to this part of the model.

Run in Linux from the repository root with a C99 compiler and Python 3.

```sh
cc -std=c99 -fPIC -shared model/voice.c -o /tmp/theradome-voice.so
python3 -B model/voice_check.py /tmp/theradome-voice.so
```

The verifier executes the original gesture and timer arithmetic using firmware instruction words. It captures arguments at the speech and EEPROM boundaries, abstracts the hardware playback and supplies the EEPROM word. Across 992 cases it compared the selected index, live gain, desired state, stored word, spoken-number request, suffix or confirmation clip, and gain at the time of the request. Forty additional comparisons execute the original shift loop and PWM field writes for fixed sample values at all four gain settings. These are checks against this image on the host. These comparisons do not measure sound from a physical LH40.
