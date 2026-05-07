package com.example.questionbank;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.mail.MailException;

@Service
public class EmailService {
	private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

	private final JavaMailSender mailSender;

	@Value("${app.email.from:noreply@questionbank.local}")
	private String fromEmail;

	@Value("${app.email.enabled:true}")
	private boolean emailEnabled;

	public EmailService(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public void sendVerificationEmail(String toEmail, String fullName, String code) {
		logger.info("=== SENDING VERIFICATION EMAIL ===");
		logger.info("Target email: {}", toEmail);
		logger.info("Full name: {}", fullName);
		logger.info("OTP code: {}", code);
		logger.info("Email enabled: {}", emailEnabled);
		logger.info("From email: {}", fromEmail);
		
		if (!emailEnabled) {
			logger.warn("🚫 EMAIL SENDING IS DISABLED - Verification code for {}: {}", toEmail, code);
			return;
		}

		logger.info("✅ Email sending enabled - proceeding to send verification email");
		String subject = "Your Verification Code - Question Bank";
		String htmlContent = buildVerificationEmail(fullName, code);

		sendHtmlEmail(toEmail, subject, htmlContent);
	}

	@Value("${server.servlet.context-path:}")
	private String contextPath;

	public void sendPasswordResetEmail(String toEmail, String fullName, String token, HttpServletRequest request) {
		if (!emailEnabled) {
			logger.warn("Email sending is disabled. Password reset token for {}: {}", toEmail, token);
			return;
		}

		String subject = "Reset Your Password - Question Bank";
		String resetLink = buildResetUrl(request, token);

		String htmlContent = buildPasswordResetEmail(fullName, resetLink);

		sendHtmlEmail(toEmail, subject, htmlContent);
	}

	private String buildResetUrl(HttpServletRequest request, String token) {
		String scheme = request.getScheme();
		String serverName = request.getServerName();
		int serverPort = request.getServerPort();
		String contextPath = request.getContextPath();
		
		StringBuilder url = new StringBuilder();
		url.append(scheme).append("://").append(serverName);
		
		// Only include port if it's not the default port for the scheme
		if (("http".equals(scheme) && serverPort != 80) || ("https".equals(scheme) && serverPort != 443)) {
			url.append(":").append(serverPort);
		}
		
		url.append(contextPath).append("/reset-password?token=").append(token);
		return url.toString();
	}

	private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
		logger.info("=== PREPARING EMAIL ===");
		logger.info("To: {}", toEmail);
		logger.info("From: {}", fromEmail);
		logger.info("Subject: {}", subject);
		logger.info("Content length: {} chars", htmlContent.length());
		
		try {
			logger.info("🔧 Creating MimeMessage...");
			MimeMessage message = mailSender.createMimeMessage();
			logger.info("✅ MimeMessage created successfully");
			
			logger.info("🔧 Creating MimeMessageHelper...");
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			logger.info("✅ MimeMessageHelper created successfully");

			logger.info("🔧 Setting email headers...");
			helper.setFrom(fromEmail, "Question Bank");
			helper.setTo(toEmail);
			helper.setSubject(subject);
			helper.setText(htmlContent, true);
			logger.info("✅ Email headers set successfully");

			logger.info("🚀 SENDING EMAIL NOW...");
			try {
				mailSender.send(message);
				logger.info("🎉 EMAIL SENT SUCCESSFULLY to: {}", toEmail);
			} catch (Exception e) {
				logger.error("❌ EMAIL FAILED - Failed to send email to {}: {}", toEmail, e.getMessage(), e);
				throw new RuntimeException("Failed to send email", e);
			}
			
		} catch (MessagingException e) {
			logger.error("❌ MESSAGING EXCEPTION - Failed to prepare email for {}: {}", toEmail, e.getMessage());
			logger.error("Full messaging exception:", e);
			throw new RuntimeException("Failed to send email", e);
		} catch (MailException e) {
			logger.error("❌ MAIL EXCEPTION - Failed to prepare email for {}: {}", toEmail, e.getMessage());
			logger.error("Full mail exception:", e);
			throw new RuntimeException("Failed to send email", e);
		} catch (Exception e) {
			logger.error("❌ UNEXPECTED EXCEPTION - Failed to prepare email for {}: {}", toEmail, e.getMessage());
			logger.error("Full unexpected exception:", e);
			throw new RuntimeException("Failed to send email", e);
		}
	}

