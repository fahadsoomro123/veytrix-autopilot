package com.veytrix.autopilot;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Real native control-plane client for the VEYTRIX Android app.
 *
 * Responsibilities:
 * - securely stores the user's GitHub PAT with Android Keystore + AES/GCM
 * - verifies GitHub access and the target repository
 * - dispatches the real VEYTRIX Smart Autopilot workflow
 * - locates the resulting workflow run
 * - polls the run until completion
 *
 * No token is written to source, logs, intents, or UI callbacks.
 */
public final class VeytrixAutopilotClient {
    private static final String OWNER = "fahadsoomro123";
    private static final String REPO = "veytrix-autopilot";
    private static final String WORKFLOW = "veytrix-autopilot.yml";
    private static final String API = "https://api.github.com";
    private static final String KEY_ALIAS = "veytrix_autopilot_token_v2";
    private static final String PREFS = "veytrix_secure_v2";
    private static final String PREF_TOKEN = "token_ciphertext";
    private static final String PREF_IV = "token_iv";
    private static final String PREF_TARGET = "target_repository";
    private static final String DEFAULT_TARGET = "owner/repository";

    public static final class RunInfo {
        public final long id;
        public final long number;
        public final String status;
        public final String conclusion;
        public final String htmlUrl;
        public final String updatedAt;

        RunInfo(long id, long number, String status, String conclusion, String htmlUrl, String updatedAt) {
            this.id = id;
            this.number = number;
            this.status = status == null ? "" : status;
            this.conclusion = conclusion == null ? "" : conclusion;
            this.htmlUrl = htmlUrl == null ? "" : htmlUrl;
            this.updatedAt = updatedAt == null ? "" : updatedAt;
        }

        public boolean isFinished() {
            return "completed".equalsIgnoreCase(status);
        }

        public boolean isSuccessful() {
            return "success".equalsIgnoreCase(conclusion);
        }
    }

    public interface Callback {
        void onStarted();
        void onRunLocated(RunInfo run);
        void onRunUpdated(RunInfo run);
        void onCompleted(RunInfo run);
        void onError(String message);
    }

    public static final class Verification {
        public final String login;
        public final String targetRepository;
        public final boolean targetAccessible;

        Verification(String login, String targetRepository, boolean targetAccessible) {
            this.login = login;
            this.targetRepository = targetRepository;
            this.targetAccessible = targetAccessible;
        }
    }

