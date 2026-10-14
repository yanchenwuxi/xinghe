package com.zaka.xinghe.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zaka.xinghe.App;
import com.zaka.xinghe.R;
import com.zaka.xinghe.adapter.VodAdapter;
import com.zaka.xinghe.model.Record;
import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.model.Vod;
import com.zaka.xinghe.net.CmsApi;
import com.zaka.xinghe.store.DataStore;
import com.zaka.xinghe.store.SiteStore;
import com.zaka.xinghe.util.ImgLoader;
import com.zaka.xinghe.util.Utils;

import java.util.ArrayList;
import java.util.List;

/** 详情页：头图 + 信息 + 简介 + 线路 + 选集。 */
public class DetailActivity extends Activity {

    private static final String EX_VOD = "vod";

    private Vod vod;
    private Vod full;
    private int flagIndex = 0;
    private boolean introExpanded = false;

    private ImageView ivBackdrop;
    private TextView tvName;
    private TextView tvInfo;
    private TextView tvActors;
    private TextView tvIntro;
    private TextView btnIntroToggle;
    private TextView tvSeriesTitle;
    private ImageView ivCollect;
    private TextView tvCollect;
    private LinearLayout flagBar;
    private HorizontalScrollView flagScroll;
    private com.zaka.xinghe.view.FlowLayout flowEpisodes;

    public static void open(Context c, Vod v) {
        Intent i = new Intent(c, DetailActivity.class);
        i.putExtra(EX_VOD, v);
        c.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_detail);
        vod = (Vod) getIntent().getSerializableExtra(EX_VOD);
        if (vod == null) {
            finish();
            return;
        }

