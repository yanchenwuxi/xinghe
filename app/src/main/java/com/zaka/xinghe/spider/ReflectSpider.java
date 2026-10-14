package com.zaka.xinghe.spider;

import android.content.Context;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 用反射包装不同 ClassLoader 下的 Spider 实例。 */
public final class ReflectSpider extends Spider {

    private final Object target;

    public ReflectSpider(Object target) {
        this.target = target;
    }

    private Object call(String name, Class<?>[] types, Object... args) {
        try {
            Method m = target.getClass().getMethod(name, types);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void init(Context context) {
        call("init", new Class[]{Context.class}, context);
    }

    @Override
    public void init(Context context, String extend) {
        Object r = call("init", new Class[]{Context.class, String.class}, context, extend);
        if (r == null) init(context);
    }

    @Override
    public String homeContent(boolean filter) {
        Object r = call("homeContent", new Class[]{boolean.class}, filter);
        return r == null ? "" : String.valueOf(r);
    }

    @Override
    public String homeVideoContent() {
        Object r = call("homeVideoContent", new Class[]{});
        return r == null ? "" : String.valueOf(r);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        Object r = call("categoryContent",
                new Class[]{String.class, String.class, boolean.class, HashMap.class},
                tid, pg, filter, extend);
        return r == null ? "" : String.valueOf(r);
    }

    @Override
    public String detailContent(List<String> ids) {
        Object r = call("detailContent", new Class[]{List.class}, ids);
        return r == null ? "" : String.valueOf(r);
    }

    @Override
    public String searchContent(String key, boolean quick) {
        Object r = call("searchContent", new Class[]{String.class, boolean.class}, key, quick);
        return r == null ? "" : String.valueOf(r);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        Object r = call("playerContent", new Class[]{String.class, String.class, List.class}, flag, id, vipFlags);
        return r == null ? "" : String.valueOf(r);
    }

    @Override
    public void destroy() {
        call("destroy", new Class[]{});
    }
}
