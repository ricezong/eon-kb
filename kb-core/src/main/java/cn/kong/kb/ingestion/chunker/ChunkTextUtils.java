package cn.kong.kb.ingestion.chunker;

/**
 * 分块相关的纯文本工具：token 估算与重叠文本提取。
 *
 * <p>供各分块器与生成层（TokenBudgetManager）共用，
 * 避免各处复制粘贴，以及生成层反向依赖具体分块器的问题。</p>
 */
public final class ChunkTextUtils {

    private ChunkTextUtils() {
    }

    /**
     * 粗略估算文本的 token 数。
     * 中文：约 1.5 token / 字；英文：约 0.25 token / 词。
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int chineseChars = 0;
        int otherChars = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }
        return (int) (chineseChars * 1.5 + otherChars * 0.25);
    }

    /**
     * 从文本末尾向前提取不超过 overlapTokenTarget 个 token 的句子，
     * 作为相邻切片间的重叠内容，保持跨切片的语义连贯。
     */
    public static String buildOverlapText(String text, int overlapTokenTarget) {
        if (text == null || text.isEmpty() || overlapTokenTarget <= 0) return "";
        String[] sentences = text.split("[。！？.!?\\n]");
        StringBuilder overlap = new StringBuilder();
        int tokens = 0;

        // 从末尾向前取句子
        for (int i = sentences.length - 1; i >= 0 && tokens < overlapTokenTarget; i--) {
            overlap.insert(0, sentences[i].trim() + " ");
            tokens += estimateTokens(sentences[i]);
        }
        return overlap.toString().trim();
    }
}
