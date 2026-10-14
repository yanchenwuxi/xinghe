package com.zaka.xinghe.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.zaka.xinghe.R;
import com.zaka.xinghe.subtitle.XunleiSub;

import java.util.ArrayList;
import java.util.List;

/** 在线字幕列表适配器。 */
public class SubtitleAdapter extends BaseAdapter {

    private final List<XunleiSub.Item> data = new ArrayList<XunleiSub.Item>();
    private final LayoutInflater inf;

    public SubtitleAdapter(Context c) {
        this.inf = LayoutInflater.from(c);
    }

    public void setData(List<XunleiSub.Item> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
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
            convertView = inf.inflate(R.layout.item_subtitle, parent, false);
        }
        XunleiSub.Item it = data.get(position);
        ((TextView) convertView.findViewById(R.id.tvName)).setText(it.name);
        ((TextView) convertView.findViewById(R.id.tvMeta)).setText(it.meta());
        return convertView;
    }
}
