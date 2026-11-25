/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import SerialCommunication.Communication;
import com.fazecast.jSerialComm.SerialPort;
import com.screens.CreateFrame;
import com.screens.FirmwareUpdateMenu;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class FirmwareUpdateCompleted {
    JFrame frame;
    Communication comm;
    SerialPort theraFoundSerialPort;
    String deviceVersion = null;
    private JButton btnNext;
    private JButton btnBack;
    private JTextField textVersion;
    private JTextField textConnectStatus;
    private JLabel lblNewLabel;
    private JLabel logolabel;
    private JLabel lblfmenu;
    private JLabel lblNewLabe3;
    private JLabel lblNewLabe4;
    private JLabel lblHeading;
    private JLabel lblfmenuHelmetdl;
    private JLabel lblCompleted;
    private JLabel lblfmenuContact;
    private JLabel lblfmenuTrouble;
    private JLabel lblConnectedMark;
    private int firstLabelYpos = 20;
    private int selectedModelbyUser = 0;
    String firmwareVersion;
    Image helmetdlimage;
    Image unHelmetdlimage;
    Image contactimage;
    Image unContactimage;
    Image troubleimage;
    Image unTroubleimage;
    private JButton btnHelmetUpgrade;
    private JButton btnContact;
    private JButton btnTroubleshoot;
    Timer fwupdateCompletedTimer;
    int commTimeout = 20;
    int myCompletedThreadRun = 0;
    int waitTime = 10;
    Thread myCompletedThread = new Thread(){

        @Override
        public void run() {
            while (true) {
                System.out.println("....myFetchThread running...");
                try {
                    if (FirmwareUpdateCompleted.this.myCompletedThreadRun == 1) {
                        FirmwareUpdateCompleted.this.disconnectTheradome();
                        FirmwareUpdateCompleted.this.myCompletedThreadRun = 0;
                        FirmwareUpdateCompleted.this.btnNext.setEnabled(true);
                        FirmwareUpdateCompleted.this.stopThread();
                        FirmwareUpdateCompleted.this.frame.dispose();
                        new FirmwareUpdateMenu((int)((FirmwareUpdateCompleted)FirmwareUpdateCompleted.this).selectedModelbyUser, (Communication)FirmwareUpdateCompleted.this.comm, (SerialPort)FirmwareUpdateCompleted.this.theraFoundSerialPort, (String)FirmwareUpdateCompleted.this.deviceVersion).frame.setVisible(true);
                    }
                    Thread.sleep(2000L);
                }
                catch (Exception exception) {
                }
            }
        }
    };

    void disconnectTheradome() {
        System.out.println("....myCheckConnected running...");
        this.lblNewLabe3.setVisible(true);
        try {
            this.btnNext.setEnabled(false);
            try {
                String printStatus;
                while (--this.waitTime > 0) {
                    this.lblNewLabe3.setVisible(true);
                    printStatus = "Processing... " + Integer.toString(this.waitTime);
                    this.lblNewLabe3.setText(printStatus);
                    Thread.sleep(1000L);
                }
                this.lblNewLabe3.setVisible(true);
                this.lblNewLabe3.setText("Please wait for few seconds...");
                while (--this.commTimeout > 0) {
                    if (this.theraFoundSerialPort == null) break;
                    try {
                        if (this.comm.sendESCCommands(this.theraFoundSerialPort) <= 0) {
                            this.lblNewLabe3.setVisible(true);
                            printStatus = "Please wait for few seconds...";
                            this.lblNewLabe3.setText(printStatus);
                            Thread.sleep(1000L);
                            continue;
                        }
                        this.commTimeout = 1;
                        this.lblNewLabe3.setText("<html>Helmet upgrade completed.<br>");
                        break;
                    }
                    catch (Exception printStatus2) {
                        // empty catch block
                    }
                }
            }
            catch (Exception ex) {
                Thread.sleep(1000L);
                this.stopThread();
            }
            Thread.sleep(2000L);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public void startThread() {
        try {
            this.myCompletedThreadRun = 1;
            this.waitTime = 5;
            this.commTimeout = 5;
            if (!this.myCompletedThread.isAlive()) {
                this.myCompletedThread.start();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    void stopThread() {
        this.myCompletedThreadRun = 0;
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

    public FirmwareUpdateCompleted(int selectedModel, Communication commobj, SerialPort deviceSerialport, String Dver) {
        this.selectedModelbyUser = selectedModel;
        this.comm = commobj;
        this.theraFoundSerialPort = deviceSerialport;
        this.deviceVersion = Dver;
        this.initialize();
    }

    private void initialize() {
        this.frame = new JFrame();
        CreateFrame cframe = new CreateFrame(this.frame, 1);
        this.logolabel = new JLabel("");
        Image img = new ImageIcon(this.getClass().getResource("/logo_140x49.jpg")).getImage();
        this.logolabel.setIcon(new ImageIcon(img));
        this.logolabel.setBounds(370, 8, 140, 49);
        this.frame.getContentPane().add(this.logolabel);
        this.createMenu(this.frame);
        this.lblCompleted = new JLabel("");
        Image lblCompletedimage = new ImageIcon(this.getClass().getResource("/programCompleted_80x76.jpg")).getImage();
        this.lblCompleted.setIcon(new ImageIcon(lblCompletedimage));
        this.lblCompleted.setBounds(380, this.firstLabelYpos + 250, 80, 76);
        this.frame.getContentPane().add(this.lblCompleted);
        this.lblNewLabe3 = new JLabel("<html>Your Theradome has been successfully updated to latest  version");
        this.lblNewLabe3.setFont(new Font("Arial", 0, 22));
        this.lblNewLabe3.setHorizontalAlignment(0);
        this.lblNewLabe3.setForeground(SystemColor.controlDkShadow);
        this.lblNewLabe3.setBounds(100, this.firstLabelYpos + 380, 700, 68);
        this.frame.getContentPane().add(this.lblNewLabe3);
        this.lblNewLabe4 = new JLabel("<html>Closing the device communication...<br>");
        this.lblNewLabe4.setFont(new Font("Arial", 0, 22));
        this.lblNewLabe4.setHorizontalAlignment(0);
        this.lblNewLabe4.setForeground(SystemColor.controlDkShadow);
        this.lblNewLabe4.setBounds(100, this.firstLabelYpos + 450, 600, 40);
        this.frame.getContentPane().add(this.lblNewLabe4);
        this.lblNewLabe4.setVisible(false);
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
        this.textConnectStatus.setBounds(580, this.firstLabelYpos + 580, 150, 20);
        this.textConnectStatus.setEditable(false);
        this.textConnectStatus.setOpaque(false);
        this.frame.getContentPane().add(this.textConnectStatus);
        Image buttonimage = new ImageIcon(this.getClass().getResource("/buttonclose.jpg")).getImage();
        this.btnNext = new JButton();
        this.btnNext.setBorderPainted(false);
        this.btnNext.setIcon(new ImageIcon(buttonimage));
        this.btnNext.setBounds(360, this.firstLabelYpos + 520, 180, 40);
        this.frame.getContentPane().add(this.btnNext);
        this.btnNext.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                FirmwareUpdateCompleted.this.startThread();
            }
        });
    }
}

