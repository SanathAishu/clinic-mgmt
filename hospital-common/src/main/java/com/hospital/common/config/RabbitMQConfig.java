package com.hospital.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for all microservices
 */
@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String HOSPITAL_EVENTS_TOPIC_EXCHANGE = "hospital.events.topic";
    public static final String HOSPITAL_EVENTS_DIRECT_EXCHANGE = "hospital.events.direct";
    public static final String HOSPITAL_EVENTS_SAGA_EXCHANGE = "hospital.events.saga";

    // Queue names - Notifications
    public static final String APPOINTMENT_NOTIFICATIONS_QUEUE = "appointment.notifications";
    public static final String PRESCRIPTION_NOTIFICATIONS_QUEUE = "prescription.notifications";
    public static final String FACILITY_NOTIFICATIONS_QUEUE = "facility.notifications";

    // Queue names - Snapshots
    public static final String PATIENT_UPDATES_QUEUE = "patient.updates";
    public static final String DOCTOR_UPDATES_QUEUE = "doctor.updates";

    // Queue names - Saga
    public static final String PATIENT_ADMISSION_REQUEST_QUEUE = "patient.admission.request";
    public static final String ADMISSION_SUCCESS_QUEUE = "admission.success";
    public static final String ADMISSION_FAILED_QUEUE = "admission.failed";

    // Queue names - Cache Invalidation
    public static final String CACHE_INVALIDATION_QUEUE = "cache.invalidation";

    // Routing keys
    public static final String PATIENT_CREATED_KEY = "patient.created";
    public static final String PATIENT_UPDATED_KEY = "patient.updated";
    public static final String PATIENT_DELETED_KEY = "patient.deleted";
    public static final String DOCTOR_CREATED_KEY = "doctor.created";
    public static final String DOCTOR_UPDATED_KEY = "doctor.updated";
    public static final String APPOINTMENT_CREATED_KEY = "appointment.created";
    public static final String APPOINTMENT_CANCELLED_KEY = "appointment.cancelled";
    public static final String MEDICAL_RECORD_CREATED_KEY = "medical.record.created";
    public static final String PRESCRIPTION_CREATED_KEY = "prescription.created";
    public static final String CACHE_INVALIDATE_KEY = "cache.invalidate";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    // Topic Exchange for notifications
    @Bean
    public TopicExchange hospitalEventsTopicExchange() {
        return new TopicExchange(HOSPITAL_EVENTS_TOPIC_EXCHANGE);
    }

    // Direct Exchange for snapshots
    @Bean
    public DirectExchange hospitalEventsDirectExchange() {
        return new DirectExchange(HOSPITAL_EVENTS_DIRECT_EXCHANGE);
    }

    // Direct Exchange for saga events
    @Bean
    public DirectExchange hospitalEventsSagaExchange() {
        return new DirectExchange(HOSPITAL_EVENTS_SAGA_EXCHANGE);
    }

    // Notification Queues
    @Bean
    public Queue appointmentNotificationsQueue() {
        return QueueBuilder.durable(APPOINTMENT_NOTIFICATIONS_QUEUE).build();
    }

    @Bean
    public Queue prescriptionNotificationsQueue() {
        return QueueBuilder.durable(PRESCRIPTION_NOTIFICATIONS_QUEUE).build();
    }

    @Bean
    public Queue facilityNotificationsQueue() {
        return QueueBuilder.durable(FACILITY_NOTIFICATIONS_QUEUE).build();
    }

    // Snapshot Update Queues
    @Bean
    public Queue patientUpdatesQueue() {
        return QueueBuilder.durable(PATIENT_UPDATES_QUEUE).build();
    }

    @Bean
    public Queue doctorUpdatesQueue() {
        return QueueBuilder.durable(DOCTOR_UPDATES_QUEUE).build();
    }

    // Saga Queues
    @Bean
    public Queue patientAdmissionRequestQueue() {
        return QueueBuilder.durable(PATIENT_ADMISSION_REQUEST_QUEUE).build();
    }

    @Bean
    public Queue admissionSuccessQueue() {
        return QueueBuilder.durable(ADMISSION_SUCCESS_QUEUE).build();
    }

    @Bean
    public Queue admissionFailedQueue() {
        return QueueBuilder.durable(ADMISSION_FAILED_QUEUE).build();
    }

    // Cache Invalidation Queue
    @Bean
    public Queue cacheInvalidationQueue() {
        return QueueBuilder.durable(CACHE_INVALIDATION_QUEUE).build();
    }

    // Bindings for Notifications (Topic Exchange)
    @Bean
    public Binding appointmentNotificationsBinding() {
        return BindingBuilder
                .bind(appointmentNotificationsQueue())
                .to(hospitalEventsTopicExchange())
                .with("appointment.*");
    }

    @Bean
    public Binding prescriptionNotificationsBinding() {
        return BindingBuilder
                .bind(prescriptionNotificationsQueue())
                .to(hospitalEventsTopicExchange())
                .with("prescription.*");
    }

    @Bean
    public Binding facilityNotificationsBinding() {
        return BindingBuilder
                .bind(facilityNotificationsQueue())
                .to(hospitalEventsTopicExchange())
                .with("facility.*");
    }

    // Bindings for Snapshots (Direct Exchange)
    @Bean
    public Binding patientUpdatesBinding() {
        return BindingBuilder
                .bind(patientUpdatesQueue())
                .to(hospitalEventsDirectExchange())
                .with(PATIENT_UPDATED_KEY);
    }

    @Bean
    public Binding doctorUpdatesBinding() {
        return BindingBuilder
                .bind(doctorUpdatesQueue())
                .to(hospitalEventsDirectExchange())
                .with(DOCTOR_UPDATED_KEY);
    }

    // Bindings for Cache Invalidation
    @Bean
    public Binding cacheInvalidationBinding() {
        return BindingBuilder
                .bind(cacheInvalidationQueue())
                .to(hospitalEventsDirectExchange())
                .with(CACHE_INVALIDATE_KEY);
    }
}
