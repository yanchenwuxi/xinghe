package com.zaka.xinghe.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.zaka.xinghe.subtitle.Cue;
import com.zaka.xinghe.util.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 自绘字幕层。
 *
 * 为什么自绘：本机播放内核的外挂字幕通道不可靠，字幕改由应用按播放进度自己绘制，
 * 好处是偏移调整变成毫秒级、点了立刻生效，不用重新加载、不用重新起播。
 */
public class SubtitleView extends View {

    private final List<Cue> cues = new ArrayList<Cue>();
    private final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private long positionMs = 0L;
    private float offsetSec = 0f;      // 字幕偏移（秒），正数=字幕延后
    private boolean enabled = true;

    public SubtitleView(Context c) {
        super(c);
        init();
    }

    public SubtitleView(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    private void init() {
        paint.setColor(Color.WHITE);
        paint.setTextSize(Utils.dp(17));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setShadowLayer(Utils.dp(1.6f), 0, Utils.dp(1f), 0xdd000000);
        bgPaint.setColor(0x99000000);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    // ------------------------------------------------------------ 对外接口

    public void setCues(List<Cue> list) {
        cues.clear();
        if (list != null) cues.addAll(list);
        postInvalidate();
    }

    public boolean hasSubtitle() {
        return cues.size() > 0;
    }

    public void setOffsetSec(float sec) {
        offsetSec = sec;
        postInvalidate();
    }

    public float getOffsetSec() {
        return offsetSec;
    }

    public void setEnabledSub(boolean b) {
        enabled = b;
        postInvalidate();
    }

    public boolean isEnabledSub() {
        return enabled;
    }

    /** 由播放器按当前进度驱动。 */
    public void update(long posMs) {
        this.positionMs = posMs;
        invalidate();
    }

    // ------------------------------------------------------------ 绘制

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!enabled || cues.isEmpty()) return;

        // 播放进度换算成字幕时间轴时间：偏移为负 = 字幕提前，正 = 延后
        double t = positionMs / 1000.0 - offsetSec;
        Cue cur = null;
        for (Cue c : cues) {
            if (t >= c.start && t <= c.end) {
                cur = c;
                break;
            }
        }
        if (cur == null || Utils.empty(cur.text)) return;

        String[] lines = cur.text.split("\n");
        int w = getWidth();
        float maxW = w - Utils.dp(28);
        float lineH = paint.getTextSize() * 1.28f;
        float totalH = lineH * lines.length;

        // 底部安全区：留出控制条空间
        float bottom = getHeight() - Utils.dp(72);
        float top = bottom - totalH;

        // 背景块
        float bgPad = Utils.dp(6);
        canvas.drawRoundRect(Utils.dp(14), top - bgPad, w - Utils.dp(14), bottom + bgPad / 2,
                Utils.dp(4), Utils.dp(4), bgPaint);

        float y = top + paint.getTextSize();
        for (String line : lines) {
            String show = line;
            // 太长的行做一次收缩
            float tw = paint.measureText(show);
            if (tw > maxW && show.length() > 1) {
                float scale = maxW / tw;
                canvas.save();
                canvas.scale(scale, 1f, w / 2f, y);
                canvas.drawText(show, w / 2f, y, paint);
                canvas.restore();
            } else {
                canvas.drawText(show, w / 2f, y, paint);
            }
            y += lineH;
        }
    }
}
