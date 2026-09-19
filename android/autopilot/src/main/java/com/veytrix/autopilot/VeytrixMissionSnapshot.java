package com.veytrix.autopilot;

/**
 * Read-only mission data contract for the Phase 2 UI.
 *
 * No percentages, counts, timestamps, models, or artifacts are invented here.
 * Producers must supply real values or explicitly represent absence.
 */
public final class VeytrixMissionSnapshot {
    public final long runId;
    public final String mission;
    public final String targetRepository;
    public final String branch;
    public final String engine;
    public final VeytrixMissionState state;
    public final String detail;
    public final boolean hasRealArtifact;
    public final String artifactName;
    public final String artifactSha256;

    public VeytrixMissionSnapshot(
            long runId,
            String mission,
            String targetRepository,
            String branch,
            String engine,
            VeytrixMissionState state,
            String detail,
            boolean hasRealArtifact,
            String artifactName,
            String artifactSha256
    ) {
        this.runId = runId;
        this.mission = safe(mission);
        this.targetRepository = safe(targetRepository);
        this.branch = safe(branch);
        this.engine = safe(engine);
        this.state = state == null ? VeytrixMissionState.IDLE : state;
        this.detail = safe(detail);
        this.hasRealArtifact = hasRealArtifact;
        this.artifactName = safe(artifactName);
        this.artifactSha256 = safe(artifactSha256);

        if (hasRealArtifact && (this.artifactName.isEmpty() || this.artifactSha256.isEmpty())) {
            throw new IllegalArgumentException(
                    "A real artifact requires both name and SHA-256"
            );
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
