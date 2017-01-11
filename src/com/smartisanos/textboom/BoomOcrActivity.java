package com.smartisanos.textboom;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.intsig.csopen.sdk.CSOcrOpenApiHandler;
import com.intsig.csopen.sdk.CSOcrResult;
import com.intsig.csopen.sdk.CSOpenAPI;
import com.intsig.csopen.sdk.CSOpenApiFactory;
import com.intsig.csopen.sdk.OCRLanguage;
import com.smartisanos.textboom.util.LogUtils;

import android.view.SurfaceControl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by jayce on 16-10-11.
 */
public class BoomOcrActivity extends Activity {

    private final static String TAG = "BoomOcrActivity";

    private String OCR_IMAGE_PATH = null;
    private static final String OCR_IMAGE_DIR = ".boom";

    private static final String PREFS_KEY_OCR = "ocr_key";
    private static final String PREFS_KEY_OCR_WONG = "ocr_key_wrong";

    private CSOpenAPI mCSOcrOpenApi;

    private Toast mToastStop;

    private Runnable mOcrRunnable = new Runnable() {
        @Override
        public void run() {
            if (!sBoomCancel) {
                startOcr();
            }
        }
    };

    private static BoomOcrActivity sSelf;

    /**
     * ocr language value
     */
    private final static int[] LANGUAGE_VALUE={
            OCRLanguage.LANGUAGE_English,
            OCRLanguage.LANGUAGE_ChsSimp,
            OCRLanguage.LANGUAGE_ChsTrad,
            OCRLanguage.LANGUAGE_Japan,
            OCRLanguage.LANGUAGE_Korean,
            OCRLanguage.LANGUAGE_France,
            OCRLanguage.LANGUAGE_Spain,
            OCRLanguage.LANGUAGE_Portuguese,
            OCRLanguage.LANGUAGE_German,
            OCRLanguage.LANGUAGE_Italy,
            OCRLanguage.LANGUAGE_Dutch,
            OCRLanguage.LANGUAGE_Swedish,
            OCRLanguage.LANGUAGE_Finnish,
            OCRLanguage.LANGUAGE_Danish,
            OCRLanguage.LANGUAGE_Norwegian,
            OCRLanguage.LANGUAGE_Hungarian
    };

    private static final int REQ_CODE_OCR_IMAGE = 1;

    private FrameLayout mLoopAnimFrame;
    private ImageView mLoopRotateImage;
    private FrameLayout mContentFrame;

    private AnimatorSet mLoopAnimation;
    private AnimatorSet mTouchAnimation;
    private AnimatorSet mCircleAnimation;
    private boolean mAnimating = false;
    private boolean mTouchAnimating = false;
    private boolean mLoopAnimating = false;
    private boolean mCircleAnimating = false;
    private boolean mOcrResult = false;

    private static final float LOOP_SCALE_FROM = 1.15f;
    private static final float LOOP_SCALE_TO = 1.1f;
    private static final long SCALE_DURATION = 600;

    private String mOcrText = null;
    private float mTouchX;
    private float mTouchY;
    private boolean mFullscreen = false;

    static boolean sBoomCancel = false;

    private Handler mHandler;

    private String mKey;
    private SharedPreferences mPrefs;
    private static final int[] KEY_ERROR = {4002, 4003};
    private String mPackage;
    private int[] mOffset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogUtils.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        sSelf = this;
        // Do not do ocr in landscape for now
        Configuration cf = getResources().getConfiguration();
        if (cf.orientation == cf.ORIENTATION_LANDSCAPE) {
            finish();
            return;
        }
        if (sBoomCancel) {
            finish();
            return;
        }
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        readKey();
        mHandler = new Handler();
        if(mCSOcrOpenApi == null) {
            mCSOcrOpenApi = CSOpenApiFactory.createCSOpenApi(this, mKey, null);
            if (null == mCSOcrOpenApi) {
                LogUtils.e(TAG, "Create api failed");
                finish();
                return;
            }
            boolean scannerAvailable = mCSOcrOpenApi.isCamScannerAvailable();
            if (!scannerAvailable) {
                finish();
                return;
            }
        }

        OCR_IMAGE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + OCR_IMAGE_DIR + "/imageboom.jpg";

