package com.zaka.xinghe.ui.page;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zaka.xinghe.R;
import com.zaka.xinghe.model.Record;
import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.store.DataStore;
import com.zaka.xinghe.store.SiteStore;
import com.zaka.xinghe.ui.Page;
import com.zaka.xinghe.ui.SearchActivity;
import com.zaka.xinghe.ui.SiteActivity;
import com.zaka.xinghe.ui.WebActivity;
import com.zaka.xinghe.util.Utils;

import java.util.ArrayList;
import java.util.List;

/** 我的页：当前接口 + 功能网格 + 其它。 */
public class PageMine extends Page {

    private LinearLayout curSiteBox;
    private LinearLayout otherBox;
    private GridView menuGrid;
    private TextView tvVersion;

    private static final String[] MENU_TITLE = {
            "接口管理", "搜索", "收藏", "历史",
            "直播源", "清缓存", "关于", "退出"
    };
    private static final int[] MENU_ICON = {
            R.drawable.ic_tab_home, R.drawable.ic_search, R.drawable.ic_star_on, R.drawable.ic_tab_history,
            R.drawable.ic_tab_live, R.drawable.ic_refresh, R.drawable.ic_tab_mine, R.drawable.ic_close
    };

    public PageMine(Context c) {
        super(c);
    }

    @Override
    protected void onInit() {
        LayoutInflater.from(getContext()).inflate(R.layout.page_mine, this, true);
        curSiteBox = (LinearLayout) findViewById(R.id.curSiteBox);
        otherBox = (LinearLayout) findViewById(R.id.otherBox);
        menuGrid = (GridView) findViewById(R.id.menuGrid);
        tvVersion = (TextView) findViewById(R.id.tvVersion);

        tvVersion.setText("版本 " + versionName());
        menuGrid.setAdapter(new MenuAdapter());
        menuGrid.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                onMenu(position);
            }
        });

        otherBox.removeAllViews();
        addRow(otherBox, "关于本应用", "", new Runnable() {
            @Override
            public void run() {
                showAbout();
            }
        });
        addRow(otherBox, "清空播放历史", "", new Runnable() {
            @Override
            public void run() {
                DataStore.clearHistory();
                Utils.toast("已清空播放历史");
            }
        });
        addRow(otherBox, "清理图片缓存", "", new Runnable() {
            @Override
            public void run() {
                com.zaka.xinghe.util.ImgLoader.init(getContext());
                Utils.toast("已清理");
            }
        });

        refreshSite();
    }

    @Override
    protected void onReShow() {
        refreshSite();
    }

    private String versionName() {
        try {
            PackageInfo info = getContext().getPackageManager()
                    .getPackageInfo(getContext().getPackageName(), 0);
            return info.versionName == null ? "1.0.0" : info.versionName;
        } catch (Throwable t) {
            return "1.0.0";
        }
    }

    private void refreshSite() {
        curSiteBox.removeAllViews();
        Site s = SiteStore.current();
        if (s == null) {
            addRow(curSiteBox, "还没有配置接口", "点这里添加", new Runnable() {
                @Override
                public void run() {
                    getContext().startActivity(new Intent(getContext(), SiteActivity.class));
                }
            });
        } else {
            addRow(curSiteBox, s.name, s.api, new Runnable() {
                @Override
                public void run() {
                    getContext().startActivity(new Intent(getContext(), SiteActivity.class));
                }
            });
        }
    }

    private void addRow(LinearLayout box, String title, String value, final Runnable click) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.item_row, box, false);
        ((TextView) v.findViewById(R.id.tvTitle)).setText(title);
        TextView tvValue = (TextView) v.findViewById(R.id.tvValue);
        if (Utils.empty(value)) {
            tvValue.setVisibility(View.GONE);
        } else {
            tvValue.setVisibility(View.VISIBLE);
            tvValue.setText(value);
        }
        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (click != null) click.run();
            }
        });
        box.addView(v);
        View line = new View(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        line.setLayoutParams(lp);
        line.setBackgroundColor(0x1affffff);
        box.addView(line);
    }

    private void onMenu(int pos) {
        switch (pos) {
            case 0:
                getContext().startActivity(new Intent(getContext(), SiteActivity.class));
                break;
            case 1:
                getContext().startActivity(new Intent(getContext(), SearchActivity.class));
                break;
            case 2:
                openHistory(true);
                break;
            case 3:
                openHistory(false);
                break;
            case 4:
                Utils.toast("在「直播」页点右上角刷新按钮填写直播源");
                break;
            case 5:
                com.zaka.xinghe.util.ImgLoader.init(getContext());
                Utils.toast("缓存已清理");
                break;
            case 6:
                showAbout();
                break;
            case 7:
                System.exit(0);
                break;
            default:
                break;
        }
    }

    private void openHistory(boolean fav) {
        Utils.toast(fav ? "收藏在「历史/收藏」页" : "历史在「历史/收藏」页");
    }

    private void showAbout() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.about_title)
                .setMessage("星河 · 影视仓复刻版\n\n"
                        + "纯自研实现的本地影视客户端：\n"
                        + "· 支持苹果 CMS 采集接口（JSON / XML）\n"
                        + "· 分类 / 筛选 / 搜索 / 选集 / 播放全链路\n"
                        + "· 在线字幕（迅雷源）+ 自绘字幕层 + 毫秒级偏移调整\n"
                        + "· 观看历史、收藏、播放进度记忆\n\n"
                        + "本应用只做聚合播放，不存储、不传播任何影视内容。")
                .setPositiveButton("知道了", null)
                .show();
    }

    /** 功能网格适配器。 */
    private class MenuAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return MENU_TITLE.length;
        }

        @Override
        public Object getItem(int position) {
            return MENU_TITLE[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_menu, parent, false);
            }
            ImageView icon = (ImageView) convertView.findViewById(R.id.ivIcon);
            TextView title = (TextView) convertView.findViewById(R.id.tvTitle);
            icon.setImageResource(MENU_ICON[position]);
            icon.setColorFilter(0xffff5f00);
            title.setText(MENU_TITLE[position]);
            return convertView;
        }
    }
}
