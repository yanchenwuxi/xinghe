package com.zaka.xinghe.store;

import android.content.Context;
import android.content.SharedPreferences;

import com.zaka.xinghe.model.Record;
import com.zaka.xinghe.util.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 观看历史 / 收藏 / 偏好设置。 */
public final class DataStore {

    private static final String SP = "xinghe_data";
    private static final String K_REC = "records";
    private static final String K_HOT = "hot_words";

    private static SharedPreferences sp;

    private DataStore() {
    }

    public static void init(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(SP, Context.MODE_PRIVATE);
    }

    // ---------------------------------------------------------------- 记录

    public static List<Record> all() {
        List<Record> out = new ArrayList<Record>();
        String s = sp.getString(K_REC, "");
        if (Utils.empty(s)) return out;
        try {
            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Record r = new Record();
                r.vodId = o.optString("vodId", "");
                r.siteUrl = o.optString("siteUrl", "");
                r.siteName = o.optString("siteName", "");
                r.name = o.optString("name", "");
                r.pic = o.optString("pic", "");
                r.note = o.optString("note", "");
                r.flagName = o.optString("flagName", "");
                r.epIndex = o.optInt("epIndex", 0);
                r.epName = o.optString("epName", "");
                r.position = o.optLong("position", 0L);
                r.duration = o.optLong("duration", 0L);
                r.updateTime = o.optLong("updateTime", 0L);
                r.fav = o.optBoolean("fav", false);
                if (!Utils.empty(r.vodId)) out.add(r);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    private static void saveAll(List<Record> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Record r : list) {
                JSONObject o = new JSONObject();
                o.put("vodId", r.vodId);
                o.put("siteUrl", r.siteUrl);
                o.put("siteName", r.siteName);
                o.put("name", r.name);
                o.put("pic", r.pic);
                o.put("note", r.note);
                o.put("flagName", r.flagName);
                o.put("epIndex", r.epIndex);
                o.put("epName", r.epName);
                o.put("position", r.position);
                o.put("duration", r.duration);
                o.put("updateTime", r.updateTime);
                o.put("fav", r.fav);
                arr.put(o);
            }
        } catch (Throwable ignore) {
        }
        sp.edit().putString(K_REC, arr.toString()).apply();
    }

    public static Record find(String key) {
        for (Record r : all()) {
            if (r.key().equals(key)) return r;
        }
        return null;
    }

    public static boolean isFav(String key) {
        Record r = find(key);
        return r != null && r.fav;
    }

    public static void setFav(Record rec) {
        List<Record> list = all();
        boolean hit = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).key().equals(rec.key())) {
                list.get(i).fav = rec.fav;
                if (rec.fav) list.get(i).updateTime = System.currentTimeMillis();
                hit = true;
                break;
            }
        }
        if (!hit && rec.fav) {
            rec.updateTime = System.currentTimeMillis();
            list.add(rec);
        }
        saveAll(list);
    }

    /** 覆盖写入全部记录（删除 / 清理用）。 */
    public static void putAll(List<Record> list) {
        saveAll(list == null ? new ArrayList<Record>() : list);
    }

    public static void putHistory(Record rec) {
        List<Record> list = all();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).key().equals(rec.key())) list.remove(i);
        }
        rec.updateTime = System.currentTimeMillis();
        list.add(rec);
        // 最多留 300 条
        while (list.size() > 300) {
            list.remove(0);
        }
        saveAll(list);
    }

    public static List<Record> history() {
        List<Record> out = new ArrayList<Record>();
        for (Record r : all()) {
            if (r.updateTime > 0) out.add(r);
        }
        Collections.sort(out, new Comparator<Record>() {
            @Override
            public int compare(Record a, Record b) {
                return Long.compare(b.updateTime, a.updateTime);
            }
        });
        return out;
    }

    public static List<Record> favs() {
        List<Record> out = new ArrayList<Record>();
        for (Record r : all()) {
            if (r.fav) out.add(r);
        }
        Collections.sort(out, new Comparator<Record>() {
            @Override
            public int compare(Record a, Record b) {
                return Long.compare(b.updateTime, a.updateTime);
            }
        });
        return out;
    }

    public static void clearHistory() {
        List<Record> list = all();
        List<Record> keep = new ArrayList<Record>();
        for (Record r : list) {
            if (r.fav) {
                r.position = 0;
                r.updateTime = 0;
                keep.add(r);
            }
        }
        saveAll(keep);
    }

    // ---------------------------------------------------------------- 搜索历史

    public static List<String> searchHistory() {
        List<String> out = new ArrayList<String>();
        String s = sp.getString(K_HOT, "");
        if (Utils.empty(s)) return out;
        for (String p : s.split("\n")) {
            if (!Utils.empty(p)) out.add(p);
        }
        return out;
    }

    public static void addSearchWord(String w) {
        if (Utils.empty(w)) return;
        List<String> list = searchHistory();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).equalsIgnoreCase(w)) list.remove(i);
        }
        list.add(0, w);
        while (list.size() > 20) list.remove(list.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(s);
        }
        sp.edit().putString(K_HOT, sb.toString()).apply();
    }

    public static void clearSearchWord() {
        sp.edit().putString(K_HOT, "").apply();
    }

    // ---------------------------------------------------------------- 偏好

    public static float subOffset() {
        return sp.getFloat("sub_offset", 0f);
    }

    public static void setSubOffset(float v) {
        sp.edit().putFloat("sub_offset", v).apply();
    }

    public static float speed() {
        return sp.getFloat("speed", 1.0f);
    }

    public static void setSpeed(float v) {
        sp.edit().putFloat("speed", v).apply();
    }

    public static void put(String k, String v) {
        sp.edit().putString(k, v).apply();
    }

    public static String get(String k, String def) {
        return sp.getString(k, def);
    }
}
