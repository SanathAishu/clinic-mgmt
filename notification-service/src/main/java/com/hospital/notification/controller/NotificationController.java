package com.hospital.notification.controller;

import com.hospital.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for notification endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final EmailService emailService;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is running");
    }

    /**
     * Test email delivery endpoint
     */
    @PostMapping("/test-email")
    public ResponseEntity<Map<String, String>> testEmail(
            @RequestParam String to,
            @RequestParam(defaultValue = "Test Email") String subject,
            @RequestParam(defaultValue = "This is a test email from the Notification Service.") String body) {

        log.info("Sending test email to: {}", to);

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("message", body);
            emailService.sendHtmlEmail(to, subject, "test-email", variables);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Test email sent successfully to " + to
            ));
        } catch (Exception e) {
            log.error("Failed to send test email to: {}", to, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to send email: " + e.getMessage()
            ));
        }
    }
}
