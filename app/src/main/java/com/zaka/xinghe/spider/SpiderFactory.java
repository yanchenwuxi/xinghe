package com.zaka.xinghe.spider;

import android.content.Context;

import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.util.Utils;

/**
 * 根据 Site 类型创建对应 Spider 实现。
 * - jar / csp_Xxx → JarLoader
 * - js → JsEngine（WebView，可选 QuickJS）
 * - python → PythonEngine（需 Chaquopy）
 */
public final class SpiderFactory {

    private SpiderFactory() {
    }

    public static Spider create(Context ctx, Site site) throws Exception {
        if (site == null) throw new RuntimeException("站点为空");
        String type = site.type == null ? "cms" : site.type.toLowerCase();
        String api = site.api == null ? "" : site.api;
        String ext = site.ext == null ? "" : site.ext;
        String jar = site.jar == null ? "" : site.jar;

        if ("python".equals(type) || type.contains("py")) {
            return new PythonEngine(ctx, api, ext);
        }
        if ("js".equals(type) || api.endsWith(".js") || (ext.endsWith(".js") && Utils.empty(jar))) {
            String js = !Utils.empty(api) && (api.endsWith(".js") || api.contains("function")) ? api : ext;
            if (Utils.empty(js)) js = api;
            return JsEngine.create(ctx, js, ext);
        }
        // jar / spider
        String jarUrl = !Utils.empty(jar) ? jar : api;
        String className = site.key;
        // TVBox 常见：api 写成 csp_Xxx，jar 在 ext 或单独字段
        if (api.startsWith("csp_")) {
            className = api;
            if (Utils.empty(jar) && ext.startsWith("http")) jarUrl = ext;
        }
        // 从 key 解析类名
        if (className.startsWith("csp_")) {
            // keep
        } else if (api.startsWith("csp_")) {
            className = api;
        } else {
            className = "csp_" + site.name.replaceAll("[^a-zA-Z0-9]", "");
        }
        return JarLoader.load(ctx, site.key, jarUrl, className, ext);
    }
}
