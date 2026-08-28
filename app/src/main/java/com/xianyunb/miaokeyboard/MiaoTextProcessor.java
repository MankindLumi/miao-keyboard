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
    private boolean enabledKaomoji = true;

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
        // 去掉末尾多余空白
        StringBuilder sb = new StringBuilder();
        // 在句子结尾标点后插入「喵」
        // 简单策略：以中文标点/.!?！？。... 结尾处加喵
        // 这里用简化实现：若文本末尾为标点则在其后补喵，否则在末尾补喵
        if (text.length() == 0) return text;

        String trimmed = text.replaceAll("\\s+$", "");
        if (trimmed.isEmpty()) return text;

        char last = trimmed.charAt(trimmed.length() - 1);
        // 标点触发：若末尾为常见句末标点，则在标点后加喵
        if (isSentenceEndMark(last)) {
            sb.append(trimmed).append("喵");
        } else if (couldBeEnd(trimmed)) {
            sb.append(trimmed).append("喵");
        } else {
            sb.append(trimmed);
        }
        return sb.toString();
    }

    private boolean isSentenceEndMark(char c) {
        return c == '。' || c == '！' || c == '？' || c == '!' || c == '?' ||
                c == '.' || c == '…' || c == ',' || c == '，' || c == '；';
    }

    // 无标点结尾时，默认也补喵（覆盖实时处理场景）
    private boolean couldBeEnd(String s) {
        return true;
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