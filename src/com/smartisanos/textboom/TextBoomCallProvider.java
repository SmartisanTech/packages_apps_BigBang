package com.smartisanos.textboom;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.util.Log;

/**
 * Created by jayce on 16-10-19.
 */
public class TextBoomCallProvider extends ContentProvider {
    private static final String TAG = "TextBoomCallProvider";

    private static final UriMatcher matcher;
    private static final int ALL_ROWS = 1;
    private static final int SINGLE_ROW = 2;

    public static final String AUTHORITY = "com.smartisanos.textboom.call_method";
    public static final String PROVIDER_TYPE_NAME = "textboom_call";

    static {
        matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(AUTHORITY, PROVIDER_TYPE_NAME, ALL_ROWS);
        matcher.addURI(AUTHORITY, PROVIDER_TYPE_NAME + "/#", SINGLE_ROW);
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        int match = matcher.match(uri);
        switch (match) {
            case ALL_ROWS : {
                return "vnd.android.cursor.dir/vnd.smartisan.textboom.call";
            }
            case SINGLE_ROW : {
                return "vnd.android.cursor.item/vnd.smartisan.textboom.call";
            }
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }

    public static final String METHOD_STOP_OCR = "stop_ocr";

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (method != null) {
            Log.e(TAG, "TextBoomProvider handle method ["+method+"]");
            if (method.equals(METHOD_STOP_OCR)) {
                BoomOcrActivity.sBoomCancel = true;
                if (null != BoomOcrActivity.getInstance()) {
                    BoomOcrActivity.getInstance().post(new Runnable() {
                        @Override
                        public void run() {
                            if (null != BoomOcrActivity.getInstance()) {
                                BoomOcrActivity.getInstance().cancelOcr();
                            }
                        }
                    });
                }
            }
        }
        return super.call(method, arg, extras);
    }
}
