package ca.gradfinder.processing;

import java.util.*;
import java.util.stream.Collectors;

/** Writes rows as CSV (RFC 4180). Starts with a byte order mark so Excel reads accents and dashes correctly. */
public final class CsvWriter {
    public String write(List<Map<String, String>> rows) {
        if (rows.isEmpty()) return "\uFEFF";
        StringBuilder out = new StringBuilder("\uFEFF").append(line(rows.getFirst().keySet()));
        for (Map<String, String> row : rows) out.append(line(row.values()));
        return out.toString();
    }

    private String line(Collection<String> values) { return values.stream().map(this::cell).collect(Collectors.joining(",")) + "\r\n"; }

    private String cell(String value) {
        // Text from websites must not be run as a formula by a spreadsheet.
        String safe = !value.isEmpty() && "=+-@\t".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        return safe.matches("(?s).*[\",\r\n].*") ? '"' + safe.replace("\"", "\"\"") + '"' : safe;
    }
}
