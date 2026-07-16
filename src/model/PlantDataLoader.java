package model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlantDataLoader {
    public List<PlantDefinition> load(Path path) throws IOException {
        List<List<String>> rows = CsvTableReader.read(path);
        if (rows.isEmpty()) {
            throw new IOException("Plant data is empty: " + path);
        }
        Map<String, Integer> columns = indexHeaders(rows.get(0));
        ArrayList<PlantDefinition> definitions = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            try {
                definitions.add(parseRow(row, columns));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid plant data at row " + (rowIndex + 1)
                    + ": " + exception.getMessage(), exception);
            }
        }
        validateUniqueNames(definitions, path);
        return List.copyOf(definitions);
    }

    private PlantDefinition parseRow(List<String> row, Map<String, Integer> columns) {
        int id = parseInteger(value(row, columns, "ID"), "ID");
        String name = value(row, columns, "Name");
        String category = value(row, columns, "Category");
        List<String> tags = splitList(value(row, columns, "Tags"));
        int cost = parseInteger(value(row, columns, "Cost"), "Cost");
        int health = parseInteger(value(row, columns, "Base HP"), "Base HP");
        String damage = value(row, columns, "Damage");
        String ability = value(row, columns, "Base Ability");
        String foodEffect = value(row, columns, "Plant Food Effect");
        List<String> upgrades = List.of(
            value(row, columns, "Lvl 2"),
            value(row, columns, "Lvl 3"),
            value(row, columns, "Lvl 4")
        );
        Double actionInterval = parseOptionalDouble(value(row, columns, "Action Interval (s)"));
        Double recharge = parseOptionalDouble(value(row, columns, "Recharge (s)"));
        return new PlantDefinition(id, name, category, tags, cost, health, damage, ability,
            foodEffect, upgrades, actionInterval, recharge);
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

    private int parseInteger(String value, String fieldName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be an integer: " + value);
        }
    }

    private Double parseOptionalDouble(String value) {
        if (value == null || value.isBlank() || value.trim().equals("-")) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid decimal value: " + value);
        }
    }

    private List<String> splitList(String value) {
        ArrayList<String> result = new ArrayList<>();
        if (value != null && !value.isBlank() && !value.trim().equals("-")) {
            for (String item : value.split(",")) {
                if (!item.isBlank()) {
                    result.add(item.trim());
                }
            }
        }
        return result;
    }

    private void validateUniqueNames(List<PlantDefinition> definitions, Path path) throws IOException {
        LinkedHashMap<String, String> names = new LinkedHashMap<>();
        for (PlantDefinition definition : definitions) {
            String previous = names.put(definition.getNormalizedName(), definition.getName());
            if (previous != null) {
                throw new IOException("Duplicate plant name in " + path + ": " + definition.getName());
            }
        }
    }
}
