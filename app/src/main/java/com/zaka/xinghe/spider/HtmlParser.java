package com.zaka.xinghe.spider;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简化版 TVBox 风格 HTML 解析（pdfa / pdfh / pd）。
 * 支持：tag、.class、#id、[attr=val]、&& 链式、:eq(n)、Text、Html、属性名。
 * 复杂 CSS 不完全等价 Jsoup，覆盖常见爬虫写法。
 */
public final class HtmlParser {

    private HtmlParser() {
    }

    /** 返回匹配节点 outerHTML 列表（JSON 数组用）。 */
    public static List<String> pdfa(String html, String rule) {
        List<String> out = new ArrayList<String>();
        if (html == null || rule == null) return out;
        try {
            List<Node> nodes = selectAll(html, rule);
            for (Node n : nodes) out.add(n.outer);
        } catch (Throwable ignore) {
        }
        return out;
    }

    /** 取第一个匹配：属性名 / Text / Html。 */
    public static String pdfh(String html, String rule) {
        if (html == null || rule == null) return "";
        try {
            String attr = null;
            String sel = rule;
            // rule 形如 body&&.item&&Text  或 a&&href
            String[] parts = rule.split("&&");
            if (parts.length >= 2) {
                String last = parts[parts.length - 1].trim();
                if (!last.contains(".") && !last.contains("#") && !last.contains("[")
                        && !last.contains(":") && !last.matches("(?i)[a-z0-9]+")) {
                    // Text / Html / 属性
                    attr = last;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < parts.length - 1; i++) {
                        if (i > 0) sb.append("&&");
                        sb.append(parts[i]);
                    }
                    sel = sb.toString();
                } else if (last.equalsIgnoreCase("Text") || last.equalsIgnoreCase("Html")
                        || last.equalsIgnoreCase("text") || last.equalsIgnoreCase("html")) {
                    attr = last;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < parts.length - 1; i++) {
                        if (i > 0) sb.append("&&");
                        sb.append(parts[i]);
                    }
                    sel = sb.toString();
                }
            } else if (rule.contains("&&")) {
                // handled above
            }
            // 也支持 "a,href" 风格
            if (attr == null && rule.contains(",")) {
                int c = rule.lastIndexOf(',');
                sel = rule.substring(0, c);
                attr = rule.substring(c + 1).trim();
            }
            List<Node> nodes = selectAll(html, sel);
            if (nodes.isEmpty()) return "";
            Node n = nodes.get(0);
            if (attr == null || attr.length() == 0 || "Html".equalsIgnoreCase(attr)) {
                return n.outer;
            }
            if ("Text".equalsIgnoreCase(attr) || "text".equals(attr)) {
                return n.text();
            }
            return n.attr(attr);
        } catch (Throwable e) {
            return "";
        }
    }

    public static String pd(String html, String rule, String baseUrl) {
        String v = pdfh(html, rule);
        if (v == null) v = "";
        v = v.trim();
        if (v.length() == 0) return "";
        if (baseUrl == null || baseUrl.length() == 0) return v;
        return joinUrl(baseUrl, v);
    }

    public static String joinUrl(String base, String rel) {
        if (rel == null || rel.length() == 0) return base == null ? "" : base;
        if (rel.startsWith("http://") || rel.startsWith("https://")
                || rel.startsWith("ftp://") || rel.startsWith("magnet:")
                || rel.startsWith("//")) {
            if (rel.startsWith("//") && base != null) {
                if (base.startsWith("https")) return "https:" + rel;
                return "http:" + rel;
            }
            return rel;
        }
        if (base == null || base.length() == 0) return rel;
        try {
            if (rel.startsWith("/")) {
                int scheme = base.indexOf("://");
                if (scheme > 0) {
                    int slash = base.indexOf('/', scheme + 3);
                    String origin = slash > 0 ? base.substring(0, slash) : base;
                    return origin + rel;
                }
                return rel;
            }
            String b = base;
            int q = b.indexOf('?');
            if (q > 0) b = b.substring(0, q);
            int slash = b.lastIndexOf('/');
            if (slash > b.indexOf("://") + 2) {
                b = b.substring(0, slash + 1);
            } else if (!b.endsWith("/")) {
                b = b + "/";
            }
            return b + rel;
        } catch (Throwable e) {
            return rel;
        }
    }

    // ---------------- internal ----------------

    private static List<Node> selectAll(String html, String rule) {
        List<Node> cur = new ArrayList<Node>();
        cur.add(new Node(html, html));
        if (rule == null || rule.trim().length() == 0) return cur;
        // 转 && 为分步；空格也近似分步
        String r = rule.trim();
        String[] steps;
        if (r.contains("&&")) {
            steps = r.split("&&");
        } else {
            steps = r.split("\\s+");
        }
        for (String step : steps) {
            step = step.trim();
            if (step.length() == 0) continue;
            int eq = -1;
            Matcher em = Pattern.compile(":eq\\((-?\\d+)\\)").matcher(step);
            if (em.find()) {
                eq = Integer.parseInt(em.group(1));
                step = step.substring(0, em.start()) + step.substring(em.end());
            }
            if (step.endsWith(":first")) {
                eq = 0;
                step = step.substring(0, step.length() - 6);
            }
            List<Node> next = new ArrayList<Node>();
            for (Node parent : cur) {
                next.addAll(matchIn(parent.inner != null ? parent.inner : parent.outer, step));
            }
            if (eq >= 0) {
                if (eq < next.size()) {
                    List<Node> one = new ArrayList<Node>();
                    one.add(next.get(eq));
                    next = one;
                } else if (eq < 0 && next.size() + eq >= 0) {
                    List<Node> one = new ArrayList<Node>();
                    one.add(next.get(next.size() + eq));
                    next = one;
                } else {
                    next = new ArrayList<Node>();
                }
            }
            cur = next;
            if (cur.isEmpty()) break;
        }
        return cur;
    }

    private static List<Node> matchIn(String html, String selector) {
        List<Node> out = new ArrayList<Node>();
        if (html == null) return out;
        selector = selector.trim();
        if (selector.length() == 0 || "*".equals(selector) || "html".equalsIgnoreCase(selector)
                || "body".equalsIgnoreCase(selector)) {
            // 粗略返回整段
            out.add(new Node(html, html));
            return out;
        }
        String tag = "[a-zA-Z][a-zA-Z0-9]*";
        String cls = null;
        String id = null;
        String attrName = null;
        String attrVal = null;
        String t = tag;

        // [attr=value]
        Matcher am = Pattern.compile("\\[([\\w-]+)(?:=['\"]?([^\\]'\"]*)['\"]?)?\\]").matcher(selector);
        if (am.find()) {
            attrName = am.group(1);
            attrVal = am.group(2);
            selector = selector.substring(0, am.start()) + selector.substring(am.end());
        }
        if (selector.contains(".")) {
            String[] p = selector.split("\\.", 2);
            t = p[0].length() == 0 ? tag : Pattern.quote(p[0]);
            cls = p[1].split("[#\\[]")[0];
        } else if (selector.contains("#")) {
            String[] p = selector.split("#", 2);
            t = p[0].length() == 0 ? tag : Pattern.quote(p[0]);
            id = p[1].split("[.\\[]")[0];
        } else if (selector.matches("[a-zA-Z][a-zA-Z0-9]*")) {
            t = Pattern.quote(selector);
        }

        Pattern p = Pattern.compile(
                "<(" + t + ")(\\s[^>]*)?>([\\s\\S]*?)</\\1>|<(" + t + ")(\\s[^>]*)?/>",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        while (m.find()) {
            String whole = m.group(0);
            String openAttrs = m.group(2) != null ? m.group(2) : (m.group(5) != null ? m.group(5) : "");
            String inner = m.group(3) != null ? m.group(3) : "";
            if (cls != null) {
                Matcher cm = Pattern.compile("class\\s*=\\s*['\"]([^'\"]*)['\"]", Pattern.CASE_INSENSITIVE)
                        .matcher(openAttrs);
                if (!cm.find()) continue;
                boolean ok = false;
                for (String c : cm.group(1).split("\\s+")) {
                    if (c.equals(cls)) {
                        ok = true;
                        break;
                    }
                }
                if (!ok) continue;
            }
            if (id != null) {
                Matcher im = Pattern.compile("id\\s*=\\s*['\"]([^'\"]*)['\"]", Pattern.CASE_INSENSITIVE)
                        .matcher(openAttrs);
                if (!im.find() || !id.equals(im.group(1))) continue;
            }
            if (attrName != null) {
                Matcher xm = Pattern.compile(
                        Pattern.quote(attrName) + "\\s*=\\s*['\"]([^'\"]*)['\"]",
                        Pattern.CASE_INSENSITIVE).matcher(openAttrs);
                if (!xm.find()) continue;
                if (attrVal != null && !attrVal.equals(xm.group(1))) continue;
            }
            out.add(new Node(whole, inner));
        }
        return out;
    }

    private static final class Node {
        final String outer;
        final String inner;

        Node(String outer, String inner) {
            this.outer = outer;
            this.inner = inner;
        }

        String text() {
            String s = inner != null ? inner : outer;
            s = s.replaceAll("(?is)<script[^>]*>[\\s\\S]*?</script>", "");
            s = s.replaceAll("(?is)<style[^>]*>[\\s\\S]*?</style>", "");
            s = s.replaceAll("<[^>]+>", " ");
            s = s.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
                    .replace("&gt;", ">").replace("&quot;", "\"");
            return s.replaceAll("\\s+", " ").trim();
        }

        String attr(String name) {
            Matcher m = Pattern.compile(
                    Pattern.quote(name) + "\\s*=\\s*['\"]([^'\"]*)['\"]",
                    Pattern.CASE_INSENSITIVE).matcher(outer);
            if (m.find()) return m.group(1);
            // 无引号
            m = Pattern.compile(
                    Pattern.quote(name) + "\\s*=\\s*([^\\s>]+)",
                    Pattern.CASE_INSENSITIVE).matcher(outer);
            if (m.find()) return m.group(1);
            return "";
        }
    }
}
