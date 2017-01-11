package com.smartisanos.textboom;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.provider.Settings.Global;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.TreeSet;

public class BoomActionHandler implements CustomScrollView.OnScrollListener {

    private final BoomChipPage mBoomPage;
    private final Toast mToast;

    private final int mRowMoveUpOffset;
    private final int mRowMoveDownOffset;
    private final int mChipRowHeight;
    private final int mSelectRectMarginTop;
    private final int mSelectRectTopOffset;
    private final int mFakeSelectBarTop;
    private final int mFakeSelectBarBottom;

    int mSelectedTopRow = -1;
    int mSelectedBottomRow = -1;
    RelativeLayout mSelectBar;
    RelativeLayout mFakeSelectBar;
    LinearLayout mSelectRect;
    TreeSet<Integer> mSelectedId = new TreeSet<Integer>();
    private Rect mSelectBarRect = new Rect();

    public BoomActionHandler(BoomChipPage boomPage) {
        mBoomPage = boomPage;
        mToast = Toast.makeText(boomPage.mActivity, "", Toast.LENGTH_SHORT);

        final Resources res = boomPage.mActivity.getResources();
        mRowMoveUpOffset = res.getDimensionPixelOffset(R.dimen.chip_row_move_up_offset);
        mRowMoveDownOffset = res.getDimensionPixelOffset(R.dimen.chip_row_move_down_offset);
        mChipRowHeight = res.getDimensionPixelOffset(R.dimen.chip_row_height);
        mSelectRectMarginTop = res.getDimensionPixelOffset(R.dimen.select_rect_margin_top);
        mSelectRectTopOffset = res.getDimensionPixelOffset(R.dimen.select_rect_top_offset);

        mFakeSelectBarTop = res.getDimensionPixelOffset(R.dimen.fake_select_bar_margin_top);
        mFakeSelectBarBottom = getScreenHeight() - res.getDimensionPixelOffset(R.dimen.fake_select_bar_margin_bottom);

        initViews(mBoomPage.mBoomTable);
        initFakeViews(mBoomPage.mBoomPage);
    }

    public void onSelect(TreeSet<Integer> savedState) {
        mSelectedId.clear();
        mSelectedId.addAll(savedState);
        onSelectInternal(savedState.first(), savedState.last());
    }

    public void onSelect(int start, int end) {
        for (int i = start; i <= end; ++i) {
            mSelectedId.add(new Integer(i));
        }
        onSelectInternal(start, end);
    }

    private void onSelectInternal(int start, int end) {
        final int topRow = mBoomPage.mLayout.getRowForIndex(start);
        final int bottomRow = mBoomPage.mLayout.getRowForIndex(end);

        if (mSelectedTopRow == -1) {
            mSelectedTopRow = topRow;
            mSelectedBottomRow = bottomRow;
            showSelBarAndBgRect(topRow);
        } else {
            if (topRow < mSelectedTopRow) {
                mSelectedTopRow = topRow;
            }
            if (bottomRow > mSelectedBottomRow) {
                mSelectedBottomRow = bottomRow;
            }
            positionSelBar(mSelectedTopRow);
            positionSelectRect(mSelectedTopRow);
        }

        moveChipRows();
    }

    public void deSelect(int stat, int end) {
        for (int i = stat; i <= end; ++i) {
            mSelectedId.remove(new Integer(i));
        }
        if (mSelectedId.size() > 0) {
            final int min = mBoomPage.mLayout.getRowForIndex(mSelectedId.first());
            final int max = mBoomPage.mLayout.getRowForIndex(mSelectedId.last());
            if (min > mSelectedTopRow) {
                mSelectedTopRow = min;
                positionSelBar(min);
                positionSelectRect(min);
            } else if (max < mSelectedBottomRow) {
                mSelectedBottomRow = max;
                positionSelBar(min);
                positionSelectRect(min);
            }
        } else {
            hideSelectBarAndRect();
            mBoomPage.resetChips();
        }
        moveChipRows();
    }

    public boolean handleClick() {
        if (mSelectedId.size() > 0) {
            mSelectedId.clear();
            mBoomPage.resetChips();
            hideSelectBarAndRect();
            return true;
        }
        return false;
    }

