package planner;

import java.util.*;

/**
 * Bare-minimum JSON helpers for reading/writing our flat data structures.
 * Handles only the simple cases we actually need: string, number, array of objects.
 */
public class JsonUtil {

    public static String quote(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /** Parse a JSON object into a flat String→String map (one level deep). */
    public static Map<String, String> parseObject(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}"))  json = json.substring(0, json.length() - 1);

        // Split on top-level commas
        List<String> pairs = splitTopLevel(json, ',');
        for (String pair : pairs) {
            int colon = pair.indexOf(':');
            if (colon < 0) continue;
            String key   = unquote(pair.substring(0, colon).trim());
            String value = unquote(pair.substring(colon + 1).trim());
            map.put(key, value);
        }
        return map;
    }

    /** Parse a JSON array of objects into a list of flat maps. */
    public static List<Map<String, String>> parseArray(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        json = json.trim();
        if (json.startsWith("[")) json = json.substring(1);
        if (json.endsWith("]"))  json = json.substring(0, json.length() - 1);
        json = json.trim();
        if (json.isEmpty()) return result;

        List<String> objects = splitTopLevel(json, ',');
        // Re-join objects split across braces
        List<String> merged = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        for (String token : objects) {
            cur.append(token);
            for (char c : token.toCharArray()) {
                if (c == '{') depth++;
                if (c == '}') depth--;
            }
            if (depth == 0) {
                merged.add(cur.toString().trim());
                cur = new StringBuilder();
            } else {
                cur.append(',');
            }
        }

        for (String obj : merged) {
            if (!obj.isEmpty()) result.add(parseObject(obj));
        }
        return result;
    }

    /** Extract a top-level JSON array by key from a root object string. */
    public static String extractArray(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return "[]";
        int bracket = json.indexOf('[', idx + search.length());
        if (bracket < 0) return "[]";
        int depth = 0;
        for (int i = bracket; i < json.length(); i++) {
            if (json.charAt(i) == '[') depth++;
            if (json.charAt(i) == ']') depth--;
            if (depth == 0) return json.substring(bracket, i + 1);
        }
        return "[]";
    }

    /** Extract a top-level string value by key from a root object string. */
    public static String extractString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        String rest = json.substring(colon + 1).trim();
        if (rest.startsWith("\"")) {
            int end = rest.indexOf('"', 1);
            return rest.substring(1, end);
        }
        // unquoted value (number, boolean, null)
        int end = rest.indexOf(',');
        if (end < 0) end = rest.indexOf('}');
        return end < 0 ? rest.trim() : rest.substring(0, end).trim();
    }

    private static String unquote(String s) {
        if (s.startsWith("\"") && s.endsWith("\""))
            return s.substring(1, s.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        return s;
    }

    /** Split a string on a delimiter, but only at depth 0 (ignoring content inside {}, []). */
    private static List<String> splitTopLevel(String s, char delim) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) inString = !inString;
            if (!inString) {
                if (c == '{' || c == '[') depth++;
                if (c == '}' || c == ']') depth--;
                if (c == delim && depth == 0) {
                    parts.add(cur.toString());
                    cur = new StringBuilder();
                    continue;
                }
            }
            cur.append(c);
        }
        if (cur.length() > 0) parts.add(cur.toString());
        return parts;
    }
}
