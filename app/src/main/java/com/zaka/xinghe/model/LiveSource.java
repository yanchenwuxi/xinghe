package com.zaka.xinghe.model;

import java.io.Serializable;

/** 直播源（TVBox lives 数组项）。 */
public class LiveSource implements Serializable {

    public String name = "";
    public String url = "";
    public int type = 0;       // 0=m3u, 1=txt 等
    public boolean epg = false;
    public String epgUrl = "";
    public String logo = "";

    public LiveSource() {
    }

    public LiveSource(String name, String url) {
        this.name = name;
        this.url = url;
    }
}
