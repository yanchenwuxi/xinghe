package com.zaka.xinghe.ui;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** 内置浏览器（关于页 / 外链用）。 */
public class WebActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String url = getIntent().getStringExtra("url");
        WebView wv = new WebView(this);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.setWebViewClient(new WebViewClient());
        wv.setBackgroundColor(0xff1b1d24);
        setContentView(wv);
        if (url != null) wv.loadUrl(url);
    }
}
