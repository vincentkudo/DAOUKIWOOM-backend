package com.daoukiwoom.roomflow.Service;

import com.daoukiwoom.roomflow.Entity.FitLevel;

public final class RoomFitScorer {

    private RoomFitScorer() {
    }

    public static FitLevel level(int capacity, int attendeeCount) {
        if (attendeeCount <= 0 || capacity < attendeeCount) {
            throw new IllegalArgumentException("Room must fit a positive attendee count");
        }
        int waste = capacity - attendeeCount;
        if (waste == 0) {
            return FitLevel.PERFECT;
        }
        double wasteRatio = (double) waste / capacity;
        if (waste <= 2 || wasteRatio <= 0.20) {
            return FitLevel.TIGHT;
        }
        if (wasteRatio <= 0.40) {
            return FitLevel.GOOD;
        }
        return FitLevel.SPACIOUS;
    }

    public static int score(int capacity, int attendeeCount) {
        return (capacity - attendeeCount) * 100 + capacity;
    }

    public static String label(FitLevel level) {
        return switch (level) {
            case PERFECT -> "Vừa";
            case TIGHT -> "Tốt";
            case GOOD -> "Ổn";
            case SPACIOUS -> "Rộng";
        };
    }
}
