package model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QuestDataLoader {
    public List<QuestDefinition> load(Path path) throws IOException {
        List<List<String>> rows = CsvTableReader.read(path);
        if (rows.isEmpty()) {
            throw new IOException("Quest data is empty: " + path);
        }
        Map<String, Integer> columns = indexHeaders(rows.get(0));
        ArrayList<QuestDefinition> definitions = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            try {
                definitions.add(parseRow(row, columns));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid quest data at row " + (rowIndex + 1)
                    + ": " + exception.getMessage(), exception);
            }
        }
        return List.copyOf(definitions);
    }

    private QuestDefinition parseRow(List<String> row, Map<String, Integer> columns) {
        return new QuestDefinition(
            value(row, columns, "نام کوئست ها"),
            value(row, columns, "دسته بندی"),
            value(row, columns, "شرط تکمیلی"),
            value(row, columns, "نوع پاداش"),
            value(row, columns, "اولویت"),
            value(row, columns, "متغیرها")
        );
    }

    private Map<String, Integer> indexHeaders(List<String> headers) {
        LinkedHashMap<String, Integer> columns = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            columns.put(headers.get(index).trim(), index);
        }
        return columns;
    }

    private String value(List<String> row, Map<String, Integer> columns, String header) {
        Integer columnIndex = columns.get(header);
        if (columnIndex == null) {
            throw new IllegalArgumentException("Missing required column: " + header);
        }
        return columnIndex < row.size() ? row.get(columnIndex).trim() : "";
    }
}
