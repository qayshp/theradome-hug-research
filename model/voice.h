#ifndef THERADOME_VOICE_H
#define THERADOME_VOICE_H

#include <stdbool.h>
#include <stdint.h>

typedef struct {
    uint8_t index;            /* user selection, RAM 0x044 */
    uint8_t shift;            /* current attenuation, RAM 0x2B9 */
    uint8_t requested_state;  /* RAM 0x2B4 */
    uint16_t slot7;           /* EEPROM word slot 7 */
} VoiceState;

typedef struct {
    uint16_t number;          /* number passed to the speech routine for a time report */
    uint8_t clip;             /* 0 = no request, 0x2D = time suffix, 0x31 = setting sample */
    uint8_t shift_at_request;
} VoiceRequest;

/* One foreground button dispatch in states 7 and 8. Returns false for other
   modes, unfinished or unknown button events, or an invalid selection. Stops
   before the main loop applies requested_state. Output describes a request to
   the playback code. Hardware and its audio-enable gate determine audibility. */
bool voice_button(VoiceState *voice, uint8_t mode, uint8_t event,
                  uint16_t ticks, VoiceRequest *request);

/* Call from the automatic reporting site at 0x5E26 when its caller's state and
   flag checks have already passed. The tick snapshots are of RAM 0x26F:0x270. */
VoiceRequest voice_automatic_report(const VoiceState *voice, uint16_t ticks);

/* PWM duty from the original unsigned 16-bit decoder result. */
uint16_t voice_duty(uint16_t sample, uint8_t shift);

#endif
