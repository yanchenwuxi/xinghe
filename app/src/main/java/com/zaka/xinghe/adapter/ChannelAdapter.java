package com.zaka.xinghe.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.zaka.xinghe.R;

import java.util.ArrayList;
import java.util.List;

/** 直播频道列表适配器。 */
public class ChannelAdapter extends BaseAdapter {

    public static class Channel {
        public String name = "";
        public String url = "";
        public String group = "";
    }

    private final List<Channel> data = new ArrayList<Channel>();
    private final LayoutInflater inf;

    public ChannelAdapter(Context c) {
        this.inf = LayoutInflater.from(c);
    }

    public void setData(List<Channel> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    public List<Channel> data() {
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
            convertView = inf.inflate(R.layout.item_channel, parent, false);
        }
        Channel ch = data.get(position);
        ((TextView) convertView.findViewById(R.id.tvIndex)).setText(String.valueOf(position + 1));
        ((TextView) convertView.findViewById(R.id.tvName)).setText(ch.name);
        ((TextView) convertView.findViewById(R.id.tvGroup)).setText(ch.group);
        return convertView;
    }
}
