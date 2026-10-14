package com.zaka.xinghe.ui.page;

import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.zaka.xinghe.App;
import com.zaka.xinghe.R;
import com.zaka.xinghe.adapter.ChannelAdapter;
import com.zaka.xinghe.net.Http;
import com.zaka.xinghe.store.DataStore;
import com.zaka.xinghe.store.SiteStore;
import com.zaka.xinghe.model.LiveSource;
import com.zaka.xinghe.ui.Page;
import com.zaka.xinghe.ui.PlayActivity;
import com.zaka.xinghe.util.Utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 直播页：解析 m3u / txt 直播源，分组浏览。 */
public class PageLive extends Page {

    private ListView list;
    private LinearLayout groupBar;
    private TextView emptyText;
    private ChannelAdapter adapter;

    private final List<ChannelAdapter.Channel> all = new ArrayList<ChannelAdapter.Channel>();
    private final Map<String, List<ChannelAdapter.Channel>> groups =
            new LinkedHashMap<String, List<ChannelAdapter.Channel>>();
    private String curGroup = "";

    public PageLive(Context c) {
        super(c);
    }

    @Override
    protected void onInit() {
        LayoutInflater.from(getContext()).inflate(R.layout.page_live, this, true);
        list = (ListView) findViewById(R.id.list);
        groupBar = (LinearLayout) findViewById(R.id.groupBar);
        emptyText = (TextView) findViewById(R.id.emptyText);
        adapter = new ChannelAdapter(getContext());
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChannelAdapter.Channel ch = adapter.data().get(position);
                PlayActivity.playLive(getContext(), ch.name, ch.url);
            }
        });
        findViewById(R.id.btnRefresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askUrl();
            }
        });
        load();
    }

    @Override
    protected void reload() {
        load();
    }

    private void askUrl() {
        final EditText et = new EditText(getContext());
        et.setHint("m3u / txt 直播源地址");
        et.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        et.setTextColor(0xffffffff);
        et.setHintTextColor(0x80ffffff);
        et.setSingleLine(true);
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("直播源")
                .setView(et)
                .setPositiveButton("保存并加载", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        DataStore.put("live_url", et.getText().toString().trim());
                        load();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void load() {
        String url = DataStore.get("live_url", "");
        // 若用户未手动设置，尝试使用配置里导入的 lives
        if (Utils.empty(url)) {
            java.util.List<LiveSource> lives = SiteStore.lives();
            if (lives != null && lives.size() > 0) {
                url = lives.get(0).url;
            }
        }
        if (Utils.empty(url)) {
            all.clear();
            groups.clear();
            groupBar.removeAllViews();
            adapter.setData(null);
            emptyText.setText("没有直播源\n点右上角刷新按钮添加");
            emptyText.setVisibility(View.VISIBLE);
            return;
        }
        emptyText.setText(R.string.loading);
        emptyText.setVisibility(View.VISIBLE);
        final String fetchUrl = url;
        App.run(new App.Task() {
            @Override
            public Object call() throws Exception {
                return parse(Http.get(fetchUrl.trim()));
            }
        }, new App.Done() {
            @Override
            public void onDone(Object result, Exception err) {
                if (err != null || result == null) {
                    emptyText.setText("直播源解析失败");
                    return;
                }
                build((List<ChannelAdapter.Channel>) result);
            }
        });
    }

    /** 兼容标准 m3u(#EXTINF 带 group-title) 与「分组,#genre# / 频道名,地址」两种格式。 */
    private List<ChannelAdapter.Channel> parse(String text) {
        List<ChannelAdapter.Channel> out = new ArrayList<ChannelAdapter.Channel>();
        if (text == null) return out;
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        String curGroupName = "默认";
        String pending = "";
        for (String raw : lines) {
            String line = raw.trim();
            if (line.length() == 0) continue;
            if (line.startsWith("#EXTINF")) {
                int gi = line.indexOf("group-title=\"");
                if (gi >= 0) {
                    int s = gi + "group-title=\"".length();
                    int e = line.indexOf('"', s);
                    if (e > s) curGroupName = line.substring(s, e).trim();
                }
                int comma = line.lastIndexOf(',');
                pending = comma >= 0 ? line.substring(comma + 1).trim() : "";
                continue;
            }
            if (line.startsWith("#")) continue;
            if (line.endsWith("#genre#")) {
                curGroupName = line.substring(0, line.length() - "#genre#".length()).trim();
                if (curGroupName.length() == 0) curGroupName = "默认";
                continue;
            }
            String name;
            String url;
            int c = line.indexOf(',');
            if (c > 0) {
                name = line.substring(0, c).trim();
                url = line.substring(c + 1).trim();
            } else {
                name = Utils.empty(pending) ? "频道" : pending;
                url = line;
            }
            if (Utils.empty(url)) continue;
            if (!url.startsWith("http") && !url.startsWith("rtmp") && !url.startsWith("rtsp")) continue;
            ChannelAdapter.Channel ch = new ChannelAdapter.Channel();
            ch.name = Utils.empty(name) ? "频道" : name;
            ch.url = url;
            ch.group = curGroupName;
            out.add(ch);
            pending = "";
        }
        return out;
    }

    private void build(List<ChannelAdapter.Channel> data) {
        all.clear();
        all.addAll(data);
        groups.clear();
        for (ChannelAdapter.Channel ch : all) {
            List<ChannelAdapter.Channel> g = groups.get(ch.group);
            if (g == null) {
                g = new ArrayList<ChannelAdapter.Channel>();
                groups.put(ch.group, g);
            }
            g.add(ch);
        }
        buildGroupBar();
        if (groups.size() > 0) {
            showGroup(groups.keySet().iterator().next());
        } else {
            emptyText.setText("没有解析到频道");
            emptyText.setVisibility(View.VISIBLE);
        }
    }

    private void buildGroupBar() {
        groupBar.removeAllViews();
        for (final String g : groups.keySet()) {
            TextView tv = new TextView(getContext());
            tv.setText(g + " (" + groups.get(g).size() + ")");
            tv.setTextSize(13f);
            tv.setPadding(Utils.dp(12), Utils.dp(5), Utils.dp(12), Utils.dp(5));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = Utils.dp(8);
            tv.setLayoutParams(lp);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showGroup(g);
                }
            });
            groupBar.addView(tv);
        }
        markGroup();
    }

    private void markGroup() {
        for (int i = 0; i < groupBar.getChildCount(); i++) {
            TextView tv = (TextView) groupBar.getChildAt(i);
            String text = tv.getText().toString();
            boolean on = text.startsWith(curGroup + " ");
            tv.setTextColor(on ? 0xffffffff : 0xb3ffffff);
            tv.setBackgroundResource(on ? R.drawable.bg_chip_on : R.drawable.bg_chip);
        }
    }

    private void showGroup(String g) {
        curGroup = g;
        markGroup();
        List<ChannelAdapter.Channel> data = groups.get(g);
        adapter.setData(data);
        emptyText.setVisibility(adapter.getCount() == 0 ? View.VISIBLE : View.GONE);
    }
}
