package tools.vitruv.framework.remote.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link StringUtils}.
 * Verifies behavior of the {@code randomString} method, which generates random alphanumeric strings.
 */
class StringUtilsTest {

    @Test
    void testRandomString_GeneratesCorrectLength() {
        // Arrange
        int length = 10;

        // Act
        String result = StringUtils.randomString(length);

        // Assert
        assertNotNull(result, "Generated string should not be null");
        assertEquals(length, result.length(), "Generated string length must match the requested length");
    }

    @Test
    void testRandomString_ThrowsExceptionForNonPositiveLength() {
        // Arrange
        int nonPositiveLength = 0;

        // Act and Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                StringUtils.randomString(nonPositiveLength));

        assertEquals("length must be positive", exception.getMessage(),
                "Exception message must indicate the invalid length");
    }

    @Test
    void testRandomString_GeneratesUniqueStrings() {
        // Arrange
        int length = 15;

        // Act
        String string1 = StringUtils.randomString(length);
        String string2 = StringUtils.randomString(length);

        // Assert
        assertNotNull(string1, "First generated string should not be null");
        assertNotNull(string2, "Second generated string should not be null");
        assertNotEquals(string1, string2, "Generated strings should be unique");
    }
}
