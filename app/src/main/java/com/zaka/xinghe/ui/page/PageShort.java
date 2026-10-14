package com.zaka.xinghe.ui.page;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.TextView;

import com.zaka.xinghe.App;
import com.zaka.xinghe.R;
import com.zaka.xinghe.adapter.VodAdapter;
import com.zaka.xinghe.model.Cate;
import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.model.Vod;
import com.zaka.xinghe.net.CmsApi;
import com.zaka.xinghe.store.SiteStore;
import com.zaka.xinghe.ui.DetailActivity;
import com.zaka.xinghe.ui.Page;
import com.zaka.xinghe.util.Utils;

import java.util.HashMap;
import java.util.List;

/** 短剧页：取当前站点里「短剧」分类的数据。 */
public class PageShort extends Page {

    private GridView grid;
    private TextView emptyText;
    private TextView footState;
    private TextView tvSite;
    private VodAdapter adapter;

    private String cateId = "";
    private int pageNo = 1;
    private boolean loading = false;
    private boolean noMore = false;

    public PageShort(Context c) {
        super(c);
    }

    @Override
    protected void onInit() {
        LayoutInflater.from(getContext()).inflate(R.layout.page_short, this, true);
        grid = (GridView) findViewById(R.id.grid);
        emptyText = (TextView) findViewById(R.id.emptyText);
        footState = (TextView) findViewById(R.id.footState);
        tvSite = (TextView) findViewById(R.id.tvSite);
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
                if (total > 0 && first + visible >= total - 4) loadMore();
            }
        });
        load();
    }

    @Override
    protected void reload() {
        cateId = "";
        adapter.setData(null);
        load();
    }

    private void load() {
        Site s = SiteStore.current();
        tvSite.setText(s == null ? "未配置接口" : s.name);
        if (s == null) {
            emptyText.setText("还没有配置接口");
            emptyText.setVisibility(View.VISIBLE);
            return;
        }
        emptyText.setText(R.string.loading);
        emptyText.setVisibility(View.VISIBLE);
        App.run(new App.Task() {
            @Override
            public Object call() throws Exception {
                Site site = SiteStore.current();
                List<Cate> cates = CmsApi.getCates(site);
                String id = "";
                for (Cate c : cates) {
                    if (c.name != null && (c.name.contains("短剧") || c.name.contains("短視"))) {
                        id = c.id;
                        break;
                    }
                }
                if (Utils.empty(id)) return null;
                cateId = id;
                return CmsApi.getList(site, id, 1, new HashMap<String, String>());
            }
        }, new App.Done() {
            @Override
            public void onDone(Object result, Exception err) {
                if (err != null || result == null) {
                    emptyText.setText("这个接口里没有短剧分类，换个接口试试");
                    emptyText.setVisibility(View.VISIBLE);
                    return;
                }
                List<Vod> list = (List<Vod>) result;
                adapter.setData(list);
                pageNo = 2;
                emptyText.setVisibility(adapter.getCount() == 0 ? View.VISIBLE : View.GONE);
                if (adapter.getCount() == 0) emptyText.setText("暂无短剧数据");
            }
        });
    }

    private void loadMore() {
        if (loading || noMore || adapter.getCount() == 0 || TextUtils.isEmpty(cateId)) return;
        loading = true;
        footState.setText(R.string.loading_more);
        App.run(new App.Task() {
            @Override
            public Object call() throws Exception {
                return CmsApi.getList(SiteStore.current(), cateId, pageNo, new HashMap<String, String>());
            }
        }, new App.Done() {
            @Override
            public void onDone(Object result, Exception err) {
                loading = false;
                if (err != null || result == null) {
                    footState.setText("");
                    return;
                }
                List<Vod> list = (List<Vod>) result;
                adapter.addData(list);
                pageNo++;
                if (list.size() < 10) {
                    noMore = true;
                    footState.setText(R.string.no_more);
                } else {
                    footState.setText("");
                }
            }
        });
    }
}
