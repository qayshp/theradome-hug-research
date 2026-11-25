#include "inputs.h"

void button_enable(ButtonState *state, bool enabled)
{
    /* 0xD9F8 clears the start bit and event on both paths. The next
       falling-edge handler clears the rising-edge latch. */
    state->event = 0;
    state->started = false;
    state->int2_enabled = enabled;
    state->int3_enabled = false;
    state->interval_ticks = 0;
}

static void finish_button(ButtonState *state, uint8_t event)
{
    state->event = event;
    state->int3_enabled = false;
    state->interval_ticks = 0;
}

void button_input(ButtonState *state, ButtonInput input)
{
    switch (input) {
    case BUTTON_INT2_FALLING: /* 0x847A */
        if (state->int2_enabled) {
            state->int2_enabled = false;
            state->int3_enabled = true;
            state->started = true;
            state->saw_rising_edge = false;
            state->event = 0;
            state->interval_ticks = 0x2000;
        }
        break;
    case BUTTON_INT3_RISING: /* 0x84B2 */
        if (!state->int3_enabled) break;
        if (state->event != 0) {
            finish_button(state, 2);
        } else if (state->saw_rising_edge) {
            finish_button(state, 5);
        } else {
            state->saw_rising_edge = true;
            state->interval_ticks = 0x2000;
        }
        break;
    case BUTTON_TIMER3_OVERFLOW: /* 0x850C */
        if (state->interval_ticks == 0) break;
        if (state->event != 0) {
            finish_button(state, 3);
        } else if (state->saw_rising_edge) {
            finish_button(state, 2);
        } else {
            state->event = 1;
            state->interval_ticks = 0xC000;
        }
        break;
    }
}

ChargeText charge_text(bool ra4, bool rb4, uint8_t mode)
{
    if (mode == 2) {
        return !ra4 && !rb4 ? CHARGE_TEST_MODE : CHARGE_ERROR;
    }
    if (!ra4 && rb4) return CHARGE_CHARGED;
    if (ra4 && !rb4) return CHARGE_CHARGING;
    return CHARGE_ERROR;
}

int charge_next_state(bool ra4, bool rb4, uint8_t current_state)
{
    if (ra4 && rb4) return 5;
    switch (current_state) {
    case 4:
    case 7:
    case 8:
    case 9:
        return 0x0A;
    case 0x0B:
        return current_state;
    case 0x1D:
        return -1;
    default:
        return ra4 ? 2 : 1;
    }
}