    private boolean isChineseWord(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B) {
            return true;
        }
        return false;
    }

    private int getContentType(String text) {
        int type = 0;
        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                type |= 1;
            } else if (isChineseWord(ch)) {
                type |= 2;
            } else {
                return 3;
            }
        }
        return type == 0 ? 3 : type - 1;
    }

    private void copy(String text) {
        mToast.setText(mBoomPage.mActivity.getResources().getString(R.string.copy_tips));
        mToast.show();
        ClipboardManager clipboard = (ClipboardManager) mBoomPage.mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, text));
        mBoomPage.mActivity.finish();
    }

    public void search(String text, int type) {
        Intent intent = new Intent(mBoomPage.mActivity, BoomSearchActivity.class);
        intent.putExtra(BoomSearchActivity.SEARCH_TYPE, type);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        mBoomPage.mActivity.startActivity(intent);
    }

    private void share() {
        final String shareText = getSelectedText();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, shareText);
        Intent i = Intent.createChooser(send, null);
        i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        mBoomPage.mActivity.startActivity(i);
        mBoomPage.mActivity.finish();
    }

    private void initViews(View contentView) {
        mSelectRect = (LinearLayout) contentView.findViewById(R.id.boom_multi_selected_bg);
        mSelectBar = (RelativeLayout) contentView.findViewById(R.id.multi_selected_bar);
        mSelectBar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                onScrollChanged();
            }
        });
        ImageView searchView = (ImageView) mSelectBar.findViewById(R.id.all_search);
        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search(getSelectedText(), Global.getInt(mBoomPage.mActivity.getContentResolver(),
                        Global.TEXT_BOOM_SEARCH_METHOD, BoomSearchActivity.TYPE_SHENMA));
            }
        });
        ImageView dictView = (ImageView) mSelectBar.findViewById(R.id.all_dict);
        dictView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search(getSelectedText(), Global.getInt(mBoomPage.mActivity.getContentResolver(),
                        BoomSearchActivity.SEARCH_DICT_KEY, BoomSearchActivity.TYPE_BINGDICT));
            }
        });
        ImageView shareView = (ImageView) mSelectBar.findViewById(R.id.all_share);
        shareView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share();
            }
        });
        ImageView copyView = (ImageView) mSelectBar.findViewById(R.id.all_copy);
        copyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copy(getSelectedText());
            }
        });
    }

    private void initFakeViews(View contentView) {
        mFakeSelectBar = (RelativeLayout) contentView.findViewById(R.id.fake_multi_selected_bar);
        ImageView topSearchView = (ImageView) mFakeSelectBar.findViewById(R.id.all_search);
        topSearchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search(getSelectedText(), Global.getInt(mBoomPage.mActivity.getContentResolver(),
                        Global.TEXT_BOOM_SEARCH_METHOD, BoomSearchActivity.TYPE_SHENMA));
            }
        });
        ImageView topDictView = (ImageView) mFakeSelectBar.findViewById(R.id.all_dict);
        topDictView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search(getSelectedText(), Global.getInt(mBoomPage.mActivity.getContentResolver(),
                        BoomSearchActivity.SEARCH_DICT_KEY, BoomSearchActivity.TYPE_BINGDICT));
            }
        });
        ImageView topShareView = (ImageView) mFakeSelectBar.findViewById(R.id.all_share);
        topShareView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share();
            }
        });
        ImageView topCopyView = (ImageView) mFakeSelectBar.findViewById(R.id.all_copy);
        topCopyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copy(getSelectedText());
            }
        });
    }

    public boolean hasSelection() {
        return mSelectedId.size() > 0;
    }

    public String getSelectedText() {
        final int wordCount = mBoomPage.mLayout.getWordCount();
        if (mSelectedId.size() == wordCount) {
            return mBoomPage.mLayout.getOriText();
        }
        StringBuilder res = new StringBuilder();
        int last = -1;
        for (Integer cur : mSelectedId) {
            if (last == -1) {
                res.append(mBoomPage.mLayout.getWord(cur));
            } else if (cur == last + 1) {
                final int end = cur == wordCount - 1 ?
                        mBoomPage.mLayout.getOriText().length() : mBoomPage.mLayout.getWordEnd(cur);
                res.append(mBoomPage.mLayout.getOriText(mBoomPage.mLayout.getWordEnd(last), end));
            } else {
                res.append(mBoomPage.mLayout.getWord(cur));
            }
            last = cur;
        }
        return res.toString();
    }

    private int getSelectRectHeight() {
        return (mSelectedBottomRow - mSelectedTopRow + 1) * mChipRowHeight + mSelectRectTopOffset;
    }

    private int getSelectRectY(int row) {
        return row * mChipRowHeight + mSelectRectMarginTop;
    }

    private int getSelectBarY(int row) {
        return row * mChipRowHeight;
    }

    private void showSelBarAndBgRect(int row) {
        mSelectBar.setVisibility(View.VISIBLE);
        mSelectRect.setVisibility(View.VISIBLE);
        mSelectBar.setTranslationY(getSelectBarY(row));
        mSelectRect.setTranslationY(getSelectRectY(row));
        BoomAnimator.makeBarAndRectShowAnimation(mSelectBar, mSelectRect, getSelectRectHeight());
    }

    private void hideSelectBarAndRect() {
        mSelectedTopRow = -1;
        mSelectedBottomRow = -1;
        BoomAnimator.makeBarAndRectHideAnimation(mSelectBar, mSelectRect);
        if (mFakeSelectBar != null && mFakeSelectBar.getVisibility() == View.VISIBLE) {
            mFakeSelectBar.setVisibility(View.INVISIBLE);
        }
    }

    private void positionSelBar(int row) {
        BoomAnimator.makeMoveAnimation(mSelectBar, mSelectBar.getTranslationY(), getSelectBarY(row));
    }

    private void positionSelectRect(int row) {
        BoomAnimator.makeHeightAnimation(mSelectRect, getSelectRectHeight(), mSelectRect.getTranslationY(), getSelectRectY(row));
    }

    private void moveChipRows() {
        final int rowCount = mBoomPage.mLayout.getRowCount();
        if (mSelectedTopRow == -1 || mSelectedBottomRow == -1) {
            for (int i = 0; i < rowCount; ++i) {
                mBoomPage.moveChipRow(i, 0);
            }
        } else {
            for (int i = 0; i < rowCount; ++i) {
                float end;
                if (i < mSelectedTopRow) {
                    end = mRowMoveUpOffset;
                } else if (i > mSelectedBottomRow) {
                    end = mRowMoveDownOffset;
                } else {
                    end = 0;
                }
                mBoomPage.moveChipRow(i, end);
            }
        }
    }

    @Override
    public void onScrollChanged() {
        if (!hasSelection()) return;
        if (mSelectBar != null && mFakeSelectBar != null) {
            mSelectBar.getGlobalVisibleRect(mSelectBarRect);
            if (mSelectBarRect.top <= mFakeSelectBarTop && mFakeSelectBar.getVisibility() != View.VISIBLE) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mFakeSelectBar.getLayoutParams();
                params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                mFakeSelectBar.setLayoutParams(params);
                mSelectBar.setVisibility(View.INVISIBLE);
                mFakeSelectBar.setVisibility(View.VISIBLE);
            } else if (mSelectBarRect.bottom >= mFakeSelectBarBottom && mFakeSelectBar.getVisibility() != View.VISIBLE) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mFakeSelectBar.getLayoutParams();
                params.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                mFakeSelectBar.setLayoutParams(params);
                mSelectBar.setVisibility(View.INVISIBLE);
                mFakeSelectBar.setVisibility(View.VISIBLE);
            } else if (mSelectBarRect.top > mFakeSelectBarTop && mSelectBarRect.bottom < mFakeSelectBarBottom){
                mFakeSelectBar.setVisibility(View.INVISIBLE);
                mSelectBar.setVisibility(View.VISIBLE);
            }
        }
    }

    private int getScreenHeight() {
        return mBoomPage.mActivity.getResources().getDisplayMetrics().heightPixels;
    }
}