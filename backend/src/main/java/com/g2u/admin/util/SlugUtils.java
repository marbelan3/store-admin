package com.g2u.admin.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class SlugUtils {

    private SlugUtils() {
        // utility class
    }

    private static final Map<String, String> UA_TRANSLITERATION = new LinkedHashMap<>();

    static {
        // Multi-char mappings first (order matters for correct replacement)
        UA_TRANSLITERATION.put("щ", "shch");
        UA_TRANSLITERATION.put("ш", "sh");
        UA_TRANSLITERATION.put("ч", "ch");
        UA_TRANSLITERATION.put("ц", "ts");
        UA_TRANSLITERATION.put("х", "kh");
        UA_TRANSLITERATION.put("ж", "zh");
        UA_TRANSLITERATION.put("є", "ye");
        UA_TRANSLITERATION.put("ї", "yi");
        UA_TRANSLITERATION.put("ю", "yu");
        UA_TRANSLITERATION.put("я", "ya");
        // Single-char mappings
        UA_TRANSLITERATION.put("а", "a");
        UA_TRANSLITERATION.put("б", "b");
        UA_TRANSLITERATION.put("в", "v");
        UA_TRANSLITERATION.put("г", "h");
        UA_TRANSLITERATION.put("ґ", "g");
        UA_TRANSLITERATION.put("д", "d");
        UA_TRANSLITERATION.put("е", "e");
        UA_TRANSLITERATION.put("з", "z");
        UA_TRANSLITERATION.put("и", "y");
        UA_TRANSLITERATION.put("і", "i");
        UA_TRANSLITERATION.put("й", "y");
        UA_TRANSLITERATION.put("к", "k");
        UA_TRANSLITERATION.put("л", "l");
        UA_TRANSLITERATION.put("м", "m");
        UA_TRANSLITERATION.put("н", "n");
        UA_TRANSLITERATION.put("о", "o");
        UA_TRANSLITERATION.put("п", "p");
        UA_TRANSLITERATION.put("р", "r");
        UA_TRANSLITERATION.put("с", "s");
        UA_TRANSLITERATION.put("т", "t");
        UA_TRANSLITERATION.put("у", "u");
        UA_TRANSLITERATION.put("ф", "f");
        UA_TRANSLITERATION.put("ь", "");
    }

    /**
     * Generates a URL-friendly slug from the given name.
     * Supports Ukrainian characters via transliteration.
     *
     * @param name the input name
     * @return a lowercase, hyphen-separated slug
     */
    public static String generateSlug(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        String result = name.toLowerCase().trim();

        // Transliterate Ukrainian characters
        for (Map.Entry<String, String> entry : UA_TRANSLITERATION.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        // Replace non-alphanumeric characters with hyphens
        result = result.replaceAll("[^a-z0-9]", "-");
        // Collapse multiple hyphens
        result = result.replaceAll("-+", "-");
        // Trim hyphens from edges
        result = result.replaceAll("^-|-$", "");

        return result;
    }

    /**
     * Ensures the slug is unique by appending a numeric suffix if needed.
     *
     * @param slug        the base slug
     * @param existsCheck predicate that returns true if the slug already exists
     * @return a unique slug
     */
    public static String ensureUnique(String slug, Predicate<String> existsCheck) {
        if (!existsCheck.test(slug)) {
            return slug;
        }
        for (int i = 1; i <= 100; i++) {
            String candidate = slug + "-" + i;
            if (!existsCheck.test(candidate)) {
                return candidate;
            }
        }
        return slug + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
