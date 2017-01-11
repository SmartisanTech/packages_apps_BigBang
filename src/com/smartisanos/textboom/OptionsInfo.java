package com.smartisanos.textboom;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;

import com.smartisanos.textboom.util.LogUtils;

import java.io.Serializable;

/**
 * Created by huangxin on 14-7-1.
 */
public class OptionsInfo implements Parcelable {

    private String[] mOptionEntries;
    private Object[] mOptionValues;
    private String mSettingKey;
    private SaveTargetTable mTargetTable;
    private OnSelectListener mSelectListener;

    public OptionsInfo(String[] entries, Object[] values, OnSelectListener selectListener) {
        mOptionEntries = entries;
        mOptionValues = values;
        mSelectListener = selectListener;
    }

    /**
     * used when save value to SettingsProvider
     */
    public OptionsInfo(String[] entries, Object[] values, SaveTargetTable targetTable, String settingKey) {
        this(entries, values, null);
        mTargetTable = targetTable;
        mSettingKey = settingKey;
    }

    public void setSelectListener(OnSelectListener selectListener) {
        mSelectListener = selectListener;
    }

    private boolean saveSettingOption(Context mContext, int which) {
        boolean result = false;
        if (which < mOptionValues.length) {
            Object value = mOptionValues[which];
            switch (mTargetTable) {
                case System:
                    result = Settings.System.putString(mContext.getContentResolver(),
                            mSettingKey, value != null ? value.toString() : null);
                    break;
                case Secure:
                    result = Settings.Secure.putString(mContext.getContentResolver(),
                            mSettingKey, value != null ? value.toString() : null);
                    break;
                case Global:
                    result = Settings.Global.putString(mContext.getContentResolver(),
                            mSettingKey, value != null ? value.toString() : null);
                    break;
            }
            LogUtils.d("", "save [" + mSettingKey + ":  " + value + "]  " + result);
        } else {
            LogUtils.e("", "warning: ArrayIndexOutOfBounds exception ?");
        }
        return result;
    }

    /**
     * Note: if save to SettingsProvider success, mSelectListener will be ignored.
     */
    public boolean save(Context context, int which) {
        boolean result = false;
        if (mSettingKey != null && mTargetTable != null) {
            result = saveSettingOption(context, which);
        }
        if (!result && mSelectListener != null) {
            result = mSelectListener.onSelect(which);
        }
        return result;
    }

    public String[] getOptionEntries() {
        return mOptionEntries;
    }

    public Object[] getOptionValues() {
        return mOptionValues;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mOptionEntries.length);
        parcel.writeStringArray(mOptionEntries);
        parcel.writeInt(mOptionValues.length);
        parcel.writeArray(mOptionValues);
        parcel.writeString(mSettingKey);
        parcel.writeSerializable(mTargetTable);
        parcel.writeSerializable(mSelectListener);
    }

    private OptionsInfo(Parcel in) {
        mOptionEntries = new String[in.readInt()];
        in.readStringArray(mOptionEntries);
        in.readInt();//mValues length
        mOptionValues = in.readArray(Object.class.getClassLoader());
        mSettingKey = in.readString();
        mTargetTable = (SaveTargetTable) in.readSerializable();
        mSelectListener = (OnSelectListener) in.readSerializable();
    }

    public static final Creator<OptionsInfo> CREATOR = new Creator<OptionsInfo>() {
        @Override
        public OptionsInfo createFromParcel(Parcel in) {
            return new OptionsInfo(in);
        }

        @Override
        public OptionsInfo[] newArray(int i) {
            return new OptionsInfo[i];
        }
    };

    public enum SaveTargetTable implements Serializable{
        System, Secure, Global
    }

    public interface OnSelectListener extends Serializable {
        boolean onSelect(int which);
    }

}
