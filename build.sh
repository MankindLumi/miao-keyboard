#!/usr/bin/env bash
# ============================================================
#  喵喵输入法 (MiaoKeyboard) 一键构建脚本
#  用法：
#    ./build.sh           # 构建 debug APK
#    ./build.sh release   # 构建 release APK（未签名）
#    ./build.sh clean     # 清理后构建 debug
# ============================================================
set -e

cd "$(dirname "$0")"

MODE="${1:-debug}"

echo "🐱 ========== 喵喵输入法构建 =========="

# ---------- 1. 检查 Java ----------
if ! command -v java >/dev/null 2>&1; then
    echo "❌ 未检测到 Java，请安装 JDK 17 及以上版本。"
    exit 1
fi
JAVA_VER=$(java -version 2>&1 | head -n1)
echo "✅ Java: $JAVA_VER"

# ---------- 2. 检查 Android SDK ----------
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    if [ ! -f local.properties ]; then
        echo ""
        echo "⚠️  未找到 local.properties，也检测不到 ANDROID_HOME。"
        echo "   请指定 Android SDK 路径，例如："
        echo "     echo 'sdk.dir=/你的/Android/Sdk路径' > local.properties"
        echo "   （或设置环境变量 ANDROID_HOME）"
        exit 1
    fi
fi
echo "✅ Android SDK 已配置"

# ---------- 3. 确保 gradle wrapper 可用 ----------
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo ""
    echo "⚠️  缺少 gradle-wrapper.jar（二进制文件，无法纯文本生成）。"
    if command -v gradle >/dev/null 2>&1; then
        echo "   检测到系统 gradle，正在自动生成 wrapper..."
        gradle wrapper --gradle-version 8.6
    else
        echo "   请任选其一："
        echo "     1) 用 Android Studio 打开本目录，它会自动补全 wrapper；"
        echo "     2) 安装 gradle 后重新运行本脚本。"
        exit 1
    fi
fi

# ---------- 4. 构建 ----------
echo ""
echo "🚀 开始构建 ($MODE) ..."
case "$MODE" in
    release)
        ./gradlew assembleRelease
        APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
        ;;
    clean)
        ./gradlew clean assembleDebug
        APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
        ;;
    *)
        ./gradlew assembleDebug
        APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
        ;;
esac

# ---------- 5. 结果 ----------
echo ""
if [ -f "$APK_PATH" ]; then
    echo "🎉 构建成功！APK 位置："
    echo "   $APK_PATH"
else
    echo "❌ 构建失败，请检查上方日志。"
    exit 1
fi