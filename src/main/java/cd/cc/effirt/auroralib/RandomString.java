package cd.cc.effirt.auroralib;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.Random;

public class RandomString {
    private static final String DICTIONARY = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Random RANDOM = new Random();

    public static String generate(int length) {
        return generateRandomString(length, null, true);
    }

    public static String generate(int length, String customDictionary) {
        return generateRandomString(length, customDictionary, true);
    }

    public static String generate(int length, String customDictionary, boolean isSecureRandom) {
        return generateRandomString(length, customDictionary, isSecureRandom);
    }

    private static String generateRandomString(int length, String customDictionary, boolean isSecureRandom) {
        String dict = Objects.requireNonNullElse(customDictionary, DICTIONARY);
        StringBuilder sb = new StringBuilder(length);
        if (isSecureRandom) {
            for (int i = 0; i < length; ++i) {
                int idx = SECURE_RANDOM.nextInt(dict.length());
                sb.append(dict.charAt(idx));
            }
        } else {
            for (int i = 0; i < length; ++i) {
                int idx = RANDOM.nextInt(dict.length());
                sb.append(dict.charAt(idx));
            }
        }
        return sb.toString();
    }

    public static byte[] generateBinary(int length) {
        return generateBinary(length, true);
    }

    public static byte[] generateBinary(int length, boolean isSecureRandom) {
        byte[] iv = new byte[length];
        if (isSecureRandom) {
            SECURE_RANDOM.nextBytes(iv);
        } else {
            RANDOM.nextBytes(iv);
        }
        return iv;
    }

    public static double generateDouble() {
        return generateDouble(true);
    }

    public static double generateDouble(boolean isSecureRandom) {
        if (isSecureRandom) {
            return SECURE_RANDOM.nextDouble();
        } else {
            return RANDOM.nextDouble();
        }
    }

    public static double generateDouble(double min, double max) {
        return generateDouble(min,max,true);
    }

    public static double generateDouble(double min, double max,boolean isSecureRandom) {
        if (isSecureRandom) {
            return min + (max - min) * SECURE_RANDOM.nextDouble();
        } else {
            return min + (max - min) * RANDOM.nextDouble();
        }
    }

    public static boolean generateBoolean() {
        return generateBoolean(true);
    }

    public static boolean generateBoolean(boolean isSecureRandom) {
        if (isSecureRandom) {
            return SECURE_RANDOM.nextBoolean();
        } else {
            return RANDOM.nextBoolean();
        }
    }
}

