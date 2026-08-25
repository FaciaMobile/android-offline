package com.facia.faciasdk.Camera.CameraXHelpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.camera.video.Quality;

public class QualityPreferences {
    private static final String PREF_VIDEO_QUALITY = "video_quality";
    private static final String PREF_IMAGE_COMPRESSION = "image_compression";

    // Hardcoded values
    private static final String VIDEO_QUALITY = "HD";
    private static final int IMAGE_COMPRESSION = 70;
    private static final int DEFAULT_COMPRESSION = 75;

    private final SharedPreferences sharedPreferences;

    public QualityPreferences(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        initializeValues();
    }

    private void initializeValues() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_VIDEO_QUALITY, VIDEO_QUALITY);
        editor.putInt(PREF_IMAGE_COMPRESSION, IMAGE_COMPRESSION);
        editor.apply();
    }

    public String getVideoQuality() {
        return sharedPreferences.getString(PREF_VIDEO_QUALITY, VIDEO_QUALITY);
    }

    public Quality getQualityFromString(String qualityString) {
        return switch (qualityString) {
            case "SD" -> Quality.SD;
            case "HD" -> Quality.HD;
            case "FHD" -> Quality.FHD;
            case "UHD" -> Quality.UHD;
            case "HIGHEST" -> Quality.HIGHEST;
            default -> Quality.SD;
        };
    }

    public int getImageCompression() {
        int compressionValue = sharedPreferences.getInt(PREF_IMAGE_COMPRESSION, IMAGE_COMPRESSION);
        if (compressionValue < 1 || compressionValue > 100) {
            return DEFAULT_COMPRESSION;
    }
        return compressionValue;
    }
}