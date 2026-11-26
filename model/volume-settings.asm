; Sparse overlay for the stock HUG application at revision 126.
        processor 18f66k22
        radix hex

read_eeprom_word        equ     0xD3F8
begin_eeprom_write      equ     0xD80A
write_eeprom_word       equ     0xD6CE
button_feedback         equ     0x74DC
countdown_report        equ     0xCA60
countdown_speech_tail   equ     0xCACC
play_clip               equ     0x94F2
treatment_count         equ     0xCC56
count_phrase_argument   equ     0x0D4
clip_number             equ     0x081
wreg_address            equ     0xFE8
volume_index            equ     0x044
live_gain               equ     0x2B9
saved_auto_gain         equ     0x2A0
preference_word         equ     0x60
eecon1_access           equ     0x7F
eedata_access           equ     0x61
eeadr_access            equ     0x62
eeadrh_access           equ     0x63

        org     0x5358
        call    volume_startup, 0

; The treatment start uses a numbered message and an optional variant clip.
        org     0x5D86
        call    volume_count_prompt, 0
        org     0x5D9A
        call    volume_prompt_clip, 0

; The routine ready prompt uses two clips on the first pass and one on the next.
        org     0x5DBE
        call    volume_prompt_clip, 0
        org     0x5DCA
        call    volume_prompt_clip, 0
        org     0x5DE2
        call    volume_prompt_clip, 0

        org     0x5E26
        call    volume_auto_report, 0

; The two resume clips and the paused-state reminder follow the shared check.
        org     0x5E96
        call    volume_prompt_clip, 0
        org     0x5EA2
        call    volume_prompt_clip, 0
        org     0x5EB4
        call    volume_prompt_clip, 0

; Normal completion follows the stock log and counter update.
        org     0x5F24
        call    volume_prompt_clip, 0
        org     0x5F2A
        call    volume_count_prompt, 0

        org     0x72AC
        goto    volume_cycle

; Idle hold still clears the temporary gain at 0x732E.
        org     0x7330
        nop

; Both reports reach this block only after deciding to speak.
        org     0xCAC8
        goto    volume_speech

        org     0xDF80
volume_startup:
; Preserve the existing revision write before reading the new preference.
        call    write_eeprom_word, 0
        movlb   0
        clrf    0x67, 1
        movlw   preference_word
        movwf   0x66, 1
        call    read_eeprom_word, 0
        movlb   0
        movf    0x66, 0, 1
        xorlw   0x03
        andlw   0x03
        movwf   volume_index, 0
        rcall   volume_encode
        xorwf   0x66, 0, 1
        btfss   0xD8, 2, 0
        clrf    volume_index, 0
        rcall   volume_apply
        return  0

volume_cycle:
        incf    volume_index, 1, 0
        movlw   0x03
        cpfsgt  volume_index, 0
        bra     volume_save
        clrf    volume_index, 0
volume_save:
; The original button dispatcher has already disabled high-priority interrupts.
; Use the original write unlock, and leave interrupt rearming to that dispatcher.
        btfsc   eecon1_access, 1, 0
        bra     volume_save
        clrf    eeadrh_access, 0
        movlw   preference_word * 2
        movwf   eeadr_access, 0
        rcall   volume_encode
        movwf   eedata_access, 0
        bcf     eecon1_access, 7, 0
        bcf     eecon1_access, 6, 0
        bsf     eecon1_access, 2, 0
        call    begin_eeprom_write, 0
volume_save_wait:
        btfsc   eecon1_access, 1, 0
        bra     volume_save_wait
        bcf     eecon1_access, 2, 0
        goto    button_feedback

; Redundant one-byte codes for indices 0, 1, 2, 3 are C3, C6, C9, CC.
volume_encode:
        movlw   0xC3
        btfsc   volume_index, 0, 0
        xorlw   0x05
        btfsc   volume_index, 1, 0
        xorlw   0x0A
        return  0

volume_auto_report:
; The preceding foreground check has already used this byte to save gain.
        movff   live_gain, saved_auto_gain
        call    countdown_report, 0
        movff   saved_auto_gain, live_gain
        return  0

volume_speech:
        rcall   volume_apply
        movlb   0
        movff   0x25D, 0x0C8
        goto    countdown_speech_tail

volume_apply:
        movlw   0x03
        cpfsgt  volume_index, 0
        bra     volume_valid_index
        return  0
volume_valid_index:
        movlw   0x00
        btfsc   volume_index, 0, 0
        movlw   0x01
        btfss   volume_index, 1, 0
        bra     volume_store_gain
        movlw   0x03
        btfsc   volume_index, 0, 0
        movlw   0x0E
volume_store_gain:
        movlb   2
        movwf   0xB9, 1
        return  0

volume_prompt_clip:
; The stock foreground check also uses this location to save the gain.
        movff   live_gain, saved_auto_gain
        rcall   volume_apply
        movlb   0
        movff   clip_number, wreg_address
        call    play_clip, 0
volume_prompt_restore:
        movff   saved_auto_gain, live_gain
        return  0

volume_count_prompt:
; The first stock instruction at treatment_count copies WREG to this same byte.
        movff   live_gain, saved_auto_gain
        movff   wreg_address, count_phrase_argument
        rcall   volume_apply
        movff   count_phrase_argument, wreg_address
        call    treatment_count, 0
        bra     volume_prompt_restore
        end
