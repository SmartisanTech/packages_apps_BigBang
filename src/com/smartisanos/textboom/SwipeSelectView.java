package com.smartisanos.textboom;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

import com.smartisanos.textboom.BoomChipPage;
import com.smartisanos.textboom.BoomChipPage.BoomChip;

import smartisanos.util.SidebarUtils;

public class SwipeSelectView extends LinearLayout {

    private final long AUTO_SCROLL_DELAY = 25;
    private int mSelStart;
    private int mSelEnd;
    private int mStartBound;
    private int mEndBound;
    private int mLastTouchIndex;
    private int mStartIndex;
    private boolean mIsSelected;
    private BoomChipPage mBoomPage;
    private boolean mDragStarted;
    private int mAutoScrollTop;
    private int mAutoScrollBottom;
    private int mAutoScrollVelocity;

    private String mDragText;
    private Runnable mStartDrag = new Runnable() {
        @Override
        public void run() {
            if (!TextUtils.isEmpty(mDragText) && SidebarUtils.isSidebarShowing(getContext())) {
                mDragStarted = true;
                SidebarUtils.dragText(SwipeSelectView.this, getContext(), mDragText);
            }
        }
    };

    private Runnable mAutoScroll = new Runnable() {
        @Override
        public void run() {
            if (mAutoScrollVelocity != 0) {
                mBoomPage.mScroller.scrollBy(0, mAutoScrollVelocity);
                postDelayed(mAutoScroll, AUTO_SCROLL_DELAY);
            }
        }
    };

    public SwipeSelectView(Context context) {
        this(context, null);
    }

    public SwipeSelectView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setBoomPage(BoomChipPage boomPage) {
        mBoomPage = boomPage;
        mAutoScrollVelocity = 0;
        final Resources res = getResources();
        mAutoScrollTop = res.getDimensionPixelSize(R.dimen.auto_scroll_top);
        mAutoScrollBottom = res.getDisplayMetrics().heightPixels - res.getDimensionPixelSize(R.dimen.auto_scroll_bottom);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final float x = ev.getX();
        final float y = ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                BoomChip touchedChip = findChip(x, y, false);
                if (touchedChip != null) {
                    initSelection(touchedChip.index);
                    mIsSelected = !touchedChip.word.isSelected();
                    if (mIsSelected) {
                        touchedChip.setSelected(mIsSelected);
                        if (!mBoomPage.mBoomActionHandler.hasSelection() && SidebarUtils.isSidebarShowing(getContext())) {
                            mDragText = touchedChip.word.getText().toString();
                            postDelayed(mStartDrag, ViewConfiguration.getLongPressTimeout());
                        }
                    } else if (SidebarUtils.isSidebarShowing(getContext())) {
                        mDragText = mBoomPage.mBoomActionHandler.getSelectedText();
                        postDelayed(mStartDrag, ViewConfiguration.getLongPressTimeout());
                    }
                } else {
                    initSelection(-1);
                }
                mAutoScrollVelocity = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mLastTouchIndex != -1) {
                    scrollIfNeeded((int) y - mBoomPage.mScroller.getScrollY());
                    touchedChip = findChip(x, y, mSelEnd > mSelStart);
                    if (touchedChip != null && touchedChip.index != mLastTouchIndex) {
                        requestDisallowInterceptTouchEvent(true);
                        removeCallbacks(mStartDrag);
                        mLastTouchIndex = touchedChip.index;
                        mStartBound = Math.min(mStartBound, touchedChip.index);
                        mEndBound = Math.max(mEndBound, touchedChip.index);
                        mSelStart = Math.min(mStartIndex, touchedChip.index);
                        mSelEnd = Math.max(mStartIndex, touchedChip.index);
                        performSelect(mIsSelected);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                removeCallbacks(mStartDrag);
                requestDisallowInterceptTouchEvent(false);
                if (mSelStart != -1) {
                    if (mSelStart == mSelEnd) {
                        performSelect(mIsSelected);
                    }
                    if (mIsSelected) {
                        mBoomPage.mBoomActionHandler.onSelect(mSelStart, mSelEnd);
                        if (mStartBound < mSelStart) {
                            mBoomPage.mBoomActionHandler.deSelect(mStartBound, mSelStart - 1);
                        }
                        if (mSelEnd < mEndBound) {
                            mBoomPage.mBoomActionHandler.deSelect(mSelEnd + 1, mEndBound);
                        }
                    } else {
                        if (mStartBound < mSelStart) {
                            mBoomPage.mBoomActionHandler.onSelect(mStartBound, mSelStart - 1);
                        }
                        if (mSelEnd < mEndBound) {
                            mBoomPage.mBoomActionHandler.onSelect(mSelEnd + 1, mEndBound);
                        }
                        mBoomPage.mBoomActionHandler.deSelect(mSelStart, mSelEnd);
                    }
                }
                mAutoScrollVelocity = 0;
                break;
            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(mStartDrag);
                requestDisallowInterceptTouchEvent(false);
                if (mSelStart != -1 && mSelStart == mSelEnd) {
                    if (!mDragStarted || !mBoomPage.mBoomActionHandler.hasSelection()) {
                        performSelect(!mIsSelected);
                    }
                }
                mDragStarted = false;
                mAutoScrollVelocity = 0;
                break;
            default:
                removeCallbacks(mStartDrag);
                mAutoScrollVelocity = 0;
                break;
        }
        return mSelStart != -1 ? true : super.onTouchEvent(ev);
    }

