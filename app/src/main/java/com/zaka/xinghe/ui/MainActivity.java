package com.zaka.xinghe.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zaka.xinghe.R;
import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.store.SiteStore;
import com.zaka.xinghe.ui.page.PageHistory;
import com.zaka.xinghe.ui.page.PageHome;
import com.zaka.xinghe.ui.page.PageLive;
import com.zaka.xinghe.ui.page.PageMine;
import com.zaka.xinghe.ui.page.PageShort;
import com.zaka.xinghe.util.Utils;

/** 主界面：五个 Tab + 底部导航（主页 / 直播 / 短剧 / 历史收藏 / 我的）。 */
public class MainActivity extends Activity {

    private static final String[] TAB_TITLE = {"主页", "直播", "短剧", "历史/收藏", "我的"};
    private static final int[] TAB_ICON = {
            R.drawable.ic_tab_home, R.drawable.ic_tab_live, R.drawable.ic_tab_short,
            R.drawable.ic_tab_history, R.drawable.ic_tab_mine
    };

    private FrameLayout pageContainer;
    private LinearLayout tabContainer;
    private final Page[] pages = new Page[5];
    private int current = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        pageContainer = (FrameLayout) findViewById(R.id.pageContainer);
        tabContainer = (LinearLayout) findViewById(R.id.tabContainer);

        pages[0] = new PageHome(this);
        pages[1] = new PageLive(this);
        pages[2] = new PageShort(this);
        pages[3] = new PageHistory(this);
        pages[4] = new PageMine(this);
        for (Page p : pages) {
            p.setVisibility(View.GONE);
            pageContainer.addView(p);
        }

        buildTabs();
        switchTo(0);
    }

    private void buildTabs() {
        tabContainer.removeAllViews();
        for (int i = 0; i < TAB_TITLE.length; i++) {
            final int index = i;
            View item = LayoutInflater.from(this).inflate(R.layout.item_tab, tabContainer, false);
            ((TextView) item.findViewById(R.id.tabLabel)).setText(TAB_TITLE[i]);
            ((ImageView) item.findViewById(R.id.tabIcon)).setImageResource(TAB_ICON[i]);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchTo(index);
                }
            });
            tabContainer.addView(item);
        }
        markTab();
    }

    private void markTab() {
        for (int i = 0; i < tabContainer.getChildCount(); i++) {
            View item = tabContainer.getChildAt(i);
            boolean on = i == current;
            ImageView icon = (ImageView) item.findViewById(R.id.tabIcon);
            TextView label = (TextView) item.findViewById(R.id.tabLabel);
            icon.setColorFilter(on ? 0xffff5f00 : 0xb3ffffff);
            label.setTextColor(on ? 0xffff5f00 : 0xb3ffffff);
        }
    }

    private void switchTo(int index) {
        if (index < 0 || index >= pages.length) return;
        if (current != index && pages[current] != null) pages[current].onHide();
        current = index;
        for (int i = 0; i < pages.length; i++) {
            pages[i].setVisibility(i == index ? View.VISIBLE : View.GONE);
        }
        pages[index].onShow();
        markTab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 站点可能在接口管理页被切过，回来时同步
        if (pages[current] != null) pages[current].onShow();
    }

    @Override
    public void onBackPressed() {
        if (current != 0) {
            switchTo(0);
            return;
        }
        super.onBackPressed();
    }
}
