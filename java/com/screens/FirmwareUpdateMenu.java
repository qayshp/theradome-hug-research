/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import SerialCommunication.Communication;
import com.fazecast.jSerialComm.SerialPort;
import com.screens.Connected;
import com.screens.CreateFrame;
import com.screens.DownloadVersion;
import com.screens.FirmwareFetchFailed;
import com.screens.FirmwareUpdateFetch;
import com.screens.SMTPConfig;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;

public class FirmwareUpdateMenu {
    String deviceVersion;
    JFrame frame;
    Communication comm;
    SerialPort theraFoundSerialPort;
    private JButton upgradebtnNext;
    private JButton btnBack;
    private JButton btnHelmetUpgrade;
    private JButton btnContact;
    private JButton btnTroubleshoot;
    private JTextField textVersion;
    private JTextField textConnectStatus;
    private JLabel upgradelblNewLabel;
    private JLabel logolabel;
    private JLabel upgradelblNewLabe3;
    private JLabel lblHeading;
    private JLabel lblConnectedMark;
    private int firstLabelYpos = 20;
    private int selectedModelbyUser = 0;
    String firmwareVersion;
    String HexVersion = "";
    Image helmetdlimage;
    Image unHelmetdlimage;
    Image contactimage;
    Image unContactimage;
    Image troubleimage;
    Image unTroubleimage;
    private JLabel contactNameLabel;
    private JLabel contactEmailLabel;
    private JTextField contactNameField;
    private Border contactNameborder;
    private JTextField contactEmailField;
    private Border contactEmailborder;
    private JTextField contactSerialField;
    private Border contactSerialborder;
    private JTextArea contactTextAreaMessage;
    private JScrollPane contactTextAreascroll;
    private Border contactTextAreaborder;
    private JButton contactSendButton;
    Image contactSendButtonimage;
    private JButton contactSendNewButton;
    Image contactSendNewButtonimage;
    Timer timerSendMail;
    private JLabel lblCompleted;
    private JLabel lblSentStatus;
    private JLabel lblSentErrorStatus;
    private JEditorPane troubleTextAreaMessage;
    private JScrollPane scrollingArea;

