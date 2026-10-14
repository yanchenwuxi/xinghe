package com.zaka.xinghe.model;

import java.io.Serializable;

/** 观看记录 / 收藏。 */
public class Record implements Serializable {

    public String vodId = "";
    public String siteUrl = "";
    public String siteName = "";
    public String name = "";
    public String pic = "";
    public String note = "";
    public String flagName = "";
    public int epIndex = 0;
    public String epName = "";
    public long position = 0L;     // 上次播放位置（毫秒）
    public long duration = 0L;
    public long updateTime = 0L;   // 最近观看时间
    public boolean fav = false;

    public String key() {
        return siteUrl + "@" + vodId;
    }
}
