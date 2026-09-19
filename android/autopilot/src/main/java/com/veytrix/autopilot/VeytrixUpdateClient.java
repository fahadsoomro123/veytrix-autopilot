package com.veytrix.autopilot;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Public GitHub release discovery and digest-verified APK download. */
public final class VeytrixUpdateClient {
    private static final String API_HOST = "api.github.com";
    private static final String RELEASE_DOWNLOAD_PREFIX =
            "https://github.com/fahadsoomro123/veytrix-autopilot/releases/download/";
    private static final String REPOSITORY = "fahadsoomro123/veytrix-autopilot";
    private static final long MAX_APK_BYTES = 100L * 1024L * 1024L;

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public VeytrixUpdateClient(Context context) {
        this.context = context.getApplicationContext();
    }

    public void checkLatest(Callback<UpdateInfo> callback) {
        executor.execute(() -> {
            try {
                HttpResult result = request("GET",
                        "https://api.github.com/repos/" + REPOSITORY + "/releases/latest", null);
                if (result.code != 200) {
                    throw new IllegalStateException("Latest release lookup failed (HTTP " + result.code + ")");
                }
                JSONObject release = new JSONObject(result.body);
                UpdateInfo info = parseRelease(release);
                postSuccess(callback, info);
            } catch (Exception error) {
                postError(callback, safeMessage(error));
            }
        });
    }

