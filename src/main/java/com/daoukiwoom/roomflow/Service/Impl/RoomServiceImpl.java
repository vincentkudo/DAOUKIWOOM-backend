package com.daoukiwoom.roomflow.Service.Impl;

import com.daoukiwoom.roomflow.Config.AppClock;
import com.daoukiwoom.roomflow.DTO.Request.RoomRequest;
import com.daoukiwoom.roomflow.DTO.Response.HoldRecord;
import com.daoukiwoom.roomflow.DTO.Response.PageResponse;
import com.daoukiwoom.roomflow.DTO.Response.RoomResponse;
import com.daoukiwoom.roomflow.DTO.Response.RoomSuggestion;
import com.daoukiwoom.roomflow.DTO.Response.RoomSuggestionResult;
import com.daoukiwoom.roomflow.DTO.Response.RoomSummaryResponse;
import com.daoukiwoom.roomflow.Entity.Amenity;
import com.daoukiwoom.roomflow.Entity.FitLevel;
import com.daoukiwoom.roomflow.Entity.Room;
import com.daoukiwoom.roomflow.Entity.RoomStatus;
import com.daoukiwoom.roomflow.Entity.SlotState;
import com.daoukiwoom.roomflow.Exception.AppException;
import com.daoukiwoom.roomflow.Exception.ErrorCode;
import com.daoukiwoom.roomflow.Mapper.RoomMapper;
import com.daoukiwoom.roomflow.Repository.IBookingRepository;
import com.daoukiwoom.roomflow.Repository.IRoomRepository;
import com.daoukiwoom.roomflow.Security.AuthUser;
import com.daoukiwoom.roomflow.Service.BookingRules;
import com.daoukiwoom.roomflow.Service.Interface.HoldService;
import com.daoukiwoom.roomflow.Service.Interface.RoomService;
import com.daoukiwoom.roomflow.Service.PageLimits;
import com.daoukiwoom.roomflow.Service.RoomFitScorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RoomServiceImpl implements RoomService {

    @Autowired
    private IRoomRepository roomRepository;

    @Autowired
    private IBookingRepository bookingRepository;

    @Autowired
    private HoldService holdService;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private AppClock appClock;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> list(Integer page, Integer size) {
        Page<Room> rooms = roomRepository.findAllByOrderByFloorAscCapacityAscCodeAsc(
                PageLimits.of(page, size, PageLimits.ROOMS)
        );
        List<UUID> ids = rooms.getContent().stream().map(Room::getId).toList();
        Set<UUID> lockedIds = ids.isEmpty()
                ? Set.of()
                : new HashSet<>(bookingRepository.findRoomIdsWithConfirmedFrom(Instant.now(), ids));
        return PageResponse.of(
                rooms.getContent().stream().map(room -> toResponse(room, lockedIds.contains(room.getId()))).toList(),
                rooms
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RoomSummaryResponse summary() {
        return new RoomSummaryResponse(
                roomRepository.countByStatus(RoomStatus.ACTIVE),
                roomRepository.minCapacityByStatus(RoomStatus.ACTIVE),
                roomRepository.maxCapacityByStatus(RoomStatus.ACTIVE)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse get(UUID id) {
        Room room = require(id);
        return toResponse(room, bookingRepository.existsConfirmedFrom(id, Instant.now()));
    }

    @Override
    @Transactional
    public RoomResponse create(RoomRequest request) {
        String code = request.getCode().trim().toUpperCase();
        request.setCode(code);
        if (roomRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new AppException(ErrorCode.ROOM_CODE_EXISTS);
        }
        Instant now = Instant.now();
        Room room = roomMapper.toRoom(request);
        room.setId(UUID.randomUUID());
        room.setCreatedAt(now);
        room.setUpdatedAt(now);
        if (room.getAmenities() == null) {
            room.setAmenities(EnumSet.noneOf(Amenity.class));
        }
        return toResponse(roomRepository.save(room), false);
    }

    @Override
    @Transactional
    public RoomResponse update(UUID id, RoomRequest request) {
        Room room = require(id);
        if (bookingRepository.existsConfirmedFrom(id, Instant.now())) {
            throw new AppException(ErrorCode.ROOM_HAS_ACTIVE_BOOKINGS);
        }
        String code = request.getCode().trim().toUpperCase();
        request.setCode(code);
        roomRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.ROOM_CODE_EXISTS);
                });
        roomMapper.updateRoom(request, room);
        room.setUpdatedAt(Instant.now());
        return toResponse(room, false);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Room room = require(id);
        if (bookingRepository.existsConfirmedFrom(id, Instant.now())
                || bookingRepository.existsByRoom_Id(id)) {
            throw new AppException(ErrorCode.ROOM_HAS_ACTIVE_BOOKINGS);
        }
        roomRepository.delete(room);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomSuggestionResult suggest(
            int attendeeCount,
            Instant startAt,
            Instant endAt,
            Set<Amenity> requiredAmenities,
            AuthUser actor,
            Integer page,
            Integer size
    ) {
        if (attendeeCount < 1) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Số người phải từ 1 trở lên.");
        }
        if (startAt != null || endAt != null) {
            if (startAt == null || endAt == null) {
                throw new AppException(ErrorCode.INVALID_TIME_RANGE, "Cần đủ thời điểm bắt đầu và kết thúc.");
            }
            BookingRules.validate(startAt, endAt, appClock.now(), appClock.zone());
        }

        Set<Amenity> amenities = requiredAmenities == null ? EnumSet.noneOf(Amenity.class) : requiredAmenities;
        Page<Room> roomPage = roomRepository.findByStatusAndCapacityGreaterThanEqualOrderByCapacityAscCodeAsc(
                RoomStatus.ACTIVE,
                attendeeCount,
                PageLimits.of(page, size, PageLimits.SUGGESTIONS)
        );
        List<Room> pageRooms = roomPage.getContent();
        List<Room> exact = amenities.isEmpty()
                ? pageRooms
                : pageRooms.stream()
                        .filter(room -> room.getAmenities() != null && room.getAmenities().containsAll(amenities))
                        .toList();
        boolean amenityFallback = !amenities.isEmpty() && exact.isEmpty();
        List<Room> candidates = amenityFallback ? pageRooms : exact;

        List<RoomSuggestion> recommended = new ArrayList<>();
        List<RoomSuggestion> held = new ArrayList<>();
        List<RoomSuggestion> occupied = new ArrayList<>();
        Instant now = appClock.now();

        for (Room room : candidates) {
            classify(room, attendeeCount, startAt, endAt, amenities, actor, now, recommended, held, occupied);
        }

        Comparator<RoomSuggestion> byFit = Comparator
                .comparingInt((RoomSuggestion s) -> s.getMissingAmenities() == null ? 0 : s.getMissingAmenities().size())
                .thenComparingInt(s -> RoomFitScorer.score(s.getRoom().getCapacity(), attendeeCount));
        recommended.sort(byFit);
        held.sort(byFit);
        occupied.sort(byFit);
        return new RoomSuggestionResult(
                attendeeCount,
                recommended.size(),
                recommended,
                held,
                occupied,
                roomPage.getNumber(),
                roomPage.getTotalPages(),
                roomPage.hasNext(),
                amenityFallback
        );
    }

    private void classify(
            Room room,
            int attendeeCount,
            Instant startAt,
            Instant endAt,
            Set<Amenity> required,
            AuthUser actor,
            Instant now,
            List<RoomSuggestion> recommended,
            List<RoomSuggestion> held,
            List<RoomSuggestion> occupied
    ) {
        SlotState state = SlotState.AVAILABLE;
        Instant holdExpiresAt = null;
        int holdRemaining = 0;
        if (startAt != null) {
            if (bookingRepository.existsConfirmedOverlap(room.getId(), startAt, endAt)) {
                state = SlotState.BOOKED;
            } else {
                HoldRecord hold = holdService.overlappingHold(room.getId(), startAt, endAt).orElse(null);
                if (hold != null) {
                    holdExpiresAt = hold.getExpiresAt();
                    holdRemaining = hold.remainingSeconds(now);
                    state = hold.getUserId().equals(actor.getId()) ? SlotState.HELD_BY_ME : SlotState.HELD_BY_OTHER;
                }
            }
        }
        FitLevel fitLevel = RoomFitScorer.level(room.getCapacity(), attendeeCount);
        List<Amenity> missing = required.stream()
                .filter(amenity -> room.getAmenities() == null || !room.getAmenities().contains(amenity))
                .toList();
        RoomSuggestion suggestion = RoomSuggestion.builder()
                .room(roomMapper.toRoomResponse(room))
                .available(state == SlotState.AVAILABLE || state == SlotState.HELD_BY_ME)
                .slotState(state)
                .fitLevel(fitLevel)
                .unusedSeats(room.getCapacity() - attendeeCount)
                .fitLabel(RoomFitScorer.label(fitLevel))
                .holdExpiresAt(holdExpiresAt)
                .holdRemainingSeconds(holdRemaining)
                .missingAmenities(missing)
                .build();
        switch (state) {
            case AVAILABLE, HELD_BY_ME -> recommended.add(suggestion);
            case HELD_BY_OTHER -> held.add(suggestion);
            case BOOKED -> occupied.add(suggestion);
        }
    }

    private Room require(UUID id) {
        return roomRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    private RoomResponse toResponse(Room room, boolean locked) {
        RoomResponse response = roomMapper.toRoomResponse(room);
        response.setLocked(locked);
        return response;
    }
}
