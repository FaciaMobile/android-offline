package com.facia.faciasdk.Utils;

import android.content.Context;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

import com.facia.faciasdk.Logs.Webhooks;
import com.facia.faciasdk.R;

import org.json.JSONObject;

public class AppColors {
    private final int darkTextColor;
    private final int lightTextColor;
    private final int dialogButtonTextColor;
    private final int buttonBgColor;
    private final int buttonTextColor;



    public AppColors(JSONObject json, Context context) {
        if (json == null) json = new JSONObject();

        darkTextColor = parseColor(json, context, "dark_text_color", R.color.dark_text_color);
        lightTextColor = parseColor(json, context, "light_text_color", R.color.light_text_color);
        dialogButtonTextColor = parseColor(json, context, "dialog_button_text_color", R.color.dialog_button_text_color);
        buttonBgColor = parseColor(json, context, "button_bg_color", R.color.button_bg_color);
        buttonTextColor = parseColor(json, context, "button_text_color", R.color.button_text_color);
    }

    private int parseColor(JSONObject json, Context context, String key, int defaultResId) {
        String raw = json.optString(key, null);
        if (raw != null && !raw.isEmpty()) {
            String cleaned = raw.trim().replaceAll("^#+", "#"); // removes all leading #, then adds exactly one
            if (cleaned.matches("#[0-9a-fA-F]{3}([0-9a-fA-F]{3})?([0-9a-fA-F]{2})?([0-9a-fA-F]{2})?")) {
                try {
                    return Color.parseColor(cleaned);
                } catch (Exception ignored) {}
            }
        }
        return ContextCompat.getColor(context, defaultResId);
    }

    // Getters
    public int getDarkTextColor() { return darkTextColor; }
    public int getLightTextColor() { return lightTextColor; }
    public int getDialogButtonTextColor() { return dialogButtonTextColor; }
    public int getButtonBgColor() { return buttonBgColor; }
    public int getButtonTextColor() { return buttonTextColor; }

}
