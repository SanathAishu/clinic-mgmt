package com.hospital.facility.event;

import com.hospital.common.config.RabbitMQConfig;
import com.hospital.common.events.PatientAdmittedEvent;
import com.hospital.common.events.PatientDischargedEvent;
import com.hospital.facility.entity.Room;
import com.hospital.facility.entity.RoomBooking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FacilityEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPatientAdmitted(RoomBooking booking, Room room) {
        PatientAdmittedEvent event = PatientAdmittedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .eventType("PatientAdmitted")
                .bookingId(booking.getId())
                .patientId(booking.getPatientId())
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .admissionDate(booking.getAdmissionDate())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.HOSPITAL_EVENTS_TOPIC_EXCHANGE,
                RabbitMQConfig.PATIENT_ADMITTED_KEY,
                event
        );

        log.info("Published PatientAdmittedEvent for booking ID: {}", booking.getId());
    }

    public void publishPatientDischarged(RoomBooking booking, Room room) {
        PatientDischargedEvent event = PatientDischargedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .eventType("PatientDischarged")
                .bookingId(booking.getId())
                .patientId(booking.getPatientId())
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .dischargeDate(booking.getDischargeDate())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.HOSPITAL_EVENTS_TOPIC_EXCHANGE,
                RabbitMQConfig.PATIENT_DISCHARGED_KEY,
                event
        );

        log.info("Published PatientDischargedEvent for booking ID: {}", booking.getId());
    }
}
