/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import SerialCommunication.Communication;
import com.fazecast.jSerialComm.SerialPort;
import com.screens.ConnectPressButton;
import com.screens.Connected;
import com.screens.CreateFrame;
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

public class NotFound {
    JFrame frame;
    Communication comm;
    SerialPort theraFoundSerialPort;
    private JButton btnNext;
    private JButton btnBack;
    private JLabel logolabel;
    private JLabel lblNewLabel;
    private JLabel lblNewLabe2;
    private JLabel lblNewLabe3;
    private JLabel lblHeading;
    private int firstLabelYpos = 20;
    private int selectedModelbyUser = 0;
    boolean enableThread = false;
    Timer timerTryAgain;

    public void startThread() {
        try {
            System.out.println("....timer task init ");
            this.timerTryAgain = new Timer("Timer");
            TimerTask task = new TimerTask(){

                @Override
                public void run() {
                    System.out.println("....myCheckTryAgain running...");
                    NotFound.this.btnNext.setEnabled(false);
                    int respcode = NotFound.this.checkDeviceConnection();
                    NotFound.this.btnNext.setEnabled(true);
                    NotFound.this.stopThread();
                    if (respcode == 1) {
                        NotFound.this.frame.dispose();
                        Connected connectedScreen = new Connected(NotFound.this.selectedModelbyUser, NotFound.this.comm, NotFound.this.theraFoundSerialPort);
                        connectedScreen.frame.setVisible(true);
                    }
                }
            };
            this.timerTryAgain.schedule(task, 1L);
        }
        catch (Exception ex) {
            this.timerTryAgain.cancel();
        }
    }

    public void stopThread() {
        this.timerTryAgain.cancel();
    }

    public NotFound(int selectedModel, Communication commobject) {
        this.selectedModelbyUser = selectedModel;
        this.comm = commobject;
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
        this.frame = new JFrame();
        CreateFrame cframe = new CreateFrame(this.frame, 1);
        this.logolabel = new JLabel("");
        Image img = new ImageIcon(this.getClass().getResource("/logo_140x49.jpg")).getImage();
        this.logolabel.setIcon(new ImageIcon(img));
        this.logolabel.setBounds(370, 8, 140, 49);
        this.frame.getContentPane().add(this.logolabel);
        if (this.selectedModelbyUser == 1) {
            this.lblNewLabel = new JLabel("");
            lh40_30 = new ImageIcon(this.getClass().getResource("/selected_LH40_260x138.jpg")).getImage();
            this.lblNewLabel.setIcon(new ImageIcon(lh40_30));
            this.lblNewLabel.setBounds(300, this.firstLabelYpos + 100, 260, 138);
            this.frame.getContentPane().add(this.lblNewLabel);
        } else {
            this.lblNewLabel = new JLabel("");
            lh40_30 = new ImageIcon(this.getClass().getResource("/selected_LH80-260x139.jpg")).getImage();
            this.lblNewLabel.setIcon(new ImageIcon(lh40_30));
            this.lblNewLabel.setBounds(300, this.firstLabelYpos + 100, 260, 139);
            this.frame.getContentPane().add(this.lblNewLabel);
        }
        this.lblNewLabe2 = new JLabel("Theradome not found");
        this.lblNewLabe2.setFont(new Font("Arial", 0, 35));
        this.lblNewLabe2.setForeground(SystemColor.controlDkShadow);
        this.lblNewLabe2.setBounds(260, this.firstLabelYpos + 280, 400, 40);
        this.frame.getContentPane().add(this.lblNewLabe2);
        this.lblNewLabe3 = new JLabel("<html>Check the device connection, please re-connect to your computer<br> <font color=red>Please press and hold the master switch for few seconds until version number is heard.!</font><br>");
        this.lblNewLabe3.setFont(new Font("Arial", 0, 22));
        this.lblNewLabe3.setForeground(SystemColor.controlDkShadow);
        this.lblNewLabe3.setBounds(150, this.firstLabelYpos + 360, 650, 70);
        this.frame.getContentPane().add(this.lblNewLabe3);
        Image buttonimage = new ImageIcon(this.getClass().getResource("/buttonTryAgain.jpg")).getImage();
        this.btnNext = new JButton();
        this.btnNext.setBorderPainted(false);
        this.btnNext.setIcon(new ImageIcon(buttonimage));
        this.btnNext.setBounds(360, this.firstLabelYpos + 520, 180, 40);
        this.frame.getContentPane().add(this.btnNext);
        this.btnNext.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("....button next is pressed...");
                NotFound.this.startThread();
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
                NotFound.this.frame.dispose();
                new ConnectPressButton((int)((NotFound)NotFound.this).selectedModelbyUser).frame.setVisible(true);
            }
        });
    }
}

