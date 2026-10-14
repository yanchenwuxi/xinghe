package com.zaka.xinghe.ui.page;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zaka.xinghe.App;
import com.zaka.xinghe.R;
import com.zaka.xinghe.adapter.VodAdapter;
import com.zaka.xinghe.model.Cate;
import com.zaka.xinghe.model.FilterGroup;
import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.model.Vod;
import com.zaka.xinghe.net.CmsApi;
import com.zaka.xinghe.store.SiteStore;
import com.zaka.xinghe.ui.DetailActivity;
import com.zaka.xinghe.ui.Page;
import com.zaka.xinghe.ui.SearchActivity;
import com.zaka.xinghe.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 首页：站点名 + 搜索框 + 分类 Tab + 筛选 + 海报网格。 */
public class PageHome extends Page {

    private TextView siteName;
    private TextView searchHint;
    private TextView footState;
    private TextView stateText;
    private LinearLayout cateBar;
    private LinearLayout filterBar;
    private LinearLayout stateBox;
    private HorizontalScrollView filterScroll;
    private GridView grid;
    private VodAdapter adapter;

    private final List<Cate> cates = new ArrayList<Cate>();
    private final List<FilterGroup> filters = new ArrayList<FilterGroup>();
    private final Map<String, String> filterMap = new HashMap<String, String>();

    private String curCateId = "";
    private int pageNo = 1;
    private boolean loading = false;
    private boolean noMore = false;

    public PageHome(Context c) {
        super(c);
    }

