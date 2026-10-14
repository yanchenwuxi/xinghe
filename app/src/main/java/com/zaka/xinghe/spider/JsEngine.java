package com.zaka.xinghe.spider;

import android.content.Context;
import android.util.Log;

import com.whl.quickjs.android.QuickJSLoader;
import com.whl.quickjs.wrapper.JSCallFunction;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;
import com.zaka.xinghe.net.Http;
import com.zaka.xinghe.util.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 QuickJS 原生 so（libquickjs-android-wrapper.so）的 JS 爬虫引擎。
 * 启动时 System.loadLibrary，向全局注入 req/http 等基础方法，再执行爬虫脚本。
 */
public final class JsEngine extends Spider {

    private static final String TAG = "JsEngine";
    private static boolean sLibLoaded = false;
    private static final Object LIB_LOCK = new Object();

    private final QuickJSContext ctx;
    private final String jsSource;
    private String extend = "";

    public static void ensureNativeLoaded() {
        synchronized (LIB_LOCK) {
            if (sLibLoaded) return;
            try {
                QuickJSLoader.init(); // System.loadLibrary("quickjs-android-wrapper")
                sLibLoaded = true;
            } catch (Throwable t) {
                Log.e(TAG, "load quickjs failed", t);
                throw new RuntimeException("加载 QuickJS 原生库失败: " + t.getMessage(), t);
            }
        }
    }

    public JsEngine(Context appCtx, String jsCodeOrUrl, String extend) throws Exception {
        ensureNativeLoaded();
        this.extend = extend == null ? "" : extend;
        if (Utils.empty(jsCodeOrUrl)) throw new RuntimeException("JS 源为空");
        if (jsCodeOrUrl.trim().startsWith("http://") || jsCodeOrUrl.trim().startsWith("https://")) {
            this.jsSource = Http.get(jsCodeOrUrl);
        } else {
            this.jsSource = jsCodeOrUrl;
        }
        this.ctx = QuickJSContext.create();
        injectGlobals(this.ctx);
        // 加载脚本
        this.ctx.evaluate(this.jsSource, "spider.js");
        // 若有 init
        try {
            this.ctx.evaluate("if(typeof init==='function'){init(" + jsonStr(this.extend) + ");}", "init.js");
        } catch (Throwable ignore) {
        }
    }

    public static Spider create(Context ctx, String js, String extend) throws Exception {
        return new JsEngine(ctx.getApplicationContext(), js, extend);
    }

    private void injectGlobals(final QuickJSContext qjs) {
        JSObject global = qjs.getGlobalObject();
        // req(url, opt) -> {code, content, headers}
        global.setProperty("req", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                try {
                    String url = args != null && args.length > 0 ? String.valueOf(args[0]) : "";
                    Map<String, String> headers = new HashMap<String, String>();
                    String method = "GET";
                    String body = null;
                    if (args != null && args.length > 1 && args[1] instanceof JSObject) {
                        JSObject opt = (JSObject) args[1];
                        Object m = opt.getProperty("method");
                        if (m != null) method = String.valueOf(m);
                        Object h = opt.getProperty("headers");
                        // headers 简化处理
                        Object d = opt.getProperty("data");
                        if (d != null) body = String.valueOf(d);
                        Object b = opt.getProperty("body");
                        if (b != null) body = String.valueOf(b);
                    }
                    String content;
                    if ("POST".equalsIgnoreCase(method)) {
                        content = Http.post(url, body, headers);
                    } else {
                        content = Http.get(url, headers, 15000);
                    }
                    JSObject res = qjs.createNewJSObject();
                    res.setProperty("code", 200);
                    res.setProperty("content", content == null ? "" : content);
                    return res;
                } catch (Throwable e) {
                    JSObject res = qjs.createNewJSObject();
                    res.setProperty("code", 500);
                    res.setProperty("content", e.getMessage() == null ? "" : e.getMessage());
                    return res;
                }
            }
        });
        // http get 简写
        global.setProperty("http", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                try {
                    String url = args != null && args.length > 0 ? String.valueOf(args[0]) : "";
                    return Http.get(url);
                } catch (Throwable e) {
                    return "";
                }
            }
        });
        // log
        global.setProperty("log", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                if (args != null) {
                    for (Object a : args) Log.d(TAG, String.valueOf(a));
                }
                return null;
            }
        });
        // md5 可选
        global.setProperty("md5", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                if (args == null || args.length == 0) return "";
                return Utils.md5(String.valueOf(args[0]));
            }
        });
    }

    private String callFn(String name, String argsExpr) {
        try {
            Object r = ctx.evaluate(
                    "(function(){ try { var r = " + name + "(" + argsExpr + ");"
                            + " if (r == null || r === undefined) return '';"
                            + " if (typeof r === 'string') return r;"
                            + " return JSON.stringify(r); } catch(e) { return 'ERR:' + e; } })()",
                    "call_" + name + ".js");
            if (r == null) return "";
            String s = String.valueOf(r);
            if (s.startsWith("ERR:")) {
                Log.e(TAG, s);
                return "";
            }
            return s;
        } catch (Throwable e) {
            Log.e(TAG, "call " + name, e);
            return "";
        }
    }

    private static String jsonStr(String s) {
        if (s == null) return "''";
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n") + "'";
    }

    @Override
    public void init(Context context, String extend) {
        this.extend = extend == null ? "" : extend;
    }

    @Override
    public String homeContent(boolean filter) {
        return callFn("homeContent", filter ? "true" : "false");
    }

    @Override
    public String homeVideoContent() {
        return callFn("homeVideoContent", "");
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        return callFn("categoryContent",
                jsonStr(tid) + "," + jsonStr(pg) + "," + (filter ? "true" : "false") + ",{}");
    }

    @Override
    public String detailContent(List<String> ids) {
        String id = ids != null && ids.size() > 0 ? ids.get(0) : "";
        return callFn("detailContent", "[" + jsonStr(id) + "]");
    }

    @Override
    public String searchContent(String key, boolean quick) {
        return callFn("searchContent", jsonStr(key) + "," + (quick ? "true" : "false"));
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        return callFn("playerContent", jsonStr(flag) + "," + jsonStr(id) + ",[]");
    }

    @Override
    public void destroy() {
        try {
            if (ctx != null) ctx.destroy();
        } catch (Throwable ignore) {
        }
    }
}