        setContentView(R.layout.boom_ocr_layout);
        mLoopAnimFrame = (FrameLayout) findViewById(R.id.anim_loop);
        mLoopAnimFrame.setVisibility(View.INVISIBLE);
        mLoopRotateImage = (ImageView) findViewById(R.id.loop_rotate);
        mContentFrame = (FrameLayout) findViewById(R.id.click_layout);
        mContentFrame.requestFocus();
        mContentFrame.setClickable(true);
        mContentFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopOcr();
            }
        });
        mTouchX = getIntent().getIntExtra("boom_startx", 0);
        mTouchY = getIntent().getIntExtra("boom_starty", 0);
        mFullscreen = getIntent().getBooleanExtra("boom_fullscreen", false);
        mPackage = getIntent().getStringExtra("caller_pkg");
        int offx = getIntent().getIntExtra("boom_offsetx", 0);
        int offy = getIntent().getIntExtra("boom_offsety", 0);
        mOffset = new int[] {offx, offy};
        LogUtils.d(TAG, "touchX:" + mTouchX + ", touchY:" + mTouchY + ", fullscreen:" + mFullscreen);

        prepareOcr();

        mLoopAnimFrame.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (!mAnimating) {
                    final int l = left;
                    final int t = top;
                    final int r = right;
                    final int b = bottom;
                    mLoopAnimFrame.post(new Runnable() {
                        @Override
                        public void run() {
                            mLoopAnimFrame.setTranslationX(mTouchX - (r - l) / 2f);
                            mLoopAnimFrame.setTranslationY(mTouchY - (b - t) / 2f);
                            startTouchBoomAnimation();
                        }
                    });
                }
            }
        });

        mContentFrame.removeCallbacks(mOcrRunnable);
        mContentFrame.postDelayed(mOcrRunnable, OCR_DELAY);
    }

    public static final long OCR_DELAY = 300;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtils.d(TAG, "onActivityResult:" + requestCode);
        if(requestCode == REQ_CODE_OCR_IMAGE){
            mOcrResult = true;
            boolean result = mCSOcrOpenApi.handleOCRResult(requestCode, resultCode, data, new CSOcrOpenApiHandler() {

                @Override
                public void onSuccess(CSOcrResult result) {
                    LogUtils.d(TAG, "onSuccess result:" + result);
                    String ocrKey = mPrefs.getString(PREFS_KEY_OCR, null);
                    if(null == ocrKey || !ocrKey.equals(mKey)) {
                        mPrefs.edit().putString(PREFS_KEY_OCR, mKey).commit();
                    }
                    String wrongKey = mPrefs.getString(PREFS_KEY_OCR_WONG, "");
                    if (null != mKey && wrongKey.contains(mKey)) {
                        wrongKey = wrongKey.replace(":" + mKey, "");
                        mPrefs.edit().putString(PREFS_KEY_OCR_WONG, wrongKey).commit();
                    }
                    if(result != null){
                        String ocrtext = result.getOcrText();
                        //LogUtils.d(TAG, "text:" + ocrtext);
                        String decode = ocrtext;
                        //LogUtils.d(TAG, "decode:" + decode);
                        mOcrText = decode.trim();
                        if (0 < mOcrText.length()) {
                            stopTouchAnimation();
                            stopLoopAnimation();
                            startCircleAnimation();
                        } else {
                            Toast.makeText(BoomOcrActivity.this, R.string.a_msg_no_words, Toast.LENGTH_SHORT).show();
                            stopOcr();
                        }
                    } else {
                        Toast.makeText(BoomOcrActivity.this, R.string.a_msg_no_words, Toast.LENGTH_SHORT).show();
                        stopOcr();
                    }
                }

                @Override
                public void onError(int errorCode) {
                    LogUtils.e(TAG, "onError errorCode:" + errorCode);
                    for (int error : KEY_ERROR) {
                        if (error == errorCode) {
                            String wrongKey = mPrefs.getString(PREFS_KEY_OCR_WONG, "");
                            if (null != mKey && !wrongKey.contains(mKey)) {
                                wrongKey = wrongKey + ":" + mKey;
                                mPrefs.edit().putString(PREFS_KEY_OCR_WONG, wrongKey).commit();
                            }
                            break;
                        }
                    }
                    stopOcr();
                }
            });
            LogUtils.d(TAG, "result=" + result);
        }
    }

    private boolean mOcrStarted = false;
    private void startOcr() {
        mOcrStarted = true;
        int language = OCRLanguage.LANGUAGE_English | OCRLanguage.LANGUAGE_ChsSimp;

        if(mCSOcrOpenApi == null) {
            mCSOcrOpenApi = CSOpenApiFactory.createCSOpenApi(this, mKey, null);
        }
        mCSOcrOpenApi.startActivityForOCR(this, REQ_CODE_OCR_IMAGE, language, OCR_IMAGE_PATH, false, !mFullscreen);
        mOcrResult = false;
    }

    private void startLoopAnimation() {
        if (sBoomCancel && !mOcrStarted || mOcrResult) {
            return;
        }
        ValueAnimator scaleIn = new ValueAnimator().ofFloat(LOOP_SCALE_FROM, LOOP_SCALE_TO);
        scaleIn.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatorValue = (Float)animation.getAnimatedValue();
                mLoopAnimFrame.setScaleX(animatorValue);
                mLoopAnimFrame.setScaleY(animatorValue);
            }
        });

        scaleIn.setDuration(SCALE_DURATION);
        scaleIn.setInterpolator(new SineInoutInterpolator());

        ValueAnimator scaleOut = new ValueAnimator().ofFloat(LOOP_SCALE_TO, LOOP_SCALE_FROM);
        scaleOut.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatorValue = (Float)animation.getAnimatedValue();
                mLoopAnimFrame.setScaleX(animatorValue);
                mLoopAnimFrame.setScaleY(animatorValue);
            }
        });

        scaleOut.setDuration(SCALE_DURATION);
        scaleOut.setInterpolator(new SineInoutInterpolator());

        AnimatorSet scaleAnimation = new AnimatorSet();
        scaleAnimation.playSequentially(scaleIn, scaleOut);
        scaleAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mLoopAnimating && !mOcrResult) {
                    animation.start();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        ValueAnimator rotateAnimation = new ValueAnimator().ofInt(0, 360);
        mLoopAnimFrame.setPivotX(mLoopAnimFrame.getWidth() / 2f);
        mLoopAnimFrame.setPivotY(mLoopAnimFrame.getHeight() / 2f);
        rotateAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animatorValue = (Integer) animation.getAnimatedValue();

                mLoopAnimFrame.setRotation(animatorValue);
            }
        });
        rotateAnimation.setDuration(SCALE_DURATION * 2);
        rotateAnimation.setRepeatMode(ValueAnimator.RESTART);
        rotateAnimation.setRepeatCount(ValueAnimator.INFINITE);
        rotateAnimation.setInterpolator(new LinearInterpolator());

        mLoopAnimation = new AnimatorSet();
        mLoopAnimation.playTogether(scaleAnimation, rotateAnimation);
        mLoopAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mLoopAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mLoopAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mLoopAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mLoopAnimation.start();
    }

    private void stopLoopAnimation() {
        if (null != mLoopAnimation) {
            mLoopAnimation.cancel();
            mLoopAnimation = null;
        }
        mLoopAnimating = false;
    }

    private void stopCircleAnimation() {
        if (null != mCircleAnimation) {
            mCircleAnimation.cancel();
            mCircleAnimation = null;
        }
        mCircleAnimating = false;
    }

    private void stopAllAnimation() {
        stopTouchAnimation();
        stopLoopAnimation();
        stopCircleAnimation();
    }

    public void onDestroy() {
        LogUtils.d(TAG, "onDestroy");
        sBoomCancel = false;
        super.onDestroy();
        stopAllAnimation();
        if (null != mContentFrame) {
            mContentFrame.removeCallbacks(mOcrRunnable);
        }

        if (null != mToastStop) {
            mToastStop.cancel();
            mToastStop = null;
        }
        mAnimating = false;
        mOcrStarted = false;
        if (null != sSelf && sSelf == this) {
            sSelf = null;
        }
    }

    public static BoomOcrActivity getInstance() {
        return sSelf;
    }

    public static final float TOUCH_SCALE_FROM = 2f;
    public static final float TOUCH_SCALE_TO_1 = 0.2f;
    public static final float TOUCH_SCALE_TO_2 = 1.15f;
    public static final long TOUCH_DELAY = 0;

    public static final long SCALE_1_DURATION = 400;
    public static final long SCALE_2_DURATION = 200;

    public static final float TOUCH_ALPHA_FROM = 0.4f;
    public static final float TOUCH_ALPHA_TO = 1f;
    private void startTouchBoomAnimation() {
        LogUtils.d(TAG, "startTouchBoomAnimation");
        if (sBoomCancel) {
            return;
        }
        mAnimating = true;
        mLoopAnimFrame.setVisibility(View.INVISIBLE);
        mLoopRotateImage.setVisibility(View.INVISIBLE);
        // init scale to TOUCH_SCALE_FROM
        mLoopAnimFrame.setScaleX(TOUCH_SCALE_FROM);
        mLoopAnimFrame.setScaleY(TOUCH_SCALE_FROM);
        mLoopAnimFrame.setAlpha(TOUCH_ALPHA_FROM);

        AnimatorSet setAnim = new AnimatorSet();
        // anim scale from TOUCH_SCALE_FROM to TOUCH_SCALE_TO_1
        ValueAnimator scaleAnimation = new ValueAnimator().ofFloat(TOUCH_SCALE_FROM, TOUCH_SCALE_TO_1);
        scaleAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatorValue = (Float)animation.getAnimatedValue();

                mLoopAnimFrame.setScaleX(animatorValue);
                mLoopAnimFrame.setScaleY(animatorValue);
            }
        });
        scaleAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                LogUtils.d(TAG, "touch scale start");
                mTouchAnimating = true;
                mLoopAnimFrame.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        scaleAnimation.setDuration(SCALE_1_DURATION);

        ValueAnimator alphaAnimation = new ValueAnimator().ofFloat(TOUCH_ALPHA_FROM, TOUCH_ALPHA_TO);
        alphaAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatorValue = (Float)animation.getAnimatedValue();

                mLoopAnimFrame.setAlpha(animatorValue);
            }
        });

        alphaAnimation.setDuration(SCALE_1_DURATION);
        setAnim.playTogether(scaleAnimation, alphaAnimation);

        // anim scale from TOUCH_SCALE_TO_1 to TOUCH_SCALE_TO_2
        ValueAnimator scaleAnimation2 = new ValueAnimator().ofFloat(TOUCH_SCALE_TO_1, TOUCH_SCALE_TO_2);
        scaleAnimation2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatorValue = (Float)animation.getAnimatedValue();

                mLoopAnimFrame.setScaleX(animatorValue);
                mLoopAnimFrame.setScaleY(animatorValue);
            }
        });

        scaleAnimation2.setDuration(SCALE_2_DURATION);

        mTouchAnimation = new AnimatorSet();
        mTouchAnimation.playSequentially(setAnim, scaleAnimation2);
        mTouchAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                LogUtils.d(TAG, "touch scale end");
                if (!mTouchAnimating) {
                    return;
                }
                mLoopRotateImage.setVisibility(View.VISIBLE);
                mLoopAnimFrame.setAlpha(1f);
                if (!mOcrResult) {
                    startLoopAnimation();
                }
                mTouchAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                LogUtils.d(TAG, "touch scale cancel");
                mTouchAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mTouchAnimation.setInterpolator(new CubicInInterpolator());
        mTouchAnimation.setStartDelay(TOUCH_DELAY);
        mTouchAnimation.start();
    }

    private void stopTouchAnimation() {
        if (null != mTouchAnimation) {
            mTouchAnimation.cancel();
            mTouchAnimation = null;
        }
        mTouchAnimating = false;
    }

    private static final float CIRCLE_END_SCALE = 4f;
    private static final long CIRCLE_END_DURATION = 100;
    private void startCircleAnimation() {
        mLoopRotateImage.setVisibility(View.INVISIBLE);
        float currentScale = mLoopAnimFrame.getScaleX();

        mCircleAnimation = new AnimatorSet();

        ValueAnimator scaleAnimation = new ValueAnimator().ofFloat(currentScale, CIRCLE_END_SCALE);
        scaleAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatorValue = (Float)animation.getAnimatedValue();

                mLoopAnimFrame.setScaleX(animatorValue);
                mLoopAnimFrame.setScaleY(animatorValue);
            }
        });

        scaleAnimation.setDuration(CIRCLE_END_DURATION);

        ValueAnimator alphaAnimation = new ValueAnimator().ofFloat(1f, 0f);
        alphaAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatorValue = (Float)animation.getAnimatedValue();

                mLoopAnimFrame.setAlpha(animatorValue);
            }
        });

        alphaAnimation.setDuration(CIRCLE_END_DURATION);
        mCircleAnimation.playTogether(scaleAnimation, alphaAnimation);
        mCircleAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mCircleAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mCircleAnimating) {
                    return;
                }
                mCircleAnimating = false;
                startBoomActivity();
                finish();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCircleAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mCircleAnimation.start();
    }

    private void startBoomActivity() {
        LogUtils.e(TAG, "startBoomActivity");
        Intent intent = new Intent(this, BoomActivity.class);
        intent.putExtra(Intent.EXTRA_TEXT, mOcrText);
        intent.putExtra("boom_startx", (int) mTouchX);
        intent.putExtra("boom_starty", (int) mTouchY);
        intent.putExtra("boom_index", 0);
        intent.putExtra("boom_image", "image");
        intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        }
        startActivity(intent, ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle());
        mOcrStarted = false;
    }

    public void post(Runnable run) {
        if (null != mHandler && null != run) {
            mHandler.post(run);
        }
    }

    public void cancelOcr() {
        LogUtils.e(TAG, "cancelOcr from touch");
        stopOcr();
    }

    private void stopOcr() {
        LogUtils.e(TAG, "stop ocr");
        stopAllAnimation();
        if (null != mContentFrame) {
            mContentFrame.removeCallbacks(mOcrRunnable);
        }

        if (null != mToastStop) {
            mToastStop.cancel();
            mToastStop = null;
        }
        mAnimating = false;
        mOcrStarted = false;
        finish();
    }

    private void prepareOcr() {
        LogUtils.e(TAG, "prepare ocr, take screenshot");
        if (!takeScreenShot()) {
            stopOcr();
            return;
        }
    }

    public static final int SCALE_SCREENSHOT = 2;
    private boolean takeScreenShot() {
        int w = getResources().getInteger(R.integer.screen_width);
        int h = getResources().getInteger(R.integer.screen_height);
        Bitmap screen = SurfaceControl.screenshot(w, h);
        if (null == screen) {
            return false;
        }
        Bitmap bm = adjustScreenshotFor(screen);
        File f = new File(OCR_IMAGE_PATH);
        try {
            File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + OCR_IMAGE_DIR);
            if (!dir.exists()) {
                dir.mkdir();
            }
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
        } catch (IOException e) {
            bm.recycle();
            e.printStackTrace();
            return false;
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            bm.recycle();
            return false;
        }
        bm.compress(Bitmap.CompressFormat.JPEG, 80, fOut);
        try {
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            bm.recycle();
        }
        return true;
    }

    private void readKey() {
        String ocrKey = mPrefs.getString(PREFS_KEY_OCR, null);
        LogUtils.e(TAG, "mPrefs key:" + ocrKey);
        if(null != ocrKey) {
            mKey = ocrKey;
        }
        if (null == mKey) {
            String[] keyArray = getResources().getStringArray(R.array.ocr_key);
            String wrongKey = mPrefs.getString(PREFS_KEY_OCR_WONG, "");
            LogUtils.e(TAG, "mPrefs wrongKey:" + wrongKey);
            for (String key : keyArray) {
                if (!wrongKey.contains(key)) {
                    mKey = key;
                    LogUtils.e(TAG, "mKey:" + mKey);
                    break;
                }
            }
            if (null == mKey) {
                mKey = keyArray[0];
                mPrefs.edit().putString(PREFS_KEY_OCR_WONG, "").commit();
            }
        }
        if (null == mKey) {
            LogUtils.e(TAG, "Do not have a correct ocr key string!");
            finish();
        }
    }

    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        stopOcr();
    }

    private static final String PKG_GALLERY = "com.android.gallery3d";
    private Bitmap adjustScreenshotFor(Bitmap screenshot) {
        int w = getResources().getInteger(R.integer.screen_width);
        int h = getResources().getInteger(R.integer.screen_height);
        int status_bar_height = getResources().getInteger(R.integer.status_bar_height);
        int top = status_bar_height;
        int bottom = 0;
        int left = 0;
        int right = 0;
        if (0 == mOffset[0] && 0 == mOffset[1]) {
            // Not in one hand or sidebar mode
            if (PKG_GALLERY.equals(mPackage) && !mFullscreen) {
                top = getResources().getInteger(R.integer.gallery_top);
                bottom = getResources().getInteger(R.integer.gallery_bottom);
            }
        } else {
            // Screen scale in one hand or sidebar mode
            float scaleFactor = mOffset[1] / (float) h;
            int sideh = mOffset[1];
            int sidew = (int) (scaleFactor * w);
            if (PKG_GALLERY.equals(mPackage) && !mFullscreen) {
                int gtop = (int) (getResources().getInteger(R.integer.gallery_top) * (1 - scaleFactor));
                int gbottom = (int) (getResources().getInteger(R.integer.gallery_bottom) * (1 - scaleFactor));
                top = sideh + gtop;
                bottom = gbottom;
            } else {
                top = sideh + (int) ((1 - scaleFactor) * status_bar_height);
            }
            if (0 == mOffset[0]) {
                right = sidew;
            } else {
                left = sidew;
            }
        }
        LogUtils.d(TAG, "top:" + top + ", bottom:" + bottom + ", left:" + left + ",right:" + right);
        int aw = (screenshot.getWidth() - left - right) / SCALE_SCREENSHOT;
        int ah = (screenshot.getHeight() - top - bottom) / SCALE_SCREENSHOT;
        Bitmap bm = Bitmap.createBitmap(aw, ah, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        Paint p = new Paint();
        p.setFilterBitmap(true);
        p.setAntiAlias(true);
        canvas.drawBitmap(screenshot, new Rect(left, top, w - right, h - bottom), new Rect(0, 0, aw, ah), p);
        screenshot.recycle();
        return bm;
    }

}
