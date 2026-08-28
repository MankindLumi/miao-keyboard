#!/usr/bin/env bash
# ============================================================
#  喵喵输入法 一键推送到 GitHub
#  用法（先设好两个环境变量）：
#    export GITHUB_USER="你的GitHub用户名"
#    export GITHUB_REPO="miao-keyboard"     # 仓库名，可改
#    export GITHUB_TOKEN="ghp_xxxx..."       # Personal Access Token
#    ./push.sh
#  或直接编辑下方 DEFAULT_USER / DEFAULT_REPO 后运行。
# ============================================================
set -e
cd "$(dirname "$0")"

USER="${GITHUB_USER:-}"
REPO="${GITHUB_REPO:-miao-keyboard}"
TOKEN="${GITHUB_TOKEN:-}"

if [ -z "$USER" ]; then
    echo "❌ 请先设置 GitHub 用户名：export GITHUB_USER=你的用户名"
    exit 1
fi

echo "🐱 ========== 推送到 GitHub =========="
echo "   目标：https://github.com/${USER}/${REPO}"

# 检查是否已配置 remote
REMOTE_URL=$(git remote get-url origin 2>/dev/null || echo "")

if [ -z "$REMOTE_URL" ]; then
    if [ -n "$TOKEN" ]; then
        # 用 token 方式（HTTPS），token 不落盘到 remote URL，只用于本次 push
        git remote add origin "https://${USER}@github.com/${USER}/${REPO}.git"
        echo "✅ 已添加 remote origin"
    else
        echo ""
        echo "⚠️  未提供 GITHUB_TOKEN，尝试用 gh CLI 或 SSH 方式。"
        if command -v gh >/dev/null 2>&1; then
            echo "   检测到 gh，请先执行：gh auth login"
            git remote add origin "https://github.com/${USER}/${REPO}.git"
        else
            echo "   请提供 token：export GITHUB_TOKEN=ghp_xxx 后重跑。"
            exit 1
        fi
    fi
fi

# 推送
echo "🚀 推送中 ..."
if [ -n "$TOKEN" ]; then
    git push -u "https://${USER}:${TOKEN}@github.com/${USER}/${REPO}.git" main
else
    git push -u origin main
fi

echo ""
echo "🎉 推送完成！仓库地址：https://github.com/${USER}/${REPO}"