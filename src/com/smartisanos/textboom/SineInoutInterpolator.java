package com.smartisanos.textboom;

import android.view.animation.Interpolator;

/**
 * Created by jayce on 16-10-13.
 */
public class SineInoutInterpolator implements Interpolator {
    private static final float PI = 3.14159265f;

    @Override
    public float getInterpolation(float input) {
        return -0.5f * ((float) Math.cos(PI * input) - 1);
    }
}