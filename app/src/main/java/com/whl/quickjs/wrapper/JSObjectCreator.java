package com.whl.quickjs.wrapper;

/* loaded from: /tmp/apkwork/dex/classes6.dex */
public interface JSObjectCreator {
    JSArray newArray(QuickJSContext quickJSContext, long j);

    JSFunction newFunction(QuickJSContext quickJSContext, long j, long j2, int i);

    JSObject newObject(QuickJSContext quickJSContext, long j);
}
