package com.veytrix.autopilot;

public final class VeytrixInputValidator {
    private static final int MAX_MISSION_LENGTH = 4000;
    private static final int MAX_VERIFICATION_DEPTH = 2;

    private VeytrixInputValidator() {
    }

    public static void validateMission(String mission) {
        if (mission == null || mission.trim().isEmpty()) {
            throw new IllegalArgumentException("Mission prompt is required");
        }
        if (mission.indexOf('\u0000') >= 0) {
            throw new IllegalArgumentException("Mission prompt contains an invalid character");
        }
        if (mission.length() > MAX_MISSION_LENGTH) {
            throw new IllegalArgumentException("Mission prompt exceeds 4000 characters");
        }
    }

    public static void validateRepository(String repository) {
        if (repository == null
                || !repository.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")
                || repository.contains("..")) {
            throw new IllegalArgumentException("Target repository must use safe owner/repository syntax");
        }
    }

    public static void validateBranch(String branch) {
        if (branch == null
                || !branch.matches("[A-Za-z0-9._/-]+")
                || branch.startsWith("/")
                || branch.endsWith("/")
                || branch.startsWith(".")
                || branch.endsWith(".")
                || branch.contains("..")
                || branch.contains("@{")
                || branch.contains("//")) {
            throw new IllegalArgumentException("Target branch contains unsupported syntax");
        }
    }

    public static void validateEngine(String engine) {
        if (!"auto".equals(engine) && !"github-free".equals(engine) && !"ai".equals(engine) && !"mesh".equals(engine)) {
            throw new IllegalArgumentException("Unsupported execution engine");
        }
    }

    public static void validateVerificationDepth(int depth) {
        if (depth < 0 || depth > MAX_VERIFICATION_DEPTH) {
            throw new IllegalArgumentException("verification_depth exceeds the safety ceiling");
        }
    }
}
