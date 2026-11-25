#ifndef THERADOME_INPUTS_H
#define THERADOME_INPUTS_H

#include <stdbool.h>
#include <stdint.h>

typedef enum {
    BUTTON_INT2_FALLING,
    BUTTON_INT3_RISING,
    BUTTON_TIMER3_OVERFLOW
} ButtonInput;

typedef struct {
    uint8_t event;             /* RAM 0x0F3, also read between interrupts */
    bool started;              /* RAM 0x27B bit 7 */
    bool saw_rising_edge;      /* RAM 0x04F bit 1 */
    bool int2_enabled;
    bool int3_enabled;
    uint16_t interval_ticks;   /* Preload interval for the active timer IRQ, or zero */
} ButtonState;

/* Start with a zero-initialized ButtonState. */
void button_enable(ButtonState *state, bool enabled);
void button_input(ButtonState *state, ButtonInput input);

typedef enum {
    CHARGE_ERROR,
    CHARGE_TEST_MODE,
    CHARGE_CHARGING,
    CHARGE_CHARGED
} ChargeText;

/* Select the diagnostic text for stable inputs, as in 0x4C6E..0x4CE6. */
ChargeText charge_text(bool ra4, bool rb4, uint8_t mode);

/* Next operating state proposed by 0xC370. A value of -1 means application restart. */
int charge_next_state(bool ra4, bool rb4, uint8_t current_state);

#endif
