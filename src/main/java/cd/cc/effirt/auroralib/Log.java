package cd.cc.effirt.auroralib;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Log {
    private enum Level {
        INFO("INFO", null, 2),
        WARN("WARN", Print.colorStr("yellow", false), 2),
        ERROR("ERROR", Print.colorStr("red", false), 1),
        DEBUG("DEBUG", Print.colorRGBStr(181, 181, 239, false), 1);

        final String label;
        final String colorCode;
        final int padding;

        Level(String label, String colorCode, int padding) {
            this.label = label;
            this.colorCode = colorCode;
            this.padding = padding;
        }

        boolean isDisabled() {
            return switch (this) {
                case INFO -> !ENABLE_INFO;
                case WARN -> !ENABLE_WARN;
                case ERROR -> !ENABLE_ERROR;
                case DEBUG -> !ENABLE_DEBUG;
            };
        }
    }

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd-HH_mm_ss");

    private static final String GRAY = Print.colorRGBStr(91, 91, 91, false);
    private static final String RESET = Print.resetStr();

    private static final String NL = System.lineSeparator();

    // ---------------------------------------------------------------------
    // 统一开关语义
    // ---------------------------------------------------------------------
    private static boolean flag(String key, boolean defaultOn) {
        String v = System.getProperty(key);
        if (v == null) return defaultOn;
        Boolean b = parseFlag(v);
        return (b != null) ? b : false;
    }

    /**
     * 解析布尔字面量, 无法识别时返回 {@code null}。
     * 大小写不敏感, 前后空白会被忽略。
     */
    private static Boolean parseFlag(String v) {
        if (v == null) return null;
        String s = v.trim();
        if (s.isEmpty()) return null;

        return switch (s.toLowerCase(java.util.Locale.ROOT)) {
            case "1b", "true", "yes", "on", "y", "是", "真", "開", "开", "1" -> Boolean.TRUE;
            case "0b", "false", "no", "off", "n", "否", "假", "關", "关", "0" -> Boolean.FALSE;
            default -> null;
        };
    }

    private static final boolean ENABLE_COLOR = !flag("Log.disableColor", false);
    private static final boolean ENABLE_OUTPUT = flag("Log.output", true);
    private static volatile boolean ENABLE_INFO = flag("Log.info", true);
    private static volatile boolean ENABLE_WARN = flag("Log.warn", true);
    private static volatile boolean ENABLE_ERROR = flag("Log.error", true);
    private static volatile boolean ENABLE_DEBUG = flag("Log.debug", false);

    private static boolean FILE_ENABLED;

    private static volatile PrintWriter fileWriter = null;
    private static final Object FILE_LOCK = new Object();

    private static final StackWalker STACK_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    static {
        if (flag("Log.help", false)) {
            System.err.println("[Log] Usage" + NL + Usage.buildUsage(4, new String[]{
                    "-DLog.help", "Show this help message and exit (Default: false)",
                    "-DLog.disableColor", "Disable color (Default: false)",
                    "-DLog.output", "Enable output in terminal (Default: true)",
                    "-DLog.info", "Enable info (Default: true)",
                    "-DLog.warn", "Enable warn (Default: true)",
                    "-DLog.error", "Enable error (Default: true)",
                    "-DLog.debug", "Enable debug (Default: false)",
                    "-DLog.file", "Enable file record (Default: false)",
                    "-DLog.file.path", "Custom file path (Default: log/runtime_[time].log)"
            }));
            System.exit(1);
        }

        boolean fileEnabled = flag("Log.file", false);

        String raw = System.getProperty("Log.file.path");
        Path path;
        if (raw == null || raw.isEmpty()) {
            path = Paths.get("log", "runtime_" + fileLogTime() + ".log");
        } else {
            // 以分隔符结尾，或已存在且是目录 → 视为目录
            boolean dirHint = raw.endsWith("/") || raw.endsWith("\\");
            Path p = Paths.get(raw);
            if (dirHint || Files.isDirectory(p)) {
                path = p.resolve("runtime_" + fileLogTime() + ".log");
            } else {
                path = p;
            }
        }

        if (fileEnabled) {
            try {
                Path parent = path.getParent();
                if (parent != null) Files.createDirectories(parent);
                fileWriter = new PrintWriter(
                        Files.newBufferedWriter(path, StandardCharsets.UTF_8), true);
                Runtime.getRuntime().addShutdownHook(
                        new Thread(Log::close, "log-shutdown-hook"));
            } catch (Exception e) {
                fileEnabled = false;
                System.err.println("[Log] Failed to initialize log file");
                e.printStackTrace();
            }
        }
        FILE_ENABLED = fileEnabled;
    }

    // ---------------------------------------------------------------------
    // Static API
    // ---------------------------------------------------------------------

    public static void info(String context) {
        log(Level.INFO, null, context, null);
    }

    public static void info(String format, Object... args) {
        logArgs(Level.INFO, null, format, args);
    }

    public static void warn(String context) {
        log(Level.WARN, null, context, null);
    }

    public static void warn(String format, Object... args) {
        logArgs(Level.WARN, null, format, args);
    }

    public static void error(String context) {
        log(Level.ERROR, null, context, null);
    }

    public static void error(String format, Object... args) {
        logArgs(Level.ERROR, null, format, args);
    }

    public static void debug(String context) {
        log(Level.DEBUG, null, context, null);
    }

    public static void debug(String format, Object... args) {
        logArgs(Level.DEBUG, null, format, args);
    }

    // ---------------------------------------------------------------------
    // Core
    // ---------------------------------------------------------------------

    private static void logArgs(Level level, String handle, String format, Object... args) {
        if (level.isDisabled()) return;

        int argCount = (args == null) ? 0 : args.length;
        Throwable throwable = null;
        if (argCount > 0 && args[argCount - 1] instanceof Throwable) {
            throwable = (Throwable) args[--argCount];
        }
        log(level, handle, fmt(format, args, argCount), throwable);
    }

    private static void log(Level level, String handle, String context, Throwable throwable) {
        if (level.isDisabled()) return;

        String time = LocalDateTime.now().format(TIME_FORMATTER);
        String padding = " ".repeat(level.padding);

        String tail = renderThrowable(throwable);

        // 无颜色版本（写入文件；ENABLE_COLOR=false 时也用于控制台）
        String fileLine = (handle != null)
                ? String.format("%s %s%s| [%s] %s", time, level.label, padding, handle, context)
                : String.format("%s %s%s| %s", time, level.label, padding, context);
        String fileEntry = fileLine + NL + tail;

        // 控制台：单次 print 整块，保证正文与堆栈相邻
        if (ENABLE_OUTPUT) {
            String consoleLine;
            if (ENABLE_COLOR) {
                String color = (level.colorCode != null) ? level.colorCode : "";
                if (handle != null) {
                    consoleLine = String.format("%s%s%s %s%s%s%s%s|%s [%s] %s",
                            GRAY, time, RESET, color, level.label, RESET, padding, GRAY, RESET, handle, context);
                } else {
                    consoleLine = String.format("%s%s%s %s%s%s%s%s|%s %s",
                            GRAY, time, RESET, color, level.label, RESET, padding, GRAY, RESET, context);
                }
            } else {
                consoleLine = fileLine;
            }
            System.out.print(consoleLine + NL + tail);
            System.out.flush();
        }

        // 文件：同一把锁内单次 print 整块
        if (FILE_ENABLED) {
            synchronized (FILE_LOCK) {
                if (fileWriter != null) {
                    fileWriter.print(fileEntry);
                    fileWriter.flush();
                }
            }
        }
    }

    /**
     * 把完整堆栈渲染成一个字符串。
     * 保留 JDK 原始行首缩进（{@code \t}），不做任何对齐处理。
     * 无异常时返回空串。
     */
    private static String renderThrowable(Throwable throwable) {
        if (throwable == null) return "";

        StringWriter sw = new StringWriter(256);
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    // ---------------------------------------------------------------------
    // {} 替换
    // ---------------------------------------------------------------------

    private static String fmt(String template, Object[] args, int argCount) {
        if (template == null) return null;
        if (args == null || argCount == 0) return template;

        StringBuilder sb = new StringBuilder(template.length() + 50);
        int argIdx = 0;
        int len = template.length();

        for (int i = 0; i < len; ) {
            char c = template.charAt(i);

            // \\{} → 输出 "\" 并消费一个参数（优先匹配，避免被 \{} 抢占）
            if (c == '\\' && i + 3 < len
                    && template.charAt(i + 1) == '\\'
                    && template.charAt(i + 2) == '{'
                    && template.charAt(i + 3) == '}') {
                sb.append('\\');
                if (argIdx < argCount) appendParameter(sb, args[argIdx++]);
                i += 4;
            }
            // \{} → 输出字面量 "{}"，不消费参数
            else if (c == '\\' && i + 2 < len
                    && template.charAt(i + 1) == '{'
                    && template.charAt(i + 2) == '}') {
                sb.append("{}");
                i += 3;
            }
            // {\ → 丢弃紧跟 "{" 的 "\"，只输出 "{"（实现 {\} → {}、{\\} → {\}）
            else if (c == '{' && i + 1 < len && template.charAt(i + 1) == '\\') {
                sb.append('{');
                i += 2;
            }
            // {} → 消费一个参数；参数不足则原样保留
            else if (c == '{' && i + 1 < len && template.charAt(i + 1) == '}') {
                if (argIdx < argCount) appendParameter(sb, args[argIdx++]);
                else sb.append("{}");
                i += 2;
            }
            // 普通字符
            else {
                sb.append(c);
                i++;
            }
        }

        return sb.toString();
    }

    private static void appendParameter(StringBuilder sb, Object o) {
        appendParameter(sb, o, new HashMap<>());
    }

    private static void appendParameter(StringBuilder sb, Object o, Map<Object[], Object> seenMap) {
        if (o == null) {
            sb.append("null");
            return;
        }

        if (!o.getClass().isArray()) {
            appendSafe(sb, o);
            return;
        }

        // 基本类型数组（无法强转 Object[]）
        if (o instanceof boolean[]) sb.append(Arrays.toString((boolean[]) o));
        else if (o instanceof byte[]) sb.append(Arrays.toString((byte[]) o));
        else if (o instanceof char[]) sb.append(Arrays.toString((char[]) o));
        else if (o instanceof short[]) sb.append(Arrays.toString((short[]) o));
        else if (o instanceof int[]) sb.append(Arrays.toString((int[]) o));
        else if (o instanceof long[]) sb.append(Arrays.toString((long[]) o));
        else if (o instanceof float[]) sb.append(Arrays.toString((float[]) o));
        else if (o instanceof double[]) sb.append(Arrays.toString((double[]) o));
        else {
            // 对象数组：递归展开，带循环引用保护
            Object[] a = (Object[]) o;
            sb.append('[');
            if (!seenMap.containsKey(a)) {
                seenMap.put(a, null);
                int n = a.length;
                for (int k = 0; k < n; k++) {
                    appendParameter(sb, a[k], seenMap);
                    if (k != n - 1) sb.append(", ");
                }
                seenMap.remove(a);
            } else {
                sb.append("...");
            }
            sb.append(']');
        }
    }

    private static void appendSafe(StringBuilder sb, Object o) {
        try {
            sb.append(o);
        } catch (StackOverflowError | Exception t) {
            sb.append("[FAILED toString(): ").append(t.getMessage()).append("]");
        }
    }

    public static void close() {
        synchronized (FILE_LOCK) {
            if (fileWriter == null) return;

            try {
                fileWriter.flush();
                if (fileWriter.checkError()) {
                    System.err.println("[Log] Flush reported an error");
                }
                fileWriter.close();
                if (fileWriter.checkError()) {
                    System.err.println("[Log] Close reported an error");
                }
            } catch (Exception e) {
                System.err.println("[Log] Failed to close log file");
                e.printStackTrace(System.err);
            } finally {
                fileWriter = null;
            }
        }
    }

    private static String fileLogTime() {
        return LocalDateTime.now().format(FILE_TIME_FORMATTER);
    }

    // ---------------------------------------------------------------------
    // Logger API
    // ---------------------------------------------------------------------

    public interface Logger {
        void info(String context);

        void warn(String context);

        void error(String context);

        void debug(String context);

        void info(String format, Object... args);

        void warn(String format, Object... args);

        void error(String format, Object... args);

        void debug(String format, Object... args);
    }

    public static Logger getLogger(String handle) {
        final String h = (handle != null) ? handle
                : STACK_WALKER.getCallerClass().getName();

        return new Logger() {
            @Override
            public void info(String c) {
                log(Level.INFO, h, c, null);
            }

            @Override
            public void warn(String c) {
                log(Level.WARN, h, c, null);
            }

            @Override
            public void error(String c) {
                log(Level.ERROR, h, c, null);
            }

            @Override
            public void debug(String c) {
                log(Level.DEBUG, h, c, null);
            }

            @Override
            public void info(String f, Object... a) {
                logArgs(Level.INFO, h, f, a);
            }

            @Override
            public void warn(String f, Object... a) {
                logArgs(Level.WARN, h, f, a);
            }

            @Override
            public void error(String f, Object... a) {
                logArgs(Level.ERROR, h, f, a);
            }

            @Override
            public void debug(String f, Object... a) {
                logArgs(Level.DEBUG, h, f, a);
            }
        };
    }

    public static boolean isInfoEnabled() {
        return ENABLE_INFO;
    }

    public static boolean isWarnEnabled() {
        return ENABLE_WARN;
    }

    public static boolean isErrorEnabled() {
        return ENABLE_ERROR;
    }

    public static boolean isDebugEnabled() {
        return ENABLE_DEBUG;
    }

    public static void setEnableInfo(boolean enable) {
        ENABLE_INFO = enable;
    }

    public static void setEnableWarn(boolean enable) {
        ENABLE_WARN = enable;
    }

    public static void setEnableError(boolean enable) {
        ENABLE_ERROR = enable;
    }

    public static void setEnableDebug(boolean enable) {
        ENABLE_DEBUG = enable;
    }

    public static void setFileEnabled(boolean enable) {
        FILE_ENABLED = enable;
    }
}