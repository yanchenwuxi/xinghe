package com.zaka.xinghe.model;

import java.io.Serializable;

/**
 * 解析接口（TVBox 风格 parses 数组项）。
 * type: 0=嗅探, 1=JSON, 2=JSON扩展, 3=聚合...
 */
public class Parse implements Serializable {

    public String name = "";
    public int type = 0;
    public String url = "";
    public String ext = "";

    public Parse() {
    }

    public Parse(String name, int type, String url) {
        this.name = name;
        this.type = type;
        this.url = url;
    }

    @Override
    public String toString() {
        return name;
    }
}
