package com.hospital.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");

    public void sendAppointmentConfirmation(String to, String patientName, String doctorName, LocalDateTime appointmentTime) {
        log.info("Sending appointment confirmation email to: {}", to);

        Map<String, Object> variables = new HashMap<>();
        variables.put("patientName", patientName);
        variables.put("doctorName", doctorName);
        variables.put("appointmentTime", appointmentTime.format(DATE_TIME_FORMATTER));

        sendHtmlEmail(to, "Appointment Confirmation", "appointment-confirmation", variables);
    }

    public void sendAppointmentReminder(String to, String patientName, String doctorName, LocalDateTime appointmentTime) {
        log.info("Sending appointment reminder email to: {}", to);

        Map<String, Object> variables = new HashMap<>();
        variables.put("patientName", patientName);
        variables.put("doctorName", doctorName);
        variables.put("appointmentTime", appointmentTime.format(DATE_TIME_FORMATTER));

        sendHtmlEmail(to, "Appointment Reminder", "appointment-reminder", variables);
    }

    public void sendWelcomeEmail(String to, String patientName) {
        log.info("Sending welcome email to: {}", to);

        Map<String, Object> variables = new HashMap<>();
        variables.put("patientName", patientName);

        sendHtmlEmail(to, "Welcome to Our Hospital", "welcome", variables);
    }

    public void sendPrescriptionNotification(String to, String patientName, String medications) {
        log.info("Sending prescription notification email to: {}", to);

        Map<String, Object> variables = new HashMap<>();
        variables.put("patientName", patientName);
        variables.put("medications", medications);

        sendHtmlEmail(to, "New Prescription Issued", "prescription-notification", variables);
    }

    public void sendAdmissionNotification(String to, String patientName, String roomNumber, LocalDate admissionDate) {
        log.info("Sending admission notification email to: {}", to);

        Map<String, Object> variables = new HashMap<>();
        variables.put("patientName", patientName);
        variables.put("roomNumber", roomNumber);
        variables.put("admissionDate", admissionDate.format(DATE_FORMATTER));

        sendHtmlEmail(to, "Hospital Admission Confirmation", "admission-notification", variables);
    }

    public void sendDischargeNotification(String to, String patientName, String roomNumber, LocalDate dischargeDate) {
        log.info("Sending discharge notification email to: {}", to);

        Map<String, Object> variables = new HashMap<>();
        variables.put("patientName", patientName);
        variables.put("roomNumber", roomNumber);
        variables.put("dischargeDate", dischargeDate.format(DATE_FORMATTER));

        sendHtmlEmail(to, "Hospital Discharge Summary", "discharge-notification", variables);
    }

    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {} with subject: {}", to, subject);

        } catch (MessagingException e) {
            log.error("Failed to send email to: {} with subject: {}. Error: {}", to, subject, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while sending email to: {}. Error: {}", to, e.getMessage());
        }
    }
}
