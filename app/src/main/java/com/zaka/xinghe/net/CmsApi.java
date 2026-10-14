package com.zaka.xinghe.net;

import com.zaka.xinghe.model.Cate;
import com.zaka.xinghe.model.FilterGroup;
import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.model.Vod;
import com.zaka.xinghe.util.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采集接口解析层（自研实现）。
 *
 * 兼容三类接口：
 * 1. 苹果 CMS 采集接口（ac=list / ac=detail / ac=videolist），JSON 返回
 * 2. 多站点配置 JSON（{"sites":[{name,api,type...}]}）
 * 3. 老式 XML 采集接口（基础支持）
 *
 * 站点在首次使用时自动探测「分类从哪个动作取」，并按站点缓存，避免每次请求都试错。
 */
public final class CmsApi {

    private static final Map<String, String> AC_CACHE = new HashMap<String, String>();
    private static final Map<String, List<Cate>> CATE_CACHE = new HashMap<String, List<Cate>>();

    private CmsApi() {
    }

    // ---------------------------------------------------------------- 配置

    /** 读取接口地址，返回站点列表（可单个，也可多个）。 */
    public static List<Site> loadConfig(String url) throws Exception {
        List<Site> out = new ArrayList<Site>();
        String body = Http.get(url);
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.length() == 0) throw new RuntimeException("empty config");

