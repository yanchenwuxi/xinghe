package com.zaka.xinghe.spider;

import android.content.Context;

import com.zaka.xinghe.net.Http;
import com.zaka.xinghe.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;

/**
 * 加载 TVBox jar 爬虫（csp_Xxx）。
 * jar 地址写在 site.api 或 site.jar / site.ext 中。
 */
public final class JarLoader {

    private static final Map<String, Spider> CACHE = new ConcurrentHashMap<String, Spider>();

    private JarLoader() {
    }

    public static Spider load(Context ctx, String key, String jarUrl, String className, String extend) throws Exception {
        String cacheKey = key + "#" + jarUrl + "#" + className;
        Spider cached = CACHE.get(cacheKey);
        if (cached != null) return cached;

        if (Utils.empty(jarUrl)) throw new RuntimeException("jar 地址为空");
        File dir = new File(ctx.getCacheDir(), "spider_jar");
        if (!dir.exists()) dir.mkdirs();
        String fileName = Utils.md5(jarUrl) + ".jar";
        File jarFile = new File(dir, fileName);
        if (!jarFile.exists() || jarFile.length() == 0) {
            byte[] data = Http.getBytes(jarUrl, null, 30000);
            if (data == null || data.length == 0) throw new RuntimeException("下载 jar 失败");
            FileOutputStream fos = new FileOutputStream(jarFile);
            try {
                fos.write(data);
            } finally {
                try { fos.close(); } catch (Throwable ignore) {}
            }
        }

        File opt = new File(dir, "opt_" + fileName);
        if (!opt.exists()) opt.mkdirs();
        DexClassLoader loader = new DexClassLoader(
                jarFile.getAbsolutePath(),
                opt.getAbsolutePath(),
                null,
                ctx.getClassLoader());

        // 常见包名尝试
        String[] tries = new String[]{
                className,
                "com.github.catvod.spider." + className,
                "com.github.catvod.crawler." + className,
                className.replace("csp_", "")
        };
        Class<?> cls = null;
        ClassNotFoundException last = null;
        for (String n : tries) {
            if (Utils.empty(n)) continue;
            try {
                cls = loader.loadClass(n);
                break;
            } catch (ClassNotFoundException e) {
                last = e;
            }
        }
        if (cls == null) {
            throw last != null ? last : new ClassNotFoundException(className);
        }
        Object inst;
        try {
            Constructor<?> c = cls.getDeclaredConstructor();
            c.setAccessible(true);
            inst = c.newInstance();
        } catch (NoSuchMethodException e) {
            inst = cls.newInstance();
        }
        if (!(inst instanceof Spider)) {
            // 兼容 catvod 原始 Spider（不同 classloader 下 instanceof 可能失败）
            // 用反射包装
            inst = new ReflectSpider(inst);
        }
        Spider spider = (Spider) inst;
        spider.siteKey = key;
        try {
            spider.init(ctx, extend == null ? "" : extend);
        } catch (Throwable t) {
            spider.init(ctx);
        }
        CACHE.put(cacheKey, spider);
        return spider;
    }

    public static void clear() {
        for (Spider s : CACHE.values()) {
            try { s.destroy(); } catch (Throwable ignore) {}
        }
        CACHE.clear();
    }
}
