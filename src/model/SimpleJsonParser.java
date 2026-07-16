package model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SimpleJsonParser {
    private final String input;
    private int index;

    private SimpleJsonParser(String input) {
        this.input = input;
    }

    static Object parse(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        try {
            return new SimpleJsonParser(content).parseDocument();
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid JSON in " + path + ": " + exception.getMessage(), exception);
        }
    }

    private Object parseDocument() {
        skipWhitespace();
        Object value = parseValue();
        skipWhitespace();
        if (index != input.length()) {
            throw error("Unexpected trailing content");
        }
        return value;
    }

    private Object parseValue() {
        skipWhitespace();
        if (index >= input.length()) {
            throw error("Unexpected end of input");
        }
        char current = input.charAt(index);
        if (current == '{') {
            return parseObject();
        }
        if (current == '[') {
            return parseArray();
        }
        if (current == '"') {
            return parseString();
        }
        if (current == 't') {
            return parseLiteral("true", Boolean.TRUE);
        }
        if (current == 'f') {
            return parseLiteral("false", Boolean.FALSE);
        }
        if (current == 'n') {
            return parseLiteral("null", null);
        }
        return parseNumber();
    }

    private Map<String, Object> parseObject() {
        LinkedHashMap<String, Object> object = new LinkedHashMap<>();
        expect('{');
        skipWhitespace();
        if (consume('}')) {
            return object;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            object.put(key, parseValue());
            skipWhitespace();
            if (consume('}')) {
                return object;
            }
            expect(',');
        }
    }

    private List<Object> parseArray() {
        ArrayList<Object> array = new ArrayList<>();
        expect('[');
        skipWhitespace();
        if (consume(']')) {
            return array;
        }
        while (true) {
            array.add(parseValue());
            skipWhitespace();
            if (consume(']')) {
                return array;
            }
            expect(',');
        }
    }

    private String parseString() {
        expect('"');
        StringBuilder result = new StringBuilder();
        while (index < input.length()) {
            char current = input.charAt(index++);
            if (current == '"') {
                return result.toString();
            }
            if (current == '\\') {
                result.append(parseEscape());
            } else {
                result.append(current);
            }
        }
        throw error("Unterminated string");
    }

    private char parseEscape() {
        if (index >= input.length()) {
            throw error("Unterminated escape sequence");
        }
        char escaped = input.charAt(index++);
        return switch (escaped) {
            case '"', '\\', '/' -> escaped;
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> parseUnicodeEscape();
            default -> throw error("Unsupported escape sequence: \\" + escaped);
        };
    }

    private char parseUnicodeEscape() {
        if (index + 4 > input.length()) {
            throw error("Incomplete unicode escape");
        }
        String hexadecimal = input.substring(index, index + 4);
        index += 4;
        try {
            return (char) Integer.parseInt(hexadecimal, 16);
        } catch (NumberFormatException exception) {
            throw error("Invalid unicode escape: " + hexadecimal);
        }
    }

    private Object parseNumber() {
        int start = index;
        consume('-');
        consumeDigits();
        boolean decimal = consumeFraction();
        boolean exponent = consumeExponent();
        String token = input.substring(start, index);
        try {
            return decimal || exponent ? Double.parseDouble(token) : Long.parseLong(token);
        } catch (NumberFormatException exception) {
            throw error("Invalid number: " + token);
        }
    }

    private void consumeDigits() {
        int start = index;
        while (index < input.length() && Character.isDigit(input.charAt(index))) {
            index++;
        }
        if (start == index) {
            throw error("Expected a digit");
        }
    }

    private boolean consumeFraction() {
        if (!consume('.')) {
            return false;
        }
        consumeDigits();
        return true;
    }

    private boolean consumeExponent() {
        if (index >= input.length() || (input.charAt(index) != 'e' && input.charAt(index) != 'E')) {
            return false;
        }
        index++;
        if (index < input.length() && (input.charAt(index) == '+' || input.charAt(index) == '-')) {
            index++;
        }
        consumeDigits();
        return true;
    }

    private Object parseLiteral(String literal, Object value) {
        if (!input.startsWith(literal, index)) {
            throw error("Expected " + literal);
        }
        index += literal.length();
        return value;
    }

    private void skipWhitespace() {
        while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
            index++;
        }
    }

    private void expect(char expected) {
        if (!consume(expected)) {
            throw error("Expected '" + expected + "'");
        }
    }

    private boolean consume(char expected) {
        if (index < input.length() && input.charAt(index) == expected) {
            index++;
            return true;
        }
        return false;
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at character " + index);
    }
}
