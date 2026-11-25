#include "voice.h"

static VoiceRequest time_report(uint16_t ticks, uint8_t shift)
{
    VoiceRequest request = { (uint16_t)(ticks / 30u + 1u), 0x2D, shift };
    return request;
}

VoiceRequest voice_automatic_report(const VoiceState *voice, uint16_t ticks)
{
    if (ticks == 449 || ticks == 299 || ticks == 149)
        return time_report(ticks, voice->shift);
    VoiceRequest silence = {0};
    return silence;
}

bool voice_button(VoiceState *voice, uint8_t mode, uint8_t event,
                  uint16_t ticks, VoiceRequest *request)
{
    static const uint8_t shifts[] = {0, 1, 3, 14};
    if ((mode != 7 && mode != 8) || voice->index > 3)
        return false;
    switch (event) {
    case 2:
        /* 0x70A4 temporarily forces full level during CA60(1). */
        *request = time_report(ticks, 0);
        return true;
    case 3:
        /* 0xD892 treats erased 0xFFFF as zero before incrementing. */
        voice->slot7 = voice->slot7 == UINT16_MAX ? 1 : (uint16_t)(voice->slot7 + 1);
        voice->requested_state = 0x0A;
        *request = (VoiceRequest){0};
        return true;
    case 5:
        voice->index = (uint8_t)((voice->index + 1u) % 4u);
        voice->shift = shifts[voice->index];
        *request = (VoiceRequest){0, 0x31, voice->shift};
        return true;
    default:
        return false;
    }
}

uint16_t voice_duty(uint16_t sample, uint8_t shift)
{
    if (shift >= 16)
        return 0;
    return (uint16_t)(((sample >> shift) >> 2) & 0x3FFu);
}
