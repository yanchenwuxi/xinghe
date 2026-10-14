package com.zaka.xinghe.net;

import com.zaka.xinghe.model.LiveSource;
import com.zaka.xinghe.model.Parse;
import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.util.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析 TVBox 风格配置（以及简化单接口）。
 *
 * 支持：
 * - 完整配置 JSON：{"sites":[...], "parses":[...], "lives":[...]}
 * - 仅 sites 数组
 * - 单个接口地址（当站点）
 * - 本地文件路径（file:// 或绝对路径）
 */
public final class ConfigParser {

    public static class Result {
        public final List<Site> sites = new ArrayList<Site>();
        public final List<Parse> parses = new ArrayList<Parse>();
        public final List<LiveSource> lives = new ArrayList<LiveSource>();
        public String wallpaper = "";
        public String warning = "";
    }

    private ConfigParser() {
    }

    /** 从网络地址或本地路径加载。 */
    public static Result load(String input) throws Exception {
        if (Utils.empty(input)) throw new RuntimeException("地址为空");
        String body;
        String src = input.trim();
        if (src.startsWith("file://")) {
            src = src.substring(7);
        }
        if (src.startsWith("/") || src.startsWith("file:")) {
            body = readFile(src.startsWith("file:") ? src.substring(5) : src);
        } else if (src.startsWith("{") || src.startsWith("[")) {
            body = src; // 直接粘贴的 JSON 内容
        } else {
            body = Http.get(src);
        }
        return parse(body, src);
    }

