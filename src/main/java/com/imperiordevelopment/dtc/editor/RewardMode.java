package com.imperiordevelopment.dtc.editor;

public enum RewardMode {
    NONE,
    COMMAND,
    LOOT;

    public RewardMode next() {
        return switch (this) {
            case NONE -> COMMAND;
            case COMMAND -> LOOT;
            case LOOT -> NONE;
        };
    }

    public static RewardMode fromString(String value) {
        if (value == null) {
            return NONE;
        }
        // Backward compatibility with older names
        if ("COMMANDS".equalsIgnoreCase(value)) {
            return COMMAND;
        }
        if ("ITEMS".equalsIgnoreCase(value)) {
            return LOOT;
        }
        if ("BOTH".equalsIgnoreCase(value)) {
            return COMMAND;
        }
        for (RewardMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return NONE;
    }
}
