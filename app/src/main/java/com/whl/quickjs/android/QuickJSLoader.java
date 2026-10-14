package com.whl.quickjs.android;

import com.whl.quickjs.wrapper.QuickJSContext;

/* loaded from: /tmp/apkwork/dex/classes6.dex */
public final class QuickJSLoader {

    /* loaded from: /tmp/apkwork/dex/classes6.dex */
    public static final class LogcatConsole implements QuickJSContext.Console {
        private final String tag;

        public LogcatConsole(String str) {
            this.tag = str;
        }

        @Override // com.whl.quickjs.wrapper.QuickJSContext.Console
        public void error(String str) {
        }

        @Override // com.whl.quickjs.wrapper.QuickJSContext.Console
        public void info(String str) {
        }

        @Override // com.whl.quickjs.wrapper.QuickJSContext.Console
        public void log(String str) {
        }

        @Override // com.whl.quickjs.wrapper.QuickJSContext.Console
        public void warn(String str) {
        }
    }

    public static void init() {
        System.loadLibrary("quickjs-android-wrapper");
    }

    @Deprecated
    public static void initConsoleLog(QuickJSContext quickJSContext) {
        initConsoleLog(quickJSContext, new LogcatConsole("quickjs"));
    }

    public static native void startRedirectingStdoutStderr(String str);

    @Deprecated
    public static void initConsoleLog(QuickJSContext quickJSContext, QuickJSContext.Console console) {
        quickJSContext.setConsole(console);
    }

    @Deprecated
    public static void initConsoleLog(QuickJSContext quickJSContext, String str) {
        initConsoleLog(quickJSContext, new LogcatConsole(str));
    }
}
