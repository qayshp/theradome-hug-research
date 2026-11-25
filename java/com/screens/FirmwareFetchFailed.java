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
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class FirmwareFetchFailed {
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

    public FirmwareFetchFailed(int selectedModel, Communication commobj, SerialPort deviceSerialport, String Dver) {
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
        Image lblCompletedimage = new ImageIcon(this.getClass().getResource("/fetchfailed_80x80.jpg")).getImage();
        this.lblCompleted.setIcon(new ImageIcon(lblCompletedimage));
        this.lblCompleted.setBounds(380, this.firstLabelYpos + 250, 80, 80);
        this.frame.getContentPane().add(this.lblCompleted);
        this.lblNewLabe3 = new JLabel("<html>There is no Internet Connection");
        this.lblNewLabe3.setFont(new Font("Arial", 0, 22));
        this.lblNewLabe3.setForeground(SystemColor.controlDkShadow);
        this.lblNewLabe3.setHorizontalAlignment(0);
        this.lblNewLabe3.setBounds(100, this.firstLabelYpos + 380, 700, 68);
        this.frame.getContentPane().add(this.lblNewLabe3);
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
        Image buttonimage = new ImageIcon(this.getClass().getResource("/buttonTryAgain.jpg")).getImage();
        this.btnNext = new JButton();
        this.btnNext.setBorderPainted(false);
        this.btnNext.setIcon(new ImageIcon(buttonimage));
        this.btnNext.setBounds(360, this.firstLabelYpos + 520, 180, 40);
        this.frame.getContentPane().add(this.btnNext);
        this.btnNext.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                FirmwareFetchFailed.this.frame.dispose();
                new FirmwareUpdateMenu((int)((FirmwareFetchFailed)FirmwareFetchFailed.this).selectedModelbyUser, (Communication)FirmwareFetchFailed.this.comm, (SerialPort)FirmwareFetchFailed.this.theraFoundSerialPort, (String)FirmwareFetchFailed.this.deviceVersion).frame.setVisible(true);
            }
        });
        Image backimage = new ImageIcon(this.getClass().getResource("/back14x14.jpg")).getImage();
        this.btnBack = new JButton();
        this.btnBack.setBorderPainted(false);
        this.btnBack.setIcon(new ImageIcon(backimage));
        this.btnBack.setBounds(20, 20, 14, 14);
        this.frame.getContentPane().add(this.btnBack);
        this.btnBack.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                FirmwareFetchFailed.this.frame.dispose();
                new FirmwareUpdateMenu((int)((FirmwareFetchFailed)FirmwareFetchFailed.this).selectedModelbyUser, (Communication)FirmwareFetchFailed.this.comm, (SerialPort)FirmwareFetchFailed.this.theraFoundSerialPort, (String)FirmwareFetchFailed.this.deviceVersion).frame.setVisible(true);
            }
        });
    }
}

