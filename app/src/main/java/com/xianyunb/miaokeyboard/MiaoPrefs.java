package com.xianyunb.miaokeyboard;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
/**
 * 配置存储（SharedPreferences 持久化）。
 * 保存：替换对、颜文字库、功能开关、总开关、悬浮窗开关、悬浮窗外观（透明度/图标/颜色/尺寸）。
 */
public class MiaoPrefs {
    private static final String PREF_NAME = "miao_prefs";
    private static final String KEY_REPLACE = "replace_pairs";
    private static final String KEY_KAOMOJI = "kaomoji_lib";
    private static final String KEY_REPLACE_ON = "replace_on";
    private static final String KEY_MIAO_ON = "miao_on";
    private static final String KEY_KAOMOJI_ON = "kaomoji_on";
    private static final String KEY_MASTER_ON = "master_on";
    private static final String KEY_AUTO_ON = "auto_on";
    private static final String KEY_AUTO_DELAY = "auto_delay";
    private static final String KEY_AUTO_INTERVAL = "auto_interval";
    private static final String KEY_FLOATING_ON = "floating_on";
    private static final String KEY_FLOAT_ALPHA = "float_alpha";
    private static final String KEY_FLOAT_TEXT = "float_text";
    private static final String KEY_FLOAT_COLOR = "float_color";
    private static final String KEY_FLOAT_SIZE = "float_size";
    private final SharedPreferences sp;

    public MiaoPrefs(Context context) {
        sp = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String[][] getReplacePairs(String[][] defaults) {
        String raw = sp.getString(KEY_REPLACE, null);
        if (raw == null) return defaults;
        return parsePairs(raw);
    }
    public void saveReplacePairs(String[][] pairs) {
        sp.edit().putString(KEY_REPLACE, pairsToRaw(pairs)).apply();
    }

    private String[][] parsePairs(String raw) {
        List<String[]> list = new ArrayList<>();
        String[] lines = raw.split("\n");
        for (String line : lines) {
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
    private String pairsToRaw(String[][] pairs) {
        StringBuilder sb = new StringBuilder();
        for (String[] p : pairs) {
            if (p.length == 2) {
                sb.append(p[0]).append('=').append(p[1]).append('\n');
            }
        }
        return sb.toString();
    }
    public String[] getKaomojiLib(String[] defaults) {
        String raw = sp.getString(KEY_KAOMOJI, null);
        if (raw == null) return defaults;
        List<String> list = new ArrayList<>();
        for (String line : raw.split("\n")) {
            String s = line.trim();
            if (!s.isEmpty()) list.add(s);
        }
        return list.isEmpty() ? defaults : list.toArray(new String[0]);
    }
    public void saveKaomojiLib(String[] lib) {
        StringBuilder sb = new StringBuilder();
        for (String s : lib) {
            if (s != null && !s.trim().isEmpty()) {
                sb.append(s.trim()).append('\n');
            }
        }
        sp.edit().putString(KEY_KAOMOJI, sb.toString()).apply();
    }
    public boolean isReplaceOn() { return sp.getBoolean(KEY_REPLACE_ON, true); }
    public boolean isMiaoOn()    { return sp.getBoolean(KEY_MIAO_ON, true); }
    public boolean isKaomojiOn() { return sp.getBoolean(KEY_KAOMOJI_ON, false); }
    public void saveFlags(boolean replace, boolean miao, boolean kaomoji) {
        sp.edit()
                .putBoolean(KEY_REPLACE_ON, replace)
                .putBoolean(KEY_MIAO_ON, miao)
                .putBoolean(KEY_KAOMOJI_ON, kaomoji)
                .apply();
    }
    public boolean isMasterOn() {
        return sp.getBoolean(KEY_MASTER_ON, true);
    }
    public void saveMasterOn(boolean on) {
        sp.edit().putBoolean(KEY_MASTER_ON, on).apply();
    }
    public boolean isAutoOn() {
        return sp.getBoolean(KEY_AUTO_ON, false);
    }
    public float getAutoDelay() {
        return sp.getFloat(KEY_AUTO_DELAY, 1.0f);
    }
    public float getAutoInterval() {
        return sp.getFloat(KEY_AUTO_INTERVAL, 3.0f);
    }
    public void saveAuto(boolean on, float delay, float interval) {
        sp.edit()
                .putBoolean(KEY_AUTO_ON, on)
                .putFloat(KEY_AUTO_DELAY, delay)
                .putFloat(KEY_AUTO_INTERVAL, interval)
                .apply();
    }

    // ---------- 悬浮窗 ----------
    public boolean isFloatingOn() {
        return sp.getBoolean(KEY_FLOATING_ON, false);
    }
    public void saveFloatingOn(boolean on) {
        sp.edit().putBoolean(KEY_FLOATING_ON, on).apply();
    }
    public float getFloatAlpha() {
        return sp.getFloat(KEY_FLOAT_ALPHA, 1.0f);
    }
    public void saveFloatAlpha(float alpha) {
        sp.edit().putFloat(KEY_FLOAT_ALPHA, alpha).apply();
    }
    public String getFloatText() {
        return sp.getString(KEY_FLOAT_TEXT, "🐾");
    }
    public void saveFloatText(String text) {
        sp.edit().putString(KEY_FLOAT_TEXT, text).apply();
    }
    public int getFloatColor() {
        return sp.getInt(KEY_FLOAT_COLOR, 0xFFE7C4F0);
    }
    public void saveFloatColor(int color) {
        sp.edit().putInt(KEY_FLOAT_COLOR, color).apply();
    }
    public int getFloatSize() {
        return sp.getInt(KEY_FLOAT_SIZE, 56);
    }
    public void saveFloatSize(int size) {
        sp.edit().putInt(KEY_FLOAT_SIZE, size).apply();
    }
}