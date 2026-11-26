/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import com.screens.SMTPConfig;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Contact
extends JFrame {
    private JLabel nameLabel = new JLabel("Name ");
    private JLabel emailLabel = new JLabel("Email ");
    private JTextField nameField = new JTextField(30);
    private JTextField emailField = new JTextField(30);
    private JButton sendButton = new JButton("Send Message");
    Properties inputConfigProperties;
    private JTextArea textAreaMessage = new JTextArea(15, 40);
    private GridBagConstraints constraints = new GridBagConstraints();

    public Contact() {
        this.setLayout(new GridBagLayout());
        this.constraints.anchor = 17;
        this.constraints.insets = new Insets(15, 15, 15, 15);
        this.setupForm();
        this.pack();
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(3);
    }

    private void setupForm() {
        this.constraints.gridx = 0;
        this.constraints.gridy = 0;
        this.add((Component)this.nameLabel, this.constraints);
        this.constraints.gridx = 1;
        this.constraints.fill = 2;
        this.add((Component)this.nameField, this.constraints);
        this.constraints.gridx = 0;
        this.constraints.gridy = 1;
        this.add((Component)this.emailLabel, this.constraints);
        this.constraints.gridx = 1;
        this.constraints.fill = 2;
        this.add((Component)this.emailField, this.constraints);
        this.constraints.gridy = 2;
        this.constraints.weightx = 1.0;
        this.constraints.weighty = 1.0;
        this.add((Component)new JScrollPane(this.textAreaMessage), this.constraints);
        this.constraints.gridy = 3;
        this.constraints.gridx = 1;
        this.constraints.gridheight = 2;
        this.constraints.fill = 10;
        this.add((Component)this.sendButton, this.constraints);
        this.sendButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                Contact.this.sendButtonActionPerformed(event);
            }
        });
    }

    private void sendButtonActionPerformed(ActionEvent event) {
        String name = this.nameField.getText();
        String emailToAddress = this.emailField.getText();
        String message = String.valueOf(this.textAreaMessage.getText()) + "<html><br><br>Received support from,<br><b>Name: </b>" + this.nameField.getText() + "<br><b>Email:</b>" + this.emailField.getText() + "<br>";
        System.out.println("Name:" + name);
        System.out.println("emailToAddress:" + emailToAddress);
        System.out.println("Message:" + message);
        SMTPConfig smtpconfig = new SMTPConfig();
        smtpconfig.sendEmail(name, /* [EDITORIAL REDACTION] contact-form recipient address */, message);
    }
}

