package com.daoukiwoom.roomflow.Repository;

import com.daoukiwoom.roomflow.Entity.Room;
import com.daoukiwoom.roomflow.Entity.RoomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface IRoomRepository extends JpaRepository<Room, UUID> {
    Optional<Room> findByCodeIgnoreCase(String code);

    Page<Room> findAllByOrderByFloorAscCapacityAscCodeAsc(Pageable pageable);

    Page<Room> findByStatusOrderByFloorAscCapacityAscCodeAsc(RoomStatus status, Pageable pageable);

    Page<Room> findByStatusAndCapacityGreaterThanEqualOrderByCapacityAscCodeAsc(
            RoomStatus status,
            int capacity,
            Pageable pageable
    );

    long countByStatus(RoomStatus status);

    @Query("select min(r.capacity) from Room r where r.status = :status")
    Integer minCapacityByStatus(@Param("status") RoomStatus status);

    @Query("select max(r.capacity) from Room r where r.status = :status")
    Integer maxCapacityByStatus(@Param("status") RoomStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") UUID id);
}
