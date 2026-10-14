package com.zaka.xinghe.subtitle;

import org.json.JSONArray;
import org.json.JSONObject;

import com.zaka.xinghe.net.Http;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 在线字幕源（迅雷公开接口，免登录）。
 * 搜索 -> 下载 -> 转 UTF-8 落盘，供自绘字幕层使用。
 */
public final class XunleiSub {

    private static final String API = "https://api-shoulei-ssl.xunlei.com/oracle/subtitle";
    private static final int CONNECT_TIMEOUT = 12000;

    private XunleiSub() {
    }

    public static class Item {
        public String name = "";
        public String simple = "";
        public String url = "";
        public String ext = "srt";
        public String lang = "";
        public long duration = 0L;
        public int score = 0;

        public String meta() {
            StringBuilder sb = new StringBuilder();
            if (!com.zaka.xinghe.util.Utils.empty(ext)) sb.append(ext.toUpperCase());
            if (duration > 0) {
                if (sb.length() > 0) sb.append("  ·  ");
                sb.append(com.zaka.xinghe.util.Utils.time(duration));
            }
            if (score > 0) {
                if (sb.length() > 0) sb.append("  ·  ");
                sb.append("评分 ").append(score);
            }
            if (!com.zaka.xinghe.util.Utils.empty(lang)) {
                if (sb.length() > 0) sb.append("  ·  ");
                sb.append(lang);
            }
            return sb.toString();
        }
    }

    public static List<Item> search(String keyword) throws Exception {
        List<Item> out = new ArrayList<Item>();
        String kw = com.zaka.xinghe.util.Utils.cleanName(keyword);
        if (com.zaka.xinghe.util.Utils.empty(kw)) return out;
        String url = API + "?name=" + com.zaka.xinghe.util.Utils.urlEncode(kw) + "&size=";
        String body = Http.get(url, null, CONNECT_TIMEOUT);
        return parseSearch(body);
    }

    /** 解析搜索返回（与网络分离，方便单独验证）。 */
    public static List<Item> parseSearch(String body) throws Exception {
        return parseSearch(body, "");
    }

    public static List<Item> parseSearch(String body, String keyword) throws Exception {
        List<Item> out = new ArrayList<Item>();
        JSONObject root = new JSONObject(body.trim());
        JSONArray arr = root.optJSONArray("data");
        if (arr == null) return out;
        HashSet<String> seen = new HashSet<String>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String u = o.optString("url", "");
            if (com.zaka.xinghe.util.Utils.empty(u)) continue;
            if (!seen.add(u)) continue;
            Item it = new Item();
            it.url = u;
            it.name = o.optString("name", "");
            it.simple = o.optString("simple_name", "");
            if (com.zaka.xinghe.util.Utils.empty(it.name)) it.name = it.simple;
            if (com.zaka.xinghe.util.Utils.empty(it.name)) it.name = "subtitle";
            it.ext = o.optString("ext", "srt");
            it.duration = o.optLong("duration", 0L);
            it.score = o.optInt("score", 0);
            JSONArray la = o.optJSONArray("languages");
            if (la != null && la.length() > 0) it.lang = la.optString(0, "");
            out.add(it);
        }
        // 简单名命中的 / 非弹幕 / 非机翻的排前面
        final String fk = keyword;
        java.util.Collections.sort(out, new java.util.Comparator<Item>() {
            @Override
            public int compare(Item a, Item b) {
                int ra = rank(a, fk);
                int rb = rank(b, fk);
                if (ra != rb) return ra - rb;
                return a.name.compareTo(b.name);
            }
        });
        return out;
    }

    private static int rank(Item it, String kw) {
        int r = 0;
        String n = it.name == null ? "" : it.name;
        String s = it.simple == null ? "" : it.simple;
        if (n.contains("弹幕")) r += 40;
        if (n.contains("机器") || n.contains("机翻")) r += 30;
        if (s.length() > 0 && s.contains(kw)) r -= 5;
        if (n.length() > 90) r += 5;
        if ("zh".equalsIgnoreCase(it.lang)) r -= 2;
        return r;
    }

    /** 下载字幕内容（自动处理 GBK / GB18030 -> UTF-8）。 */
    public static String fetchText(Item it) throws Exception {
        byte[] raw = Http.getBytes(it.url, Http.headers("Referer", "https://www.xunlei.com/"), 20000);
        return Http.decode(raw);
    }
}
