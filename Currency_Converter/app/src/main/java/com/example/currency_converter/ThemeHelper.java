package com.example.currency_converter;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

/**
 * Utility class for managing app theme (Light/Dark mode).
 */
public class ThemeHelper {

    private static final String PREFS_KEY_DARK_MODE = "dark_mode";

    /**
     * Apply the theme based on saved preference.
     *
     * @param context The application context
     */
    public static void applyTheme(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isDarkMode = prefs.getBoolean(PREFS_KEY_DARK_MODE, false);
        applyTheme(isDarkMode);
    }

    /**
     * Apply the theme based on boolean flag.
     *
     * @param isDarkMode true for dark mode, false for light mode
     */
    public static void applyTheme(boolean isDarkMode) {
        int mode = isDarkMode
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /**
     * Check if dark mode is currently enabled.
     *
     * @param context The application context
     * @return true if dark mode is enabled
     */
    public static boolean isDarkMode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFS_KEY_DARK_MODE, false);
    }

    /**
     * Toggle the theme and save preference.
     *
     * @param context The application context
     * @return the new dark mode state
     */
    public static boolean toggleTheme(Context context) {
        boolean currentMode = isDarkMode(context);
        boolean newMode = !currentMode;
        setDarkMode(context, newMode);
        return newMode;
    }

    /**
     * Set dark mode preference.
     *
     * @param context The application context
     * @param isDarkMode true to enable dark mode
     */
    public static void setDarkMode(Context context, boolean isDarkMode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREFS_KEY_DARK_MODE, isDarkMode).apply();
        applyTheme(isDarkMode);
    }
}