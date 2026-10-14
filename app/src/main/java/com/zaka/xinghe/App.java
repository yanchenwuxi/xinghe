package com.zaka.xinghe;

import android.app.Application;

import com.zaka.xinghe.store.DataStore;
import com.zaka.xinghe.store.SiteStore;
import com.zaka.xinghe.util.ImgLoader;
import com.zaka.xinghe.util.Utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 应用入口：全局单例都挂这儿。 */
public class App extends Application {

    private static App inst;
    private ExecutorService pool;

    @Override
    public void onCreate() {
        super.onCreate();
        inst = this;
        Utils.init(this);
        pool = Executors.newFixedThreadPool(6);
        SiteStore.init(this);
        DataStore.init(this);
        ImgLoader.init(this);
    }

    public static App get() {
        return inst;
    }

    public static ExecutorService pool() {
        return inst.pool;
    }

    /** 后台跑，主线程回调。 */
    public static void run(final Task task, final Done done) {
        inst.pool.execute(new Runnable() {
            @Override
            public void run() {
                Object result = null;
                Exception err = null;
                try {
                    result = task.call();
                } catch (Exception e) {
                    err = e;
                }
                final Object r = result;
                final Exception e2 = err;
                if (done != null) {
                    App.get().runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            done.onDone(r, e2);
                        }
                    });
                }
            }
        });
    }

    public void runOnUi(Runnable r) {
        android.os.Handler h = new android.os.Handler(getMainLooper());
        h.post(r);
    }

    public interface Task {
        Object call() throws Exception;
    }

    public interface Done {
        void onDone(Object result, Exception err);
    }
}
