/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import SerialCommunication.Communication;
import com.fazecast.jSerialComm.SerialPort;
import com.screens.Choose;
import com.screens.ConnectPressButton;
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

public class Connect {
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
                        Connect.this.btnNext.setEnabled(false);
                        int respState = Connect.this.checkDeviceConnection();
                        Connect.this.btnNext.setEnabled(true);
                        Connect.this.stopThread();
                        if (respState == 1) {
                            Connect.this.frame.dispose();
                            Connected connectedScreen = new Connected(Connect.this.selectedModelbyUser, Connect.this.comm, Connect.this.theraFoundSerialPort);
                            connectedScreen.frame.setVisible(true);
                        } else {
                            Connect.this.frame.dispose();
                            NotFound NotFoundScreen = new NotFound(Connect.this.selectedModelbyUser, Connect.this.comm);
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

    public Connect(int selectedModel) {
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
            lh40_30 = new ImageIcon(this.getClass().getResource("/LH40-03_180x163.jpg")).getImage();
            this.lblNewLabel.setIcon(new ImageIcon(lh40_30));
            this.lblNewLabel.setBounds(90, this.firstLabelYpos + 140, 180, 163);
            this.frame.getContentPane().add(this.lblNewLabel);
        } else {
            lh40_30 = new ImageIcon(this.getClass().getResource("/LH80-03_180x169.jpg")).getImage();
            this.lblNewLabel.setIcon(new ImageIcon(lh40_30));
            this.lblNewLabel.setBounds(90, this.firstLabelYpos + 140, 180, 169);
            this.frame.getContentPane().add(this.lblNewLabel);
        }
        this.lblNewLabel_1 = new JLabel("");
        Image computer = new ImageIcon(this.getClass().getResource("/connectPC_250x150.jpg")).getImage();
        this.lblNewLabel_1.setIcon(new ImageIcon(computer));
        this.lblNewLabel_1.setBounds(500, this.firstLabelYpos + 140, 250, 150);
        this.frame.getContentPane().add(this.lblNewLabel_1);
        this.lblNewLabel_2 = new JLabel("");
        Image usbConnect = new ImageIcon(this.getClass().getResource("/connectUSBText_450x47.jpg")).getImage();
        this.lblNewLabel_2.setIcon(new ImageIcon(usbConnect));
        this.lblNewLabel_2.setBounds(178, this.firstLabelYpos + 303, 450, 47);
        this.frame.getContentPane().add(this.lblNewLabel_2);
        this.lblNewLabel_3 = new JLabel("<html>Please connect the Theradome USB cable to the device and computer<br>");
        this.lblNewLabel_3.setHorizontalAlignment(0);
        this.lblNewLabel_3.setFont(new Font("Arial", 0, 35));
        this.lblNewLabel_3.setForeground(SystemColor.controlDkShadow);
        this.lblNewLabel_3.setBounds(120, this.firstLabelYpos + 360, 680, 100);
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
                Connect.this.frame.dispose();
                new ConnectPressButton((int)((Connect)Connect.this).selectedModelbyUser).frame.setVisible(true);
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
                Connect.this.frame.dispose();
                new Choose().frame.setVisible(true);
            }
        });
    }
}

