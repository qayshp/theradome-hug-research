/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import SerialCommunication.Communication;
import com.fazecast.jSerialComm.SerialPort;
import com.screens.CreateFrame;
import com.screens.DownloadUtil;
import com.screens.FancyProgressBar;
import com.screens.FirmwareFetchFailed;
import com.screens.FirmwareUpdateMenu;
import com.screens.FirmwareUpdateProgram;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.UIManager;

public class FirmwareUpdateFetch {
    JFrame frame;
    Communication comm;
    SerialPort theraFoundSerialPort;
    String deviceVersion = "";
    private JButton btnNext;
    private JButton btnBack;
    private JTextField textVersion;
    private JTextField textConnectStatus;
    private JLabel lblNewLabel;
    private JLabel logolabel;
    private JLabel lblfmenu;
    private JLabel lblNewLabe2;
    private JLabel lblNewLabe3;
    private JLabel lblprogress;
    private JLabel lblHeading;
    private JLabel lblfmenuHelmetdl;
    private JLabel lblfmenuContact;
    private JLabel lblfmenuTrouble;
    private JLabel lblConnectedMark;
    private int firstLabelYpos = 20;
    private int selectedModelbyUser = 0;
    String firmwareVersion;
    int myFetchThreadRun;
    private JProgressBar fancyPB;
    private JLabel progresslblMsg1;
    Image helmetdlimage;
    Image unHelmetdlimage;
    Image contactimage;
    Image unContactimage;
    Image troubleimage;
    Image unTroubleimage;
    private JButton btnHelmetUpgrade;
    private JButton btnContact;
    private JButton btnTroubleshoot;
    Thread myFetchThread = new Thread(){

        @Override
        public void run() {
            while (true) {
                System.out.println("....myFetchThread running...");
                try {
                    if (FirmwareUpdateFetch.this.myFetchThreadRun == 1) {
                        FirmwareUpdateFetch.this.fetchHexFile();
                        FirmwareUpdateFetch.this.myFetchThreadRun = 0;
                    }
                    Thread.sleep(10000L);
                }
                catch (Exception exception) {
                }
            }
        }
    };

