package com.veytrix.autopilot;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class VeytrixSecureStore {
    private static final String PREFS = "veytrix_secure_v1";
    private static final String KEY_ALIAS = "veytrix_github_token_v1";
    private static final String TOKEN = "token_ciphertext";
    private static final String IV = "token_iv";
    private static final String TARGET_REPOSITORY = "target_repository";

    private final SharedPreferences prefs;

    public VeytrixSecureStore(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean hasToken() {
        return prefs.contains(TOKEN) && prefs.contains(IV);
    }

    public void saveToken(String token) throws GeneralSecurityException {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("GitHub token is required");
        }

        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.ENCRYPT_MODE,
                getOrCreateKey(),
                new GCMParameterSpec(128, iv)
        );

        byte[] ciphertext = cipher.doFinal(
                token.trim().getBytes(StandardCharsets.UTF_8)
        );

        prefs.edit()
                .putString(TOKEN, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .putString(IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply();
    }

    public String loadToken() throws GeneralSecurityException {
        String encoded = prefs.getString(TOKEN, "");
        String encodedIv = prefs.getString(IV, "");
        if (encoded.isEmpty() || encodedIv.isEmpty()) {
            return "";
        }

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                new GCMParameterSpec(128, Base64.decode(encodedIv, Base64.NO_WRAP))
        );

        byte[] plaintext = cipher.doFinal(
                Base64.decode(encoded, Base64.NO_WRAP)
        );
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    public void clearToken() {
        prefs.edit()
                .remove(TOKEN)
                .remove(IV)
                .apply();
    }

    public String getTargetRepository() {
        return prefs.getString(TARGET_REPOSITORY, "");
    }

    public void saveTargetRepository(String repository) {
        VeytrixInputValidator.validateRepository(repository);
        prefs.edit().putString(TARGET_REPOSITORY, repository).apply();
    }

    private SecretKey getOrCreateKey() throws GeneralSecurityException {
        try {
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            store.load(null);

            if (store.containsAlias(KEY_ALIAS)) {
                KeyStore.Entry entry = store.getEntry(KEY_ALIAS, null);
                if (!(entry instanceof KeyStore.SecretKeyEntry)) {
                    throw new GeneralSecurityException("Stored VEYTRIX key type is invalid");
                }
                return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            }

            KeyGenerator generator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore"
            );
            generator.init(
                    new KeyGenParameterSpec.Builder(
                            KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
                    )
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setUserAuthenticationRequired(false)
                            .build()
            );
            return generator.generateKey();
        } catch (IOException e) {
            throw new GeneralSecurityException("Unable to access Android keystore", e);
        }
    }
}