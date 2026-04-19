package io.github.sonofmagic.javachanges.core;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ReleaseJsonUtils {
    private ReleaseJsonUtils() {
    }

    static Map<String, Map<String, String>> parseFlatJsonObjects(String json) {
        Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
        Matcher matcher = Pattern.compile("\\{([^{}]*)\\}").matcher(json);
        while (matcher.find()) {
            String objectText = matcher.group(1);
            Map<String, String> fields = new HashMap<String, String>();
            Matcher fieldMatcher = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"((?:\\\\.|[^\"])*)\"|null)").matcher(objectText);
            while (fieldMatcher.find()) {
                String key = fieldMatcher.group(1);
                String rawValue = fieldMatcher.group(2);
                String value = "null".equals(rawValue) ? "" : jsonUnescape(fieldMatcher.group(3));
                fields.put(key, value);
            }
            String name = fields.containsKey("name") ? fields.get("name") : fields.get("key");
            if (name != null) {
                result.put(name, fields);
            }
        }
        return result;
    }

    static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static String jsonUnescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
