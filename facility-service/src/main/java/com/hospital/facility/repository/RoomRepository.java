package com.hospital.facility.repository;

import com.hospital.facility.entity.Room;
import com.hospital.facility.entity.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    Optional<Room> findByRoomNumber(String roomNumber);

    List<Room> findByRoomTypeAndActiveTrue(RoomType roomType);

    List<Room> findByAvailableTrueAndActiveTrue();

    Page<Room> findByActiveTrue(Pageable pageable);

    @Query("SELECT r FROM Room r WHERE r.roomType = :roomType AND r.available = true AND r.active = true AND r.currentOccupancy < r.capacity")
    List<Room> findAvailableRoomsByType(@Param("roomType") RoomType roomType);

    @Query("SELECT r FROM Room r WHERE r.floor = :floor AND r.active = true")
    List<Room> findByFloor(@Param("floor") String floor);

    @Query("SELECT r FROM Room r WHERE r.wing = :wing AND r.active = true")
    List<Room> findByWing(@Param("wing") String wing);

    @Query("SELECT COUNT(r) FROM Room r WHERE r.available = true AND r.active = true")
    long countAvailableRooms();

    @Query("SELECT SUM(r.capacity - r.currentOccupancy) FROM Room r WHERE r.active = true")
    Long countAvailableBeds();
}
