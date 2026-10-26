package com.daoukiwoom.roomflow.Repository;

import com.daoukiwoom.roomflow.Entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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
            where b.startAt < :to
              and b.endAt > :from
            order by b.startAt asc
            """)
    List<Booking> findOverlappingRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select b from Booking b
            join fetch b.room
            join fetch b.organizer
            where b.organizer.id = :organizerId
            order by b.startAt desc
            """)
    List<Booking> findByOrganizerId(@Param("organizerId") UUID organizerId);
}
