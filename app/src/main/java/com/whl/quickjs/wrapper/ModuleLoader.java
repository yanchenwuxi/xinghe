package com.whl.quickjs.wrapper;

/* loaded from: /tmp/apkwork/dex/classes6.dex */
public abstract class ModuleLoader {
    public abstract byte[] getModuleBytecode(String str);

    public abstract String getModuleStringCode(String str);

    public abstract boolean isBytecodeMode();

    public String moduleNormalizeName(String str, String str2) {
        return str2;
    }
}
