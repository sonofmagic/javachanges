package io.github.sonofmagic.javachanges.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ReleaseJsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ObjectWriter PRETTY_WRITER = MAPPER.writerWithDefaultPrettyPrinter();

    private ReleaseJsonUtils() {
    }

    static Map<String, Map<String, String>> parseFlatJsonObjects(String json) {
        Map<String, Map<String, String>> result = new LinkedHashMap<String, Map<String, String>>();
        JsonNode root = readTree(json);
        if (!root.isArray()) {
            return result;
        }
        for (JsonNode node : root) {
            if (!node.isObject()) {
                continue;
            }
            Map<String, String> fields = new LinkedHashMap<String, String>();
            java.util.Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                JsonNode valueNode = entry.getValue();
                fields.put(entry.getKey(), valueNode.isNull() ? "" : valueNode.asText());
            }
            String name = fields.containsKey("name") ? fields.get("name") : fields.get("key");
            if (name != null) {
                result.put(name, fields);
            }
        }
        return result;
    }

    static String jsonEscape(String value) {
        return new String(JsonStringEncoder.getInstance().quoteAsString(value));
    }

    static String jsonUnescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    static JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse JSON", exception);
        }
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to write JSON", exception);
        }
    }

    static String toPrettyJson(Object value) {
        try {
            return PRETTY_WRITER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to write JSON", exception);
        }
    }
}
