package com.whl.quickjs.wrapper;

/* loaded from: /tmp/apkwork/dex/classes6.dex */
public class QuickJSException extends RuntimeException {
    private final boolean jsError;

    public QuickJSException(String str) {
        this(str, false);
    }

    public boolean isJSError() {
        return this.jsError;
    }

    public QuickJSException(String str, boolean z) {
        super(str);
        this.jsError = z;
    }
}
