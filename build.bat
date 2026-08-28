@echo off
REM ============================================================
REM  喵喵输入法 (MiaoKeyboard) Windows 构建脚本
REM  用法：build.bat [debug|release|clean]
REM ============================================================
setlocal

cd /d "%~dp0"

set MODE=%1
if "%MODE%"=="" set MODE=debug

echo 🐱 ========== 喵喵输入法构建 ==========

REM 检查 Java
where java >nul 2>nul
if errorlevel 1 (
    echo ❌ 未检测到 Java，请安装 JDK 17+。
    exit /b 1
)
echo ✅ Java 已检测到

REM 检查 Android SDK
if "%ANDROID_HOME%"=="" if "%ANDROID_SDK_ROOT%"=="" (
    if not exist local.properties (
        echo.
        echo ⚠️ 未找到 local.properties，请指定 Android SDK 路径：
        echo    echo sdk.dir=D:\Android\Sdk ^> local.properties
        exit /b 1
    )
)
echo ✅ Android SDK 已配置

REM 检查 wrapper
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo.
    echo ⚠️ 缺少 gradle-wrapper.jar，请用 Android Studio 打开本目录自动补全。
    exit /b 1
)

REM 构建
echo.
echo 🚀 开始构建 (%MODE%) ...
if "%MODE%"=="release" (
    call gradlew.bat assembleRelease
    set APK_PATH=app\build\outputs\apk\release\app-release-unsigned.apk
) else if "%MODE%"=="clean" (
    call gradlew.bat clean assembleDebug
    set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
) else (
    call gradlew.bat assembleDebug
    set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
)

echo.
if exist "%APK_PATH%" (
    echo 🎉 构建成功！APK 位置：%APK_PATH%
) else (
    echo ❌ 构建失败，请检查上方日志。
    exit /b 1
)

endlocal