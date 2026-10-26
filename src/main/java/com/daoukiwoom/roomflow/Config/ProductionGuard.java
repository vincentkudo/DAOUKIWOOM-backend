package com.daoukiwoom.roomflow.Config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("prod")
public class ProductionGuard {

    private static final Logger log = LoggerFactory.getLogger(ProductionGuard.class);
    private static final List<String> REQUIRED = List.of(
            "DB_URL",
            "DB_USERNAME",
            "DB_PASSWORD",
            "JWT_SECRET",
            "CORS_ORIGINS",
            "REDIS_HOST"
    );

    private final Environment environment;

    public ProductionGuard(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void verify() {
        List<String> missing = new ArrayList<>();
        for (String key : REQUIRED) {
            String value = environment.getProperty(key);
            if (value == null || value.isBlank()) {
                missing.add(key);
            }
        }
        String jwt = environment.getProperty("JWT_SECRET", "");
        if (!jwt.isBlank() && jwt.length() < 32) {
            missing.add("JWT_SECRET (must be at least 32 characters)");
        }
        if (jwt.toLowerCase().contains("change-me") || jwt.toLowerCase().contains("dev-secret")) {
            missing.add("JWT_SECRET (must not use a development placeholder)");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Production profile is missing required environment variables: " + missing);
        }
        log.info("Production environment checks passed. CORS origins = {}", environment.getProperty("CORS_ORIGINS"));
    }
}
