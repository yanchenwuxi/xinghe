package com.zaka.xinghe.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 筛选组：一组可选值 + 当前选中。 */
public class FilterGroup implements Serializable {

    public String name = "";
    public String key = "";
    public List<String> values = new ArrayList<String>();
    public String selected = "";

    public FilterGroup(String name, String key) {
        this.name = name;
        this.key = key;
    }

    public String label() {
        return selected == null || selected.length() == 0 ? name : selected;
    }
}
