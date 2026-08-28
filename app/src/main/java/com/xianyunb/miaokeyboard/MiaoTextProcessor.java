package com.xianyunb.miaokeyboard;

import java.util.Random;

/**
 * 猫猫语气转换引擎（纯文本逻辑，与平台无关）。
 *
 * 核心规则：
 *  - 把你 → 主人
 *  - 把我 → 本喵
 *  - 句末/标点后随机附「喵」
 *  - 句末随机附猫咪颜文字
 *  - 支持配置自定义替换表与颜文字库
 */
public class MiaoTextProcessor {

    /** 默认替换表：原词 -> 猫猫词 */
    private String[][] replacePairs = new String[][]{
            {"你", "主人"},
            {"我", "本喵"},
            {"您", "主人"},
            {"咱们", "咱们喵"},
            // 常见口语转化
            {"好吗", "好喵"},
            {"好不好", "好不好喵"},
            {"知道了", "知道啦喵"},
            {"明白", "明白喵"},
            {"谢谢", "谢谢喵"},
            {"好的", "好哒喵"},
            {"嗯嗯", "嗯嗯喵~"},
            {"再见", "拜拜喵"},
    };

    /** 默认颜文字库 */
    private String[] kaomojiLib = new String[]{
            "喵~", "喵喵~", "喵!", "(｡•̀ᴗ-)✧", "(=^･ω･^=)",
            "ฅ^•ﻌ•^ฅ", "(≧ω≦)", "喵呜呜", "~(≧▽≦)", "(=^･ω･^=)ﾉ",
            "(=˘ω˘=)", "(^･ω･^)", "(=^..^=)ﾉ", "喵呜~", "( ˶ °ω° ˶ )",
    };

    public String[][] getDefaultReplacePairs() { return replacePairs; }
    public String[] getDefaultKaomojiLib() { return kaomojiLib; }

    public String[][] getReplacePairs() { return replacePairs; }
    public String[] getKaomojiLib() { return kaomojiLib; }

    private final Random random = new Random();

    private boolean enabledReplace = true;
    private boolean enabledMiao = true;
    private boolean enabledKaomoji = false;

    public void setCustomPairs(String[][] pairs) {
        if (pairs != null && pairs.length > 0) {
            this.replacePairs = pairs;
        }
    }

    public void setKaomojiLib(String[] lib) {
        if (lib != null && lib.length > 0) {
            this.kaomojiLib = lib;
        }
    }

    public void setFlags(boolean replace, boolean miao, boolean kaomoji) {
        this.enabledReplace = replace;
        this.enabledMiao = miao;
        this.enabledKaomoji = kaomoji;
    }

    /** 从持久化配置加载词库与开关。 */
    public void loadFromPrefs(MiaoPrefs prefs) {
        if (prefs == null) return;
        String[][] pairs = prefs.getReplacePairs(replacePairs);
        if (pairs != null && pairs.length > 0) replacePairs = pairs;
        String[] lib = prefs.getKaomojiLib(kaomojiLib);
        if (lib != null && lib.length > 0) kaomojiLib = lib;
        setFlags(prefs.isReplaceOn(), prefs.isMiaoOn(), prefs.isKaomojiOn());
    }

    /**
     * 把原始输入转成猫猫语气。
     * @param raw 用户原始输入
     * @return 转换后的文本
     */
    public String process(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        String text = raw;

        // 1. 词替换
        if (enabledReplace && replacePairs != null) {
            for (String[] pair : replacePairs) {
                if (pair.length == 2 && pair[0] != null && !pair[0].isEmpty()) {
                    text = text.replace(pair[0], pair[1]);
                }
            }
        }

        // 2. 句末/标点加喵
        if (enabledMiao) {
            text = appendMiao(text);
        }

        // 3. 末尾随机颜文字
        if (enabledKaomoji) {
            text = appendKaomoji(text);
        }

        return text;
    }

    private String appendMiao(String text) {
        if (text == null || text.isEmpty()) return text;
        String trimmed = text.replaceAll("\\s+$", "");
        if (trimmed.isEmpty()) return text;
        // 已经以「喵」结尾时不再重复追加，避免出现「喵喵」
        if (trimmed.endsWith("喵")) return text;
        return trimmed + "喵";
    }

    private String appendKaomoji(String text) {
        String picked = kaomojiLib[random.nextInt(kaomojiLib.length)];
        // 如果文本本身已经以颜文字/喵结尾，就不再叠加，避免堆砌
        String trimmed = text.trim();
        if (trimmed.endsWith("喵") || trimmed.endsWith("=") || trimmed.endsWith("^") || trimmed.endsWith(")")) {
            return text;
        }
        return text + " " + picked;
    }
}