package com.veytrix.autopilot;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class VeytrixInputValidatorTest {

    @Test
    public void acceptsSafeInputs() {
        VeytrixInputValidator.validateMission("Implement a secure Android control surface");
        VeytrixInputValidator.validateRepository("fahadsoomro123/veytrix-autopilot");
        VeytrixInputValidator.validateBranch("feature/secure-foundation");
        VeytrixInputValidator.validateEngine("auto");
        VeytrixInputValidator.validateEngine("mesh");
        VeytrixInputValidator.validateVerificationDepth(2);
    }

    @Test
    public void rejectsUnsafeRepository() {
        assertThrows(
                IllegalArgumentException.class,
                () -> VeytrixInputValidator.validateRepository("owner/../repo")
        );
    }

    @Test
    public void rejectsUnsafeBranch() {
        assertThrows(
                IllegalArgumentException.class,
                () -> VeytrixInputValidator.validateBranch("feature/../../main")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> VeytrixInputValidator.validateBranch("feature/branch name")
        );
    }

    @Test
    public void rejectsUnsafeExecutionControls() {
        assertThrows(
                IllegalArgumentException.class,
                () -> VeytrixInputValidator.validateEngine("shell")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> VeytrixInputValidator.validateVerificationDepth(3)
        );
    }

    @Test
    public void validatesOtaReleaseIdentity() {
        assertTrue(VeytrixUpdateClient.isValidReleaseTag("v1.0.1"));
        assertTrue(VeytrixUpdateClient.isValidReleaseTag("v1.0.1-rc1"));
        assertTrue(VeytrixUpdateClient.isValidReleaseTag("v12.3.4+build.7"));
        assertFalse(VeytrixUpdateClient.isValidReleaseTag("1.0.1"));
        assertFalse(VeytrixUpdateClient.isValidReleaseTag("v1.0"));
        assertFalse(VeytrixUpdateClient.isValidReleaseTag("v1.0.1/evil"));

        assertTrue(VeytrixUpdateClient.isValidReleaseDigest(
                "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
        assertFalse(VeytrixUpdateClient.isValidReleaseDigest("sha256:xyz"));
        assertFalse(VeytrixUpdateClient.isValidReleaseDigest(
                "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcde"));

        assertTrue(VeytrixUpdateClient.isValidReleaseDownloadUrl(
                "https://github.com/fahadsoomro123/veytrix-autopilot/releases/download/v1.0.1/veytrix-autopilot-release.apk",
                "v1.0.1"));
        assertFalse(VeytrixUpdateClient.isValidReleaseDownloadUrl(
                "https://github.com/fahadsoomro123/veytrix-autopilot/releases/download/v9.9.9/veytrix-autopilot-release.apk",
                "v1.0.1"));
        assertFalse(VeytrixUpdateClient.isValidReleaseDownloadUrl(
                "https://evil.example/releases/download/v1.0.1/veytrix-autopilot-release.apk",
                "v1.0.1"));
    }
}
