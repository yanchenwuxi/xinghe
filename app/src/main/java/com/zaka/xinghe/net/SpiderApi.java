package com.zaka.xinghe.net;

import android.content.Context;

import com.zaka.xinghe.App;
import com.zaka.xinghe.model.Cate;
import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.model.Vod;
import com.zaka.xinghe.spider.Spider;
import com.zaka.xinghe.spider.SpiderFactory;
import com.zaka.xinghe.util.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 Spider 返回的 JSON（TVBox 标准结构）转为内部模型。
 */
public final class SpiderApi {

    private static final Map<String, Spider> SPIDERS = new HashMap<String, Spider>();

    private static void checkSpiderResult(String method, String json) throws Exception {
        if (json != null && json.startsWith("ERR:")) {
            throw new RuntimeException(method + " 失败: " + json.substring(4));
        }
    }

    private SpiderApi() {
    }

    private static Spider spider(Site site) throws Exception {
        Spider s = SPIDERS.get(site.key);
        if (s != null) return s;
        Context ctx = App.get();
        if (ctx == null) throw new RuntimeException("App 未初始化");
        s = SpiderFactory.create(ctx, site);
        SPIDERS.put(site.key, s);
        return s;
    }

    public static List<Cate> getCates(Site site) throws Exception {
        Spider sp = spider(site);
        String json = sp.homeContent(true);
        checkSpiderResult("homeContent", json);
        List<Cate> out = new ArrayList<Cate>();
        if (Utils.empty(json)) return out;
        JSONObject root = new JSONObject(json);
        JSONArray classes = root.optJSONArray("class");
        if (classes == null) classes = root.optJSONArray("classes");
        if (classes != null) {
            for (int i = 0; i < classes.length(); i++) {
                JSONObject o = classes.optJSONObject(i);
                if (o == null) continue;
                Cate c = new Cate();
                c.id = o.optString("type_id", o.optString("id", ""));
                c.name = o.optString("type_name", o.optString("name", ""));
                if (!Utils.empty(c.id) || !Utils.empty(c.name)) out.add(c);
            }
        }
        return out;
    }

    public static List<Vod> getList(Site site, String cateId, int page, Map<String, String> filters) throws Exception {
        Spider sp = spider(site);
        HashMap<String, String> ext = new HashMap<String, String>();
        if (filters != null) ext.putAll(filters);
        String json = sp.categoryContent(cateId == null ? "" : cateId, String.valueOf(page <= 0 ? 1 : page), true, ext);
        checkSpiderResult("categoryContent", json);
        return parseVodList(json, site);
    }

    public static List<Vod> search(Site site, String wd, int page) throws Exception {
        Spider sp = spider(site);
        String json = sp.searchContent(wd, false);
        checkSpiderResult("searchContent", json);
        return parseVodList(json, site);
    }

    public static Vod getDetail(Site site, String id) throws Exception {
        Spider sp = spider(site);
        List<String> ids = new ArrayList<String>();
        ids.add(id);
        String json = sp.detailContent(ids);
        checkSpiderResult("detailContent", json);
        if (Utils.empty(json)) return null;
        JSONObject root = new JSONObject(json);
        JSONArray list = root.optJSONArray("list");
        if (list == null || list.length() == 0) {
            // 有的直接返回单个
            return vodFromSpiderObj(root, site);
        }
        return vodFromSpiderObj(list.optJSONObject(0), site);
    }

    public static String playerContent(Site site, String flag, String id) throws Exception {
        Spider sp = spider(site);
        String json = sp.playerContent(flag, id, new ArrayList<String>());
        checkSpiderResult("playerContent", json);
        return json;
    }

    private static List<Vod> parseVodList(String json, Site site) {
        List<Vod> out = new ArrayList<Vod>();
        if (Utils.empty(json)) return out;
        try {
            JSONObject root = new JSONObject(json);
            JSONArray list = root.optJSONArray("list");
            if (list == null) return out;
            for (int i = 0; i < list.length(); i++) {
                Vod v = vodFromSpiderObj(list.optJSONObject(i), site);
                if (v != null) out.add(v);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    private static Vod vodFromSpiderObj(JSONObject o, Site site) {
        if (o == null) return null;
        Vod v = new Vod();
        v.id = o.optString("vod_id", o.optString("id", ""));
        v.name = o.optString("vod_name", o.optString("name", ""));
        v.pic = o.optString("vod_pic", o.optString("pic", ""));
        v.note = o.optString("vod_remarks", o.optString("note", o.optString("remarks", "")));
        v.year = o.optString("vod_year", o.optString("year", ""));
        v.area = o.optString("vod_area", o.optString("area", ""));
        v.type = o.optString("type_name", o.optString("vod_class", ""));
        v.actor = o.optString("vod_actor", o.optString("actor", ""));
        v.director = o.optString("vod_director", o.optString("director", ""));
        v.content = o.optString("vod_content", o.optString("content", ""));
        v.siteUrl = site.api;
        v.siteName = site.name;
        // 播放线路
        String from = o.optString("vod_play_from", "");
        String url = o.optString("vod_play_url", "");
        if (!Utils.empty(from) && !Utils.empty(url)) {
            CmsApi.parsePlay(v, from, url);
        }
        return v;
    }
}
