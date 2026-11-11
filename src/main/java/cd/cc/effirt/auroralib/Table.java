package cd.cc.effirt.auroralib;

import java.util.ArrayList;
import java.util.List;

public final class Table {

    private final List<String> headers;
    private final List<List<String>> rows = new ArrayList<>();
    private final int[] widths;

    public Table(String... headers) {
        this.headers = List.of(headers);
        this.widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = displayWidth(headers[i]);
        }
    }

    public Table row(String... cells) {
        if (cells.length != headers.size()) {
            throw new IllegalArgumentException("Column mismatch: expect: " + headers.size() + ", now: " + cells.length);
        }
        List<String> line = new ArrayList<>(cells.length);
        for (int i = 0; i < cells.length; i++) {
            String c = cells[i] == null ? "" : cells[i];
            widths[i] = Math.max(widths[i], displayWidth(c));
            line.add(c);
        }
        rows.add(line);
        return this;
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append(rule()).append(System.lineSeparator());
        sb.append(formatLine(headers)).append(System.lineSeparator());
        sb.append(rule()).append(System.lineSeparator());
        for (List<String> r : rows) {
            sb.append(formatLine(r)).append(System.lineSeparator());
        }
        sb.append(rule());
        return sb.toString();
    }

    private String rule() {
        StringBuilder sb = new StringBuilder();
        sb.append('+');
        for (int width : widths) {
            sb.append("-".repeat(width + 2));
            sb.append('+');
        }
        return sb.toString();
    }

    private String formatLine(List<String> cells) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < cells.size(); i++) {
            String c = cells.get(i);
            sb.append(' ').append(c);
            sb.append(" ".repeat(widths[i] - displayWidth(c)));
            sb.append(" |");
        }
        return sb.toString();
    }

    // ---------- CJK / emoji 显示宽度 ----------

    public static int displayWidth(String s) {
        if (s == null) return 0;
        int w = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (isZeroWidth(cp)) continue;
            w += isWide(cp) ? 2 : 1;
        }
        return w;
    }

    private static boolean isZeroWidth(int cp) {
        return (cp >= 0x0300 && cp <= 0x036F)   // combining marks
                || (cp >= 0x200B && cp <= 0x200F)   // zero-width space / joiners
                || (cp >= 0xFE00 && cp <= 0xFE0F)   // variation selectors
                || cp == 0xFEFF;
    }

    private static boolean isWide(int cp) {
        return (cp >= 0x1100 && cp <= 0x115F)   // Hangul Jamo
                || (cp >= 0x2E80 && cp <= 0xA4CF)   // CJK 部首 ~ Yi
                || (cp >= 0xAC00 && cp <= 0xD7A3)   // Hangul
                || (cp >= 0xF900 && cp <= 0xFAFF)   // CJK 兼容
                || (cp >= 0xFE30 && cp <= 0xFE4F)   // CJK 兼容形式
                || (cp >= 0xFF00 && cp <= 0xFF60)   // 全角
                || (cp >= 0xFFE0 && cp <= 0xFFE6)
                || (cp >= 0x1F300 && cp <= 0x1FAFF) // emoji
                || (cp >= 0x20000 && cp <= 0x3FFFD); // CJK 扩展
    }
}