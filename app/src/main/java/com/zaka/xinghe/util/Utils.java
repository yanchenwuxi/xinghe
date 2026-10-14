package com.zaka.xinghe.util;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 通用小工具。全部自研实现。 */
public final class Utils {

    private static Context appCtx;

    private Utils() {
    }

    public static void init(Context ctx) {
        appCtx = ctx.getApplicationContext();
    }

    public static Context ctx() {
        return appCtx;
    }

    public static int dp(float v) {
        Resources r = appCtx.getResources();
        return (int) (v * r.getDisplayMetrics().density + 0.5f);
    }

    public static int screenW() {
        return appCtx.getResources().getDisplayMetrics().widthPixels;
    }

    public static int screenH() {
        return appCtx.getResources().getDisplayMetrics().heightPixels;
    }

    public static void toast(String msg) {
        if (TextUtils.isEmpty(msg)) return;
        try {
            Toast.makeText(appCtx, msg, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignore) {
        }
    }

    public static boolean empty(String s) {
        return s == null || s.trim().length() == 0;
    }

    /** 毫秒 -> mm:ss 或 hh:mm:ss */
    public static String time(long ms) {
        if (ms < 0) ms = 0;
        long total = ms / 1000L;
        long h = total / 3600L;
        long m = (total % 3600L) / 60L;
        long s = total % 60L;
        StringBuilder sb = new StringBuilder();
        if (h > 0) {
            sb.append(h).append(':');
            if (m < 10) sb.append('0');
        }
        sb.append(m).append(':');
        if (s < 10) sb.append('0');
        sb.append(s);
        return sb.toString();
    }

    /** 秒（浮点）-> mm:ss，用于字幕时间轴显示 */
    public static String subTime(double sec) {
        if (sec < 0) sec = 0;
        int total = (int) sec;
        int h = total / 3600;
        int m = (total % 3600) / 60;
        int s = total % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) {
            sb.append(h).append(':');
            if (m < 10) sb.append('0');
        }
        sb.append(m).append(':');
        if (s < 10) sb.append('0');
        sb.append(s);
        return sb.toString();
    }

    public static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                String h = Integer.toHexString(b & 0xff);
                if (h.length() == 1) sb.append('0');
                sb.append(h);
            }
            return sb.toString();
        } catch (Throwable t) {
            return String.valueOf(s.hashCode());
        }
    }

    public static String urlEncode(String s) {
        if (s == null) return "";
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

    /** 拼接 base + 参数，自动处理 ? 和 & */
    public static String join(String base, String params) {
        if (Utils.empty(base)) return "";
        if (Utils.empty(params)) return base;
        if (base.contains("?")) {
            if (base.endsWith("?") || base.endsWith("&")) return base + params;
            return base + "&" + params;
        }
        return base + "?" + params;
    }

    public static String nowDate() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date());
    }

    /** 从播放地址里抠出扩展名，判断要不要特殊处理 */
    public static boolean isM3u8(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.ROOT);
        return u.contains(".m3u8");
    }

    /** 清理影片名：去掉扩展名、清晰度、集数标识，用于字幕检索 */
    public static String cleanName(String name) {
        if (name == null) return "";
        String s = name.trim();
        s = s.replaceAll("(?i)\\.(mp4|mkv|avi|rmvb|ts|flv|wmv|mov|m3u8)$", "");
        s = s.replaceAll("(?i)\\b(1080p|720p|480p|2160p|4k|8k|bluray|blu-ray|web-dl|webrip|hdtv|hdrip|dvdrip|x264|x265|h264|h265|hevc|avc|aac|ac3|remux|hd|tc|ts|bd)\\b", " ");
        s = s.replaceAll("(?i)\\bS\\d{1,2}E\\d{1,3}\\b", " ");
        s = s.replaceAll("第\\s*\\d{1,4}\\s*[集话話期]", " ");
        s = s.replaceAll("[\\[\\]【】()（）]", " ");
        s = s.replace('.', ' ').replace('_', ' ').replace('|', ' ');
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }
}
