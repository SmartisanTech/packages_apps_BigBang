package com.smartisanos.textboom;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Created by denglinling on 16-11-11.
 */
public class CustomScrollView extends ScrollView{
    private OnScrollListener mOnScrollListener;

    public CustomScrollView(Context context) {
        super(context);
    }
    public CustomScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        mOnScrollListener = onScrollListener;
    }

    public interface OnScrollListener{
        void onScrollChanged();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollChanged();
        }
    }
}
