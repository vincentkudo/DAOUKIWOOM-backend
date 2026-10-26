package com.daoukiwoom.roomflow.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class AppClock {

    private final ZoneId zoneId;
    private final Clock clock;

    public AppClock(@Value("${app.timezone}") String timezone) {
        this.zoneId = ZoneId.of(timezone);
        this.clock = Clock.system(zoneId);
    }

    public ZoneId zone() {
        return zoneId;
    }

    public Instant now() {
        return clock.instant();
    }

    public ZonedDateTime nowZoned() {
        return ZonedDateTime.now(clock);
    }
}
