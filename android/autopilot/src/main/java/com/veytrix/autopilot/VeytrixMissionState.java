package com.veytrix.autopilot;

/**
 * Observable lifecycle states for an autonomous engineering mission.
 * A screen must render state from this enum rather than inventing status text.
 */
public enum VeytrixMissionState {
    IDLE,
    PLANNING,
    IMPLEMENTING,
    BUILDING,
    TESTING,
    FAILED,
    REPAIRING,
    RETESTING,
    VERIFYING,
    VERIFIED,
    DELIVERED
}
