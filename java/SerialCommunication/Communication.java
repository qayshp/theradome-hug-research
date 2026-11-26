/*
 * Decompiled with CFR 0.152.
 */
package SerialCommunication;

import com.fazecast.jSerialComm.SerialPort;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import javax.swing.JOptionPane;

public class Communication {
    private int baudRate;
    private int dataBits;
    private int stopBits;
    private int parity;
    private SerialPort theradomeDevice;
    private SerialPort mainSerialPort;
    String mainPath;
    public int pBarVal = 0;
    public volatile int myCommThreadRun = 0;
    public volatile int programStatus = -1;
    InputStream hexfile = null;
    InputStream hexLengthFile = null;
    volatile long fileLength = 0L;
    volatile long line = 0L;
    public String TheradomeVersion = null;
    SerialPort finalCom = null;
    private static final byte CR = 13;
    private static final String[] WakeUp_Response = new String[]{"G6", "G7", "G8", "Are you sure", "Enter Password - ", "[ESC] to Exit", "ACK", "NAK"};
    private static final String[] FileWrite_Response = new String[]{"ACK", "NAK", "Critical Error...Try Again", "Success"};
    public SerialPort[] comPort;
    Thread myCommThread = new Thread(){

        @Override
        public void run() {
            while (true) {
                System.out.println("....myCommThreadd running...");
                try {
                    if (Communication.this.myCommThreadRun == 1) {
                        Communication.this.programStatus = Communication.this.UpdateFirmware(Communication.this.mainSerialPort);
                        System.out.println("completed write " + Integer.toString(Communication.this.programStatus));
                        Communication.this.myCommThreadRun = 0;
                    }
                    Thread.sleep(5000L);
                }
                catch (Exception exception) {
                }
            }
        }
    };

    public SerialPort GetTheradomeDevices() {
        try {
            this.DiscoverComPorts();
            if (this.comPort == null) {
                return null;
            }
        }
        catch (Exception ex) {
            return null;
        }
        try {
            ArrayList<SerialPort> devices = new ArrayList<SerialPort>();
            SerialPort[] serialPortArray = this.comPort;
            int n = this.comPort.length;
            int n2 = 0;
            while (n2 < n) {
                SerialPort port = serialPortArray[n2];
                System.out.println("COMChecking--   " + port.getSystemPortName());
                if (this.IsTheradomeDevice(port)) {
                    devices.add(port);
                    this.theradomeDevice = port;
                    System.out.println("COM  found " + this.theradomeDevice.getSystemPortName());
                    return this.theradomeDevice;
                }
                ++n2;
            }
        }
        catch (Exception ex) {
            System.out.println("COMChecking--   " + this.theradomeDevice.getSystemPortName());
        }
        System.out.println("Error: No devices  found ");
        this.theradomeDevice = null;
        return this.theradomeDevice;
    }

