/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import java.util.Date;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class SMTPConfig {
    private Properties properties;

    public Properties configProperties(String configUserName, String configPassword) {
        this.properties = new Properties();
        this.properties.setProperty("mail.smtp.host", "smtp.gmail.com");
        this.properties.setProperty("mail.smtp.port", "587");
        this.properties.setProperty("mail.user", configUserName);
        this.properties.setProperty("mail.password", configPassword);
        this.properties.setProperty("mail.smtp.starttls.enable", "true");
        this.properties.setProperty("mail.smtp.auth", "true");
        return this.properties;
    }

    public int sendEmail(String name, String emailToAddress, String message) {
        this.properties = this.configProperties("<redacted-mail-account>", "<redacted-mail-password>");
        Authenticator authenticator = null;
        try {
            authenticator = new Authenticator(){

                @Override
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTPConfig.this.properties.getProperty("mail.user"), SMTPConfig.this.properties.getProperty("mail.password"));
                }
            };
        }
        catch (Exception ex) {
            return 1;
        }
        Session session = Session.getInstance(this.properties, authenticator);
        MimeMessage msg = new MimeMessage(session);
        try {
            ((Message)msg).setFrom(new InternetAddress(this.properties.getProperty("mail.user")));
            Address[] toAddresses = new InternetAddress[]{new InternetAddress(emailToAddress)};
            ((Message)msg).setRecipients(Message.RecipientType.TO, toAddresses);
            ((Message)msg).setSentDate(new Date());
            ((Message)msg).setSubject("Theradome Support");
            System.out.println("Internet Address config Done");
        }
        catch (Exception e) {
            return 2;
        }
        MimeMultipart multipart = new MimeMultipart();
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        try {
            messageBodyPart.setContent(message, "text/html");
            ((Multipart)multipart).addBodyPart(messageBodyPart);
            System.out.println("Message Body config Done");
        }
        catch (MessagingException e) {
            return 3;
        }
        try {
            msg.setContent(multipart);
            Transport.send(msg);
            System.out.println("Email send successfully");
        }
        catch (MessagingException e) {
            return 4;
        }
        return 0;
    }
}

