package com.zaka.xinghe.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.zaka.xinghe.R;
import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.store.SiteStore;

import java.util.ArrayList;
import java.util.List;

/** 站点（接口）列表适配器。 */
public class SiteAdapter extends BaseAdapter {

    private final List<Site> data = new ArrayList<Site>();
    private final LayoutInflater inf;

    public SiteAdapter(Context c) {
        this.inf = LayoutInflater.from(c);
    }

    public void setData(List<Site> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    public List<Site> data() {
        return data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inf.inflate(R.layout.item_site, parent, false);
        }
        Site s = data.get(position);
        String name = s.name;
        if (s.isSpiderOrScript()) name = name + " [脚本]";
        else if (s.isXml()) name = name + " [XML]";
        ((TextView) convertView.findViewById(R.id.tvName)).setText(name);
        ((TextView) convertView.findViewById(R.id.tvUrl)).setText(s.api);
        boolean cur = s.key.equals(SiteStore.currentKey());
        convertView.findViewById(R.id.tvState).setVisibility(cur ? View.VISIBLE : View.GONE);
        TextView btn = (TextView) convertView.findViewById(R.id.btnSwitch);
        if (btn != null) btn.setText(cur ? "重新加载" : "切换");
        return convertView;
    }
}
