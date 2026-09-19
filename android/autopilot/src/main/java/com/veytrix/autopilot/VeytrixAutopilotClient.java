package com.veytrix.autopilot;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public final class VeytrixAutopilotClient {
    private static final String OWNER = "fahadsoomro123";
    private static final String REPO = "veytrix-autopilot";
    private static final String WORKFLOW = "veytrix-autopilot.yml";
    private static final String API_HOST = "api.github.com";
    private static final String API_BASE = "https://" + API_HOST;
    private static final int MAX_API_RESPONSE_CHARS = 4 * 1024 * 1024;

    private final VeytrixSecureStore secureStore;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onStarted();
        void onRunLocated(RunInfo run);
        void onRunUpdated(RunInfo run);
        void onCompleted(RunInfo run);
        void onError(String message);
    }

    public interface SimpleCallback<T> {
        void onSuccess(T value);
        void onError(String message);
    }

    public static final class RunInfo {
        public final long id;
        public final long number;
        public final String status;
        public final String conclusion;
        public final String htmlUrl;
        public final String updatedAt;

        RunInfo(long id, long number, String status, String conclusion,
                String htmlUrl, String updatedAt) {
            this.id = id;
            this.number = number;
            this.status = safe(status);
            this.conclusion = safe(conclusion);
            this.htmlUrl = safe(htmlUrl);
            this.updatedAt = safe(updatedAt);
        }

        public boolean isFinished() {
            return "completed".equalsIgnoreCase(status);
        }

        public boolean isSuccessful() {
            return "success".equalsIgnoreCase(conclusion);
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
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

    private static final class HttpResult {
        final int code;
        final String body;

        HttpResult(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }

    public VeytrixAutopilotClient(Context context) {
        this.context = context.getApplicationContext();
        this.secureStore = new VeytrixSecureStore(this.context);
    }

    public boolean hasToken() {
        return secureStore.hasToken();
    }

    public String getTargetRepository() {
        return secureStore.getTargetRepository();
    }

    public void saveTargetRepository(String repository) {
        secureStore.saveTargetRepository(repository);
    }

    public void saveConnection(String token, String targetRepository)
            throws GeneralSecurityException {
        VeytrixInputValidator.validateRepository(targetRepository);
        secureStore.saveTargetRepository(targetRepository);
        secureStore.saveToken(token);
    }

    public void clearConnection() {
        secureStore.clearToken();
    }

    public void verifyConnection(
            String tokenOverride,
            String targetRepository,
            SimpleCallback<Verification> callback
    ) {
        executor.execute(() -> {
            try {
                VeytrixInputValidator.validateRepository(targetRepository);

                String token = tokenOverride == null || tokenOverride.trim().isEmpty()
                        ? secureStore.loadToken()
                        : tokenOverride.trim();

                if (token.isEmpty()) {
                    throw new IllegalStateException("GitHub token is required");
                }

                HttpResult user = request("GET", "/user", token, null);
                if (user.code != 200) {
                    throw apiError("GitHub user verification failed", user.code);
                }

                HttpResult target = request(
                        "GET",
                        "/repos/" + targetRepository,
                        token,
                        null
                );
                if (target.code != 200) {
                    throw apiError("Target repository access failed", target.code);
                }

                if (tokenOverride != null && !tokenOverride.trim().isEmpty()) {
                    secureStore.saveToken(token);
                }
                secureStore.saveTargetRepository(targetRepository);

                String login = new JSONObject(user.body).optString("login", "GitHub user");
                main.post(() -> callback.onSuccess(
                        new Verification(login, targetRepository, true)
                ));
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
            int verificationDepth,
            Callback callback
    ) {
        executor.execute(() -> {
            long startedAt = System.currentTimeMillis();

            try {
                VeytrixInputValidator.validateMission(mission);
                VeytrixInputValidator.validateRepository(targetRepository);
                VeytrixInputValidator.validateBranch(branch);
                VeytrixInputValidator.validateEngine(engine);
                VeytrixInputValidator.validateVerificationDepth(verificationDepth);

                String token = secureStore.loadToken();
                if (token.isEmpty()) {
                    throw new IllegalStateException("Connect GitHub first");
                }

                main.post(callback::onStarted);

                JSONObject inputs = new JSONObject()
                        .put("mission", mission.trim())
                        .put("target_repository", targetRepository.trim())
                        .put("branch", branch.trim())
                        .put("engine", engine)
                        .put("verification_depth", String.valueOf(verificationDepth));

                JSONObject body = new JSONObject()
                        .put("ref", "main")
                        .put("inputs", inputs);

                HttpResult dispatch = request(
                        "POST",
                        "/repos/" + OWNER + "/" + REPO
                                + "/actions/workflows/" + WORKFLOW + "/dispatches",
                        token,
                        body.toString()
                );

                if (dispatch.code != 204) {
                    throw apiError("Mission dispatch failed", dispatch.code);
                }

                RunInfo found = waitForRun(token, startedAt);
                if (found == null) {
                    throw new IllegalStateException(
                            "Workflow accepted, but its live run could not be located"
                    );
                }

                main.post(() -> callback.onRunLocated(found));

                RunInfo run = found;
                while (!run.isFinished()) {
                    Thread.sleep(3500L);
                    run = getRun(token, run.id);
                    RunInfo update = run;
                    main.post(() -> callback.onRunUpdated(update));
                }

                RunInfo complete = run;
                main.post(() -> callback.onCompleted(complete));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                main.post(() -> callback.onError("Mission monitoring was interrupted"));
            } catch (Exception e) {
                main.post(() -> callback.onError(safeMessage(e)));
            }
        });
    }

    public void fetchRecentRuns(int limit, SimpleCallback<List<RunInfo>> callback) {
        executor.execute(() -> {
            try {
                if (limit < 1 || limit > 20) {
                    throw new IllegalArgumentException("Run history limit must be 1-20");
                }

                String token = secureStore.loadToken();
                if (token.isEmpty()) {
                    throw new IllegalStateException("Connect GitHub first");
                }

                HttpResult result = request(
                        "GET",
                        "/repos/" + OWNER + "/" + REPO
                                + "/actions/workflows/" + WORKFLOW
                                + "/runs?per_page=" + limit,
                        token,
                        null
                );

                if (result.code != 200) {
                    throw apiError("Run history lookup failed", result.code);
                }

                JSONArray runs = new JSONObject(result.body)
                        .optJSONArray("workflow_runs");
                List<RunInfo> output = new ArrayList<>();
                if (runs != null) {
                    for (int i = 0; i < runs.length(); i++) {
                        output.add(toRunInfo(runs.getJSONObject(i)));
                    }
                }

                main.post(() -> callback.onSuccess(output));
            } catch (Exception e) {
                main.post(() -> callback.onError(safeMessage(e)));
            }
        });
    }

    public static final class ArtifactInfo {
        public final long id;
        public final String name;
        public final long sizeBytes;
        public final boolean expired;

        ArtifactInfo(long id, String name, long sizeBytes, boolean expired) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.sizeBytes = sizeBytes;
            this.expired = expired;
        }
    }

    public void fetchArtifacts(
            long runId,
            int limit,
            SimpleCallback<List<ArtifactInfo>> callback
    ) {
        executor.execute(() -> {
            try {
                if (runId <= 0) {
                    throw new IllegalArgumentException("Invalid workflow run");
                }
                if (limit < 1 || limit > 20) {
                    throw new IllegalArgumentException("Artifact limit must be 1-20");
                }

                String token = secureStore.loadToken();
                if (token.isEmpty()) {
                    throw new IllegalStateException("Connect GitHub first");
                }

                HttpResult result = request(
                        "GET",
                        "/repos/" + OWNER + "/" + REPO
                                + "/actions/runs/" + runId
                                + "/artifacts?per_page=" + limit,
                        token,
                        null
                );

                if (result.code != 200) {
                    throw apiError("Artifact lookup failed", result.code);
                }

                JSONArray artifacts = new JSONObject(result.body)
                        .optJSONArray("artifacts");
                List<ArtifactInfo> output = new ArrayList<>();

                if (artifacts != null) {
                    for (int i = 0; i < artifacts.length(); i++) {
                        JSONObject item = artifacts.getJSONObject(i);
                        output.add(new ArtifactInfo(
                                item.optLong("id"),
                                item.optString("name"),
                                item.optLong("size_in_bytes"),
                                item.optBoolean("expired", false)
                        ));
                    }
                }

                main.post(() -> callback.onSuccess(output));
            } catch (Exception e) {
                main.post(() -> callback.onError(safeMessage(e)));
            }
        });
    }

    public void fetchRun(long runId, SimpleCallback<RunInfo> callback) {
        executor.execute(() -> {
            try {
                String token = secureStore.loadToken();
                if (token.isEmpty()) {
                    throw new IllegalStateException("Connect GitHub first");
                }
                RunInfo info = getRun(token, runId);
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
            HttpResult result = request(
                    "GET",
                    "/repos/" + OWNER + "/" + REPO
                            + "/actions/workflows/" + WORKFLOW
                            + "/runs?per_page=10&event=workflow_dispatch",
                    token,
                    null
            );

            if (result.code != 200) {
                throw apiError("Run lookup failed", result.code);
            }

            JSONArray runs = new JSONObject(result.body).optJSONArray("workflow_runs");
            if (runs != null) {
                for (int j = 0; j < runs.length(); j++) {
                    JSONObject item = runs.getJSONObject(j);
                    if (parseGithubTime(item.optString("created_at")) >= startedAt - 120000L) {
                        return toRunInfo(item);
                    }
                }
            }

            Thread.sleep(2500L);
        }
        return null;
    }

    private RunInfo getRun(String token, long runId) throws Exception {
        HttpResult result = request(
                "GET",
                "/repos/" + OWNER + "/" + REPO + "/actions/runs/" + runId,
                token,
                null
        );
        if (result.code != 200) {
            throw apiError("Live run lookup failed", result.code);
        }
        return toRunInfo(new JSONObject(result.body));
    }

    private RunInfo toRunInfo(JSONObject object) {
        return new RunInfo(
                object.optLong("id"),
                object.optLong("run_number"),
                object.optString("status"),
                object.optString("conclusion"),
                object.optString("html_url"),
                object.optString("updated_at")
        );
    }

    private HttpResult request(String method, String path, String token, String body)
            throws Exception {
        URL url = new URL(API_BASE + path);
        if (!API_HOST.equalsIgnoreCase(url.getHost())) {
            throw new SecurityException("Unexpected GitHub API host");
        }

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        connection.setRequestProperty(
                "User-Agent",
                "Veytrix-Android/" + context.getPackageName()
        );
        connection.setRequestProperty("Authorization", "Bearer " + token);

        try {
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=utf-8"
                );
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }

            int code = connection.getResponseCode();
            InputStream stream = code >= 400
                    ? connection.getErrorStream()
                    : connection.getInputStream();
            return new HttpResult(code, read(stream));
        } finally {
            connection.disconnect();
        }
    }

    private String read(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
                if (output.length() > MAX_API_RESPONSE_CHARS) {
                    throw new SecurityException("GitHub API response is too large");
                }
            }
        }
        return output.toString().trim();
    }

    private static long parseGithubTime(String value) {
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static IllegalStateException apiError(String message, int code) {
        return new IllegalStateException(message + " (HTTP " + code + ")");
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isEmpty()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 240 ? message.substring(0, 240) : message;
    }
}
