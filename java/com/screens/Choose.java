/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import com.screens.Connect;
import com.screens.CreateFrame;
import com.screens.Splash;
import java.awt.Font;
import java.awt.Image;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Choose {
    JFrame frame;
    private JButton btnNext;
    private JButton btnLH80;
    private JButton btnLH40;
    private JButton btnBack;
    private JLabel lblchoosemsg;
    private JLabel lblLH80;
    private JLabel lblLH40;
    private int firstLabelYpos = 20;
    private int modelSelected = 0;

    public Choose() {
        this.initialize();
    }

    private void initialize() {
        this.frame = new JFrame();
        CreateFrame cframe = new CreateFrame(this.frame, 1);
        JLabel label = new JLabel("");
        Image img = new ImageIcon(this.getClass().getResource("/logo_140x49.jpg")).getImage();
        label.setIcon(new ImageIcon(img));
        label.setBounds(370, 8, 140, 49);
        this.frame.getContentPane().add(label);
        this.lblchoosemsg = new JLabel("Choose your Theradome helmet");
        this.lblchoosemsg.setFont(new Font("Arial", 0, 35));
        this.lblchoosemsg.setForeground(SystemColor.controlDkShadow);
        this.lblchoosemsg.setBounds(170, this.firstLabelYpos + 100, 600, 60);
        this.frame.getContentPane().add(this.lblchoosemsg);
        Image image_LH80 = new ImageIcon(this.getClass().getResource("/selected_LH80-260x139.jpg")).getImage();
        this.btnLH80 = new JButton();
        this.btnLH80.setBorderPainted(false);
        this.btnLH80.setIcon(new ImageIcon(image_LH80));
        this.btnLH80.setBounds(112, this.firstLabelYpos + 220, 260, 139);
        this.btnLH80.setBorderPainted(false);
        this.frame.getContentPane().add(this.btnLH80);
        this.lblLH80 = new JLabel("Theradome PRO (LH80)");
        this.lblLH80.setText("<html><font color=green><b>Theradome PRO (LH80)</font><br>");
        this.lblLH80.setFont(new Font("Arial", 0, 22));
        this.lblLH80.setForeground(SystemColor.controlDkShadow);
        this.lblLH80.setBounds(126, this.firstLabelYpos + 400, 300, 30);
        this.frame.getContentPane().add(this.lblLH80);
        this.btnLH80.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                if (Choose.this.modelSelected == 1) {
                    Choose.this.modelSelected = 0;
                    Image image_LH80 = new ImageIcon(this.getClass().getResource("/selected_LH80-260x139.jpg")).getImage();
                    Choose.this.btnLH80.setIcon(new ImageIcon(image_LH80));
                    Choose.this.lblLH80.setText("<html><font color=green><b>Theradome PRO (LH80)</font><br>");
                    Image image_LH40 = new ImageIcon(this.getClass().getResource("/unselected_LH40_260x138.jpg")).getImage();
                    Choose.this.btnLH40.setIcon(new ImageIcon(image_LH40));
                    Choose.this.lblLH40.setText("<html><font color=gray>Theradome EVO (LH40)</font><br>");
                }
            }
        });
        Image image_LH40 = new ImageIcon(this.getClass().getResource("/unselected_LH40_260x138.jpg")).getImage();
        this.btnLH40 = new JButton();
        this.btnLH40.setBorderPainted(false);
        this.btnLH40.setIcon(new ImageIcon(image_LH40));
        this.btnLH40.setBounds(490, this.firstLabelYpos + 220, 260, 138);
        this.btnLH40.setBorderPainted(false);
        this.frame.getContentPane().add(this.btnLH40);
        this.lblLH40 = new JLabel("Theradome EVO (LH40)");
        this.lblLH40.setFont(new Font("Arial", 0, 22));
        this.lblLH40.setForeground(SystemColor.controlDkShadow);
        this.lblLH40.setBounds(504, this.firstLabelYpos + 400, 300, 30);
        this.lblLH40.setText("<html><font color=gray>Theradome EVO (LH40)</font><br>");
        this.frame.getContentPane().add(this.lblLH40);
        this.btnLH40.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                if (Choose.this.modelSelected == 0) {
                    Choose.this.modelSelected = 1;
                    Image image_LH80 = new ImageIcon(this.getClass().getResource("/unselected_LH80-260x139.jpg")).getImage();
                    Choose.this.btnLH80.setIcon(new ImageIcon(image_LH80));
                    Choose.this.lblLH80.setText("<html><font color=gray>Theradome PRO (LH80)</font><br>");
                    Image image_LH40 = new ImageIcon(this.getClass().getResource("/selected_LH40_260x138.jpg")).getImage();
                    Choose.this.btnLH40.setIcon(new ImageIcon(image_LH40));
                    Choose.this.lblLH40.setText("<html><font color=green><b>Theradome EVO (LH40)</font><br>");
                }
            }
        });
        Image buttonimage = new ImageIcon(this.getClass().getResource("/buttonNext.jpg")).getImage();
        this.btnNext = new JButton();
        this.btnNext.setBorderPainted(false);
        this.btnNext.setIcon(new ImageIcon(buttonimage));
        this.btnNext.setBounds(360, this.firstLabelYpos + 520, 180, 40);
        this.frame.getContentPane().add(this.btnNext);
        this.btnNext.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                Choose.this.frame.dispose();
                Connect ConnectScreen = new Connect(Choose.this.modelSelected);
                ConnectScreen.frame.setVisible(true);
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
                Choose.this.frame.dispose();
                new Splash().frame.setVisible(true);
            }
        });
    }
}

