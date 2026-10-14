package com.zaka.xinghe.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zaka.xinghe.App;
import com.zaka.xinghe.model.Site;
import com.zaka.xinghe.net.CmsApi;
import com.zaka.xinghe.store.SiteStore;
import com.zaka.xinghe.util.Utils;

import java.util.List;

/** 启动页：先把当前接口预热好再进主界面。 */
public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(0xff14161c);

        TextView logo = new TextView(this);
        logo.setText("星河");
        logo.setTextSize(30f);
        logo.setTextColor(0xffff5f00);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo);

        TextView sub = new TextView(this);
        sub.setText("影视仓 · 纯自研复刻版");
        sub.setTextSize(12f);
        sub.setTextColor(0x80ffffff);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Utils.dp(8);
        sub.setLayoutParams(lp);
        root.addView(sub);

        setContentView(root);

        final Site site = SiteStore.current();
        if (site == null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    go();
                }
            }, 500);
            return;
        }
        App.run(new App.Task() {
            @Override
            public Object call() throws Exception {
                return CmsApi.getCates(site);
            }
        }, new App.Done() {
            @Override
            public void onDone(Object result, Exception err) {
                go();
            }
        });
        // 兜底：最多等 6 秒
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) go();
            }
        }, 6000);
    }

    private boolean went = false;

    private void go() {
        if (went || isFinishing()) return;
        went = true;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
