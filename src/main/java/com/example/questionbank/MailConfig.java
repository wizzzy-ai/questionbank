package com.example.questionbank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {
    private static final Logger logger = LoggerFactory.getLogger(MailConfig.class);

    @Bean
    public JavaMailSender javaMailSender() {
        logger.info("=== CONFIGURING JAVAMAIL SENDER ===");
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        // Basic SMTP configuration
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(System.getenv("EMAIL_USERNAME") != null ? System.getenv("EMAIL_USERNAME") : "samjosh013@gmail.com");
        mailSender.setPassword(System.getenv("EMAIL_PASSWORD") != null ? System.getenv("EMAIL_PASSWORD") : "ewqe xebl vkyg rgxm");
        
        logger.info("✅ Mail host: {}", mailSender.getHost());
        logger.info("✅ Mail port: {}", mailSender.getPort());
        logger.info("✅ Mail username: {}", mailSender.getUsername());
        logger.info("✅ Mail password: [HIDDEN FOR SECURITY]");
        
        // SMTP properties
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.debug", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        
        logger.info("✅ JavaMail properties configured");
        logger.info("✅ JavaMailSender bean created successfully");
        
        return mailSender;
    }
}
