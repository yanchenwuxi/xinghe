package com.zaka.xinghe.ui.page;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.zaka.xinghe.R;
import com.zaka.xinghe.adapter.VodAdapter;
import com.zaka.xinghe.model.Record;
import com.zaka.xinghe.model.Vod;
import com.zaka.xinghe.store.DataStore;
import com.zaka.xinghe.ui.DetailActivity;
import com.zaka.xinghe.ui.Page;
import com.zaka.xinghe.util.Utils;

import java.util.ArrayList;
import java.util.List;

/** 历史 / 收藏页。 */
public class PageHistory extends Page {

    private GridView grid;
    private TextView emptyText;
    private TextView segHistory;
    private TextView segFav;
    private TextView btnClear;
    private VodAdapter adapter;
    private boolean showFav = false;

    public PageHistory(Context c) {
        super(c);
    }

    @Override
    protected void onInit() {
        LayoutInflater.from(getContext()).inflate(R.layout.page_history, this, true);
        grid = (GridView) findViewById(R.id.grid);
        emptyText = (TextView) findViewById(R.id.emptyText);
        segHistory = (TextView) findViewById(R.id.segHistory);
        segFav = (TextView) findViewById(R.id.segFav);
        btnClear = (TextView) findViewById(R.id.btnClear);

        adapter = new VodAdapter(getContext(), new VodAdapter.OnClick() {
            @Override
            public void onClick(Vod v) {
                DetailActivity.open(getContext(), v);
            }
        });
        grid.setAdapter(adapter);
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Vod v = adapter.data().get(position);
                new android.app.AlertDialog.Builder(getContext())
                        .setTitle(v.name)
                        .setItems(new String[]{"删除这条记录"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                removeRecord(v);
                                refresh();
                            }
                        })
                        .show();
                return true;
            }
        });

        segHistory.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showFav = false;
                refresh();
            }
        });
        segFav.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showFav = true;
                refresh();
            }
        });
        btnClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new android.app.AlertDialog.Builder(getContext())
                        .setTitle("确认清空？")
                        .setMessage(showFav ? "将清空全部收藏" : "将清空观看历史（收藏会保留）")
                        .setPositiveButton("清空", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                if (showFav) clearFav();
                                else DataStore.clearHistory();
                                refresh();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
        refresh();
    }

    @Override
    protected void onReShow() {
        refresh();
    }

    private void removeRecord(Vod v) {
        List<Record> list = DataStore.all();
        List<Record> keep = new ArrayList<Record>();
        String key = v.siteUrl + "@" + v.id;
        for (Record r : list) {
            if (!r.key().equals(key)) keep.add(r);
        }
        DataStore.putAll(keep);
    }

    private void clearFav() {
        List<Record> list = DataStore.all();
        for (Record r : list) r.fav = false;
        DataStore.putAll(list);
    }

    private void refresh() {
        segHistory.setBackgroundResource(showFav ? R.drawable.bg_chip : R.drawable.bg_chip_on);
        segFav.setBackgroundResource(showFav ? R.drawable.bg_chip_on : R.drawable.bg_chip);
        segHistory.setTextColor(showFav ? 0xb3ffffff : 0xffffffff);
        segFav.setTextColor(showFav ? 0xffffffff : 0xb3ffffff);

        List<Record> records = showFav ? DataStore.favs() : DataStore.history();
        List<Vod> vods = new ArrayList<Vod>();
        for (Record r : records) {
            Vod v = new Vod();
            v.id = r.vodId;
            v.siteUrl = r.siteUrl;
            v.siteName = r.siteName;
            v.name = r.name;
            v.pic = r.pic;
            v.note = r.epName;
            vods.add(v);
        }
        adapter.setData(vods);
        if (vods.size() == 0) {
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText(showFav ? getContext().getString(R.string.empty_fav)
                    : getContext().getString(R.string.empty_history));
        } else {
            emptyText.setVisibility(View.GONE);
        }
        if (Utils.empty("")) {
            // keep lint quiet
        }
    }
}
