星河 · 影视仓复刻版 v1.2.1
=====================================

一、本版新增
1. QuickJS 原生 so 已打入工程
   - app/src/main/jniLibs/arm64-v8a/libquickjs-android-wrapper.so
   - Java 绑定：com.whl.quickjs.*
   - JsEngine 优先使用 QuickJS 执行爬虫脚本，并注入 req/http/log/md5

2. 播放页对接爬虫 playerContent
   - 非直链地址时，后台调用 SpiderApi.playerContent
   - 解析 TVBox 风格 JSON（url / playUrl）得到真实播放地址再交给 MediaPlayer

二、架构摘要
  spider/JsEngine.java     QuickJS 引擎
  spider/JarLoader.java    Jar 爬虫
  spider/PythonEngine.java Chaquopy（可选）
  net/SpiderApi.java       爬虫 JSON → 模型 + playerContent
  ui/PlayActivity.java     播放前解析
  jniLibs/arm64-v8a/       QuickJS so（仅 arm64）

三、编译
  Android Studio 打开本目录即可（jniLibs 已配置）。
  命令行：bash build/build.sh [--sign]
  当前仅打包 arm64-v8a；真机请用 64 位设备。

四、注意
  本应用只做聚合播放，接口由使用者配置。
