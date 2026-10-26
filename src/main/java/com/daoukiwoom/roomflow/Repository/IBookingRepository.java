package com.daoukiwoom.roomflow.Repository;

import com.daoukiwoom.roomflow.Entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IBookingRepository extends JpaRepository<Booking, UUID> {

    @Query("""
            select (count(b) > 0)
            from Booking b
            where b.room.id = :roomId
              and b.status = com.daoukiwoom.roomflow.Entity.BookingStatus.CONFIRMED
              and b.startAt < :endAt
              and b.endAt > :startAt
            """)
    boolean existsConfirmedOverlap(
            @Param("roomId") UUID roomId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );

    @Query("""
            select (count(b) > 0)
            from Booking b
            where b.room.id = :roomId
              and b.status = com.daoukiwoom.roomflow.Entity.BookingStatus.CONFIRMED
              and b.id <> :excludeId
              and b.startAt < :endAt
              and b.endAt > :startAt
            """)
    boolean existsConfirmedOverlapExcluding(
            @Param("roomId") UUID roomId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("excludeId") UUID excludeId
    );

    @Query("""
            select b from Booking b
            join fetch b.room
            join fetch b.organizer
            where b.room.id in :roomIds
              and b.startAt < :to
              and b.endAt > :from
            order by b.startAt asc
            """)
    List<Booking> findOverlappingRangeForRooms(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("roomIds") Collection<UUID> roomIds
    );

    Page<Booking> findByOrganizer_IdOrderByStartAtDesc(UUID organizerId, Pageable pageable);

    Page<Booking> findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(
            Instant to,
            Instant from,
            Pageable pageable
    );

    @Query("""
            select (count(b) > 0)
            from Booking b
            where b.room.id = :roomId
              and b.status = com.daoukiwoom.roomflow.Entity.BookingStatus.CONFIRMED
              and b.endAt > :from
            """)
    boolean existsConfirmedFrom(@Param("roomId") UUID roomId, @Param("from") Instant from);

    @Query("""
            select distinct b.room.id
            from Booking b
            where b.status = com.daoukiwoom.roomflow.Entity.BookingStatus.CONFIRMED
              and b.endAt > :from
              and b.room.id in :roomIds
            """)
    List<UUID> findRoomIdsWithConfirmedFrom(
            @Param("from") Instant from,
            @Param("roomIds") Collection<UUID> roomIds
    );

    boolean existsByRoom_Id(UUID roomId);
}