    private void scrollIfNeeded(int y) {
        if (y < mAutoScrollTop) {
            if (mAutoScrollVelocity >= 0) {
                removeCallbacks(mAutoScroll);
                postDelayed(mAutoScroll, AUTO_SCROLL_DELAY);
            }
            mAutoScrollVelocity = (y - mAutoScrollTop) / 2;
        } else if (y > mAutoScrollBottom) {
            if (mAutoScrollVelocity <= 0) {
                removeCallbacks(mAutoScroll);
                postDelayed(mAutoScroll, AUTO_SCROLL_DELAY);
            }
            mAutoScrollVelocity = (y - mAutoScrollBottom) / 2;
        } else {
            mAutoScrollVelocity = 0;
        }
    }

    private void initSelection(int value) {
        mStartBound = value;
        mEndBound = value;
        mSelStart = value;
        mSelEnd = value;
        mStartIndex = value;
        mLastTouchIndex = value;
    }

    private void performSelect(boolean isSelected) {
        int startRow = mBoomPage.mLayout.getRowForIndex(mStartBound);
        int endRow = mBoomPage.mLayout.getRowForIndex(mEndBound);
        for (int i = startRow; i <= endRow; ++i) {
            final LinearLayout row = (LinearLayout) getChildAt(i);
            for (int j = 0; j < row.getChildCount(); ++j) {
                View child = row.getChildAt(j);
                if (child.getTag() instanceof BoomChip) {
                    BoomChip chip = (BoomChip) child.getTag();
                    final int index = chip.index;
                    if (index < mStartBound) continue;
                    if (index > mEndBound) return;
                    if (index >= mSelStart && index <= mSelEnd) {
                        chip.setSelected(isSelected);
                    } else {
                        chip.setSelected(!isSelected);
                    }
                }
            }
        }
    }

    private BoomChip findChip(float x, float y, boolean isSwiping) {
        for (int i = 0; i < getChildCount(); ++i) {
            final LinearLayout row = (LinearLayout) getChildAt(i);
            if (isTransformedTouchPointInView(x, y, row, null)) {
                final float offsetX = row.getScrollX() - row.getLeft();
                final float offsetY = row.getScrollY() - row.getTop();
                float newX = x + offsetX;
                float newY = y + offsetY - row.getTranslationY();
                for (int j = row.getChildCount() - 1; j >= 0; --j) {
                    View child = row.getChildAt(j);
                    if (isSwiping && newX > child.getX()) {
                        if (child.getTag() instanceof BoomChip) {
                            BoomChip chip = (BoomChip) child.getTag();
                            return chip;
                        }
                        return null;
                    }
                    if (isTransformedTouchPointInView(newX, newY, child, null)) {
                        if (child.getTag() instanceof BoomChip) {
                            BoomChip chip = (BoomChip) child.getTag();
                            return chip;
                        }
                        return null;
                    }
                }
                return null;
            }
        }
        return null;
    }
}
