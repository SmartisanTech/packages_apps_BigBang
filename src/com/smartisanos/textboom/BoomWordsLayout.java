package com.smartisanos.textboom;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

public class BoomWordsLayout {

    private final static String TAG = "BoomWordsLayout";

    private final int mMaxRowNumber;
    private final int mBoomPageWidth;
    private final int mWordMinWidth;
    private final int mWordBaseWidth;
    private final int mPuncMinWidth;
    private final int mPuncBaseWidth;
    private final TextPaint mWordPaint;
    private final TextPaint mPuncPaint;

    private RangeList<Word> mWords = new RangeList<Word>();
    private ArrayList<Integer> mRowStart = new ArrayList<Integer>();
    private ArrayList<Integer> mRowCount = new ArrayList<Integer>();
    private int[] mIdToRow;
    private int mTouchedIndex;
    private String mOriText;

    private class RangeList<E> extends ArrayList<E> {
        public void remove(int fromIndex, int toIndex) {
            if (fromIndex < toIndex) {
                removeRange(fromIndex, toIndex);
            }
        }
    }

    private class Word {
        public final String word;
        public final int start;
        public final boolean punc;

        public Word(String w, int s, boolean isPunc) {
            word = w;
            start = s;
            punc = isPunc;
        }
    }

    public BoomWordsLayout(Context context) {
        Resources res = context.getResources();
        final int displayWidth = res.getDisplayMetrics().widthPixels;
        mBoomPageWidth = displayWidth - res.getDimensionPixelSize(R.dimen.page_margin_left)
                - res.getDimensionPixelSize(R.dimen.page_margin_right);
        //mMaxRowNumber = displayWidth > 1080 ? 11 : 10;
        mMaxRowNumber = 1000;
        mWordMinWidth = res.getDimensionPixelSize(R.dimen.word_min_width);
        mWordBaseWidth = res.getDimensionPixelSize(R.dimen.word_base_width);
        mPuncMinWidth = res.getDimensionPixelSize(R.dimen.punc_min_width);
        mPuncBaseWidth = res.getDimensionPixelSize(R.dimen.punc_base_width);
        mWordPaint = ((TextView) View.inflate(context, R.layout.boom_chip_layout, null)
                .findViewById(R.id.word)).getPaint();
        mPuncPaint = ((TextView) View.inflate(context, R.layout.boom_punc_layout, null)
                .findViewById(R.id.punc)).getPaint();
    }

    public boolean layoutWords(int[] segment, String text, int touchedIndex) {
        int puncIndexStart = -1;
        for (int i = 0; i < segment.length; ++i) {
            if (segment[i] == -1) {
                puncIndexStart = i;
                break;
            }
        }
        if (puncIndexStart == -1) return false;
        int[] newSeg = new int[puncIndexStart];
        int wordIndexStart = 0;
        ++puncIndexStart;
        int garbageOffset = 0;
        int touchIndexOffset = 0;
        StringBuilder newText = new StringBuilder();
        for (int i = 0; i < newSeg.length; i += 2) {
            int curWordStart = segment[i];
            int curPuncStart = puncIndexStart == segment.length ? text.length() : segment[puncIndexStart];
            if (curWordStart < curPuncStart) {
                if (curWordStart > wordIndexStart) {
                    int garbageDiff = curWordStart - wordIndexStart;
                    if (touchedIndex > curWordStart) {
                        touchIndexOffset += garbageDiff;
                    }
                    garbageOffset += garbageDiff;
                } else if (curWordStart < wordIndexStart) {
                    Log.e(TAG, "Something wrong with rebuild segment curWordStart=" + curWordStart + ", wordIndexStart=" + wordIndexStart);
                    return false;
                }
                newSeg[i] = segment[i] - garbageOffset;
                newSeg[i + 1] = segment[i + 1] - garbageOffset;
                wordIndexStart = segment[i + 1] + 1;
                newText.append(text.substring(segment[i], segment[i + 1] + 1));
            } else {
                if (curPuncStart > wordIndexStart) {
                    int garbageDiff = curPuncStart - wordIndexStart;
                    if (touchedIndex > curPuncStart) {
                        touchIndexOffset += garbageDiff;
                    }
                    garbageOffset += garbageDiff;
                } else if (curPuncStart < wordIndexStart) {
                    Log.e(TAG, "Something wrong with rebuild segment curPuncStart=" + curPuncStart + ", wordIndexStart=" + wordIndexStart);
                    return false;
                }
                wordIndexStart = segment[puncIndexStart + 1] + 1;
                newText.append(text.substring(segment[puncIndexStart], segment[puncIndexStart + 1] + 1));
                puncIndexStart += 2;
                i -= 2;
            }
        }
        if (puncIndexStart < segment.length) {
            for (int i = puncIndexStart; i < segment.length; i += 2) {
                int curPuncStart = segment[i];
                if (curPuncStart > wordIndexStart) {
                    if (touchedIndex > curPuncStart) {
                        touchIndexOffset += curPuncStart - wordIndexStart;
                    }
                } else if (curPuncStart < wordIndexStart) {
                    Log.e(TAG, "Something wrong with add ending punc curPuncStart=" + curPuncStart + ", wordIndexStart=" + wordIndexStart);
                    return false;
                }
                wordIndexStart = segment[i + 1] + 1;
                newText.append(text.substring(segment[i], segment[i + 1] + 1));
            }
        }
        return layoutWordsAfterFilter(newSeg, newText.toString(), touchedIndex - touchIndexOffset);
    }

