package com.zaka.xinghe.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import com.zaka.xinghe.net.Http;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 自研图片加载：内存缓存 + 线程池 + 采样解码 + 复用防错位。 */
public final class ImgLoader {

    private static LruCache<String, Bitmap> cache;
    private static ExecutorService pool;
    private static Handler ui;
    private static final Bitmap EMPTY = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);

    private ImgLoader() {
    }

    public static void init(android.content.Context ctx) {
        int max = (int) (Runtime.getRuntime().maxMemory() / 1024L / 8L);
        cache = new LruCache<String, Bitmap>(Math.max(max, 8 * 1024)) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value == null ? 0 : value.getByteCount() / 1024;
            }
        };
        pool = Executors.newFixedThreadPool(4);
        ui = new Handler(Looper.getMainLooper());
    }

    public static Bitmap cached(String url) {
        return cache == null || url == null ? null : cache.get(url);
    }

    public static void load(final ImageView iv, final String url) {
        if (iv == null) return;
        if (Utils.empty(url)) {
            iv.setTag(null);
            iv.setImageBitmap(null);
            return;
        }
        Bitmap b = cached(url);
        if (b != null) {
            iv.setTag(null);
            iv.setImageBitmap(b);
            return;
        }
        iv.setTag(url);
        pool.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = null;
                try {
                    int target = iv.getWidth() > 0 ? iv.getWidth() : Utils.dp(110);
                    byte[] data = Http.getBytes(url, Http.headers("Referer", url), 15000);
                    bmp = decode(data, target);
                    if (bmp != null) cache.put(url, bmp);
                } catch (Throwable ignore) {
                }
                final Bitmap fb = bmp;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        Object tag = iv.getTag();
                        if (tag != null && url.equals(tag) && fb != null && !fb.isRecycled()) {
                            iv.setImageBitmap(fb);
                        }
                    }
                });
            }
        });
    }

    private static Bitmap decode(byte[] data, int target) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            int w = o.outWidth;
            int h = o.outHeight;
            if (w <= 0 || h <= 0) return null;
            int scale = 1;
            while (w / scale > target * 2) {
                scale *= 2;
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            o2.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeByteArray(data, 0, data.length, o2);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 同步取一张图（详情页头图用）。 */
    public static Bitmap fetch(String url, int target) {
        if (Utils.empty(url)) return null;
        Bitmap b = cached(url);
        if (b != null) return b;
        try {
            byte[] data = Http.getBytes(url, Http.headers("Referer", url), 15000);
            Bitmap bmp = decode(data, target);
            if (bmp != null) cache.put(url, bmp);
            return bmp;
        } catch (Throwable t) {
            return null;
        }
    }
}