    @Override
    protected void onInit() {
        LayoutInflater.from(getContext()).inflate(R.layout.page_home, this, true);

        siteName = (TextView) findViewById(R.id.siteName);
        searchHint = (TextView) findViewById(R.id.searchHint);
        footState = (TextView) findViewById(R.id.footState);
        stateText = (TextView) findViewById(R.id.stateText);
        stateBox = (LinearLayout) findViewById(R.id.stateBox);
        cateBar = (LinearLayout) findViewById(R.id.cateBar);
        filterBar = (LinearLayout) findViewById(R.id.filterBar);
        filterScroll = (HorizontalScrollView) findViewById(R.id.filterScroll);
        grid = (GridView) findViewById(R.id.grid);

        adapter = new VodAdapter(getContext(), new VodAdapter.OnClick() {
            @Override
            public void onClick(Vod v) {
                DetailActivity.open(getContext(), v);
            }
        });
        grid.setAdapter(adapter);
        grid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int first, int visible, int total) {
                if (total > 0 && first + visible >= total - 4) {
                    loadMore();
                }
            }
        });

        findViewById(R.id.searchBar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().startActivity(new Intent(getContext(), SearchActivity.class));
            }
        });
        findViewById(R.id.btnRefresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reloadAll();
            }
        });

        reloadAll();
    }

    @Override
    protected void onReShow() {
        refreshSiteName();
    }

    @Override
    protected void reload() {
        reloadAll();
    }

    private void refreshSiteName() {
        Site s = SiteStore.current();
        siteName.setText(s == null ? getContext().getString(R.string.app_name) : s.name);
    }

    private void reloadAll() {
        refreshSiteName();
        cates.clear();
        filters.clear();
        filterMap.clear();
        curCateId = "";
        cateBar.removeAllViews();
        filterBar.removeAllViews();
        filterScroll.setVisibility(View.GONE);
        adapter.setData(null);
        showState(getContext().getString(R.string.loading));

        final Site site = SiteStore.current();
        if (site == null) {
            showState("还没有配置接口，去「我的 → 接口管理」添加一个");
            return;
        }
        App.run(new App.Task() {
            @Override
            public Object call() throws Exception {
                return CmsApi.getCates(site);
            }
        }, new App.Done() {
            @Override
            public void onDone(Object result, Exception err) {
                if (err != null || result == null || ((List<Cate>) result).size() == 0) {
                    showState("站点解析失败，换个接口试试");
                    return;
                }
                cates.clear();
                cates.addAll((List<Cate>) result);
                buildCateBar();
                selectCate(cates.get(0));
            }
        });
    }

    // ------------------------------------------------------------- 分类

    private void buildCateBar() {
        cateBar.removeAllViews();
        for (int i = 0; i < cates.size(); i++) {
            final Cate c = cates.get(i);
            TextView tv = new TextView(getContext());
            tv.setText(c.name);
            tv.setTextSize(16f);
            tv.setPadding(Utils.dp(2), Utils.dp(6), Utils.dp(2), Utils.dp(6));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = Utils.dp(18);
            tv.setLayoutParams(lp);
            tv.setTextColor(0xb3ffffff);
            tv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectCate(c);
                }
            });
            cateBar.addView(tv);
        }
        markCate();
    }

    private void markCate() {
        for (int i = 0; i < cateBar.getChildCount() && i < cates.size(); i++) {
            TextView tv = (TextView) cateBar.getChildAt(i);
            boolean on = cates.get(i).id.equals(curCateId);
            tv.setTextColor(on ? 0xffffffff : 0xb3ffffff);
            tv.getPaint().setFakeBoldText(on);
        }
    }

    private void selectCate(Cate c) {
        curCateId = c.id;
        markCate();
        // 分类切换 -> 重新取筛选
        filters.clear();
        filterMap.clear();
        filterBar.removeAllViews();
        filterScroll.setVisibility(View.GONE);
        adapter.setData(null);
        pageNo = 1;
        noMore = false;
        loadList(true);
        loadFilters(c.id);
    }

    private void loadFilters(final String cateId) {
        final Site site = SiteStore.current();
        if (site == null) return;
        App.run(new App.Task() {
            @Override
            public Object call() throws Exception {
                return CmsApi.getFilters(site, cateId);
            }
        }, new App.Done() {
            @Override
            public void onDone(Object result, Exception err) {
                if (err != null || result == null) return;
                if (!cateId.equals(curCateId)) return;
                filters.clear();
                filters.addAll((List<FilterGroup>) result);
                buildFilterBar();
            }
        });
    }

    private void buildFilterBar() {
        filterBar.removeAllViews();
        if (filters.size() == 0) {
            filterScroll.setVisibility(View.GONE);
            return;
        }
        filterScroll.setVisibility(View.VISIBLE);
        for (int i = 0; i < filters.size(); i++) {
            final FilterGroup g = filters.get(i);
            TextView tv = new TextView(getContext());
            tv.setText(g.label() + " ▾");
            tv.setTextSize(12f);
            tv.setPadding(Utils.dp(10), Utils.dp(4), Utils.dp(10), Utils.dp(4));
            tv.setTextColor(0xb3ffffff);
            tv.setBackgroundResource(Utils.empty(g.selected) ? R.drawable.bg_chip : R.drawable.bg_chip_on);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = Utils.dp(8);
            lp.topMargin = Utils.dp(4);
            lp.bottomMargin = Utils.dp(4);
            tv.setLayoutParams(lp);
            tv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    pickFilter(g);
                }
            });
            filterBar.addView(tv);
        }
    }

    private void pickFilter(final FilterGroup g) {
        final List<String> opts = new ArrayList<String>();
        opts.add("全部");
        opts.addAll(g.values);
        new AlertDialog.Builder(getContext())
                .setTitle(g.name)
                .setItems(opts.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String val = which == 0 ? "" : opts.get(which);
                        g.selected = val;
                        if (Utils.empty(val)) {
                            filterMap.remove(g.key);
                        } else {
                            filterMap.put(g.key, val);
                        }
                        buildFilterBar();
                        adapter.setData(null);
                        pageNo = 1;
                        noMore = false;
                        loadList(true);
                    }
                })
                .show();
    }

    // ------------------------------------------------------------- 列表

    private void loadList(final boolean reset) {
        if (loading) return;
        final Site site = SiteStore.current();
        if (site == null) return;
        loading = true;
        if (reset) showState(null);
        final int reqPage = reset ? 1 : pageNo;
        App.run(new App.Task() {
            @Override
            public Object call() throws Exception {
                return CmsApi.getList(site, curCateId, reqPage, filterMap);
            }
        }, new App.Done() {
            @Override
            public void onDone(Object result, Exception err) {
                loading = false;
                if (err != null || result == null) {
                    if (reset) {
                        showState("加载失败，点一下重试");
                    } else {
                        footState.setText(R.string.load_failed);
                    }
                    return;
                }
                List<Vod> list = (List<Vod>) result;
                if (reset) adapter.setData(list);
                else adapter.addData(list);
                if (adapter.getCount() == 0) {
                    showState(getContext().getString(R.string.empty_data));
                } else {
                    hideState();
                    pageNo = reqPage + 1;
                    if (list.size() < 10) {
                        noMore = true;
                        footState.setText(R.string.no_more);
                    } else {
                        noMore = false;
                        footState.setText("");
                    }
                }
            }
        });
    }

    private void loadMore() {
        if (loading || noMore || adapter.getCount() == 0) return;
        footState.setText(R.string.loading_more);
        loadList(false);
    }

    private void showState(String msg) {
        if (msg == null) {
            stateBox.setVisibility(View.GONE);
            return;
        }
        stateText.setText(msg);
        stateBox.setVisibility(View.VISIBLE);
        stateBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                reloadAll();
            }
        });
    }

    private void hideState() {
        stateBox.setVisibility(View.GONE);
        stateBox.setOnClickListener(null);
    }

    /** 供其它页面复用的站点名刷新入口。 */
    public String currentSiteName() {
        Site s = SiteStore.current();
        return s == null ? "" : s.name;
    }
}
