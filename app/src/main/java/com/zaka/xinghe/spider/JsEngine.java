package com.zaka.xinghe.spider;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.whl.quickjs.android.QuickJSLoader;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSCallFunction;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;
import com.zaka.xinghe.net.Http;
import com.zaka.xinghe.util.Utils;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * QuickJS 爬虫引擎（对齐 TVBox type=3 JS 源）。
 *
 * - 单线程执行，满足 QuickJSContext 同线程约束
 * - 注入 req / pdfa / pdfh / pd / joinUrl / md5 / aesX / log 等常用全局
 * - 支持 http(s) 脚本地址与相对路径（配合 baseUrl）
 */
public final class JsEngine extends Spider {

    private static final String TAG = "JsEngine";
    private static boolean sLibLoaded = false;
    private static final Object LIB_LOCK = new Object();

    private final ExecutorService worker;
    private QuickJSContext ctx;
    private final String jsSource;
    private String extend = "";
    private volatile boolean destroyed = false;

    public static void ensureNativeLoaded() {
        synchronized (LIB_LOCK) {
            if (sLibLoaded) return;
            try {
                QuickJSLoader.init();
                sLibLoaded = true;
            } catch (Throwable t) {
                Log.e(TAG, "load quickjs failed", t);
                throw new RuntimeException("加载 QuickJS 原生库失败: " + t.getMessage(), t);
            }
        }
    }

