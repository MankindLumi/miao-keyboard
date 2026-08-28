package com.xianyunb.miaokeyboard;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 主界面：总开关 + 功能开关 + 自定义替换词/颜文字 + 实时预览 + 悬浮窗开关。
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQ_OVERLAY = 1001;

    private MiaoPrefs prefs;
    private MiaoTextProcessor processor;

    private Switch swMaster;
    private Switch swReplace;
    private Switch swMiao;
    private Switch swKaomoji;
    private EditText etPairs;
    private EditText etKaomoji;
    private EditText etTest;
    private TextView tvResult;
    private Switch swAuto;
    private SeekBar sbDelay;
    private SeekBar sbInterval;
    private TextView tvDelayValue;
    private TextView tvIntervalValue;
    private Switch swFloating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new MiaoPrefs(this);
        processor = new MiaoTextProcessor();
        processor.loadFromPrefs(prefs);

        swMaster = findViewById(R.id.sw_master);
        swReplace = findViewById(R.id.sw_replace);
        swMiao = findViewById(R.id.sw_miao);
        swKaomoji = findViewById(R.id.sw_kaomoji);
        etPairs = findViewById(R.id.et_pairs);
        etKaomoji = findViewById(R.id.et_kaomoji);
        etTest = findViewById(R.id.et_test);
        tvResult = findViewById(R.id.tv_test_result);
        swAuto = findViewById(R.id.sw_auto);
        sbDelay = findViewById(R.id.sb_delay);
        sbInterval = findViewById(R.id.sb_interval);
        tvDelayValue = findViewById(R.id.tv_delay_value);
        tvIntervalValue = findViewById(R.id.tv_interval_value);
        swFloating = findViewById(R.id.sw_floating);

        loadUi();

        swMaster.setOnCheckedChangeListener((v, checked) -> prefs.saveMasterOn(checked));

        swAuto.setOnCheckedChangeListener((v, checked) -> saveAutoConfig());
        sbDelay.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                tvDelayValue.setText(String.format(java.util.Locale.CHINA, "%.1f 秒", delayFromProgress(p)));
                if (fromUser) saveAutoConfig();
            }
        });
        sbInterval.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                tvIntervalValue.setText(String.format(java.util.Locale.CHINA, "%.1f 秒", intervalFromProgress(p)));
                if (fromUser) saveAutoConfig();
            }
        });

        swFloating.setOnCheckedChangeListener((v, checked) -> {
            if (checked) {
                if (!Settings.canDrawOverlays(this)) {
                    swFloating.setChecked(false);
                    requestOverlayPermission();
                    return;
                }
                prefs.saveFloatingOn(true);
                startFloatingService();
            } else {
                prefs.saveFloatingOn(false);
                stopFloatingService();
            }
        });

        findViewById(R.id.btn_try).setOnClickListener(v -> doTest());

        findViewById(R.id.btn_save).setOnClickListener(v -> saveConfig());

        findViewById(R.id.btn_accessibility).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        // 恢复悬浮窗（若之前已开启且有权限）
        if (prefs.isFloatingOn() && Settings.canDrawOverlays(this)) {
            startFloatingService();
        }
    }

    private void requestOverlayPermission() {
        Toast.makeText(this, "请允许悬浮窗权限后重新开启喵~", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQ_OVERLAY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                prefs.saveFloatingOn(true);
                swFloating.setChecked(true);
                startFloatingService();
            }
        }
    }

    private void startFloatingService() {
        startService(new Intent(this, MiaoFloatingService.class));
    }

    private void stopFloatingService() {
        stopService(new Intent(this, MiaoFloatingService.class));
    }

    private void loadUi() {
        swMaster.setChecked(prefs.isMasterOn());
        swReplace.setChecked(prefs.isReplaceOn());
        swMiao.setChecked(prefs.isMiaoOn());
        swKaomoji.setChecked(prefs.isKaomojiOn());

        etPairs.setText(pairsToText(processor.getReplacePairs()));
        etKaomoji.setText(join(processor.getKaomojiLib(), "\n"));

        swAuto.setChecked(prefs.isAutoOn());
        sbDelay.setProgress(progressFromDelay(prefs.getAutoDelay()));
        sbInterval.setProgress(progressFromInterval(prefs.getAutoInterval()));
        tvDelayValue.setText(String.format(java.util.Locale.CHINA, "%.1f 秒", prefs.getAutoDelay()));
        tvIntervalValue.setText(String.format(java.util.Locale.CHINA, "%.1f 秒", prefs.getAutoInterval()));

        swFloating.setChecked(prefs.isFloatingOn());
    }

    private String pairsToText(String[][] pairs) {
        StringBuilder sb = new StringBuilder();
        for (String[] p : pairs) {
            if (p.length == 2) {
                sb.append(p[0]).append("=").append(p[1]).append("\n");
            }
        }
        return sb.toString();
    }

    private String join(String[] arr, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(sep);
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private void saveConfig() {
        String[][] pairs = parsePairsText(etPairs.getText().toString());
        prefs.saveReplacePairs(pairs);

        String[] lib = parseLibText(etKaomoji.getText().toString());
        prefs.saveKaomojiLib(lib);

        prefs.saveFlags(swReplace.isChecked(), swMiao.isChecked(), swKaomoji.isChecked());

        processor.setCustomPairs(pairs);
        processor.setKaomojiLib(lib);
        processor.setFlags(swReplace.isChecked(), swMiao.isChecked(), swKaomoji.isChecked());

        Toast.makeText(this, "已保存喵~", Toast.LENGTH_SHORT).show();
    }

    private void saveAutoConfig() {
        float delay = delayFromProgress(sbDelay.getProgress());
        float interval = intervalFromProgress(sbInterval.getProgress());
        prefs.saveAuto(swAuto.isChecked(), delay, interval);
    }

    private float delayFromProgress(int p) { return 0.5f + p * 0.1f; }
    private float intervalFromProgress(int p) { return 1.0f + p * 0.1f; }

    private int progressFromDelay(float sec) { return Math.round((sec - 0.5f) / 0.1f); }
    private int progressFromInterval(float sec) { return Math.round((sec - 1.0f) / 0.1f); }

    private abstract static class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    }

    private String[][] parsePairsText(String text) {
        java.util.List<String[]> list = new java.util.ArrayList<>();
        for (String line : text.split("\n")) {
            if (line == null || line.trim().isEmpty()) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String k = line.substring(0, eq).trim();
            String v = line.substring(eq + 1).trim();
            if (!k.isEmpty() && !v.isEmpty()) {
                list.add(new String[]{k, v});
            }
        }
        return list.toArray(new String[0][]);
    }

    private String[] parseLibText(String text) {
        java.util.List<String> list = new java.util.ArrayList<>();
        for (String line : text.split("\n")) {
            String s = line.trim();
            if (!s.isEmpty()) list.add(s);
        }
        return list.toArray(new String[0]);
    }

    private void doTest() {
        String input = etTest.getText().toString();
        if (input.isEmpty()) {
            Toast.makeText(this, "先输入点文字喵~", Toast.LENGTH_SHORT).show();
            return;
        }
        tvResult.setText(processor.process(input));
    }
}
