package com.hospital.auth.repository;

import com.hospital.auth.entity.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * User repository for R2DBC database operations
 */
@Repository
public interface UserRepository extends R2dbcRepository<User, UUID> {

    Mono<User> findByEmail(String email);

    Mono<Boolean> existsByEmail(String email);
}
