/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import com.screens.FrameMouseTrag;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.border.Border;

public class CreateFrame {
    private JButton btnClose;
    private JButton btnMinimize;
    private JButton btnBack;
    private int Xaxis = 840;
    private int Yaxis = 630;

    public CreateFrame(JFrame newframe, int backEnable) {
        this.initialize(newframe, backEnable);
    }

    private void initialize(final JFrame newframe, int backEnable) {
        newframe.setUndecorated(true);
        newframe.getContentPane().setFont(new Font("Arial", 0, 11));
        newframe.getContentPane().setBackground(Color.white);
        newframe.setBounds(300, 50, this.Xaxis, this.Yaxis);
        newframe.setDefaultCloseOperation(3);
        newframe.getContentPane().setLayout(null);
        Border border = BorderFactory.createLineBorder(Color.GRAY, 1);
        JLabel borderlabel = new JLabel("");
        borderlabel.setBounds(0, 0, this.Xaxis, this.Yaxis);
        borderlabel.setBorder(border);
        newframe.getContentPane().add(borderlabel);
        Image closeimage = new ImageIcon(this.getClass().getResource("/close14x14.jpg")).getImage();
        this.btnClose = new JButton();
        this.btnClose.setBorderPainted(false);
        this.btnClose.setIcon(new ImageIcon(closeimage));
        this.btnClose.setBounds(this.Xaxis - 25, 20, 14, 14);
        newframe.getContentPane().add(this.btnClose);
        this.btnClose.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        Image minimizeimage = new ImageIcon(this.getClass().getResource("/minimize14x14.jpg")).getImage();
        this.btnMinimize = new JButton();
        this.btnMinimize.setBorderPainted(false);
        this.btnMinimize.setIcon(new ImageIcon(minimizeimage));
        this.btnMinimize.setBounds(this.Xaxis - 55, 20, 14, 14);
        newframe.getContentPane().add(this.btnMinimize);
        this.btnMinimize.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                newframe.setState(1);
            }
        });
        FrameMouseTrag frameDragListener = new FrameMouseTrag(newframe);
        newframe.addMouseListener(frameDragListener);
        newframe.addMouseMotionListener(frameDragListener);
    }
}

