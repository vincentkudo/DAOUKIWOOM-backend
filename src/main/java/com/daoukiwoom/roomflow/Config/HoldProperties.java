package com.daoukiwoom.roomflow.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HoldProperties {

    public static final int MIN_SECONDS = 30;
    public static final int MAX_SECONDS = 300;

    @Value("${app.hold.ttl-seconds:90}")
    private int ttlSeconds;

    public int getTtlSeconds() {
        return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, ttlSeconds));
    }

    public int heartbeatSeconds() {
        return Math.max(15, getTtlSeconds() / 3);
    }
}
