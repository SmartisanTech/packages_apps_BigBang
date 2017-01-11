package com.smartisanos.textboom.util;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigUtils {

    public static final String SP_FILE_NAME_UPDATE = "textboom_update_config";
    public static final String KEY_LAST_CHECK_UPDATE_DICT_TIME = "last_check_update_dict_time";
    public static final String KEY_DICT_VERSION = "dict_version";
    public static final long UPDATE_DICT_PERIOD_DAY = 7 * 24 * 60 * 60 * 1000;

    private static SharedPreferences sSharedPreferences;

    public static void init(Context context) {
        sSharedPreferences = context
                .getSharedPreferences(SP_FILE_NAME_UPDATE, Context.MODE_PRIVATE);
    }

    public static boolean isNeedCheckUpdateDict() {
        long currentTime = System.currentTimeMillis();
        long lastCheckTime = sSharedPreferences.getLong(KEY_LAST_CHECK_UPDATE_DICT_TIME, 0);
        return (currentTime - lastCheckTime) > UPDATE_DICT_PERIOD_DAY;
    }

    public static void setCheckUpdateQATime(long time) {
        sSharedPreferences.edit().putLong(KEY_LAST_CHECK_UPDATE_DICT_TIME, time).apply();
    }

    public static int getDictVersion() {
        return sSharedPreferences.getInt(KEY_DICT_VERSION, 0);
    }

    public static void setDictVersion(int value) {
        sSharedPreferences.edit().putInt(KEY_DICT_VERSION, value).apply();
    }

}
