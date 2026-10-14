package com.zaka.xinghe.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.zaka.xinghe.App;
import com.zaka.xinghe.R;
import com.zaka.xinghe.adapter.SubtitleAdapter;
import com.zaka.xinghe.model.Record;
import com.zaka.xinghe.model.Vod;
import com.zaka.xinghe.net.CmsApi;
import com.zaka.xinghe.net.SpiderApi;
import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.store.SiteStore;
import com.zaka.xinghe.store.DataStore;
import com.zaka.xinghe.subtitle.Cue;
import com.zaka.xinghe.subtitle.SubParser;
import com.zaka.xinghe.subtitle.XunleiSub;
import com.zaka.xinghe.util.Utils;
import com.zaka.xinghe.view.SubtitleView;

import java.util.ArrayList;
import java.util.List;

/**
 * 播放页。
 *
 * 播放器是本机系统解码（MediaPlayer + SurfaceView），字幕走自绘层——
 * 这样字幕偏移是毫秒级、点了立刻生效，不依赖播放内核的外挂字幕通道。
 */
public class PlayActivity extends Activity implements SurfaceHolder.Callback {

    private static final String EX_VOD = "vod";
    private static final String EX_FLAG = "flag";
    private static final String EX_EP = "ep";
    private static final String EX_NAME = "name";
    private static final String EX_URL = "url";

    private MediaPlayer mp;
    private SurfaceView surface;
    private SurfaceHolder holder;
    private SubtitleView subtitleView;

    private FrameLayout controlLayer;
    private LinearLayout loadingBox;
    private LinearLayout errorBox;
    private LinearLayout gestureTip;
    private TextView tvTitle;
    private TextView tvCur;
    private TextView tvTotal;
    private TextView tvSpeed;
    private TextView loadingText;
    private TextView errorText;
    private TextView gestureText;
    private SeekBar seekBar;
    private ImageView btnPlayPause;
    private ImageView btnCenter;
    private ImageView btnUnlockFloat;
    private ImageView gestureIcon;

    private Vod vod;
    private int flagIndex = 0;
    private int epIndex = 0;
    private boolean liveMode = false;
    private String liveName = "";
    private String liveUrl = "";

    private boolean prepared = false;
    private boolean surfaceReady = false;
    private String currentUrl = "";
    private boolean locked = false;
    private boolean controlsVisible = true;
    private boolean videoSizeApplied = false;
    private float speed = 1.0f;
    private int videoW = 0;
    private int videoH = 0;
    private boolean stretched = false;

