import com.zaka.xinghe.net.Http;
import com.zaka.xinghe.util.Utils;

/** 接口诊断：逐条打印探测请求的真实返回，定位解析失败在哪一步。 */
public class Diag {

    public static void main(String[] args) throws Exception {
        String base = args.length > 0 ? args[0] : "https://caiji.dyttzyapi.com/api.php/provide/vod/";
        String[] probes = {"ac=list&t=1&pg=1", "ac=detail&pg=1", "ac=list"};
        for (String p : probes) {
            String url = Utils.join(base, p);
            System.out.println("---- " + url);
            try {
                String body = Http.get(url);
                System.out.println("     长度=" + body.length());
                System.out.println("     头部: " + body.substring(0, Math.min(200, body.length())).replace("\n", " "));
                int ci = body.indexOf("\"class\"");
                System.out.println("     含 class 字段: " + (ci >= 0));
            } catch (Throwable t) {
                System.out.println("     ERROR " + t);
            }
        }
    }
}
