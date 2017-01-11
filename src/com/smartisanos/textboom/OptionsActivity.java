package com.smartisanos.textboom;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.smartisanos.textboom.util.LogUtils;
import com.smartisanos.textboom.util.Utils;

import smartisanos.widget.Title;
import smartisanos.api.IntentSmt;

/**
 * Created by huangxin on 14-7-1.
 */
public class OptionsActivity extends Activity {

    private static final String TAG = "OptionsActivity";

    private ListView mOptionsList;
    private BaseAdapter mAdapter;

    private OptionsInfo mOptionsInfo;

    private String[] mOptions;
    private Object[] mValues;

    private Object mCurrentValue;

    private boolean mAutoFinishAfterSelect;

    public static final String EXTRA_OPTION_INFO = "extra_option_info";
    public static final String EXTRA_CURRENT_VALUE = "extra_current_value";

    public static final String EXTRA_AUTO_FINISH = "extra_auto_finish";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.options_layout);
        Title title = (Title) findViewById(R.id.view_title);
        setTitleByIntent(title);
        title.setBackButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mOptionsList = (ListView) findViewById(R.id.options_list);

        Intent intent = getIntent();

        mAutoFinishAfterSelect = intent.getBooleanExtra(EXTRA_AUTO_FINISH, true);//auto finish default

        if (intent.hasExtra(EXTRA_OPTION_INFO)) {
            mOptionsInfo = intent.getParcelableExtra(EXTRA_OPTION_INFO);
            mCurrentValue = intent.getStringExtra(EXTRA_CURRENT_VALUE);
            mOptions = mOptionsInfo.getOptionEntries();
            mValues = mOptionsInfo.getOptionValues();
        } else {
            LogUtils.e(TAG, "has no option info, finish.");
            finish();
            return;
        }

        mOptionsList.addHeaderView(Utils.inflateListTransparentHeader(this));

        mAdapter = new OptionsAdapter();
        mOptionsList.setAdapter(mAdapter);

        mOptionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {
                index -= mOptionsList.getHeaderViewsCount();
                if (index < 0) return;
                if (mOptionsInfo != null) {
                    if (mAutoFinishAfterSelect) finish();
                    if (mOptionsInfo.save(OptionsActivity.this, index)) {
                        mCurrentValue = mValues[index].toString();
                        LogUtils.d(TAG, "onItemClick:  current:" + mCurrentValue);
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }
        });

    }

    protected void setTitleByIntent(Title title) {
        if (getIntent().hasExtra(Title.EXTRA_TITLE_TEXT)) {
            String titleStr = getIntent().getStringExtra(Title.EXTRA_TITLE_TEXT);
            if (!TextUtils.isEmpty(titleStr)) {
                title.setTitle(titleStr);
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (getIntent() != null) {
            int[] anims = getIntent().getIntArrayExtra(
                    IntentSmt.EXTRA_SMARTISAN_ANIM_RESOURCE_ID);
            if (anims != null) {
                overridePendingTransition(anims[0], anims[1]);
            }
        }
    }

    private class OptionsAdapter extends BaseAdapter {

        LayoutInflater mInflater;

        public OptionsAdapter() {
            mInflater = LayoutInflater.from(OptionsActivity.this);
        }

        @Override
        public int getCount() {
            return mOptions.length;
        }

        @Override
        public Object getItem(int i) {
            return mOptions[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            ViewHolder holder;
            if (convertView == null || !(convertView.getTag() instanceof ViewHolder)) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.option_item_layout, null);
                holder.mOptionTitle = (TextView) convertView.findViewById(R.id.option_title);
                holder.mOptionCheckedImg = (ImageView) convertView.findViewById(R.id.option_checked);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (getCount() == 1) {
                convertView.setBackgroundResource(R.drawable.selector_setting_sub_item_bg_single);
            } else if (i == 0) {
                convertView.setBackgroundResource(R.drawable.selector_setting_sub_item_bg_top);
            } else if (i == getCount() - 1) {
                convertView.setBackgroundResource(R.drawable.selector_setting_sub_item_bg_bottom);
            } else {
                convertView.setBackgroundResource(R.drawable.selector_setting_sub_item_bg_middle);
            }

            holder.mOptionTitle.setText(mOptions[i]);
            holder.mOptionCheckedImg.setVisibility(
                    (mValues[i] == null && mCurrentValue == null) || mValues[i].toString().equals(mCurrentValue)
                            ? View.VISIBLE : View.GONE);

            return convertView;
        }
    }

    static class ViewHolder {
        TextView mOptionTitle;
        ImageView mOptionCheckedImg;
    }

}