    public int sendESCCommands(SerialPort serialPort) {
        if (serialPort == null) {
            JOptionPane.showMessageDialog(null, "Incorrect port number ");
            return 0;
        }
        serialPort.closePort();
        try {
            Thread.sleep(2000L);
        }
        catch (Exception exception) {
            // empty catch block
        }
        serialPort.closePort();
        try {
            this.InitializePort(serialPort);
        }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Port init error ");
            serialPort.closePort();
            serialPort.closePort();
            this.InitializePort(serialPort);
        }
        if (this.OpenPort(serialPort)) {
            Boolean result = false;
            try {
                result = this.sendCRtoComeOutOfBootMode(serialPort);
            }
            catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "ESC: send failed exception  " + serialPort.getSystemPortName());
            }
            this.ClosePort(serialPort);
            if (result.booleanValue()) {
                return 1;
            }
            return 0;
        }
        JOptionPane.showMessageDialog(null, "ESC Send: Unable to open COM port : " + serialPort.getSystemPortName());
        return -1;
    }

    public int moveTheradomeToBootMode(SerialPort serialPort) {
        this.InitializePort(serialPort);
        if (this.OpenPort(serialPort)) {
            Boolean result = this.sendFlashMode(serialPort);
            this.ClosePort(serialPort);
            if (result.booleanValue()) {
                return 1;
            }
            return 0;
        }
        return -1;
    }

    public int getVersionNumberFromDevice(SerialPort serialPort) {
        this.InitializePort(serialPort);
        if (this.OpenPort(serialPort)) {
            int result = this.sendGETVERCommand(serialPort);
            this.ClosePort(serialPort);
            return result;
        }
        return -1;
    }

    public int startCommThread(SerialPort serialport, String fPath) {
        this.mainSerialPort = serialport;
        if (fPath != null) {
            try {
                this.hexfile = this.getClass().getResourceAsStream(fPath);
                this.hexLengthFile = this.getClass().getResourceAsStream(fPath);
            }
            catch (Exception ex) {
                return 0;
            }
            if (this.hexfile != null) {
                this.mainPath = fPath;
                this.myCommThreadRun = 1;
                this.programStatus = -1;
                this.pBarVal = 0;
                if (!this.myCommThread.isAlive()) {
                    this.myCommThread.start();
                }
                return 1;
            }
            return 0;
        }
        return 0;
    }

    public void showstatusPopUp(int result) {
        if (result == 3) {
            JOptionPane.showMessageDialog(null, "Firmware Updated Successfully for Theradome device...!");
        } else if (result == 2) {
            JOptionPane.showMessageDialog(null, "Firmware Updated failed for Theradome device...!/nCritical Error...Try Again");
        } else if (result == 4) {
            JOptionPane.showMessageDialog(null, "Firmware Updated failed for Theradome device...!/nTimed Out..");
        } else if (result == 5) {
            JOptionPane.showMessageDialog(null, "Firmware update completed, no success recvd\n\r " + Long.toString(this.pBarVal) + "% ");
        } else if (result == -1) {
            JOptionPane.showMessageDialog(null, "Firmware Updated failed for Theradome device...!/nPort unavailable");
        }
    }

    public int UpdateFirmware(SerialPort serialport) {
        int wrstatus = 0;
        this.finalCom = serialport;
        System.out.println("Firmware update port " + this.finalCom.getSystemPortName());
        if (this.finalCom != null) {
            try {
                this.ClosePort(this.finalCom);
                this.InitializePort115k(this.finalCom);
                if (!this.OpenPort(this.finalCom)) {
                    return 0;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                this.ClosePort(this.finalCom);
                return 0;
            }
            try {
                wrstatus = this.SendHexFileLine(this.finalCom);
            }
            catch (Exception ex) {
                this.ClosePort(this.finalCom);
            }
            this.ClosePort(this.finalCom);
            if (this.finalCom.isOpen()) {
                this.finalCom.closePort();
            }
            return wrstatus;
        }
        return 0;
    }

    private void DiscoverComPorts() {
        this.comPort = SerialPort.getCommPorts();
    }

    public boolean IsTheradomeDevice(SerialPort port) {
        try {
            port.closePort();
        }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Initialize COM port failed : " + port.getSystemPortName());
        }
        if (this.OpenPort(port)) {
            this.InitializePort(port);
            Boolean result = this.SendWakeUpCommand(port);
            port.removeDataListener();
            this.ClosePort(port);
            return result;
        }
        return false;
    }

    /*
     * Unable to fully structure code
     */
    private Boolean sendCRtoComeOutOfBootMode(SerialPort port) {
        data = new byte[]{13};
        response_str = "";
        response = new byte[800];
        i = 0;
        bytesToRead = 0;
        if (port == null) {
            System.out.println("wakeup no port found");
            return false;
        }
        try {
            port.writeBytes(data, 1L);
            System.out.println("bytes writtenn");
            if (true) ** GOTO lbl36
        }
        catch (Exception ex) {
            System.out.println("write failed");
            return false;
        }
        do {
            try {
                Thread.sleep(200L);
                bytesToRead = port.bytesAvailable();
                System.out.println("\n\rBytes available" + Integer.toString(bytesToRead));
                try {
                    if (bytesToRead > 0) {
                        port.readBytes(response, bytesToRead);
                        str = new String(response);
                        response_str = String.valueOf(response_str) + str;
                        System.out.println("\n\rResponse" + response_str);
                    }
                }
                catch (Exception str) {
                    // empty catch block
                }
                ++i;
            }
            catch (Exception e) {
                System.out.println("waiting for response");
            }
lbl36:
            // 3 sources

        } while (i < 10);
        if (response_str.length() > 0) {
            System.out.println("\n\r-----checking response ----------" + response_str);
            temperatureStr = "Enter Temperature";
            temperatureStr2 = "Temperature";
            escToExitStr = "[ESC] to Exit";
            if (response_str.contains(temperatureStr)) {
                port.writeBytes(data, 1L);
                System.out.println("got response with Enter temperature string");
                return true;
            }
            if (response_str.contains(temperatureStr2)) {
                System.out.println("got response with temperature string");
                return true;
            }
            if (response_str.contains(escToExitStr)) {
                return this.sendESCcmd(port);
            }
            return false;
        }
        System.out.println("Err: No response");
        return false;
    }

    private boolean sendESCcmd(SerialPort port) {
        block13: {
            String Tempe2;
            byte[] dataCR;
            String response_str;
            block14: {
                response_str = "";
                byte[] response = new byte[3000];
                int i = 0;
                int bytesToRead = 0;
                byte[] data = new byte[]{36};
                dataCR = new byte[]{13};
                if (port == null) {
                    System.out.println("send esc no port found");
                    return false;
                }
                try {
                    port.writeBytes(data, 1L);
                    System.out.println("bytes writtenn");
                }
                catch (Exception ex) {
                    System.out.println("write failed");
                    return false;
                }
                System.out.println("send esc writtenn");
                while (i < 10) {
                    try {
                        Thread.sleep(200L);
                        bytesToRead = port.bytesAvailable();
                        System.out.println("\n\rBytes available" + Integer.toString(bytesToRead));
                        try {
                            if (bytesToRead > 0) {
                                port.readBytes(response, bytesToRead);
                                String str = new String(response);
                                response_str = String.valueOf(response_str) + str;
                                System.out.println("\n\rResponse" + response_str);
                            }
                        }
                        catch (Exception str) {
                            // empty catch block
                        }
                        ++i;
                    }
                    catch (Exception e) {
                        System.out.println("waiting for response");
                    }
                }
                if (response_str.length() <= 0) break block13;
                System.out.println("\n\r-----checking response ----------" + response_str);
                String temperatureStr = "Enter Temperature";
                Tempe2 = "Temperature";
                if (!response_str.contains(temperatureStr)) break block14;
                port.writeBytes(dataCR, 1L);
                return true;
            }
            if (!response_str.contains(Tempe2)) break block13;
            port.writeBytes(dataCR, 1L);
            return true;
        }
        try {
            System.out.println("Err: No response");
        }
        catch (Exception ex) {
            System.out.println("write failed");
        }
        return false;
    }

    /*
     * Unable to fully structure code
     */
    private Boolean SendWakeUpCommand(SerialPort port) {
        data = new byte[]{13};
        response_str = "";
        response = new byte[1000];
        i = 0;
        bytesToRead = 0;
        System.out.println("Send wakup command ");
        if (port == null) {
            System.out.println("wakeup no port found");
            return false;
        }
        try {
            port.writeBytes(data, 1L);
            System.out.println("bytes writtenn");
            if (true) ** GOTO lbl37
        }
        catch (Exception ex) {
            System.out.println("write failed");
            return false;
        }
        do {
            try {
                Thread.sleep(200L);
                bytesToRead = port.bytesAvailable();
                System.out.println("\n\rBytes available" + Integer.toString(bytesToRead));
                try {
                    if (bytesToRead > 0) {
                        port.readBytes(response, bytesToRead);
                        str = new String(response);
                        response_str = String.valueOf(response_str) + str;
                        System.out.println("\n\rResponse" + response_str);
                    }
                }
                catch (Exception str) {
                    // empty catch block
                }
                ++i;
            }
            catch (Exception e) {
                System.out.println("waiting for response");
            }
lbl37:
            // 3 sources

        } while (i < 5);
        if (response_str.length() > 0) {
            System.out.println("\n\r-----checking response ----------" + response_str);
            enterPass = "Enter Password - ";
            boardTest = "[ESC] to Exit";
            var12_12 = Communication.WakeUp_Response;
            var11_13 = Communication.WakeUp_Response.length;
            var10_14 = 0;
            while (var10_14 < var11_13) {
                val = var12_12[var10_14];
                System.out.println("checking str " + val + response);
                if (response_str.contains(val)) {
                    if (response_str.contains(enterPass)) {
                        System.out.println("passwordr req came");
                        return this.sendPassword(port);
                    }
                    if (response_str.contains(boardTest)) {
                        this.sendESCcmd(port);
                        return true;
                    }
                    return true;
                }
                if (response_str.contains(boardTest)) {
                    this.sendESCcmd(port);
                    return true;
                }
                ++var10_14;
            }
            System.out.println("Parsing error");
        }
        System.out.println("Err: No response");
        return false;
    }

    private boolean sendPassword(SerialPort port) {
        block8: {
            String response_str = "";
            byte[] response = new byte[500];
            int i = 0;
            int bytesToRead = 0;
            byte[] data = /* [EDITORIAL REDACTION] device login credential */;
            if (port == null) {
                System.out.println("wakeup no port found");
            }
            port.writeBytes(data, 5L);
            i = 0;
            System.out.println("password writtenn");
            while (i < 5) {
                try {
                    Thread.sleep(200L);
                    bytesToRead = port.bytesAvailable();
                    System.out.println("\n\rBytes available" + Integer.toString(bytesToRead));
                    if (bytesToRead > 0) {
                        port.readBytes(response, bytesToRead);
                        String str = new String(response);
                        response_str = String.valueOf(response_str) + str;
                        System.out.println("\n\rResponse" + response_str);
                    }
                    ++i;
                }
                catch (Exception e) {
                    System.out.println("waiting for response");
                }
            }
            if (response_str.length() <= 0) break block8;
            System.out.println("\n\r-----checking response ----------" + response_str);
            String boardTest = "BOARD TEST";
            if (!response_str.contains(boardTest)) break block8;
            return true;
        }
        try {
            System.out.println("Err: No response");
        }
        catch (Exception ex) {
            System.out.println("write failed");
        }
        return false;
    }

    private int sendGETVERCommand(SerialPort port) {
        String response_str = "";
        byte[] response = new byte[3000];
        int i = 0;
        int bytesToRead = 0;
        byte[] data = new byte[]{68};
        if (port == null) {
            System.out.println("wakeup no port found");
            return 0;
        }
        try {
            port.writeBytes(data, 1L);
            i = 0;
            System.out.println("D comamnd written");
            while (i < 15) {
                try {
                    Thread.sleep(200L);
                    bytesToRead = port.bytesAvailable();
                    try {
                        System.out.println("\n\rBytes available" + Integer.toString(bytesToRead));
                        if (bytesToRead > 0) {
                            port.readBytes(response, bytesToRead);
                            String str = new String(response);
                            response_str = String.valueOf(response_str) + str;
                            System.out.println("\n\rResponse" + response_str);
                        }
                    }
                    catch (Exception str) {
                        // empty catch block
                    }
                    ++i;
                }
                catch (Exception e) {
                    System.out.println("waiting for response");
                }
            }
            if (response_str.length() > 10) {
                System.out.println("\n\r-----checking response ----------" + response_str);
                return this.ParseVersion(response_str);
            }
            System.out.println("Err: No response");
        }
        catch (Exception ex) {
            System.out.println("write failed");
        }
        return 0;
    }

    int ParseVersion(String resp) {
        String substr = "PRODUCT CODE REVISION - ";
        int strFoundAt = resp.indexOf(substr);
        if (strFoundAt > 1) {
            try {
                String version = null;
                char V1stDigit = resp.charAt(strFoundAt + 24);
                char V2ndDigit = resp.charAt(strFoundAt + 25);
                char V3rdDigit = resp.charAt(strFoundAt + 26);
                System.out.println(V1stDigit);
                System.out.println(V2ndDigit);
                System.out.println(V3rdDigit);
                this.TheradomeVersion = version = String.valueOf(String.valueOf(V1stDigit)) + String.valueOf(V2ndDigit) + String.valueOf(V3rdDigit);
                return 1;
            }
            catch (Exception ex) {
                return 0;
            }
        }
        return 0;
    }

    private boolean sendFlashMode(SerialPort port) {
        String response_str = "";
        byte[] response = new byte[800];
        int i = 0;
        int bytesToRead = 0;
        byte[] data = new byte[]{42};
        if (port == null) {
            System.out.println("wakeup no port found");
        }
        try {
            port.writeBytes(data, 1L);
            i = 0;
            System.out.println("flash mode writtenn");
            while (i < 5) {
                try {
                    Thread.sleep(200L);
                    bytesToRead = port.bytesAvailable();
                    System.out.println("\n\rBytes available" + Integer.toString(bytesToRead));
                    if (bytesToRead > 0) {
                        port.readBytes(response, bytesToRead);
                        String str = new String(response);
                        response_str = String.valueOf(response_str) + str;
                        System.out.println("\n\rResponse" + response_str);
                    }
                    ++i;
                }
                catch (InterruptedException e) {
                    System.out.println("waiting for response");
                }
            }
            if (response_str.length() > 0) {
                System.out.println("\n\r-----checking response ----------" + response_str);
                String strackc = "ACK";
                if (response_str.contains(strackc)) {
                    return this.sendFlashWakeUp(port);
                }
            }
            System.out.println("Err: No response");
        }
        catch (Exception ex) {
            System.out.println("write failed");
        }
        return false;
    }

    private boolean sendFlashWakeUp(SerialPort port) {
        String response_str = "";
        byte[] response = new byte[800];
        int i = 0;
        int bytesToRead = 0;
        byte[] data = new byte[]{13};
        System.out.println("Changing badurate: wait for 2sec");
        try {
            Thread.sleep(2000L);
            this.ClosePort(port);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.InitializePort115k(port);
        if (!this.OpenPort(port)) {
            return false;
        }
        try {
            Thread.sleep(1000L);
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            port.writeBytes(data, 1L);
            i = 0;
            System.out.println("flash wake cr writtenn");
            while (i < 5) {
                try {
                    Thread.sleep(200L);
                    bytesToRead = port.bytesAvailable();
                    System.out.println("\n\rBytes available" + Integer.toString(bytesToRead));
                    if (bytesToRead > 0) {
                        port.readBytes(response, bytesToRead);
                        String str = new String(response);
                        response_str = String.valueOf(response_str) + str;
                        System.out.println("\n\rResponse" + response_str);
                    }
                    ++i;
                }
                catch (InterruptedException e) {
                    System.out.println("waiting for response");
                }
            }
            if (response_str.length() > 0) {
                System.out.println("\n\r-----checking response ----------" + response_str);
                String areyou = "Are you sure";
                if (response_str.contains(areyou)) {
                    return this.sendAreYouSureACK(port);
                }
            }
            System.out.println("Err: No response");
        }
        catch (Exception ex) {
            System.out.println("write failed");
        }
        return false;
    }

    private boolean sendAreYouSureACK(SerialPort port) {
        block7: {
            String response_str = "";
            byte[] response = new byte[800];
            int i = 0;
            int bytesToRead = 0;
            byte[] data = new byte[]{121};
            port.writeBytes(data, 1L);
            i = 0;
            System.out.println("flash are you sure wake writtenn");
            while (i < 5) {
                try {
                    Thread.sleep(200L);
                    bytesToRead = port.bytesAvailable();
                    System.out.println("\n\rBytes available" + Integer.toString(bytesToRead));
                    if (bytesToRead > 0) {
                        port.readBytes(response, bytesToRead);
                        String str = new String(response);
                        response_str = String.valueOf(response_str) + str;
                        System.out.println("\n\rResponse" + response_str);
                    }
                    ++i;
                }
                catch (InterruptedException e) {
                    System.out.println("waiting for response");
                }
            }
            if (response_str.length() <= 0) break block7;
            System.out.println("\n\r-----checking response ----------" + response_str);
            String areyousureACK = "ACK";
            if (!response_str.contains(areyousureACK)) break block7;
            System.out.println("\n\r send hex ack received " + response_str);
            return true;
        }
        try {
            System.out.println("Err: No response");
        }
        catch (Exception ex) {
            System.out.println("write failed");
        }
        return false;
    }

    private boolean OpenPort(SerialPort port) {
        if (!port.isOpen()) {
            block5: {
                if (!port.openPort()) break block5;
                System.out.println("Open: Port is opened");
                return true;
            }
            try {
                System.out.println("Open: Failed to open port ");
                return false;
            }
            catch (Exception ex) {
                System.out.println("Open: Port open exception)");
            }
        } else {
            return true;
        }
        return false;
    }

    private void ClosePort(SerialPort port) {
        try {
            if (!port.closePort()) {
                System.out.println("Deinit: Failed to close port :(");
                return;
            }
            System.out.println("Deinit: Port is closed :)");
        }
        catch (Exception ex) {
            System.out.println("Deinit: port close exception)");
        }
    }

    private void InitializePort(SerialPort port) {
        try {
            port.setComPortParameters(57600, 8, 1, 0);
            port.setComPortTimeouts(256, 0, 0);
            System.out.println("Init: Baudrate changed to 57600 -" + port.getSystemPortName());
        }
        catch (Exception e) {
            System.out.println("Init: Failed to init " + port.getSystemPortName());
        }
    }

    private void InitializePort115k(SerialPort port) {
        port.setComPortParameters(115200, 8, 1, 0);
        port.setComPortTimeouts(256, 0, 0);
        System.out.println("baudrate changed to 115200");
    }

    long getFileLength(InputStream inputfile) {
        BufferedReader filebuffer = null;
        this.line = 0L;
        this.fileLength = 0L;
        try {
            String lineread;
            filebuffer = new BufferedReader(new InputStreamReader(inputfile));
            while ((lineread = filebuffer.readLine()) != null) {
                byte[] tempbuffer = lineread.getBytes();
                this.fileLength += (long)tempbuffer.length;
                ++this.line;
            }
            filebuffer.close();
        }
        catch (Exception ex) {
            return 0L;
        }
        return 1L;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private int SendHexFileLine(SerialPort portx) throws FileNotFoundException, IOException {
        String str = new String();
        int status = 0;
        int linecnt = 0;
        long transferredLength = 0L;
        this.getFileLength(this.hexLengthFile);
        if (this.fileLength < 1000L) {
            JOptionPane.showMessageDialog(null, "Error File Length is low" + Long.toString(this.fileLength));
            return 0;
        }
        this.fileLength += this.line;
        BufferedReader filebuffer = new BufferedReader(new InputStreamReader(this.hexfile));
        try {
            while (true) {
                byte[] b;
                if (status == 0) {
                    try {
                        str = filebuffer.readLine();
                        this.pBarVal = (int)(transferredLength * 100L / this.fileLength);
                        if (str != null) {
                            str = String.valueOf(str) + "\r";
                            b = str.getBytes();
                            this.finalCom.writeBytes(b, b.length);
                            transferredLength += (long)b.length;
                            ++linecnt;
                        }
                    }
                    catch (Exception exception) {}
                } else {
                    if (status != true) break;
                    if (str != null) {
                        b = str.getBytes();
                        this.finalCom.writeBytes(b, b.length);
                    }
                }
                String response_str = "";
                int i = 0;
                while (i < 20) {
                    int bytesToRead = this.finalCom.bytesAvailable();
                    if (bytesToRead > 1) {
                        byte[] response = new byte[100];
                        this.finalCom.readBytes(response, bytesToRead);
                        String mystr = new String(response);
                        response_str = String.valueOf(response_str) + mystr;
                        break;
                    }
                    try {
                        Thread.sleep(100L);
                    }
                    catch (InterruptedException interruptedException) {
                        // empty catch block
                    }
                    ++i;
                }
                if (response_str.length() > 1) {
                    status = this.ParseResponse(response_str);
                    response_str = "";
                } else {
                    status = 4;
                }
                try {
                    Thread.sleep(100L);
                }
                catch (Exception exception) {}
            }
        }
        catch (Exception ex) {
            this.finalCom.closePort();
            this.finalCom.closePort();
        }
        filebuffer.close();
        if (this.pBarVal > 99) {
            status = 5;
        }
        if (!this.finalCom.isOpen()) {
            this.finalCom.closePort();
            this.finalCom.closePort();
        }
        return status;
    }

    private int ParseResponse(String response) {
        int iReturn = 0;
        String[] stringArray = FileWrite_Response;
        int n = FileWrite_Response.length;
        int n2 = 0;
        while (n2 < n) {
            String val = stringArray[n2];
            if (response.contains(val)) {
                System.out.println("resp rcvd" + response);
                return iReturn;
            }
            ++iReturn;
            ++n2;
        }
        return iReturn;
    }
}

