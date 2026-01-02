package com.hospital.audit.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQListenerConfig {

    public static final String AUDIT_EXCHANGE = "hospital.audit.topic";

    // Queue names for audit
    public static final String AUTH_AUDIT_QUEUE = "audit.auth.queue";
    public static final String PATIENT_AUDIT_QUEUE = "audit.patient.queue";
    public static final String PATIENT_UPDATE_AUDIT_QUEUE = "audit.patient.update.queue";
    public static final String PATIENT_DELETE_AUDIT_QUEUE = "audit.patient.delete.queue";
    public static final String APPOINTMENT_AUDIT_QUEUE = "audit.appointment.queue";
    public static final String APPOINTMENT_CANCEL_AUDIT_QUEUE = "audit.appointment.cancel.queue";
    public static final String MEDICAL_RECORD_AUDIT_QUEUE = "audit.medical.record.queue";
    public static final String PRESCRIPTION_AUDIT_QUEUE = "audit.prescription.queue";
    public static final String FACILITY_AUDIT_QUEUE = "audit.facility.queue";
    public static final String DISCHARGE_AUDIT_QUEUE = "audit.discharge.queue";

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public TopicExchange auditExchange() {
        return new TopicExchange(AUDIT_EXCHANGE);
    }

    // Auth audit queue
    @Bean
    public Queue authAuditQueue() {
        return QueueBuilder.durable(AUTH_AUDIT_QUEUE).build();
    }

    @Bean
    public Binding authAuditBinding(Queue authAuditQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(authAuditQueue).to(auditExchange).with("auth.#");
    }

    // Patient audit queues
    @Bean
    public Queue patientAuditQueue() {
        return QueueBuilder.durable(PATIENT_AUDIT_QUEUE).build();
    }

    @Bean
    public Binding patientAuditBinding(Queue patientAuditQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(patientAuditQueue).to(auditExchange).with("patient.created");
    }

    @Bean
    public Queue patientUpdateAuditQueue() {
        return QueueBuilder.durable(PATIENT_UPDATE_AUDIT_QUEUE).build();
    }

    @Bean
    public Binding patientUpdateAuditBinding(Queue patientUpdateAuditQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(patientUpdateAuditQueue).to(auditExchange).with("patient.updated");
    }

    @Bean
    public Queue patientDeleteAuditQueue() {
        return QueueBuilder.durable(PATIENT_DELETE_AUDIT_QUEUE).build();
    }

    @Bean
    public Binding patientDeleteAuditBinding(Queue patientDeleteAuditQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(patientDeleteAuditQueue).to(auditExchange).with("patient.deleted");
    }

    // Appointment audit queues
    @Bean
    public Queue appointmentAuditQueue() {
        return QueueBuilder.durable(APPOINTMENT_AUDIT_QUEUE).build();
    }

    @Bean
    public Binding appointmentAuditBinding(Queue appointmentAuditQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(appointmentAuditQueue).to(auditExchange).with("appointment.created");
    }

    @Bean
    public Queue appointmentCancelAuditQueue() {
        return QueueBuilder.durable(APPOINTMENT_CANCEL_AUDIT_QUEUE).build();
    }

    @Bean
    public Binding appointmentCancelAuditBinding(Queue appointmentCancelAuditQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(appointmentCancelAuditQueue).to(auditExchange).with("appointment.cancelled");
    }

    // Medical record audit queues
    @Bean
    public Queue medicalRecordAuditQueue() {
        return QueueBuilder.durable(MEDICAL_RECORD_AUDIT_QUEUE).build();
    }

    @Bean
    public Binding medicalRecordAuditBinding(Queue medicalRecordAuditQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(medicalRecordAuditQueue).to(auditExchange).with("medical.record.created");
    }

    @Bean
    public Queue prescriptionAuditQueue() {
        return QueueBuilder.durable(PRESCRIPTION_AUDIT_QUEUE).build();
    }

    @Bean
    public Binding prescriptionAuditBinding(Queue prescriptionAuditQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(prescriptionAuditQueue).to(auditExchange).with("prescription.created");
    }

    // Facility audit queues
    @Bean
    public Queue facilityAuditQueue() {
        return QueueBuilder.durable(FACILITY_AUDIT_QUEUE).build();
    }

    @Bean
    public Binding facilityAuditBinding(Queue facilityAuditQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(facilityAuditQueue).to(auditExchange).with("facility.admitted");
    }

    @Bean
    public Queue dischargeAuditQueue() {
        return QueueBuilder.durable(DISCHARGE_AUDIT_QUEUE).build();
    }

    @Bean
    public Binding dischargeAuditBinding(Queue dischargeAuditQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(dischargeAuditQueue).to(auditExchange).with("facility.discharged");
    }
}
