# 星河 (XingHe)

影视仓风格的安卓影视聚合客户端（纯自研 UI + CMS/XML + JS/Jar 爬虫扩展）。

## 功能概览

- 主页 / 直播 / 短剧 / 历史收藏 / 我的
- TVBox 风格配置导入（sites + parses + lives）
- 配置弹窗：名称 + 地址 + **选择本地文件**（SAF）
- JS 爬虫：内置 **QuickJS** 原生 so（arm64）
- Jar 爬虫：DexClassLoader
- Python：可选 Chaquopy
- 播放页对接爬虫 `playerContent` 解析真实地址

## 编译

### Android Studio

打开本仓库根目录，Sync 后 Build → Build APK(s)。

### 命令行

```bash
# 需要 Android SDK + JDK 17
./gradlew assembleDebug
# 或
bash build/build.sh
```

## CI

GitHub Actions：`.github/workflows/android.yml`，push 后自动 `assembleDebug` 并上传 APK 产物。

## 包名

`com.zaka.xinghe` · version 1.2.1
