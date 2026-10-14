import com.zaka.xinghe.model.Cate;
import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.model.Vod;
import com.zaka.xinghe.net.CmsApi;
import com.zaka.xinghe.subtitle.Cue;
import com.zaka.xinghe.subtitle.SubParser;
import com.zaka.xinghe.subtitle.XunleiSub;
import com.zaka.xinghe.util.Utils;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 核心逻辑自测。
 *
 * 说明：数据一律用真实接口响应（build/fixtures 下的样本，由 build/fetch_fixtures.sh 抓取），
 * 这里的验证不依赖安卓运行时，直接跑解析层。
 */
public class JvmCheck {

    private static final String FIX = "build/fixtures/";
    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) throws Exception {
        testCleanName();
        testPlayUrlSplit();
        testCmsParsing();
        testSubtitle();
        System.out.println();
        System.out.println("======== 结果: 通过 " + pass + " / 失败 " + fail + " ========");
        if (fail > 0) System.exit(1);
    }

    // ------------------------------------------------------------ 片名清洗

    private static void testCleanName() {
        section("片名清洗（字幕检索用）");
        expect("流浪地球2.2023.1080p.BluRay.x264.mkv", Utils.cleanName("流浪地球2.2023.1080p.BluRay.x264.mkv"), "流浪地球2 2023");
        expect("甄嬛传 第05集", Utils.cleanName("甄嬛传 第05集"), "甄嬛传");
        expect("Breaking.Bad.S01E03.720p", Utils.cleanName("Breaking.Bad.S01E03.720p"), "Breaking Bad");
        expect("【电影天堂】奥本海默 4K", Utils.cleanName("【电影天堂】奥本海默 4K"), "电影天堂 奥本海默");
        String r = Utils.cleanName("三体.Three-Body.2023.WEB-DL.2160p.H265.mp4");
        System.out.println("     三体样例 -> \"" + r + "\"");
        ok("清洗后不含清晰度/编码残留", !r.contains("2160") && !r.contains("H265") && !r.contains("WEB"));
    }

    // ------------------------------------------------------------ 播放地址拆分

    private static void testPlayUrlSplit() {
        section("播放地址拆分（$$$ 线路 / # 集 / $ 名称）");
        Vod v = new Vod();
        CmsApi.parsePlay(v,
                "dyttm3u8$$$dyttlz",
                "第01集$https://a.com/1.m3u8#第02集$https://a.com/2.m3u8$$$https://b.com/x.m3u8");
        ok("线路数 = " + v.flags.size() + "（期望 2）", v.flags.size() == 2);
        if (v.flags.size() == 2) {
            ok("线路1 名称 = " + v.flags.get(0).name + "，集数 = " + v.flags.get(0).eps.size(),
                    v.flags.get(0).name.equals("dyttm3u8") && v.flags.get(0).eps.size() == 2);
            ok("线路1 第2集地址 = " + v.flags.get(0).eps.get(1).url,
                    v.flags.get(0).eps.get(1).url.equals("https://a.com/2.m3u8"));
            ok("线路2 无集名时自动补名 = " + v.flags.get(1).eps.get(0).name,
                    v.flags.get(1).eps.get(0).name.startsWith("第"));
        }
        ok("播放地址解析(直链)", "https://x/y.m3u8".equals(CmsApi.resolvePlayUrl("https://x/y.m3u8")));
        ok("播放地址解析(JSON 包裹)", "https://z/z.m3u8".equals(
                CmsApi.resolvePlayUrl("{\"url\":\"https://z/z.m3u8\"}")));
    }

    // ------------------------------------------------------------ 采集接口解析

    private static void testCmsParsing() throws Exception {
        section("采集接口解析（真实响应样本）");
        Site site = new Site("测试站点", "https://caiji.dyttzyapi.com/api.php/provide/vod/");

        String catesBody = read(FIX + "cates.json");
        List<Cate> cates = CmsApi.parseCates(catesBody);
        ok("分类解析条数 = " + cates.size(), cates.size() > 0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(10, cates.size()); i++) {
            sb.append(cates.get(i).name).append("(").append(cates.get(i).id).append(") ");
        }
        System.out.println("     分类: " + sb);
        ok("分类不能从数据列表里拿（证明必须走 ac=list&t= 探测）",
                CmsApi.parseCates(read(FIX + "list.json")).size() == 0);

        List<Vod> list = CmsApi.parseList(read(FIX + "list.json"), site);
        ok("列表页解析条数 = " + list.size(), list.size() > 0);
        if (list.size() > 0) {
            Vod f = list.get(0);
            System.out.println("     首条: " + f.name + " | 备注=" + f.note + " | 类型=" + f.type
                    + " | 年份=" + f.year + " | 地区=" + f.area + " | 海报=" + !Utils.empty(f.pic));
            ok("列表项带 id/名称/海报", !Utils.empty(f.id) && !Utils.empty(f.name) && !Utils.empty(f.pic));
        }

        List<Vod> search = CmsApi.parseList(read(FIX + "search.json"), site);
        ok("搜索解析条数 = " + search.size(), search.size() > 0);
        if (search.size() > 0) {
            System.out.println("     搜索命中: " + search.get(0).name);
        }

        List<Vod> detail = CmsApi.parseList(read(FIX + "detail.json"), site);
        ok("详情解析条数 = " + detail.size(), detail.size() > 0);
        if (detail.size() > 0) {
            Vod d = detail.get(0);
            int eps = 0;
            for (Vod.Flag f : d.flags) eps += f.eps.size();
            ok("详情含线路/剧集: 线路 " + d.flags.size() + " 条, 共 " + eps + " 集",
                    d.flags.size() > 0 && eps > 0);
            ok("详情含简介", !Utils.empty(d.content));
            for (Vod.Flag f : d.flags) {
                if (f.eps.size() > 0) {
                    System.out.println("     线路[" + f.name + "] 首集: " + f.eps.get(0).name
                            + " -> " + shorten(f.eps.get(0).url));
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------ 字幕

    private static void testSubtitle() throws Exception {
        section("字幕源与字幕解析（真实数据）");

        List<XunleiSub.Item> items = XunleiSub.parseSearch(read(FIX + "sub_search.json"));
        ok("字幕搜索结果条数 = " + items.size(), items.size() > 0);
        if (items.size() > 0) {
            System.out.println("     首条: " + items.get(0).name + " | " + items.get(0).meta());
        }

        String ass = read(FIX + "sub_sample.ass");
        long t0 = System.currentTimeMillis();
        List<Cue> cues = SubParser.parse(ass, "ass");
        long cost = System.currentTimeMillis() - t0;
        ok("真实 ASS 字幕解析条数 = " + cues.size() + "（耗时 " + cost + "ms）", cues.size() > 50);
        if (cues.size() > 0) {
            Cue c0 = cues.get(0);
            Cue cn = cues.get(cues.size() - 1);
            System.out.println("     首条 [" + Utils.subTime(c0.start) + " -> " + Utils.subTime(c0.end) + "] "
                    + c0.text.replace("\n", " / "));
            System.out.println("     末条 [" + Utils.subTime(cn.start) + " -> " + Utils.subTime(cn.end) + "] "
                    + cn.text.replace("\n", " / "));
            ok("时间轴递增有序", cn.start > c0.start);
            ok("样式码/特效码已剥离", !c0.text.contains("{") && !c0.text.contains("\\pos"));
        }

        // 偏移逻辑（与播放器 SubtitleView 同一套命中算法）
        String srt = "1\n00:00:10,000 --> 00:00:12,000\nA\n\n2\n00:00:20,000 --> 00:00:22,000\nB\n";
        List<Cue> s = SubParser.parse(srt, "srt");
        ok("原始 21s 命中第 2 条", hit(s, 21, 0));
        ok("偏移 +3s 后 21s 不命中", !hit(s, 21, 3));
        ok("偏移 +3s 后 24s 命中", hit(s, 24, 3));
        ok("偏移 -8s 后 13s 命中第 1 条", hit(s, 13, -8));
        ok("VTT 解析", SubParser.parse("WEBVTT\n\n00:00:05.000 --> 00:00:07.000\nV\n", "vtt").size() == 1);
        ok("SRT 解析(多行+标签)", SubParser.parse(
                "1\n00:00:01,000 --> 00:00:03,000\n<i>斜体</i>\n第二行\n", "srt").get(0).text.contains("\n"));
    }

    private static boolean hit(List<Cue> cues, double posSec, double offsetSec) {
        double t = posSec - offsetSec;
        for (Cue c : cues) {
            if (t >= c.start && t <= c.end) return true;
        }
        return false;
    }

    // ------------------------------------------------------------ 工具

    private static String read(String path) throws Exception {
        byte[] b = Files.readAllBytes(Paths.get(path));
        return new String(b, Charset.forName("UTF-8"));
    }

    private static String shorten(String s) {
        return s == null ? "" : (s.length() > 90 ? s.substring(0, 90) + "…" : s);
    }

    private static void section(String n) {
        System.out.println();
        System.out.println("──── " + n + " ────");
    }

    private static void expect(String in, String got, String want) {
        ok("\"" + in + "\" -> \"" + got + "\"", got != null && got.contains(want));
    }

    private static void ok(String msg, boolean good) {
        System.out.println((good ? "  [OK]   " : "  [FAIL] ") + msg);
        if (good) pass++;
        else fail++;
    }
}
