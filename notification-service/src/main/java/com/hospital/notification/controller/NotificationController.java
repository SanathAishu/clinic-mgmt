package com.hospital.notification.controller;

import com.hospital.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final EmailService emailService;

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("Notification Service is running");
    }

    @PostMapping("/patient-registered")
    public Mono<ResponseEntity<Map<String, String>>> patientRegistered(@RequestBody Map<String, Object> payload) {
        log.info("Patient registered notification received: {}", payload.get("email"));

        return Mono.fromRunnable(() -> {
            String email = (String) payload.get("email");
            String name = (String) payload.get("name");
            if (email != null && name != null) {
                emailService.sendWelcomeEmail(email, name);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then(Mono.just(ResponseEntity.ok(Map.of("status", "success", "message", "Patient registration notification sent"))));
    }

    @PostMapping("/patient-updated")
    public Mono<ResponseEntity<Map<String, String>>> patientUpdated(@RequestBody Map<String, Object> payload) {
        log.info("Patient updated notification received: {}", payload.get("email"));
        return Mono.just(ResponseEntity.ok(Map.of("status", "success", "message", "Patient update notification received")));
    }

    @PostMapping("/patient-deleted")
    public Mono<ResponseEntity<Map<String, String>>> patientDeleted(@RequestBody Object payload) {
        log.info("Patient deleted notification received");
        return Mono.just(ResponseEntity.ok(Map.of("status", "success", "message", "Patient deletion notification received")));
    }

    @PostMapping("/doctor-registered")
    public Mono<ResponseEntity<Map<String, String>>> doctorRegistered(@RequestBody Map<String, Object> payload) {
        log.info("Doctor registered notification received: {}", payload.get("email"));
        return Mono.just(ResponseEntity.ok(Map.of("status", "success", "message", "Doctor registration notification received")));
    }

    @PostMapping("/doctor-updated")
    public Mono<ResponseEntity<Map<String, String>>> doctorUpdated(@RequestBody Map<String, Object> payload) {
        log.info("Doctor updated notification received: {}", payload.get("email"));
        return Mono.just(ResponseEntity.ok(Map.of("status", "success", "message", "Doctor update notification received")));
    }

    @PostMapping("/doctor-deleted")
    public Mono<ResponseEntity<Map<String, String>>> doctorDeleted(@RequestBody Object payload) {
        log.info("Doctor deleted notification received");
        return Mono.just(ResponseEntity.ok(Map.of("status", "success", "message", "Doctor deletion notification received")));
    }

    @PostMapping("/appointment-created")
    public Mono<ResponseEntity<Map<String, String>>> appointmentCreated(@RequestBody Map<String, Object> payload) {
        log.info("Appointment created notification received for patient: {}", payload.get("patientEmail"));

        return Mono.fromRunnable(() -> {
            String patientEmail = (String) payload.get("patientEmail");
            String patientName = (String) payload.get("patientName");
            String doctorName = (String) payload.get("doctorName");
            String appointmentDateStr = (String) payload.get("appointmentDate");

            if (patientEmail != null && patientName != null && doctorName != null && appointmentDateStr != null) {
                LocalDateTime appointmentDate = LocalDateTime.parse(appointmentDateStr);
                emailService.sendAppointmentConfirmation(patientEmail, patientName, doctorName, appointmentDate);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then(Mono.just(ResponseEntity.ok(Map.of("status", "success", "message", "Appointment creation notification sent"))));
    }

    @PostMapping("/appointment-updated")
    public Mono<ResponseEntity<Map<String, String>>> appointmentUpdated(@RequestBody Map<String, Object> payload) {
        log.info("Appointment updated notification received for patient: {}", payload.get("patientEmail"));
        return Mono.just(ResponseEntity.ok(Map.of("status", "success", "message", "Appointment update notification received")));
    }

    @PostMapping("/appointment-cancelled")
    public Mono<ResponseEntity<Map<String, String>>> appointmentCancelled(@RequestBody Map<String, Object> payload) {
        log.info("Appointment cancelled notification received for patient: {}", payload.get("patientEmail"));
        return Mono.just(ResponseEntity.ok(Map.of("status", "success", "message", "Appointment cancellation notification received")));
    }

    @PostMapping("/prescription-created")
    public Mono<ResponseEntity<Map<String, String>>> prescriptionCreated(@RequestBody Map<String, Object> payload) {
        log.info("Prescription created notification received");

        return Mono.fromRunnable(() -> {
            String patientEmail = (String) payload.get("patientEmail");
            String patientName = (String) payload.get("patientName");
            String medications = (String) payload.get("medications");

            if (patientEmail != null && patientName != null) {
                emailService.sendPrescriptionNotification(patientEmail, patientName, medications != null ? medications : "See prescription for details");
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then(Mono.just(ResponseEntity.ok(Map.of("status", "success", "message", "Prescription notification sent"))));
    }

    @PostMapping("/room-booked")
    public Mono<ResponseEntity<Map<String, String>>> roomBooked(@RequestBody Map<String, Object> payload) {
        log.info("Room booked notification received");
        return Mono.just(ResponseEntity.ok(Map.of("status", "success", "message", "Room booking notification received")));
    }

    @PostMapping("/test-email")
    public Mono<ResponseEntity<Map<String, String>>> testEmail(
            @RequestParam String to,
            @RequestParam(defaultValue = "Test Email") String subject,
            @RequestParam(defaultValue = "This is a test email from the Notification Service.") String body) {

        log.info("Sending test email to: {}", to);

        return Mono.fromCallable(() -> {
            Map<String, Object> variables = Map.of("message", body);
            emailService.sendHtmlEmail(to, subject, "test-email", variables);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Test email sent successfully to " + to
            ));
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> {
            log.error("Failed to send test email to: {}", to, e);
            return Mono.just(ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to send email: " + e.getMessage()
            )));
        });
    }
}