    public static Result parse(String body, String fallbackUrl) throws Exception {
        Result r = new Result();
        if (Utils.empty(body)) throw new RuntimeException("配置内容为空");
        String trimmed = body.trim();

        if (trimmed.startsWith("{")) {
            JSONObject root = new JSONObject(trimmed);
            r.wallpaper = root.optString("wallpaper", "");
            r.warning = root.optString("warningText", root.optString("warning", ""));

            // sites
            JSONArray sites = root.optJSONArray("sites");
            if (sites != null) {
                for (int i = 0; i < sites.length(); i++) {
                    JSONObject o = sites.optJSONObject(i);
                    if (o == null) continue;
                    Site s = siteFromJson(o, i);
                    if (s != null) {
                        if (fallbackUrl != null && (fallbackUrl.startsWith("http://") || fallbackUrl.startsWith("https://"))) {
                            s.baseUrl = fallbackUrl;
                        }
                        r.sites.add(s);
                    }
                }
            }

            // parses
            JSONArray parses = root.optJSONArray("parses");
            if (parses != null) {
                for (int i = 0; i < parses.length(); i++) {
                    JSONObject o = parses.optJSONObject(i);
                    if (o == null) continue;
                    Parse p = new Parse();
                    p.name = o.optString("name", "解析" + (i + 1));
                    p.type = o.optInt("type", 0);
                    p.url = o.optString("url", "");
                    p.ext = o.optString("ext", "");
                    if (!Utils.empty(p.url) || p.type == 0) r.parses.add(p);
                }
            }

            // lives
            JSONArray lives = root.optJSONArray("lives");
            if (lives != null) {
                for (int i = 0; i < lives.length(); i++) {
                    Object item = lives.opt(i);
                    if (item instanceof JSONObject) {
                        JSONObject o = (JSONObject) item;
                        LiveSource ls = new LiveSource();
                        ls.name = o.optString("name", "直播" + (i + 1));
                        ls.url = o.optString("url", o.optString("api", ""));
                        ls.type = o.optInt("type", 0);
                        ls.epg = o.optBoolean("epg", false) || !Utils.empty(o.optString("epg", ""));
                        ls.epgUrl = o.optString("epg", o.optString("epgUrl", ""));
                        ls.logo = o.optString("logo", "");
                        if (!Utils.empty(ls.url)) r.lives.add(ls);
                    } else if (item instanceof String) {
                        String u = (String) item;
                        if (!Utils.empty(u)) r.lives.add(new LiveSource("直播" + (i + 1), u));
                    }
                }
            }

            // 单站点兜底
            if (r.sites.isEmpty()) {
                String api = root.optString("api", root.optString("url", ""));
                if (!Utils.empty(api)) {
                    Site s = new Site(root.optString("name", hostOf(api)), api);
                    s.typeCode = root.optInt("type", 1);
                    s.type = typeName(s.typeCode, root.optString("type", ""));
                    r.sites.add(s);
                }
            }
        } else if (trimmed.startsWith("[")) {
            JSONArray arr = new JSONArray(trimmed);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Site s = siteFromJson(o, i);
                if (s != null) r.sites.add(s);
            }
        } else {
            // 纯接口地址
            Site s = new Site(hostOf(fallbackUrl), fallbackUrl);
            r.sites.add(s);
        }
        return r;
    }

    private static Site siteFromJson(JSONObject o, int index) {
        String api = o.optString("api", o.optString("url", ""));
        if (Utils.empty(api)) return null;
        Site s = new Site();
        s.name = o.optString("name", "站点" + (index + 1));
        s.api = api;
        s.ext = o.optString("ext", "");
        s.key = o.optString("key", Utils.md5(api + "#" + s.name));
        s.typeCode = o.optInt("type", 1);
        // 字符串 type 优先（部分配置写 "type":"csp_xxx"）
        String typeStr = o.optString("type", "");
        if (typeStr.length() > 0 && !typeStr.matches("\\d+")) {
            s.type = typeStr.toLowerCase();
            if (s.type.contains("spider") || s.type.startsWith("csp_")) s.type = "spider";
            else if (s.type.contains("js")) s.type = "js";
            else if (s.type.contains("py") || s.type.contains("python")) s.type = "python";
            else if (s.type.contains("xml")) s.type = "xml";
            else s.type = "cms";
        } else {
            s.type = typeName(s.typeCode, "");
        }
        s.searchable = o.optInt("searchable", 1) != 0;
        s.changeable = o.optInt("changeable", 1) != 0;
        s.playUrl = o.optString("playUrl", "");
        s.jar = o.optString("jar", "");
        s.order = index;
        // 二次纠正：api/ext 指向 .js 时强制 js
        String apiL = s.api == null ? "" : s.api.toLowerCase();
        String extL = s.ext == null ? "" : s.ext.toLowerCase();
        if (apiL.endsWith(".js") || extL.endsWith(".js") || apiL.contains(".js?") || extL.contains(".js?")) {
            if (!"python".equals(s.type) && !"spider".equals(s.type)) {
                s.type = "js";
                if (s.typeCode == 1) s.typeCode = 3;
            }
        }
        // csp_ 且无 jar 但有 js → js
        if (apiL.startsWith("csp_") && (extL.endsWith(".js") || extL.contains(".js"))) {
            s.type = "js";
            s.typeCode = 3;
        }
        return s;
    }

    private static String typeName(int code, String hint) {
        if (hint != null && hint.length() > 0) {
            String h = hint.toLowerCase();
            if (h.contains("xml")) return "xml";
            if (h.contains("spider") || h.startsWith("csp")) return "spider";
            if (h.contains("js")) return "js";
            if (h.contains("py") || h.contains("python")) return "python";
        }
        switch (code) {
            case 0: return "xml";
            case 2: return "spider";
            case 3: return "js";      // TVBox: type 3 = JS 爬虫
            case 4: return "spider";  // Jar / csp
            default: return "cms";
        }
    }

    private static String readFile(String path) throws Exception {
        File f = new File(path);
        if (!f.exists() || !f.isFile()) throw new RuntimeException("文件不存在: " + path);
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        try {
            char[] buf = new char[4096];
            int n;
            while ((n = br.read(buf)) >= 0) sb.append(buf, 0, n);
        } finally {
            try { br.close(); } catch (Throwable ignore) {}
        }
        return sb.toString();
    }

    private static String hostOf(String url) {
        try {
            String u = url;
            int i = u.indexOf("://");
            if (i > 0) u = u.substring(i + 3);
            int j = u.indexOf('/');
            if (j > 0) u = u.substring(0, j);
            int k = u.indexOf(':');
            if (k > 0) u = u.substring(0, k);
            return u.length() > 0 ? u : "站点";
        } catch (Throwable t) {
            return "站点";
        }
    }
}
