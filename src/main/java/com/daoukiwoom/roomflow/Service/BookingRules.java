package com.daoukiwoom.roomflow.Service;

import com.daoukiwoom.roomflow.Exception.AppException;
import com.daoukiwoom.roomflow.Exception.ErrorCode;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class BookingRules {

    public static final int SLOT_MINUTES = 15;
    public static final int MIN_DURATION_MINUTES = 15;
    public static final int MAX_DURATION_HOURS = 8;
    public static final int MAX_ADVANCE_DAYS = 30;
    public static final LocalTime OPEN = LocalTime.of(7, 0);
    public static final LocalTime CLOSE = LocalTime.of(21, 0);

    private BookingRules() {
    }

    public static void validate(Instant startAt, Instant endAt, Instant now, ZoneId zone) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new AppException(ErrorCode.INVALID_TIME_RANGE);
        }
        if (startAt.isBefore(now.minusSeconds(30))) {
            throw new AppException(ErrorCode.BOOKING_IN_PAST);
        }
        if (startAt.isAfter(now.plus(Duration.ofDays(MAX_ADVANCE_DAYS)))) {
            throw new AppException(ErrorCode.BOOKING_TOO_FAR);
        }
        long minutes = Duration.between(startAt, endAt).toMinutes();
        if (minutes < MIN_DURATION_MINUTES) {
            throw new AppException(ErrorCode.BOOKING_TOO_SHORT);
        }
        if (minutes > MAX_DURATION_HOURS * 60L) {
            throw new AppException(ErrorCode.BOOKING_TOO_LONG);
        }
        ZonedDateTime start = startAt.atZone(zone);
        ZonedDateTime end = endAt.atZone(zone);
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw new AppException(ErrorCode.INVALID_TIME_RANGE, "Lịch đặt phải trong cùng một ngày.");
        }
        if (!aligned(start) || !aligned(end)) {
            throw new AppException(ErrorCode.SLOT_NOT_ALIGNED);
        }
        if (start.toLocalTime().isBefore(OPEN) || end.toLocalTime().isAfter(CLOSE)) {
            throw new AppException(ErrorCode.OUTSIDE_WORKING_HOURS);
        }
    }

    private static boolean aligned(ZonedDateTime dateTime) {
        return dateTime.getMinute() % SLOT_MINUTES == 0 && dateTime.getSecond() == 0 && dateTime.getNano() == 0;
    }
}
