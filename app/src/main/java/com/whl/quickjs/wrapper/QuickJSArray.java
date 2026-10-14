package com.whl.quickjs.wrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/* loaded from: /tmp/apkwork/dex/classes6.dex */
public class QuickJSArray extends QuickJSObject implements JSArray {
    public QuickJSArray(QuickJSContext quickJSContext, long j) {
        super(quickJSContext, j);
    }

    @Override // com.whl.quickjs.wrapper.JSArray
    public Object get(int i) {
        checkRefCountIsZero();
        return getContext().get(this, i);
    }

    @Override // com.whl.quickjs.wrapper.JSArray
    public int length() {
        checkRefCountIsZero();
        return getContext().length(this);
    }

    @Override // com.whl.quickjs.wrapper.JSArray
    public void set(Object obj, int i) {
        checkRefCountIsZero();
        getContext().set(this, obj, i);
    }

    @Override // com.whl.quickjs.wrapper.QuickJSObject, com.whl.quickjs.wrapper.JSObject
    public ArrayList<Object> toArray() {
        return toArray(null);
    }

    @Override // com.whl.quickjs.wrapper.QuickJSObject, com.whl.quickjs.wrapper.JSObject
    public HashMap<String, Object> toMap() {
        return toMap(null);
    }

    @Override // com.whl.quickjs.wrapper.QuickJSObject, com.whl.quickjs.wrapper.JSObject
    public ArrayList<Object> toArray(MapFilter mapFilter) {
        return toArray(mapFilter, null, null);
    }

    @Override // com.whl.quickjs.wrapper.QuickJSObject, com.whl.quickjs.wrapper.JSObject
    public HashMap<String, Object> toMap(MapFilter mapFilter) {
        return (HashMap) toMap(mapFilter, null, null);
    }

    @Override // com.whl.quickjs.wrapper.QuickJSObject, com.whl.quickjs.wrapper.JSObject
    public ArrayList<Object> toArray(MapFilter mapFilter, Object obj, MapCreator mapCreator) {
        ArrayList<Object> arrayList = new ArrayList<>(length());
        HashMap<Long, Object> hashMap = new HashMap<>();
        convertToMap(this, arrayList, hashMap, mapFilter, obj, mapCreator);
        hashMap.clear();
        return arrayList;
    }

    @Override // com.whl.quickjs.wrapper.QuickJSObject, com.whl.quickjs.wrapper.JSObject
    public Map<String, Object> toMap(MapFilter mapFilter, Object obj, MapCreator mapCreator) {
        throw new UnsupportedOperationException("Array types are not yet supported for conversion to map. You should use toArray.");
    }
}
