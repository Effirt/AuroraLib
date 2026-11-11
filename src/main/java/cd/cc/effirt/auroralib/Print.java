package cd.cc.effirt.auroralib;

import cd.cc.effirt.auroralib.exception.PrintException;

public class Print {

    public static String clearStr(boolean line) {
        if (line) {
            return "\033[2K";
        } else {
            return "\033[K";
        }
    }

    public static String moveStr(String moveFacing, int step) {
        return switch (moveFacing.toLowerCase()) {
            case "w" -> "\033[" + step + "A";
            case "a" -> "\033[" + step + "D";
            case "s" -> "\033[" + step + "B";
            case "d" -> "\033[" + step + "C";
            default -> throw new PrintException("Move error: " + moveFacing);
        };
    }

    public static String colorStr(String colorValue, boolean background) {
        String color = switch (colorValue.toLowerCase()) {
            case "green" -> background ? "42" : "32";
            case "red" -> background ? "41" : "31";
            case "blue" -> background ? "44" : "34";
            case "yellow" -> background ? "43" : "33";
            case "black" -> background ? "40" : "30";
            case "white" -> background ? "47" : "37";
            default -> throw new PrintException("Color error: " + colorValue);
        };
        return "\033[" + color + "m";
    }

    public static String color256Str(int colorCode, boolean background) {
        String prefix = background ? "48;5;" : "38;5;";
        return "\033[" + prefix + colorCode + "m";
    }

    public static String colorRGBStr(int r, int g, int b, boolean background) {
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        String type = background ? "48" : "38";
        return String.format("\033[%s;2;%d;%d;%dm", type, r, g, b);
    }

    public static String styleStr(String styleSelection) {
        String style = switch (styleSelection.toLowerCase()) {
            case "bol" -> "1";
            case "dim" -> "2";
            case "ita" -> "3";
            case "und" -> "4";
            case "bli" -> "5";
            case "ind" -> "7";
            case "hid" -> "8";
            case "str" -> "9";
            default -> throw new PrintException("Style error: " + styleSelection);
        };
        return "\033[" + style + "m";
    }

    public static String resetStr() {
        return "\033[0m";
    }

    public static String clearScreenStr() {
        return "\033[H\033[2J";
    }

    public static String cursorStr(int row, int col) {
        return String.format("\033[%d;%dH", row, col);
    }

    public static String scrollStr(int line, boolean moveFacing) {
        if (moveFacing) {
            return "\033[" + line + "S";
        } else {
            return "\033[" + line + "T";
        }
    }

    public static String setScrollStr(int top, int bottom) {
        return "\033[" + top + ";" + bottom + "r";
    }

    public static String resetScrollRegionStr() {
        return "\033[r";
    }

    public static String cursorDisplayStr(boolean display) {
        if (display) {
            return "\033[?25h";
        } else {
            return "\033[?25l";
        }
    }

    public static String cursorSaveStr(boolean save) {
        if (save) {
            return "\033[s";
        } else {
            return "\033[u";
        }
    }

    public static String setTitleStr(String title) {
        return "\033]0;" + title + "\007";
    }

    public static String alternateBufferStr(boolean enable) {
        if (enable) {
            return "\033[?1049h";
        } else {
            return "\033[?1049l";
        }
    }

    public static String queryCursorPosStr() {
        return "\033[6n";
    }

    public static String mouseDisplayStr(boolean show) {
        if (show) {
            return "\033[?1000h";
        } else {
            return "\033[?1000l";
        }
    }

    public static String clearBottomStr() {
        return "\033[J";
    }


    public static void clear(boolean line) {
        System.out.print(clearStr(line));
    }

    public static void move(String moveFacing, int step) {
        System.out.print(moveStr(moveFacing, step));
    }

    public static void color(String colorValue, boolean background) {
        System.out.print(colorStr(colorValue, background));
    }

    public static void color256(int colorCode, boolean background) {
        System.out.print(color256Str(colorCode, background));
    }

    public static void colorRGB(int r, int g, int b, boolean background) {
        System.out.print(colorRGBStr(r, g, b, background));
    }

    public static void style(String styleSelection) {
        System.out.print(styleStr(styleSelection));
    }

    public static void reset() {
        System.out.print(resetStr());
    }

    public static void clearScreen() {
        System.out.print(clearScreenStr());
    }

    public static void cursor(int row, int col) {
        System.out.print(cursorStr(row, col));
    }

    public static void scroll(int line, boolean moveFacing) {
        System.out.print(scrollStr(line, moveFacing));
    }

    public static void setScroll(int top, int bottom) {
        System.out.print(setScrollStr(top, bottom));
    }

    public static void resetScrollRegion() {
        System.out.print(resetScrollRegionStr());
    }

    public static void cursorDisplay(boolean display) {
        System.out.print(cursorDisplayStr(display));
    }

    public static void cursorSave(boolean save) {
        System.out.print(cursorSaveStr(save));
    }

    public static void setTitle(String title) {
        System.out.print(setTitleStr(title));
    }

    public static void alternateBuffer(boolean enable) {
        System.out.print(alternateBufferStr(enable));
    }

    public static void queryCursorPos() {
        System.out.print(queryCursorPosStr());
    }

    public static void mouseDisplay(boolean show) {
        System.out.print(mouseDisplayStr(show));
    }

    public static void clearBottom() {
        System.out.print(clearBottomStr());
    }
}