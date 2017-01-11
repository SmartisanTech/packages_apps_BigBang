package com.smartisanos.textboom;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

public class ImageSpanAlignCenter extends ImageSpan {

    private static final char[] ELLIPSIS_NORMAL = { '\u2026' }; // this is "..."
    private static final char[] ELLIPSIS_TWO_DOTS = { '\u2025' }; // this is ".."

    public ImageSpanAlignCenter(Context context, int resourceId) {
        super(context, resourceId);
    }

    public ImageSpanAlignCenter(Drawable d) {
        super(d);
    }

    @Override
    public int getSize(Paint paint, CharSequence text,
                         int start, int end,
                         Paint.FontMetricsInt fm) {
        Drawable d = getCachedDrawable(paint);
        Rect rect = d.getBounds();

        if (fm != null) {
            Paint.FontMetricsInt fontMetrics = new Paint.FontMetricsInt();
            paint.getFontMetricsInt(fontMetrics);

            fm.ascent = fontMetrics.ascent; 
            fm.descent = fontMetrics.descent; 

            fm.top = fontMetrics.top;
            fm.bottom = fontMetrics.bottom;
        }
        return rect.right;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y,
            int bottom, Paint paint) {
        final String s = text.toString();
        String subS = s.substring(start, end);
        if (ELLIPSIS_NORMAL[0] == subS.charAt(0)
                || ELLIPSIS_TWO_DOTS[0] == subS.charAt(0)) {
            canvas.save();
            canvas.drawText(subS, x, y, paint);
            canvas.restore();
        } else {
            Drawable d = getCachedDrawable(paint);
            canvas.save();
            int transY;
            Paint.FontMetricsInt fontMetrics = new Paint.FontMetricsInt();
            paint.getFontMetricsInt(fontMetrics);
            Rect rect = d.getBounds();

            transY = y + fontMetrics.ascent;

            canvas.translate(x, transY);
            d.draw(canvas);
            canvas.restore();
        }
    }

    private Drawable getCachedDrawable(Paint paint) {
        WeakReference<Drawable> wr = mDrawableRef;
        Drawable d = null;

        if (wr != null)
            d = wr.get();

        if (d == null) {
            d = getDrawable();
            int fontMetricsInt = paint.getFontMetricsInt(null);
            int drawableWidth = d.getIntrinsicWidth();
            int drawableHeight = d.getIntrinsicHeight();
            //drawableHeight <= fontMetricsInt is better, or the image would be out of shape
            int top = 0;
            if (fontMetricsInt > drawableHeight) {
                top = (fontMetricsInt - drawableHeight)/2;
            }
            d.setBounds(new Rect(0, top, drawableWidth, top + Math.min(fontMetricsInt, drawableHeight)));
            mDrawableRef = new WeakReference<Drawable>(d);
        }

        return d;
    }

    private WeakReference<Drawable> mDrawableRef;
}
