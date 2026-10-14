package com.zaka.xinghe.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.zaka.xinghe.R;
import com.zaka.xinghe.model.Vod;
import com.zaka.xinghe.util.ImgLoader;
import com.zaka.xinghe.util.Utils;

import java.util.ArrayList;
import java.util.List;

/** 海报网格适配器。 */
public class VodAdapter extends BaseAdapter {

    public interface OnClick {
        void onClick(Vod v);
    }

    private final List<Vod> data = new ArrayList<Vod>();
    private final LayoutInflater inf;
    private final OnClick click;
    private final int thumbH;

    public VodAdapter(Context c, OnClick click) {
        this.inf = LayoutInflater.from(c);
        this.click = click;
        int colW = (Utils.screenW() - Utils.dp(24) - Utils.dp(16)) / 3;
        this.thumbH = (int) (colW * 280f / 220f);
    }

    public void setData(List<Vod> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    public void addData(List<Vod> list) {
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    public List<Vod> data() {
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
        Holder h;
        if (convertView == null) {
            convertView = inf.inflate(R.layout.item_vod, parent, false);
            h = new Holder();
            h.pic = (ImageView) convertView.findViewById(R.id.ivThumb);
            h.name = (TextView) convertView.findViewById(R.id.tvName);
            h.flag = (TextView) convertView.findViewById(R.id.tvFlag);
            h.note = (TextView) convertView.findViewById(R.id.tvNote);
            convertView.setTag(h);
        } else {
            h = (Holder) convertView.getTag();
        }
        final Vod v = data.get(position);
        h.name.setText(v.name);

        ViewGroup.LayoutParams lp = h.pic.getLayoutParams();
        if (lp instanceof FrameLayout.LayoutParams) {
            lp.height = thumbH;
            h.pic.setLayoutParams(lp);
        }

        if (Utils.empty(v.note)) {
            h.note.setVisibility(View.GONE);
        } else {
            h.note.setVisibility(View.VISIBLE);
            h.note.setText(v.note.length() > 10 ? v.note.substring(0, 10) : v.note);
        }
        if (Utils.empty(v.type)) {
            h.flag.setVisibility(View.GONE);
        } else {
            h.flag.setVisibility(View.VISIBLE);
            h.flag.setText(v.type.length() > 4 ? v.type.substring(0, 4) : v.type);
        }
        h.pic.setTag(null);
        h.pic.setImageBitmap(null);
        ImgLoader.load(h.pic, v.pic);

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (click != null) click.onClick(v);
            }
        });
        return convertView;
    }

    static class Holder {
        ImageView pic;
        TextView name;
        TextView flag;
        TextView note;
    }
}