	private String buildVerificationEmail(String fullName, String code) {
		return """
			<!DOCTYPE html>
			<html>
			<head>
				<style>
					body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
					.container { max-width: 600px; margin: 0 auto; padding: 20px; }
					.header { background: linear-gradient(135deg, #F97316 0%, #EA580C 100%); padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
					.header h1 { color: white; margin: 0; font-size: 24px; }
					.content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
					.code-box { background: #FFF7ED; border: 3px dashed #F97316; padding: 30px; text-align: center; margin: 25px 0; border-radius: 12px; }
					.code { font-size: 42px; font-weight: bold; color: #EA580C; letter-spacing: 12px; font-family: 'Courier New', monospace; }
					.code-label { margin: 0 0 15px 0; color: #666; font-size: 14px; text-transform: uppercase; letter-spacing: 1px; }
					.expiry { text-align: center; color: #666; font-size: 14px; margin-top: 20px; }
					.footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
					.ignore { background: #F3F4F6; padding: 15px; border-radius: 8px; margin-top: 25px; font-size: 13px; color: #666; }
				</style>
			</head>
			<body>
				<div class="container">
					<div class="header">
						<h1>Question Bank</h1>
					</div>
					<div class="content">
						<h2>Hello ${fullName},</h2>
						<p>Thank you for registering with Question Bank! To complete your registration, please enter the verification code below on our website.</p>
						
						<div class="code-box">
							<p class="code-label">Your Verification Code</p>
							<div class="code">${code}</div>
						</div>
						
						<p class="expiry">This code expires in 10 minutes.</p>
						
						<div class="ignore">
							<strong>Didn't create this account?</strong> You can safely ignore this email.
						</div>
					</div>
					<div class="footer">
						<p>&copy; 2026 Question Bank. All rights reserved.</p>
					</div>
				</div>
			</body>
			</html>
			"""
			.replace("${fullName}", fullName != null ? fullName : "User")
			.replace("${code}", code != null ? code : "000000");
	}

	private String buildPasswordResetEmail(String fullName, String resetLink) {
		return """
			<!DOCTYPE html>
			<html>
			<head>
				<style>
					body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
					.container { max-width: 600px; margin: 0 auto; padding: 20px; }
					.header { background: linear-gradient(135deg, #F97316 0%, #EA580C 100%); padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
					.header h1 { color: white; margin: 0; font-size: 24px; }
					.content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
					.button { display: inline-block; background: #F97316; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
					.footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
					.warning { background: #FEF3C7; border-left: 4px solid #F59E0B; padding: 10px; margin: 15px 0; }
				</style>
			</head>
			<body>
				<div class="container">
					<div class="header">
						<h1>Question Bank</h1>
					</div>
					<div class="content">
						<h2>Hello ${fullName},</h2>
						<p>We received a request to reset your password.</p>
						<p style="text-align: center;">
							<a href="${resetLink}" class="button">Reset Password</a>
						</p>
						<div class="warning">
							<strong>Important:</strong> This link expires in 1 hour and can only be used once.
						</div>
						<p>If you didn't request a password reset, you can safely ignore this email. Your password will remain unchanged.</p>
					</div>
					<div class="footer">
						<p>&copy; 2026 Question Bank. All rights reserved.</p>
					</div>
				</div>
			</body>
			</html>
			"""
			.replace("${fullName}", fullName != null ? fullName : "User")
			.replace("${resetLink}", resetLink != null ? resetLink : "#");
	}
}
