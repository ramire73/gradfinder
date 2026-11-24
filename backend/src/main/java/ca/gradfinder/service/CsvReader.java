package ca.gradfinder.service;

import java.util.*;

/** Reads CSV text (RFC 4180): quoted cells may contain commas, quotes and line breaks. The first row is the header. */
public final class CsvReader {
    public List<Map<String, String>> read(String csv) {
        List<List<String>> table = cells(csv.startsWith("\uFEFF") ? csv.substring(1) : csv);
        if (table.isEmpty()) return List.of();
        List<String> header = table.getFirst().stream().map(String::strip).toList();
        List<Map<String, String>> rows = new ArrayList<>();
        for (List<String> cells : table.subList(1, table.size())) {
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < Math.min(header.size(), cells.size()); i++) row.put(header.get(i), cells.get(i).strip());
            if (row.values().stream().anyMatch(v -> !v.isEmpty())) rows.add(row);
        }
        return rows;
    }

    private List<List<String>> cells(String csv) {
        List<List<String>> table = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (quoted) {
                if (c != '"') cell.append(c);
                else if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') { cell.append('"'); i++; }
                else quoted = false;
            } else if (c == '"') quoted = true;
            else if (c == ',') { row.add(cell.toString()); cell.setLength(0); }
            else if (c == '\n') {
                row.add(cell.toString()); cell.setLength(0);
                table.add(row); row = new ArrayList<>();
            } else if (c != '\r') cell.append(c);
        }
        if (!cell.isEmpty() || !row.isEmpty()) { row.add(cell.toString()); table.add(row); }
        return table;
    }
}
