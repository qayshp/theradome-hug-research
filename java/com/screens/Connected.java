/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import SerialCommunication.Communication;
import com.fazecast.jSerialComm.SerialPort;
import com.screens.Connect;
import com.screens.CreateFrame;
import com.screens.FirmwareUpdateMenu;
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

public class Connected {
    public JFrame frame;
    Communication comm;
    SerialPort theraFoundSerialPort;
    private JButton btnNext;
    private JButton btnBack;
    private JTextField textVersion;
    private JTextField txtMicro;
    private JLabel lblHeading;
    private JLabel lblMsg2;
    private JLabel logolabel;
    private JLabel lblNewLabel;
    private JLabel lblNewLabel2;
    private JLabel lblNewLabel2_1;
    private JLabel lblNewLabel2_2;
    private JLabel lblNewLabel3;
    private JLabel lblHelmetCntdMsg;
    private int firstLabelYpos = 20;
    private int selectedModelbyUser = 0;
    String comPortName;
    Timer timerConnected;

    public void startThread() {
        try {
            this.timerConnected = new Timer("Timer");
            TimerTask task = new TimerTask(){

                @Override
                public void run() {
                    System.out.println("....myCheckConnected running...");
                    try {
                        Connected.this.btnNext.setEnabled(false);
                        Connected.this.comm.getVersionNumberFromDevice(Connected.this.theraFoundSerialPort);
                        String deviceVersion = Connected.this.comm.TheradomeVersion;
                        System.out.println("Device version : " + deviceVersion);
                        Connected.this.btnNext.setEnabled(true);
                        Connected.this.stopThread();
                        Connected.this.frame.dispose();
                        FirmwareUpdateMenu firmwareupdatescreen = new FirmwareUpdateMenu(Connected.this.selectedModelbyUser, Connected.this.comm, Connected.this.theraFoundSerialPort, deviceVersion);
                        firmwareupdatescreen.frame.setVisible(true);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
            };
            this.timerConnected.schedule(task, 1L);
        }
        catch (Exception ex) {
            this.timerConnected.cancel();
        }
    }

    public void stopThread() {
        this.timerConnected.cancel();
    }

    public Connected(int selectedModel, Communication commobj, SerialPort deviceSerialport) {
        this.selectedModelbyUser = selectedModel;
        this.comm = commobj;
        this.theraFoundSerialPort = deviceSerialport;
        this.initialize();
    }

    public Connected(int selectedModel) {
        this.selectedModelbyUser = selectedModel;
        this.initialize();
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
            this.lblNewLabel.setBounds(80, this.firstLabelYpos + 160, 260, 138);
            this.frame.getContentPane().add(this.lblNewLabel);
        } else {
            this.lblNewLabel = new JLabel("");
            lh40_30 = new ImageIcon(this.getClass().getResource("/selected_LH80-260x139.jpg")).getImage();
            this.lblNewLabel.setIcon(new ImageIcon(lh40_30));
            this.lblNewLabel.setBounds(80, this.firstLabelYpos + 160, 260, 139);
            this.frame.getContentPane().add(this.lblNewLabel);
        }
        this.lblNewLabel2 = new JLabel("");
        Image leflineimage = new ImageIcon(this.getClass().getResource("/connected_leftline_40x2.jpg")).getImage();
        this.lblNewLabel2.setIcon(new ImageIcon(leflineimage));
        this.lblNewLabel2.setBounds(360, this.firstLabelYpos + 240, 40, 2);
        this.frame.getContentPane().add(this.lblNewLabel2);
        this.lblNewLabel2_1 = new JLabel("");
        Image tickmarkimage = new ImageIcon(this.getClass().getResource("/connected_tick_50x50.jpg")).getImage();
        this.lblNewLabel2_1.setIcon(new ImageIcon(tickmarkimage));
        this.lblNewLabel2_1.setBounds(410, this.firstLabelYpos + 215, 50, 50);
        this.frame.getContentPane().add(this.lblNewLabel2_1);
        this.lblNewLabel2_2 = new JLabel("");
        Image rightlineimage = new ImageIcon(this.getClass().getResource("/connected_leftline_40x2.jpg")).getImage();
        this.lblNewLabel2_2.setIcon(new ImageIcon(rightlineimage));
        this.lblNewLabel2_2.setBounds(470, this.firstLabelYpos + 240, 40, 2);
        this.frame.getContentPane().add(this.lblNewLabel2_2);
        this.lblNewLabel3 = new JLabel("");
        Image computer = new ImageIcon(this.getClass().getResource("/connectPC_250x150.jpg")).getImage();
        this.lblNewLabel3.setIcon(new ImageIcon(computer));
        this.lblNewLabel3.setBounds(530, this.firstLabelYpos + 160, 250, 150);
        this.frame.getContentPane().add(this.lblNewLabel3);
        this.lblHelmetCntdMsg = new JLabel("Theradome connected");
        this.lblHelmetCntdMsg.setFont(new Font("Arial", 0, 35));
        this.lblHelmetCntdMsg.setForeground(SystemColor.controlDkShadow);
        this.lblHelmetCntdMsg.setBounds(300, this.firstLabelYpos + 360, 500, 100);
        this.frame.getContentPane().add(this.lblHelmetCntdMsg);
        this.textVersion = new JTextField();
        Image buttonimage = new ImageIcon(this.getClass().getResource("/buttonNext.jpg")).getImage();
        this.btnNext = new JButton();
        this.btnNext.setBorderPainted(false);
        this.btnNext.setIcon(new ImageIcon(buttonimage));
        this.btnNext.setBounds(360, this.firstLabelYpos + 520, 180, 40);
        this.frame.getContentPane().add(this.btnNext);
        this.btnNext.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Connected.this.startThread();
                }
                catch (Exception exception) {
                    // empty catch block
                }
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
                Connected.this.frame.dispose();
                new Connect((int)((Connected)Connected.this).selectedModelbyUser).frame.setVisible(true);
            }
        });
    }
}

