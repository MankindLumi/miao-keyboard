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
 *  - 在任意 App 的输入框中监测文本变化；
 *  - 用户停止输入「触发延迟」秒后，自动把整段文字喵化；
 *  - 两次自动喵化之间至少间隔「执行间隔」秒，避免频繁刷屏；
 *  - 延迟与间隔均可在主界面手动调节。
 */
public class MiaoAccessibilityService extends AccessibilityService {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private MiaoTextProcessor processor;
    private long lastMiaoTime = 0L;
    private boolean processing = false;

    private final Runnable miaoTask = new Runnable() {
        @Override
        public void run() {
            doAutoMiao();
        }
    };

    private final Runnable resetFlagTask = new Runnable() {
        @Override
        public void run() {
            processing = false;
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        processor = new MiaoTextProcessor();
        processor.loadFromPrefs(new MiaoPrefs(this));
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (processing) return; // 忽略自己写回文本触发的事件，防止死循环

        if (event.getEventType() != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            return;
        }

        MiaoPrefs prefs = new MiaoPrefs(this);
        if (!prefs.isAutoOn()) return;

        // 防抖：每次文本变化都重置计时器，停止输入「延迟」秒后才触发
        handler.removeCallbacks(miaoTask);
        handler.postDelayed(miaoTask, (long) (prefs.getAutoDelay() * 1000f));
    }

    private void doAutoMiao() {
        MiaoPrefs prefs = new MiaoPrefs(this);
        if (!prefs.isAutoOn()) return;

        // 执行间隔判断
        long now = System.currentTimeMillis();
        float intervalSec = prefs.getAutoInterval();
        if (intervalSec > 0 && now - lastMiaoTime < (long) (intervalSec * 1000f)) {
            return;
        }

        AccessibilityNodeInfo node = findFocusedEditable(getRootInActiveWindow());
        if (node == null) return;

        CharSequence text = node.getText();
        if (text == null || text.length() == 0) return;

        String raw = text.toString();
        String output = processor.process(raw);

        // 没有变化则跳过，避免无意义的写回
        if (output.equals(raw)) return;

        processing = true;
        lastMiaoTime = now;

        Bundle args = new Bundle();
        args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, output);
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);

        // 短暂延迟后解除 processing 标志，继续响应新的输入
        handler.removeCallbacks(resetFlagTask);
        handler.postDelayed(resetFlagTask, 300L);
    }

    /** 找到当前聚焦的可编辑节点。 */
    private AccessibilityNodeInfo findFocusedEditable(AccessibilityNodeInfo root) {
        if (root == null) return null;
        AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused != null && focused.isEditable()) {
            return focused;
        }
        // 兜底：遍历找可编辑且聚焦的节点
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
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
