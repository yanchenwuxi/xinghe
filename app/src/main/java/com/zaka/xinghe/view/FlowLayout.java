package com.zaka.xinghe.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.zaka.xinghe.util.Utils;

import java.util.ArrayList;
import java.util.List;

/** 自动换行的流式布局（选集、筛选 chip、搜索历史都用它）。 */
public class FlowLayout extends ViewGroup {

    private int hGap = Utils.dp(8);
    private int vGap = Utils.dp(8);

    public FlowLayout(Context c) {
        super(c);
    }

    public FlowLayout(Context c, AttributeSet a) {
        super(c, a);
    }

    public void setGap(int h, int v) {
        this.hGap = h;
        this.vGap = v;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int maxW = widthMode == MeasureSpec.UNSPECIFIED ? Utils.screenW() : widthSize;

        int lineW = 0;
        int lineH = 0;
        int totalH = 0;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            int cw = child.getMeasuredWidth();
            int ch = child.getMeasuredHeight();
            if (lineW > 0 && lineW + hGap + cw > maxW) {
                totalH += lineH + vGap;
                lineW = cw;
                lineH = ch;
            } else {
                lineW = lineW == 0 ? cw : lineW + hGap + cw;
                lineH = Math.max(lineH, ch);
            }
        }
        totalH += lineH;
        setMeasuredDimension(maxW, totalH + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int maxW = r - l - getPaddingLeft() - getPaddingRight();
        int x = getPaddingLeft();
        int y = getPaddingTop();
        int lineH = 0;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            int cw = child.getMeasuredWidth();
            int ch = child.getMeasuredHeight();
            if (x > getPaddingLeft() && x + cw > getPaddingLeft() + maxW) {
                x = getPaddingLeft();
                y += lineH + vGap;
                lineH = 0;
            }
            child.layout(x, y, x + cw, y + ch);
            x += cw + hGap;
            lineH = Math.max(lineH, ch);
        }
    }

    public List<View> children() {
        List<View> out = new ArrayList<View>();
        for (int i = 0; i < getChildCount(); i++) out.add(getChildAt(i));
        return out;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p != null;
    }
}
