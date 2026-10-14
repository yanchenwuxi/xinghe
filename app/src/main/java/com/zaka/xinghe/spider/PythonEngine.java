package com.zaka.xinghe.spider;

import android.content.Context;

import com.zaka.xinghe.util.Utils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

/**
 * Python 爬虫桥接。
 * 若工程集成了 Chaquopy（com.chaquo.python），通过反射调用；
 * 否则抛出明确提示，便于用户自行加入 chaquopy AAR。
 */
public final class PythonEngine extends Spider {

    private final Object pyModule;
    private final Object pyObject;

    public PythonEngine(Context ctx, String moduleOrCode, String extend) throws Exception {
        Class<?> pythonCls;
        try {
            pythonCls = Class.forName("com.chaquo.python.Python");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "未集成 Chaquopy。请在 app 依赖中加入 chaquopy，并在 Application 中初始化 Python。");
        }
        Method getInstance = pythonCls.getMethod("getInstance");
        Object py = getInstance.invoke(null);
        if (py == null) {
            // 尝试 start
            try {
                Class<?> platform = Class.forName("com.chaquo.python.android.AndroidPlatform");
                Object plat = platform.getConstructor(Context.class).newInstance(ctx.getApplicationContext());
                Method start = pythonCls.getMethod("start", Class.forName("com.chaquo.python.Python$Platform"));
                // Platform is interface - use AndroidPlatform
                Method start2 = pythonCls.getMethod("start", Class.forName("com.chaquo.python.GenericPlatform").getSuperclass());
            } catch (Throwable ignore) {
            }
            py = getInstance.invoke(null);
        }
        if (py == null) throw new RuntimeException("Python 引擎未启动，请检查 Chaquopy 初始化");

        Method getModule = py.getClass().getMethod("getModule", String.class);
        // moduleOrCode 作为模块名；若是文件路径需先复制到 python 路径
        String modName = moduleOrCode;
        if (modName.contains("/") || modName.endsWith(".py")) {
            modName = modName.replace(".py", "");
            int slash = modName.lastIndexOf('/');
            if (slash >= 0) modName = modName.substring(slash + 1);
        }
        this.pyModule = getModule.invoke(py, modName);
        this.pyObject = this.pyModule;
        this.siteKey = modName;
    }

    private String call(String name, Object... args) {
        try {
            Method callAttr = pyObject.getClass().getMethod("callAttr", String.class, Object[].class);
            Object r = callAttr.invoke(pyObject, name, args);
            return r == null ? "" : String.valueOf(r);
        } catch (Throwable t) {
            return "";
        }
    }

    @Override
    public String homeContent(boolean filter) {
        return call("homeContent", filter);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        return call("categoryContent", tid, pg, filter, extend);
    }

    @Override
    public String detailContent(List<String> ids) {
        return call("detailContent", ids);
    }

    @Override
    public String searchContent(String key, boolean quick) {
        return call("searchContent", key, quick);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        return call("playerContent", flag, id, vipFlags);
    }
}
