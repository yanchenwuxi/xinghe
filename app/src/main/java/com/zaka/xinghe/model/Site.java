package com.zaka.xinghe.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 采集站点（TVBox sites 数组项）。
 *
 * type 对齐 TVBox：
 *   0 = XML 源
 *   1 = JSON 源（苹果 CMS 风格）
 *   2 = 爬虫/JS 源（需要 QuickJS，本纯实现仅检测提示）
 *   3 = 标准 CMS（同 1）
 *   4 = 钻研等
 * 另支持 spider / python 标记，运行时给出友好提示。
 */
public class Site implements Serializable {

    public String key = "";
    public String name = "";
    public String api = "";
    public String ext = "";
    /** 字符串形式：cms / xml / spider / js / python */
    public String type = "cms";
    /** TVBox 原始 type 数字 */
    public int typeCode = 1;
    public int order = 0;
    public boolean searchable = true;
    public boolean changeable = true;
    public String playUrl = "";
    public String jar = "";
    public List<Parse> parses = new ArrayList<Parse>();

    public Site() {
    }

    public Site(String name, String api) {
        this.name = name;
        this.api = api;
        this.key = com.zaka.xinghe.util.Utils.md5(api);
        this.type = "cms";
        this.typeCode = 1;
    }

    public boolean isSpiderOrScript() {
        return "spider".equals(type) || "js".equals(type) || "python".equals(type)
                || typeCode == 2 || typeCode == 4;
    }

    public boolean isXml() {
        return "xml".equals(type) || typeCode == 0;
    }

    public boolean isCms() {
        return !isXml() && !isSpiderOrScript();
    }

    @Override
    public String toString() {
        return name;
    }
}