    public void download(UpdateInfo info, Callback<File> callback) {
        executor.execute(() -> {
            File target = new File(context.getCacheDir(),
                    "veytrix-update-" + info.sha256.substring(0, 16) + ".apk");
            try {
                downloadToFile(info.downloadUrl, target, info.sha256);
                postSuccess(callback, target);
            } catch (Exception error) {
                if (target.exists()) target.delete();
                postError(callback, safeMessage(error));
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private UpdateInfo parseRelease(JSONObject release) throws Exception {
        String tag = release.optString("tag_name", "").trim();
        if (!isValidReleaseTag(tag)) {
            throw new SecurityException("Release tag is not semantic-versioned");
        }
        JSONArray assets = release.optJSONArray("assets");
        if (assets == null) throw new IllegalStateException("Release has no assets");

        JSONObject apk = null;
        int matchingAssets = 0;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject candidate = assets.getJSONObject(i);
            String name = candidate.optString("name", "");
            if ("veytrix-autopilot-release.apk".equals(name) ||
                    "autopilot-release.apk".equals(name)) {
                matchingAssets++;
                apk = candidate;
            }
        }
        if (matchingAssets == 0) {
            throw new IllegalStateException("Latest release has no VEYTRIX APK asset");
        }
        if (matchingAssets > 1) {
            throw new SecurityException("Latest release has ambiguous VEYTRIX APK assets");
        }

        String url = apk.optString("browser_download_url", "").trim();
        String digest = apk.optString("digest", "").trim().toLowerCase();
        if (!isValidReleaseDownloadUrl(url, tag)) {
            throw new SecurityException("Release APK URL does not match the published release tag");
        }
        if (!isValidReleaseDigest(digest)) {
            throw new SecurityException("Release APK does not expose a valid SHA-256 digest");
        }

        return new UpdateInfo(tag, url, digest.substring("sha256:".length()),
                release.optString("name", tag), release.optString("body", ""));
    }

    static boolean isValidReleaseTag(String tag) {
        return tag != null && tag.matches("v[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][0-9A-Za-z.-]+)?");
    }

    static boolean isValidReleaseDigest(String digest) {
        return digest != null && digest.matches("sha256:[0-9a-f]{64}");
    }

    static boolean isValidReleaseDownloadUrl(String url, String tag) {
        return url != null && tag != null &&
                url.startsWith(RELEASE_DOWNLOAD_PREFIX + tag + "/");
    }

    private void downloadToFile(String initialUrl, File target, String expectedSha256) throws Exception {
        String currentUrl = initialUrl;
        InputStream input = null;
        HttpURLConnection connection = null;
        try {
            for (int redirects = 0; redirects <= 2; redirects++) {
                URL url = new URL(currentUrl);
                if (!"https".equalsIgnoreCase(url.getProtocol())) {
                    throw new SecurityException("Update download must use HTTPS");
                }
                if (!isAllowedDownloadHost(url.getHost())) {
                    throw new SecurityException("Update download redirected to an untrusted host");
                }
                if (redirects > 0 && "github.com".equalsIgnoreCase(url.getHost())) {
                    throw new SecurityException("Release redirects must terminate on a GitHub asset host");
                }
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(60000);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("Accept", "application/vnd.android.package-archive");
                connection.setRequestProperty("User-Agent", "Veytrix-Android-OTA");

                int code = connection.getResponseCode();
                if (code == 200) {
                    long length = connection.getContentLengthLong();
                    if (length > MAX_APK_BYTES) throw new SecurityException("Update APK is too large");
                    input = new BufferedInputStream(connection.getInputStream());
                    writeAndVerify(input, target, expectedSha256, length);
                    return;
                }
                if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                    String next = connection.getHeaderField("Location");
                    if (next == null || next.isEmpty()) throw new IllegalStateException("GitHub release download redirect has no location");
                    currentUrl = next;
                    connection.disconnect();
                    connection = null;
                    continue;
                }
                throw new IllegalStateException("Update download failed (HTTP " + code + ")");
            }
            throw new SecurityException("Too many release download redirects");
        } finally {
            if (input != null) try { input.close(); } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
        }
    }

    private void writeAndVerify(InputStream input, File target, String expectedSha256, long contentLength) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long total = 0L;
        File parent = target.getParentFile();
        if (parent != null) parent.mkdirs();
        try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(target))) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_APK_BYTES) throw new SecurityException("Update APK is too large");
                digest.update(buffer, 0, read);
                output.write(buffer, 0, read);
            }
        }
        if (contentLength >= 0 && total != contentLength) {
            throw new IllegalStateException("Update download length changed during transfer");
        }
        String actual = hex(digest.digest());
        if (!actual.equalsIgnoreCase(expectedSha256)) {
            throw new SecurityException("Update APK SHA-256 verification failed");
        }
        if (total == 0L) throw new IllegalStateException("Downloaded update is empty");
    }

    private HttpResult request(String method, String urlText, String body) throws Exception {
        URL url = new URL(urlText);
        if (!API_HOST.equalsIgnoreCase(url.getHost()) || !"https".equalsIgnoreCase(url.getProtocol())) {
            throw new SecurityException("Unexpected GitHub API host");
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", "2026-03-10");
        connection.setRequestProperty("User-Agent", "Veytrix-Android-OTA");
        try {
            int code = connection.getResponseCode();
            try (InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream()) {
                return new HttpResult(code, read(stream));
            }
        } finally {
            connection.disconnect();
        }
    }

    private boolean isAllowedDownloadHost(String host) {
        return "github.com".equalsIgnoreCase(host) ||
                "release-assets.githubusercontent.com".equalsIgnoreCase(host) ||
                "objects.githubusercontent.com".equalsIgnoreCase(host);
    }

    private String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
            if (output.size() > 2 * 1024 * 1024) {
                throw new SecurityException("GitHub response is too large");
            }
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }

    private static String hex(byte[] bytes) {
        StringBuilder output = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) output.append(String.format("%02x", value & 0xff));
        return output.toString();
    }

    private <T> void postSuccess(Callback<T> callback, T value) {
        main.post(() -> callback.onSuccess(value));
    }

    private <T> void postError(Callback<T> callback, String message) {
        main.post(() -> callback.onError(message));
    }

    private static String safeMessage(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isEmpty()) return error.getClass().getSimpleName();
        return message.length() > 240 ? message.substring(0, 240) : message;
    }

    public interface Callback<T> {
        void onSuccess(T value);
        void onError(String message);
    }

    public static final class UpdateInfo {
        public final String tag;
        public final String downloadUrl;
        public final String sha256;
        public final String releaseName;
        public final String releaseNotes;

        public UpdateInfo(String tag, String downloadUrl, String sha256,
                String releaseName, String releaseNotes) {
            this.tag = tag;
            this.downloadUrl = downloadUrl;
            this.sha256 = sha256;
            this.releaseName = releaseName;
            this.releaseNotes = releaseNotes;
        }
    }

    private static final class HttpResult {
        final int code;
        final String body;
        HttpResult(int code, String body) { this.code = code; this.body = body; }
    }
}