    public void startThread() {
        try {
            this.myFetchThreadRun = 1;
            if (!this.myFetchThread.isAlive()) {
                this.myFetchThread.start();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public FirmwareUpdateFetch(int selectedModel, Communication commobj, SerialPort deviceSerialport, String Dversion) {
        this.selectedModelbyUser = selectedModel;
        this.comm = commobj;
        this.theraFoundSerialPort = deviceSerialport;
        this.deviceVersion = Dversion;
        this.initialize();
        this.startThread();
    }

    public int fetchHexFile() {
        try {
            if (this.getHexFile() == 1) {
                this.moveToProgramMode();
            } else {
                this.showErrorMsginFetchScreen("Fetching Err1");
            }
        }
        catch (Exception ex) {
            this.showErrorMsginFetchScreen("Fetching Err0");
            return 0;
        }
        return 1;
    }

    void setProgressbar() {
        try {
            this.fancyPB = new JProgressBar();
            UIManager.put("ProgressBar.selectionBackground", Color.white);
            UIManager.put("ProgressBar.selectionForeground", Color.white);
            this.fancyPB.setUI(new FancyProgressBar());
            this.fancyPB.setMinimum(0);
            this.fancyPB.setStringPainted(true);
            this.fancyPB.setMaximum(100);
            this.fancyPB.setPreferredSize(new Dimension(500, 30));
            this.fancyPB.setBorderPainted(false);
            this.fancyPB.setBounds(230, this.firstLabelYpos + 280, 370, 40);
            this.fancyPB.setVisible(true);
            this.frame.getContentPane().add(this.fancyPB);
            Color myLightblue = new Color(25, 164, 219);
            this.progresslblMsg1 = new JLabel("0%");
            this.progresslblMsg1.setFont(new Font("Arial", 0, 20));
            this.progresslblMsg1.setForeground(SystemColor.controlDkShadow);
            this.progresslblMsg1.setBounds(150, this.firstLabelYpos + 285, 80, 25);
            this.progresslblMsg1.setForeground(myLightblue);
            this.frame.getContentPane().add(this.progresslblMsg1);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public int getHexFile() {
        DownloadUtil dwnldFile = new DownloadUtil();
        String fileURL = "https://raw.githubusercontent.com/renuraja03/fwupdate/master/my.hex";
        String dirPath = dwnldFile.getHomePath();
        long error = dwnldFile.downLoadProcess(fileURL, dirPath, this.fancyPB, this.progresslblMsg1);
        if (error == 1L) {
            this.lblNewLabe3.setText("Fetching binary is done, Please wait...");
            System.out.println("Get hex success");
            return 1;
        }
        System.out.println("Http file error:" + Long.toString(error));
        this.frame.dispose();
        new FirmwareFetchFailed((int)this.selectedModelbyUser, (Communication)this.comm, (SerialPort)this.theraFoundSerialPort, (String)this.deviceVersion).frame.setVisible(true);
        return 1;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public int moveToProgramMode() {
        try {
            int status = -1;
            status = this.comm.moveTheradomeToBootMode(this.theraFoundSerialPort);
            if (status != 1) {
                if (status != 0) return 1;
                this.showErrorMsginFetchScreen("Fetch: Device not responding");
                return 0;
            }
            FirmwareUpdateProgram FirmwareUpdateProgramscreen = null;
            try {
                FirmwareUpdateProgramscreen = new FirmwareUpdateProgram(this.selectedModelbyUser, this.comm, this.theraFoundSerialPort, this.deviceVersion);
            }
            catch (Exception ex) {
                this.showErrorMsginFetchScreen("Fetch: Err1");
                return 0;
            }
            if (FirmwareUpdateProgramscreen == null) return 1;
            FirmwareUpdateProgramscreen.frame.setVisible(true);
            this.frame.dispose();
            return 1;
        }
        catch (Exception ex) {
            this.showErrorMsginFetchScreen("Fetch: Check not available");
            return 0;
        }
    }

    public void showErrorMsginFetchScreen(String msg) {
        int i = 0;
        try {
            this.lblNewLabe3.setText(msg);
            this.lblNewLabe3.setForeground(SystemColor.RED);
            try {
                i = 5;
                while (i > 0) {
                    TimeUnit.SECONDS.sleep(1L);
                    this.lblNewLabe3.setText(String.valueOf(msg) + ", Timeout: " + Integer.toString(i) + " sec");
                    --i;
                }
                this.frame.dispose();
                FirmwareUpdateMenu firmwareupdatescreen = new FirmwareUpdateMenu(this.selectedModelbyUser, this.comm, this.theraFoundSerialPort, this.deviceVersion);
                firmwareupdatescreen.frame.setVisible(true);
            }
            catch (Exception exception) {}
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    void createMenu(JFrame frm) {
        this.helmetdlimage = new ImageIcon(this.getClass().getResource("/select_fmenuHelmetupdate_220x47.jpg")).getImage();
        this.btnHelmetUpgrade = new JButton();
        this.btnHelmetUpgrade.setBorderPainted(false);
        this.btnHelmetUpgrade.setIcon(new ImageIcon(this.helmetdlimage));
        this.btnHelmetUpgrade.setBounds(70, this.firstLabelYpos + 100, 220, 47);
        this.frame.getContentPane().add(this.btnHelmetUpgrade);
        this.unContactimage = new ImageIcon(this.getClass().getResource("/unselect_fmenuContact_220x47.jpg")).getImage();
        this.btnContact = new JButton();
        this.btnContact.setBorderPainted(false);
        this.btnContact.setIcon(new ImageIcon(this.unContactimage));
        this.btnContact.setBounds(310, this.firstLabelYpos + 100, 220, 47);
        this.frame.getContentPane().add(this.btnContact);
        this.unTroubleimage = new ImageIcon(this.getClass().getResource("/unselect_fmenuTroubleshoot_220x47.jpg")).getImage();
        this.btnTroubleshoot = new JButton();
        this.btnTroubleshoot.setBorderPainted(false);
        this.btnTroubleshoot.setIcon(new ImageIcon(this.unTroubleimage));
        this.btnTroubleshoot.setBounds(550, this.firstLabelYpos + 100, 220, 47);
        this.frame.getContentPane().add(this.btnTroubleshoot);
        this.btnHelmetUpgrade.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        this.btnContact.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        this.btnTroubleshoot.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
    }

    private void initialize() {
        this.frame = new JFrame();
        CreateFrame cframe = new CreateFrame(this.frame, 0);
        this.logolabel = new JLabel("");
        Image img = new ImageIcon(this.getClass().getResource("/logo_140x49.jpg")).getImage();
        this.logolabel.setIcon(new ImageIcon(img));
        this.logolabel.setBounds(370, 8, 140, 49);
        this.frame.getContentPane().add(this.logolabel);
        this.createMenu(this.frame);
        try {
            this.lblNewLabe2 = new JLabel("Do not  disconnect your Theradome");
            this.lblNewLabe2.setFont(new Font("Arial", 0, 35));
            this.lblNewLabe2.setForeground(SystemColor.controlDkShadow);
            this.lblNewLabe2.setBounds(150, this.firstLabelYpos + 180, 600, 60);
            this.frame.getContentPane().add(this.lblNewLabe2);
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            this.setProgressbar();
            this.lblNewLabe3 = new JLabel("Fetching binary...");
            this.lblNewLabe3.setHorizontalAlignment(0);
            this.lblNewLabe3.setFont(new Font("Arial", 0, 22));
            this.lblNewLabe3.setForeground(SystemColor.controlDkShadow);
            this.lblNewLabe3.setBounds(100, this.firstLabelYpos + 320, 600, 60);
            this.frame.getContentPane().add(this.lblNewLabe3);
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            this.textVersion = new JTextField();
            this.firmwareVersion = "Current Version " + this.deviceVersion;
            this.textVersion.setHorizontalAlignment(0);
            this.textVersion.setBorder(null);
            this.textVersion.setForeground(Color.GRAY);
            this.textVersion.setFont(new Font("Arial", 0, 14));
            this.textVersion.setText(this.firmwareVersion);
            this.textVersion.setBounds(10, this.firstLabelYpos + 580, 200, 20);
            this.textVersion.setEditable(false);
            this.textVersion.setOpaque(false);
            this.frame.getContentPane().add(this.textVersion);
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            this.lblConnectedMark = new JLabel("");
            Image lblconnectedmarkimage = new ImageIcon(this.getClass().getResource("/fmenu_markconnected_25x17.jpg")).getImage();
            this.lblConnectedMark.setIcon(new ImageIcon(lblconnectedmarkimage));
            this.lblConnectedMark.setBounds(550, this.firstLabelYpos + 580, 25, 17);
            this.frame.getContentPane().add(this.lblConnectedMark);
        }
        catch (Exception lblconnectedmarkimage) {
            // empty catch block
        }
        try {
            this.textConnectStatus = new JTextField();
            this.textConnectStatus.setHorizontalAlignment(0);
            this.textConnectStatus.setBorder(null);
            this.textConnectStatus.setForeground(Color.GRAY);
            this.textConnectStatus.setFont(new Font("Arial", 0, 14));
            this.textConnectStatus.setText("Theradome Connected");
            this.textConnectStatus.setBounds(580, this.firstLabelYpos + 580, 150, 20);
            this.textConnectStatus.setEditable(false);
            this.textConnectStatus.setOpaque(false);
            this.frame.getContentPane().add(this.textConnectStatus);
        }
        catch (Exception lblconnectedmarkimage) {
            // empty catch block
        }
        Image backimage = new ImageIcon(this.getClass().getResource("/back14x14.jpg")).getImage();
        this.btnBack = new JButton();
        this.btnBack.setBorderPainted(false);
        this.btnBack.setIcon(new ImageIcon(backimage));
        this.btnBack.setBounds(20, 20, 14, 14);
        this.btnBack.setVisible(false);
        this.frame.getContentPane().add(this.btnBack);
        this.btnBack.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                FirmwareUpdateFetch.this.frame.dispose();
                new FirmwareUpdateMenu((int)((FirmwareUpdateFetch)FirmwareUpdateFetch.this).selectedModelbyUser, (Communication)FirmwareUpdateFetch.this.comm, (SerialPort)FirmwareUpdateFetch.this.theraFoundSerialPort, (String)FirmwareUpdateFetch.this.deviceVersion).frame.setVisible(true);
            }
        });
    }
}

