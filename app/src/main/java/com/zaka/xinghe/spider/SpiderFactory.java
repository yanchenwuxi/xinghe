package com.zaka.xinghe.spider;

import android.content.Context;

import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.util.Utils;

/**
 * 根据站点 type / api / ext 创建对应 Spider。
 *
 * TVBox：type 3 = JS，type 4 = Jar(csp)，api 也可能直接是 .js URL。
 */
public final class SpiderFactory {

    private SpiderFactory() {
    }

    public static Spider create(Context ctx, Site site) throws Exception {
        if (site == null) throw new RuntimeException("site null");
        String type = site.type == null ? "" : site.type.toLowerCase();
        String api = site.api == null ? "" : site.api.trim();
        String ext = site.ext == null ? "" : site.ext.trim();
        String jar = site.jar == null ? "" : site.jar.trim();
        String base = site.baseUrl;

        // Python
        if ("python".equals(type) || api.endsWith(".py") || ext.endsWith(".py")) {
            return PythonEngine.create(ctx, api, ext);
        }

        // JS：type / typeCode / 扩展名
        boolean looksJs = "js".equals(type)
                || site.typeCode == 3
                || endsJs(api) || endsJs(ext)
                || (api.startsWith("csp_") && endsJs(ext));
        // 有些源 type=2 且 api 为 .js
        if (!looksJs && site.typeCode == 2 && endsJs(api)) looksJs = true;

        if (looksJs) {
            String js = pickJsSource(api, ext);
            if (Utils.empty(js)) {
                throw new RuntimeException("JS 站点未配置脚本地址: " + site.name);
            }
            return JsEngine.create(ctx, js, ext, base);
        }

        // Jar / csp
        if ("spider".equals(type) || site.typeCode == 4 || site.typeCode == 2
                || api.startsWith("csp_") || !Utils.empty(jar)) {
            String jarUrl = !Utils.empty(jar) ? jar : (ext.startsWith("http") ? ext : api);
            String className = api.startsWith("csp_") ? api : site.key;
            if (!className.startsWith("csp_")) {
                className = "csp_" + (site.name == null ? "X" : site.name.replaceAll("[^a-zA-Z0-9]", ""));
            }
            return JarLoader.load(ctx, site.key, jarUrl, className, ext);
        }

        // 兜底：api 含 function 当 JS
        if (api.contains("function") && (api.contains("homeContent") || api.contains("__jsEvalReturn"))) {
            return JsEngine.create(ctx, api, ext, base);
        }

        throw new RuntimeException("无法识别爬虫类型: type=" + type + " api=" + api);
    }

    private static boolean endsJs(String s) {
        if (s == null) return false;
        String x = s.toLowerCase();
        return x.endsWith(".js") || x.contains(".js?") || x.contains(".js&");
    }

    private static String pickJsSource(String api, String ext) {
        if (endsJs(api)) return api;
        if (endsJs(ext)) return ext;
        // csp_ + js in ext already handled; api 可能是相对路径 xxx.js 未带点场景
        if (!Utils.empty(api) && !api.startsWith("csp_")) return api;
        if (!Utils.empty(ext) && !ext.startsWith("{")) return ext;
        return api;
    }
}
