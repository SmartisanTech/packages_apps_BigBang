package com.smartisanos.textboom;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

public class BoomAnimator {

    private final static DecelerateInterpolator mIterpolator = new DecelerateInterpolator(1.5f);
    public final static long BOOM_DURATION = 200;
    public final static long FADE_DURATION = 200;
    private final static long MOVE_DURATION = 200;
    private final static long HIDE_DURATION = 100;

    private static Animator makeScaleAnimator(final View view, float from, float to, long duration) {
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, from, to);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, from, to);

        animatorSet.setInterpolator(mIterpolator);
        animatorSet.setDuration(duration);
        animatorSet.play(scaleX).with(scaleY);
        return animatorSet;
    }

    private static Animator makeAlphaAnimator(final View view, float from, float to, long duration) {
        Animator animator = ObjectAnimator.ofFloat(view, View.ALPHA, from, to);
        animator.setDuration(duration);
        animator.setInterpolator(mIterpolator);
        return animator;
    }

    private static Animator makeXYAnimator(final View view, float toX, float toY, long duration) {
        AnimatorSet animatorSet = new AnimatorSet();
        ValueAnimator x = ValueAnimator.ofFloat(view.getX(), toX);
        x.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                view.setX((Float) animation.getAnimatedValue());
            }
        });
        ValueAnimator y = ValueAnimator.ofFloat(view.getY(), toY);
        y.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                view.setY((Float) animation.getAnimatedValue());
            }
        });
        animatorSet.setInterpolator(mIterpolator);
        animatorSet.setDuration(duration);
        animatorSet.play(x).with(y);
        return animatorSet;
    }

    private static Animator makeTranslationAnimator(final View view, long duration) {
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator transX = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, view.getTranslationX(), 0);
        ObjectAnimator transY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, view.getTranslationY(), 0);

        animatorSet.setInterpolator(mIterpolator);
        animatorSet.setDuration(duration);
        animatorSet.play(transX).with(transY);
        return animatorSet;
    }

    private static Animator makeTranslationYAnimator(final View view, float start, float end, long duration) {
        ObjectAnimator transY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, start, end);
        transY.setInterpolator(mIterpolator);
        transY.setDuration(duration);
        return transY;
    }

    private static Animator makeHeightAnimator(final View view, int targetHeight, long duration) {
        ValueAnimator anim = ValueAnimator.ofInt(view.getMeasuredHeight(), targetHeight);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                setHeight(view, (Integer) valueAnimator.getAnimatedValue());
            }
        });
        anim.setInterpolator(mIterpolator);
        anim.setDuration(duration);
        return anim;
    }

    private static void setHeight(final View view, int height) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        layoutParams.height = height;
        view.setLayoutParams(layoutParams);
    }

    public static void makeFadeIn(final View view, long duration) {
        Animator animator = makeAlphaAnimator(view, 0, 1.0f, duration);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setVisibility(View.VISIBLE);
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
        animator.start();
    }

    public static void makeFadeOut(final View view, long duration) {
        Animator animator = makeAlphaAnimator(view, 1.0f, 0, duration);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                view.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animator.start();
    }

    public static void makeHeightAnimation(final View view, final int targetHeight, float startY, final float endY) {
        if (startY == endY && targetHeight == view.getMeasuredHeight()) {
            return;
        }
        AnimatorSet animatorSet = new AnimatorSet();
        Animator heightAnimator = makeHeightAnimator(view, targetHeight, MOVE_DURATION);
        Animator transAnimator = makeTranslationYAnimator(view, startY, endY, MOVE_DURATION);
        animatorSet.playTogether(heightAnimator, transAnimator);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (view.getVisibility() != View.VISIBLE) {
                    view.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {
                setHeight(view, targetHeight);
                view.setTranslationY(endY);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.start();
    }

    public static void makeBarAndRectShowAnimation(final View bar, final View rect, int targetHeight) {
        AnimatorSet animatorSet = new AnimatorSet();
        Animator heightAnimator = makeHeightAnimator(rect, targetHeight, MOVE_DURATION);
        Animator bgAlpha = makeAlphaAnimator(rect, 0, 1.0f, MOVE_DURATION);
        Animator barAlpha = makeAlphaAnimator(bar, 0, 1.0f, MOVE_DURATION);
        animatorSet.playTogether(heightAnimator, bgAlpha, barAlpha);
        animatorSet.start();
    }

    public static void makeBarAndRectHideAnimation(final View bar, final View rect) {
        AnimatorSet animatorSet = new AnimatorSet();
        Animator bgAlpha = makeAlphaAnimator(rect, 1.0f, 0, HIDE_DURATION);
        Animator barAlpha = makeAlphaAnimator(bar, 1.0f, 0, HIDE_DURATION);
        animatorSet.playTogether(bgAlpha, barAlpha);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setHeight(rect, 0);
                rect.setVisibility(View.INVISIBLE);
                bar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animatorSet.start();
    }

    public static void makeSectorAnimation(final View view, float toX, float toY) {
        AnimatorSet animatorSet = new AnimatorSet();
        Animator scaleAnimator = makeScaleAnimator(view, 0f, 1f, BOOM_DURATION);
        Animator alphaAnimator = makeAlphaAnimator(view, 0f, 1f, BOOM_DURATION);
        animatorSet.playTogether(scaleAnimator, alphaAnimator, makeXYAnimator(view, toX, toY, BOOM_DURATION));
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                view.setScaleX(1.0f);
                view.setScaleY(1.0f);
                view.setAlpha(1.0f);
                view.setTranslationX(0);
                view.setTranslationY(0);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animatorSet.start();
    }

    public static void makeBoomAnimation(final View view) {
        AnimatorSet animatorSet = new AnimatorSet();
        Animator scaleAnimator = makeScaleAnimator(view, 0f, 1f, BOOM_DURATION);
        Animator alphaAnimator = makeAlphaAnimator(view, 0f, 1f, BOOM_DURATION);
        animatorSet.playTogether(scaleAnimator, alphaAnimator, makeTranslationAnimator(view, BOOM_DURATION));
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                view.setScaleX(1.0f);
                view.setScaleY(1.0f);
                view.setAlpha(1.0f);
                view.setTranslationX(0);
                view.setTranslationY(0);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animatorSet.start();
    }

    public static void makeMoveAnimation(final View view, float startY, final float endY) {
        if (startY != endY) {
            AnimatorSet animatorSet = new AnimatorSet();
            Animator moveAnimator = makeTranslationYAnimator(view, startY, endY, MOVE_DURATION);
            animatorSet.playTogether(moveAnimator);
            animatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (view.getVisibility() != View.VISIBLE) {
                        view.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    view.setTranslationY(endY);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            animatorSet.start();
        }
    }
}