    private final Handler ui = new Handler();
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            ui.postDelayed(this, 500);
        }
    };

    // ------------------------------------------------------------ 入口

    public static void play(Context c, Vod vod, int flagIndex, int epIndex) {
        Intent i = new Intent(c, PlayActivity.class);
        i.putExtra(EX_VOD, vod);
        i.putExtra(EX_FLAG, flagIndex);
        i.putExtra(EX_EP, epIndex);
        c.startActivity(i);
    }

    public static void playLive(Context c, String name, String url) {
        Intent i = new Intent(c, PlayActivity.class);
        i.putExtra(EX_NAME, name);
        i.putExtra(EX_URL, url);
        c.startActivity(i);
    }

    // ------------------------------------------------------------ 生命周期

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_play);

        surface = (SurfaceView) findViewById(R.id.surface);
        subtitleView = (SubtitleView) findViewById(R.id.subtitleView);
        controlLayer = (FrameLayout) findViewById(R.id.controlLayer);
        loadingBox = (LinearLayout) findViewById(R.id.loadingBox);
        errorBox = (LinearLayout) findViewById(R.id.errorBox);
        gestureTip = (LinearLayout) findViewById(R.id.gestureTip);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvCur = (TextView) findViewById(R.id.tvCur);
        tvTotal = (TextView) findViewById(R.id.tvTotal);
        tvSpeed = (TextView) findViewById(R.id.btnSpeed);
        loadingText = (TextView) findViewById(R.id.loadingText);
        errorText = (TextView) findViewById(R.id.errorText);
        gestureText = (TextView) findViewById(R.id.gestureText);
        gestureIcon = (ImageView) findViewById(R.id.gestureIcon);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        btnPlayPause = (ImageView) findViewById(R.id.btnPlayPause);
        btnCenter = (ImageView) findViewById(R.id.btnCenter);
        btnUnlockFloat = (ImageView) findViewById(R.id.btnUnlockFloat);

        holder = surface.getHolder();
        holder.addCallback(this);
        surface.setOnTouchListener(new GestureListener());

        vod = (Vod) getIntent().getSerializableExtra(EX_VOD);
        flagIndex = getIntent().getIntExtra(EX_FLAG, 0);
        epIndex = getIntent().getIntExtra(EX_EP, 0);
        liveName = getIntent().getStringExtra(EX_NAME);
        liveUrl = getIntent().getStringExtra(EX_URL);
        liveMode = !Utils.empty(liveUrl);

        speed = DataStore.speed();
        subtitleView.setOffsetSec(DataStore.subOffset());
        updateSpeedLabel();

        bindClicks();
        startPlay();
        ui.postDelayed(ticker, 500);
    }

    private void bindClicks() {
        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        btnCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlay();
            }
        });
        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlay();
            }
        });
        findViewById(R.id.btnNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextEp();
            }
        });
        findViewById(R.id.btnEpisodes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEpisodeDialog();
            }
        });
        tvSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSpeedDialog();
            }
        });
        findViewById(R.id.btnSubtitle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSubtitlePanel();
            }
        });
        findViewById(R.id.btnScale).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stretched = !stretched;
                applyVideoSize();
                Utils.toast(stretched ? "铺满屏幕" : "原始比例");
            }
        });
        findViewById(R.id.btnLock).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLocked(!locked);
            }
        });
        btnUnlockFloat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLocked(false);
            }
        });
        findViewById(R.id.btnRetry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errorBox.setVisibility(View.GONE);
                startPlay();
            }
        });

        controlLayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!locked) toggleControls();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && mp != null && prepared) {
                    int dur = mp.getDuration();
                    if (dur > 0) {
                        int pos = (int) (dur * (progress / 1000f));
                        tvCur.setText(Utils.time(pos));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                if (mp != null && prepared) {
                    int dur = mp.getDuration();
                    if (dur > 0) {
                        mp.seekTo((int) (dur * (sb.getProgress() / 1000f)));
                    }
                }
            }
        });
    }

    // ------------------------------------------------------------ 播放

    private void startPlay() {
        if (liveMode) {
            tvTitle.setText(liveName);
            playResolvedUrl(liveUrl);
            return;
        }
        if (vod == null || vod.flags.size() == 0) {
            showError("没有可播放的地址");
            return;
        }
        Vod.Flag f = vod.flags.get(Math.min(flagIndex, vod.flags.size() - 1));
        if (f.eps.size() == 0) {
            showError("这条线路没有剧集");
            return;
        }
        final Vod.Ep ep = f.eps.get(Math.min(epIndex, f.eps.size() - 1));
        final String flagName = f.name;
        tvTitle.setText(vod.name + "  " + ep.name);

        loadingBox.setVisibility(View.VISIBLE);
        loadingText.setText(R.string.loading);
        errorBox.setVisibility(View.GONE);

        // 爬虫源：走 playerContent 解析真实播放地址
        final Site site = SiteStore.current();
        final boolean needSpider = site != null && site.isSpiderOrScript()
                && !isDirectMediaUrl(ep.url);

        if (!needSpider) {
            playResolvedUrl(CmsApi.resolvePlayUrl(ep.url));
            return;
        }

        loadingText.setText("解析播放地址…");
        App.run(new App.Task() {
            @Override
            public Object call() throws Exception {
                String raw = SpiderApi.playerContent(site, flagName, ep.url);
                if (Utils.empty(raw)) {
                    // 回退：用集地址本身
                    return CmsApi.resolvePlayUrl(ep.url);
                }
                return parsePlayerContent(raw, ep.url);
            }
        }, new App.Done() {
            @Override
            public void onDone(Object result, Exception err) {
                if (err != null) {
                    showError("解析失败：" + (err.getMessage() != null ? err.getMessage() : ""));
                    return;
                }
                playResolvedUrl(result == null ? "" : String.valueOf(result));
            }
        });
    }

    /** 是否已是直链媒体地址（无需爬虫二次解析）。 */
    private static boolean isDirectMediaUrl(String u) {
        if (Utils.empty(u)) return false;
        String s = u.trim().toLowerCase();
        if (!(s.startsWith("http://") || s.startsWith("https://"))) return false;
        // 常见直链后缀或 m3u8/mp4
        if (s.contains(".m3u8") || s.contains(".mp4") || s.contains(".flv")
                || s.contains(".mkv") || s.contains(".ts") || s.contains("playurl")) {
            return true;
        }
        // 没有明显脚本特征的也先当直链试
        return !s.contains("playercontent") && s.length() < 500;
    }

    /**
     * 解析爬虫 playerContent 返回值。
     * TVBox 约定大致为 JSON：{"parse":0,"url":"...","header":{...}} 或纯 url 字符串。
     */
    private static String parsePlayerContent(String raw, String fallback) {
        if (Utils.empty(raw)) return CmsApi.resolvePlayUrl(fallback);
        String t = raw.trim();
        if (t.startsWith("http://") || t.startsWith("https://")) return t;
        if (t.startsWith("{")) {
            try {
                org.json.JSONObject o = new org.json.JSONObject(t);
                String url = o.optString("url", o.optString("playUrl", ""));
                // 部分返回 url 数组
                if (Utils.empty(url) && o.optJSONArray("url") != null && o.optJSONArray("url").length() > 0) {
                    url = o.optJSONArray("url").optString(0);
                }
                if (!Utils.empty(url)) return CmsApi.resolvePlayUrl(url);
            } catch (Throwable ignore) {
            }
        }
        return CmsApi.resolvePlayUrl(fallback);
    }

    private void playResolvedUrl(String url) {
        currentUrl = url;
        if (Utils.empty(url)) {
            showError("播放地址为空");
            return;
        }
        if (!surfaceReady) return;

        loadingBox.setVisibility(View.VISIBLE);
        loadingText.setText(R.string.loading);
        errorBox.setVisibility(View.GONE);
        prepared = false;
        videoSizeApplied = false;

        releasePlayer();
        try {
            mp = new MediaPlayer();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setDataSource(url);
            mp.setDisplay(holder);
            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer player) {
                    prepared = true;
                    loadingBox.setVisibility(View.GONE);
                    applySpeed();
                    // 续播
                    long resume = 0;
                    if (!liveMode && vod != null) {
                        Record r = DataStore.find(vod.siteUrl + "@" + vod.id);
                        if (r != null && r.position > 5000) {
                            if (r.flagName.equals(currentFlagName()) || vod.flags.size() <= 1) {
                                resume = r.position;
                            }
                        }
                    }
                    if (resume > 0 && resume < player.getDuration() - 3000) {
                        player.seekTo((int) resume);
                        Utils.toast("已从上次位置继续播放");
                    }
                    player.start();
                    updatePlayIcon(true);
                    int dur = player.getDuration();
                    tvTotal.setText(liveMode ? "直播" : Utils.time(dur));
                    videoW = player.getVideoWidth();
                    videoH = player.getVideoHeight();
                    applyVideoSize();
                    maybeAutoSearchSubtitle();
                }
            });
            mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer player, int what, int extra) {
                    showError("播放出错 (代码 " + what + ")，点重试换一条线路试试");
                    return true;
                }
            });
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer player) {
                    saveHistory(true);
                    nextEp();
                }
            });
            mp.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(MediaPlayer player, int w, int h) {
                    videoW = w;
                    videoH = h;
                    applyVideoSize();
                }
            });
            mp.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer player, int percent) {
                    if (!liveMode) seekBar.setSecondaryProgress(percent * 10);
                }
            });
            mp.prepareAsync();
        } catch (Throwable t) {
            showError("播放失败：" + t.getClass().getSimpleName());
        }
    }

    private void releasePlayer() {
        if (mp != null) {
            try {
                mp.reset();
                mp.release();
            } catch (Throwable ignore) {
            }
            mp = null;
        }
    }

    private void togglePlay() {
        if (mp == null || !prepared) return;
        if (mp.isPlaying()) {
            mp.pause();
            updatePlayIcon(false);
        } else {
            mp.start();
            updatePlayIcon(true);
        }
    }

    private void updatePlayIcon(boolean playing) {
        int res = playing ? R.drawable.ic_pause : R.drawable.ic_play;
        btnPlayPause.setImageResource(res);
        btnCenter.setImageResource(res);
    }

    private void nextEp() {
        if (liveMode || vod == null) {
            finish();
            return;
        }
        Vod.Flag f = vod.flags.get(Math.min(flagIndex, vod.flags.size() - 1));
        if (epIndex + 1 < f.eps.size()) {
            epIndex++;
            startPlay();
        } else if (flagIndex + 1 < vod.flags.size()) {
            flagIndex++;
            epIndex = 0;
            startPlay();
        } else {
            Utils.toast("已经是最后一集了");
        }
    }

    private String currentFlagName() {
        if (vod == null || vod.flags.size() == 0) return "";
        return vod.flags.get(Math.min(flagIndex, vod.flags.size() - 1)).name;
    }

    private void updateProgress() {
        if (mp == null || !prepared) return;
        try {
            int pos = mp.getCurrentPosition();
            int dur = mp.getDuration();
            if (!liveMode && dur > 0) {
                seekBar.setProgress((int) (pos * 1000L / dur));
                tvTotal.setText(Utils.time(dur));
            }
            tvCur.setText(Utils.time(pos));
            subtitleView.update(pos);
        } catch (Throwable ignore) {
        }
    }

    private void applyVideoSize() {
        if (videoW <= 0 || videoH <= 0) return;
        int sw = Utils.screenW();
        int sh = Utils.screenH();
        int w;
        int h;
        if (stretched) {
            w = sw;
            h = sh;
        } else if (videoW * sh > videoH * sw) {
            w = sw;
            h = (int) (sw * (float) videoH / videoW);
        } else {
            h = sh;
            w = (int) (sh * (float) videoW / videoH);
        }
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.gravity = Gravity.CENTER;
        surface.setLayoutParams(lp);
        videoSizeApplied = true;
    }

    private void applySpeed() {
        if (mp == null || !prepared) return;
        if (Build.VERSION.SDK_INT < 23) return;
        try {
            android.media.PlaybackParams p = new android.media.PlaybackParams();
            p.setSpeed(speed);
            mp.setPlaybackParams(p);
        } catch (Throwable ignore) {
        }
    }

    private void updateSpeedLabel() {
        tvSpeed.setText(speed == 1.0f ? "1.0x" : (speed + "x"));
    }

    private void showSpeedDialog() {
        final float[] opts = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f};
        String[] names = new String[opts.length];
        for (int i = 0; i < opts.length; i++) names[i] = opts[i] + "x";
        new AlertDialog.Builder(this)
                .setTitle(R.string.player_speed)
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        speed = opts[which];
                        DataStore.setSpeed(speed);
                        updateSpeedLabel();
                        applySpeed();
                    }
                })
                .show();
    }

    private void showEpisodeDialog() {
        if (liveMode || vod == null || vod.flags.size() == 0) {
            Utils.toast("当前没有选集");
            return;
        }
        final List<String> names = new ArrayList<String>();
        final List<int[]> targets = new ArrayList<int[]>();
        for (int fi = 0; fi < vod.flags.size(); fi++) {
            Vod.Flag f = vod.flags.get(fi);
            for (int ei = 0; ei < f.eps.size(); ei++) {
                names.add((vod.flags.size() > 1 ? (f.name + " · ") : "") + f.eps.get(ei).name);
                targets.add(new int[]{fi, ei});
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.player_episode)
                .setItems(names.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        int[] t = targets.get(which);
                        flagIndex = t[0];
                        epIndex = t[1];
                        startPlay();
                    }
                })
                .show();
    }

    private void setLocked(boolean lock) {
        locked = lock;
        if (lock) {
            hideControls();
            findViewById(R.id.btnLock).setVisibility(View.GONE);
            btnUnlockFloat.setVisibility(View.VISIBLE);
            Utils.toast(getString(R.string.player_lock));
        } else {
            btnUnlockFloat.setVisibility(View.GONE);
            findViewById(R.id.btnLock).setVisibility(View.VISIBLE);
            showControls();
            Utils.toast(getString(R.string.player_unlock));
        }
    }

    private void toggleControls() {
        if (controlsVisible) hideControls();
        else showControls();
    }

    private void showControls() {
        controlsVisible = true;
        if (controlLayer != null) {
            for (int i = 0; i < controlLayer.getChildCount(); i++) {
                View v = controlLayer.getChildAt(i);
                if (v.getId() == R.id.btnUnlockFloat && !locked) continue;
                v.setVisibility(View.VISIBLE);
            }
        }
    }

    private void hideControls() {
        controlsVisible = false;
        if (controlLayer != null) {
            for (int i = 0; i < controlLayer.getChildCount(); i++) {
                View v = controlLayer.getChildAt(i);
                if (v.getId() == R.id.btnUnlockFloat) continue;
                v.setVisibility(View.GONE);
            }
        }
    }

    private void showError(String msg) {
        loadingBox.setVisibility(View.GONE);
        errorBox.setVisibility(View.VISIBLE);
        errorText.setText(msg);
    }

    // ------------------------------------------------------------ 手势

    private float downX;
    private float downY;
    private int downVolume;
    private float downBright;
    private boolean gestureBright;
    private boolean gestureVolume;
    private boolean gestureSeek;
    private int downPos;

    private class GestureListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getX();
                    downY = e.getY();
                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    downVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    downBright = lp.screenBrightness <= 0 ? 0.5f : lp.screenBrightness;
                    downPos = mp != null && prepared ? mp.getCurrentPosition() : 0;
                    gestureBright = gestureVolume = gestureSeek = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = e.getX() - downX;
                    float dy = e.getY() - downY;
                    if (!gestureBright && !gestureVolume && !gestureSeek) {
                        if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > Utils.dp(20)) gestureSeek = true;
                        else if (Math.abs(dy) > Utils.dp(20)) {
                            if (downX < Utils.screenW() / 2f) gestureBright = true;
                            else gestureVolume = true;
                        }
                    }
                    if (gestureSeek && mp != null && prepared && !liveMode) {
                        int dur = mp.getDuration();
                        if (dur > 0) {
                            int delta = (int) (dx / Utils.screenW() * dur);
                            int target = Math.max(0, Math.min(dur, downPos + delta));
                            showGesture(R.drawable.ic_next, Utils.time(target) + " / " + Utils.time(dur));
                            return true;
                        }
                    }
                    if (gestureBright) {
                        float p = downBright - dy / (Utils.screenH() * 0.7f);
                        p = Math.max(0.05f, Math.min(1f, p));
                        WindowManager.LayoutParams l = getWindow().getAttributes();
                        l.screenBrightness = p;
                        getWindow().setAttributes(l);
                        showGesture(R.drawable.ic_brightness, "亮度 " + (int) (p * 100) + "%");
                    } else if (gestureVolume) {
                        AudioManager am2 = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        int max = am2.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        float p = downVolume - dy / (Utils.screenH() * 0.7f) * max;
                        int vol = (int) Math.max(0, Math.min(max, p));
                        am2.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
                        showGesture(R.drawable.ic_volume, "音量 " + (int) (vol * 100f / max) + "%");
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (gestureSeek && mp != null && prepared && !liveMode) {
                        int dur = mp.getDuration();
                        if (dur > 0) {
                            int delta = (int) ((e.getX() - downX) / Utils.screenW() * dur);
                            mp.seekTo(Math.max(0, Math.min(dur, downPos + delta)));
                        }
                    } else if (!gestureBright && !gestureVolume && !gestureSeek) {
                        if (!locked) toggleControls();
                    }
                    gestureTip.setVisibility(View.GONE);
                    return true;
                default:
                    return false;
            }
        }
    }

    private void showGesture(int icon, String text) {
        gestureTip.setVisibility(View.VISIBLE);
        gestureIcon.setImageResource(icon);
        gestureIcon.setColorFilter(0xffffffff);
        gestureText.setText(text);
    }

    // ------------------------------------------------------------ 字幕

    private void maybeAutoSearchSubtitle() {
        // 播放开始后用片名自动搜一次字幕（有就静默加载，没有就算了）
        if (liveMode || vod == null || subtitleView.hasSubtitle()) return;
        final String kw = Utils.cleanName(vod.name);
        if (Utils.empty(kw)) return;
        App.run(new App.Task() {
            @Override
            public Object call() throws Exception {
                return XunleiSub.search(kw);
            }
        }, new App.Done() {
            @Override
            public void onDone(Object result, Exception err) {
                if (err != null || result == null) return;
                List<XunleiSub.Item> items = (List<XunleiSub.Item>) result;
                if (items.size() == 0 || isFinishing()) return;
                loadSubtitle(items.get(0));
            }
        });
    }

    private void loadSubtitle(final XunleiSub.Item item) {
        loadingBox.setVisibility(View.VISIBLE);
        loadingText.setText("正在加载字幕…");
        App.run(new App.Task() {
            @Override
            public Object call() throws Exception {
                String text = XunleiSub.fetchText(item);
                return SubParser.parse(text, item.ext);
            }
        }, new App.Done() {
            @Override
            public void onDone(Object result, Exception err) {
                loadingBox.setVisibility(View.GONE);
                if (err != null || result == null) {
                    Utils.toast("字幕加载失败");
                    return;
                }
                List<Cue> cues = (List<Cue>) result;
                if (cues.size() == 0) {
                    Utils.toast("这个字幕解析出来是空的，换一条试试");
                    return;
                }
                subtitleView.setOffsetSec(DataStore.subOffset());
                subtitleView.setCues(cues);
                Utils.toast("字幕已加载（" + cues.size() + " 条），偏移可调");
            }
        });
    }

    private void showSubtitlePanel() {
        final Dialog dialog = new Dialog(this, R.style.AppTheme_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View root = getLayoutInflater().inflate(R.layout.dialog_subtitle, null);
        dialog.setContentView(root);

        Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dp(460));
            w.setGravity(Gravity.BOTTOM);
            w.setBackgroundDrawableResource(android.R.color.transparent);
        }

        final TextView tvOffset = (TextView) root.findViewById(R.id.tvOffset);
        final EditText etKw = (EditText) root.findViewById(R.id.etSubKeyword);
        final ListView list = (ListView) root.findViewById(R.id.subList);
        final TextView subState = (TextView) root.findViewById(R.id.subState);
        final SubtitleAdapter adapter = new SubtitleAdapter(this);
        list.setAdapter(adapter);

        if (!liveMode && vod != null) etKw.setText(Utils.cleanName(vod.name));
        tvOffset.setText(formatOffset(subtitleView.getOffsetSec()));
        if (subtitleView.hasSubtitle()) {
            subState.setVisibility(View.GONE);
        } else {
            subState.setText(R.string.player_sub_none);
            subState.setVisibility(View.VISIBLE);
        }

        View.OnClickListener offsetClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float delta = 0f;
                int id = v.getId();
                if (id == R.id.btnM5) delta = -5f;
                else if (id == R.id.btnM1) delta = -1f;
                else if (id == R.id.btnM01) delta = -0.1f;
                else if (id == R.id.btnP01) delta = 0.1f;
                else if (id == R.id.btnP1) delta = 1f;
                else if (id == R.id.btnP5) delta = 5f;
                float now = subtitleView.getOffsetSec() + delta;
                if (id == R.id.btnOffsetZero) now = 0f;
                now = Math.max(-60f, Math.min(60f, now));
                subtitleView.setOffsetSec(now);
                DataStore.setSubOffset(now);
                tvOffset.setText(formatOffset(now));
                subtitleView.update(mp != null && prepared ? mp.getCurrentPosition() : 0);
            }
        };
        root.findViewById(R.id.btnM5).setOnClickListener(offsetClick);
        root.findViewById(R.id.btnM1).setOnClickListener(offsetClick);
        root.findViewById(R.id.btnM01).setOnClickListener(offsetClick);
        root.findViewById(R.id.btnP01).setOnClickListener(offsetClick);
        root.findViewById(R.id.btnP1).setOnClickListener(offsetClick);
        root.findViewById(R.id.btnP5).setOnClickListener(offsetClick);
        root.findViewById(R.id.btnOffsetZero).setOnClickListener(offsetClick);

        root.findViewById(R.id.btnClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        root.findViewById(R.id.btnSubSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String kw = etKw.getText().toString().trim();
                if (Utils.empty(kw)) {
                    Utils.toast("先输入片名");
                    return;
                }
                subState.setText(R.string.player_sub_searching);
                subState.setVisibility(View.VISIBLE);
                App.run(new App.Task() {
                    @Override
                    public Object call() throws Exception {
                        return XunleiSub.search(kw);
                    }
                }, new App.Done() {
                    @Override
                    public void onDone(Object result, Exception err) {
                        if (err != null || result == null) {
                            subState.setText("搜索失败，检查下网络");
                            return;
                        }
                        List<XunleiSub.Item> items = (List<XunleiSub.Item>) result;
                        adapter.setData(items);
                        if (items.size() == 0) {
                            subState.setText(R.string.player_sub_empty);
                            subState.setVisibility(View.VISIBLE);
                        } else {
                            subState.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                XunleiSub.Item item = (XunleiSub.Item) adapter.getItem(position);
                dialog.dismiss();
                loadSubtitle(item);
            }
        });

        dialog.show();
    }

    private String formatOffset(float sec) {
        String sign = sec > 0 ? "+" : (sec < 0 ? "-" : "+");
        return sign + String.format(java.util.Locale.US, "%.1f", Math.abs(sec)) + "s";
    }

    // ------------------------------------------------------------ 记录 / 生命周期

    private void saveHistory(boolean completed) {
        if (liveMode || vod == null) return;
        try {
            Record r = new Record();
            r.vodId = vod.id;
            r.siteUrl = vod.siteUrl;
            r.siteName = vod.siteName;
            r.name = vod.name;
            r.pic = vod.pic;
            r.note = vod.note;
            r.flagName = currentFlagName();
            r.epIndex = epIndex;
            Vod.Flag f = vod.flags.get(Math.min(flagIndex, vod.flags.size() - 1));
            if (f.eps.size() > epIndex) {
                r.epName = f.eps.get(epIndex).name;
                if (f.eps.size() > 1) r.name = vod.name;
            }
            if (mp != null && prepared) {
                r.position = completed ? 0 : mp.getCurrentPosition();
                r.duration = mp.getDuration();
            }
            r.updateTime = System.currentTimeMillis();
            Record old = DataStore.find(r.key());
            r.fav = old != null && old.fav;
            DataStore.putHistory(r);
        } catch (Throwable ignore) {
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder h) {
        surfaceReady = true;
        if (Utils.empty(currentUrl)) startPlay();
        else startPlay();
    }

    @Override
    public void surfaceChanged(SurfaceHolder h, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder h) {
        surfaceReady = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveHistory(false);
        if (mp != null && prepared && mp.isPlaying()) {
            mp.pause();
            updatePlayIcon(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ui.removeCallbacks(ticker);
        releasePlayer();
    }

    @Override
    public void onBackPressed() {
        if (locked) {
            setLocked(false);
            return;
        }
        super.onBackPressed();
    }
}
