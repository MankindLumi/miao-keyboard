package com.xianyunb.miaokeyboard;

import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * 猫猫自动化喵化服务（无障碍）。
 *
 * 功能：
 *  - 自动模式：监测输入框文本变化，停止输入「触发延迟」秒后自动喵化；
 *  - 手动模式：悬浮窗点击后立即对当前焦点文本喵化一次；
 *  - 受「总开关」控制，关闭后自动/手动都不生效。
 */
public class MiaoAccessibilityService extends AccessibilityService {

    private static MiaoAccessibilityService instance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private MiaoTextProcessor processor;
    private long lastMiaoTime = 0L;
    private boolean processing = false;

    private final Runnable miaoTask = new Runnable() {
        @Override
        public void run() {
            doMiao(false);
        }
    };

    private final Runnable resetFlagTask = new Runnable() {
        @Override
        public void run() {
            processing = false;
        }
    };

    /** 供悬浮窗等服务获取当前无障碍服务实例。 */
    public static MiaoAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        processor = new MiaoTextProcessor();
        processor.loadFromPrefs(new MiaoPrefs(this));
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (processing) return;

        if (event.getEventType() != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            return;
        }

        MiaoPrefs prefs = new MiaoPrefs(this);
        if (!prefs.isMasterOn()) return;
        if (!prefs.isAutoOn()) return;

        handler.removeCallbacks(miaoTask);
        handler.postDelayed(miaoTask, (long) (prefs.getAutoDelay() * 1000f));
    }

    /** 手动触发一次喵化（供悬浮窗调用）。 */
    public void triggerManualMiao() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                doMiao(true);
            }
        });
    }

    private void doMiao(boolean manual) {
        MiaoPrefs prefs = new MiaoPrefs(this);
        if (!prefs.isMasterOn()) return;
        if (!manual && !prefs.isAutoOn()) return;

        long now = System.currentTimeMillis();
        if (!manual) {
            float intervalSec = prefs.getAutoInterval();
            if (intervalSec > 0 && now - lastMiaoTime < (long) (intervalSec * 1000f)) {
                return;
            }
        }

        AccessibilityNodeInfo node = findFocusedEditable(getRootInActiveWindow());
        if (node == null) return;

        CharSequence text = node.getText();
        if (text == null || text.length() == 0) return;

        String raw = text.toString();
        String output = processor.process(raw);

        if (output.equals(raw)) return;

        processing = true;
        lastMiaoTime = now;

        Bundle args = new Bundle();
        args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, output);
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);

        handler.removeCallbacks(resetFlagTask);
        handler.postDelayed(resetFlagTask, 300L);
    }

    private AccessibilityNodeInfo findFocusedEditable(AccessibilityNodeInfo root) {
        if (root == null) return null;
        AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused != null && focused.isEditable()) {
            return focused;
        }
        return findEditableRecursive(root);
    }

    private AccessibilityNodeInfo findEditableRecursive(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isEditable() && node.isFocused()) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            AccessibilityNodeInfo hit = findEditableRecursive(child);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacks(miaoTask);
        handler.removeCallbacks(resetFlagTask);
    }

    @Override
    public void onDestroy() {
        instance = null;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
