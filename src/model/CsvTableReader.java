package model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class CsvTableReader {
    private CsvTableReader() {
    }

    static List<List<String>> read(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        ArrayList<List<String>> rows = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < content.length(); index++) {
            char current = content.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < content.length() && content.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                finishField(row, field);
            } else if ((current == '\n' || current == '\r') && !quoted) {
                index = skipLineFeedAfterCarriageReturn(content, index, current);
                finishField(row, field);
                finishRow(rows, row);
            } else {
                field.append(current);
            }
        }
        finishField(row, field);
        finishRow(rows, row);
        return rows;
    }

    private static int skipLineFeedAfterCarriageReturn(String content, int index, char current) {
        if (current == '\r' && index + 1 < content.length() && content.charAt(index + 1) == '\n') {
            return index + 1;
        }
        return index;
    }

    private static void finishField(List<String> row, StringBuilder field) {
        row.add(field.toString());
        field.setLength(0);
    }

    private static void finishRow(List<List<String>> rows, ArrayList<String> row) {
        if (!isEmptyRow(row)) {
            rows.add(List.copyOf(row));
        }
        row.clear();
    }

    private static boolean isEmptyRow(List<String> row) {
        return row.size() == 1 && row.get(0).isEmpty();
    }
}
