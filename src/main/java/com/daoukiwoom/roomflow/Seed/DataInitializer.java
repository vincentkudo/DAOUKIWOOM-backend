package com.daoukiwoom.roomflow.Seed;

import com.daoukiwoom.roomflow.Config.AppClock;
import com.daoukiwoom.roomflow.Entity.Amenity;
import com.daoukiwoom.roomflow.Entity.Booking;
import com.daoukiwoom.roomflow.Entity.BookingStatus;
import com.daoukiwoom.roomflow.Entity.Role;
import com.daoukiwoom.roomflow.Entity.Room;
import com.daoukiwoom.roomflow.Entity.RoomStatus;
import com.daoukiwoom.roomflow.Entity.User;
import com.daoukiwoom.roomflow.Repository.IBookingRepository;
import com.daoukiwoom.roomflow.Repository.IRoomRepository;
import com.daoukiwoom.roomflow.Repository.IUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IRoomRepository roomRepository;

    @Autowired
    private IBookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AppClock appClock;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled || userRepository.count() > 0) {
            return;
        }

        String adminPassword = firstNonBlank(System.getenv("SEED_ADMIN_PASSWORD"), "Admin123!");
        String employeePassword = firstNonBlank(System.getenv("SEED_EMPLOYEE_PASSWORD"), "Emp123!");

        User admin = user("admin@daoukiwoom.vn", adminPassword, "Minh Quản Trị", "IT", Role.ADMIN);
        User linh = user("linh.tran@daoukiwoom.vn", employeePassword, "Trần Khánh Linh", "Sales", Role.EMPLOYEE);
        User hung = user("hung.nguyen@daoukiwoom.vn", employeePassword, "Nguyễn Quốc Hùng", "Engineering", Role.EMPLOYEE);
        User mai = user("mai.pham@daoukiwoom.vn", employeePassword, "Phạm Thanh Mai", "HR", Role.EMPLOYEE);
        User duc = user("duc.le@daoukiwoom.vn", employeePassword, "Lê Minh Đức", "Finance", Role.EMPLOYEE);
        userRepository.saveAll(List.of(admin, linh, hung, mai, duc));

        Room huddleA = room("R01", "Huddle A", 7, "Tầng 7 — Cánh trái", 4, "Phòng họp nhỏ 1:1 / standup.",
                EnumSet.of(Amenity.TV, Amenity.WHITEBOARD, Amenity.AIR_CONDITIONER));
        Room huddleB = room("R02", "Huddle B", 7, "Tầng 7 — Cánh phải", 4, "Phòng họp nhanh 4 người.",
                EnumSet.of(Amenity.TV, Amenity.PHONE, Amenity.AIR_CONDITIONER));
        Room focus = room("R03", "Focus 6", 8, "Tầng 8 — Gần pantry", 6, "Phù hợp team nhỏ.",
                EnumSet.of(Amenity.TV, Amenity.WHITEBOARD, Amenity.VIDEO_CONFERENCE));
        Room collab = room("R04", "Collab 8", 8, "Tầng 8 — Cánh giữa", 8, "Phòng cộng tác hàng ngày.",
                EnumSet.of(Amenity.PROJECTOR, Amenity.WHITEBOARD, Amenity.TV));
        Room sprint = room("R05", "Sprint 8", 9, "Tầng 9 — Engineering", 8, "Planning / retro.",
                EnumSet.of(Amenity.TV, Amenity.WHITEBOARD, Amenity.VIDEO_CONFERENCE));
        Room client = room("R06", "Client 10", 9, "Tầng 9 — Gần lễ tân", 10, "Tiếp khách, có call.",
                EnumSet.of(Amenity.TV, Amenity.VIDEO_CONFERENCE, Amenity.PHONE, Amenity.AIR_CONDITIONER));
        Room workshop = room("R07", "Workshop 12", 10, "Tầng 10 — Training", 12, "Workshop nội bộ.",
                EnumSet.of(Amenity.PROJECTOR, Amenity.WHITEBOARD, Amenity.TV));
        Room allHands = room("R08", "All-hands 16", 10, "Tầng 10 — Hall", 16, "Họp liên phòng ban.",
                EnumSet.of(Amenity.PROJECTOR, Amenity.VIDEO_CONFERENCE, Amenity.TV));
        Room board = room("R09", "Boardroom 20", 11, "Tầng 11 — Executive", 20, "Họp lãnh đạo / khách lớn.",
                EnumSet.of(Amenity.PROJECTOR, Amenity.VIDEO_CONFERENCE, Amenity.TV, Amenity.PHONE));
        Room auditorium = room("R10", "Auditorium 30", 11, "Tầng 11 — Sự kiện", 30, "Townhall, training lớn.",
                EnumSet.of(Amenity.PROJECTOR, Amenity.VIDEO_CONFERENCE, Amenity.TV, Amenity.AIR_CONDITIONER));
        roomRepository.saveAll(List.of(huddleA, huddleB, focus, collab, sprint, client, workshop, allHands, board, auditorium));

        LocalDate today = appClock.nowZoned().toLocalDate();
        LocalDate day = today.getDayOfWeek().getValue() >= 6 ? today.plusDays(8 - today.getDayOfWeek().getValue()) : today;

        bookingRepository.saveAll(List.of(
                booking(focus, linh, "Daily Sales standup", 4, day, LocalTime.of(9, 0), LocalTime.of(9, 30)),
                booking(sprint, hung, "Sprint planning", 7, day, LocalTime.of(10, 0), LocalTime.of(11, 30)),
                booking(client, mai, "Phỏng vấn ứng viên", 3, day, LocalTime.of(14, 0), LocalTime.of(15, 0)),
                booking(board, admin, "Weekly leadership", 12, day.plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 30))
        ));

        log.info("Seeded demo users, 10 rooms, and sample bookings. Admin = admin@daoukiwoom.vn");
    }

    private User user(String email, String password, String name, String department, Role role) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(name)
                .department(department)
                .role(role)
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    private Room room(String code, String name, int floor, String location, int capacity, String description, Set<Amenity> amenities) {
        Instant now = Instant.now();
        return Room.builder()
                .id(UUID.randomUUID())
                .code(code)
                .name(name)
                .floor(floor)
                .location(location)
                .capacity(capacity)
                .description(description)
                .status(RoomStatus.ACTIVE)
                .amenities(amenities)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Booking booking(Room room, User organizer, String title, int people, LocalDate day, LocalTime start, LocalTime end) {
        Instant startAt = ZonedDateTime.of(day, start, appClock.zone()).toInstant();
        Instant endAt = ZonedDateTime.of(day, end, appClock.zone()).toInstant();
        Instant now = Instant.now();
        return Booking.builder()
                .id(UUID.randomUUID())
                .room(room)
                .organizer(organizer)
                .title(title)
                .startAt(startAt)
                .endAt(endAt)
                .attendeeCount(people)
                .status(BookingStatus.CONFIRMED)
                .attendeeIds(Set.of(organizer.getId()))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
