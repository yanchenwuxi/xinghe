package com.zaka.xinghe.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zaka.xinghe.App;
import com.zaka.xinghe.R;
import com.zaka.xinghe.adapter.VodAdapter;
import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.model.Vod;
import com.zaka.xinghe.net.CmsApi;
import com.zaka.xinghe.store.DataStore;
import com.zaka.xinghe.store.SiteStore;
import com.zaka.xinghe.util.Utils;

import java.util.List;

/** 搜索页。 */
public class SearchActivity extends Activity {

    private EditText etKeyword;
    private GridView grid;
    private LinearLayout historyBox;
    private LinearLayout stateBox;
    private TextView stateText;
    private com.zaka.xinghe.view.FlowLayout flowHistory;
    private com.zaka.xinghe.view.FlowLayout flowHot;
    private VodAdapter adapter;

    private static final String[] HOT = {
            "庆余年", "狂飙", "漫长的季节", "三体", "流浪地球", "甄嬛传",
            "琅琊榜", "人民的名义", "隐秘的角落", "琅琊榜之风起长林"
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_search);
        etKeyword = (EditText) findViewById(R.id.etKeyword);
        grid = (GridView) findViewById(R.id.grid);
        historyBox = (LinearLayout) findViewById(R.id.historyBox);
        stateBox = (LinearLayout) findViewById(R.id.stateBox);
        stateText = (TextView) findViewById(R.id.stateText);
        flowHistory = (com.zaka.xinghe.view.FlowLayout) findViewById(R.id.flowHistory);
        flowHot = (com.zaka.xinghe.view.FlowLayout) findViewById(R.id.flowHot);

        adapter = new VodAdapter(this, new VodAdapter.OnClick() {
            @Override
            public void onClick(Vod v) {
                DetailActivity.open(SearchActivity.this, v);
            }
        });
        grid.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.btnSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSearch(etKeyword.getText().toString().trim());
            }
        });
        findViewById(R.id.btnClearHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataStore.clearSearchWord();
                buildHistory();
            }
        });
        etKeyword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    doSearch(etKeyword.getText().toString().trim());
                    return true;
                }
                return false;
            }
        });

        buildHistory();
        buildHot();
    }

    private void buildHistory() {
        flowHistory.removeAllViews();
        List<String> words = DataStore.searchHistory();
        if (words.size() == 0) {
            historyBox.setVisibility(View.GONE);
            return;
        }
        historyBox.setVisibility(View.VISIBLE);
        for (final String w : words) {
            flowHistory.addView(chip(w));
        }
    }

    private void buildHot() {
        flowHot.removeAllViews();
        for (String w : HOT) {
            flowHot.addView(chip(w));
        }
    }

    private TextView chip(final String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13f);
        tv.setPadding(Utils.dp(14), Utils.dp(7), Utils.dp(14), Utils.dp(7));
        tv.setTextColor(0xb3ffffff);
        tv.setBackgroundResource(R.drawable.bg_chip);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etKeyword.setText(text);
                etKeyword.setSelection(text.length());
                doSearch(text);
            }
        });
        return tv;
    }

    private void doSearch(final String wd) {
        if (Utils.empty(wd)) {
            Utils.toast("先输入片名");
            return;
        }
        DataStore.addSearchWord(wd);
        buildHistory();
        final Site site = SiteStore.current();
        if (site == null) {
            Utils.toast("还没有配置接口");
            return;
        }
        stateBox.setVisibility(View.VISIBLE);
        stateText.setText(R.string.loading);
        grid.setVisibility(View.INVISIBLE);
        App.run(new App.Task() {
            @Override
            public Object call() throws Exception {
                return CmsApi.search(site, wd, 1);
            }
        }, new App.Done() {
            @Override
            public void onDone(Object result, Exception err) {
                if (isFinishing()) return;
                if (err != null || result == null) {
                    stateText.setText(R.string.net_error);
                    return;
                }
                List<Vod> list = (List<Vod>) result;
                adapter.setData(list);
                grid.setVisibility(View.VISIBLE);
                if (list.size() == 0) {
                    stateBox.setVisibility(View.VISIBLE);
                    stateText.setText(R.string.empty_search);
                } else {
                    stateBox.setVisibility(View.GONE);
                }
            }
        });
    }
}
