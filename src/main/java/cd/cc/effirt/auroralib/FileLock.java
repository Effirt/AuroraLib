package cd.cc.effirt.auroralib;

import cd.cc.effirt.auroralib.exception.LockFileException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileLock {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd;HH:mm:ss.SSS");
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([dhms])");

    private static Duration parseExpiredTime(String expired) {
        Matcher m = DURATION_PATTERN.matcher(expired);
        Duration total = Duration.ZERO;
        boolean hasMatch = false;

        while (m.find()) {
            hasMatch = true;
            long value = Long.parseLong(m.group(1));
            String unit = m.group(2);
            total = switch (unit) {
                case "d" -> total.plusDays(value);
                case "h" -> total.plusHours(value);
                case "m" -> total.plusMinutes(value);
                case "s" -> total.plusSeconds(value);
                default -> throw new LockFileException("Unsupported unit :" + unit);
            };
        }

        if (!hasMatch) {
            throw new LockFileException("Invalid expired format :" + expired);
        }
        return total;
    }

    public static void writeLock(Path lockPath, String expiredTimeStr) {
        Duration duration = parseExpiredTime(expiredTimeStr);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plus(duration);

        String content = now.format(FORMATTER) + " | " + expireAt.format(FORMATTER);
        try {
            Path parent = lockPath.getParent();
            if (parent != null) {
                try {
                    Files.createDirectories(parent);
                } catch (Exception e) {
                    throw new LockFileException("Failed to create parent directories for: " + lockPath, e);
                }
            }
            Files.writeString(lockPath, content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new LockFileException("Write lock file failed :" + lockPath, e);
        }
    }

    public static boolean expirationCheck(Path lockPath) {
        if (!Files.exists(lockPath)) {
            return true;
        }

        try {
            String content = Files.readString(lockPath).trim();
            String[] parts = content.split("\\s*\\|\\s*");

            LocalDateTime expireAt = LocalDateTime.parse(parts[1], FORMATTER);
            LocalDateTime now = LocalDateTime.now();
            return now.isAfter(expireAt);
        } catch (Exception e) {
            return true;
        }
    }

    public static void cleanIfExpired(Path lockPath) {
        if (expirationCheck(lockPath)) {
            try {
                Files.deleteIfExists(lockPath);
            } catch (Exception e) {
                throw new LockFileException("Failed to delete expired lock file :" + lockPath, e);
            }
        }
    }
}