        ivBackdrop = (ImageView) findViewById(R.id.ivBackdrop);
        tvName = (TextView) findViewById(R.id.tvName);
        tvInfo = (TextView) findViewById(R.id.tvInfo);
        tvActors = (TextView) findViewById(R.id.tvActors);
        tvIntro = (TextView) findViewById(R.id.tvIntro);
        btnIntroToggle = (TextView) findViewById(R.id.btnIntroToggle);
        tvSeriesTitle = (TextView) findViewById(R.id.tvSeriesTitle);
        ivCollect = (ImageView) findViewById(R.id.ivCollect);
        tvCollect = (TextView) findViewById(R.id.tvCollect);
        flagBar = (LinearLayout) findViewById(R.id.flagBar);
        flagScroll = (HorizontalScrollView) findViewById(R.id.flagScroll);
        flowEpisodes = (com.zaka.xinghe.view.FlowLayout) findViewById(R.id.flowEpisodes);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.btnPlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPlay(0);
            }
        });
        findViewById(R.id.btnCollect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFav();
            }
        });
        btnIntroToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                introExpanded = !introExpanded;
                tvIntro.setMaxLines(introExpanded ? 100 : 3);
                btnIntroToggle.setText(introExpanded ? R.string.detail_collapse : R.string.detail_expand);
            }
        });

        bindBasic(vod);
        updateFavState();
        loadDetail();
    }

    private void bindBasic(Vod v) {
        tvName.setText(v.name);
        StringBuilder sb = new StringBuilder();
        if (!Utils.empty(v.score)) sb.append("评分 ").append(v.score).append("   ");
        if (!Utils.empty(v.year)) sb.append(v.year).append("   ");
        if (!Utils.empty(v.area)) sb.append(v.area).append("   ");
        if (!Utils.empty(v.type)) sb.append(v.type);
        tvInfo.setText(sb.toString().trim());
        if (Utils.empty(v.actor)) tvActors.setVisibility(View.GONE);
        else tvActors.setText("主演：" + v.actor);

        if (Utils.empty(v.content)) {
            tvIntro.setVisibility(View.GONE);
            btnIntroToggle.setVisibility(View.GONE);
        } else {
            tvIntro.setVisibility(View.VISIBLE);
            tvIntro.setText(v.content.replaceAll("<[^>]+>", "").trim());
        }
        final String pic = v.pic;
        if (!Utils.empty(pic)) {
            App.run(new App.Task() {
                @Override
                public Object call() throws Exception {
                    return ImgLoader.fetch(pic, Utils.dp(320));
                }
            }, new App.Done() {
                @Override
                public void onDone(Object result, Exception err) {
                    if (result instanceof Bitmap && !isFinishing()) {
                        ivBackdrop.setImageBitmap((Bitmap) result);
                    }
                }
            });
        }
    }

    private void loadDetail() {
        final Site site = SiteStore.current();
        if (site == null) return;
        if (Utils.empty(vod.id)) return;
        App.run(new App.Task() {
            @Override
            public Object call() throws Exception {
                return CmsApi.getDetail(site, vod.id);
            }
        }, new App.Done() {
            @Override
            public void onDone(Object result, Exception err) {
                if (err != null || result == null) return;
                if (isFinishing()) return;
                full = (Vod) result;
                if (Utils.empty(full.siteUrl)) full.siteUrl = vod.siteUrl;
                bindBasic(full);
                buildFlags();
            }
        });
    }

    private void buildFlags() {
        if (full == null || full.flags.size() == 0) return;
        if (full.flags.size() > 1) {
            flagScroll.setVisibility(View.VISIBLE);
            flagBar.removeAllViews();
            for (int i = 0; i < full.flags.size(); i++) {
                final int index = i;
                TextView tv = new TextView(this);
                tv.setText(full.flags.get(i).name);
                tv.setTextSize(13f);
                tv.setPadding(Utils.dp(14), Utils.dp(6), Utils.dp(14), Utils.dp(6));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.rightMargin = Utils.dp(8);
                tv.setLayoutParams(lp);
                tv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        flagIndex = index;
                        markFlags();
                        buildEpisodes();
                    }
                });
                flagBar.addView(tv);
            }
            markFlags();
        }
        buildEpisodes();
    }

    private void markFlags() {
        for (int i = 0; i < flagBar.getChildCount(); i++) {
            TextView tv = (TextView) flagBar.getChildAt(i);
            boolean on = i == flagIndex;
            tv.setTextColor(on ? 0xffffffff : 0xb3ffffff);
            tv.setBackgroundResource(on ? R.drawable.bg_chip_on : R.drawable.bg_chip);
        }
    }

    private void buildEpisodes() {
        flowEpisodes.removeAllViews();
        if (full == null || full.flags.size() == 0) return;
        Vod.Flag f = full.flags.get(Math.min(flagIndex, full.flags.size() - 1));
        tvSeriesTitle.setVisibility(View.VISIBLE);
        tvSeriesTitle.setText(f.eps.size() > 1 ? ("选集 (" + f.eps.size() + ")")
                : getString(R.string.detail_series));
        for (int i = 0; i < f.eps.size(); i++) {
            final int index = i;
            Vod.Ep ep = f.eps.get(i);
            TextView tv = new TextView(this);
            tv.setText(ep.name);
            tv.setTextSize(13f);
            tv.setSingleLine(true);
            tv.setEllipsize(TextUtils.TruncateAt.END);
            tv.setPadding(Utils.dp(14), Utils.dp(8), Utils.dp(14), Utils.dp(8));
            tv.setTextColor(0xffffffff);
            tv.setBackgroundResource(R.drawable.bg_chip);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tv.setLayoutParams(lp);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startPlay(index);
                }
            });
            flowEpisodes.addView(tv);
        }
    }

    private void startPlay(int epIndex) {
        if (full == null || full.flags.size() == 0) {
            Utils.toast("正在获取播放地址，稍等一下");
            return;
        }
        PlayActivity.play(this, full, flagIndex, epIndex);
    }

    // -------------------------------------------------------- 收藏

    private void updateFavState() {
        boolean fav = DataStore.isFav(vod.siteUrl + "@" + vod.id);
        ivCollect.setImageResource(fav ? R.drawable.ic_star_on : R.drawable.ic_star_off);
        ivCollect.setColorFilter(fav ? 0xffffb300 : 0xb3ffffff);
        tvCollect.setText(fav ? R.string.collected : R.string.collect);
    }

    private void toggleFav() {
        Vod v = full == null ? vod : full;
        boolean fav = DataStore.isFav(v.siteUrl + "@" + v.id);
        Record r = new Record();
        r.vodId = v.id;
        r.siteUrl = v.siteUrl;
        r.siteName = v.siteName;
        r.name = v.name;
        r.pic = v.pic;
        r.note = v.note;
        r.fav = !fav;
        DataStore.setFav(r);
        updateFavState();
        Utils.toast(getString(fav ? R.string.collect_cancel : R.string.collect_done));
    }
}
