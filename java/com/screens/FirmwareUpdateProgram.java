/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import SerialCommunication.Communication;
import com.fazecast.jSerialComm.SerialPort;
import com.screens.CreateFrame;
import com.screens.FancyProgressBar;
import com.screens.FirmwareUpdateCompleted;
import com.screens.FirmwareUpdateMenu;
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

public class FirmwareUpdateProgram {
    JFrame frame;
    Communication comm;
    SerialPort theraFoundSerialPort;
    String deviceVersion = null;
    private JButton btnNext;
    private JButton btnBack;
    private JTextField textVersion;
    private JTextField textConnectStatus;
    private JLabel lblNewLabel;
    private JLabel progresslblMsg1;
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
    private JProgressBar fancyPB;
    private int firstLabelYpos = 20;
    private int selectedModelbyUser = 0;
    String firmwareVersion;
    volatile int myProgramProgressThreadRun = 0;
    Image helmetdlimage;
    Image unHelmetdlimage;
    Image contactimage;
    Image unContactimage;
    Image troubleimage;
    Image unTroubleimage;
    private JButton btnHelmetUpgrade;
    private JButton btnContact;
    private JButton btnTroubleshoot;
    Thread myProgramProgressThread = new Thread(){

        @Override
        public void run() {
            while (true) {
                if (FirmwareUpdateProgram.this.comm.myCommThreadRun == 1) {
                    try {
                        FirmwareUpdateProgram.this.fancyPB.setVisible(true);
                        FirmwareUpdateProgram.this.fancyPB.setValue(FirmwareUpdateProgram.this.comm.pBarVal);
                        FirmwareUpdateProgram.this.progresslblMsg1.setText(String.valueOf(Integer.toString(FirmwareUpdateProgram.this.comm.pBarVal)) + "%");
                        Thread.sleep(50L);
                    }
                    catch (Exception exception) {}
                    continue;
                }
                try {
                    FirmwareUpdateProgram.this.fancyPB.setValue(0);
                    FirmwareUpdateProgram.this.progresslblMsg1.setText(" ");
                    FirmwareUpdateProgram.this.fancyPB.setVisible(false);
                    if (FirmwareUpdateProgram.this.comm.programStatus != -1) {
                        if (FirmwareUpdateProgram.this.comm.programStatus == 3 || FirmwareUpdateProgram.this.comm.programStatus == 5) {
                            FirmwareUpdateProgram.this.moveToCompleteScreen();
                        } else {
                            FirmwareUpdateProgram.this.showErrorInProgramScreen("Failed to Program the device ErrP" + Integer.toString(FirmwareUpdateProgram.this.comm.programStatus));
                            System.out.println("\n\rfirmware update failed");
                        }
                        FirmwareUpdateProgram.this.comm.programStatus = -1;
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
                try {
                    Thread.sleep(1000L);
                }
                catch (Exception exception) {
                }
            }
        }
    };

    public void startThread() {
        try {
            this.myProgramProgressThreadRun = 1;
            if (!this.myProgramProgressThread.isAlive()) {
                this.myProgramProgressThread.start();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public FirmwareUpdateProgram(int selectedModel, Communication commobj, SerialPort deviceSerialport, String Dver) {
        this.selectedModelbyUser = selectedModel;
        this.comm = commobj;
        this.theraFoundSerialPort = deviceSerialport;
        this.deviceVersion = Dver;
        this.initialize();
        this.startThread();
        try {
            if (this.comm.startCommThread(this.theraFoundSerialPort, "/myTheradome.hex") == 0) {
                System.out.println("startCommThread: failed");
                this.showErrorInProgramScreen("Failed to Program the device ErrP0");
            }
        }
        catch (Exception ex) {
            this.showErrorInProgramScreen("Failed to Program the device ErrF0");
            System.out.println("startCommThread: exception");
        }
    }

    void moveToCompleteScreen() {
        try {
            this.frame.dispose();
            FirmwareUpdateCompleted FirmwareUpdateCompletedscree = new FirmwareUpdateCompleted(this.selectedModelbyUser, this.comm, this.theraFoundSerialPort, this.deviceVersion);
            FirmwareUpdateCompletedscree.frame.setVisible(true);
            System.out.println("\n\rfirmware update completed");
        }
        catch (Exception ex) {
            System.out.println("move to completed failed");
        }
    }

    void showErrorInProgramScreen(String msg) {
        int i = 0;
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
        catch (Exception exception) {
            // empty catch block
        }
    }

    void showStatusInProgramScreen(String msg) {
        this.lblNewLabe3.setText(msg);
        this.lblNewLabe3.setForeground(SystemColor.controlDkShadow);
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
        catch (Exception myLightblue) {
            // empty catch block
        }
        try {
            this.lblNewLabe3 = new JLabel("Updating your Theradome...");
            this.lblNewLabe3.setHorizontalAlignment(0);
            this.lblNewLabe3.setFont(new Font("Arial", 0, 22));
            this.lblNewLabe3.setForeground(SystemColor.controlDkShadow);
            this.lblNewLabe3.setBounds(100, this.firstLabelYpos + 320, 600, 60);
            this.frame.getContentPane().add(this.lblNewLabe3);
        }
        catch (Exception myLightblue) {
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
            this.lblConnectedMark = new JLabel("");
            Image lblconnectedmarkimage = new ImageIcon(this.getClass().getResource("/fmenu_markconnected_25x17.jpg")).getImage();
            this.lblConnectedMark.setIcon(new ImageIcon(lblconnectedmarkimage));
            this.lblConnectedMark.setBounds(550, this.firstLabelYpos + 580, 25, 17);
            this.frame.getContentPane().add(this.lblConnectedMark);
            this.textConnectStatus = new JTextField();
            this.textConnectStatus.setHorizontalAlignment(0);
            this.textConnectStatus.setBorder(null);
            this.textConnectStatus.setForeground(Color.GRAY);
            this.textConnectStatus.setFont(new Font("Arial", 0, 14));
            this.textConnectStatus.setText("Theradome Connected");
            this.textConnectStatus.setEditable(false);
            this.textConnectStatus.setOpaque(false);
            this.textConnectStatus.setBounds(580, this.firstLabelYpos + 580, 150, 20);
            this.frame.getContentPane().add(this.textConnectStatus);
        }
        catch (Exception lblconnectedmarkimage) {
            // empty catch block
        }
        try {
            Image buttonimage = new ImageIcon(this.getClass().getResource("/buttonClose.jpg")).getImage();
            this.btnNext = new JButton();
            this.btnNext.setBorderPainted(false);
            this.btnNext.setIcon(new ImageIcon(buttonimage));
            this.btnNext.setBounds(360, this.firstLabelYpos + 520, 180, 40);
            this.btnNext.setVisible(false);
            this.frame.getContentPane().add(this.btnNext);
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            this.btnNext.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e) {
                    FirmwareUpdateProgram.this.frame.dispose();
                    FirmwareUpdateMenu firmwareupdatescreen = new FirmwareUpdateMenu(FirmwareUpdateProgram.this.selectedModelbyUser, FirmwareUpdateProgram.this.comm, FirmwareUpdateProgram.this.theraFoundSerialPort, FirmwareUpdateProgram.this.deviceVersion);
                    firmwareupdatescreen.frame.setVisible(true);
                }
            });
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

