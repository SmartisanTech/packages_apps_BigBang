package com.smartisanos.textboom;

import android.view.animation.Interpolator;

/**
 * Created by jayce on 16-10-14.
 */
public class CubicInInterpolator implements Interpolator {
    private static final float PI = 3.14159265f;

    @Override
    public float getInterpolation(float input) {
        return input * input * input;
    }
}