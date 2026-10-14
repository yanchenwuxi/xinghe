package com.zaka.xinghe.subtitle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字幕解析：SRT / ASS / SSA / VTT 通吃，时间轴统一转成秒。
 * 全部自研，不依赖任何字幕库。
 */
public final class SubParser {

    private static final Pattern SRT_TIME = Pattern.compile(
            "(\\d{1,2}):(\\d{2}):(\\d{2})[,.](\\d{1,3})\\s*-->\\s*(\\d{1,2}):(\\d{2}):(\\d{2})[,.](\\d{1,3})");
    private static final Pattern VTT_TIME = Pattern.compile(
            "((?:\\d{1,2}:)?\\d{2}:\\d{2}\\.\\d{1,3})\\s*-->\\s*((?:\\d{1,2}:)?\\d{2}:\\d{2}\\.\\d{1,3})");
    private static final Pattern ASS_TAG = Pattern.compile("\\{[^}]*\\}");

    private SubParser() {
    }

    public static List<Cue> parse(String content, String ext) {
        if (content == null) return new ArrayList<Cue>();
        String e = ext == null ? "" : ext.toLowerCase();
        List<Cue> out;
        if (e.contains("ass") || e.contains("ssa") || content.contains("[Script Info]")) {
            out = parseAss(content);
        } else if (e.contains("vtt") || content.trim().startsWith("WEBVTT")) {
            out = parseVtt(content);
        } else if (e.contains("srt") || content.contains("-->")) {
            out = parseSrt(content);
        } else {
            out = parseSrt(content);
        }
        Collections.sort(out, new Comparator<Cue>() {
            @Override
            public int compare(Cue a, Cue b) {
                return Double.compare(a.start, b.start);
            }
        });
        return out;
    }

    // --------------------------------------------------------------- SRT

    private static List<Cue> parseSrt(String text) {
        List<Cue> out = new ArrayList<Cue>();
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        int i = 0;
        while (i < lines.length) {
            String line = lines[i].trim();
            Matcher m = SRT_TIME.matcher(line);
            if (m.find()) {
                double s = sec(m.group(1), m.group(2), m.group(3), m.group(4));
                double en = sec(m.group(5), m.group(6), m.group(7), m.group(8));
                StringBuilder sb = new StringBuilder();
                i++;
                while (i < lines.length && !lines[i].trim().isEmpty()) {
                    if (SRT_TIME.matcher(lines[i]).find()) break;
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(stripTags(lines[i].trim()));
                    i++;
                }
                String t = sb.toString().trim();
                if (t.length() > 0) out.add(new Cue(s, en, t));
            } else {
                i++;
            }
        }
        return out;
    }

    // --------------------------------------------------------------- VTT

    private static List<Cue> parseVtt(String text) {
        List<Cue> out = new ArrayList<Cue>();
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        int i = 0;
        while (i < lines.length) {
            String line = lines[i].trim();
            Matcher m = VTT_TIME.matcher(line);
            if (m.find()) {
                double s = vttSec(m.group(1));
                double en = vttSec(m.group(2));
                StringBuilder sb = new StringBuilder();
                i++;
                while (i < lines.length && !lines[i].trim().isEmpty()) {
                    if (VTT_TIME.matcher(lines[i]).find()) break;
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(stripTags(lines[i].trim()));
                    i++;
                }
                String t = sb.toString().trim();
                if (t.length() > 0) out.add(new Cue(s, en, t));
            } else {
                i++;
            }
        }
        return out;
    }

    // --------------------------------------------------------------- ASS

    private static List<Cue> parseAss(String text) {
        List<Cue> out = new ArrayList<Cue>();
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (!line.startsWith("Dialogue:")) continue;
            String body = line.substring("Dialogue:".length());
            String[] parts = body.split(",", 10);
            if (parts.length < 10) continue;
            try {
                double s = assSec(parts[1].trim());
                double en = assSec(parts[2].trim());
                String t = ASS_TAG.matcher(parts[9]).replaceAll("")
                        .replace("\\N", "\n").replace("\\n", "\n")
                        .replace("\\h", " ").trim();
                if (t.length() > 0) out.add(new Cue(s, en, t));
            } catch (Throwable ignore) {
            }
        }
        return out;
    }

    // --------------------------------------------------------------- 工具

    private static String stripTags(String s) {
        if (s == null) return "";
        return s.replaceAll("(?i)</?[a-z][^>]*>", "")
                .replace("{\\an8}", "")
                .replaceAll("\\{[^}]*\\}", "")
                .trim();
    }

    private static double sec(String h, String m, String s, String ms) {
        return toInt(h) * 3600 + toInt(m) * 60 + toInt(s) + toInt(pad(ms)) / 1000.0;
    }

    private static String pad(String ms) {
        if (ms == null) return "0";
        if (ms.length() == 1) return ms + "00";
        if (ms.length() == 2) return ms + "0";
        return ms;
    }

    private static double vttSec(String t) {
        String[] p = t.split(":");
        try {
            if (p.length == 3) {
                return Double.parseDouble(p[0]) * 3600 + Double.parseDouble(p[1]) * 60 + Double.parseDouble(p[2]);
            }
            if (p.length == 2) {
                return Double.parseDouble(p[0]) * 60 + Double.parseDouble(p[1]);
            }
            return Double.parseDouble(p[0]);
        } catch (Throwable e) {
            return 0;
        }
    }

    private static double assSec(String t) {
        String[] p = t.split(":");
        try {
            if (p.length == 3) {
                return Double.parseDouble(p[0]) * 3600 + Double.parseDouble(p[1]) * 60 + Double.parseDouble(p[2]);
            }
            return 0;
        } catch (Throwable e) {
            return 0;
        }
    }

    private static int toInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Throwable e) {
            return 0;
        }
    }
}
