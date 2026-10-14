package com.zaka.xinghe.store;

import android.content.Context;
import android.content.SharedPreferences;

import com.zaka.xinghe.model.LiveSource;
import com.zaka.xinghe.model.Parse;
import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.net.ConfigParser;
import com.zaka.xinghe.util.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 站点 / 解析 / 直播源 管理。 */
public final class SiteStore {

    private static final String SP = "xinghe_site";
    private static final String K_LIST = "sites";
    private static final String K_CUR = "current";
    private static final String K_PARSES = "parses";
    private static final String K_LIVES = "lives";

    private static SharedPreferences sp;

    private static final String[][] BUILTIN = {
            {"电影天堂资源", "https://caiji.dyttzyapi.com/api.php/provide/vod/"},
            {"量子资源", "https://cj.lziapi.com/api.php/provide/vod/"},
            {"百度云资源", "https://api.apibdzy.com/api.php/provide/vod/"},
    };

    private SiteStore() {
    }

    public static void init(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(SP, Context.MODE_PRIVATE);
        if (!sp.contains(K_LIST)) {
            List<Site> init = new ArrayList<Site>();
            for (String[] kv : BUILTIN) {
                init.add(new Site(kv[0], kv[1]));
            }
            saveSites(init);
            if (init.size() > 0) {
                sp.edit().putString(K_CUR, init.get(0).key).apply();
            }
        }
    }

