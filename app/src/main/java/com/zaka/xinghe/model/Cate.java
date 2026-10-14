package com.zaka.xinghe.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 分类。 */
public class Cate implements Serializable {

    public String id = "";
    public String name = "";

    public Cate(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /** 采集接口返回的筛选结构。 */
    public static class FilterBox implements Serializable {
        public List<FilterGroup> groups = new ArrayList<FilterGroup>();
    }
}
