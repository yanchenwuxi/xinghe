package com.zaka.xinghe.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 影片条目（列表 / 详情共用一个模型）。 */
public class Vod implements Serializable {

    public String siteUrl = "";      // 来源接口
    public String siteName = "";     // 来源站点名
    public String id = "";
    public String name = "";
    public String pic = "";
    public String note = "";         // 更新备注 / 集数
    public String year = "";
    public String area = "";
    public String type = "";
    public String actor = "";
    public String director = "";
    public String content = "";
    public String score = "";
    public String lang = "";

    public List<Flag> flags = new ArrayList<Flag>();

    public String idKey() {
        return siteUrl + "@" + id;
    }

    /** 播放线路。 */
    public static class Flag implements Serializable {
        public String name = "";
        public List<Ep> eps = new ArrayList<Ep>();

        public Flag(String n) {
            name = n;
        }
    }

    /** 一集。 */
    public static class Ep implements Serializable {
        public String name = "";
        public String url = "";

        public Ep(String n, String u) {
            name = n;
            url = u;
        }
    }
}