    public static List<Site> sites() {
        List<Site> out = new ArrayList<Site>();
        String s = sp.getString(K_LIST, "");
        if (Utils.empty(s)) return out;
        try {
            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Site site = new Site();
                site.key = o.optString("key", "");
                site.name = o.optString("name", "");
                site.api = o.optString("api", "");
                site.ext = o.optString("ext", "");
                site.type = o.optString("type", "cms");
                site.typeCode = o.optInt("typeCode", 1);
                site.order = o.optInt("order", i);
                site.searchable = o.optBoolean("searchable", true);
                site.changeable = o.optBoolean("changeable", true);
                site.playUrl = o.optString("playUrl", "");
                site.jar = o.optString("jar", "");
                if (Utils.empty(site.key)) site.key = Utils.md5(site.api + "#" + site.name);
                out.add(site);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static void saveSites(List<Site> list) {
        JSONArray arr = new JSONArray();
        try {
            int i = 0;
            for (Site s : list) {
                JSONObject o = new JSONObject();
                o.put("key", s.key);
                o.put("name", s.name);
                o.put("api", s.api);
                o.put("ext", s.ext);
                o.put("type", s.type);
                o.put("typeCode", s.typeCode);
                o.put("order", i++);
                o.put("searchable", s.searchable);
                o.put("changeable", s.changeable);
                o.put("playUrl", s.playUrl);
                o.put("jar", s.jar);
                arr.put(o);
            }
        } catch (Throwable ignore) {
        }
        sp.edit().putString(K_LIST, arr.toString()).apply();
    }

    public static void add(Site s) {
        List<Site> list = sites();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).key.equals(s.key)) {
                list.set(i, s);
                saveSites(list);
                return;
            }
        }
        list.add(s);
        saveSites(list);
    }

    public static void addAll(List<Site> add) {
        List<Site> list = sites();
        for (Site s : add) {
            boolean found = false;
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).key.equals(s.key)) {
                    list.set(i, s);
                    found = true;
                    break;
                }
            }
            if (!found) list.add(s);
        }
        saveSites(list);
    }

    public static void remove(Site s) {
        List<Site> list = sites();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).key.equals(s.key)) list.remove(i);
        }
        saveSites(list);
        if (s.key.equals(currentKey())) {
            sp.edit().putString(K_CUR, list.size() > 0 ? list.get(0).key : "").apply();
        }
    }

    public static String currentKey() {
        return sp.getString(K_CUR, "");
    }

    public static void setCurrent(Site s) {
        if (s == null) return;
        sp.edit().putString(K_CUR, s.key).apply();
    }

    public static Site current() {
        String k = currentKey();
        List<Site> list = sites();
        if (list.size() == 0) return null;
        for (Site s : list) {
            if (s.key.equals(k)) return s;
        }
        return list.get(0);
    }

    public static List<Parse> parses() {
        List<Parse> out = new ArrayList<Parse>();
        String s = sp.getString(K_PARSES, "");
        if (Utils.empty(s)) return out;
        try {
            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Parse p = new Parse();
                p.name = o.optString("name", "");
                p.type = o.optInt("type", 0);
                p.url = o.optString("url", "");
                p.ext = o.optString("ext", "");
                out.add(p);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static void saveParses(List<Parse> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Parse p : list) {
                JSONObject o = new JSONObject();
                o.put("name", p.name);
                o.put("type", p.type);
                o.put("url", p.url);
                o.put("ext", p.ext);
                arr.put(o);
            }
        } catch (Throwable ignore) {
        }
        sp.edit().putString(K_PARSES, arr.toString()).apply();
    }

    public static List<LiveSource> lives() {
        List<LiveSource> out = new ArrayList<LiveSource>();
        String s = sp.getString(K_LIVES, "");
        if (Utils.empty(s)) return out;
        try {
            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                LiveSource ls = new LiveSource();
                ls.name = o.optString("name", "");
                ls.url = o.optString("url", "");
                ls.type = o.optInt("type", 0);
                ls.epg = o.optBoolean("epg", false);
                ls.epgUrl = o.optString("epgUrl", "");
                ls.logo = o.optString("logo", "");
                out.add(ls);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static void saveLives(List<LiveSource> list) {
        JSONArray arr = new JSONArray();
        try {
            for (LiveSource ls : list) {
                JSONObject o = new JSONObject();
                o.put("name", ls.name);
                o.put("url", ls.url);
                o.put("type", ls.type);
                o.put("epg", ls.epg);
                o.put("epgUrl", ls.epgUrl);
                o.put("logo", ls.logo);
                arr.put(o);
            }
        } catch (Throwable ignore) {
        }
        sp.edit().putString(K_LIVES, arr.toString()).apply();
    }

    /** 从配置地址/本地文件/粘贴内容导入，返回结果摘要。 */
    public static String importConfig(String input) throws Exception {
        ConfigParser.Result r = ConfigParser.load(input);
        int siteCount = 0, parseCount = 0, liveCount = 0;
        if (r.sites.size() > 0) {
            addAll(r.sites);
            siteCount = r.sites.size();
            if (Utils.empty(currentKey()) && r.sites.size() > 0) {
                setCurrent(r.sites.get(0));
            }
        }
        if (r.parses.size() > 0) {
            List<Parse> old = parses();
            for (Parse np : r.parses) {
                boolean found = false;
                for (int i = 0; i < old.size(); i++) {
                    if (old.get(i).name.equals(np.name)) {
                        old.set(i, np);
                        found = true;
                        break;
                    }
                }
                if (!found) old.add(np);
            }
            saveParses(old);
            parseCount = r.parses.size();
        }
        if (r.lives.size() > 0) {
            List<LiveSource> old = lives();
            for (LiveSource nl : r.lives) {
                boolean found = false;
                for (int i = 0; i < old.size(); i++) {
                    if (old.get(i).url.equals(nl.url)) {
                        old.set(i, nl);
                        found = true;
                        break;
                    }
                }
                if (!found) old.add(nl);
            }
            saveLives(old);
            liveCount = r.lives.size();
        }
        StringBuilder msg = new StringBuilder();
        msg.append("导入成功：站点 ").append(siteCount);
        msg.append("，解析 ").append(parseCount);
        msg.append("，直播源 ").append(liveCount);
        if (!Utils.empty(r.warning)) {
            msg.append("\n提示：").append(r.warning);
        }
        int script = 0;
        for (Site s : r.sites) {
            if (s.isSpiderOrScript()) script++;
        }
        if (script > 0) {
            msg.append("\n含 ").append(script)
                    .append(" 个 JS/爬虫/Python 源，将尝试脚本引擎：")
                    .append("Jar→DexClassLoader，JS→WebView，Python→需 Chaquopy。");
        }
        return msg.toString();
    }
}