    void sendMailService() {
        int mailError = 0;
        String name = this.contactNameField.getText();
        String emailToAddress = this.contactEmailField.getText();
        String serialnumber = this.contactSerialField.getText();
        String msgArea = this.contactTextAreaMessage.getText();
        String FinalMsg = "<html>";
        int i = 0;
        while (i < msgArea.length()) {
            String addStr = "<br>";
            if (msgArea.charAt(i) == '\n') {
                addStr = "<br>";
            } else {
                char a = msgArea.charAt(i);
                addStr = Character.toString(a);
            }
            FinalMsg = String.valueOf(FinalMsg) + addStr;
            ++i;
        }
        FinalMsg = String.valueOf(FinalMsg) + "<br>";
        String message = String.valueOf(FinalMsg) + "<html><br><br>Received support for,<br><b>Name: </b>" + name + "<br><b>Email:</b>" + emailToAddress + "<br><b>Serial Number:</b>" + serialnumber + "<br>";
        System.out.println("Name:" + name);
        System.out.println("emailToAddress:" + emailToAddress);
        System.out.println("Message:" + message);
        SMTPConfig smtpconfig = new SMTPConfig();
        mailError = smtpconfig.sendEmail(name, /* [EDITORIAL REDACTION] support-form recipient address */, message);
        this.contactNameField.setVisible(false);
        this.contactEmailField.setVisible(false);
        this.contactSerialField.setVisible(false);
        this.contactSendButton.setVisible(false);
        this.contactTextAreascroll.setVisible(false);
        if (mailError == 0) {
            this.lblCompleted.setVisible(true);
            this.lblSentStatus.setVisible(true);
            this.lblSentErrorStatus.setVisible(false);
            this.contactSendNewButton.setVisible(true);
        } else {
            String[] mailErrorCode = new String[]{" ", "<html> Error: Authentication<br>", "<html> Error: Config1<br>", "<html> Error: Config2<br>", "<html> Error: Unable To Sent mail, Check Internet Connection<br>"};
            this.lblCompleted.setVisible(false);
            this.lblSentStatus.setVisible(false);
            this.lblSentErrorStatus.setVisible(true);
            try {
                this.lblSentErrorStatus.setText(mailErrorCode[mailError]);
            }
            catch (Exception exception) {
                // empty catch block
            }
            this.contactSendNewButton.setVisible(true);
        }
        this.contactSendNewButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                FirmwareUpdateMenu.this.upgradelblNewLabel.setVisible(false);
                FirmwareUpdateMenu.this.upgradelblNewLabe3.setVisible(false);
                FirmwareUpdateMenu.this.upgradebtnNext.setVisible(false);
                FirmwareUpdateMenu.this.scrollingArea.setVisible(false);
                FirmwareUpdateMenu.this.troubleTextAreaMessage.setVisible(false);
                FirmwareUpdateMenu.this.lblCompleted.setVisible(false);
                FirmwareUpdateMenu.this.lblSentStatus.setVisible(false);
                FirmwareUpdateMenu.this.lblSentErrorStatus.setVisible(false);
                FirmwareUpdateMenu.this.contactSendNewButton.setVisible(false);
                FirmwareUpdateMenu.this.contactNameField.setVisible(true);
                FirmwareUpdateMenu.this.contactEmailField.setVisible(true);
                FirmwareUpdateMenu.this.contactSerialField.setVisible(true);
                FirmwareUpdateMenu.this.contactTextAreaMessage.setVisible(true);
                FirmwareUpdateMenu.this.contactTextAreascroll.setVisible(true);
                FirmwareUpdateMenu.this.contactSendButton.setVisible(true);
                FirmwareUpdateMenu.this.contactNameField.setText(" Name");
                FirmwareUpdateMenu.this.contactEmailField.setText(" Email");
                FirmwareUpdateMenu.this.contactSerialField.setText(" Serial Number");
                FirmwareUpdateMenu.this.contactTextAreaMessage.setText(" Message");
            }
        });
    }

    public void startThreadMail() {
        try {
            System.out.println("....timer send mail running");
            this.timerSendMail = new Timer("Timer");
            TimerTask task = new TimerTask(){

                @Override
                public void run() {
                    FirmwareUpdateMenu.this.contactSendButton.setEnabled(false);
                    FirmwareUpdateMenu.this.sendMailService();
                    FirmwareUpdateMenu.this.contactSendButton.setEnabled(true);
                }
            };
            this.timerSendMail.schedule(task, 1L);
        }
        catch (Exception ex) {
            this.timerSendMail.cancel();
        }
    }

    public void stopThreadMail() {
        this.timerSendMail.cancel();
    }

    public FirmwareUpdateMenu(int selectedModel, Communication commobj, SerialPort deviceSerialport, String Dversion) {
        this.selectedModelbyUser = selectedModel;
        this.comm = commobj;
        this.theraFoundSerialPort = deviceSerialport;
        this.deviceVersion = Dversion;
        this.initialize();
    }

    void createJdialogPopup(String popupMsg) {
        final JDialog jdialog = new JDialog((Frame)this.frame, "Firmware Upgrade");
        jdialog.setBounds(550, 320, 400, 200);
        JLabel dLabelMsg1 = new JLabel();
        dLabelMsg1.setText(popupMsg);
        dLabelMsg1.setFont(new Font("Arial", 0, 16));
        dLabelMsg1.setForeground(SystemColor.controlDkShadow);
        dLabelMsg1.setBounds(50, 50, 300, 80);
        JButton dButtonYes = new JButton("Yes");
        dButtonYes.setBorderPainted(false);
        dButtonYes.setBackground(Color.LIGHT_GRAY);
        dButtonYes.setSize(50, 30);
        dButtonYes.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                FirmwareUpdateMenu.this.frame.dispose();
                FirmwareUpdateFetch FirmwareUpdateFetchScreen = new FirmwareUpdateFetch(FirmwareUpdateMenu.this.selectedModelbyUser, FirmwareUpdateMenu.this.comm, FirmwareUpdateMenu.this.theraFoundSerialPort, FirmwareUpdateMenu.this.deviceVersion);
                FirmwareUpdateFetchScreen.frame.setVisible(true);
            }
        });
        JButton dButtonNo = new JButton("No");
        dButtonNo.setBorderPainted(false);
        dButtonNo.setBackground(Color.LIGHT_GRAY);
        dButtonNo.setSize(50, 30);
        dButtonNo.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                jdialog.setVisible(false);
            }
        });
        JPanel jpanel = new JPanel();
        FlowLayout flow = new FlowLayout();
        flow.setHgap(50);
        jpanel.setLayout(flow);
        flow.setVgap(10);
        jpanel.add(dLabelMsg1);
        flow.setVgap(20);
        jpanel.add(dButtonYes);
        jpanel.add(dButtonNo);
        jdialog.add(jpanel);
        jdialog.setVisible(true);
    }

    void checkHexVersion() {
        DownloadVersion dwnldVer = new DownloadVersion();
        String fileURL = "https://raw.githubusercontent.com/renuraja03/fwupdate/master/fwversion.txt";
        long error = dwnldVer.downLoadProcess(fileURL);
        if (error == 1L) {
            this.HexVersion = dwnldVer.getVersion();
            Integer DeviceVer = Integer.valueOf(this.deviceVersion);
            Integer hexfileVer = dwnldVer.getHexVersionInt();
            System.out.println("\n\rDevice version:" + Integer.toString(DeviceVer) + "Hex version:" + Integer.toString(hexfileVer));
            String popupStr = "";
            popupStr = hexfileVer > DeviceVer ? String.valueOf(popupStr) + "<html>WARNING: Newer version of firmware is available<br>" + " Do you like to Upgrade ? " : String.valueOf(popupStr) + "<html>WARNING: Device already have latest firmware.<br>" + " Do you like to Upgrade ? ";
            System.out.println(popupStr);
            try {
                this.createJdialogPopup(popupStr);
            }
            catch (Exception ex) {
                System.out.println("get version failed");
            }
        } else {
            System.out.println("Http file error:" + Long.toString(error));
            this.frame.dispose();
            new FirmwareFetchFailed((int)this.selectedModelbyUser, (Communication)this.comm, (SerialPort)this.theraFoundSerialPort, (String)this.deviceVersion).frame.setVisible(true);
        }
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
        this.helmetdlimage = new ImageIcon(this.getClass().getResource("/select_fmenuHelmetupdate_220x47.jpg")).getImage();
        this.unHelmetdlimage = new ImageIcon(this.getClass().getResource("/unselect_fmenuHelmetupdate_220x47.jpg")).getImage();
        this.btnHelmetUpgrade = new JButton();
        this.btnHelmetUpgrade.setBorderPainted(false);
        this.btnHelmetUpgrade.setIcon(new ImageIcon(this.helmetdlimage));
        this.btnHelmetUpgrade.setBounds(70, this.firstLabelYpos + 100, 220, 47);
        this.frame.getContentPane().add(this.btnHelmetUpgrade);
        this.contactimage = new ImageIcon(this.getClass().getResource("/select_fmenuContact_220x47.jpg")).getImage();
        this.unContactimage = new ImageIcon(this.getClass().getResource("/unselect_fmenuContact_220x47.jpg")).getImage();
        this.btnContact = new JButton();
        this.btnContact.setBorderPainted(false);
        this.btnContact.setIcon(new ImageIcon(this.unContactimage));
        this.btnContact.setBounds(310, this.firstLabelYpos + 100, 220, 47);
        this.frame.getContentPane().add(this.btnContact);
        this.troubleimage = new ImageIcon(this.getClass().getResource("/select_fmenuTroubleshoot_220x47.jpg")).getImage();
        this.unTroubleimage = new ImageIcon(this.getClass().getResource("/unselect_fmenuTroubleshoot_220x47.jpg")).getImage();
        this.btnTroubleshoot = new JButton();
        this.btnTroubleshoot.setBorderPainted(false);
        this.btnTroubleshoot.setIcon(new ImageIcon(this.unTroubleimage));
        this.btnTroubleshoot.setBounds(550, this.firstLabelYpos + 100, 220, 47);
        this.frame.getContentPane().add(this.btnTroubleshoot);
        if (this.selectedModelbyUser == 1) {
            this.upgradelblNewLabel = new JLabel("");
            lh40_30 = new ImageIcon(this.getClass().getResource("/selected_LH40_260x138.jpg")).getImage();
            this.upgradelblNewLabel.setIcon(new ImageIcon(lh40_30));
            this.upgradelblNewLabel.setBounds(300, this.firstLabelYpos + 220, 260, 138);
            this.frame.getContentPane().add(this.upgradelblNewLabel);
        } else {
            this.upgradelblNewLabel = new JLabel("");
            lh40_30 = new ImageIcon(this.getClass().getResource("/selected_LH80-260x139.jpg")).getImage();
            this.upgradelblNewLabel.setIcon(new ImageIcon(lh40_30));
            this.upgradelblNewLabel.setBounds(300, this.firstLabelYpos + 220, 260, 139);
            this.frame.getContentPane().add(this.upgradelblNewLabel);
        }
        this.upgradelblNewLabe3 = new JLabel("Click on Helmet Upgrade to update your Theradome");
        this.upgradelblNewLabe3.setFont(new Font("Arial", 0, 22));
        this.upgradelblNewLabe3.setForeground(SystemColor.controlDkShadow);
        this.upgradelblNewLabe3.setBounds(180, this.firstLabelYpos + 400, 600, 60);
        this.frame.getContentPane().add(this.upgradelblNewLabe3);
        Image buttonimage = new ImageIcon(this.getClass().getResource("/buttonHelmetUpgrade.jpg")).getImage();
        this.upgradebtnNext = new JButton();
        this.upgradebtnNext.setBorderPainted(false);
        this.upgradebtnNext.setIcon(new ImageIcon(buttonimage));
        this.upgradebtnNext.setBounds(360, this.firstLabelYpos + 520, 180, 40);
        this.frame.getContentPane().add(this.upgradebtnNext);
        this.contactNameField = new JTextField();
        this.contactNameField.setHorizontalAlignment(2);
        this.contactNameField.setForeground(Color.GRAY);
        this.contactNameField.setFont(new Font("Arial", 0, 22));
        this.contactNameField.setText(" Name");
        this.contactNameField.setBounds(180, this.firstLabelYpos + 180, 500, 40);
        this.contactNameborder = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1);
        this.contactNameField.setBorder(this.contactNameborder);
        this.contactNameField.setVisible(false);
        this.contactNameField.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e) {
                if (FirmwareUpdateMenu.this.contactNameField.getText().equals(" Name")) {
                    FirmwareUpdateMenu.this.contactNameField.setText("");
                }
            }
        });
        this.frame.getContentPane().add(this.contactNameField);
        this.contactEmailField = new JTextField();
        this.contactEmailField.setHorizontalAlignment(2);
        this.contactEmailField.setForeground(Color.GRAY);
        this.contactEmailField.setFont(new Font("Arial", 0, 22));
        this.contactEmailField.setText(" Email");
        this.contactEmailField.setBounds(180, this.firstLabelYpos + 250, 500, 40);
        this.contactEmailborder = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1);
        this.contactEmailField.setBorder(this.contactEmailborder);
        this.contactEmailField.setVisible(false);
        this.frame.getContentPane().add(this.contactEmailField);
        this.contactEmailField.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e) {
                if (FirmwareUpdateMenu.this.contactEmailField.getText().equals(" Email")) {
                    FirmwareUpdateMenu.this.contactEmailField.setText("");
                }
            }
        });
        this.contactSerialField = new JTextField();
        this.contactSerialField.setHorizontalAlignment(2);
        this.contactSerialField.setForeground(Color.GRAY);
        this.contactSerialField.setFont(new Font("Arial", 0, 22));
        this.contactSerialField.setText(" Serial Number");
        this.contactSerialField.setBounds(180, this.firstLabelYpos + 320, 500, 40);
        this.contactSerialborder = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1);
        this.contactSerialField.setBorder(this.contactSerialborder);
        this.contactSerialField.setVisible(false);
        this.frame.getContentPane().add(this.contactSerialField);
        this.contactSerialField.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e) {
                if (FirmwareUpdateMenu.this.contactSerialField.getText().equals(" Serial Number")) {
                    FirmwareUpdateMenu.this.contactSerialField.setText("");
                }
            }
        });
        this.contactTextAreascroll = new JScrollPane();
        this.contactTextAreascroll.setBounds(180, this.firstLabelYpos + 390, 500, 110);
        this.contactTextAreascroll.setVisible(false);
        this.contactTextAreaborder = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1);
        this.contactTextAreascroll.setBorder(this.contactTextAreaborder);
        this.frame.getContentPane().add(this.contactTextAreascroll);
        this.contactTextAreaMessage = new JTextArea();
        this.contactTextAreaMessage.setForeground(Color.GRAY);
        this.contactTextAreaMessage.setFont(new Font("Arial", 0, 22));
        this.contactTextAreaMessage.setText(" Message");
        this.contactTextAreaMessage.setBounds(0, 0, 500, 110);
        this.contactTextAreaMessage.setVisible(true);
        this.contactTextAreascroll.getViewport().add(this.contactTextAreaMessage);
        this.contactTextAreaMessage.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e) {
                if (FirmwareUpdateMenu.this.contactTextAreaMessage.getText().equals(" Message")) {
                    FirmwareUpdateMenu.this.contactTextAreaMessage.setText("");
                }
            }
        });
        this.contactSendButtonimage = new ImageIcon(this.getClass().getResource("/buttonSendMessage.jpg")).getImage();
        this.contactSendButton = new JButton();
        this.contactSendButton.setBorderPainted(false);
        this.contactSendButton.setIcon(new ImageIcon(this.contactSendButtonimage));
        this.contactSendButton.setBounds(360, this.firstLabelYpos + 520, 180, 40);
        this.contactSendButton.setVisible(false);
        this.frame.getContentPane().add(this.contactSendButton);
        this.lblCompleted = new JLabel("");
        Image lblCompletedimage = new ImageIcon(this.getClass().getResource("/programCompleted_80x76.jpg")).getImage();
        this.lblCompleted.setIcon(new ImageIcon(lblCompletedimage));
        this.lblCompleted.setBounds(380, this.firstLabelYpos + 250, 80, 76);
        this.lblCompleted.setVisible(false);
        this.frame.getContentPane().add(this.lblCompleted);
        this.lblSentStatus = new JLabel("<html> Your Message has been successfully sent<br>");
        this.lblSentStatus.setFont(new Font("Arial", 0, 22));
        this.lblSentStatus.setForeground(SystemColor.controlDkShadow);
        this.lblSentStatus.setVisible(false);
        this.lblSentStatus.setBounds(220, this.firstLabelYpos + 400, 600, 80);
        this.frame.getContentPane().add(this.lblSentStatus);
        this.lblSentErrorStatus = new JLabel("<html>Error: Failed to Sent<br>");
        this.lblSentErrorStatus.setFont(new Font("Arial", 0, 22));
        this.lblSentErrorStatus.setForeground(SystemColor.RED);
        this.lblSentErrorStatus.setHorizontalAlignment(0);
        this.lblSentErrorStatus.setVisible(false);
        this.lblSentErrorStatus.setBounds(100, this.firstLabelYpos + 280, 600, 80);
        this.frame.getContentPane().add(this.lblSentErrorStatus);
        this.contactSendNewButtonimage = new ImageIcon(this.getClass().getResource("/buttonSendNewMessage.jpg")).getImage();
        this.contactSendNewButton = new JButton();
        this.contactSendNewButton.setBorderPainted(false);
        this.contactSendNewButton.setIcon(new ImageIcon(this.contactSendNewButtonimage));
        this.contactSendNewButton.setBounds(360, this.firstLabelYpos + 520, 180, 40);
        this.contactSendNewButton.setVisible(false);
        this.frame.getContentPane().add(this.contactSendNewButton);
        String troubleShootText1a = "<html><b>- My Theradome is unrecognized by the application</b><br><html><font color=gray>  Make sure the USB-micro USB cable is  connected properly to the computer and your Theradome device.</font><br><html><br><b>- My Theradome is stuck in calibration mode after the upgrade process.</b><br><html><font color=gray>  If your Theradome device is upgraded successfully and repeatedly says \"calibration\", please plug-in your Theradome for charging until the master switch turns solid green. The Theradome automatically comes out of calibration mode. Press the master switch once to continue using the device as normal.</font><br><html><br><b>- My Theradome is connected successfully, but the upgrade process does not start.</b><br><html><font color=gray>  There might be an issue  with your internet connectivity. Please check if you have an active internet connection.</font><br><html><br><b>- My device already has the latest version, should I proceed for an upgrade ?</b><br><html><font color=gray>  The purpose of the HUG is not only to update your Theradome to the latest version but to also reset the device to normal if the user is experiencing any issues with the device.</font><br>";
        this.scrollingArea = new JScrollPane();
        this.scrollingArea.setHorizontalScrollBarPolicy(31);
        this.scrollingArea.setVerticalScrollBarPolicy(22);
        this.scrollingArea.setBounds(120, this.firstLabelYpos + 180, 600, 370);
        this.scrollingArea.setVisible(false);
        this.scrollingArea.setBorder(null);
        this.frame.getContentPane().add(this.scrollingArea);
        this.troubleTextAreaMessage = new JEditorPane("text/html", "");
        this.troubleTextAreaMessage.setForeground(Color.LIGHT_GRAY);
        this.troubleTextAreaMessage.setFont(new Font("Arial", 0, 18));
        this.troubleTextAreaMessage.setText(troubleShootText1a);
        this.troubleTextAreaMessage.setBounds(120, this.firstLabelYpos + 200, 600, 350);
        this.troubleTextAreaMessage.setEditable(false);
        this.troubleTextAreaMessage.setVisible(true);
        this.scrollingArea.getViewport().add(this.troubleTextAreaMessage);
        this.textVersion = new JTextField();
        this.firmwareVersion = "Current Version " + this.deviceVersion;
        this.textVersion.setHorizontalAlignment(0);
        this.textVersion.setBorder(null);
        this.textVersion.setForeground(Color.GRAY);
        this.textVersion.setOpaque(false);
        this.textVersion.setFont(new Font("Arial", 0, 14));
        this.textVersion.setText(this.firmwareVersion);
        this.textVersion.setBounds(10, this.firstLabelYpos + 580, 200, 20);
        this.textVersion.setEditable(false);
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
        this.textConnectStatus.setOpaque(false);
        this.textConnectStatus.setEditable(false);
        this.textConnectStatus.setBounds(580, this.firstLabelYpos + 580, 150, 20);
        this.frame.getContentPane().add(this.textConnectStatus);
        Image backimage = new ImageIcon(this.getClass().getResource("/back14x14.jpg")).getImage();
        this.btnBack = new JButton();
        this.btnBack.setBorderPainted(false);
        this.btnBack.setIcon(new ImageIcon(backimage));
        this.btnBack.setBounds(20, 20, 14, 14);
        this.frame.getContentPane().add(this.btnBack);
        this.btnHelmetUpgrade.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                FirmwareUpdateMenu.this.contactNameField.setVisible(false);
                FirmwareUpdateMenu.this.contactEmailField.setVisible(false);
                FirmwareUpdateMenu.this.contactSerialField.setVisible(false);
                FirmwareUpdateMenu.this.contactTextAreascroll.setVisible(false);
                FirmwareUpdateMenu.this.contactSendButton.setVisible(false);
                FirmwareUpdateMenu.this.lblCompleted.setVisible(false);
                FirmwareUpdateMenu.this.lblSentStatus.setVisible(false);
                FirmwareUpdateMenu.this.lblSentErrorStatus.setVisible(false);
                FirmwareUpdateMenu.this.contactSendNewButton.setVisible(false);
                FirmwareUpdateMenu.this.scrollingArea.setVisible(false);
                FirmwareUpdateMenu.this.troubleTextAreaMessage.setVisible(false);
                FirmwareUpdateMenu.this.btnHelmetUpgrade.setIcon(new ImageIcon(FirmwareUpdateMenu.this.helmetdlimage));
                FirmwareUpdateMenu.this.btnContact.setIcon(new ImageIcon(FirmwareUpdateMenu.this.unContactimage));
                FirmwareUpdateMenu.this.btnTroubleshoot.setIcon(new ImageIcon(FirmwareUpdateMenu.this.unTroubleimage));
                FirmwareUpdateMenu.this.upgradelblNewLabel.setVisible(true);
                FirmwareUpdateMenu.this.upgradelblNewLabe3.setVisible(true);
                FirmwareUpdateMenu.this.upgradebtnNext.setVisible(true);
            }
        });
        this.btnContact.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                FirmwareUpdateMenu.this.btnHelmetUpgrade.setIcon(new ImageIcon(FirmwareUpdateMenu.this.unHelmetdlimage));
                FirmwareUpdateMenu.this.btnContact.setIcon(new ImageIcon(FirmwareUpdateMenu.this.contactimage));
                FirmwareUpdateMenu.this.btnTroubleshoot.setIcon(new ImageIcon(FirmwareUpdateMenu.this.unTroubleimage));
                FirmwareUpdateMenu.this.upgradelblNewLabel.setVisible(false);
                FirmwareUpdateMenu.this.upgradelblNewLabe3.setVisible(false);
                FirmwareUpdateMenu.this.upgradebtnNext.setVisible(false);
                FirmwareUpdateMenu.this.scrollingArea.setVisible(false);
                FirmwareUpdateMenu.this.troubleTextAreaMessage.setVisible(false);
                FirmwareUpdateMenu.this.contactEmailField.setText(" Email");
                FirmwareUpdateMenu.this.contactNameField.setText(" Name");
                FirmwareUpdateMenu.this.contactSerialField.setText(" Serial Number");
                FirmwareUpdateMenu.this.contactTextAreaMessage.setText(" Message");
                FirmwareUpdateMenu.this.contactNameField.setVisible(true);
                FirmwareUpdateMenu.this.contactEmailField.setVisible(true);
                FirmwareUpdateMenu.this.contactSerialField.setVisible(true);
                FirmwareUpdateMenu.this.contactTextAreaMessage.setVisible(true);
                FirmwareUpdateMenu.this.contactTextAreascroll.setVisible(true);
                FirmwareUpdateMenu.this.contactSendButton.setVisible(true);
                FirmwareUpdateMenu.this.lblCompleted.setVisible(false);
                FirmwareUpdateMenu.this.lblSentStatus.setVisible(false);
                FirmwareUpdateMenu.this.lblSentErrorStatus.setVisible(false);
                FirmwareUpdateMenu.this.contactSendNewButton.setVisible(false);
            }
        });
        this.btnTroubleshoot.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                FirmwareUpdateMenu.this.contactNameField.setVisible(false);
                FirmwareUpdateMenu.this.contactEmailField.setVisible(false);
                FirmwareUpdateMenu.this.contactSerialField.setVisible(false);
                FirmwareUpdateMenu.this.contactTextAreascroll.setVisible(false);
                FirmwareUpdateMenu.this.contactSendButton.setVisible(false);
                FirmwareUpdateMenu.this.lblCompleted.setVisible(false);
                FirmwareUpdateMenu.this.lblSentStatus.setVisible(false);
                FirmwareUpdateMenu.this.lblSentErrorStatus.setVisible(false);
                FirmwareUpdateMenu.this.contactSendNewButton.setVisible(false);
                FirmwareUpdateMenu.this.btnHelmetUpgrade.setIcon(new ImageIcon(FirmwareUpdateMenu.this.unHelmetdlimage));
                FirmwareUpdateMenu.this.btnContact.setIcon(new ImageIcon(FirmwareUpdateMenu.this.unContactimage));
                FirmwareUpdateMenu.this.btnTroubleshoot.setIcon(new ImageIcon(FirmwareUpdateMenu.this.troubleimage));
                FirmwareUpdateMenu.this.upgradelblNewLabel.setVisible(false);
                FirmwareUpdateMenu.this.upgradelblNewLabe3.setVisible(false);
                FirmwareUpdateMenu.this.upgradebtnNext.setVisible(false);
                FirmwareUpdateMenu.this.troubleTextAreaMessage.setVisible(true);
                FirmwareUpdateMenu.this.scrollingArea.setVisible(true);
            }
        });
        this.upgradebtnNext.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    FirmwareUpdateMenu.this.checkHexVersion();
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        });
        this.contactSendButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                if (FirmwareUpdateMenu.this.contactNameField.getText().length() > 1 && FirmwareUpdateMenu.this.contactEmailField.getText().length() > 1) {
                    FirmwareUpdateMenu.this.startThreadMail();
                }
            }
        });
        this.btnBack.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                FirmwareUpdateMenu.this.frame.dispose();
                new Connected((int)((FirmwareUpdateMenu)FirmwareUpdateMenu.this).selectedModelbyUser, (Communication)FirmwareUpdateMenu.this.comm, (SerialPort)FirmwareUpdateMenu.this.theraFoundSerialPort).frame.setVisible(true);
            }
        });
    }
}

