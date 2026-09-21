package tools.vitruv.framework.remote.common.utils;

import java.security.SecureRandom;

/**
 * Utility class providing methods for common string-related operations.
 * This class cannot be instantiated.
 */
public final class StringUtils {

    private StringUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Generates a random alphanumeric string of the specified length.
     *
     * @param length The length of the random string to generate. Must be a positive integer.
     * @return A random alphanumeric string of the specified length.
     * @throws IllegalArgumentException If the specified length is not a positive integer.
     */
    public static String randomString(int length) {
        final String alphanumeric =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        final SecureRandom random = new SecureRandom();

        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive");
        }

        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(alphanumeric.length());
            result.append(alphanumeric.charAt(index));
        }
        return result.toString();
    }
}
