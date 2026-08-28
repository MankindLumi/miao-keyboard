package com.xianyunb.miaokeyboard;

import android.inputmethodservice.InputMethodService;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 猫猫输入法服务。
 *
 * V1：提供英文/数字/符号基础键盘 + 一键「喵化」，提交文本自动走 MiaoTextProcessor。
 * 中文拼音输入引擎将在后续版本迭代（候选词引擎工程量大，先立骨架）。
 */
public class MiaoInputMethodService extends InputMethodService {

    private MiaoTextProcessor processor;
    private boolean miaoEnabled = true;

    @Override
    public void onCreate() {
        super.onCreate();
        processor = new MiaoTextProcessor();
        // 读取用户自定义词库与开关
        processor.loadFromPrefs(new MiaoPrefs(this));
    }

    @Override
    public View onCreateInputView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFEEE7FF);
        int pad = (int) (6 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        // 顶部说明 & 开关
        TextView title = new TextView(this);
        title.setText("喵喵输入法  @微信 QQ 抖音 通用");
        title.setTextColor(0xFF6C4AB6);
        title.setGravity(Gravity.CENTER);
        title.setTextSize(14);
        title.setPadding(0, 0, 0, pad);
        root.addView(title);

        Button toggle = new Button(this);
        toggle.setText("猫猫语气：开");
        toggle.setAllCaps(false);
        toggle.setOnClickListener(v -> {
            miaoEnabled = !miaoEnabled;
            toggle.setText(miaoEnabled ? "猫猫语气：开" : "猫猫语气：关");
        });
        root.addView(toggle);

        // 键行：1 2 3 4 5 6 7 8 9 0
        root.addView(buildRow(new String[]{"1","2","3","4","5","6","7","8","9","0"}, false));
        // 第一行字母
        root.addView(buildRow(new String[]{"q","w","e","r","t","y","u","i","o","p"}, false));
        // 第二行字母
        root.addView(buildRow(new String[]{"a","s","d","f","g","h","j","k","l"}, false));
        // 第三行字母 + 常用符号 + 退格
        root.addView(buildBottomRow());

        // 底部功能键
        root.addView(buildFunctionRow());

        return root;
    }

    private View buildRow(String[] keys, boolean isSymbol) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        int pad = (int) (4 * getResources().getDisplayMetrics().density);
        for (String k : keys) {
            Button b = new Button(this);
            b.setText(k);
            b.setAllCaps(false);
            b.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            b.setOnClickListener(v -> sendText(k));
            row.addView(b);
        }
        return row;
    }

    private View buildBottomRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        int pad = (int) (4 * getResources().getDisplayMetrics().density);

        for (String k : new String[]{"z","x","c","v","b","n","m",",",".","?"}) {
            Button b = new Button(this);
            b.setText(k);
            b.setAllCaps(false);
            b.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            b.setOnClickListener(v -> sendText(k));
            row.addView(b);
        }

        // 退格
        Button del = new Button(this);
        del.setText("⌫");
        del.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.deleteSurroundingText(1, 0);
            }
        });
        row.addView(del, new LinearLayout.LayoutParams((int)(60*getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private View buildFunctionRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        int pad = (int) (4 * getResources().getDisplayMetrics().density);

        // 空格
        Button space = new Button(this);
        space.setText("空格");
        space.setAllCaps(false);
        space.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
        space.setOnClickListener(v -> sendText(" "));
        row.addView(space);

        // 喵化一键：把当前整段文本转喵
        Button miao = new Button(this);
        miao.setText("喵！");
        miao.setAllCaps(false);
        miao.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        miao.setOnClickListener(v -> miaoWholeText());
        row.addView(miao);

        // 回车
        Button enter = new Button(this);
        enter.setText("↩");
        enter.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        enter.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.commitText("\n", 1);
            }
        });
        row.addView(enter);

        return row;
    }

    private void sendText(String t) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (miaoEnabled) {
            ic.commitText(t, 1);
        } else {
            ic.commitText(t, 1);
        }
    }

    /** 把当前编辑区整段文字喵化（读取全部 -> 处理 -> 替换回去）。 */
    private void miaoWholeText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        // 尝试读取附近文本（从光标往前取较大范围）
        try {
            // deleteSurrounding + 重新提交的做法；此处简化为读取当前选区文本并处理
            CharSequence before = ic.getTextBeforeCursor(500, 0);
            String beforeStr = before == null ? "" : before.toString();

            // 处理：取最后一次分词后的整句
            String output = processor.process(beforeStr);
            // 清掉原文，回填喵化文本
            ic.deleteSurroundingText(beforeStr.length(), 0);
            ic.commitText(output, 1);
            Toast.makeText(this, "喵化完成喵~", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "喵化出错了喵…", Toast.LENGTH_SHORT).show();
        }
    }
}