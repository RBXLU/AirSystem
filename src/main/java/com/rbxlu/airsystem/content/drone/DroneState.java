package com.rbxlu.airsystem.content.drone;

// Ordinals are persisted and sent over the network: append, never reorder.
public enum DroneState {
    IDLE,

    LAUNCH,

    CRUISE,

    ORBIT,

    DIVE,

    FALLING,

    DESTROYED,

    RTB,

    LANDING,

    LANDED;

    public boolean isAirborne() {
        return this == LAUNCH || this == CRUISE || this == ORBIT || this == DIVE
                || this == FALLING || this == RTB || this == LANDING;
    }

    public boolean isControllable() {
        return this == LAUNCH || this == CRUISE || this == ORBIT || this == DIVE
                || this == RTB || this == LANDING;
    }

    public boolean isRecovering() {
        return this == RTB || this == LANDING;
    }

    public boolean isFinished() {
        return this == DESTROYED || this == LANDED;
    }

    public static DroneState byOrdinal(int ordinal) {
        DroneState[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return IDLE;
        }
        return values[ordinal];
    }
}
