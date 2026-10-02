package com.recruitshield;

import com.recruitshield.converter.StringListConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 11 - Priority 10: JPA StringListConverter Attribute Converter Unit Tests.
 *
 * Verifies serialization and deserialization of List<String> to/from database VARCHAR columns:
 * 1. Null and empty collections serialize to empty strings.
 * 2. Multi-element lists serialize with delimiter ';;;'.
 * 3. Null, empty, and whitespace strings deserialize to empty lists without NPE.
 * 4. Delimited strings split, trim, and filter blank entries cleanly.
 * 5. Full bidirectional fidelity is preserved across special characters.
 */
class StringListConverterTest {

    private StringListConverter converter;

    @BeforeEach
    void setUp() {
        converter = new StringListConverter();
    }

    @Test
    @DisplayName("1. Convert to DB: Null list produces empty string")
    void testConvertToDatabaseColumnNull() {
        String result = converter.convertToDatabaseColumn(null);
        assertEquals("", result);
    }

    @Test
    @DisplayName("2. Convert to DB: Empty list produces empty string")
    void testConvertToDatabaseColumnEmpty() {
        String result = converter.convertToDatabaseColumn(new ArrayList<>());
        assertEquals("", result);
    }

    @Test
    @DisplayName("3. Convert to DB: Single element list produces single string without delimiter")
    void testConvertToDatabaseColumnSingle() {
        String result = converter.convertToDatabaseColumn(List.of("Single Flag"));
        assertEquals("Single Flag", result);
    }

    @Test
    @DisplayName("4. Convert to DB: Multiple elements are joined by delimiter ';;;'")
    void testConvertToDatabaseColumnMultiple() {
        String result = converter.convertToDatabaseColumn(List.of("Flag 1", "Flag 2", "Flag 3"));
        assertEquals("Flag 1;;;Flag 2;;;Flag 3", result);
    }

    @Test
    @DisplayName("5. Convert to Entity: Null string produces empty list")
    void testConvertToEntityAttributeNull() {
        List<String> result = converter.convertToEntityAttribute(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("6. Convert to Entity: Empty string produces empty list")
    void testConvertToEntityAttributeEmpty() {
        List<String> result = converter.convertToEntityAttribute("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("7. Convert to Entity: Whitespace string produces empty list")
    void testConvertToEntityAttributeWhitespace() {
        List<String> result = converter.convertToEntityAttribute("   \n\t   ");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("8. Convert to Entity: Single item string produces single element list")
    void testConvertToEntityAttributeSingle() {
        List<String> result = converter.convertToEntityAttribute("Only Signal");
        assertEquals(List.of("Only Signal"), result);
    }

    @Test
    @DisplayName("9. Convert to Entity: Delimited string splits into elements in order")
    void testConvertToEntityAttributeMultiple() {
        List<String> result = converter.convertToEntityAttribute("Signal A;;;Signal B;;;Signal C");
        assertEquals(List.of("Signal A", "Signal B", "Signal C"), result);
    }

    @Test
    @DisplayName("10. Convert to Entity: Trims items and filters consecutive/empty delimiters")
    void testConvertToEntityAttributeFiltersEmptyAndTrims() {
        String rawDbData = "  Flag Alpha  ;;;   ;;; Flag Beta ;;;  ;;;Flag Gamma;;; ";
        List<String> result = converter.convertToEntityAttribute(rawDbData);
        assertEquals(List.of("Flag Alpha", "Flag Beta", "Flag Gamma"), result);
    }

    @Test
    @DisplayName("11. Bidirectional Integrity: Serializing and deserializing preserves original elements")
    void testBidirectionalIntegrity() {
        List<String> original = List.of(
                "Advance payment requested: Rs 2500",
                "Company website uses insecure HTTP: http://domain.xyz",
                "Recruiter freemail: contact@gmail.com"
        );

        String dbColumn = converter.convertToDatabaseColumn(original);
        List<String> reconstructed = converter.convertToEntityAttribute(dbColumn);

        assertEquals(original, reconstructed);
    }

    @Test
    @DisplayName("12. Special Characters: Punctuation, commas, quotes, and symbols are preserved across conversion")
    void testSpecialCharactersPreserved() {
        List<String> original = List.of(
                "Quote: \"Urgent interview!\"",
                "Commas, colons: and semi-colons; preserved",
                "Math & symbols: 100% genuine @ 5.0 LPA / (WFH)"
        );

        String dbColumn = converter.convertToDatabaseColumn(original);
        List<String> reconstructed = converter.convertToEntityAttribute(dbColumn);

        assertEquals(original, reconstructed);
    }
}
