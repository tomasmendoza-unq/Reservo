package com.reservo.service.impl;

import com.reservo.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Profile("!test")
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender emailSender;

    private String appEmail = "reservoapptmmj@gmail.com";

    private final List<String> KNOWN_EMAILS = Arrays.asList(appEmail, "joelhc.98@gmail.com", "joel.c.98@hotmail.com", "matiasfd.deo@gmail.com", "rockito10.mfd@gmail.com", "tm1453766@gmail.com", "ma.nahuel.d@gmail.com", "reservoapptmmj@gmail.com");

    @Override
    public void sendSimpleEmail(String to, String subject, String text) {
        if (!KNOWN_EMAILS.contains(to)) return;

        try {
            sendEmail(to, subject, text);
        } catch (MailSendException e) {
            sendEmail(to,subject, text);
        }

    }

    @Override
    public void sendHTMLEmail(String to, String subject, String html) {
//        if (!KNOWN_EMAILS.contains(to)) return; // para que nos manden a nosotros nada más

        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

        try {
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

        emailSender.send(message);
    }

    private void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(appEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }
}
