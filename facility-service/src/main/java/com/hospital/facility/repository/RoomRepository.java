package com.hospital.facility.repository;

import com.hospital.facility.entity.Room;
import com.hospital.facility.entity.RoomType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface RoomRepository extends R2dbcRepository<Room, UUID> {

    Mono<Room> findByRoomNumber(String roomNumber);

    Flux<Room> findByRoomTypeAndActiveTrue(RoomType roomType);

    Flux<Room> findByAvailableTrueAndActiveTrue();

    Flux<Room> findByActiveTrue();

    @Query("SELECT * FROM rooms WHERE room_type = :roomType AND available = true AND active = true AND current_occupancy < capacity")
    Flux<Room> findAvailableRoomsByType(@Param("roomType") RoomType roomType);

    @Query("SELECT * FROM rooms WHERE floor = :floor AND active = true")
    Flux<Room> findByFloor(@Param("floor") String floor);

    @Query("SELECT * FROM rooms WHERE wing = :wing AND active = true")
    Flux<Room> findByWing(@Param("wing") String wing);

    @Query("SELECT COUNT(*) FROM rooms WHERE available = true AND active = true")
    Mono<Long> countAvailableRooms();

    @Query("SELECT SUM(capacity - current_occupancy) FROM rooms WHERE active = true")
    Mono<Long> countAvailableBeds();
}
