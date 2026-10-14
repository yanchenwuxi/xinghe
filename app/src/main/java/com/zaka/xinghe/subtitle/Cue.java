package com.zaka.xinghe.subtitle;

import java.util.ArrayList;
import java.util.List;

/** 一条字幕（时间轴 + 文本）。 */
public class Cue {

    public double start;   // 秒
    public double end;     // 秒
    public String text;    // 可能多行

    public Cue(double s, double e, String t) {
        start = s;
        end = e;
        text = t;
    }

    public static List<Cue> copy(List<Cue> in) {
        return in == null ? new ArrayList<Cue>() : new ArrayList<Cue>(in);
    }
}
