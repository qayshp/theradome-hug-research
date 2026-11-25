/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import SerialCommunication.Communication;
import com.fazecast.jSerialComm.SerialPort;
import com.screens.Connect;
import com.screens.Connected;
import com.screens.CreateFrame;
import com.screens.NotFound;
import java.awt.Font;
import java.awt.Image;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class ConnectPressButton {
    JFrame frame;
    Communication comm;
    SerialPort theraFoundSerialPort;
    private JButton btnNext;
    private JButton btnBack;
    private JTextField txtMicro;
    private JLabel logolabel;
    private JLabel lblMsg2;
    private JLabel lblNewLabel;
    private JLabel lblNewLabel_1;
    private JLabel lblNewLabel_2;
    private JLabel lblNewLabel_3;
    private JLabel lblNewLabel_4;
    private JLabel lblConnecting;
    private int firstLabelYpos = 20;
    private int selectedModelbyUser = 0;
    boolean checkDeviceConnectionThread;
    boolean toggleButton = false;
    Image buttonimage;
    Image buttonConnecting;
    Timer timerConnect;

    public void startThread() {
        try {
            this.timerConnect = new Timer("Timer");
            TimerTask task = new TimerTask(){

                @Override
                public void run() {
                    System.out.println("....myCheckConnection running...");
                    try {
                        ConnectPressButton.this.btnNext.setEnabled(false);
                        int respState = ConnectPressButton.this.checkDeviceConnection();
                        ConnectPressButton.this.btnNext.setEnabled(true);
                        ConnectPressButton.this.stopThread();
                        if (respState == 1) {
                            ConnectPressButton.this.frame.dispose();
                            Connected connectedScreen = new Connected(ConnectPressButton.this.selectedModelbyUser, ConnectPressButton.this.comm, ConnectPressButton.this.theraFoundSerialPort);
                            connectedScreen.frame.setVisible(true);
                        } else {
                            ConnectPressButton.this.frame.dispose();
                            NotFound NotFoundScreen = new NotFound(ConnectPressButton.this.selectedModelbyUser, ConnectPressButton.this.comm);
                            NotFoundScreen.frame.setVisible(true);
                        }
                        Thread.sleep(500L);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
            };
            this.timerConnect.schedule(task, 1L);
        }
        catch (Exception ex) {
            this.timerConnect.cancel();
        }
    }

    public void stopThread() {
        this.timerConnect.cancel();
    }

    public ConnectPressButton(int selectedModel) {
        this.selectedModelbyUser = selectedModel;
        this.initialize();
    }

    public int checkDeviceConnection() {
        this.theraFoundSerialPort = this.comm.GetTheradomeDevices();
        if (this.theraFoundSerialPort != null) {
            return 1;
        }
        return 0;
    }

    private void initialize() {
        Image lh40_30;
        this.comm = new Communication();
        this.frame = new JFrame();
        CreateFrame cframe = new CreateFrame(this.frame, 1);
        this.logolabel = new JLabel("");
        Image img = new ImageIcon(this.getClass().getResource("/logo_140x49.jpg")).getImage();
        this.logolabel.setIcon(new ImageIcon(img));
        this.logolabel.setBounds(370, 8, 140, 49);
        this.frame.getContentPane().add(this.logolabel);
        this.lblNewLabel = new JLabel("");
        if (this.selectedModelbyUser == 1) {
            lh40_30 = new ImageIcon(this.getClass().getResource("/LH40connectPressButton230x260.jpg")).getImage();
            this.lblNewLabel.setIcon(new ImageIcon(lh40_30));
            this.lblNewLabel.setBounds(315, this.firstLabelYpos + 100, 230, 260);
            this.frame.getContentPane().add(this.lblNewLabel);
        } else {
            lh40_30 = new ImageIcon(this.getClass().getResource("/LH80connectPressButton230x240.jpg")).getImage();
            this.lblNewLabel.setIcon(new ImageIcon(lh40_30));
            this.lblNewLabel.setBounds(315, this.firstLabelYpos + 100, 230, 240);
            this.frame.getContentPane().add(this.lblNewLabel);
        }
        this.lblNewLabel_3 = new JLabel("<html>Please press and hold the master switch for a few seconds until you hear the version number.<br>");
        this.lblNewLabel_3.setHorizontalAlignment(0);
        this.lblNewLabel_3.setFont(new Font("Arial", 0, 35));
        this.lblNewLabel_3.setForeground(SystemColor.controlDkShadow);
        this.lblNewLabel_3.setBounds(120, this.firstLabelYpos + 380, 680, 120);
        this.frame.getContentPane().add(this.lblNewLabel_3);
        this.buttonimage = new ImageIcon(this.getClass().getResource("/buttonNext.jpg")).getImage();
        this.buttonConnecting = new ImageIcon(this.getClass().getResource("/buttonConnecting.jpg")).getImage();
        this.btnNext = new JButton();
        this.btnNext.setBorderPainted(false);
        this.btnNext.setIcon(new ImageIcon(this.buttonimage));
        this.btnNext.setBounds(360, this.firstLabelYpos + 520, 180, 40);
        this.frame.getContentPane().add(this.btnNext);
        this.btnNext.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                ConnectPressButton.this.startThread();
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
                ConnectPressButton.this.frame.dispose();
                new Connect((int)((ConnectPressButton)ConnectPressButton.this).selectedModelbyUser).frame.setVisible(true);
            }
        });
    }
}