        if (trimmed.startsWith("{")) {
            JSONObject root = new JSONObject(trimmed);
            JSONArray sites = root.optJSONArray("sites");
            if (sites != null) {
                for (int i = 0; i < sites.length(); i++) {
                    JSONObject o = sites.optJSONObject(i);
                    if (o == null) continue;
                    Site s = new Site();
                    s.name = o.optString("name", "站点" + (i + 1));
                    s.api = o.optString("api", "");
                    s.ext = o.optString("ext", "");
                    int t = o.optInt("type", 3);
                    s.type = t == 1 ? "xml" : "cms";
                    s.order = i;
                    if (Utils.empty(s.api)) continue;
                    s.key = Utils.md5(s.api + "#" + s.name);
                    out.add(s);
                }
            } else {
                // 单站点
                Site s = new Site();
                s.name = root.optString("name", "");
                s.api = root.optString("api", root.optString("url", ""));
                if (Utils.empty(s.api)) s.api = url;
                if (Utils.empty(s.name)) s.name = hostOf(s.api);
                s.key = Utils.md5(s.api + "#" + s.name);
                out.add(s);
            }
        } else if (trimmed.startsWith("[")) {
            JSONArray arr = new JSONArray(trimmed);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String api = o.optString("api", o.optString("url", ""));
                if (Utils.empty(api)) continue;
                Site s = new Site();
                s.name = o.optString("name", hostOf(api));
                s.api = api;
                s.key = Utils.md5(api + "#" + s.name);
                s.order = i;
                out.add(s);
            }
        } else {
            // 直接把接口本身当单站点（苹果 CMS 地址）
            Site s = new Site();
            s.name = hostOf(url);
            s.api = url;
            s.key = Utils.md5(url);
            out.add(s);
        }
        return out;
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
            return u;
        } catch (Throwable t) {
            return "站点";
        }
    }

    // ---------------------------------------------------------------- 分类

    public static List<Cate> getCates(Site site) throws Exception {
        if (site != null && site.isSpiderOrScript()) {
            return SpiderApi.getCates(site);
        }

        List<Cate> cached = CATE_CACHE.get(site.key);
        if (cached != null && cached.size() > 0) {
            return cached;
        }
        List<Cate> out = new ArrayList<Cate>();

        // 探测顺序：
        // 1) ac=list&t=1 —— 苹果 CMS 指定分类时才会带出 class 数组（实测多数源走这条）
        // 2) ac=detail&pg=1 / ac=list —— 部分源直接返回 class
        String[] probes = new String[]{
                Utils.join(site.api, "ac=list&t=1&pg=1"),
                Utils.join(site.api, "ac=detail&pg=1"),
                Utils.join(site.api, "ac=videolist&pg=1"),
                Utils.join(site.api, "ac=list")
        };
        for (String p : probes) {
            try {
                String body = Http.get(p);
                List<Cate> list = parseCates(body);
                if (list.size() > 0) {
                    out = list;
                    AC_CACHE.put(site.key, p);
                    break;
                }
            } catch (Throwable ignore) {
            }
        }
        // 3) 兜底：接口压根不给 class 的，从列表数据里聚合 type_id/type_name，
        //    保证首页分类 Tab 一定出得来
        if (out.size() == 0) {
            out = aggregateCates(site);
        }
        if (out.size() > 0) {
            CATE_CACHE.put(site.key, out);
        }
        return out;
    }

    /** 从列表数据的 type_id / type_name 聚合出分类（兜底方案）。 */
    private static List<Cate> aggregateCates(Site site) {
        List<Cate> out = new ArrayList<Cate>();
        Map<String, String> seen = new HashMap<String, String>();
        for (int pg = 1; pg <= 3; pg++) {
            try {
                String body = Http.get(Utils.join(site.api, "ac=detail&pg=" + pg));
                String s = body == null ? "" : body.trim();
                if (!s.startsWith("{")) continue;
                JSONObject root = new JSONObject(s);
                JSONArray arr = root.optJSONArray("list");
                if (arr == null) continue;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    String id = o.optString("type_id", "");
                    String name = o.optString("type_name", "");
                    if (Utils.empty(id) || Utils.empty(name)) continue;
                    if (seen.containsKey(id)) continue;
                    seen.put(id, name);
                    out.add(new Cate(id, name));
                }
            } catch (Throwable ignore) {
            }
        }
        return out;
    }

    public static List<Cate> parseCates(String body) {
        List<Cate> out = new ArrayList<Cate>();
        if (body == null) return out;
        String s = body.trim();
        if (s.startsWith("{")) {
            try {
                JSONObject root = new JSONObject(s);
                JSONArray arr = root.optJSONArray("class");
                if (arr == null) arr = root.optJSONArray("classes");
                if (arr == null) return out;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    String id = o.optString("type_id", o.optString("type_pid", ""));
                    String name = o.optString("type_name", "");
                    if (Utils.empty(name)) continue;
                    out.add(new Cate(id, name));
                }
            } catch (Throwable ignore) {
            }
        } else if (s.startsWith("<")) {
            out = XmlCms.parseCates(s);
        }
        return out;
    }

    /** 分类对应的筛选组（接口自带 filters 就用接口的）。 */
    public static List<FilterGroup> getFilters(Site site, String cateId) {
        List<FilterGroup> out = new ArrayList<FilterGroup>();
        try {
            String url = Utils.join(site.api, "ac=detail&t=" + Utils.urlEncode(cateId) + "&pg=1");
            String body = Http.get(url);
            JSONObject root = new JSONObject(body.trim());
            JSONObject filters = root.optJSONObject("filters");
            if (filters == null) return out;
            JSONArray arr = filters.optJSONArray(cateId);
            if (arr == null) {
                // 有的接口按数字 key 返回
                java.util.Iterator<String> it = filters.keys();
                if (it.hasNext()) arr = filters.optJSONArray(it.next());
            }
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                FilterGroup g = new FilterGroup(o.optString("name", ""), o.optString("key", ""));
                JSONArray vals = o.optJSONArray("value");
                if (vals != null) {
                    for (int j = 0; j < vals.length(); j++) {
                        JSONObject v = vals.optJSONObject(j);
                        if (v == null) continue;
                        String n = v.optString("n", "");
                        if (Utils.empty(n)) continue;
                        g.values.add(n);
                    }
                }
                if (g.values.size() > 0) out.add(g);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    // ---------------------------------------------------------------- 列表

    public static List<Vod> getList(Site site, String cateId, int page, Map<String, String> filters) throws Exception {
        if (site != null && site.isSpiderOrScript()) {
            return SpiderApi.getList(site, cateId, page, filters);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("ac=detail");
        if (!Utils.empty(cateId)) sb.append("&t=").append(Utils.urlEncode(cateId));
        sb.append("&pg=").append(page <= 0 ? 1 : page);
        if (filters != null) {
            for (Map.Entry<String, String> e : filters.entrySet()) {
                if (Utils.empty(e.getValue())) continue;
                sb.append('&').append(e.getKey()).append('=').append(Utils.urlEncode(e.getValue()));
            }
        }
        String url = Utils.join(site.api, sb.toString());
        String body;
        try {
            body = Http.get(url);
        } catch (Throwable t) {
            // 退到 ac=videolist（老接口）
            String u2 = Utils.join(site.api, "ac=videolist&pg=" + page
                    + (Utils.empty(cateId) ? "" : "&t=" + Utils.urlEncode(cateId)));
            body = Http.get(u2);
        }
        List<Vod> list = parseList(body, site);
        // 接口不认筛选参数时，本地按地区/年份兜底过滤，保证选了就真按它过
        if (filters != null && !filters.isEmpty() && list.size() > 0) {
            List<Vod> filtered = localFilter(list, filters);
            if (filtered.size() > 0) return filtered;
        }
        return list;
    }

    private static List<Vod> localFilter(List<Vod> in, Map<String, String> filters) {
        List<Vod> out = new ArrayList<Vod>();
        String area = filters.get("area");
        String year = filters.get("year");
        String type = filters.get("type");
        for (Vod v : in) {
            if (!Utils.empty(area) && !Utils.empty(v.area) && !v.area.contains(area)) continue;
            if (!Utils.empty(year) && !Utils.empty(v.year) && !v.year.contains(year)) continue;
            if (!Utils.empty(type) && !Utils.empty(v.type) && !v.type.contains(type)) continue;
            out.add(v);
        }
        return out;
    }

    // ---------------------------------------------------------------- 搜索

    public static List<Vod> search(Site site, String wd, int page) throws Exception {
        if (site != null && site.isSpiderOrScript()) {
            return SpiderApi.search(site, wd, page);
        }

        String w = Utils.urlEncode(wd);
        String body = null;
        String[] tries = new String[]{
                Utils.join(site.api, "ac=detail&wd=" + w + "&pg=" + page),
                Utils.join(site.api, "ac=videolist&wd=" + w + "&pg=" + page),
                Utils.join(site.api, "wd=" + w + "&ac=detail&pg=" + page)
        };
        List<Vod> list = new ArrayList<Vod>();
        for (String u : tries) {
            try {
                body = Http.get(u);
                list = parseList(body, site);
                if (list.size() > 0) break;
            } catch (Throwable ignore) {
            }
        }
        return list;
    }

    // ---------------------------------------------------------------- 详情

    public static Vod getDetail(Site site, String id) throws Exception {
        if (site != null && site.isSpiderOrScript()) {
            return SpiderApi.getDetail(site, id);
        }

        String url = Utils.join(site.api, "ac=detail&ids=" + Utils.urlEncode(id));
        String body = Http.get(url);
        List<Vod> list = parseList(body, site);
        if (list.size() > 0) return list.get(0);
        String u2 = Utils.join(site.api, "ac=videolist&ids=" + Utils.urlEncode(id));
        List<Vod> l2 = parseList(Http.get(u2), site);
        if (l2.size() > 0) return l2.get(0);
        throw new RuntimeException("detail empty");
    }

    // ---------------------------------------------------------------- 解析

    public static List<Vod> parseList(String body, Site site) {
        List<Vod> out = new ArrayList<Vod>();
        if (body == null) return out;
        String s = body.trim();
        if (s.startsWith("{")) {
            try {
                JSONObject root = new JSONObject(s);
                JSONArray arr = root.optJSONArray("list");
                if (arr == null) arr = root.optJSONArray("data");
                if (arr == null) return out;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    Vod v = fromJson(o);
                    if (v == null) continue;
                    v.siteUrl = site.api;
                    v.siteName = site.name;
                    out.add(v);
                }
            } catch (Throwable ignore) {
            }
        } else if (s.startsWith("<")) {
            out = XmlCms.parseList(s, site);
        }
        return out;
    }

    public static Vod fromJson(JSONObject o) {
        try {
            Vod v = new Vod();
            v.id = o.optString("vod_id", o.optString("id", ""));
            v.name = o.optString("vod_name", o.optString("name", ""));
            if (Utils.empty(v.id) && Utils.empty(v.name)) return null;
            if (Utils.empty(v.name)) v.name = "未命名";
            v.pic = o.optString("vod_pic", o.optString("pic", ""));
            v.note = o.optString("vod_remarks", o.optString("note", ""));
            v.year = o.optString("vod_year", "");
            v.area = o.optString("vod_area", "");
            v.type = o.optString("type_name", o.optString("vod_class", ""));
            v.actor = o.optString("vod_actor", "");
            v.director = o.optString("vod_director", "");
            v.content = o.optString("vod_content", o.optString("vod_blurb", ""));
            v.score = o.optString("vod_score", "");
            v.lang = o.optString("vod_lang", "");

            String from = o.optString("vod_play_from", "");
            String playUrl = o.optString("vod_play_url", "");
            parsePlay(v, from, playUrl);

            // 单集直链（部分源只有 vod_url）
            if (v.flags.size() == 0) {
                String single = o.optString("vod_url", "");
                if (!Utils.empty(single)) {
                    Vod.Flag f = new Vod.Flag("默认");
                    f.eps.add(new Vod.Ep(v.name, single));
                    v.flags.add(f);
                }
            }
            return v;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 解析 vod_play_from / vod_play_url（$$$ 分线路，# 分集，$ 分名字和地址）。 */
    public static void parsePlay(Vod v, String from, String playUrl) {
        if (Utils.empty(playUrl)) return;
        String[] fromArr = Utils.empty(from) ? new String[]{"线路1"} : from.split("\\$\\$\\$");
        String[] groupArr = playUrl.split("\\$\\$\\$");
        for (int i = 0; i < groupArr.length; i++) {
            String gname = i < fromArr.length ? fromArr[i] : ("线路" + (i + 1));
            if (Utils.empty(gname)) gname = "线路" + (i + 1);
            Vod.Flag f = new Vod.Flag(gname.trim());
            String group = groupArr[i];
            // 组内：# 分集
            String[] eps = group.split("#");
            int idx = 0;
            for (String e : eps) {
                if (Utils.empty(e)) continue;
                idx++;
                String name;
                String url;
                int d = e.indexOf('$');
                if (d > 0 && d < e.length() - 1) {
                    name = e.substring(0, d).trim();
                    url = e.substring(d + 1).trim();
                } else {
                    name = "第" + idx + "集";
                    url = e.trim();
                }
                if (Utils.empty(name)) name = "第" + idx + "集";
                if (Utils.empty(url)) continue;
                f.eps.add(new Vod.Ep(name, url));
            }
            if (f.eps.size() > 0) v.flags.add(f);
        }
    }

    /**
     * 播放地址解析：多数采集源给的就是直链；部分源给的是需要二次解析的片段，
     * 这里做基础处理——拿到 http(s) 直链直接用，是 JSON 就取 url 字段。
     */
    public static String resolvePlayUrl(String url) {
        if (Utils.empty(url)) return "";
        String u = url.trim();
        if (u.startsWith("http://") || u.startsWith("https://")) return u;
        if (u.startsWith("{")) {
            try {
                JSONObject o = new JSONObject(u);
                String inside = o.optString("url", o.optString("playUrl", ""));
                if (!Utils.empty(inside)) return inside;
            } catch (Throwable ignore) {
            }
        }
        // 相对地址 / 二次解析接口，原样返回交给播放器试
        return u;
    }
}
