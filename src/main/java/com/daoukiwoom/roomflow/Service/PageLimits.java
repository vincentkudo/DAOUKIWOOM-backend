package com.daoukiwoom.roomflow.Service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class PageLimits {

    public static final int MAX_SIZE = 50;
    public static final int ROOMS = 15;
    public static final int BOOKINGS = 20;
    public static final int CALENDAR_ROOMS = 10;
    public static final int USERS = 20;
    public static final int SUGGESTIONS = 12;
    public static final int RANGE_DAYS = 7;

    private PageLimits() {
    }

    public static Pageable of(Integer page, Integer size, int defaultSize) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size < 1 ? defaultSize : Math.min(size, MAX_SIZE);
        return PageRequest.of(safePage, safeSize);
    }
}
