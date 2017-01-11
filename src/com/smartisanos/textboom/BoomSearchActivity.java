package com.smartisanos.textboom;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import smartisanos.app.SearchActivity;

import org.json.JSONArray;

public class BoomSearchActivity extends SearchActivity {

    private static final String TAG = "BoomSearchActivity";
    private static final String SEARCH_WEB = "search_web";
    private static final String SEARCH_DICT = "search_dict";
    private static final String SEARCH_WIKI = "search_wiki";
    public static final String SEARCH_TYPE = "search_type";
    public static final String SEARCH_DICT_KEY = "big_bang_default_dict";
    public static final int TYPE_BAIDU = Settings.TEXT_BOOM_SEARCH_VALUE.TYPE_BAIDU;
    public static final int TYPE_GOOGLE = Settings.TEXT_BOOM_SEARCH_VALUE.TYPE_GOOGLE;
    public static final int TYPE_BING = Settings.TEXT_BOOM_SEARCH_VALUE.TYPE_BING;
    public static final int TYPE_SHENMA = Settings.TEXT_BOOM_SEARCH_VALUE.TYPE_SHENMA;
    public static final int TYPE_WIKI = Settings.TEXT_BOOM_SEARCH_VALUE.TYPE_WIKI;
    public static final int TYPE_BAIKE = TYPE_WIKI + 1;
    public static final int TYPE_YOUDAO = Settings.TEXT_BOOM_SEARCH_VALUE.TYPE_YOUDAO;
    public static final int TYPE_KINGSOFT = Settings.TEXT_BOOM_SEARCH_VALUE.TYPE_KINGSOFT;
    public static final int TYPE_BINGDICT = Settings.TEXT_BOOM_SEARCH_VALUE.TYPE_BINGDICT;
    public static final int TYPE_HIDICT = Settings.TEXT_BOOM_SEARCH_VALUE.TYPE_HIDICT;

    private int mArrowHorrizontalOffset;
    private int mSearchType;
    private int mWebSearchType;
    private int mDictSearchType;

    private RadioButton mSearchWeb;
    private RadioButton mSearchWiki;
    private RadioButton mSearchDict;

    @Override
    protected void initContentView() {
        mContext = this;
        mArrowHorrizontalOffset = getResources().getDimensionPixelOffset(R.dimen.popup_arrow_horrizontal_offset);

        setContentView(R.layout.boom_search_activity);
        mTitle = (TextView) findViewById(R.id.search_title);
        mProgess = findViewById(R.id.search_progress);
        mColse = findViewById(R.id.search_setting);
        mWebView = (WebView) findViewById(R.id.search_webview);
        mGoBack = (ImageView) findViewById(R.id.go_back);
        mGoForward = null;
        mBrowser = findViewById(R.id.goto_browser);
        mSearchWeb = (RadioButton) findViewById(R.id.search_web);
        mSearchWiki = (RadioButton) findViewById(R.id.search_wiki);
        mSearchDict = (RadioButton) findViewById(R.id.search_dict);

        mSearchType = getIntent().getIntExtra(SEARCH_TYPE, -1);
        mWebSearchType = mDictSearchType = -1;
    }

    public void outsideClick(View v) {
        finish();
    }

