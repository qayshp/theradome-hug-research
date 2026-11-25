# What the USB connection tells us about the LH40

The project started with an EVO LH40 connected to a Windows 11 ARM64 PC. Windows displayed the product string `LH 80 - Laser Helmet BL4` and USB IDs `0403:6015`. The manufacturer’s model name on the helmet, the bridge’s USB descriptor and the text inside the archived firmware are three independent observations. This distinction matters when comparing one device to an archived image.

## USB and charging

The USB IDs match FTDI’s FT-X bridge family. Windows initially reported driver error Code 28. Once the converter and serial-port drivers were associated, the device appeared as a healthy virtual COM port. That verified Windows enumeration. No live HUG handshake, installed firmware revision or device memory dump was captured. HUG itself enumerates serial ports and examines the replies. [FTDI driver guide](https://ftdichip.com/wp-content/uploads/2022/05/AN_396-FTDI-Drivers-Installation-Guide-for-Windows-10_11.pdf) · [HUG Java analysis](java.md)

Using the same data-capable cable, the owner observed charging on a wall adapter and no apparent charging on the PC. The current supplied by either source was never measured. The firmware can explain the master-switch display once the helmet recognizes a charge state. During ordinary charging it holds the green channel steady and varies red. Theradome describes a slowly changing light on the charger and specifies ten seconds of solid green for a full charge. [Charge inputs](../model/inputs.md) · [Indicator waveform](../model/indicator.md) · [Theradome charger guide](https://theradome.com/a/docs/troubleshooting/use-and-care-theradome-charger)

## Could the LH40 and LH80 share a controller?

There is stronger evidence for a shared design than the USB name. Table I of Theradome’s US patent 10,112,058 compares its 40- and 80-emitter designs. It lists the controller board as `TD-CONTROLLER-I`, firmware revision 109 and a 20-minute session for **both**. It lists different battery sizes and capacities. This establishes what the patent describes for that generation. HUG likewise presents two models in its interface and selects one bundled HEX in the programming path examined here. [Patent, Table I](https://patents.google.com/patent/US10112058B2/en) · [HUG application analysis](java.md)

The archive contains two LH80 diagnostic menu labels. The firmware’s use of timer, serial-peripheral and real-time clock registers points to the PIC18F87K22 family. The 18F66K22 assembler profile reproduces the bytes. The physical chip marking, board revision and application bytes on the examined LH40 remain unknown. A controller-board photograph could narrow the hardware identification. A read-only image comparison would answer the separate firmware-identity question. [Controller evidence](../model/controller.md) · [Firmware inventory](firmware.md)
