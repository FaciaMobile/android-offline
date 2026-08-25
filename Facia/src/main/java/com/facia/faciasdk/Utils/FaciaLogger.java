package com.facia.faciasdk.Utils;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.ResponseBody;
import okio.BufferedSource;
import retrofit2.Response;

/**
 * Centralized logging for the Facia SDK.
 * <p>
 * Every debug/log statement in the SDK (Log.d/e/i/w/v, printStackTrace, API responses and
 * API error messages, and any other debug output) routes through this class so that all of
 * it can be toggled with a single flag.
 * <p>
 * When {@link #isDebug} is {@code false} (the default) nothing is printed. Set it to
 * {@code true} to enable logs, e.g. {@code FaciaLogger.isDebug = true;}
 */
public class FaciaLogger {

    /**
     * Single master switch for all SDK logging.
     * {@code true}  -> logs are printed.
     * {@code false} -> no logs are printed (default).
     */
    public static boolean isDebug = false;

    /** Fallback tag used when a caller does not provide one (e.g. plain stack traces). */
    private static final String DEFAULT_TAG = "FaciaSDK";

    private FaciaLogger() {
        // no instances
    }

    public static void d(String tag, String message) {
        if (isDebug) {
            Log.d(tag, safe(message));
        }
    }

    public static void e(String tag, String message) {
        if (isDebug) {
            Log.e(tag, safe(message));
        }
    }

    public static void e(String tag, String message, Throwable throwable) {
        if (isDebug) {
            Log.e(tag, safe(message), throwable);
        }
    }

    public static void i(String tag, String message) {
        if (isDebug) {
            Log.i(tag, safe(message));
        }
    }

    public static void w(String tag, String message) {
        if (isDebug) {
            Log.w(tag, safe(message));
        }
    }

    public static void v(String tag, String message) {
        if (isDebug) {
            Log.v(tag, safe(message));
        }
    }

    /**
     * Centralized replacement for {@code Throwable.printStackTrace()}.
     * Only prints when {@link #isDebug} is enabled.
     */
    public static void e(Throwable throwable) {
        e(DEFAULT_TAG, throwable);
    }

    public static void e(String tag, Throwable throwable) {
        if (isDebug && throwable != null) {
            Log.e(tag, safe(throwable.getMessage()), throwable);
        }
    }

    private static String safe(String message) {
        return message == null ? "" : message;
    }

    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Serializes any object (e.g. an API response body) to a JSON string for readable logging.
     * Falls back to {@code String.valueOf(obj)} if the object cannot be serialized.
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return JSON.toJson(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    /**
     * Returns the loggable content of a Retrofit response. For a successful response the parsed
     * body is serialized to JSON; for an unsuccessful response the raw error body is returned
     * (that is where the server puts the payload when {@code body()} is null, e.g. HTTP 4xx/5xx).
     */
    public static String responseBody(Response<?> response) {
        if (response == null) {
            return "null";
        }
        if (response.isSuccessful()) {
            return toJson(response.body());
        }
        return peekErrorBody(response.errorBody());
    }

    /**
     * Reads an error {@link ResponseBody} <b>without consuming it</b>, so existing code that later
     * calls {@code errorBody().string()} still works. Uses a peeking source, so the underlying
     * buffered bytes are left untouched.
     */
    public static String peekErrorBody(ResponseBody body) {
        if (body == null) {
            return "null";
        }
        try {

            BufferedSource source = body.source();
            source.request(Long.MAX_VALUE); // buffer the entire body
            return source.peek().readUtf8();
        } catch (Exception e) {
            return "<unreadable error body>";
        }
    }
}
