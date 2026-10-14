package com.whl.quickjs.wrapper;

import java.util.ArrayList;
import java.util.HashMap;

/* loaded from: /tmp/apkwork/dex/classes6.dex */
public class QuickJSFunction extends QuickJSObject implements JSFunction {
    private Status currentStatus;
    private int stashTimes;
    private final long thisPointer;
    private final int thisPointerTag;

    /* loaded from: /tmp/apkwork/dex/classes6.dex */
    public enum Status {
        NOT_CALLED,
        CALLING,
        CALLED
    }

    public QuickJSFunction(QuickJSContext quickJSContext, long j, long j2, int i) {
        super(quickJSContext, j);
        this.stashTimes = 0;
        this.currentStatus = Status.NOT_CALLED;
        this.thisPointer = j2;
        this.thisPointerTag = i;
    }

    @Override // com.whl.quickjs.wrapper.JSFunction
    public Object call(Object... objArr) {
        checkRefCountIsZero();
        this.currentStatus = Status.CALLING;
        try {
            return getContext().call(this, this.thisPointer, this.thisPointerTag, objArr);
        } finally {
            this.currentStatus = Status.CALLED;
            if (this.stashTimes > 0) {
                for (int i = 0; i < this.stashTimes; i++) {
                    release();
                }
                this.stashTimes = 0;
            }
        }
    }

    @Override // com.whl.quickjs.wrapper.JSFunction
    public void callVoid(Object... objArr) {
        Object call = call(objArr);
        if (call instanceof JSObject) {
            ((JSObject) call).release();
        }
    }

    @Override // com.whl.quickjs.wrapper.QuickJSObject, com.whl.quickjs.wrapper.JSObject
    public void release() {
        if (this.currentStatus == Status.CALLING) {
            this.stashTimes++;
        } else {
            super.release();
        }
    }

    @Override // com.whl.quickjs.wrapper.QuickJSObject, com.whl.quickjs.wrapper.JSObject
    public ArrayList<Object> toArray() {
        throw new UnsupportedOperationException("JSFunction types do not support conversion to map or array.");
    }

    @Override // com.whl.quickjs.wrapper.QuickJSObject, com.whl.quickjs.wrapper.JSObject
    public HashMap<String, Object> toMap() {
        throw new UnsupportedOperationException("JSFunction types do not support conversion to map or array.");
    }
}