    private final Context context;
    private final SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());

    public VeytrixAutopilotClient(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ensureKey();
    }

    public boolean hasToken() {
        return prefs.contains(PREF_TOKEN) && prefs.contains(PREF_IV);
    }

    public String getTargetRepository() {
        return prefs.getString(PREF_TARGET, DEFAULT_TARGET);
    }

    public void saveConnection(String token, String targetRepository) throws Exception {
        if (token == null || token.trim().isEmpty()) throw new IllegalArgumentException("GitHub token is required");
        if (!isRepoName(targetRepository)) throw new IllegalArgumentException("Target repository must look like owner/name");
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getKey(), new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(token.trim().getBytes(StandardCharsets.UTF_8));
        prefs.edit()
                .putString(PREF_TOKEN, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(PREF_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .putString(PREF_TARGET, targetRepository.trim())
                .apply();
    }

    public void clearConnection() {
        prefs.edit().remove(PREF_TOKEN).remove(PREF_IV).apply();
    }

    public void saveTargetRepository(String targetRepository) {
        if (!isRepoName(targetRepository)) throw new IllegalArgumentException("Target repository must look like owner/name");
        prefs.edit().putString(PREF_TARGET, targetRepository.trim()).apply();
    }

    public void verifyConnection(String tokenOverride, String targetRepository, SimpleCallback<Verification> callback) {
        executor.execute(() -> {
            try {
                String token = tokenOverride == null || tokenOverride.trim().isEmpty() ? loadToken() : tokenOverride.trim();
                if (token.isEmpty()) throw new IllegalStateException("GitHub token is required");
                if (!isRepoName(targetRepository)) throw new IllegalArgumentException("Target repository must look like owner/name");

                HttpResult user = request("GET", "/user", token, null);
                if (user.code != 200) throw new IllegalStateException("GitHub token rejected (HTTP " + user.code + ")");
                JSONObject userJson = new JSONObject(user.body);
                String login = userJson.optString("login", "GitHub user");

                HttpResult target = request("GET", "/repos/" + targetRepository, token, null);
                if (target.code != 200) throw new IllegalStateException("Target repository access failed (HTTP " + target.code + ")");

                if (tokenOverride != null && !tokenOverride.trim().isEmpty()) saveConnection(token, targetRepository);

                Verification result = new Verification(login, targetRepository, true);
                main.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                main.post(() -> callback.onError(safeMessage(e)));
            }
        });
    }

    public void startMission(
            String mission,
            String targetRepository,
            String branch,
            String engine,
            String verificationDepth,
            Callback callback) {
        executor.execute(() -> {
            long startedAt = System.currentTimeMillis();
            try {
                if (mission == null || mission.trim().isEmpty()) throw new IllegalArgumentException("Mission prompt is required");
                if (!isRepoName(targetRepository)) throw new IllegalArgumentException("Target repository must look like owner/name");
                if (branch == null || branch.trim().isEmpty()) throw new IllegalArgumentException("Target branch is required");
                String token = loadToken();
                if (token.isEmpty()) throw new IllegalStateException("Connect GitHub first");

                main.post(callback::onStarted);

                JSONObject inputs = new JSONObject()
                        .put("mission", mission.trim())
                        .put("target_repository", targetRepository.trim())
                        .put("branch", branch.trim())
                        .put("engine", engine == null || engine.trim().isEmpty() ? "auto" : engine.trim())
                        .put("verification_depth", verificationDepth == null || verificationDepth.trim().isEmpty() ? "2" : verificationDepth.trim());

                JSONObject body = new JSONObject()
                        .put("ref", "main")
                        .put("inputs", inputs);

                HttpResult dispatch = request(
                        "POST",
                        "/repos/" + OWNER + "/" + REPO + "/actions/workflows/" + WORKFLOW + "/dispatches",
                        token,
                        body.toString());

                if (dispatch.code != 204) {
                    throw new IllegalStateException("Mission dispatch failed (HTTP " + dispatch.code + ")");
                }

                RunInfo run = waitForRun(token, startedAt);
                if (run == null) throw new IllegalStateException("Workflow accepted, but its live run could not be located");

                main.post(() -> callback.onRunLocated(run));

                while (!run.isFinished()) {
                    Thread.sleep(3500L);
                    run = getRun(token, run.id);
                    final RunInfo update = run;
                    main.post(() -> callback.onRunUpdated(update));
                }

                final RunInfo complete = run;
                main.post(() -> callback.onCompleted(complete));
            } catch (Exception e) {
                main.post(() -> callback.onError(safeMessage(e)));
            }
        });
    }

    public void fetchRun(long runId, SimpleCallback<RunInfo> callback) {
        executor.execute(() -> {
            try {
                RunInfo info = getRun(loadToken(), runId);
                main.post(() -> callback.onSuccess(info));
            } catch (Exception e) {
                main.post(() -> callback.onError(safeMessage(e)));
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private RunInfo waitForRun(String token, long startedAt) throws Exception {
        for (int i = 0; i < 18; i++) {
            HttpResult r = request(
                    "GET",
                    "/repos/" + OWNER + "/" + REPO + "/actions/workflows/" + WORKFLOW + "/runs?per_page=10&event=workflow_dispatch",
                    token,
                    null);
            if (r.code != 200) throw new IllegalStateException("Run lookup failed (HTTP " + r.code + ")");

            JSONArray runs = new JSONObject(r.body).optJSONArray("workflow_runs");
            if (runs != null) {
                for (int iRun = 0; iRun < runs.length(); iRun++) {
                    JSONObject item = runs.getJSONObject(iRun);
                    long created = parseGithubTime(item.optString("created_at"));
                    if (created >= startedAt - 120000L) {
                        return toRunInfo(item);
                    }
                }
            }
            Thread.sleep(2500L);
        }
        return null;
    }

    private RunInfo getRun(String token, long runId) throws Exception {
        HttpResult r = request("GET", "/repos/" + OWNER + "/" + REPO + "/actions/runs/" + runId, token, null);
        if (r.code != 200) throw new IllegalStateException("Live run lookup failed (HTTP " + r.code + ")");
        return toRunInfo(new JSONObject(r.body));
    }

    private RunInfo toRunInfo(JSONObject o) {
        return new RunInfo(
                o.optLong("id"),
                o.optLong("run_number"),
                o.optString("status"),
                o.optString("conclusion"),
                o.optString("html_url"),
                o.optString("updated_at"));
    }

    private String loadToken() throws Exception {
        String encoded = prefs.getString(PREF_TOKEN, "");
        String ivEncoded = prefs.getString(PREF_IV, "");
        if (encoded.isEmpty() || ivEncoded.isEmpty()) return "";
        byte[] encrypted = Base64.decode(encoded, Base64.NO_WRAP);
        byte[] iv = Base64.decode(ivEncoded, Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private SecretKey getKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        return ((KeyStore.SecretKeyEntry) store.getEntry(KEY_ALIAS, null)).getSecretKey();
    }

    private void ensureKey() {
        try {
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            store.load(null);
            if (store.containsAlias(KEY_ALIAS)) return;
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false)
                    .build());
            generator.generateKey();
        } catch (Exception ignored) {
            // The first authenticated operation will surface a concrete error to the UI.
        }
    }

    private HttpResult request(String method, String path, String token, String body) throws Exception {
        URL url = new URL(API + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        connection.setRequestProperty("User-Agent", "Veytrix-Android/" + context.getPackageName());
        connection.setRequestProperty("Authorization", "Bearer " + token);
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(bytes);
            }
        }

        int code = connection.getResponseCode();
        InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String response = read(stream);
        connection.disconnect();
        return new HttpResult(code, response);
    }

    private String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString().trim();
    }

    private static boolean isRepoName(String value) {
        return value != null && value.matches("[^/\\s]+/[^/\\s]+");
    }

    private static long parseGithubTime(String value) {
        try {
            return java.time.Instant.parse(value).toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isEmpty()) return e.getClass().getSimpleName();
        return message.length() > 240 ? message.substring(0, 240) : message;
    }

    private static final class HttpResult {
        final int code;
        final String body;
        HttpResult(int code, String body) { this.code = code; this.body = body; }
    }

    public interface SimpleCallback<T> {
        void onSuccess(T value);
        void onError(String message);
    }
}
