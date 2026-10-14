package com.whl.quickjs.wrapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/* loaded from: /tmp/apkwork/dex/classes6.dex */
public class QuickJSObject implements JSObject {
    private final QuickJSContext context;
    private final long pointer;
    private int refCount;
    private Throwable stackTrace;

    public QuickJSObject(QuickJSContext quickJSContext, long j) {
        this.context = quickJSContext;
        this.pointer = j;
        this.refCount++;
    }

    public static /* synthetic */ Object lambda$setProperty$0(Method method, Object obj, Object[] objArr) {
        try {
            return method.invoke(obj, objArr);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        } catch (InvocationTargetException e2) {
            e2.printStackTrace();
            return null;
        }
    }

    private void setPropertyObject(String str, Object obj) {
        checkRefCountIsZero();
        this.context.setProperty(this, str, obj);
    }

    public final void checkRefCountIsZero() {
        if (!isRefCountZero()) {
        } else {
            throw new QuickJSException("The call threw an exception, the reference count of the current object has already reached zero.");
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:56:0x004a, code lost:
    
        if (r10.shouldSkipKey(r3, r17.pointer, r22) != false) goto L82;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void convertToMap(Object key, Object container, java.util.HashMap<Long, Object> seen, MapFilter filter, Object extra, MapCreator creator) {
        // simplified: conversion to Map not fully ported; callers should prefer stringify()
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public void decrementRefCount() {
        checkRefCountIsZero();
        this.refCount--;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public Boolean getBoolean(String str) {
        Object property = getProperty(str);
        if (property instanceof Boolean) {
            return (Boolean) property;
        }
        return null;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public Boolean getBooleanProperty(String str) {
        return getBoolean(str);
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public byte[] getBytes(String str) {
        Object property = getProperty(str);
        if (property instanceof byte[]) {
            return (byte[]) property;
        }
        return null;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public QuickJSContext getContext() {
        return this.context;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public Double getDouble(String str) {
        Object property = getProperty(str);
        if (property instanceof Double) {
            return (Double) property;
        }
        return null;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public Double getDoubleProperty(String str) {
        return getDouble(str);
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public Integer getIntProperty(String str) {
        return getInteger(str);
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public Integer getInteger(String str) {
        Object property = getProperty(str);
        if (property instanceof Integer) {
            return (Integer) property;
        }
        return null;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public JSArray getJSArray(String str) {
        Object property = getProperty(str);
        if (property instanceof JSArray) {
            return (JSArray) property;
        }
        return null;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public JSArray getJSArrayProperty(String str) {
        return getJSArray(str);
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public JSFunction getJSFunction(String str) {
        Object property = getProperty(str);
        if (property instanceof JSFunction) {
            return (JSFunction) property;
        }
        return null;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public JSFunction getJSFunctionProperty(String str) {
        return getJSFunction(str);
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public JSObject getJSObject(String str) {
        Object property = getProperty(str);
        if (property instanceof JSObject) {
            return (JSObject) property;
        }
        return null;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public JSObject getJSObjectProperty(String str) {
        return getJSObject(str);
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public Long getLong(String str) {
        Object property = getProperty(str);
        if (property instanceof Long) {
            return (Long) property;
        }
        return null;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public JSArray getNames() {
        checkRefCountIsZero();
        return (JSArray) this.context.getOwnPropertyNames(this);
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public JSArray getOwnPropertyNames() {
        return getNames();
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public long getPointer() {
        return this.pointer;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public Object getProperty(String str) {
        checkRefCountIsZero();
        return this.context.getProperty(this, str);
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public int getRefCount() {
        return this.refCount;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public Throwable getStackTrace() {
        return this.stackTrace;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public String getString(String str) {
        Object property = getProperty(str);
        if (property instanceof String) {
            return (String) property;
        }
        return null;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public String getStringProperty(String str) {
        return getString(str);
    }

    public int hashCode() {
        return Arrays.hashCode(new long[]{this.pointer});
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public void hold() {
        checkRefCountIsZero();
        this.refCount++;
        this.context.hold(this);
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public boolean isAlive() {
        return !isRefCountZero();
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public boolean isRefCountZero() {
        if (this.refCount == 0) {
            return true;
        }
        return false;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public void release() {
        if (isRefCountZero()) {
            return;
        }
        this.refCount--;
        this.context.freeValue(this);
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public void setProperty(String str, String str2) {
        setPropertyObject(str, str2);
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public void setStackTrace(Throwable th) {
        this.stackTrace = th;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public String stringify() {
        checkRefCountIsZero();
        return this.context.stringify(this);
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public ArrayList<Object> toArray() {
        return toArray(null);
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public HashMap<String, Object> toMap() {
        return toMap(null);
    }

    public String toString() {
        checkRefCountIsZero();
        JSFunction jSFunction = getJSFunction("toString");
        String str = (String) jSFunction.call(new Object[0]);
        jSFunction.release();
        return str;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public void setProperty(String str, int i) {
        setPropertyObject(str, Integer.valueOf(i));
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public ArrayList<Object> toArray(MapFilter mapFilter, Object obj, MapCreator mapCreator) {
        throw new UnsupportedOperationException("Object types are not yet supported for conversion to array. You should use toMap.");
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public HashMap<String, Object> toMap(MapFilter mapFilter) {
        return (HashMap) toMap(mapFilter, null, new c(10));
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public void setProperty(String str, long j) {
        setPropertyObject(str, Long.valueOf(j));
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public ArrayList<Object> toArray(MapFilter mapFilter) {
        return toArray(mapFilter, null, null);
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public Map<String, Object> toMap(MapFilter mapFilter, Object obj, MapCreator mapCreator) {
        Map<String, Object> map = mapCreator.get();
        HashMap<Long, Object> hashMap = new HashMap<>();
        convertToMap(this, map, hashMap, mapFilter, obj, mapCreator);
        hashMap.clear();
        return map;
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public void setProperty(String str, JSObject jSObject) {
        setPropertyObject(str, jSObject);
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public void setProperty(String str, boolean z) {
        setPropertyObject(str, Boolean.valueOf(z));
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public void setProperty(String str, double d) {
        setPropertyObject(str, Double.valueOf(d));
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public void setProperty(String str, byte[] bArr) {
        setPropertyObject(str, bArr);
    }

    @Override // com.whl.quickjs.wrapper.JSObject
    public void setProperty(String str, JSCallFunction jSCallFunction) {
        setPropertyObject(str, jSCallFunction);
    }

    /* JADX WARN: Removed duplicated region for block: B:16:0x003f  */
    /* JADX WARN: Removed duplicated region for block: B:4:0x0011  */
    @Override // com.whl.quickjs.wrapper.JSObject
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void setProperty(String name, Class<?> clazz) {
        try {
            Object instance = clazz.newInstance();
            JSObject obj = this.context.createNewJSObject();
            for (java.lang.reflect.Method m : clazz.getMethods()) {
                if (m.isAnnotationPresent(JSMethod.class)) {
                    final java.lang.reflect.Method method = m;
                    final Object target = instance;
                    obj.setProperty(m.getName(), new JSCallFunction() {
                        @Override
                        public Object call(Object... args) {
                            try {
                                Class<?>[] pts = method.getParameterTypes();
                                Object[] invokeArgs = new Object[pts.length];
                                for (int i = 0; i < pts.length; i++) {
                                    invokeArgs[i] = (args != null && i < args.length) ? args[i] : null;
                                }
                                return method.invoke(target, invokeArgs);
                            } catch (Throwable e) {
                                throw new QuickJSException(String.valueOf(e.getMessage()));
                            }
                        }
                    });
                }
            }
            setProperty(name, obj);
        } catch (Exception e) {
            throw new QuickJSException("bind class failed: " + e.getMessage());
        }
    }
}
