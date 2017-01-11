package com.smartisanos.textboom.network;

import android.os.Handler;
import android.os.Looper;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;


/**
 * Use OkHttp for network request Utils.
 */
public class OkHttpClientManager {

    private static OkHttpClientManager sInstance;
    private OkHttpClient mOkHttpClient;
    private Handler mDelivery;

    private OkHttpClientManager() {
        mOkHttpClient = new OkHttpClient();
        mDelivery = new Handler(Looper.getMainLooper());
    }

    public static OkHttpClientManager getInstance() {
        if (sInstance == null) {
            synchronized (OkHttpClientManager.class) {
                if (sInstance == null) {
                    sInstance = new OkHttpClientManager();
                }
            }
        }
        return sInstance;
    }

    public void post(String url, Map<String, String> paramsMap, ResultCallback callback) {
        if (paramsMap != null) {
            FormEncodingBuilder builder = new FormEncodingBuilder();
            Iterator<String> keys = paramsMap.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                builder.add(key, paramsMap.get(key));
            }
            Request req = new Request.Builder().url(url).post(builder.build()).build();
            deliveryResult(callback, req);
        }
    }

    private void deliveryResult(final ResultCallback callback, Request request) {
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(final Request request, final IOException e) {
                sendFailedStringCallback(request, e, callback);
            }

            @Override
            public void onResponse(final Response response) {
                try {
                    if (response.isSuccessful()) {
                        final String string = response.body().string();
                        sendSuccessResultCallback(string, callback);
                    } else {
                        throw new IOException("response code :" + response.code());
                    }
                } catch (IOException e) {
                    sendFailedStringCallback(response.request(), e, callback);
                }
            }
        });
    }

    private void sendFailedStringCallback(final Request request, final Exception e,
            final ResultCallback callback) {
        mDelivery.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null)
                    callback.onError(request, e);
            }
        });
    }

    private void sendSuccessResultCallback(final String response, final ResultCallback callback) {
        mDelivery.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onResponse(response);
                }
            }
        });
    }

    public static abstract class ResultCallback {
        public abstract void onError(Request request, Exception e);

        public abstract void onResponse(String response);
    }

}