    private boolean layoutWordsAfterFilter(int[] segment, String text, int touchedIndex) {
        mOriText = text;
        mWords.clear();
        mTouchedIndex = -1;
        int start;
        int end;
        int prev = 0;
        for (int i = 0; i < segment.length; i += 2) {
            start = segment[i];
            end = segment[i + 1] + 1;
            addPuncIntoChips(prev, start);
            String trim = text.substring(start, end).replaceAll("\\p{Z}", " ").trim();
            if (!TextUtils.isEmpty(trim)) {
                if (touchedIndex >= start && touchedIndex < end) {
                    mTouchedIndex = mWords.size();
                }
                mWords.add(new Word(trim, start, false));
            }
            prev = end;
        }
        addPuncIntoChips(prev, text.length());

        final int wordCount = mWords.size();
        if (wordCount > 0) {
            generateLayout();
            final int rowCount = mRowCount.size();
            if (rowCount > mMaxRowNumber) {
                if (mTouchedIndex == -1) {
                    start = 0;
                    end = mRowStart.get(mMaxRowNumber);
                } else {
                    final int row = getRowForIndex(mTouchedIndex);
                    if (row < mMaxRowNumber / 2) {
                        start = 0;
                        end = getRowStart(mMaxRowNumber);
                    } else if (row >= rowCount - mMaxRowNumber / 2) {
                        start = getRowStart(rowCount - mMaxRowNumber);
                        end = wordCount;
                    } else {
                        start = getRowStart(row - mMaxRowNumber / 2);
                        end = getRowStart(row + mMaxRowNumber / 2);
                    }
                }
                mWords.remove(end, wordCount);
                mWords.remove(0, start);
                generateLayout();
            }
            return true;
        }
        return false;
    }

    private void addPuncIntoChips(int start, int end) {
        for (int i = start; i < end; ++i) {
            char punc = mOriText.charAt(i);
            if (!Character.isWhitespace(punc) && !Character.isSpaceChar(punc)) {
                mWords.add(new Word(String.valueOf(punc), i, true));
            }
        }
    }

    private int measureChip(int index) {
        final Word word = mWords.get(index);
        if (word.punc) {
            return Math.max(mPuncMinWidth, mPuncBaseWidth + (int)mPuncPaint.measureText(word.word));
        } else {
            return Math.max(mWordMinWidth, mWordBaseWidth + (int)mWordPaint.measureText(word.word));
        }
    }

    private void generateLayout() {
        int count = 0;
        int start = 0;
        int remain = mBoomPageWidth;
        mRowCount.clear();
        mRowStart.clear();
        mIdToRow = new int[mWords.size()];
        for (int i = 0; i < mWords.size(); ++i) {
            final int chipWidth = measureChip(i);
            if (chipWidth > remain) {
                if (count == 0) {
                    mIdToRow[i] = mRowCount.size();
                    mRowCount.add(1);
                    mRowStart.add(i);
                    start = i + 1;
                } else {
                    mRowCount.add(count);
                    mRowStart.add(start);
                    start = i;
                    count = 0;
                    remain = mBoomPageWidth;
                    --i;
                }
            } else {
                ++count;
                remain -= chipWidth;
                mIdToRow[i] = mRowCount.size();
            }
        }
        if (count > 0) {
            mRowCount.add(count);
            mRowStart.add(start);
        }
    }

    public int getRowCount() {
        return mRowCount.size();
    }

    public int getRowStart(int row) {
        return mRowStart.get(row);
    }

    public int getColumnCount(int row) {
        return mRowCount.get(row);
    }

    public int getRowForIndex(int index) {
        return mIdToRow[index];
    }

    public boolean isPunc(int index) {
        return mWords.get(index).punc;
    }

    public String getWord(int index) {
        return mWords.get(index).word;
    }

    public int getWordEnd(int index) {
        return mWords.get(index).word.length() + mWords.get(index).start;
    }

    public String getOriText(int start, int end) {
        return mOriText.substring(start, end);
    }

    public String getOriText() {
        return mOriText;
    }

    public int getWordCount() {
        return mWords.size();
    }
}
