package com.daoukiwoom.roomflow.Service.Impl;

import com.daoukiwoom.roomflow.Config.AppClock;
import com.daoukiwoom.roomflow.DTO.Request.CancelBookingRequest;
import com.daoukiwoom.roomflow.DTO.Request.CreateBookingRequest;
import com.daoukiwoom.roomflow.DTO.Response.BookingResponse;
import com.daoukiwoom.roomflow.DTO.Response.CalendarPageResponse;
import com.daoukiwoom.roomflow.DTO.Response.PageResponse;
import com.daoukiwoom.roomflow.DTO.Response.RoomResponse;
import com.daoukiwoom.roomflow.Entity.Booking;
import com.daoukiwoom.roomflow.Entity.BookingStatus;
import com.daoukiwoom.roomflow.Entity.Room;
import com.daoukiwoom.roomflow.Entity.RoomStatus;
import com.daoukiwoom.roomflow.Entity.User;
import com.daoukiwoom.roomflow.Exception.AppException;
import com.daoukiwoom.roomflow.Exception.ErrorCode;
import com.daoukiwoom.roomflow.Mapper.BookingMapper;
import com.daoukiwoom.roomflow.Mapper.RoomMapper;
import com.daoukiwoom.roomflow.Repository.IBookingRepository;
import com.daoukiwoom.roomflow.Repository.IRoomRepository;
import com.daoukiwoom.roomflow.Repository.IUserRepository;
import com.daoukiwoom.roomflow.Security.AuthUser;
import com.daoukiwoom.roomflow.Service.BookingRules;
import com.daoukiwoom.roomflow.Service.Interface.BookingService;
import com.daoukiwoom.roomflow.Service.Interface.HoldService;
import com.daoukiwoom.roomflow.Service.Interface.SlotEventPublisher;
import com.daoukiwoom.roomflow.Service.OffsetLimit;
import com.daoukiwoom.roomflow.Service.PageLimits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
    private RoomMapper roomMapper;

    @Autowired
    private AppClock appClock;

    @Autowired
    private SlotEventPublisher slotEvents;

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
        slotEvents.publish("BOOKED", room.getId(), request.getStartAt(), request.getEndAt());
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
        slotEvents.publish("BOOKED", room.getId(), request.getStartAt(), request.getEndAt());
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
        slotEvents.publish("FREED", booking.getRoom().getId(), booking.getStartAt(), booking.getEndAt());
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
    public PageResponse<BookingResponse> listMine(AuthUser actor, Integer page, Integer size) {
        Page<Booking> bookings = bookingRepository.findByOrganizer_IdOrderByStartAtDesc(
                actor.getId(),
                PageLimits.of(page, size, PageLimits.BOOKINGS)
        );
        return PageResponse.of(bookings.map(bookingMapper::toBookingResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> listRange(Instant from, Instant to, Integer page, Integer size) {
        assertListRange(from, to);
        Page<Booking> bookings = bookingRepository.findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(
                to,
                from,
                PageLimits.of(page, size, PageLimits.BOOKINGS)
        );
        return PageResponse.of(bookings.map(bookingMapper::toBookingResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public CalendarPageResponse calendar(Instant from, Instant to, Integer offset, Integer size) {
        assertListRange(from, to);
        int limit = PageLimits.of(0, size, 20).getPageSize();
        int start = offset == null || offset < 0 ? 0 : offset;
        Page<Room> rooms = roomRepository.findByStatusOrderByFloorAscCapacityAscCodeAsc(
                RoomStatus.ACTIVE,
                new OffsetLimit(start, limit)
        );
        List<UUID> roomIds = rooms.getContent().stream().map(Room::getId).toList();
        List<BookingResponse> bookings = roomIds.isEmpty()
                ? List.of()
                : bookingRepository.findOverlappingRangeForRooms(from, to, roomIds).stream()
                        .map(bookingMapper::toBookingResponse)
                        .toList();
        List<RoomResponse> roomItems = rooms.getContent().stream()
                .map(roomMapper::toRoomResponse)
                .toList();
        return new CalendarPageResponse(PageResponse.of(roomItems, rooms), bookings);
    }

    private void assertListRange(Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new AppException(ErrorCode.INVALID_TIME_RANGE);
        }
        if (Duration.between(from, to).compareTo(Duration.ofDays(PageLimits.RANGE_DAYS)) > 0) {
            throw new AppException(ErrorCode.RANGE_TOO_WIDE);
        }
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
