package com.zaka.xinghe.net;

import com.zaka.xinghe.model.Cate;
import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.model.Vod;
import com.zaka.xinghe.util.Utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/** 老式 XML 采集接口的基础解析。 */
public final class XmlCms {

    private XmlCms() {
    }

    public static List<Cate> parseCates(String xml) {
        List<Cate> out = new ArrayList<Cate>();
        try {
            XmlPullParser p = factory(xml);
            int ev;
            while ((ev = p.next()) != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) {
                    String tag = p.getName();
                    if ("ty".equalsIgnoreCase(tag) || "class".equalsIgnoreCase(tag)) {
                        String id = attr(p, "id");
                        String name = attr(p, "text");
                        if (Utils.empty(name)) name = attr(p, "name");
                        if (!Utils.empty(name)) out.add(new Cate(id, name));
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static List<Vod> parseList(String xml, Site site) {
        List<Vod> out = new ArrayList<Vod>();
        try {
            XmlPullParser p = factory(xml);
            Vod cur = null;
            String tag = "";
            StringBuilder txt = new StringBuilder();
            int ev;
            while ((ev = p.next()) != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) {
                    tag = p.getName();
                    txt.setLength(0);
                    if ("video".equalsIgnoreCase(tag) || "item".equalsIgnoreCase(tag)) {
                        cur = new Vod();
                        cur.siteUrl = site.api;
                        cur.siteName = site.name;
                    }
                } else if (ev == XmlPullParser.TEXT) {
                    txt.append(p.getText() == null ? "" : p.getText());
                } else if (ev == XmlPullParser.END_TAG) {
                    String name = p.getName();
                    String val = txt.toString().trim();
                    if (cur != null) {
                        if ("id".equalsIgnoreCase(name)) cur.id = val;
                        else if ("name".equalsIgnoreCase(name) || "title".equalsIgnoreCase(name)) cur.name = val;
                        else if ("pic".equalsIgnoreCase(name)) cur.pic = val;
                        else if ("note".equalsIgnoreCase(name) || "remarks".equalsIgnoreCase(name)) cur.note = val;
                        else if ("year".equalsIgnoreCase(name)) cur.year = val;
                        else if ("area".equalsIgnoreCase(name)) cur.area = val;
                        else if ("type".equalsIgnoreCase(name)) cur.type = val;
                        else if ("actor".equalsIgnoreCase(name)) cur.actor = val;
                        else if ("director".equalsIgnoreCase(name)) cur.director = val;
                        else if ("des".equalsIgnoreCase(name) || "content".equalsIgnoreCase(name)) cur.content = val;
                        else if ("dl".equalsIgnoreCase(name) && !Utils.empty(val)) {
                            CmsApi.parsePlay(cur, "", val);
                        } else if ("video".equalsIgnoreCase(name) || "item".equalsIgnoreCase(name)) {
                            if (!Utils.empty(cur.name)) out.add(cur);
                            cur = null;
                        }
                    }
                }
            }
            if (cur != null && !Utils.empty(cur.name)) out.add(cur);
        } catch (Throwable ignore) {
        }
        return out;
    }

    private static XmlPullParser factory(String xml) throws Exception {
        XmlPullParserFactory f = XmlPullParserFactory.newInstance();
        f.setNamespaceAware(false);
        XmlPullParser p = f.newPullParser();
        p.setInput(new StringReader(xml));
        return p;
    }

    private static String attr(XmlPullParser p, String name) {
        String v = p.getAttributeValue(null, name);
        return v == null ? "" : v;
    }
}
