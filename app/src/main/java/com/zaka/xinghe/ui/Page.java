package com.zaka.xinghe.ui;

import android.content.Context;
import android.widget.FrameLayout;

/** 页面基类：五个 Tab 页都继承它。 */
public abstract class Page extends FrameLayout {

    protected boolean inited = false;
    protected boolean visible = false;

    public Page(Context c) {
        super(c);
        setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    /** 页面首次可见时调用一次。 */
    public void onShow() {
        visible = true;
        if (!inited) {
            inited = true;
            onInit();
        } else {
            onReShow();
        }
    }

    public void onHide() {
        visible = false;
    }

    protected abstract void onInit();

    protected void onReShow() {
    }

    /** 当前站点变了，页面需要重新加载。 */
    public void onSiteChanged() {
        if (inited) reload();
    }

    protected void reload() {
    }
}
