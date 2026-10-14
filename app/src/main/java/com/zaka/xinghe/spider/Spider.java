package com.zaka.xinghe.spider;

import android.content.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 爬虫基类，对齐 catvod / TVBox Spider API。
 * Jar 源通过 DexClassLoader 加载子类；JS/Python 由对应 Engine 包装实现。
 */
public abstract class Spider {

    public String siteKey = "";

    public void init(Context context) {
    }

    public void init(Context context, String extend) {
        init(context);
    }

    public String homeContent(boolean filter) {
        return "";
    }

    public String homeVideoContent() {
        return "";
    }

    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        return "";
    }

    public String detailContent(List<String> ids) {
        return "";
    }

    public String searchContent(String key, boolean quick) {
        return "";
    }

    public String searchContent(String key, boolean quick, String pg) {
        return searchContent(key, quick);
    }

    public String playerContent(String flag, String id, List<String> vipFlags) {
        return "";
    }

    public String liveContent(String url) {
        return "";
    }

    public boolean isVideoFormat(String url) {
        return false;
    }

    public boolean manualVideoCheck() {
        return false;
    }

    public String action(String action) {
        return null;
    }

    public Object[] proxy(Map<String, String> params) {
        return null;
    }

    public void destroy() {
    }
}