    public interface onCheckChangedListener {
        public void onCheckChanged(int id);
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        mColse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(BoomSearchActivity.this, TextBoomSettingsActivity.class);
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "error=" + e);
                }
                finish();
            }
        });
        mBrowser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoBrowser();
            }
        });
        mGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                }
            }
        });
        mSearchWeb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mProgess.setTranslationX(-mArrowHorrizontalOffset);
                    performSearch(getSearchInfo(SEARCH_WEB));
                }
            }
        });
        mSearchWeb.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final int search_type = getSearchInfo(SEARCH_WEB);
                OptionPopupWindow popup = new OptionPopupWindow(BoomSearchActivity.this, R.array.search_engine_text,
                        R.array.search_engine_icon, search_type - TYPE_BAIDU, -1,
                        new onCheckChangedListener() {

                            @Override
                            public void onCheckChanged(int id) {
                                final int new_type = TYPE_BAIDU + id;
                                if (search_type != new_type) {
                                    mSearchWeb.setButtonDrawable(getIconResByType(new_type));
                                    putSearchInfo(SEARCH_WEB, new_type);
                                    if (mSearchWeb.isChecked()) {
                                        performSearch(new_type);
                                    } else {
                                        mSearchWeb.setChecked(true);
                                    }
                                }
                            }
                        });
                popup.show(v);
                return true;
            }
        });
        mSearchDict.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mProgess.setTranslationX(0);
                    performSearch(getSearchInfo(SEARCH_DICT));
                }
            }
        });
        mSearchDict.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final int search_type = getSearchInfo(SEARCH_DICT);
                OptionPopupWindow popup = new OptionPopupWindow(BoomSearchActivity.this, R.array.search_dict_text,
                        R.array.search_dict_icon, search_type - TYPE_YOUDAO, 0,
                        new onCheckChangedListener() {

                            @Override
                            public void onCheckChanged(int id) {
                                final int new_type = TYPE_YOUDAO + id;
                                if (search_type != new_type) {
                                    mSearchDict.setButtonDrawable(getIconResByType(new_type));
                                    putSearchInfo(SEARCH_DICT, new_type);
                                    if (mSearchDict.isChecked()) {
                                        performSearch(new_type);
                                    } else {
                                        mSearchDict.setChecked(true);
                                    }
                                }
                            }
                        });
                popup.show(v);
                return true;
            }
        });
        mSearchWiki.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mProgess.setTranslationX(mArrowHorrizontalOffset);
                    performSearch(getSearchInfo(SEARCH_WIKI));
                }
            }
        });
        mSearchWiki.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final int search_type = getSearchInfo(SEARCH_WIKI);
                OptionPopupWindow popup = new OptionPopupWindow(BoomSearchActivity.this, R.array.search_wiki_text,
                        R.array.search_wiki_icon, search_type - TYPE_WIKI, 1,
                        new onCheckChangedListener() {

                            @Override
                            public void onCheckChanged(int id) {
                                final int new_type = TYPE_WIKI + id;
                                if (search_type != new_type) {
                                    mSearchWiki.setButtonDrawable(getIconResByType(new_type));
                                    putSearchInfo(SEARCH_WIKI, new_type);
                                    if (mSearchWiki.isChecked()) {
                                        performSearch(new_type);
                                    } else {
                                        mSearchWiki.setChecked(true);
                                    }
                                }
                            }
                        });
                popup.show(v);
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        final int webType = Settings.Global.getInt(getContentResolver(), Settings.Global.TEXT_BOOM_SEARCH_METHOD, TYPE_SHENMA);
        final int dictType = Settings.Global.getInt(getContentResolver(), SEARCH_DICT_KEY, TYPE_BINGDICT);
        if (mWebSearchType == -1) mWebSearchType = webType;
        if (mDictSearchType == -1) mDictSearchType = dictType;
        if (mSearchType < TYPE_YOUDAO) {
            if (webType != mWebSearchType) {
                mSearchType = mWebSearchType = webType;
            }
            if (mSearchType < TYPE_WIKI) {
                putSearchInfo(SEARCH_WEB, mSearchType);
                mSearchWeb.setChecked(true);
            } else if (mSearchType < TYPE_YOUDAO) {
                putSearchInfo(SEARCH_WIKI, mSearchType);
                mSearchWiki.setChecked(true);
            }
            if (dictType != mDictSearchType) {
                mDictSearchType = dictType;
                mSearchDict.setButtonDrawable(getIconResByType(dictType));
                putSearchInfo(SEARCH_DICT, dictType);
            }
        } else {
            if (dictType != mDictSearchType) {
                mSearchType = mDictSearchType = dictType;
            }
            putSearchInfo(SEARCH_DICT, mSearchType);
            mSearchDict.setChecked(true);
            if (webType != mWebSearchType) {
                mWebSearchType = webType;
                if (webType < TYPE_WIKI) {
                    mSearchWeb.setButtonDrawable(getIconResByType(webType));
                    putSearchInfo(SEARCH_WEB, webType);
                } else if (webType < TYPE_YOUDAO) {
                    mSearchWiki.setButtonDrawable(getIconResByType(webType));
                    putSearchInfo(SEARCH_WIKI, webType);
                }
            }
        }
        mSearchWeb.setButtonDrawable(getIconResByType(getSearchInfo(SEARCH_WEB)));
        mSearchDict.setButtonDrawable(getIconResByType(getSearchInfo(SEARCH_DICT)));
        mSearchWiki.setButtonDrawable(getIconResByType(getSearchInfo(SEARCH_WIKI)));
    }

    private String getSearchInfoByType(int type) {
        switch (type) {
            case TYPE_BAIDU:
                return "Baidu";
            case TYPE_GOOGLE:
                return "Google";
            case TYPE_BING:
                return "Bing";
            case TYPE_SHENMA:
                return "Shenma";
            case TYPE_WIKI:
                return "Hudong Baike";
            case TYPE_BAIKE:
                return "Baidu Baike";
            case TYPE_YOUDAO:
                return "Youdao Dictionary";
            case TYPE_KINGSOFT:
                return "Iciba";
            case TYPE_BINGDICT:
                return "Bing Dictionary";
            case TYPE_HIDICT:
                return "DICT.CN";
            default:
                return "Unknown";
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(getSearchInfoByType(getSearchInfo(SEARCH_WEB)));
        jsonArray.put(getSearchInfoByType(getSearchInfo(SEARCH_DICT)));
        jsonArray.put(getSearchInfoByType(getSearchInfo(SEARCH_WIKI)));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Remove slop for shadow
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            final Window window = getWindow();
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            final View decorView = window.getDecorView();
            if (x < 0 || x > decorView.getWidth() || y < 0 || y > decorView.getHeight()) {
                if (window.peekDecorView() != null) {
                    finish();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected String getOriUrl() {
        switch (mSearchType) {
            case TYPE_GOOGLE:
                return "https://www.google.com/search?q=" + mSearchText;
            case TYPE_BING:
                return "https://www.bing.com/search?q=" + mSearchText;
            case TYPE_SHENMA:
                return "http://m.yz.sm.cn/s?q=" + mSearchText;
            case TYPE_WIKI:
                return "http://www.baike.com/gwiki/" + mSearchText;
            case TYPE_BAIKE:
                return "http://wapbaike.baidu.com/search/word?word=" + mSearchText;
            case TYPE_YOUDAO:
                return "http://m.youdao.com/dict?q=" + mSearchText;
            case TYPE_KINGSOFT:
                return "http://www.iciba.com/" + mSearchText;
            case TYPE_BINGDICT:
                return "http://cn.bing.com/dict/?q=" + mSearchText;
            case TYPE_HIDICT:
                return "http://m.dict.cn/" + mSearchText;
            default:
                return "https://www.baidu.com/s?wd=" + mSearchText;
        }
    }

    private int getIconResByType(int type) {
        if (type == TYPE_BAIDU) {
            return R.drawable.boom_win_search_baidu;
        } else if (type == TYPE_GOOGLE) {
            return R.drawable.boom_win_search_google;
        } else if (type == TYPE_BING) {
            return R.drawable.boom_win_search_bing;
        } else if (type == TYPE_SHENMA) {
            return R.drawable.boom_win_search_shenma;
        } else if (type == TYPE_WIKI) {
            return R.drawable.boom_win_search_hudongdict;
        } else if (type == TYPE_BAIKE) {
            return R.drawable.boom_win_search_baike;
        } else if (type == TYPE_YOUDAO) {
            return R.drawable.boom_win_search_youdao;
        } else if (type == TYPE_KINGSOFT) {
            return R.drawable.boom_win_search_kingsoft;
        } else if (type == TYPE_BINGDICT) {
            return R.drawable.boom_win_search_bingdict;
        } else if (type == TYPE_HIDICT) {
            return R.drawable.boom_win_search_hidict;
        }
        return 0;
    }

    private void performSearch(int type) {
        mSearchType = type;
        mWebView.clearHistory();
        mFirstPage = true;
        mWebView.loadUrl(getOriUrl());
    }

    private int getSearchInfo(String key) {
        SharedPreferences preferences = getSharedPreferences("setting", Context.MODE_PRIVATE);
        int defaultValue = TYPE_SHENMA;
        if (key.equals(SEARCH_DICT)) {
            defaultValue = TYPE_BINGDICT;
        } else if (key.equals(SEARCH_WIKI)) {
            defaultValue = TYPE_WIKI;
        }
        return preferences.getInt(key, defaultValue);
    }

    private void putSearchInfo(String key, int value) {
        SharedPreferences preferences = getSharedPreferences("setting", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    @Override
    protected void loadUrl() {
    }
}