    public JsEngine(Context appCtx, String jsCodeOrUrl, String extend, String baseUrl) throws Exception {
        ensureNativeLoaded();
        this.extend = extend == null ? "" : extend;
        if (Utils.empty(jsCodeOrUrl)) throw new RuntimeException("JS 源为空");
        this.jsSource = loadScript(jsCodeOrUrl, baseUrl);
        if (Utils.empty(this.jsSource)) throw new RuntimeException("JS 脚本内容为空: " + jsCodeOrUrl);

        this.worker = Executors.newSingleThreadExecutor(new java.util.concurrent.ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "js-spider");
                t.setDaemon(true);
                return t;
            }
        });

        // 在同一 worker 线程创建 context 并 evaluate
        runOnWorker(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                ctx = QuickJSContext.create();
                injectGlobals(ctx);
                ctx.evaluate(jsSource, "spider.js");
                try {
                    String extArg = toJsString(extend);
                    ctx.evaluate(
                            "try{ if (typeof init === 'function') { init(" + extArg + "); }"
                                    + " else if (typeof __jsEvalReturn === 'function') { var __s=__jsEvalReturn();"
                                    + " if(__s&&typeof __s.init==='function') __s.init(" + extArg + "); }"
                                    + "}catch(e){ }",
                            "init.js");
                } catch (Throwable e) {
                    Log.w(TAG, "init: " + e.getMessage());
                }
                return null;
            }
        });
    }

    public static Spider create(Context ctx, String js, String extend) throws Exception {
        return new JsEngine(ctx.getApplicationContext(), js, extend, null);
    }

    public static Spider create(Context ctx, String js, String extend, String baseUrl) throws Exception {
        return new JsEngine(ctx.getApplicationContext(), js, extend, baseUrl);
    }

    private static String loadScript(String src, String baseUrl) throws Exception {
        String s = src.trim();
        // 已是源码
        if (s.contains("function") && (s.contains("homeContent") || s.contains("__jsEvalReturn")
                || s.contains("playerContent") || s.contains("categoryContent"))) {
            return s;
        }
        if (s.startsWith("http://") || s.startsWith("https://")) {
            return Http.get(s);
        }
        // 相对路径
        if (baseUrl != null && (baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
            String abs = HtmlParser.joinUrl(baseUrl, s);
            if (abs.startsWith("http")) return Http.get(abs);
        }
        // file
        if (s.startsWith("/") || s.startsWith("file:")) {
            String path = s.startsWith("file:") ? s.substring(5) : s;
            return readFile(path);
        }
        return s;
    }

    private static String readFile(String path) throws Exception {
        java.io.File f = new java.io.File(path);
        byte[] data = new byte[(int) f.length()];
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        in.read(data);
        in.close();
        return Http.decode(data);
    }

    private void injectGlobals(final QuickJSContext qjs) {
        JSObject global = qjs.getGlobalObject();

        global.setProperty("req", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                return doReq(qjs, args);
            }
        });
        global.setProperty("_http", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                return doReq(qjs, args);
            }
        });
        global.setProperty("http", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                try {
                    String url = argStr(args, 0);
                    return Http.get(url);
                } catch (Throwable e) {
                    return "";
                }
            }
        });
        global.setProperty("pdfa", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                String html = argStr(args, 0);
                String rule = argStr(args, 1);
                List<String> list = HtmlParser.pdfa(html, rule);
                JSArray arr = qjs.createNewJSArray();
                for (int i = 0; i < list.size(); i++) {
                    arr.set(list.get(i), i);
                }
                return arr;
            }
        });
        global.setProperty("pdfh", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                return HtmlParser.pdfh(argStr(args, 0), argStr(args, 1));
            }
        });
        global.setProperty("pd", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                return HtmlParser.pd(argStr(args, 0), argStr(args, 1), argStr(args, 2));
            }
        });
        global.setProperty("joinUrl", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                return HtmlParser.joinUrl(argStr(args, 0), argStr(args, 1));
            }
        });
        global.setProperty("md5", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                return Utils.md5(argStr(args, 0));
            }
        });
        global.setProperty("md5X", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                return Utils.md5(argStr(args, 0));
            }
        });
        global.setProperty("log", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                if (args != null) {
                    StringBuilder sb = new StringBuilder();
                    for (Object a : args) sb.append(a).append(' ');
                    Log.d(TAG, sb.toString());
                }
                return null;
            }
        });
        global.setProperty("aesX", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                // aesX(data, isDecrypt, key, ivIsHex?, iv, keyIsHex?, resultBase64?)
                try {
                    return aesX(argStr(args, 0), argBool(args, 1), argStr(args, 2),
                            argBool(args, 3), argStr(args, 4), argBool(args, 5), argBool(args, 6));
                } catch (Throwable e) {
                    Log.e(TAG, "aesX", e);
                    return "";
                }
            }
        });
        global.setProperty("base64Encode", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                return Base64.encodeToString(argStr(args, 0).getBytes(), Base64.NO_WRAP);
            }
        });
        global.setProperty("base64Decode", new JSCallFunction() {
            @Override
            public Object call(Object... args) {
                try {
                    return new String(Base64.decode(argStr(args, 0), Base64.DEFAULT));
                } catch (Throwable e) {
                    return "";
                }
            }
        });
        // console
        try {
            JSObject console = qjs.createNewJSObject();
            JSCallFunction clog = new JSCallFunction() {
                @Override
                public Object call(Object... args) {
                    Log.d(TAG, String.valueOf(args != null && args.length > 0 ? args[0] : ""));
                    return null;
                }
            };
            console.setProperty("log", clog);
            console.setProperty("info", clog);
            console.setProperty("warn", clog);
            console.setProperty("error", clog);
            global.setProperty("console", console);
        } catch (Throwable ignore) {
        }
    }

    private Object doReq(QuickJSContext qjs, Object[] args) {
        try {
            String url = argStr(args, 0);
            String method = "GET";
            String body = null;
            Map<String, String> headers = new HashMap<String, String>();
            if (args != null && args.length > 1 && args[1] instanceof JSObject) {
                JSObject opt = (JSObject) args[1];
                Object m = opt.getProperty("method");
                if (m != null) method = String.valueOf(m);
                Object d = opt.getProperty("data");
                if (d != null) body = String.valueOf(d);
                Object b = opt.getProperty("body");
                if (b != null) body = String.valueOf(b);
                Object h = opt.getProperty("headers");
                if (h instanceof JSObject) {
                    // 无法枚举全部 key 时忽略；常见 UA
                    JSObject ho = (JSObject) h;
                    Object ua = ho.getProperty("User-Agent");
                    if (ua == null) ua = ho.getProperty("user-agent");
                    if (ua != null) headers.put("User-Agent", String.valueOf(ua));
                    Object ref = ho.getProperty("Referer");
                    if (ref == null) ref = ho.getProperty("referer");
                    if (ref != null) headers.put("Referer", String.valueOf(ref));
                    Object ct = ho.getProperty("Content-Type");
                    if (ct != null) headers.put("Content-Type", String.valueOf(ct));
                    Object ck = ho.getProperty("Cookie");
                    if (ck != null) headers.put("Cookie", String.valueOf(ck));
                }
            }
            String content;
            if ("POST".equalsIgnoreCase(method)) {
                content = Http.post(url, body, headers);
            } else {
                content = Http.get(url, headers, 20000);
            }
            JSObject res = qjs.createNewJSObject();
            res.setProperty("code", 200);
            res.setProperty("content", content == null ? "" : content);
            return res;
        } catch (Throwable e) {
            Log.e(TAG, "req", e);
            JSObject res = qjs.createNewJSObject();
            res.setProperty("code", 500);
            res.setProperty("content", e.getMessage() == null ? "" : e.getMessage());
            return res;
        }
    }

    private static String aesX(String data, boolean decrypt, String key, boolean keyHex,
                               String iv, boolean ivHex, boolean outBase64) throws Exception {
        if (data == null) return "";
        byte[] keyBytes = keyHex ? hexToBytes(key) : key.getBytes(Charset.forName("UTF-8"));
        byte[] ivBytes = (iv == null || iv.length() == 0)
                ? new byte[16]
                : (ivHex ? hexToBytes(iv) : iv.getBytes(Charset.forName("UTF-8")));
        // 对齐 key 长度 16/24/32
        keyBytes = fitKey(keyBytes);
        String trans = (iv != null && iv.length() > 0) ? "AES/CBC/PKCS5Padding" : "AES/ECB/PKCS5Padding";
        Cipher cipher = Cipher.getInstance(trans);
        SecretKeySpec ks = new SecretKeySpec(keyBytes, "AES");
        if (trans.contains("CBC")) {
            cipher.init(decrypt ? Cipher.DECRYPT_MODE : Cipher.ENCRYPT_MODE, ks, new IvParameterSpec(fitIv(ivBytes)));
        } else {
            cipher.init(decrypt ? Cipher.DECRYPT_MODE : Cipher.ENCRYPT_MODE, ks);
        }
        byte[] input;
        if (decrypt) {
            try {
                input = Base64.decode(data, Base64.DEFAULT);
            } catch (Throwable e) {
                input = hexToBytes(data);
            }
            byte[] out = cipher.doFinal(input);
            return new String(out, Charset.forName("UTF-8"));
        } else {
            byte[] out = cipher.doFinal(data.getBytes(Charset.forName("UTF-8")));
            if (outBase64) return Base64.encodeToString(out, Base64.NO_WRAP);
            return bytesToHex(out);
        }
    }

    private static byte[] fitKey(byte[] k) {
        int n = k.length;
        if (n == 16 || n == 24 || n == 32) return k;
        byte[] o = new byte[16];
        System.arraycopy(k, 0, o, 0, Math.min(n, 16));
        return o;
    }

    private static byte[] fitIv(byte[] iv) {
        byte[] o = new byte[16];
        System.arraycopy(iv, 0, o, 0, Math.min(iv.length, 16));
        return o;
    }

    private static byte[] hexToBytes(String hex) {
        if (hex == null) return new byte[0];
        String s = hex.trim();
        if (s.length() % 2 != 0) s = "0" + s;
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    private static String bytesToHex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte value : b) {
            sb.append(String.format("%02x", value & 0xff));
        }
        return sb.toString();
    }

    private String callFn(String name, String argsExpr) {
        if (destroyed) return "";
        try {
            final String fname = name;
            final String fargs = argsExpr;
            return runOnWorker(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    String script =
                            "(function(){ try {"
                                    + " var fn = null;"
                                    + " if (typeof " + fname + " === 'function') fn = " + fname + ";"
                                    + " else if (typeof __jsEvalReturn === 'function') {"
                                    + "   var o = __jsEvalReturn(); if (o && typeof o." + fname + " === 'function') fn = o." + fname + ".bind(o);"
                                    + " }"
                                    + " if (!fn) return 'ERR: function " + fname + " not found';"
                                    + " var r = fn(" + fargs + ");"
                                    + " if (r == null || r === undefined) return '';"
                                    + " if (typeof r === 'string') return r;"
                                    + " return JSON.stringify(r);"
                                    + "} catch(e) { return 'ERR:' + (e && e.message ? e.message : e); } })()";
                    Object r = ctx.evaluate(script, "call_" + fname + ".js");
                    return r == null ? "" : String.valueOf(r);
                }
            });
        } catch (Throwable e) {
            Log.e(TAG, "call " + name, e);
            return "ERR:" + e.getMessage();
        }
    }

    private <T> T runOnWorker(Callable<T> c) throws Exception {
        if (Thread.currentThread().getName().startsWith("js-spider")) {
            return c.call();
        }
        Future<T> f = worker.submit(c);
        return f.get(60, TimeUnit.SECONDS);
    }

    private static String argStr(Object[] args, int i) {
        if (args == null || i >= args.length || args[i] == null) return "";
        return String.valueOf(args[i]);
    }

    private static boolean argBool(Object[] args, int i) {
        if (args == null || i >= args.length || args[i] == null) return false;
        Object a = args[i];
        if (a instanceof Boolean) return (Boolean) a;
        String s = String.valueOf(a);
        return "1".equals(s) || "true".equalsIgnoreCase(s);
    }

    private static String toJsString(String s) {
        if (s == null) return "''";
        // 若已是 JSON 对象/数组，直接当 JS 字面量
        String t = s.trim();
        if ((t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"))) {
            return t;
        }
        return "'" + t.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "") + "'";
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
        String extJson = "{}";
        if (extend != null && !extend.isEmpty()) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, String> e : extend.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(e.getKey()).append("\":\"").append(
                        e.getValue() == null ? "" : e.getValue().replace("\"", "\\\"")).append('"');
            }
            sb.append('}');
            extJson = sb.toString();
        }
        return callFn("categoryContent",
                toJsString(tid) + "," + toJsString(pg) + "," + (filter ? "true" : "false") + "," + extJson);
    }

    @Override
    public String detailContent(List<String> ids) {
        String id = ids != null && ids.size() > 0 ? ids.get(0) : "";
        return callFn("detailContent", "[" + toJsString(id) + "]");
    }

    @Override
    public String searchContent(String key, boolean quick) {
        return callFn("searchContent", toJsString(key) + "," + (quick ? "true" : "false"));
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        return callFn("playerContent", toJsString(flag) + "," + toJsString(id) + ",[]");
    }

    @Override
    public void destroy() {
        destroyed = true;
        try {
            runOnWorker(new Callable<Void>() {
                @Override
                public Void call() {
                    if (ctx != null) {
                        ctx.destroy();
                        ctx = null;
                    }
                    return null;
                }
            });
        } catch (Throwable ignore) {
        }
        worker.shutdownNow();
    }
}
