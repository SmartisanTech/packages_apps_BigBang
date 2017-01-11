
package com.smartisanos.textboom.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.LayoutInflater;
import android.view.View;

import com.smartisanos.textboom.R;

public class Utils {

    public static boolean isWIFIConnected(Context context) {
        ConnectivityManager conMgr = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        }
        return false;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = conn.getActiveNetworkInfo();
        return info != null && info.isAvailable();
    }

    public static boolean copyFileOrDir(AssetManager assetManager, String assetsDir, String destDir) {
        try {
            LogUtils.d("copyFileOrDir() assets assetsDir= " + assetsDir);
            String[] assets = assetManager.list(assetsDir);
            if (assets.length == 0) {
                copyFile(assetManager, assetsDir, destDir);
            } else {
                String fullPath = destDir + assetsDir;
                File dir = new File(fullPath);
                if (!dir.exists() && !dir.mkdirs()) {
                    dir.mkdirs();
                }
                for (int i = 0; i < assets.length; ++i) {
                    String path = assetsDir.length() == 0 ? "" : assetsDir + "/";
                    if (!copyFileOrDir(assetManager, path + assets[i], destDir)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (IOException ex) {
            LogUtils.e("I/O Exception", ex);
            return false;
        }
    }

    private static void copyFile(AssetManager assetManager, String filename, String destDir) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(filename);
            String newFileName = destDir + filename;
            out = new FileOutputStream(newFileName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            LogUtils.e(e.getMessage());
        }
    }

    public static boolean isPackageInstalled(Context context, String pkg) {
        if (pkg == null) return false;
        PackageInfo info = null;
        try {
            info = context.getPackageManager().getPackageInfo(pkg, 0);
        } catch (PackageManager.NameNotFoundException e) {
        }
        return info != null;
    }

    public static View inflateListTransparentHeader(Context context) {
        return LayoutInflater.from(context).inflate(R.layout.settings_list_header_footer_view, null);
    }

}
