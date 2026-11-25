/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import com.screens.Choose;
import com.screens.CreateFrame;
import java.awt.Color;
import java.awt.EventQueue;
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

public class Splash {
    JFrame frame;
    private JLabel lblWelcome;
    private JLabel lblMsg1;
    private JLabel lblMsg2;
    private JButton btnNext;
    private int firstLabelYpos = 20;

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable(){

            @Override
            public void run() {
                try {
                    Splash window = new Splash();
                    window.frame.setVisible(true);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public Splash() {
        this.initialize();
    }

    private void initialize() {
        int XstartPos = 112;
        this.frame = new JFrame();
        CreateFrame cframe = new CreateFrame(this.frame, 1);
        JLabel label = new JLabel("");
        Image img = new ImageIcon(this.getClass().getResource("/logo_200x71.jpg")).getImage();
        label.setIcon(new ImageIcon(img));
        label.setBounds(XstartPos, this.firstLabelYpos + 20, 200, 71);
        this.frame.getContentPane().add(label);
        this.lblWelcome = new JLabel("<html> Welcome to<br>Theradome HUG<br>(Helmet Upgrade)");
        this.lblWelcome.setFont(new Font("Arial", 0, 35));
        this.lblWelcome.setForeground(SystemColor.controlDkShadow);
        this.lblWelcome.setBounds(XstartPos, this.firstLabelYpos + 150, 600, 130);
        this.frame.getContentPane().add(this.lblWelcome);
        this.lblMsg1 = new JLabel("<html>The HUG application upgrades your device software<br>to the latest version.");
        this.lblMsg1.setFont(new Font("Arial", 0, 25));
        this.lblMsg1.setForeground(SystemColor.controlDkShadow);
        this.lblMsg1.setBounds(XstartPos, this.firstLabelYpos + 300, 600, 80);
        this.frame.getContentPane().add(this.lblMsg1);
        this.lblMsg2 = new JLabel("<html>Note: Please note to fully charge (until the Master switch to solid green) your Theradome device before starting the upgrde process. <br>");
        this.lblMsg2.setFont(new Font("Arial", 0, 18));
        this.lblMsg2.setForeground(SystemColor.controlDkShadow);
        this.lblMsg2.setForeground(Color.LIGHT_GRAY);
        this.lblMsg2.setBounds(XstartPos, this.firstLabelYpos + 380, 600, 100);
        this.frame.getContentPane().add(this.lblMsg2);
        Image buttonimage = new ImageIcon(this.getClass().getResource("/buttonNext.jpg")).getImage();
        this.btnNext = new JButton();
        this.btnNext.setBorderPainted(false);
        this.btnNext.setIcon(new ImageIcon(buttonimage));
        this.btnNext.setBounds(360, this.firstLabelYpos + 520, 180, 40);
        this.frame.getContentPane().add(this.btnNext);
        this.btnNext.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                Splash.this.frame.dispose();
                Choose chooseScreen = new Choose();
                chooseScreen.frame.setVisible(true);
            }
        });
        JTextField textVersion = new JTextField();
        String firmwareVersion = "V2.0.0";
        textVersion.setHorizontalAlignment(0);
        textVersion.setBorder(null);
        textVersion.setForeground(Color.GRAY);
        textVersion.setFont(new Font("Arial", 0, 12));
        textVersion.setText(firmwareVersion);
        textVersion.setBounds(5, this.firstLabelYpos + 580, 100, 20);
        this.frame.getContentPane().add(textVersion);
    }
}

