package com.hospital.audit.service;

import com.hospital.audit.dto.AuditSearchCriteria;
import com.hospital.audit.entity.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AuditLogSpecification {

    public static Specification<AuditLog> withCriteria(AuditSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getCategory() != null) {
                predicates.add(cb.equal(root.get("category"), criteria.getCategory()));
            }

            if (criteria.getAction() != null) {
                predicates.add(cb.equal(root.get("action"), criteria.getAction()));
            }

            if (criteria.getServiceName() != null && !criteria.getServiceName().isEmpty()) {
                predicates.add(cb.equal(root.get("serviceName"), criteria.getServiceName()));
            }

            if (criteria.getEntityType() != null && !criteria.getEntityType().isEmpty()) {
                predicates.add(cb.equal(root.get("entityType"), criteria.getEntityType()));
            }

            if (criteria.getEntityId() != null && !criteria.getEntityId().isEmpty()) {
                predicates.add(cb.equal(root.get("entityId"), criteria.getEntityId()));
            }

            if (criteria.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), criteria.getUserId()));
            }

            if (criteria.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), criteria.getStartDate()));
            }

            if (criteria.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), criteria.getEndDate()));
            }

            if (criteria.getSuccess() != null) {
                predicates.add(cb.equal(root.get("success"), criteria.getSuccess()));
            }

            query.orderBy(cb.desc(root.get("timestamp")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
