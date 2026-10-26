package com.daoukiwoom.roomflow.Service.Impl;

import com.daoukiwoom.roomflow.Config.AppClock;
import com.daoukiwoom.roomflow.DTO.Request.CancelBookingRequest;
import com.daoukiwoom.roomflow.DTO.Request.CreateBookingRequest;
import com.daoukiwoom.roomflow.DTO.Response.BookingResponse;
import com.daoukiwoom.roomflow.Entity.Booking;
import com.daoukiwoom.roomflow.Entity.BookingStatus;
import com.daoukiwoom.roomflow.Entity.Room;
import com.daoukiwoom.roomflow.Entity.RoomStatus;
import com.daoukiwoom.roomflow.Entity.User;
import com.daoukiwoom.roomflow.Exception.AppException;
import com.daoukiwoom.roomflow.Exception.ErrorCode;
import com.daoukiwoom.roomflow.Mapper.BookingMapper;
import com.daoukiwoom.roomflow.Repository.IBookingRepository;
import com.daoukiwoom.roomflow.Repository.IRoomRepository;
import com.daoukiwoom.roomflow.Repository.IUserRepository;
import com.daoukiwoom.roomflow.Security.AuthUser;
import com.daoukiwoom.roomflow.Service.BookingRules;
import com.daoukiwoom.roomflow.Service.Interface.BookingService;
import com.daoukiwoom.roomflow.Service.Interface.HoldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private IBookingRepository bookingRepository;

    @Autowired
    private IRoomRepository roomRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private HoldService holdService;

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private AppClock appClock;

    @Override
    @Transactional
    public BookingResponse create(CreateBookingRequest request, AuthUser actor) {
        BookingRules.validate(request.getStartAt(), request.getEndAt(), appClock.now(), appClock.zone());
        User organizer = userRepository.findById(actor.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Room room = roomRepository.findByIdForUpdate(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        assertRoomBookable(room, request.getAttendeeCount());
        holdService.assertNotHeldByOther(room.getId(), request.getStartAt(), request.getEndAt(), actor.getId());
        if (bookingRepository.existsConfirmedOverlap(room.getId(), request.getStartAt(), request.getEndAt())) {
            throw new AppException(ErrorCode.ROOM_ALREADY_BOOKED);
        }
        Instant now = Instant.now();
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .room(room)
                .organizer(organizer)
                .title(request.getTitle().trim())
                .purpose(trimToNull(request.getPurpose()))
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .attendeeCount(request.getAttendeeCount())
                .status(BookingStatus.CONFIRMED)
                .attendeeIds(sanitizeAttendees(request.getAttendeeIds(), organizer.getId()))
                .createdAt(now)
                .updatedAt(now)
                .build();
        BookingResponse response = bookingMapper.toBookingResponse(bookingRepository.save(booking));
        holdService.consumeOwn(actor.getId(), room.getId(), request.getStartAt(), request.getEndAt());
        return response;
    }

    @Override
    @Transactional
    public BookingResponse update(UUID id, CreateBookingRequest request, AuthUser actor) {
        Booking booking = require(id);
        assertCanManage(booking, actor);
        assertNotStarted(booking);
        BookingRules.validate(request.getStartAt(), request.getEndAt(), appClock.now(), appClock.zone());
        Room room = roomRepository.findByIdForUpdate(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        assertRoomBookable(room, request.getAttendeeCount());
        holdService.assertNotHeldByOther(room.getId(), request.getStartAt(), request.getEndAt(), actor.getId());
        if (bookingRepository.existsConfirmedOverlapExcluding(room.getId(), request.getStartAt(), request.getEndAt(), booking.getId())) {
            throw new AppException(ErrorCode.ROOM_ALREADY_BOOKED);
        }
        booking.setRoom(room);
        booking.setTitle(request.getTitle().trim());
        booking.setPurpose(trimToNull(request.getPurpose()));
        booking.setStartAt(request.getStartAt());
        booking.setEndAt(request.getEndAt());
        booking.setAttendeeCount(request.getAttendeeCount());
        booking.setAttendeeIds(sanitizeAttendees(request.getAttendeeIds(), booking.getOrganizer().getId()));
        booking.setUpdatedAt(Instant.now());
        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse cancel(UUID id, CancelBookingRequest request, AuthUser actor) {
        Booking booking = require(id);
        assertCanManage(booking, actor);
        if (!booking.isConfirmed()) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }
        assertNotStarted(booking);
        booking.cancel(trimToNull(request == null ? null : request.getReason()), appClock.now());
        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse get(UUID id, AuthUser actor) {
        Booking booking = require(id);
        if (!actor.isAdmin() && !booking.getOrganizer().getId().equals(actor.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> listMine(AuthUser actor) {
        return bookingRepository.findByOrganizerId(actor.getId()).stream()
                .map(bookingMapper::toBookingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> listRange(Instant from, Instant to) {
        return bookingRepository.findOverlappingRange(from, to).stream()
                .map(bookingMapper::toBookingResponse)
                .toList();
    }

    private Booking require(UUID id) {
        return bookingRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
    }

    private void assertRoomBookable(Room room, int attendeeCount) {
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new AppException(ErrorCode.ROOM_INACTIVE);
        }
        if (attendeeCount > room.getCapacity()) {
            throw new AppException(
                    ErrorCode.ROOM_CAPACITY_EXCEEDED,
                    "Phòng " + room.getName() + " chỉ chứa " + room.getCapacity()
                            + " người, bạn đang đặt cho " + attendeeCount + " người."
            );
        }
    }

    private void assertCanManage(Booking booking, AuthUser actor) {
        if (!actor.isAdmin() && !booking.getOrganizer().getId().equals(actor.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private void assertNotStarted(Booking booking) {
        if (!booking.getStartAt().isAfter(appClock.now())) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_STARTED);
        }
    }

    private Set<UUID> sanitizeAttendees(Set<UUID> attendeeIds, UUID organizerId) {
        Set<UUID> sanitized = attendeeIds == null ? new HashSet<>() : new HashSet<>(attendeeIds);
        sanitized.add(organizerId);
        return sanitized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
